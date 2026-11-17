package com.example.util

import android.net.Uri
import com.example.data.local.AppLanguage
import com.example.data.model.LaundryOrder
import com.example.data.model.OrderItem
import com.example.data.model.ServiceCategory
import java.net.URLEncoder

object WhatsAppHelper {

  /**
   * Validates and normalizes Indonesian phone numbers to international WhatsApp format (62...).
   * Examples:
   * - "08123456789" -> "628123456789"
   * - "+62 812-3456-789" -> "628123456789"
   * - "8123456789" -> "628123456789"
   * Returns null if empty or invalid.
   */
  fun normalizeIndonesianPhone(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.trim().replace(Regex("[\\s\\-().+]"), "")
    if (cleaned.isEmpty() || !cleaned.all { it.isDigit() }) return null

    val normalized = when {
      cleaned.startsWith("62") -> cleaned
      cleaned.startsWith("0") -> "62" + cleaned.drop(1)
      cleaned.startsWith("8") -> "62$cleaned"
      else -> return null
    }

    // Indonesian mobile/phone numbers after 62 typically range from 10 to 15 digits total
    if (normalized.length !in 10..15) return null
    if (!normalized.startsWith("62")) return null
    // Must not be all zeros after 62
    val subscriber = normalized.removePrefix("62")
    if (subscriber.isEmpty() || subscriber.all { it == '0' }) return null

    return normalized
  }

  fun isValidIndonesianPhone(raw: String?): Boolean = normalizeIndonesianPhone(raw) != null

  /**
   * Formats a single order line item clearly according to service category:
   * - KILOAN: "4 kg x Rp7.000 = Rp28.000"
   * - SATUAN: "Kemeja (2 pcs x Rp8.000 = Rp16.000)"
   * - KARPET: "Karpet Standar (2 lembar x Rp25.000 = Rp50.000)"
   */
  fun formatOrderItemLine(item: OrderItem, lang: AppLanguage = AppLanguage.ID): String {
    val qtyText = Formatters.formatQuantity(item.quantity, localizedUnit(item.unitLabel, lang))
    val unitPriceText = Formatters.formatRupiah(item.unitPrice)
    val lineTotalText = Formatters.formatRupiah(item.lineTotal)
    return "${item.serviceName} ($qtyText x $unitPriceText = $lineTotalText)"
  }

  const val DEFAULT_WHATSAPP_TEMPLATE: String =
    "Halo Kak {nama_pelanggan} 👋\n\n" +
      "Kami informasikan bahwa cucian Anda sudah selesai dan siap diambil.\n\n" +
      "DETAIL PESANAN\n" +
      "Nomor Nota: {no_nota}\n" +
      "{rincian_pesanan}\n" +
      "Subtotal: {subtotal}\n" +
      "{baris_diskon}{baris_tambahan}{baris_ppn}Total: {total}\n" +
      "Sudah Dibayar: {dibayar}\n" +
      "Sisa Pembayaran: {sisa}\n\n" +
      "Silakan datang ke outlet {nama_usaha} untuk mengambil cucian Anda.\n\n" +
      "Terima kasih telah menggunakan layanan kami. 😊"

  const val DEFAULT_WHATSAPP_TEMPLATE_EN: String =
    "Hello {nama_pelanggan} 👋\n\n" +
      "Your laundry order is finished and ready for pickup.\n\n" +
      "ORDER DETAILS\n" +
      "Receipt No: {no_nota}\n" +
      "{rincian_pesanan}\n" +
      "Subtotal: {subtotal}\n" +
      "{baris_diskon}{baris_tambahan}{baris_ppn}Total: {total}\n" +
      "Paid: {dibayar}\n" +
      "Remaining Balance: {sisa}\n\n" +
      "Please visit {nama_usaha} outlet to pick up your laundry.\n\n" +
      "Thank you for trusting us with your laundry. 😊"

  fun defaultTemplateFor(lang: AppLanguage): String =
    if (lang == AppLanguage.EN) DEFAULT_WHATSAPP_TEMPLATE_EN else DEFAULT_WHATSAPP_TEMPLATE

  val TEMPLATE_PLACEHOLDERS: List<Pair<String, String>> = listOf(
    "{nama_pelanggan}" to "Nama Pelanggan",
    "{no_nota}" to "Nomor Nota",
    "{rincian_pesanan}" to "Rincian Cucian",
    "{subtotal}" to "Subtotal",
    "{total}" to "Total Tagihan",
    "{dibayar}" to "Sudah Dibayar",
    "{sisa}" to "Sisa Pembayaran",
    "{status_bayar}" to "Status Bayar",
    "{estimasi_selesai}" to "Estimasi Selesai",
    "{nama_usaha}" to "Nama Outlet"
  )

  fun localizedPlaceholders(lang: AppLanguage): List<Pair<String, String>> {
    if (lang != AppLanguage.EN) return TEMPLATE_PLACEHOLDERS
    return listOf(
      "{nama_pelanggan}" to "Customer Name",
      "{no_nota}" to "Receipt Number",
      "{rincian_pesanan}" to "Order Details",
      "{subtotal}" to "Subtotal",
      "{total}" to "Total Bill",
      "{dibayar}" to "Amount Paid",
      "{sisa}" to "Remaining Balance",
      "{status_bayar}" to "Payment Status",
      "{estimasi_selesai}" to "Est. Completion",
      "{nama_usaha}" to "Outlet Name"
    )
  }

  /**
   * Generates a polite, accurate WhatsApp message strictly from real transaction data,
   * using customTemplate if provided by the owner.
   */
  fun buildReadyWhatsAppMessage(
    order: LaundryOrder,
    shopName: String = "ADA LAUNDRY",
    customTemplate: String = "",
    lang: AppLanguage = AppLanguage.ID
  ): String {
    val isEn = lang == AppLanguage.EN
    val items = order.parsedItems
    val categorySummary = items
      .map { ServiceCategory.fromString(it.category).localizedLabel(isEn) }
      .distinct()
      .joinToString(", ")

    val detailLines = if (items.size == 1) {
      val single = items.first()
      val qtyText = Formatters.formatQuantity(single.quantity, localizedUnit(single.unitLabel, lang))
      val unitPriceText = Formatters.formatRupiah(single.unitPrice)
      buildString {
        appendLine(if (isEn) "Service: $categorySummary - ${single.serviceName}" else "Layanan: $categorySummary - ${single.serviceName}")
        appendLine(if (isEn) "Weight/Quantity: $qtyText" else "Berat/Jumlah: $qtyText")
        append(if (isEn) "Unit Price: $unitPriceText" else "Harga Satuan: $unitPriceText")
      }
    } else {
      buildString {
        appendLine(if (isEn) "Service: $categorySummary" else "Layanan: $categorySummary")
        appendLine(if (isEn) "Service/Item Details:" else "Rincian Layanan/Barang:")
        items.forEachIndexed { index, item ->
          append("${index + 1}. ${formatOrderItemLine(item, lang)}")
          if (index < items.lastIndex) appendLine()
        }
      }
    }

    val discountLine = if (order.discount > 0L) {
      val label = if (isEn) "Discount" else "Diskon"
      "$label: -${Formatters.formatRupiah(order.discount)}\n"
    } else ""

    val extraLabel = order.extraFeeLabel.ifBlank { if (isEn) "Extra Fee" else "Biaya Tambahan" }
    val extraLine = if (order.extraFee > 0L) {
      val prefix = if (isEn) "Extra Fee" else "Biaya Tambahan"
      "$prefix ($extraLabel): ${Formatters.formatRupiah(order.extraFee)}\n"
    } else ""

    val rateText = if (order.taxRatePercent % 1.0 == 0.0) {
      order.taxRatePercent.toInt().toString()
    } else {
      order.taxRatePercent.toString()
    }
    val taxLine = if (order.taxAmount > 0L) {
      val taxLabel = if (isEn) "Tax" else "PPN"
      "$taxLabel ($rateText%): ${Formatters.formatRupiah(order.taxAmount)}\n"
    } else ""

    val activeTemplate = customTemplate.trim().ifEmpty { defaultTemplateFor(lang) }

    return activeTemplate
      .replace("{nama_pelanggan}", order.customerName)
      .replace("{no_nota}", order.notaNumber)
      .replace("{rincian_pesanan}", detailLines)
      .replace("{subtotal}", Formatters.formatRupiah(order.subtotal))
      .replace("{baris_diskon}", discountLine)
      .replace("{diskon}", if (order.discount > 0L) "-${Formatters.formatRupiah(order.discount)}" else "Rp0")
      .replace("{baris_tambahan}", extraLine)
      .replace("{biaya_tambahan}", Formatters.formatRupiah(order.extraFee))
      .replace("{baris_ppn}", taxLine)
      .replace("{ppn}", Formatters.formatRupiah(order.taxAmount))
      .replace("{total}", Formatters.formatRupiah(order.totalAmount))
      .replace("{dibayar}", Formatters.formatRupiah(order.paidAmount))
      .replace("{sisa}", Formatters.formatRupiah(order.remainingAmount))
      .replace("{status_bayar}", order.paymentStatusEnum.localizedLabel(isEn))
      .replace("{estimasi_selesai}", Formatters.formatDateTime(order.estimatedFinishAtMillis, lang))
      .replace("{nama_usaha}", shopName.ifBlank { "ADA LAUNDRY" })
  }

  fun buildWhatsAppUri(normalizedPhone: String, message: String): Uri {
    val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
    return Uri.parse("https://wa.me/$normalizedPhone?text=$encoded")
  }
}
