package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.model.Expense
import com.example.data.model.LaundryOrder
import com.example.data.model.ReportSummary
import com.example.data.model.ShopProfile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReceiptAndReportExporter {

  private fun createExportCacheFile(context: Context, fileName: String): File {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    return File(dir, fileName)
  }

  private fun saveBytesToDownloads(
    context: Context,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
    lang: AppLanguage = AppLanguage.ID
  ): Result<String> {
    val isEn = lang == AppLanguage.EN
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, fileName)
          put(MediaStore.Downloads.MIME_TYPE, mimeType)
          put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ADA_LAUNDRY")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
          ?: throw IllegalStateException(if (isEn) "Failed to create file in Downloads." else "Gagal membuat berkas di folder Downloads.")
        resolver.openOutputStream(uri)?.use { out ->
          out.write(bytes)
        } ?: throw IllegalStateException(if (isEn) "Failed to write file." else "Gagal menulis berkas.")
        Result.success("Download/ADA_LAUNDRY/$fileName")
      } else {
        val dir = File(
          context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
          "ADA_LAUNDRY"
        ).apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        Result.success(file.absolutePath)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    val uri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
  }

  fun buildReceiptPdfBytes(
    context: Context,
    order: LaundryOrder,
    shop: ShopProfile,
    paperSizeOverride: com.example.data.local.ReceiptPaperSize? = null,
    lang: AppLanguage = AppLanguage.ID
  ): ByteArray {
    val isEn = lang == AppLanguage.EN
    val paper = paperSizeOverride ?: com.example.data.local.ReceiptPaperSize.fromString(shop.paperSize)
    val doc = PdfDocument()
    val pageWidth = paper.widthPx
    val isNarrow = pageWidth <= 340
    val margin = if (isNarrow) 16f else 24f
    val items = order.parsedItems
    val pageHeight = (640 + items.size * 34).coerceAtLeast(680)
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#0D5BD9")
      textSize = if (isNarrow) 14f else 18f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#0F172A")
      textSize = if (isNarrow) 9.5f else 11f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#334155")
      textSize = if (isNarrow) 9f else 10.5f
      typeface = Typeface.DEFAULT
    }
    val rightPaint = Paint(bodyPaint).apply {
      textAlign = Paint.Align.RIGHT
    }
    val rightBoldPaint = Paint(boldPaint).apply {
      textAlign = Paint.Align.RIGHT
    }
    val linePaint = Paint().apply {
      color = Color.parseColor("#CBD5E1")
      strokeWidth = 1f
    }

    var y = 22f
    val left = margin
    val right = pageWidth - margin
    val maxHeaderTextChars = if (isNarrow) 30 else 46

    var headerTextLeft = left
    if (shop.receiptShowLogo) {
      val logoSize = if (isNarrow) 34 else 40
      try {
        val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_brand_logo)
        if (bmp != null) {
          canvas.drawBitmap(
            bmp,
            null,
            Rect(left.toInt(), 18, left.toInt() + logoSize, 18 + logoSize),
            null
          )
        }
      } catch (_: Exception) {
      }
      headerTextLeft = left + logoSize + 10f
    }

    var headerY = 32f
    if (shop.receiptShowName) {
      canvas.drawText(shop.businessName.ifBlank { "ADA LAUNDRY" }.take(maxHeaderTextChars), headerTextLeft, headerY, titlePaint)
      headerY += 14f
    }
    if (shop.receiptShowAddress && shop.address.isNotBlank()) {
      canvas.drawText(shop.address.take(maxHeaderTextChars), headerTextLeft, headerY, bodyPaint)
      headerY += 13f
    }
    if (shop.receiptShowPhone && shop.phone.isNotBlank()) {
      val telPrefix = if (isEn) "Phone/WA" else "Telp/WA"
      canvas.drawText("$telPrefix: ${shop.phone}".take(maxHeaderTextChars), headerTextLeft, headerY, bodyPaint)
      headerY += 13f
    }
    y = maxOf(headerY + 8f, if (shop.receiptShowLogo) 68f else 48f)
    canvas.drawLine(left, y, right, y, linePaint)
    y += 16f

    canvas.drawText(if (isEn) "DIGITAL RECEIPT" else "NOTA DIGITAL", left, y, boldPaint)
    canvas.drawText(order.notaNumber, right, y, rightBoldPaint)
    y += 15f
    canvas.drawText(if (isEn) "Date:" else "Tanggal:", left, y, bodyPaint)
    canvas.drawText(Formatters.formatDateTime(order.transactionAtMillis, lang), right, y, rightPaint)
    y += 14f
    if (shop.receiptShowCustomer) {
      canvas.drawText(if (isEn) "Customer:" else "Pelanggan:", left, y, bodyPaint)
      canvas.drawText(order.customerName.take(24), right, y, rightPaint)
      y += 14f
      canvas.drawText("WhatsApp:", left, y, bodyPaint)
      canvas.drawText(order.customerPhone, right, y, rightPaint)
      y += 14f
    }
    canvas.drawText(if (isEn) "Cashier:" else "Kasir:", left, y, bodyPaint)
    canvas.drawText(order.cashierName.take(22), right, y, rightPaint)
    y += 14f
    canvas.drawText(if (isEn) "Est. Ready:" else "Estimasi Selesai:", left, y, bodyPaint)
    canvas.drawText(Formatters.formatDateTime(order.estimatedFinishAtMillis, lang), right, y, rightPaint)
    y += 14f
    canvas.drawText("Status:", left, y, bodyPaint)
    canvas.drawText(order.orderStatusEnum.localizedLabel(isEn), right, y, rightBoldPaint)
    y += 11f
    canvas.drawLine(left, y, right, y, linePaint)
    y += 16f

    canvas.drawText(if (isEn) "SERVICE DETAILS" else "RINCIAN LAYANAN", left, y, boldPaint)
    y += 15f
    val maxServiceChars = if (isNarrow) 22 else 34
    items.forEach { item ->
      canvas.drawText(item.serviceName.take(maxServiceChars), left, y, boldPaint)
      canvas.drawText(Formatters.formatRupiah(item.lineTotal), right, y, rightBoldPaint)
      y += 13f
      val qtyAndUnit = "${Formatters.formatQuantity(item.quantity, localizedUnit(item.unitLabel, isEn))} x ${Formatters.formatRupiah(item.unitPrice)}"
      canvas.drawText(qtyAndUnit, left + 6f, y, bodyPaint)
      y += 16f
    }

    canvas.drawLine(left, y, right, y, linePaint)
    y += 15f
    canvas.drawText("Subtotal", left, y, bodyPaint)
    canvas.drawText(Formatters.formatRupiah(order.subtotal), right, y, rightPaint)
    y += 14f
    if (order.discount > 0L) {
      canvas.drawText(if (isEn) "Discount" else "Diskon", left, y, bodyPaint)
      canvas.drawText("-${Formatters.formatRupiah(order.discount)}", right, y, rightPaint)
      y += 14f
    }
    if (order.extraFee > 0L) {
      val label = order.extraFeeLabel.ifBlank { if (isEn) "Extra Fee" else "Biaya Tambahan" }
      canvas.drawText(label.take(24), left, y, bodyPaint)
      canvas.drawText(Formatters.formatRupiah(order.extraFee), right, y, rightPaint)
      y += 14f
    }
    if (order.taxAmount > 0L) {
      val rateText = if (order.taxRatePercent % 1.0 == 0.0) {
        order.taxRatePercent.toInt().toString()
      } else {
        order.taxRatePercent.toString()
      }
      canvas.drawText("${if (isEn) "Tax" else "PPN"} ($rateText%)", left, y, bodyPaint)
      canvas.drawText(Formatters.formatRupiah(order.taxAmount), right, y, rightPaint)
      y += 14f
    }
    canvas.drawText(if (isEn) "TOTAL BILL" else "TOTAL TAGIHAN", left, y, boldPaint)
    canvas.drawText(Formatters.formatRupiah(order.totalAmount), right, y, rightBoldPaint)
    y += 15f
    canvas.drawText(
      "${if (isEn) "Paid" else "Dibayar"} (${order.paymentMethodEnum.localizedLabel(isEn)})",
      left,
      y,
      bodyPaint
    )
    canvas.drawText(Formatters.formatRupiah(order.paidAmount), right, y, rightPaint)
    y += 14f
    canvas.drawText(if (isEn) "Remaining Balance" else "Sisa Pembayaran", left, y, boldPaint)
    canvas.drawText(Formatters.formatRupiah(order.remainingAmount), right, y, rightBoldPaint)
    y += 14f
    canvas.drawText(if (isEn) "Payment Status" else "Status Pembayaran", left, y, bodyPaint)
    canvas.drawText(order.paymentStatusEnum.localizedLabel(isEn), right, y, rightBoldPaint)
    y += 16f

    if (order.notes.isNotBlank()) {
      canvas.drawLine(left, y, right, y, linePaint)
      y += 14f
      val notePrefix = if (isEn) "Notes" else "Catatan"
      canvas.drawText("$notePrefix: ${order.notes.take(44)}", left, y, bodyPaint)
      y += 15f
    }

    canvas.drawLine(left, y, right, y, linePaint)
    y += 16f
    val defaultFooter = if (isEn) "Thank you for trusting us with your laundry." else "Terima kasih telah mempercayakan cucian Anda."
    val footer = if (
      shop.receiptFooterMessage.isBlank() ||
      (isEn && shop.receiptFooterMessage.trim() == "Terima kasih telah mempercayakan cucian Anda.")
    ) {
      defaultFooter
    } else {
      shop.receiptFooterMessage
    }
    canvas.drawText(footer.take(if (isNarrow) 38 else 56), left, y, bodyPaint)
    y += 13f
    val hoursLabel = if (isEn) "Operating Hours" else "Jam Operasional"
    canvas.drawText("$hoursLabel: ${shop.operatingHours}", left, y, bodyPaint)

    doc.finishPage(page)
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
  }

  fun saveReceiptPdf(
    context: Context,
    order: LaundryOrder,
    shop: ShopProfile,
    paperSizeOverride: com.example.data.local.ReceiptPaperSize? = null,
    lang: AppLanguage = AppLanguage.ID
  ): Result<String> {
    val bytes = buildReceiptPdfBytes(context, order, shop, paperSizeOverride, lang)
    val safeNota = order.notaNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val prefix = if (lang == AppLanguage.EN) "Receipt" else "Nota"
    return saveBytesToDownloads(context, "${prefix}_${safeNota}.pdf", "application/pdf", bytes, lang)
  }

  fun shareReceiptPdf(
    context: Context,
    order: LaundryOrder,
    shop: ShopProfile,
    paperSizeOverride: com.example.data.local.ReceiptPaperSize? = null,
    lang: AppLanguage = AppLanguage.ID
  ) {
    val isEn = lang == AppLanguage.EN
    val bytes = buildReceiptPdfBytes(context, order, shop, paperSizeOverride, lang)
    val safeNota = order.notaNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val prefix = if (isEn) "Receipt" else "Nota"
    val file = createExportCacheFile(context, "${prefix}_${safeNota}.pdf")
    FileOutputStream(file).use { it.write(bytes) }
    shareFile(
      context,
      file,
      "application/pdf",
      if (isEn) "Share Receipt ${order.notaNumber}" else "Bagikan Nota ${order.notaNumber}"
    )
  }

  fun printReceipt(
    context: Context,
    order: LaundryOrder,
    shop: ShopProfile,
    paperSizeOverride: com.example.data.local.ReceiptPaperSize? = null,
    lang: AppLanguage = AppLanguage.ID
  ) {
    val isEn = lang == AppLanguage.EN
    val bytes = buildReceiptPdfBytes(context, order, shop, paperSizeOverride, lang)
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val docName = if (isEn) "Receipt_${order.notaNumber}.pdf" else "Nota_${order.notaNumber}.pdf"
    val adapter = object : PrintDocumentAdapter() {
      override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
      ) {
        if (cancellationSignal?.isCanceled == true) {
          callback.onLayoutCancelled()
          return
        }
        val info = PrintDocumentInfo.Builder(docName)
          .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
          .setPageCount(1)
          .build()
        callback.onLayoutFinished(info, true)
      }

      override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
      ) {
        try {
          FileOutputStream(destination.fileDescriptor).use { it.write(bytes) }
          callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
          callback.onWriteFailed(e.message)
        }
      }
    }
    printManager.print(if (isEn) "Receipt ${order.notaNumber}" else "Nota ${order.notaNumber}", adapter, null)
  }

  fun exportReportPdf(
    context: Context,
    shop: ShopProfile,
    periodTitle: String,
    summary: ReportSummary,
    orders: List<LaundryOrder>,
    expenses: List<Expense>,
    shareAfterSave: Boolean,
    lang: AppLanguage = AppLanguage.ID
  ): Result<String> {
    val isEn = lang == AppLanguage.EN
    return try {
      val doc = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
      val page = doc.startPage(pageInfo)
      val canvas = page.canvas

      val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D5BD9")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val h2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        textSize = 10.5f
      }
      val rightBody = Paint(body).apply { textAlign = Paint.Align.RIGHT }
      val rightBold = Paint(h2).apply { textAlign = Paint.Align.RIGHT }
      val linePaint = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 1f }

      val left = 36f
      val right = 559f
      var y = 44f

      val reportHeading = if (isEn) "FINANCIAL REPORT" else "LAPORAN KEUANGAN"
      canvas.drawText("$reportHeading - ${shop.businessName.ifBlank { "ADA LAUNDRY" }}", left, y, h1)
      y += 18f
      val printedLabel = if (isEn) "Period: $periodTitle | Printed: ${Formatters.formatDateTime(System.currentTimeMillis(), lang)}"
      else "Periode: $periodTitle | Dicetak: ${Formatters.formatDateTime(System.currentTimeMillis(), lang)}"
      canvas.drawText(printedLabel, left, y, body)
      y += 14f
      canvas.drawLine(left, y, right, y, linePaint)
      y += 22f

      val rows = if (isEn) {
        listOf(
          "Total Revenue (Order Value)" to Formatters.formatRupiah(summary.totalOmzet),
          "Total Active Transactions" to "${summary.totalTransactions} transactions",
          "Incoming Cash Payments" to Formatters.formatRupiah(summary.totalCashPaid),
          "Incoming Bank Transfer Payments" to Formatters.formatRupiah(summary.totalTransferPaid),
          "Manual QRIS Records" to Formatters.formatRupiah(summary.totalQrisPaid),
          "Total Full Payments" to Formatters.formatRupiah(summary.totalLunasAmount),
          "Total Deposits / DP" to Formatters.formatRupiah(summary.totalDpAmount),
          "Total Unpaid Receivables" to Formatters.formatRupiah(summary.totalUnpaidReceivables),
          "Total Business Expenses" to Formatters.formatRupiah(summary.totalExpenses),
          "Estimated Net Profit (Cash In - Expenses)" to Formatters.formatRupiah(summary.estimatedNetProfit)
        )
      } else {
        listOf(
          "Total Omzet (Nilai Transaksi)" to Formatters.formatRupiah(summary.totalOmzet),
          "Total Transaksi Aktif" to "${summary.totalTransactions} transaksi",
          "Total Pemasukan Masuk (Tunai)" to Formatters.formatRupiah(summary.totalCashPaid),
          "Total Pemasukan Masuk (Transfer Bank)" to Formatters.formatRupiah(summary.totalTransferPaid),
          "Total Pencatatan Manual QRIS" to Formatters.formatRupiah(summary.totalQrisPaid),
          "Total Pembayaran Lunas" to Formatters.formatRupiah(summary.totalLunasAmount),
          "Total Uang Muka / DP" to Formatters.formatRupiah(summary.totalDpAmount),
          "Total Sisa Tagihan Pelanggan" to Formatters.formatRupiah(summary.totalUnpaidReceivables),
          "Total Pengeluaran Usaha" to Formatters.formatRupiah(summary.totalExpenses),
          "Estimasi Laba (Kas Masuk - Pengeluaran)" to Formatters.formatRupiah(summary.estimatedNetProfit)
        )
      }

      canvas.drawText(if (isEn) "FINANCIAL SUMMARY" else "RINGKASAN KEUANGAN", left, y, h2)
      y += 18f
      rows.forEachIndexed { idx, (label, value) ->
        val pLeft = if (idx == rows.lastIndex) h2 else body
        val pRight = if (idx == rows.lastIndex) rightBold else rightBody
        canvas.drawText(label, left, y, pLeft)
        canvas.drawText(value, right, y, pRight)
        y += 17f
      }

      y += 10f
      canvas.drawLine(left, y, right, y, linePaint)
      y += 20f
      canvas.drawText(if (isEn) "BREAKDOWN BY SERVICE CATEGORY" else "RINCIAN PER KATEGORI LAYANAN", left, y, h2)
      y += 18f
      val orderWord = if (isEn) "orders" else "pesanan"
      canvas.drawText(
        "${if (isEn) "Weight Laundry (Kg)" else "Laundry Kiloan"} (${summary.kiloanCount} $orderWord)",
        left,
        y,
        body
      )
      canvas.drawText(Formatters.formatRupiah(summary.kiloanRevenue), right, y, rightBody)
      y += 16f
      canvas.drawText(
        "${if (isEn) "Per-Item Laundry" else "Laundry Satuan"} (${summary.satuanCount} $orderWord)",
        left,
        y,
        body
      )
      canvas.drawText(Formatters.formatRupiah(summary.satuanRevenue), right, y, rightBody)
      y += 16f
      canvas.drawText(
        "${if (isEn) "Carpet Laundry Per Sheet" else "Laundry Karpet Per Lembar"} (${summary.karpetCount} $orderWord)",
        left,
        y,
        body
      )
      canvas.drawText(Formatters.formatRupiah(summary.karpetRevenue), right, y, rightBody)

      y += 22f
      canvas.drawLine(left, y, right, y, linePaint)
      y += 20f
      canvas.drawText(if (isEn) "MOST FREQUENTLY USED SERVICES" else "LAYANAN PALING SERING DIGUNAKAN", left, y, h2)
      y += 18f
      summary.topServices.take(8).forEach { stat ->
        canvas.drawText(
          "${stat.serviceName} (${stat.orderCount}x - ${Formatters.formatQuantity(stat.totalQuantity, localizedUnit(stat.unitLabel, isEn))})",
          left,
          y,
          body
        )
        canvas.drawText(Formatters.formatRupiah(stat.totalRevenue), right, y, rightBody)
        y += 16f
      }

      y += 18f
      canvas.drawLine(left, y, right, y, linePaint)
      y += 18f
      val recentHeading = if (isEn) {
        "Recent Transactions (${orders.take(10).size} of ${orders.size}):"
      } else {
        "Transaksi Terbaru (${orders.take(10).size} dari ${orders.size}):"
      }
      canvas.drawText(recentHeading, left, y, h2)
      y += 16f
      orders.take(10).forEach { ord ->
        if (y < 780f) {
          canvas.drawText("${ord.notaNumber} | ${ord.customerName.take(20)} | ${ord.orderStatusEnum.localizedLabel(isEn)}", left, y, body)
          canvas.drawText(Formatters.formatRupiah(ord.totalAmount), right, y, rightBody)
          y += 14f
        }
      }

      doc.finishPage(page)
      val baos = ByteArrayOutputStream()
      doc.writeTo(baos)
      doc.close()
      val bytes = baos.toByteArray()
      val prefix = if (isEn) "Report" else "Laporan"
      val fileName = "${prefix}_ADA_LAUNDRY_${System.currentTimeMillis()}.pdf"

      if (shareAfterSave) {
        val file = createExportCacheFile(context, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        shareFile(context, file, "application/pdf", if (isEn) "Share PDF Report" else "Bagikan Laporan PDF")
        Result.success(file.name)
      } else {
        saveBytesToDownloads(context, fileName, "application/pdf", bytes, lang)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Generates a real OpenXML Excel (.xlsx) workbook with 3 worksheets:
   * 1. Summary
   * 2. Transactions
   * 3. Expenses
   */
  fun exportReportExcelXlsx(
    context: Context,
    shop: ShopProfile,
    periodTitle: String,
    summary: ReportSummary,
    orders: List<LaundryOrder>,
    expenses: List<Expense>,
    shareAfterSave: Boolean,
    lang: AppLanguage = AppLanguage.ID
  ): Result<String> {
    val isEn = lang == AppLanguage.EN
    return try {
      val bytes = buildXlsxBytes(shop, periodTitle, summary, orders, expenses, lang)
      val prefix = if (isEn) "Report" else "Laporan"
      val fileName = "${prefix}_ADA_LAUNDRY_${System.currentTimeMillis()}.xlsx"
      val mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

      if (shareAfterSave) {
        val file = createExportCacheFile(context, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        shareFile(context, file, mime, if (isEn) "Share Excel Report (.xlsx)" else "Bagikan Laporan Excel (.xlsx)")
        Result.success(file.name)
      } else {
        saveBytesToDownloads(context, fileName, mime, bytes, lang)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun buildXlsxBytes(
    shop: ShopProfile,
    periodTitle: String,
    summary: ReportSummary,
    orders: List<LaundryOrder>,
    expenses: List<Expense>,
    lang: AppLanguage = AppLanguage.ID
  ): ByteArray {
    val isEn = lang == AppLanguage.EN
    fun escapeXml(s: String): String = s
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

    fun colName(index: Int): String {
      var n = index
      val sb = StringBuilder()
      while (n >= 0) {
        sb.append(('A' + (n % 26)))
        n = n / 26 - 1
      }
      return sb.reverse().toString()
    }

    fun buildSheetXml(rows: List<List<Any>>): String {
      val sb = StringBuilder()
      sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
      sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
      rows.forEachIndexed { rIdx, cols ->
        val rowNum = rIdx + 1
        sb.append("""<row r="$rowNum">""")
        cols.forEachIndexed { cIdx, cellVal ->
          val ref = "${colName(cIdx)}$rowNum"
          when (cellVal) {
            is Number -> sb.append("""<c r="$ref"><v>$cellVal</v></c>""")
            else -> sb.append("""<c r="$ref" t="inlineStr"><is><t>${escapeXml(cellVal.toString())}</t></is></c>""")
          }
        }
        sb.append("</row>")
      }
      sb.append("</sheetData></worksheet>")
      return sb.toString()
    }

    val sheet1Rows = if (isEn) {
      listOf(
        listOf("FINANCIAL REPORT", shop.businessName.ifBlank { "ADA LAUNDRY" }),
        listOf("Period", periodTitle),
        listOf("Metric", "Value"),
        listOf("Total Revenue", summary.totalOmzet),
        listOf("Total Transactions", summary.totalTransactions),
        listOf("Cash Payments", summary.totalCashPaid),
        listOf("Bank Transfer Payments", summary.totalTransferPaid),
        listOf("Manual QRIS Records", summary.totalQrisPaid),
        listOf("Total Incoming Payments", summary.totalPaidIncome),
        listOf("Total Full Payments", summary.totalLunasAmount),
        listOf("Total Deposits / DP", summary.totalDpAmount),
        listOf("Total Unpaid Receivables", summary.totalUnpaidReceivables),
        listOf("Total Expenses", summary.totalExpenses),
        listOf("Estimated Net Profit (Cash In - Expenses)", summary.estimatedNetProfit),
        listOf("Weight Laundry Revenue", summary.kiloanRevenue),
        listOf("Per-Item Laundry Revenue", summary.satuanRevenue),
        listOf("Carpet Laundry Revenue", summary.karpetRevenue)
      )
    } else {
      listOf(
        listOf("LAPORAN KEUANGAN", shop.businessName.ifBlank { "ADA LAUNDRY" }),
        listOf("Periode", periodTitle),
        listOf("Metrik", "Nilai"),
        listOf("Total Omzet", summary.totalOmzet),
        listOf("Total Transaksi", summary.totalTransactions),
        listOf("Pembayaran Tunai", summary.totalCashPaid),
        listOf("Pembayaran Transfer Bank", summary.totalTransferPaid),
        listOf("Pencatatan Manual QRIS", summary.totalQrisPaid),
        listOf("Total Pembayaran Masuk", summary.totalPaidIncome),
        listOf("Total Pembayaran Lunas", summary.totalLunasAmount),
        listOf("Total Uang Muka / DP", summary.totalDpAmount),
        listOf("Total Sisa Tagihan Pelanggan", summary.totalUnpaidReceivables),
        listOf("Total Pengeluaran", summary.totalExpenses),
        listOf("Estimasi Laba (Kas Masuk - Pengeluaran)", summary.estimatedNetProfit),
        listOf("Omzet Kiloan", summary.kiloanRevenue),
        listOf("Omzet Satuan", summary.satuanRevenue),
        listOf("Omzet Karpet", summary.karpetRevenue)
      )
    }

    val sheet2Rows = buildList<List<Any>> {
      add(
        if (isEn) {
          listOf("Receipt No", "Date", "Customer", "WhatsApp", "Services", "Subtotal", "Discount", "Extra Fee", "Total", "Paid", "Remaining", "Method", "Payment Status", "Order Status", "Cashier")
        } else {
          listOf("No. Nota", "Tanggal", "Pelanggan", "WhatsApp", "Layanan", "Subtotal", "Diskon", "Biaya Tambahan", "Total", "Dibayar", "Sisa", "Metode", "Status Bayar", "Status Pesanan", "Kasir")
        }
      )
      orders.forEach { o ->
        val svcText = o.parsedItems.joinToString("; ") { "${it.serviceName} (${Formatters.formatQuantity(it.quantity, localizedUnit(it.unitLabel, isEn))})" }
        add(
          listOf(
            o.notaNumber,
            Formatters.formatDateTime(o.transactionAtMillis, lang),
            o.customerName,
            o.customerPhone,
            svcText,
            o.subtotal,
            o.discount,
            o.extraFee,
            o.totalAmount,
            o.paidAmount,
            o.remainingAmount,
            o.paymentMethodEnum.localizedLabel(isEn),
            o.paymentStatusEnum.localizedLabel(isEn),
            o.orderStatusEnum.localizedLabel(isEn),
            o.cashierName
          )
        )
      }
    }

    val sheet3Rows = buildList<List<Any>> {
      add(
        if (isEn) listOf("Date", "Category", "Amount", "Notes")
        else listOf("Tanggal", "Kategori", "Nominal", "Catatan")
      )
      expenses.forEach { e ->
        add(listOf(Formatters.formatDateTime(e.dateMillis, lang), e.category, e.amount, e.notes))
      }
    }

    val sheetNames = if (isEn) Triple("Summary", "Transactions", "Expenses")
    else Triple("Ringkasan", "Transaksi", "Pengeluaran")

    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zip ->
      fun putEntry(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
      }

      putEntry(
        "[Content_Types].xml",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
      )
      putEntry(
        "_rels/.rels",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
      )
      putEntry(
        "xl/workbook.xml",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${sheetNames.first}" sheetId="1" r:id="rId1"/>
    <sheet name="${sheetNames.second}" sheetId="2" r:id="rId2"/>
    <sheet name="${sheetNames.third}" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""
      )
      putEntry(
        "xl/_rels/workbook.xml.rels",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
</Relationships>"""
      )
      putEntry("xl/worksheets/sheet1.xml", buildSheetXml(sheet1Rows))
      putEntry("xl/worksheets/sheet2.xml", buildSheetXml(sheet2Rows))
      putEntry("xl/worksheets/sheet3.xml", buildSheetXml(sheet3Rows))
    }
    return baos.toByteArray()
  }

  fun shareBackupJson(context: Context, jsonContent: String, lang: AppLanguage = AppLanguage.ID) {
    val file = createExportCacheFile(context, "Backup_ADA_LAUNDRY_${System.currentTimeMillis()}.json")
    FileOutputStream(file).use { it.write(jsonContent.toByteArray(Charsets.UTF_8)) }
    shareFile(
      context,
      file,
      "application/json",
      if (lang == AppLanguage.EN) "Save / Share JSON Backup" else "Simpan / Bagikan Backup JSON"
    )
  }

  fun saveBackupJsonToDownloads(context: Context, jsonContent: String, lang: AppLanguage = AppLanguage.ID): Result<String> {
    return saveBytesToDownloads(
      context,
      "Backup_ADA_LAUNDRY_${System.currentTimeMillis()}.json",
      "application/json",
      jsonContent.toByteArray(Charsets.UTF_8),
      lang
    )
  }
}
