package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.ThemePreferences
import com.example.data.model.ActivityLog
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.LaundryOrder
import com.example.data.model.LaundryService
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.ShopProfile
import com.example.data.model.StaffInvite
import com.example.data.model.StaffPermissions
import com.example.data.model.StatusHistoryEntry
import com.example.data.model.UserRole
import com.example.data.model.WhatsAppLogEntry
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.util.Formatters
import com.example.util.WhatsAppHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

data class NewOrderRequest(
  val draftOrderId: String,
  val customerId: String?,
  val customerName: String,
  val customerPhone: String,
  val customerAddress: String,
  val items: List<OrderItem>,
  val discount: Long,
  val extraFee: Long,
  val extraFeeLabel: String,
  val taxRatePercent: Double = 0.0,
  val initialPaymentStatus: PaymentStatus,
  val paymentMethod: PaymentMethod,
  val receivedCashAmount: Long,
  val dpAmount: Long,
  val notes: String,
  val estimatedFinishAtMillis: Long,
  val transactionAtMillis: Long = System.currentTimeMillis()
)

class LaundryRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth,
  private val appContext: Context? = null
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    FirebaseAuth.getInstance(),
    context.applicationContext
  )

  private val isEn: Boolean
    get() = appContext?.let { ThemePreferences.getSavedLanguageSync(it) == AppLanguage.EN } ?: false

  private fun msg(id: String, en: String): String = if (isEn) en else id

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException(
        msg(
          "Pengguna harus login terlebih dahulu sebelum mengakses database.",
          "User must sign in before accessing the database."
        )
      )

  private fun <T> safeSnapshotFlow(
    path: String,
    operationType: OperationType = OperationType.LIST,
    upstream: () -> Flow<T>
  ): Flow<T> = flow {
    val uid = requireUserId()
    emitAll(
      upstream().catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
          handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
      }
    )
  }

  fun observeShop(shopId: String): Flow<ShopProfile?> =
    safeSnapshotFlow("shops/$shopId", OperationType.GET) {
      db.collection("shops").document(shopId)
        .snapshots()
        .map { snap ->
          snap.toObject(ShopProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
    }

  fun observeServices(shopId: String): Flow<List<LaundryService>> =
    safeSnapshotFlow("shops/$shopId/services") {
      db.collection("shops").document(shopId).collection("services")
        .snapshots()
        .map { snap ->
          snap.toObjects(
            LaundryService::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedWith(compareBy<LaundryService> { it.isExtraFee }.thenBy { it.category }.thenBy { it.name })
        }
    }

  fun observeCustomers(shopId: String): Flow<List<Customer>> =
    safeSnapshotFlow("shops/$shopId/customers") {
      db.collection("shops").document(shopId).collection("customers")
        .snapshots()
        .map { snap ->
          snap.toObjects(
            Customer::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedByDescending { it.lastTransactionAtMillis }
        }
    }

  fun observeOrders(shopId: String): Flow<List<LaundryOrder>> =
    safeSnapshotFlow("shops/$shopId/orders") {
      db.collection("shops").document(shopId).collection("orders")
        .snapshots()
        .map { snap ->
          snap.toObjects(
            LaundryOrder::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedByDescending { it.transactionAtMillis }
        }
    }

  fun observeExpenses(shopId: String): Flow<List<Expense>> =
    safeSnapshotFlow("shops/$shopId/expenses") {
      db.collection("shops").document(shopId).collection("expenses")
        .snapshots()
        .map { snap ->
          snap.toObjects(
            Expense::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedByDescending { it.dateMillis }
        }
    }

  fun observeExpenseCategories(shopId: String): Flow<List<ExpenseCategory>> =
    safeSnapshotFlow("shops/$shopId/expenseCategories") {
      db.collection("shops").document(shopId).collection("expenseCategories")
        .snapshots()
        .map { snap ->
          snap.toObjects(
            ExpenseCategory::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedBy { it.name }
        }
    }

  fun observeStaffInvites(ownerUid: String): Flow<List<StaffInvite>> =
    safeSnapshotFlow("staffDirectory") {
      db.collection("staffDirectory")
        .whereEqualTo("ownerUid", ownerUid)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            StaffInvite::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedBy { it.name }
        }
    }

  fun observeActivityLogs(shopId: String): Flow<List<ActivityLog>> =
    safeSnapshotFlow("shops/$shopId/activityLogs") {
      db.collection("shops").document(shopId).collection("activityLogs")
        .snapshots()
        .map { snap ->
          snap.toObjects(
            ActivityLog::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
    }

  suspend fun createOrder(
    shopId: String,
    notaPrefix: String,
    actorName: String,
    actorRole: UserRole,
    request: NewOrderRequest
  ): Result<LaundryOrder> {
    val uid = requireUserId()
    val ordersCol = db.collection("shops").document(shopId).collection("orders")
    return try {
      require(request.customerName.trim().length >= 2) {
        msg("Nama pelanggan minimal 2 karakter.", "Customer name must be at least 2 characters.")
      }
      val normalizedPhone = WhatsAppHelper.normalizeIndonesianPhone(request.customerPhone)
        ?: throw IllegalArgumentException(
          msg(
            "Nomor WhatsApp pelanggan tidak valid (contoh: 08123456789).",
            "Invalid customer WhatsApp number (e.g. 08123456789)."
          )
        )
      require(request.items.isNotEmpty()) {
        msg("Pilih minimal 1 layanan laundry.", "Select at least 1 laundry service.")
      }
      require(request.items.all { it.quantity > 0.0 && it.unitPrice >= 0L }) {
        msg("Berat/jumlah dan harga satuan harus bernilai valid.", "Weight/quantity and unit price must be valid.")
      }
      require(request.discount >= 0L && request.extraFee >= 0L) {
        msg("Diskon dan biaya tambahan tidak boleh negatif.", "Discount and extra fee cannot be negative.")
      }
      require(request.taxRatePercent in 0.0..100.0) {
        msg("Tarif PPN harus antara 0% dan 100%.", "Tax rate must be between 0% and 100%.")
      }

      val totals = Formatters.calculateOrderTotalsWithTax(
        request.items,
        request.discount,
        request.extraFee,
        request.taxRatePercent
      )
      val subtotal = totals.subtotal
      val safeDiscount = totals.discount
      val taxAmount = totals.taxAmount
      val totalAmount = totals.totalAmount

      val paidAmount: Long
      val receivedAmount: Long
      val changeAmount: Long
      val paymentStatus: PaymentStatus

      when (request.initialPaymentStatus) {
        PaymentStatus.UNPAID -> {
          paidAmount = 0L
          receivedAmount = 0L
          changeAmount = 0L
          paymentStatus = PaymentStatus.UNPAID
        }
        PaymentStatus.DP -> {
          require(request.dpAmount > 0L) {
            msg("Nominal uang muka (DP) harus lebih dari Rp0.", "Down payment amount must be greater than Rp0.")
          }
          require(request.dpAmount <= totalAmount) {
            msg("Nominal DP tidak boleh melebihi total tagihan.", "Down payment cannot exceed total bill.")
          }
          paidAmount = request.dpAmount
          receivedAmount = request.dpAmount
          changeAmount = 0L
          paymentStatus = if (paidAmount >= totalAmount) PaymentStatus.PAID else PaymentStatus.DP
        }
        PaymentStatus.PAID -> {
          if (request.paymentMethod == PaymentMethod.CASH) {
            require(request.receivedCashAmount >= totalAmount) {
              msg("Uang tunai diterima kurang dari total tagihan.", "Cash received is less than total bill.")
            }
            receivedAmount = request.receivedCashAmount
            changeAmount = request.receivedCashAmount - totalAmount
          } else {
            receivedAmount = totalAmount
            changeAmount = 0L
          }
          paidAmount = totalAmount
          paymentStatus = PaymentStatus.PAID
        }
      }

      val remainingAmount = (totalAmount - paidAmount).coerceAtLeast(0L)
      val orderRef = ordersCol.document(request.draftOrderId)
      val counterRef = db.collection("shops").document(shopId).collection("counters").document("orders")
      val customerId = request.customerId?.takeIf { it.isNotBlank() }
        ?: "cust_${normalizedPhone}"
      val customerRef = db.collection("shops").document(shopId).collection("customers").document(customerId)
      val logRef = db.collection("shops").document(shopId).collection("activityLogs").document()

      val cleanPrefix = notaPrefix.trim().uppercase(Locale.ROOT).ifBlank { "ADA" }
      val dateCode = Formatters.formatCompactDateForNota(request.transactionAtMillis)

      val createdOrder = db.runTransaction { tx ->
        // Prevent duplicate order creation on double-tap / retry
        val existingOrderSnap = tx.get(orderRef)
        if (existingOrderSnap.exists()) {
          val existing = existingOrderSnap.toObject(
            LaundryOrder::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
          if (existing != null) return@runTransaction existing
        }

        val counterSnap = tx.get(counterRef)
        val nextSeq = if (counterSnap.exists()) {
          (counterSnap.getLong("seq") ?: 0L) + 1L
        } else {
          1L
        }

        val customerSnap = tx.get(customerRef)
        val prevCount = if (customerSnap.exists()) customerSnap.getLong("transactionCount") ?: 0L else 0L
        val prevSpent = if (customerSnap.exists()) customerSnap.getLong("totalSpent") ?: 0L else 0L

        val notaNumber = "$cleanPrefix-$dateCode-${nextSeq.toString().padStart(4, '0')}"

        val statusHistory = listOf(
          StatusHistoryEntry(
            status = OrderStatus.RECEIVED.name,
            changedAtMillis = request.transactionAtMillis,
            changedByUid = uid,
            changedByName = actorName,
            note = "Pesanan dibuat"
          )
        )

        val paymentHistory = if (paidAmount > 0L) {
          listOf(
            PaymentRecord(
              id = "pay_${UUID.randomUUID().toString().take(8)}",
              amount = paidAmount,
              receivedAmount = receivedAmount,
              changeAmount = changeAmount,
              method = request.paymentMethod.name,
              type = paymentStatus.name,
              paidAtMillis = request.transactionAtMillis,
              recordedByUid = uid,
              recordedByName = actorName,
              note = if (request.paymentMethod == PaymentMethod.QRIS) {
                "Pencatatan manual QRIS saat transaksi"
              } else {
                "Pembayaran awal transaksi"
              }
            )
          )
        } else {
          emptyList()
        }

        val orderPayload = buildMap<String, Any> {
          put("shopId", shopId)
          put("notaNumber", notaNumber)
          put("customerId", customerId)
          put("customerName", request.customerName.trim())
          put("customerPhone", normalizedPhone)
          put("items", request.items.map { it.toMap() })
          put("subtotal", subtotal)
          put("discount", safeDiscount)
          put("extraFee", request.extraFee)
          if (request.extraFeeLabel.isNotBlank()) put("extraFeeLabel", request.extraFeeLabel.trim())
          if (taxAmount > 0L || request.taxRatePercent > 0.0) {
            put("taxRatePercent", request.taxRatePercent)
            put("taxAmount", taxAmount)
          }
          put("totalAmount", totalAmount)
          put("paidAmount", paidAmount)
          put("remainingAmount", remainingAmount)
          put("paymentStatus", paymentStatus.name)
          put("paymentMethod", request.paymentMethod.name)
          put("orderStatus", OrderStatus.RECEIVED.name)
          if (request.notes.isNotBlank()) put("notes", request.notes.trim())
          put("estimatedFinishAtMillis", request.estimatedFinishAtMillis)
          put("transactionAtMillis", request.transactionAtMillis)
          put("statusHistory", statusHistory.map { it.toMap() })
          put("paymentHistory", paymentHistory.map { it.toMap() })
          put("whatsappLogs", emptyList<Map<String, Any>>())
          put("cashierUid", uid)
          put("cashierName", actorName)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }

        if (counterSnap.exists()) {
          tx.update(
            counterRef,
            mapOf("seq" to nextSeq, "updatedAt" to FieldValue.serverTimestamp())
          )
        } else {
          tx.set(
            counterRef,
            mapOf("seq" to 1L, "updatedAt" to FieldValue.serverTimestamp())
          )
        }

        val customerPayload = buildMap<String, Any> {
          put("shopId", shopId)
          put("name", request.customerName.trim())
          put("phone", normalizedPhone)
          if (request.customerAddress.isNotBlank()) {
            put("address", request.customerAddress.trim())
          } else if (customerSnap.exists()) {
            val oldAddr = customerSnap.getString("address")
            if (!oldAddr.isNullOrBlank()) put("address", oldAddr)
          }
          put("lastTransactionAtMillis", request.transactionAtMillis)
          put("transactionCount", prevCount + 1L)
          put("totalSpent", prevSpent + totalAmount)
          put("createdAt", customerSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        tx.set(customerRef, customerPayload)
        tx.set(orderRef, orderPayload)
        tx.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "actorUid" to uid,
            "actorName" to actorName,
            "actorRole" to actorRole.name,
            "actionType" to "CREATE_ORDER",
            "targetId" to request.draftOrderId,
            "description" to "Transaksi baru $notaNumber a/n ${request.customerName.trim()} (${Formatters.formatRupiah(totalAmount)})",
            "createdAt" to FieldValue.serverTimestamp()
          )
        )

        LaundryOrder(
          id = request.draftOrderId,
          shopId = shopId,
          notaNumber = notaNumber,
          customerId = customerId,
          customerName = request.customerName.trim(),
          customerPhone = normalizedPhone,
          items = request.items.map { it.toMap() },
          subtotal = subtotal,
          discount = safeDiscount,
          extraFee = request.extraFee,
          extraFeeLabel = request.extraFeeLabel.trim(),
          taxRatePercent = request.taxRatePercent,
          taxAmount = taxAmount,
          totalAmount = totalAmount,
          paidAmount = paidAmount,
          remainingAmount = remainingAmount,
          paymentStatus = paymentStatus.name,
          paymentMethod = request.paymentMethod.name,
          orderStatus = OrderStatus.RECEIVED.name,
          notes = request.notes.trim(),
          estimatedFinishAtMillis = request.estimatedFinishAtMillis,
          transactionAtMillis = request.transactionAtMillis,
          statusHistory = statusHistory.map { it.toMap() },
          paymentHistory = paymentHistory.map { it.toMap() },
          whatsappLogs = emptyList(),
          cashierUid = uid,
          cashierName = actorName
        )
      }.await()

      Result.success(createdOrder)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, ordersCol.path)
      Result.failure(e)
    }
  }

  suspend fun updateOrderStatus(
    shopId: String,
    order: LaundryOrder,
    newStatus: OrderStatus,
    actorName: String,
    actorRole: UserRole,
    note: String = ""
  ): Result<Unit> {
    val uid = requireUserId()
    val orderRef = db.collection("shops").document(shopId).collection("orders").document(order.id)
    val logRef = db.collection("shops").document(shopId).collection("activityLogs").document()
    return try {
      require(!order.orderStatusEnum.isTerminal) {
        msg(
          "Pesanan dengan status ${order.orderStatusEnum.label} bersifat final dan tidak dapat diubah.",
          "Orders with status ${order.orderStatusEnum.localizedLabel(true)} are final and cannot be changed."
        )
      }
      require(newStatus != OrderStatus.CANCELLED) {
        msg(
          "Gunakan fitur pembatalan pesanan dengan alasan yang jelas.",
          "Use the order cancellation action with a clear reason."
        )
      }
      require(order.orderStatusEnum != newStatus) {
        msg(
          "Status pesanan sudah ${newStatus.label}.",
          "Order status is already ${newStatus.localizedLabel(true)}."
        )
      }
      require(newStatus.stepIndex > order.orderStatusEnum.stepIndex) {
        msg(
          "Status ${newStatus.label} sudah dilewati dan tidak dapat dipilih kembali.",
          "Status ${newStatus.localizedLabel(true)} has already been passed and cannot be selected again."
        )
      }

      val nowMillis = System.currentTimeMillis()
      val updatedHistory = order.parsedStatusHistory + StatusHistoryEntry(
        status = newStatus.name,
        changedAtMillis = nowMillis,
        changedByUid = uid,
        changedByName = actorName,
        note = note.trim()
      )

      val batch = db.batch()
      batch.update(
        orderRef,
        mapOf(
          "orderStatus" to newStatus.name,
          "statusHistory" to updatedHistory.map { it.toMap() },
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        logRef,
        mapOf(
          "shopId" to shopId,
          "actorUid" to uid,
          "actorName" to actorName,
          "actorRole" to actorRole.name,
          "actionType" to "UPDATE_STATUS",
          "targetId" to order.id,
          "description" to "Status ${order.notaNumber} diubah menjadi ${newStatus.label}",
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
      Result.failure(e)
    }
  }

  suspend fun cancelOrder(
    shopId: String,
    order: LaundryOrder,
    reason: String,
    actorName: String,
    actorRole: UserRole
  ): Result<Unit> {
    val uid = requireUserId()
    val orderRef = db.collection("shops").document(shopId).collection("orders").document(order.id)
    val logRef = db.collection("shops").document(shopId).collection("activityLogs").document()
    return try {
      require(!order.orderStatusEnum.isTerminal) {
        msg(
          "Pesanan ${order.orderStatusEnum.label} tidak dapat dibatalkan.",
          "Orders with status ${order.orderStatusEnum.localizedLabel(true)} cannot be cancelled."
        )
      }
      val cleanReason = reason.trim()
      require(cleanReason.length >= 3) {
        msg("Alasan pembatalan wajib diisi (minimal 3 karakter).", "Cancellation reason is required (min. 3 characters).")
      }

      val updatedHistory = order.parsedStatusHistory + StatusHistoryEntry(
        status = OrderStatus.CANCELLED.name,
        changedAtMillis = System.currentTimeMillis(),
        changedByUid = uid,
        changedByName = actorName,
        note = "Dibatalkan: $cleanReason"
      )

      val batch = db.batch()
      batch.update(
        orderRef,
        mapOf(
          "orderStatus" to OrderStatus.CANCELLED.name,
          "cancelReason" to cleanReason,
          "statusHistory" to updatedHistory.map { it.toMap() },
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        logRef,
        mapOf(
          "shopId" to shopId,
          "actorUid" to uid,
          "actorName" to actorName,
          "actorRole" to actorRole.name,
          "actionType" to "CANCEL_ORDER",
          "targetId" to order.id,
          "description" to "Pembatalan ${order.notaNumber}: $cleanReason",
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordPayment(
    shopId: String,
    orderId: String,
    paymentRequestId: String,
    receivedAmount: Long,
    method: PaymentMethod,
    isDpPayment: Boolean,
    actorName: String,
    actorRole: UserRole
  ): Result<PaymentRecord> {
    val uid = requireUserId()
    val orderRef = db.collection("shops").document(shopId).collection("orders").document(orderId)
    val logRef = db.collection("shops").document(shopId).collection("activityLogs").document()
    return try {
      require(receivedAmount > 0L) {
        msg("Nominal pembayaran harus lebih dari Rp0.", "Payment amount must be greater than Rp0.")
      }

      val record = db.runTransaction { tx ->
        val snap = tx.get(orderRef)
        val current = snap.toObject(
          LaundryOrder::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: throw IllegalStateException(msg("Pesanan tidak ditemukan.", "Order not found."))

        require(current.orderStatusEnum != OrderStatus.CANCELLED) {
          msg("Pesanan yang dibatalkan tidak dapat menerima pembayaran.", "Cancelled orders cannot accept payments.")
        }

        // Idempotency check to prevent duplicate payment on double-tap
        val existingPay = current.parsedPaymentHistory.firstOrNull { it.id == paymentRequestId }
        if (existingPay != null) {
          return@runTransaction existingPay
        }

        require(current.remainingAmount > 0L) {
          msg("Pesanan ini sudah lunas (sisa tagihan Rp0).", "This order is already paid (remaining Rp0).")
        }

        val appliedAmount: Long
        val changeAmount: Long

        if (isDpPayment) {
          require(receivedAmount <= current.remainingAmount) {
            msg(
              "Nominal DP tidak boleh melebihi sisa tagihan (${Formatters.formatRupiah(current.remainingAmount)}).",
              "Down payment cannot exceed remaining balance (${Formatters.formatRupiah(current.remainingAmount)})."
            )
          }
          appliedAmount = receivedAmount
          changeAmount = 0L
        } else {
          if (method == PaymentMethod.CASH) {
            require(receivedAmount >= current.remainingAmount) {
              msg(
                "Uang tunai kurang dari sisa tagihan (${Formatters.formatRupiah(current.remainingAmount)}).",
                "Cash is less than the remaining balance (${Formatters.formatRupiah(current.remainingAmount)})."
              )
            }
            appliedAmount = current.remainingAmount
            changeAmount = receivedAmount - current.remainingAmount
          } else {
            require(receivedAmount == current.remainingAmount) {
              msg(
                "Nominal ${method.label} untuk pelunasan harus sama dengan sisa tagihan (${Formatters.formatRupiah(current.remainingAmount)}).",
                "${method.label} settlement amount must match the remaining balance (${Formatters.formatRupiah(current.remainingAmount)})."
              )
            }
            appliedAmount = current.remainingAmount
            changeAmount = 0L
          }
        }

        val newPaid = (current.paidAmount + appliedAmount).coerceAtMost(current.totalAmount)
        val newRemaining = (current.totalAmount - newPaid).coerceAtLeast(0L)
        val newPayStatus = when {
          newRemaining == 0L -> PaymentStatus.PAID
          newPaid > 0L -> PaymentStatus.DP
          else -> PaymentStatus.UNPAID
        }

        val newRecord = PaymentRecord(
          id = paymentRequestId,
          amount = appliedAmount,
          receivedAmount = receivedAmount,
          changeAmount = changeAmount,
          method = method.name,
          type = newPayStatus.name,
          paidAtMillis = System.currentTimeMillis(),
          recordedByUid = uid,
          recordedByName = actorName,
          note = if (method == PaymentMethod.QRIS) {
            "Pencatatan manual QRIS"
          } else {
            "Pembayaran ${method.label}"
          }
        )

        val updatedPayments = current.parsedPaymentHistory + newRecord

        tx.update(
          orderRef,
          mapOf(
            "paidAmount" to newPaid,
            "remainingAmount" to newRemaining,
            "paymentStatus" to newPayStatus.name,
            "paymentMethod" to method.name,
            "paymentHistory" to updatedPayments.map { it.toMap() },
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        tx.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "actorUid" to uid,
            "actorName" to actorName,
            "actorRole" to actorRole.name,
            "actionType" to "RECORD_PAYMENT",
            "targetId" to orderId,
            "description" to "Pembayaran ${current.notaNumber} sebesar ${Formatters.formatRupiah(appliedAmount)} (${method.label})",
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        newRecord
      }.await()

      Result.success(record)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
      Result.failure(e)
    }
  }

  suspend fun logWhatsAppOpened(
    shopId: String,
    order: LaundryOrder,
    normalizedPhone: String,
    actorName: String,
    actorRole: UserRole
  ): Result<Unit> {
    val uid = requireUserId()
    val orderRef = db.collection("shops").document(shopId).collection("orders").document(order.id)
    val logRef = db.collection("shops").document(shopId).collection("activityLogs").document()
    return try {
      val entry = WhatsAppLogEntry(
        openedAtMillis = System.currentTimeMillis(),
        openedByUid = uid,
        openedByName = actorName,
        targetPhone = normalizedPhone,
        note = "Tautan WhatsApp dibuka oleh $actorName (menunggu pengiriman di WhatsApp)"
      )
      val updatedLogs = order.parsedWhatsAppLogs + entry
      val batch = db.batch()
      batch.update(
        orderRef,
        mapOf(
          "whatsappLogs" to updatedLogs.map { it.toMap() },
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        logRef,
        mapOf(
          "shopId" to shopId,
          "actorUid" to uid,
          "actorName" to actorName,
          "actorRole" to actorRole.name,
          "actionType" to "WHATSAPP_OPENED",
          "targetId" to order.id,
          "description" to "Membuka tautan WhatsApp untuk nota ${order.notaNumber} ke $normalizedPhone",
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveService(
    shopId: String,
    service: LaundryService,
    actorName: String
  ): Result<Unit> {
    val uid = requireUserId()
    val col = db.collection("shops").document(shopId).collection("services")
    return try {
      require(service.name.trim().length >= 2) {
        msg("Nama layanan minimal 2 karakter.", "Service name must be at least 2 characters.")
      }
      require(service.price >= 0L) {
        msg("Harga layanan tidak boleh negatif.", "Service price cannot be negative.")
      }
      val docRef = if (service.id.isBlank()) col.document() else col.document(service.id)
      val category = ServiceCategory.fromString(service.category)
      val payload = buildMap<String, Any> {
        put("shopId", shopId)
        put("name", service.name.trim())
        put("category", category.name)
        put("unitLabel", category.unitLabel)
        put("price", service.price)
        put("isExtraFee", service.isExtraFee)
        put("active", service.active)
        if (service.description.isNotBlank()) put("description", service.description.trim())
        put("createdAt", service.createdAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      val batch = db.batch()
      batch.set(docRef, payload)
      batch.set(
        db.collection("shops").document(shopId).collection("activityLogs").document(),
        mapOf(
          "shopId" to shopId,
          "actorUid" to uid,
          "actorName" to actorName,
          "actorRole" to UserRole.OWNER.name,
          "actionType" to "UPDATE_PRICE",
          "targetId" to docRef.id,
          "description" to "Menyimpan layanan '${service.name.trim()}' (${Formatters.formatRupiah(service.price)}/${category.unitLabel})",
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, col.path)
      Result.failure(e)
    }
  }

  suspend fun deleteService(shopId: String, serviceId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("shops").document(shopId).collection("services").document(serviceId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveCustomer(shopId: String, customer: Customer): Result<Unit> {
    requireUserId()
    val col = db.collection("shops").document(shopId).collection("customers")
    return try {
      require(customer.name.trim().length >= 2) {
        msg("Nama pelanggan minimal 2 karakter.", "Customer name must be at least 2 characters.")
      }
      val normalizedPhone = WhatsAppHelper.normalizeIndonesianPhone(customer.phone)
        ?: throw IllegalArgumentException(
          msg("Nomor WhatsApp pelanggan tidak valid.", "Invalid customer WhatsApp number.")
        )
      val docId = customer.id.ifBlank { "cust_$normalizedPhone" }
      val docRef = col.document(docId)
      val existing = docRef.get().await()
      val payload = buildMap<String, Any> {
        put("shopId", shopId)
        put("name", customer.name.trim())
        put("phone", normalizedPhone)
        if (customer.address.isNotBlank()) put("address", customer.address.trim())
        put(
          "lastTransactionAtMillis",
          customer.lastTransactionAtMillis.takeIf { it > 0L }
            ?: existing.getLong("lastTransactionAtMillis")
            ?: 0L
        )
        put(
          "transactionCount",
          customer.transactionCount.takeIf { it > 0L }
            ?: existing.getLong("transactionCount")
            ?: 0L
        )
        put(
          "totalSpent",
          customer.totalSpent.takeIf { it > 0L }
            ?: existing.getLong("totalSpent")
            ?: 0L
        )
        put("createdAt", existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, col.path)
      Result.failure(e)
    }
  }

  suspend fun saveExpense(
    shopId: String,
    expense: Expense,
    actorName: String
  ): Result<Unit> {
    val uid = requireUserId()
    val col = db.collection("shops").document(shopId).collection("expenses")
    return try {
      require(expense.category.trim().length >= 2) {
        msg("Kategori pengeluaran wajib dipilih.", "Expense category is required.")
      }
      require(expense.amount > 0L) {
        msg("Nominal pengeluaran harus lebih dari Rp0.", "Expense amount must be greater than Rp0.")
      }
      val docRef = if (expense.id.isBlank()) col.document() else col.document(expense.id)
      val payload = buildMap<String, Any> {
        put("shopId", shopId)
        put("category", expense.category.trim())
        put("amount", expense.amount)
        put("dateMillis", expense.dateMillis.takeIf { it > 0L } ?: System.currentTimeMillis())
        if (expense.notes.isNotBlank()) put("notes", expense.notes.trim())
        put("createdByUid", expense.createdByUid.ifBlank { uid })
        put("createdAt", expense.createdAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      val batch = db.batch()
      batch.set(docRef, payload)
      batch.set(
        db.collection("shops").document(shopId).collection("activityLogs").document(),
        mapOf(
          "shopId" to shopId,
          "actorUid" to uid,
          "actorName" to actorName,
          "actorRole" to UserRole.OWNER.name,
          "actionType" to "EXPENSE_CREATE",
          "targetId" to docRef.id,
          "description" to "Pengeluaran ${expense.category.trim()}: ${Formatters.formatRupiah(expense.amount)}",
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, col.path)
      Result.failure(e)
    }
  }

  suspend fun deleteExpense(shopId: String, expenseId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("shops").document(shopId).collection("expenses").document(expenseId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addExpenseCategory(shopId: String, categoryName: String): Result<Unit> {
    requireUserId()
    val col = db.collection("shops").document(shopId).collection("expenseCategories")
    return try {
      val clean = categoryName.trim()
      require(clean.length >= 2) {
        msg("Nama kategori minimal 2 karakter.", "Category name must be at least 2 characters.")
      }
      col.document().set(
        mapOf(
          "shopId" to shopId,
          "name" to clean,
          "isDefault" to false,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, col.path)
      Result.failure(e)
    }
  }

  suspend fun saveStaffInvite(
    shopId: String,
    invite: StaffInvite,
    actorName: String
  ): Result<Unit> {
    val uid = requireUserId()
    val dirCol = db.collection("staffDirectory")
    return try {
      val cleanName = invite.name.trim()
      val cleanEmail = invite.email.trim().lowercase(Locale.ROOT)
      val cleanRole = if (invite.role == "OWNER") "OWNER" else "KASIR"
      require(cleanName.length >= 2) {
        msg("Nama pengguna minimal 2 karakter.", "User name must be at least 2 characters.")
      }
      require(cleanEmail.contains("@") && cleanEmail.length >= 5) {
        msg("Email Google tidak valid.", "Invalid Google email.")
      }

      val isCreate = invite.id.isBlank()
      if (cleanRole == "OWNER") {
        val existingOwnerInvites = dirCol
          .whereEqualTo("ownerUid", uid)
          .whereEqualTo("role", "OWNER")
          .get()
          .await()
        val otherCount = existingOwnerInvites.documents.count { it.id != invite.id }
        require(otherCount < 2) {
          msg(
            "Maksimal 2 akun Owner tambahan yang dapat disambungkan untuk pemantauan jarak jauh.",
            "Maximum 2 additional Owner accounts can be connected for remote monitoring."
          )
        }
      }

      val finalPerms = if (cleanRole == "OWNER") {
        StaffPermissions.ownerAll().toMap()
      } else {
        invite.permissions
      }
      val inviteRef = if (isCreate) dirCol.document() else dirCol.document(invite.id)
      val batch = db.batch()

      val payload = mapOf(
        "shopId" to shopId,
        "ownerUid" to uid,
        "name" to cleanName,
        "email" to cleanEmail,
        "staffUid" to if (isCreate) "" else invite.staffUid,
        "role" to cleanRole,
        "active" to invite.active,
        "permissions" to finalPerms,
        "createdAt" to (invite.createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      batch.set(inviteRef, payload)

      if (!isCreate && invite.staffUid.isNotBlank()) {
        val memberRef = db.collection("shops").document(shopId)
          .collection("staffMembers").document(invite.staffUid)
        val memberSnap = memberRef.get().await()
        if (memberSnap.exists()) {
          batch.update(
            memberRef,
            mapOf(
              "name" to cleanName,
              "email" to cleanEmail,
              "role" to cleanRole,
              "active" to invite.active,
              "permissions" to finalPerms,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }

      batch.set(
        db.collection("shops").document(shopId).collection("activityLogs").document(),
        mapOf(
          "shopId" to shopId,
          "actorUid" to uid,
          "actorName" to actorName,
          "actorRole" to UserRole.OWNER.name,
          "actionType" to "STAFF_UPDATE",
          "targetId" to inviteRef.id,
          "description" to "${if (isCreate) "Menambah" else "Memperbarui"} akun $cleanRole $cleanName ($cleanEmail) - ${if (invite.active) "Aktif" else "Nonaktif"}",
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, dirCol.path)
      Result.failure(e)
    }
  }

  suspend fun deleteStaffInvite(shopId: String, invite: StaffInvite): Result<Unit> {
    requireUserId()
    val inviteRef = db.collection("staffDirectory").document(invite.id)
    return try {
      val batch = db.batch()
      batch.delete(inviteRef)
      if (invite.staffUid.isNotBlank()) {
        val memberRef = db.collection("shops").document(shopId)
          .collection("staffMembers").document(invite.staffUid)
        batch.delete(memberRef)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, inviteRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateShopProfile(shopId: String, profile: ShopProfile): Result<Unit> {
    requireUserId()
    val shopRef = db.collection("shops").document(shopId)
    return try {
      require(profile.businessName.trim().length >= 2) {
        msg("Nama usaha minimal 2 karakter.", "Shop name must be at least 2 characters.")
      }
      require(profile.notaPrefix.trim().isNotEmpty()) {
        msg("Prefix nomor nota wajib diisi.", "Receipt prefix is required.")
      }
      shopRef.set(profile.toUpdateMap()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, shopRef.path)
      Result.failure(e)
    }
  }

  suspend fun exportShopBackupJson(shopId: String): Result<String> {
    requireUserId()
    return try {
      val shopSnap = db.collection("shops").document(shopId).get().await()
      val servicesSnap = db.collection("shops").document(shopId).collection("services").get().await()
      val customersSnap = db.collection("shops").document(shopId).collection("customers").get().await()
      val ordersSnap = db.collection("shops").document(shopId).collection("orders").get().await()
      val expensesSnap = db.collection("shops").document(shopId).collection("expenses").get().await()

      val root = JSONObject().apply {
        put("exportedAtMillis", System.currentTimeMillis())
        put("shopId", shopId)
        put("shopProfile", JSONObject(shopSnap.data ?: emptyMap<String, Any>()))
        put("services", JSONArray(servicesSnap.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()).put("id", it.id) }))
        put("customers", JSONArray(customersSnap.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()).put("id", it.id) }))
        put("orders", JSONArray(ordersSnap.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()).put("id", it.id) }))
        put("expenses", JSONArray(expensesSnap.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()).put("id", it.id) }))
      }
      Result.success(root.toString(2))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "shops/$shopId")
      Result.failure(e)
    }
  }
}
