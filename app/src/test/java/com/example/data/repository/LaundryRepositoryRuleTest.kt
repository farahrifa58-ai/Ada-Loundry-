package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Expense
import com.example.data.model.LaundryService
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LaundryRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ownerCreateShopAndMultiCategoryOrders_succeedsAndPreventsDuplicates() = runBlocking {
    val ownerEmail = "owner_${UUID.randomUUID().toString().take(6)}@adalaundry.test"
    val ownerUid = signInTestUser(ownerEmail)
    val authRepo = AuthSessionRepository(firestore, auth)
    val repo = LaundryRepository(firestore, auth)

    val shopRes = withTimeout(TIMEOUT_MS) {
      authRepo.createOwnerShopWithCatalog(
        businessName = "ADA LAUNDRY PUSAT",
        address = "Jl. Sudirman 12",
        phone = "081234567890",
        operatingHours = "07:00 - 21:00",
        notaPrefix = "ADA"
      )
    }
    assertTrue("Shop setup should succeed: ${shopRes.exceptionOrNull()}", shopRes.isSuccess)

    // Create combined order: Kiloan + Satuan + Karpet per lembar, discount, extra fee, DP
    val draftId = UUID.randomUUID().toString()
    val items = listOf(
      OrderItem("svc_1", "Cuci Komplit Reguler", ServiceCategory.KILOAN.name, "kg", 4.0, 7000L, 28000L),
      OrderItem("svc_4", "Kemeja", ServiceCategory.SATUAN.name, "pcs", 2.0, 8000L, 16000L),
      OrderItem("svc_9", "Karpet Standar", ServiceCategory.KARPET.name, "lembar", 2.0, 25000L, 50000L)
    )
    // Subtotal = 94,000; discount = 4,000; extra = 10,000 => total = 100,000; DP = 40,000 => rem = 60,000
    val req = NewOrderRequest(
      draftOrderId = draftId,
      customerId = null,
      customerName = "Ibu Sari",
      customerPhone = "08123456789",
      customerAddress = "Jl. Melati No. 5",
      items = items,
      discount = 4000L,
      extraFee = 10000L,
      extraFeeLabel = "Express",
      initialPaymentStatus = PaymentStatus.DP,
      paymentMethod = PaymentMethod.CASH,
      receivedCashAmount = 40000L,
      dpAmount = 40000L,
      notes = "Lipat rapi",
      estimatedFinishAtMillis = System.currentTimeMillis() + 86_400_000L
    )

    val firstCreate = withTimeout(TIMEOUT_MS) {
      repo.createOrder(ownerUid, "ADA", "Pemilik", UserRole.OWNER, req)
    }
    assertTrue("Create order should succeed: ${firstCreate.exceptionOrNull()}", firstCreate.isSuccess)
    val order = firstCreate.getOrThrow()
    assertEquals(94000L, order.subtotal)
    assertEquals(100000L, order.totalAmount)
    assertEquals(40000L, order.paidAmount)
    assertEquals(60000L, order.remainingAmount)
    assertEquals("628123456789", order.customerPhone)
    assertEquals(PaymentStatus.DP.name, order.paymentStatus)

    // Double-submit same draftOrderId must return same nota and NOT duplicate
    val secondCreate = withTimeout(TIMEOUT_MS) {
      repo.createOrder(ownerUid, "ADA", "Pemilik", UserRole.OWNER, req)
    }
    assertTrue(secondCreate.isSuccess)
    assertEquals(order.notaNumber, secondCreate.getOrThrow().notaNumber)

    // Record remaining payment with idempotent ID
    val payId = "pay_${UUID.randomUUID().toString().take(8)}"
    val payRes = withTimeout(TIMEOUT_MS) {
      repo.recordPayment(
        shopId = ownerUid,
        orderId = order.id,
        paymentRequestId = payId,
        receivedAmount = 100000L, // pays 60,000, change = 40,000
        method = PaymentMethod.CASH,
        isDpPayment = false,
        actorName = "Pemilik",
        actorRole = UserRole.OWNER
      )
    }
    assertTrue("Payment should succeed: ${payRes.exceptionOrNull()}", payRes.isSuccess)
    assertEquals(60000L, payRes.getOrThrow().amount)
    assertEquals(40000L, payRes.getOrThrow().changeAmount)

    // Duplicate payment click with same payId returns same record without overcharging
    val dupPayRes = withTimeout(TIMEOUT_MS) {
      repo.recordPayment(
        shopId = ownerUid,
        orderId = order.id,
        paymentRequestId = payId,
        receivedAmount = 100000L,
        method = PaymentMethod.CASH,
        isDpPayment = false,
        actorName = "Pemilik",
        actorRole = UserRole.OWNER
      )
    }
    assertTrue(dupPayRes.isSuccess)
    assertEquals(payId, dupPayRes.getOrThrow().id)

    // Verify order observed in Flow
    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOrders(ownerUid).first { list -> list.any { it.id == order.id } }
    }.first { it.id == order.id }
    assertEquals(PaymentStatus.PAID.name, observed.paymentStatus)
    assertEquals(0L, observed.remainingAmount)

    // Update status to READY, log WhatsApp, then COMPLETED, then verify terminal lock
    assertTrue(
      withTimeout(TIMEOUT_MS) {
        repo.updateOrderStatus(ownerUid, observed, OrderStatus.READY, "Pemilik", UserRole.OWNER)
      }.isSuccess
    )

    val readyOrder = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOrders(ownerUid).first { list ->
        list.firstOrNull { it.id == order.id }?.orderStatusEnum == OrderStatus.READY
      }
    }.first { it.id == order.id }

    assertTrue(
      withTimeout(TIMEOUT_MS) {
        repo.logWhatsAppOpened(ownerUid, readyOrder, "628123456789", "Pemilik", UserRole.OWNER)
      }.isSuccess
    )

    val readyWithWa = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOrders(ownerUid).first { list ->
        list.firstOrNull { it.id == order.id }?.parsedWhatsAppLogs?.isNotEmpty() == true
      }
    }.first { it.id == order.id }

    assertTrue(
      withTimeout(TIMEOUT_MS) {
        repo.updateOrderStatus(ownerUid, readyWithWa, OrderStatus.COMPLETED, "Pemilik", UserRole.OWNER)
      }.isSuccess
    )

    val completedOrder = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOrders(ownerUid).first { list ->
        list.firstOrNull { it.id == order.id }?.orderStatusEnum == OrderStatus.COMPLETED
      }
    }.first { it.id == order.id }

    val revertAttempt = withTimeout(TIMEOUT_MS) {
      repo.updateOrderStatus(ownerUid, completedOrder, OrderStatus.RECEIVED, "Pemilik", UserRole.OWNER)
    }
    assertTrue("Terminal COMPLETED order must reject status change", revertAttempt.isFailure)

    // Owner expense succeeds
    val expRes = withTimeout(TIMEOUT_MS) {
      repo.saveExpense(
        ownerUid,
        Expense(category = "Deterjen", amount = 45000L, dateMillis = System.currentTimeMillis(), notes = "Beli 5L"),
        "Pemilik"
      )
    }
    assertTrue(expRes.isSuccess)
  }

  @Test
  fun outsiderAndUnauthenticated_failWithPermissionDenied() = runBlocking {
    val ownerEmail = "owner2_${UUID.randomUUID().toString().take(6)}@adalaundry.test"
    val ownerUid = signInTestUser(ownerEmail)
    val authRepo = AuthSessionRepository(firestore, auth)
    assertTrue(
      withTimeout(TIMEOUT_MS) {
        authRepo.createOwnerShopWithCatalog("ADA LAUNDRY", "Jl. A", "08111111111", "08-20", "ADA")
      }.isSuccess
    )

    // Another signed-in user (outsider) cannot read or write owner's services/orders
    val outsiderEmail = "outsider_${UUID.randomUUID().toString().take(6)}@evil.test"
    signInTestUser(outsiderEmail)
    val repo = LaundryRepository(firestore, auth)

    val writeSvc = withTimeout(TIMEOUT_MS) {
      repo.saveService(
        ownerUid,
        LaundryService(name = "Hack", category = ServiceCategory.KILOAN.name, price = 100L),
        "Outsider"
      )
    }
    assertTrue(writeSvc.isFailure)
    val writeEx = writeSvc.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(writeEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, writeEx?.code)

    // Unauthenticated access rejected
    auth.signOut()
    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeOrders(ownerUid).first() }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull(caught)
  }

  private companion object {
    const val TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
