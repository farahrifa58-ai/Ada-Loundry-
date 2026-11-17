package com.example.util

import com.example.data.local.AppLanguage
import com.example.data.model.LaundryOrder
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ReportPeriod
import com.example.data.model.ReportSummary
import com.example.data.model.ServiceCategory
import com.example.data.model.ServiceUsageStat
import com.example.data.model.Expense
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object Formatters {
  private val indonesiaLocale = Locale("in", "ID")
  private val rupiahSymbols = DecimalFormatSymbols(indonesiaLocale).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
  }
  private val rupiahFormatter = DecimalFormat("#,##0", rupiahSymbols)
  private val qtyFormatter = DecimalFormat("#.##", rupiahSymbols)

  fun formatRupiah(amount: Long): String {
    val prefix = if (amount < 0) "-Rp" else "Rp"
    return "$prefix${rupiahFormatter.format(kotlin.math.abs(amount))}"
  }

  fun formatQuantity(quantity: Double, unitLabel: String): String {
    val formatted = if (quantity % 1.0 == 0.0) {
      quantity.toLong().toString()
    } else {
      qtyFormatter.format(quantity)
    }
    return "$formatted $unitLabel"
  }

  fun calculateLineTotal(quantity: Double, unitPrice: Long): Long {
    if (quantity <= 0.0 || unitPrice < 0L) return 0L
    return (quantity * unitPrice).roundToLong()
  }

  data class OrderTotalsBreakdown(
    val subtotal: Long,
    val discount: Long,
    val extraFee: Long,
    val taxAmount: Long,
    val totalAmount: Long
  )

  fun calculateOrderTotals(
    items: List<OrderItem>,
    discount: Long,
    extraFee: Long
  ): Triple<Long, Long, Long> {
    val subtotal = items.sumOf { it.lineTotal }
    val safeExtra = extraFee.coerceAtLeast(0L)
    val safeDiscount = discount.coerceIn(0L, subtotal + safeExtra)
    val total = (subtotal - safeDiscount + safeExtra).coerceAtLeast(0L)
    return Triple(subtotal, safeDiscount, total)
  }

  fun calculateOrderTotalsWithTax(
    items: List<OrderItem>,
    discount: Long,
    extraFee: Long,
    taxRatePercent: Double
  ): OrderTotalsBreakdown {
    val subtotal = items.sumOf { it.lineTotal }
    val safeExtra = extraFee.coerceAtLeast(0L)
    val safeDiscount = discount.coerceIn(0L, subtotal + safeExtra)
    val taxableBase = (subtotal - safeDiscount + safeExtra).coerceAtLeast(0L)
    val safeTaxRate = taxRatePercent.coerceIn(0.0, 100.0)
    val taxAmount = if (safeTaxRate > 0.0) (taxableBase * (safeTaxRate / 100.0)).roundToLong() else 0L
    val total = (taxableBase + taxAmount).coerceAtLeast(0L)
    return OrderTotalsBreakdown(
      subtotal = subtotal,
      discount = safeDiscount,
      extraFee = safeExtra,
      taxAmount = taxAmount,
      totalAmount = total
    )
  }

  private fun localeFor(lang: AppLanguage): Locale =
    if (lang == AppLanguage.EN) Locale.ENGLISH else indonesiaLocale

  fun formatDateTime(millis: Long, lang: AppLanguage = AppLanguage.ID): String {
    if (millis <= 0L) return "-"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", localeFor(lang))
    return sdf.format(Date(millis))
  }

  fun formatDateOnly(millis: Long, lang: AppLanguage = AppLanguage.ID): String {
    if (millis <= 0L) return "-"
    val sdf = SimpleDateFormat("dd MMM yyyy", localeFor(lang))
    return sdf.format(Date(millis))
  }

  fun formatTimeOnly(millis: Long, lang: AppLanguage = AppLanguage.ID): String {
    if (millis <= 0L) return "-"
    val sdf = SimpleDateFormat("HH:mm", localeFor(lang))
    return sdf.format(Date(millis))
  }

  fun formatCompactDateForNota(millis: Long): String {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
    return sdf.format(Date(millis))
  }

  fun startOfPeriodMillis(period: ReportPeriod, anchorMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance(indonesiaLocale).apply {
      timeInMillis = anchorMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    when (period) {
      ReportPeriod.DAILY -> Unit
      ReportPeriod.WEEKLY -> {
        cal.add(Calendar.DAY_OF_YEAR, -6)
      }
      ReportPeriod.MONTHLY -> {
        cal.set(Calendar.DAY_OF_MONTH, 1)
      }
      ReportPeriod.YEARLY -> {
        cal.set(Calendar.DAY_OF_YEAR, 1)
      }
    }
    return cal.timeInMillis
  }

  fun endOfDayMillis(anchorMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance(indonesiaLocale).apply {
      timeInMillis = anchorMillis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
      set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
  }

  fun computeReportSummary(
    orders: List<LaundryOrder>,
    expenses: List<Expense>,
    startMillis: Long,
    endMillis: Long,
    searchQuery: String = ""
  ): ReportSummary {
    val query = searchQuery.trim().lowercase(indonesiaLocale)
    val periodOrders = orders.filter { order ->
      val inRange = order.transactionAtMillis in startMillis..endMillis
      val matchesQuery = if (query.isEmpty()) true else {
        order.notaNumber.lowercase(indonesiaLocale).contains(query) ||
          order.customerName.lowercase(indonesiaLocale).contains(query) ||
          order.customerPhone.contains(query) ||
          order.parsedItems.any { it.serviceName.lowercase(indonesiaLocale).contains(query) }
      }
      inRange && matchesQuery
    }
    val periodExpenses = expenses.filter { it.dateMillis in startMillis..endMillis }

    val activeOrders = periodOrders.filter { it.orderStatusEnum != OrderStatus.CANCELLED }
    val cancelledCount = periodOrders.count { it.orderStatusEnum == OrderStatus.CANCELLED }

    var cashPaid = 0L
    var transferPaid = 0L
    var qrisPaid = 0L
    var lunasAmount = 0L
    var dpAmount = 0L

    activeOrders.forEach { order ->
      val payments = order.parsedPaymentHistory
      if (payments.isNotEmpty()) {
        payments.forEach { pay ->
          when (PaymentMethod.fromString(pay.method)) {
            PaymentMethod.CASH -> cashPaid += pay.amount
            PaymentMethod.TRANSFER -> transferPaid += pay.amount
            PaymentMethod.QRIS -> qrisPaid += pay.amount
          }
        }
      } else if (order.paidAmount > 0L) {
        when (order.paymentMethodEnum) {
          PaymentMethod.CASH -> cashPaid += order.paidAmount
          PaymentMethod.TRANSFER -> transferPaid += order.paidAmount
          PaymentMethod.QRIS -> qrisPaid += order.paidAmount
        }
      }

      when (order.paymentStatusEnum) {
        PaymentStatus.PAID -> lunasAmount += order.paidAmount
        PaymentStatus.DP -> dpAmount += order.paidAmount
        PaymentStatus.UNPAID -> Unit
      }
    }

    val totalOmzet = activeOrders.sumOf { it.totalAmount }
    val totalPaidIncome = activeOrders.sumOf { it.paidAmount }
    val totalReceivables = activeOrders.sumOf { it.remainingAmount }
    val totalExpenses = periodExpenses.sumOf { it.amount }
    // Estimated net profit strictly uses actual recorded incoming cash payments minus actual recorded expenses
    val estimatedProfit = totalPaidIncome - totalExpenses

    var kiloanRev = 0L
    var kiloanCount = 0
    var satuanRev = 0L
    var satuanCount = 0
    var karpetRev = 0L
    var karpetCount = 0

    data class Acc(
      val name: String,
      val cat: ServiceCategory,
      var orders: Int = 0,
      var qty: Double = 0.0,
      val unit: String,
      var rev: Long = 0L
    )
    val usageMap = mutableMapOf<String, Acc>()

    activeOrders.forEach { order ->
      val items = order.parsedItems
      val categoriesInOrder = items.map { ServiceCategory.fromString(it.category) }.toSet()
      if (ServiceCategory.KILOAN in categoriesInOrder) kiloanCount++
      if (ServiceCategory.SATUAN in categoriesInOrder) satuanCount++
      if (ServiceCategory.KARPET in categoriesInOrder) karpetCount++

      items.forEach { item ->
        val cat = ServiceCategory.fromString(item.category)
        when (cat) {
          ServiceCategory.KILOAN -> kiloanRev += item.lineTotal
          ServiceCategory.SATUAN -> satuanRev += item.lineTotal
          ServiceCategory.KARPET -> karpetRev += item.lineTotal
        }
        val key = "${item.serviceName}|${cat.name}"
        val acc = usageMap.getOrPut(key) {
          Acc(item.serviceName, cat, 0, 0.0, item.unitLabel, 0L)
        }
        acc.orders += 1
        acc.qty += item.quantity
        acc.rev += item.lineTotal
      }
    }

    val topServices = usageMap.values
      .sortedWith(compareByDescending<Acc> { it.orders }.thenByDescending { it.rev })
      .map {
        ServiceUsageStat(
          serviceName = it.name,
          category = it.cat,
          orderCount = it.orders,
          totalQuantity = it.qty,
          unitLabel = it.unit,
          totalRevenue = it.rev
        )
      }

    return ReportSummary(
      totalOmzet = totalOmzet,
      totalTransactions = activeOrders.size,
      receivedOrders = activeOrders.count { it.orderStatusEnum == OrderStatus.RECEIVED },
      processingOrders = activeOrders.count { it.orderStatusEnum == OrderStatus.PROCESSING },
      readyOrders = activeOrders.count { it.orderStatusEnum == OrderStatus.READY },
      completedOrders = activeOrders.count { it.orderStatusEnum == OrderStatus.COMPLETED },
      cancelledOrders = cancelledCount,
      totalCashPaid = cashPaid,
      totalTransferPaid = transferPaid,
      totalQrisPaid = qrisPaid,
      totalPaidIncome = totalPaidIncome,
      totalLunasAmount = lunasAmount,
      totalDpAmount = dpAmount,
      totalUnpaidReceivables = totalReceivables,
      totalExpenses = totalExpenses,
      estimatedNetProfit = estimatedProfit,
      kiloanRevenue = kiloanRev,
      kiloanCount = kiloanCount,
      satuanRevenue = satuanRev,
      satuanCount = satuanCount,
      karpetRevenue = karpetRev,
      karpetCount = karpetCount,
      topServices = topServices
    )
  }
}
