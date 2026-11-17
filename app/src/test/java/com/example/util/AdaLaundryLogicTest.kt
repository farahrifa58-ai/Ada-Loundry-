package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLanguage
import com.example.data.local.ReceiptPaperSize
import com.example.data.local.ThemeMode
import com.example.data.model.Expense
import com.example.data.model.LaundryOrder
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.ReportPeriod
import com.example.data.model.ServiceCategory
import com.example.data.model.ShopProfile
import com.example.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdaLaundryLogicTest {

  @Test
  fun whatsappPhoneNormalization_validAndInvalidCases() {
    assertEquals("628123456789", WhatsAppHelper.normalizeIndonesianPhone("08123456789"))
    assertEquals("628123456789", WhatsAppHelper.normalizeIndonesianPhone("+62 812-3456-789"))
    assertEquals("628123456789", WhatsAppHelper.normalizeIndonesianPhone("628123456789"))
    assertEquals("628123456789", WhatsAppHelper.normalizeIndonesianPhone("8123456789"))

    assertNull(WhatsAppHelper.normalizeIndonesianPhone(""))
    assertNull(WhatsAppHelper.normalizeIndonesianPhone("   "))
    assertNull(WhatsAppHelper.normalizeIndonesianPhone("08123")) // too short
    assertNull(WhatsAppHelper.normalizeIndonesianPhone("12345678901")) // invalid prefix
    assertNull(WhatsAppHelper.normalizeIndonesianPhone("000000000000")) // all zeros
    assertNull(WhatsAppHelper.normalizeIndonesianPhone("0812abcd5678")) // non-digits
  }

  @Test
  fun whatsappMessage_containsRealMultiCategoryAndKarpetPerLembarDetails() {
    val items = listOf(
      OrderItem("s1", "Cuci Komplit", ServiceCategory.KILOAN.name, "kg", 4.0, 7000L, 28000L),
      OrderItem("s2", "Kemeja", ServiceCategory.SATUAN.name, "pcs", 3.0, 8000L, 24000L),
      OrderItem("s3", "Karpet Standar", ServiceCategory.KARPET.name, "lembar", 2.0, 25000L, 50000L)
    )
    val order = LaundryOrder(
      id = "ord1",
      notaNumber = "ADA-20261116-0001",
      customerName = "Budi Santoso",
      customerPhone = "628123456789",
      items = items.map { it.toMap() },
      subtotal = 102000L,
      discount = 2000L,
      extraFee = 10000L,
      extraFeeLabel = "Express",
      taxRatePercent = 11.0,
      taxAmount = 12100L,
      totalAmount = 122100L,
      paidAmount = 50000L,
      remainingAmount = 72100L,
      paymentStatus = PaymentStatus.DP.name,
      paymentMethod = PaymentMethod.CASH.name,
      orderStatus = OrderStatus.READY.name
    )

    val message = WhatsAppHelper.buildReadyWhatsAppMessage(order, "ADA LAUNDRY PUSAT")
    assertTrue(message.contains("Halo Kak Budi Santoso"))
    assertTrue(message.contains("ADA-20261116-0001"))
    assertTrue(message.contains("Cuci Komplit (4 kg x Rp7.000 = Rp28.000)"))
    assertTrue(message.contains("Kemeja (3 pcs x Rp8.000 = Rp24.000)"))
    assertTrue(message.contains("Karpet Standar (2 lembar x Rp25.000 = Rp50.000)"))
    assertFalse("Must never reference square meters", message.contains("m2") || message.contains("m²"))
    assertTrue(message.contains("Diskon: -Rp2.000"))
    assertTrue(message.contains("Biaya Tambahan (Express): Rp10.000"))
    assertTrue(message.contains("PPN (11%): Rp12.100"))
    assertTrue(message.contains("Total: Rp122.100"))
    assertTrue(message.contains("Sudah Dibayar: Rp50.000"))
    assertTrue(message.contains("Sisa Pembayaran: Rp72.100"))
    assertTrue(message.contains("ADA LAUNDRY PUSAT"))

    val uri = WhatsAppHelper.buildWhatsAppUri("628123456789", message)
    assertTrue(uri.toString().startsWith("https://wa.me/628123456789?text="))
  }

  @Test
  fun priceCalculationsAndDiscountClamping_areAccurate() {
    assertEquals(28000L, Formatters.calculateLineTotal(4.0, 7000L))
    assertEquals(17500L, Formatters.calculateLineTotal(2.5, 7000L))
    assertEquals(50000L, Formatters.calculateLineTotal(2.0, 25000L))

    val items = listOf(
      OrderItem("s1", "Cuci Komplit", ServiceCategory.KILOAN.name, "kg", 4.0, 7000L, 28000L)
    )
    // Excess discount clamped cleanly to subtotal + extraFee
    val (sub, disc, total) = Formatters.calculateOrderTotals(items, discount = 999999L, extraFee = 5000L)
    assertEquals(28000L, sub)
    assertEquals(33000L, disc)
    assertEquals(0L, total)

    // PPN 11% applied after discount + extraFee
    val withTax = Formatters.calculateOrderTotalsWithTax(items, discount = 3000L, extraFee = 5000L, taxRatePercent = 11.0)
    assertEquals(28000L, withTax.subtotal)
    assertEquals(3000L, withTax.discount)
    assertEquals(5000L, withTax.extraFee)
    assertEquals(3300L, withTax.taxAmount) // 11% of 30,000 = 3,300
    assertEquals(33300L, withTax.totalAmount)
  }

  @Test
  fun reportSummary_distinguishesOmzetCashPaidAndEstimatedProfit() {
    val now = System.currentTimeMillis()
    val start = Formatters.startOfPeriodMillis(ReportPeriod.DAILY, now)
    val end = Formatters.endOfDayMillis(now)

    val paidCashOrder = LaundryOrder(
      id = "o1",
      notaNumber = "ADA-1",
      customerName = "Ani",
      customerPhone = "628111111111",
      items = listOf(OrderItem("s1", "Kiloan", ServiceCategory.KILOAN.name, "kg", 4.0, 7000L, 28000L).toMap()),
      subtotal = 28000L,
      totalAmount = 28000L,
      paidAmount = 28000L,
      remainingAmount = 0L,
      paymentStatus = PaymentStatus.PAID.name,
      paymentMethod = PaymentMethod.CASH.name,
      orderStatus = OrderStatus.COMPLETED.name,
      transactionAtMillis = now,
      paymentHistory = listOf(
        PaymentRecord("p1", 28000L, 30000L, 2000L, PaymentMethod.CASH.name, PaymentStatus.PAID.name, now).toMap()
      )
    )

    val dpQrisOrder = LaundryOrder(
      id = "o2",
      notaNumber = "ADA-2",
      customerName = "Rudi",
      customerPhone = "628222222222",
      items = listOf(OrderItem("s3", "Karpet", ServiceCategory.KARPET.name, "lembar", 2.0, 25000L, 50000L).toMap()),
      subtotal = 50000L,
      totalAmount = 50000L,
      paidAmount = 20000L,
      remainingAmount = 30000L,
      paymentStatus = PaymentStatus.DP.name,
      paymentMethod = PaymentMethod.QRIS.name,
      orderStatus = OrderStatus.READY.name,
      transactionAtMillis = now,
      paymentHistory = listOf(
        PaymentRecord("p2", 20000L, 20000L, 0L, PaymentMethod.QRIS.name, PaymentStatus.DP.name, now).toMap()
      )
    )

    val cancelledOrder = LaundryOrder(
      id = "o3",
      notaNumber = "ADA-3",
      customerName = "Batal",
      totalAmount = 999999L,
      paidAmount = 0L,
      orderStatus = OrderStatus.CANCELLED.name,
      transactionAtMillis = now
    )

    val expenses = listOf(
      Expense(id = "e1", category = "Deterjen", amount = 15000L, dateMillis = now)
    )

    val summary = Formatters.computeReportSummary(
      orders = listOf(paidCashOrder, dpQrisOrder, cancelledOrder),
      expenses = expenses,
      startMillis = start,
      endMillis = end
    )

    assertEquals(2, summary.totalTransactions)
    assertEquals(1, summary.cancelledOrders)
    assertEquals(78000L, summary.totalOmzet)
    assertEquals(28000L, summary.totalCashPaid)
    assertEquals(20000L, summary.totalQrisPaid)
    assertEquals(48000L, summary.totalPaidIncome)
    assertEquals(30000L, summary.totalUnpaidReceivables)
    assertEquals(15000L, summary.totalExpenses)
    // Profit = 48,000 paid income - 15,000 expense = 33,000 (NOT 78,000 - 15,000)
    assertEquals(33000L, summary.estimatedNetProfit)
    assertEquals(28000L, summary.kiloanRevenue)
    assertEquals(50000L, summary.karpetRevenue)
  }

  @Test
  fun excelXlsx_generatesValidThreeSheetWorkbook() {
    val shop = ShopProfile(
      id = "shop1",
      businessName = "ADA LAUNDRY",
      address = "Jl. Merdeka No. 10",
      phone = "081299998888",
      operatingHours = "07:00 - 21:00",
      notaPrefix = "ADA"
    )
    val order = LaundryOrder(
      id = "o1",
      notaNumber = "ADA-20261116-0001",
      customerName = "Siti Aminah",
      customerPhone = "628123456789",
      items = listOf(OrderItem("s1", "Cuci Komplit", ServiceCategory.KILOAN.name, "kg", 3.0, 7000L, 21000L).toMap()),
      subtotal = 21000L,
      totalAmount = 21000L,
      paidAmount = 21000L,
      remainingAmount = 0L,
      paymentStatus = PaymentStatus.PAID.name,
      paymentMethod = PaymentMethod.TRANSFER.name,
      orderStatus = OrderStatus.READY.name,
      transactionAtMillis = System.currentTimeMillis(),
      estimatedFinishAtMillis = System.currentTimeMillis() + 86_400_000L,
      cashierName = "Kasir Andi"
    )
    val expense = Expense(
      id = "e1",
      category = "Pewangi",
      amount = 12000L,
      dateMillis = System.currentTimeMillis(),
      notes = "Pewangi Sakura"
    )

    val summary = Formatters.computeReportSummary(listOf(order), listOf(expense), 0L, Long.MAX_VALUE)
    val xlsxBytes = ReceiptAndReportExporter.buildXlsxBytes(shop, "Hari Ini", summary, listOf(order), listOf(expense))
    assertTrue("XLSX should not be empty", xlsxBytes.size > 200)

    val entries = mutableMapOf<String, String>()
    ZipInputStream(ByteArrayInputStream(xlsxBytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
    assertTrue(entries.containsKey("[Content_Types].xml"))
    assertTrue(entries.containsKey("xl/workbook.xml"))
    assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
    assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))
    assertTrue(entries.containsKey("xl/worksheets/sheet3.xml"))
    assertTrue(entries["xl/worksheets/sheet2.xml"]!!.contains("ADA-20261116-0001"))
    assertTrue(entries["xl/worksheets/sheet2.xml"]!!.contains("Siti Aminah"))
    assertTrue(entries["xl/worksheets/sheet3.xml"]!!.contains("Pewangi Sakura"))
  }

  @Test
  fun orderStatusTerminalStates_lockCompletedAndCancelled() {
    assertFalse(OrderStatus.RECEIVED.isTerminal)
    assertFalse(OrderStatus.PROCESSING.isTerminal)
    assertFalse(OrderStatus.READY.isTerminal)
    assertTrue(OrderStatus.COMPLETED.isTerminal)
    assertTrue(OrderStatus.CANCELLED.isTerminal)

    // Verify monotonic forward-only step indices
    assertTrue(OrderStatus.RECEIVED.stepIndex < OrderStatus.PROCESSING.stepIndex)
    assertTrue(OrderStatus.PROCESSING.stepIndex < OrderStatus.READY.stepIndex)
    assertTrue(OrderStatus.READY.stepIndex < OrderStatus.COMPLETED.stepIndex)
  }

  @Test
  fun whatsappCustomTemplate_replacesPlaceholdersCorrectly() {
    val order = LaundryOrder(
      id = "ord_tpl",
      notaNumber = "ADA-20261117-0077",
      customerName = "Rina Wijaya",
      customerPhone = "628129990001",
      items = listOf(OrderItem("s1", "Cuci Komplit", ServiceCategory.KILOAN.name, "kg", 3.0, 7000L, 21000L).toMap()),
      subtotal = 21000L,
      totalAmount = 21000L,
      paidAmount = 10000L,
      remainingAmount = 11000L,
      paymentStatus = PaymentStatus.DP.name,
      orderStatus = OrderStatus.READY.name
    )
    val customTpl = "Hai {nama_pelanggan}, nota {no_nota} dari {nama_usaha} siap diambil! Sisa: {sisa} ({status_bayar})."
    val rendered = WhatsAppHelper.buildReadyWhatsAppMessage(
      order = order,
      shopName = "ADA LAUNDRY JAYA",
      customTemplate = customTpl
    )
    assertEquals(
      "Hai Rina Wijaya, nota ADA-20261117-0077 dari ADA LAUNDRY JAYA siap diambil! Sisa: Rp11.000 (Uang Muka / DP).",
      rendered
    )
  }

  @Test
  fun englishLocalization_switchesAllDomainStringsWhatsAppAndExports() {
    assertEquals("Owner", UserRole.OWNER.localizedLabel(true))
    assertEquals("Cashier", UserRole.KASIR.localizedLabel(true))
    assertEquals("Weight Laundry (Kg)", ServiceCategory.KILOAN.localizedLabel(true))
    assertEquals("Per-Item Laundry", ServiceCategory.SATUAN.localizedLabel(true))
    assertEquals("Carpet Laundry", ServiceCategory.KARPET.localizedLabel(true))
    assertEquals("sheet", localizedUnit("lembar", true))
    assertEquals("READY FOR PICKUP", OrderStatus.READY.localizedLabel(true))
    assertEquals("Ready for Pickup", OrderStatus.READY.localizedTitleCase(true))
    assertEquals("Deposit / DP", PaymentStatus.DP.localizedLabel(true))
    assertEquals("Daily", ReportPeriod.DAILY.localizedLabel(true))
    assertEquals("Dark Theme", ThemeMode.DARK.localizedLabel(true))
    assertEquals("A4 / Standard", ReceiptPaperSize.A4.localizedLabel(true))
    assertEquals(
      "Order created",
      com.example.data.model.StatusHistoryEntry(note = "Pesanan dibuat").localizedNote(true)
    )
    assertEquals(
      "New transaction ADA-1 for Budi (Rp10.000)",
      com.example.data.model.ActivityLog(description = "Transaksi baru ADA-1 a/n Budi (Rp10.000)").localizedDescription(true)
    )

    val items = listOf(
      OrderItem("s1", "Wash & Fold", ServiceCategory.KILOAN.name, "kg", 2.0, 7000L, 14000L),
      OrderItem("s3", "Standard Carpet", ServiceCategory.KARPET.name, "lembar", 1.0, 25000L, 25000L)
    )
    val order = LaundryOrder(
      id = "ord_en",
      notaNumber = "ADA-20261117-0099",
      customerName = "Sarah",
      customerPhone = "628123456789",
      items = items.map { it.toMap() },
      subtotal = 39000L,
      discount = 4000L,
      extraFee = 5000L,
      extraFeeLabel = "Express",
      taxRatePercent = 10.0,
      taxAmount = 4000L,
      totalAmount = 44000L,
      paidAmount = 20000L,
      remainingAmount = 24000L,
      paymentStatus = PaymentStatus.DP.name,
      orderStatus = OrderStatus.READY.name
    )

    val enMsg = WhatsAppHelper.buildReadyWhatsAppMessage(order, "ADA LAUNDRY", lang = AppLanguage.EN)
    assertTrue(enMsg.contains("Hello Sarah"))
    assertTrue(enMsg.contains("Receipt No: ADA-20261117-0099"))
    assertTrue(enMsg.contains("1 sheet x Rp25.000"))
    assertTrue(enMsg.contains("Discount: -Rp4.000"))
    assertTrue(enMsg.contains("Extra Fee (Express): Rp5.000"))
    assertTrue(enMsg.contains("Tax (10%): Rp4.000"))
    assertTrue(enMsg.contains("Remaining Balance: Rp24.000"))

    val shop = ShopProfile(id = "s1", businessName = "ADA LAUNDRY")
    val summary = Formatters.computeReportSummary(listOf(order), emptyList(), 0L, Long.MAX_VALUE)
    val xlsxEn = ReceiptAndReportExporter.buildXlsxBytes(shop, "Daily", summary, listOf(order), emptyList(), AppLanguage.EN)
    val sheet1Xml = ZipInputStream(ByteArrayInputStream(xlsxEn)).use { zis ->
      var out = ""
      var entry = zis.nextEntry
      while (entry != null) {
        if (entry.name == "xl/worksheets/sheet1.xml") {
          out = zis.readBytes().toString(Charsets.UTF_8)
          break
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
      out
    }
    assertTrue(sheet1Xml.contains("FINANCIAL REPORT"))
    assertTrue(sheet1Xml.contains("Total Revenue"))
  }

  @Test
  fun reminderHelper_detectsNewlyDueOrdersOnceAndSkipsTerminal() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val now = 1_800_000_000_000L

    val dueProcessing = LaundryOrder(
      id = "due_1",
      notaNumber = "ADA-01",
      customerName = "Ani",
      orderStatus = OrderStatus.PROCESSING.name,
      estimatedFinishAtMillis = now - 60_000L
    )
    val futureOrder = LaundryOrder(
      id = "future_1",
      notaNumber = "ADA-02",
      customerName = "Budi",
      orderStatus = OrderStatus.RECEIVED.name,
      estimatedFinishAtMillis = now + 3_600_000L
    )
    val completedDue = LaundryOrder(
      id = "done_1",
      notaNumber = "ADA-03",
      customerName = "Citra",
      orderStatus = OrderStatus.COMPLETED.name,
      estimatedFinishAtMillis = now - 60_000L
    )

    val allOrders = listOf(dueProcessing, futureOrder, completedDue)
    val newlyDue = LaundryReminderHelper.findNewlyDueOrders(context, allOrders, nowMillis = now)
    assertEquals(listOf("due_1"), newlyDue.map { it.id })

    // Once marked notified, should not trigger again for same scheduled time
    LaundryReminderHelper.markOrdersNotified(context, newlyDue)
    val secondCheck = LaundryReminderHelper.findNewlyDueOrders(context, allOrders, nowMillis = now + 15_000L)
    assertTrue(secondCheck.isEmpty())
  }

  @Test
  fun reminderHelper_persistsUpcomingAlarmCacheAndConsumesWhenDue() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val now = 1_900_000_000_000L
    val upcomingOrder = LaundryOrder(
      id = "lock_alarm_1",
      notaNumber = "ADA-20261117-0101",
      customerName = "Dewi",
      orderStatus = OrderStatus.PROCESSING.name,
      estimatedFinishAtMillis = now + 120_000L
    )
    val completedOrder = LaundryOrder(
      id = "lock_done_1",
      notaNumber = "ADA-20261117-0102",
      customerName = "Eko",
      orderStatus = OrderStatus.COMPLETED.name,
      estimatedFinishAtMillis = now + 60_000L
    )

    LaundryReminderHelper.scheduleNextDueAlarm(
      context = context,
      orders = listOf(upcomingOrder, completedOrder),
      playSound = true,
      nowMillis = now
    )

    assertTrue(LaundryReminderHelper.isDueSoundEnabledSync(context, fallback = false))
    // Before pickup time: none due yet
    assertTrue(LaundryReminderHelper.consumeDueCachedOrders(context, now + 30_000L).isEmpty())
    // At/after pickup time: upcoming order is returned, terminal order was excluded
    val dueAtLockWake = LaundryReminderHelper.consumeDueCachedOrders(context, now + 125_000L)
    assertEquals(listOf("lock_alarm_1"), dueAtLockWake.map { it.id })
  }
}
