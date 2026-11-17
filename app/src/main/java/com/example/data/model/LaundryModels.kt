package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class UserRole(val label: String) {
  OWNER("Pemilik (Owner)"),
  KASIR("Kasir");

  fun localizedLabel(isEn: Boolean): String = when (this) {
    OWNER -> if (isEn) "Owner" else label
    KASIR -> if (isEn) "Cashier" else label
  }
}

@IgnoreExtraProperties
data class StaffPermissions(
  val canCreateOrder: Boolean = true,
  val canUpdateStatus: Boolean = true,
  val canRecordPayment: Boolean = true,
  val canManageCustomers: Boolean = true,
  val canSendWhatsApp: Boolean = true,
  val canCancelOrder: Boolean = false,
  val canViewReports: Boolean = false
) {
  fun toMap(): Map<String, Any> = mapOf(
    "canCreateOrder" to canCreateOrder,
    "canUpdateStatus" to canUpdateStatus,
    "canRecordPayment" to canRecordPayment,
    "canManageCustomers" to canManageCustomers,
    "canSendWhatsApp" to canSendWhatsApp,
    "canCancelOrder" to canCancelOrder,
    "canViewReports" to canViewReports
  )

  companion object {
    fun ownerAll() = StaffPermissions(
      canCreateOrder = true,
      canUpdateStatus = true,
      canRecordPayment = true,
      canManageCustomers = true,
      canSendWhatsApp = true,
      canCancelOrder = true,
      canViewReports = true
    )

    fun fromMap(map: Map<String, Any?>?): StaffPermissions {
      if (map == null) return StaffPermissions()
      return StaffPermissions(
        canCreateOrder = map["canCreateOrder"] as? Boolean ?: true,
        canUpdateStatus = map["canUpdateStatus"] as? Boolean ?: true,
        canRecordPayment = map["canRecordPayment"] as? Boolean ?: true,
        canManageCustomers = map["canManageCustomers"] as? Boolean ?: true,
        canSendWhatsApp = map["canSendWhatsApp"] as? Boolean ?: true,
        canCancelOrder = map["canCancelOrder"] as? Boolean ?: false,
        canViewReports = map["canViewReports"] as? Boolean ?: false
      )
    }
  }
}

@IgnoreExtraProperties
data class ShopProfile(
  @DocumentId val id: String = "",
  val ownerUid: String = "",
  val ownerEmail: String = "",
  val businessName: String = "ADA LAUNDRY",
  val address: String = "",
  val phone: String = "",
  val operatingHours: String = "07:00 - 21:00",
  val notaPrefix: String = "ADA",
  val logoUri: String = "",
  val printerName: String = "",
  val businessIconKey: String = "WASHING_MACHINE",
  val brandColorKey: String = "BLUE",
  val taxEnabled: Boolean = false,
  val taxAutoApply: Boolean = true,
  val taxRatePercent: Double = 11.0,
  val defaultDiscountPercent: Double = 0.0,
  val defaultDiscountAmount: Long = 0L,
  val receiptShowLogo: Boolean = true,
  val receiptShowName: Boolean = true,
  val receiptShowAddress: Boolean = true,
  val receiptShowPhone: Boolean = true,
  val receiptShowCustomer: Boolean = true,
  val receiptFooterMessage: String = "Terima kasih telah mempercayakan cucian Anda.",
  val paperSize: String = "MM_58",
  val ownerPin: String = "",
  val whatsappTemplate: String = "",
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
  @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> = buildMap {
    put("ownerUid", ownerUid)
    put("ownerEmail", ownerEmail)
    put("businessName", businessName)
    put("address", address)
    put("phone", phone)
    put("operatingHours", operatingHours)
    put("notaPrefix", notaPrefix)
    if (logoUri.isNotBlank()) put("logoUri", logoUri)
    if (printerName.isNotBlank()) put("printerName", printerName)
    put("businessIconKey", businessIconKey)
    put("brandColorKey", brandColorKey)
    put("taxEnabled", taxEnabled)
    put("taxAutoApply", taxAutoApply)
    put("taxRatePercent", taxRatePercent)
    put("defaultDiscountPercent", defaultDiscountPercent)
    put("defaultDiscountAmount", defaultDiscountAmount)
    put("receiptShowLogo", receiptShowLogo)
    put("receiptShowName", receiptShowName)
    put("receiptShowAddress", receiptShowAddress)
    put("receiptShowPhone", receiptShowPhone)
    put("receiptShowCustomer", receiptShowCustomer)
    put("receiptFooterMessage", receiptFooterMessage)
    put("paperSize", paperSize)
    if (ownerPin.isNotBlank()) put("ownerPin", ownerPin)
    if (whatsappTemplate.isNotBlank()) put("whatsappTemplate", whatsappTemplate)
    put("createdAt", FieldValue.serverTimestamp())
    put("updatedAt", FieldValue.serverTimestamp())
  }

  fun toUpdateMap(): Map<String, Any> = buildMap {
    put("ownerUid", ownerUid)
    put("ownerEmail", ownerEmail)
    put("businessName", businessName)
    put("address", address)
    put("phone", phone)
    put("operatingHours", operatingHours)
    put("notaPrefix", notaPrefix)
    if (logoUri.isNotBlank()) put("logoUri", logoUri)
    if (printerName.isNotBlank()) put("printerName", printerName)
    put("businessIconKey", businessIconKey)
    put("brandColorKey", brandColorKey)
    put("taxEnabled", taxEnabled)
    put("taxAutoApply", taxAutoApply)
    put("taxRatePercent", taxRatePercent)
    put("defaultDiscountPercent", defaultDiscountPercent)
    put("defaultDiscountAmount", defaultDiscountAmount)
    put("receiptShowLogo", receiptShowLogo)
    put("receiptShowName", receiptShowName)
    put("receiptShowAddress", receiptShowAddress)
    put("receiptShowPhone", receiptShowPhone)
    put("receiptShowCustomer", receiptShowCustomer)
    put("receiptFooterMessage", receiptFooterMessage)
    put("paperSize", paperSize)
    put("ownerPin", ownerPin)
    put("whatsappTemplate", whatsappTemplate)
    put("createdAt", createdAt ?: FieldValue.serverTimestamp())
    put("updatedAt", FieldValue.serverTimestamp())
  }
}

@IgnoreExtraProperties
data class StaffInvite(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val ownerUid: String = "",
  val name: String = "",
  val email: String = "",
  val staffUid: String = "",
  val role: String = "KASIR",
  val active: Boolean = true,
  val permissions: Map<String, Any> = StaffPermissions().toMap(),
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
  @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
  val parsedPermissions: StaffPermissions
    get() = StaffPermissions.fromMap(permissions)

  val isRemoteOwner: Boolean
    get() = role == "OWNER"
}

@IgnoreExtraProperties
data class StaffMember(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val inviteId: String = "",
  val name: String = "",
  val email: String = "",
  val role: String = "KASIR",
  val active: Boolean = true,
  val permissions: Map<String, Any> = StaffPermissions().toMap(),
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
  @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
  val parsedPermissions: StaffPermissions
    get() = StaffPermissions.fromMap(permissions)
}

enum class ServiceCategory(val label: String, val unitLabel: String) {
  KILOAN("Laundry Kiloan", "kg"),
  SATUAN("Laundry Satuan", "pcs"),
  KARPET("Laundry Karpet", "lembar");

  fun localizedLabel(isEn: Boolean): String = when (this) {
    KILOAN -> if (isEn) "Weight Laundry (Kg)" else label
    SATUAN -> if (isEn) "Per-Item Laundry" else label
    KARPET -> if (isEn) "Carpet Laundry" else label
  }

  fun localizedUnitLabel(isEn: Boolean): String = when (this) {
    KARPET -> if (isEn) "sheet" else unitLabel
    else -> unitLabel
  }

  companion object {
    fun fromString(value: String): ServiceCategory =
      entries.firstOrNull { it.name == value } ?: KILOAN
  }
}

@IgnoreExtraProperties
data class LaundryService(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val name: String = "",
  val category: String = ServiceCategory.KILOAN.name,
  val unitLabel: String = "kg",
  val price: Long = 0L,
  val isExtraFee: Boolean = false,
  val active: Boolean = true,
  val description: String = "",
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
  @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
  val categoryEnum: ServiceCategory
    get() = ServiceCategory.fromString(category)
}

@IgnoreExtraProperties
data class Customer(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val name: String = "",
  val phone: String = "",
  val address: String = "",
  val lastTransactionAtMillis: Long = 0L,
  val transactionCount: Long = 0L,
  val totalSpent: Long = 0L,
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
  @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

enum class OrderStatus(val label: String, val stepIndex: Int) {
  RECEIVED("DITERIMA", 0),
  PROCESSING("DIPROSES", 1),
  READY("SIAP DIAMBIL", 2),
  COMPLETED("SUDAH DIAMBIL", 3),
  CANCELLED("DIBATALKAN", -1);

  val isTerminal: Boolean
    get() = this == COMPLETED || this == CANCELLED

  fun localizedLabel(isEn: Boolean): String = when (this) {
    RECEIVED -> if (isEn) "RECEIVED" else label
    PROCESSING -> if (isEn) "PROCESSING" else label
    READY -> if (isEn) "READY FOR PICKUP" else label
    COMPLETED -> if (isEn) "PICKED UP" else label
    CANCELLED -> if (isEn) "CANCELLED" else label
  }

  fun localizedTitleCase(isEn: Boolean): String = when (this) {
    RECEIVED -> if (isEn) "Received" else "Diterima"
    PROCESSING -> if (isEn) "Processing" else "Diproses"
    READY -> if (isEn) "Ready for Pickup" else "Siap Diambil"
    COMPLETED -> if (isEn) "Picked Up" else "Sudah Diambil"
    CANCELLED -> if (isEn) "Cancelled" else "Dibatalkan"
  }

  companion object {
    fun fromString(value: String): OrderStatus =
      entries.firstOrNull { it.name == value } ?: RECEIVED
  }
}

enum class PaymentStatus(val label: String) {
  UNPAID("Belum Bayar"),
  DP("Uang Muka / DP"),
  PAID("Lunas");

  fun localizedLabel(isEn: Boolean): String = when (this) {
    UNPAID -> if (isEn) "Unpaid" else label
    DP -> if (isEn) "Deposit / DP" else label
    PAID -> if (isEn) "Paid" else label
  }

  companion object {
    fun fromString(value: String): PaymentStatus =
      entries.firstOrNull { it.name == value } ?: UNPAID
  }
}

enum class PaymentMethod(val label: String, val note: String) {
  CASH("Tunai", "Pembayaran tunai di kasir"),
  TRANSFER("Transfer Bank", "Pencatatan transfer bank"),
  QRIS("QRIS", "Pencatatan manual QRIS (periksa mutasi sebelum konfirmasi lunas)");

  fun localizedLabel(isEn: Boolean): String = when (this) {
    CASH -> if (isEn) "Cash" else label
    TRANSFER -> if (isEn) "Bank Transfer" else label
    QRIS -> "QRIS"
  }

  fun localizedNote(isEn: Boolean): String = when (this) {
    CASH -> if (isEn) "Cash payment at cashier" else note
    TRANSFER -> if (isEn) "Manual bank transfer record" else note
    QRIS -> if (isEn) "Manual QRIS record (verify settlement before marking paid)" else note
  }

  companion object {
    fun fromString(value: String): PaymentMethod =
      entries.firstOrNull { it.name == value } ?: CASH
  }
}

data class OrderItem(
  val serviceId: String = "",
  val serviceName: String = "",
  val category: String = ServiceCategory.KILOAN.name,
  val unitLabel: String = "kg",
  val quantity: Double = 1.0,
  val unitPrice: Long = 0L,
  val lineTotal: Long = 0L
) {
  fun toMap(): Map<String, Any> = mapOf(
    "serviceId" to serviceId,
    "serviceName" to serviceName,
    "category" to category,
    "unitLabel" to unitLabel,
    "quantity" to quantity,
    "unitPrice" to unitPrice,
    "lineTotal" to lineTotal
  )

  companion object {
    fun fromMap(map: Map<String, Any?>): OrderItem {
      return OrderItem(
        serviceId = map["serviceId"] as? String ?: "",
        serviceName = map["serviceName"] as? String ?: "",
        category = map["category"] as? String ?: ServiceCategory.KILOAN.name,
        unitLabel = map["unitLabel"] as? String ?: "kg",
        quantity = (map["quantity"] as? Number)?.toDouble() ?: 0.0,
        unitPrice = (map["unitPrice"] as? Number)?.toLong() ?: 0L,
        lineTotal = (map["lineTotal"] as? Number)?.toLong() ?: 0L
      )
    }
  }
}

data class StatusHistoryEntry(
  val status: String = OrderStatus.RECEIVED.name,
  val changedAtMillis: Long = 0L,
  val changedByUid: String = "",
  val changedByName: String = "",
  val note: String = ""
) {
  fun localizedNote(isEn: Boolean): String {
    if (!isEn) return note
    return when {
      note == "Pesanan dibuat" -> "Order created"
      note.startsWith("Dibatalkan: ") -> "Cancelled: ${note.removePrefix("Dibatalkan: ")}"
      else -> note
    }
  }

  fun toMap(): Map<String, Any> = buildMap {
    put("status", status)
    put("changedAtMillis", changedAtMillis)
    put("changedByUid", changedByUid)
    put("changedByName", changedByName)
    if (note.isNotBlank()) put("note", note)
  }

  companion object {
    fun fromMap(map: Map<String, Any?>): StatusHistoryEntry = StatusHistoryEntry(
      status = map["status"] as? String ?: OrderStatus.RECEIVED.name,
      changedAtMillis = (map["changedAtMillis"] as? Number)?.toLong() ?: 0L,
      changedByUid = map["changedByUid"] as? String ?: "",
      changedByName = map["changedByName"] as? String ?: "",
      note = map["note"] as? String ?: ""
    )
  }
}

data class PaymentRecord(
  val id: String = "",
  val amount: Long = 0L,
  val receivedAmount: Long = 0L,
  val changeAmount: Long = 0L,
  val method: String = PaymentMethod.CASH.name,
  val type: String = PaymentStatus.PAID.name,
  val paidAtMillis: Long = 0L,
  val recordedByUid: String = "",
  val recordedByName: String = "",
  val note: String = ""
) {
  fun toMap(): Map<String, Any> = buildMap {
    put("id", id)
    put("amount", amount)
    put("receivedAmount", receivedAmount)
    put("changeAmount", changeAmount)
    put("method", method)
    put("type", type)
    put("paidAtMillis", paidAtMillis)
    put("recordedByUid", recordedByUid)
    put("recordedByName", recordedByName)
    if (note.isNotBlank()) put("note", note)
  }

  companion object {
    fun fromMap(map: Map<String, Any?>): PaymentRecord = PaymentRecord(
      id = map["id"] as? String ?: "",
      amount = (map["amount"] as? Number)?.toLong() ?: 0L,
      receivedAmount = (map["receivedAmount"] as? Number)?.toLong() ?: 0L,
      changeAmount = (map["changeAmount"] as? Number)?.toLong() ?: 0L,
      method = map["method"] as? String ?: PaymentMethod.CASH.name,
      type = map["type"] as? String ?: PaymentStatus.PAID.name,
      paidAtMillis = (map["paidAtMillis"] as? Number)?.toLong() ?: 0L,
      recordedByUid = map["recordedByUid"] as? String ?: "",
      recordedByName = map["recordedByName"] as? String ?: "",
      note = map["note"] as? String ?: ""
    )
  }
}

data class WhatsAppLogEntry(
  val openedAtMillis: Long = 0L,
  val openedByUid: String = "",
  val openedByName: String = "",
  val targetPhone: String = "",
  val note: String = "Tautan WhatsApp dibuka (menunggu kasir menekan KIRIM di WhatsApp)"
) {
  fun localizedNote(isEn: Boolean): String {
    if (!isEn) return note
    return if (note.contains("Tautan WhatsApp dibuka")) {
      "WhatsApp link opened (waiting for cashier to tap SEND in WhatsApp)"
    } else note
  }
  fun toMap(): Map<String, Any> = mapOf(
    "openedAtMillis" to openedAtMillis,
    "openedByUid" to openedByUid,
    "openedByName" to openedByName,
    "targetPhone" to targetPhone,
    "note" to note
  )

  companion object {
    fun fromMap(map: Map<String, Any?>): WhatsAppLogEntry = WhatsAppLogEntry(
      openedAtMillis = (map["openedAtMillis"] as? Number)?.toLong() ?: 0L,
      openedByUid = map["openedByUid"] as? String ?: "",
      openedByName = map["openedByName"] as? String ?: "",
      targetPhone = map["targetPhone"] as? String ?: "",
      note = map["note"] as? String ?: ""
    )
  }
}

@IgnoreExtraProperties
data class LaundryOrder(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val notaNumber: String = "",
  val customerId: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val items: List<Map<String, Any?>> = emptyList(),
  val subtotal: Long = 0L,
  val discount: Long = 0L,
  val extraFee: Long = 0L,
  val extraFeeLabel: String = "",
  val taxRatePercent: Double = 0.0,
  val taxAmount: Long = 0L,
  val totalAmount: Long = 0L,
  val paidAmount: Long = 0L,
  val remainingAmount: Long = 0L,
  val paymentStatus: String = PaymentStatus.UNPAID.name,
  val paymentMethod: String = PaymentMethod.CASH.name,
  val orderStatus: String = OrderStatus.RECEIVED.name,
  val notes: String = "",
  val estimatedFinishAtMillis: Long = 0L,
  val transactionAtMillis: Long = 0L,
  val statusHistory: List<Map<String, Any?>> = emptyList(),
  val paymentHistory: List<Map<String, Any?>> = emptyList(),
  val whatsappLogs: List<Map<String, Any?>> = emptyList(),
  val cashierUid: String = "",
  val cashierName: String = "",
  val cancelReason: String = "",
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
  @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
  val parsedItems: List<OrderItem>
    get() = items.map { OrderItem.fromMap(it) }

  val parsedStatusHistory: List<StatusHistoryEntry>
    get() = statusHistory.map { StatusHistoryEntry.fromMap(it) }

  val parsedPaymentHistory: List<PaymentRecord>
    get() = paymentHistory.map { PaymentRecord.fromMap(it) }

  val parsedWhatsAppLogs: List<WhatsAppLogEntry>
    get() = whatsappLogs.map { WhatsAppLogEntry.fromMap(it) }

  val orderStatusEnum: OrderStatus
    get() = OrderStatus.fromString(orderStatus)

  val paymentStatusEnum: PaymentStatus
    get() = PaymentStatus.fromString(paymentStatus)

  val paymentMethodEnum: PaymentMethod
    get() = PaymentMethod.fromString(paymentMethod)
}

@IgnoreExtraProperties
data class Expense(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val category: String = "",
  val amount: Long = 0L,
  val dateMillis: Long = 0L,
  val notes: String = "",
  val createdByUid: String = "",
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
  @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpenseCategory(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val name: String = "",
  val isDefault: Boolean = false,
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityLog(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val actorUid: String = "",
  val actorName: String = "",
  val actorRole: String = "OWNER",
  val actionType: String = "",
  val targetId: String = "",
  val description: String = "",
  @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null
) {
  fun localizedDescription(isEn: Boolean): String {
    if (!isEn || description.isBlank()) return description
    var text = description
    text = when {
      text.startsWith("Transaksi baru ") ->
        text.replaceFirst("Transaksi baru ", "New transaction ").replace(" a/n ", " for ")
      text.startsWith("Status ") && text.contains(" diubah menjadi ") -> {
        val updated = text.replace(" diubah menjadi ", " changed to ")
        OrderStatus.entries.fold(updated) { acc, st ->
          acc.replace(st.label, st.localizedLabel(true))
        }
      }
      text.startsWith("Pembatalan ") ->
        text.replaceFirst("Pembatalan ", "Cancelled ")
      text.startsWith("Pembayaran ") ->
        text.replaceFirst("Pembayaran ", "Payment ").replace(" sebesar ", " of ")
      text.startsWith("Membuka tautan WhatsApp untuk nota ") ->
        text.replaceFirst("Membuka tautan WhatsApp untuk nota ", "Opened WhatsApp link for receipt ")
          .replace(" ke ", " to ")
      text.startsWith("Menyimpan layanan ") ->
        text.replaceFirst("Menyimpan layanan ", "Saved service ")
      text.startsWith("Pengeluaran ") ->
        text.replaceFirst("Pengeluaran ", "Expense ")
      else -> text
    }
    return text
  }
}

enum class ReportPeriod(val label: String) {
  DAILY("Harian"),
  WEEKLY("Mingguan"),
  MONTHLY("Bulanan"),
  YEARLY("Tahunan");

  fun localizedLabel(isEn: Boolean): String = when (this) {
    DAILY -> if (isEn) "Daily" else label
    WEEKLY -> if (isEn) "Weekly" else label
    MONTHLY -> if (isEn) "Monthly" else label
    YEARLY -> if (isEn) "Yearly" else label
  }
}

data class ServiceUsageStat(
  val serviceName: String,
  val category: ServiceCategory,
  val orderCount: Int,
  val totalQuantity: Double,
  val unitLabel: String,
  val totalRevenue: Long
)

data class ReportSummary(
  val totalOmzet: Long = 0L,
  val totalTransactions: Int = 0,
  val receivedOrders: Int = 0,
  val processingOrders: Int = 0,
  val readyOrders: Int = 0,
  val completedOrders: Int = 0,
  val cancelledOrders: Int = 0,
  val totalCashPaid: Long = 0L,
  val totalTransferPaid: Long = 0L,
  val totalQrisPaid: Long = 0L,
  val totalPaidIncome: Long = 0L,
  val totalLunasAmount: Long = 0L,
  val totalDpAmount: Long = 0L,
  val totalUnpaidReceivables: Long = 0L,
  val totalExpenses: Long = 0L,
  val estimatedNetProfit: Long = 0L,
  val kiloanRevenue: Long = 0L,
  val kiloanCount: Int = 0,
  val satuanRevenue: Long = 0L,
  val satuanCount: Int = 0,
  val karpetRevenue: Long = 0L,
  val karpetCount: Int = 0,
  val topServices: List<ServiceUsageStat> = emptyList()
)

data class ActiveShopSession(
  val shopId: String,
  val userUid: String,
  val userName: String,
  val userEmail: String,
  val role: UserRole,
  val permissions: StaffPermissions,
  val shopProfile: ShopProfile
)
