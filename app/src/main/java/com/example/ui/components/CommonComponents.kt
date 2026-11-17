package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Iron
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.ReceiptPaperSize
import com.example.data.model.LaundryOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ShopProfile
import com.example.ui.theme.BrandColorOption
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainerLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainerLight
import com.example.util.Formatters
import com.example.util.LocalAppLanguage
import com.example.util.ReceiptAndReportExporter
import com.example.util.WhatsAppHelper
import com.example.util.isEnglish
import com.example.util.localizedUnit
import com.example.util.tr
import java.util.UUID

enum class BusinessIconOption(
  val key: String,
  val label: String,
  val labelEn: String,
  val icon: ImageVector?
) {
  BRAND_LOGO("BRAND_LOGO", "Logo ADA", "ADA Logo", null),
  WASHING_MACHINE("WASHING_MACHINE", "Mesin Cuci", "Washer", Icons.Filled.LocalLaundryService),
  STOREFRONT("STOREFRONT", "Outlet", "Outlet", Icons.Filled.Storefront),
  LAUNDRY_BASKET("LAUNDRY_BASKET", "Keranjang", "Basket", Icons.Filled.ShoppingBasket),
  IRON("IRON", "Setrika", "Iron", Icons.Filled.Iron),
  SPARKLES("SPARKLES", "Kilau", "Sparkle", Icons.Filled.AutoAwesome),
  WATER_DROP("WATER_DROP", "Air Segar", "Fresh Water", Icons.Filled.WaterDrop);

  fun localizedLabel(isEn: Boolean): String = if (isEn) labelEn else label

  companion object {
    fun fromKey(key: String?): BusinessIconOption =
      entries.firstOrNull { it.key == key } ?: WASHING_MACHINE
  }
}

@Composable
fun BusinessAvatarBadge(
  shop: ShopProfile,
  size: Dp = 44.dp,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(size * 0.25f)
  val brandColor = BrandColorOption.fromKey(shop.brandColorKey).lightPrimary
  val iconOpt = BusinessIconOption.fromKey(shop.businessIconKey)

  when {
    shop.logoUri.isNotBlank() -> {
      AsyncImage(
        model = shop.logoUri,
        contentDescription = tr("Foto Usaha", "Shop Photo"),
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape)
      )
    }
    iconOpt.icon != null -> {
      Box(
        modifier = modifier
          .size(size)
          .clip(shape)
          .background(brandColor),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = iconOpt.icon,
          contentDescription = iconOpt.localizedLabel(isEnglish()),
          tint = Color.White,
          modifier = Modifier.size(size * 0.56f)
        )
      }
    }
    else -> {
      Image(
        painter = painterResource(id = R.drawable.img_brand_logo),
        contentDescription = tr("Logo ADA LAUNDRY", "ADA LAUNDRY Logo"),
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape)
      )
    }
  }
}

@Composable
fun SyncStatusBanner(isOnline: Boolean, modifier: Modifier = Modifier) {
  Surface(
    color = if (isOnline) {
      MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
    } else {
      WarningContainerLight
    },
    shape = RoundedCornerShape(10.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
        contentDescription = null,
        tint = if (isOnline) EmeraldAccent else WarningAmber,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = if (isOnline) {
          tr("Online • Tersinkronisasi dengan Cloud Firestore", "Online • Synced with Cloud Firestore")
        } else {
          tr("Mode Offline • Perubahan disimpan lokal dan akan disinkronkan saat online", "Offline Mode • Changes saved locally and will sync when online")
        },
        style = MaterialTheme.typography.labelSmall,
        color = if (isOnline) MaterialTheme.colorScheme.onTertiaryContainer else Color(0xFF78350F),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
  val isEn = isEnglish()
  val (bg, fg) = when (status) {
    OrderStatus.RECEIVED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    OrderStatus.PROCESSING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
    OrderStatus.READY -> EmeraldContainerLight to EmeraldAccent
    OrderStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
  }
  Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
    Text(
      text = status.localizedLabel(isEn),
      color = fg,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun PaymentStatusChip(status: PaymentStatus) {
  val isEn = isEnglish()
  val (bg, fg) = when (status) {
    PaymentStatus.PAID -> EmeraldContainerLight to EmeraldAccent
    PaymentStatus.DP -> WarningContainerLight to WarningAmber
    PaymentStatus.UNPAID -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
  }
  Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
    Text(
      text = status.localizedLabel(isEn),
      color = fg,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun ReceiptPaperPreviewCard(
  order: LaundryOrder,
  shop: ShopProfile,
  paperSize: ReceiptPaperSize = ReceiptPaperSize.fromString(shop.paperSize),
  modifier: Modifier = Modifier
) {
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  val rateText = if (order.taxRatePercent % 1.0 == 0.0) {
    order.taxRatePercent.toInt().toString()
  } else {
    order.taxRatePercent.toString()
  }
  Surface(
    color = Color(0xFFFFFDF8),
    contentColor = Color(0xFF0F172A),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          if (shop.receiptShowLogo) {
            BusinessAvatarBadge(shop = shop, size = 40.dp)
          }
          Column {
            if (shop.receiptShowName) {
              Text(
                text = shop.businessName.ifBlank { "ADA LAUNDRY" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandColorOption.fromKey(shop.brandColorKey).lightPrimary
              )
            }
            if (shop.receiptShowAddress && shop.address.isNotBlank()) {
              Text(
                text = shop.address,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
              )
            }
            if (shop.receiptShowPhone && shop.phone.isNotBlank()) {
              Text(
                text = tr("Telp/WA: ${shop.phone}", "Phone/WA: ${shop.phone}"),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
              )
            }
          }
        }
        Surface(
          color = Color(0xFFE2E8F0),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = paperSize.localizedLabel(isEn),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF334155),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      HorizontalDivider(color = Color(0xFFCBD5E1))
      ReceiptKeyValue(tr("No. Nota", "Receipt No."), order.notaNumber, bold = true)
      ReceiptKeyValue(tr("Tanggal", "Date"), Formatters.formatDateTime(order.transactionAtMillis, lang))
      if (shop.receiptShowCustomer) {
        ReceiptKeyValue(tr("Pelanggan", "Customer"), order.customerName)
        ReceiptKeyValue("WhatsApp", order.customerPhone)
      }
      ReceiptKeyValue(tr("Kasir", "Cashier"), order.cashierName)
      ReceiptKeyValue(tr("Estimasi Selesai", "Est. Pickup"), Formatters.formatDateTime(order.estimatedFinishAtMillis, lang))
      ReceiptKeyValue(tr("Status Pesanan", "Order Status"), order.orderStatusEnum.localizedLabel(isEn), bold = true)

      HorizontalDivider(color = Color(0xFFCBD5E1))
      Text(tr("RINCIAN LAYANAN", "SERVICE DETAILS"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
      order.parsedItems.forEach { item ->
        val unitLbl = localizedUnit(item.unitLabel, isEn)
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.serviceName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Text(Formatters.formatRupiah(item.lineTotal), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
          }
          Text(
            "${Formatters.formatQuantity(item.quantity, unitLbl)} x ${Formatters.formatRupiah(item.unitPrice)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF475569)
          )
        }
      }

      HorizontalDivider(color = Color(0xFFCBD5E1))
      ReceiptKeyValue(tr("Subtotal", "Subtotal"), Formatters.formatRupiah(order.subtotal))
      if (order.discount > 0L) {
        ReceiptKeyValue(tr("Diskon", "Discount"), "-${Formatters.formatRupiah(order.discount)}")
      }
      if (order.extraFee > 0L) {
        ReceiptKeyValue(order.extraFeeLabel.ifBlank { tr("Biaya Tambahan", "Extra Fee") }, Formatters.formatRupiah(order.extraFee))
      }
      if (order.taxAmount > 0L) {
        ReceiptKeyValue(tr("PPN ($rateText%)", "Tax ($rateText%)"), Formatters.formatRupiah(order.taxAmount))
      }
      ReceiptKeyValue(tr("TOTAL TAGIHAN", "TOTAL BILL"), Formatters.formatRupiah(order.totalAmount), bold = true)
      ReceiptKeyValue(tr("Dibayar (${order.paymentMethodEnum.localizedLabel(isEn)})", "Paid (${order.paymentMethodEnum.localizedLabel(isEn)})"), Formatters.formatRupiah(order.paidAmount))
      ReceiptKeyValue(tr("Sisa Pembayaran", "Remaining"), Formatters.formatRupiah(order.remainingAmount), bold = true)
      ReceiptKeyValue(tr("Status Pembayaran", "Payment Status"), order.paymentStatusEnum.localizedLabel(isEn))

      if (order.notes.isNotBlank()) {
        Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
          Text(
            text = tr("Catatan: ${order.notes}", "Notes: ${order.notes}"),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF334155),
            modifier = Modifier.padding(8.dp)
          )
        }
      }

      HorizontalDivider(color = Color(0xFFCBD5E1))
      val footerText = if (
        shop.receiptFooterMessage.isBlank() ||
        (isEn && shop.receiptFooterMessage.trim() == "Terima kasih telah mempercayakan cucian Anda.")
      ) {
        tr("Terima kasih telah mempercayakan cucian Anda.", "Thank you for trusting us with your laundry.")
      } else {
        shop.receiptFooterMessage
      }
      Text(
        text = footerText,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFF334155),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
      Text(
        text = tr("Jam Operasional: ${shop.operatingHours}", "Opening Hours: ${shop.operatingHours}"),
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF64748B),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
fun DigitalReceiptDialog(
  order: LaundryOrder,
  shop: ShopProfile,
  paperSize: ReceiptPaperSize = ReceiptPaperSize.fromString(shop.paperSize),
  onDismiss: () -> Unit,
  onNotify: (String) -> Unit
) {
  val context = LocalContext.current
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 16.dp)
    ) {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            tr("Nota Digital • ${order.notaNumber}", "Digital Receipt • ${order.notaNumber}"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = tr("Tutup Nota", "Close Receipt"))
          }
        }

        ReceiptPaperPreviewCard(
          order = order,
          shop = shop,
          paperSize = paperSize
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              ReceiptAndReportExporter.printReceipt(context, order, shop, paperSize, lang)
            },
            modifier = Modifier.weight(1f).height(48.dp).testTag("print_receipt_button")
          ) {
            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Cetak", "Print"))
          }
          OutlinedButton(
            onClick = {
              ReceiptAndReportExporter.shareReceiptPdf(context, order, shop, paperSize, lang)
            },
            modifier = Modifier.weight(1f).height(48.dp).testTag("share_receipt_button")
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Bagikan", "Share"))
          }
        }
        Button(
          onClick = {
            ReceiptAndReportExporter.saveReceiptPdf(context, order, shop, paperSize, lang)
              .onSuccess { path ->
                onNotify(
                  if (isEn) "Receipt PDF (${paperSize.localizedLabel(true)}) saved to $path"
                  else "Nota PDF (${paperSize.label}) disimpan ke $path"
                )
              }
              .onFailure { err ->
                onNotify(
                  if (isEn) "Failed to save PDF: ${err.message}"
                  else "Gagal menyimpan PDF: ${err.message}"
                )
              }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_receipt_pdf_button")
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(tr("Simpan Nota sebagai PDF", "Save Receipt as PDF"))
        }
      }
    }
  }
}

@Composable
fun PinVerificationDialog(
  expectedPin: String,
  title: String = tr("Masukkan PIN Keamanan Owner", "Enter Owner Security PIN"),
  subtitle: String = tr("Masukkan PIN keamanan 4–6 digit untuk membuka menu ini.", "Enter your 4–6 digit security PIN to open this menu."),
  onVerified: () -> Unit,
  onDismiss: () -> Unit
) {
  val isEn = isEnglish()
  var pinInput by remember { mutableStateOf("") }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = pinInput,
          onValueChange = { input ->
            pinInput = input.filter { it.isDigit() }.take(6)
            errorMsg = null
          },
          label = { Text(tr("PIN Keamanan", "Security PIN")) },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          isError = errorMsg != null,
          supportingText = if (errorMsg != null) {
            { Text(errorMsg!!, color = MaterialTheme.colorScheme.error) }
          } else null,
          modifier = Modifier.fillMaxWidth().testTag("owner_pin_verify_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (pinInput == expectedPin) {
            onVerified()
          } else {
            errorMsg = if (isEn) "Incorrect PIN. Please try again." else "PIN tidak sesuai. Silakan coba lagi."
          }
        },
        enabled = pinInput.length >= 4,
        modifier = Modifier.testTag("owner_pin_verify_button")
      ) {
        Text(tr("Buka", "Unlock"))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) }
    }
  )
}

@Composable
private fun ReceiptKeyValue(label: String, value: String, bold: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color(0xFF475569),
      fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelSmall,
      color = Color(0xFF0F172A),
      fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentDialog(
  order: LaundryOrder,
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onConfirmPayment: (requestId: String, receivedAmount: Long, method: PaymentMethod, isDp: Boolean) -> Unit
) {
  val isEn = isEnglish()
  val paymentRequestId = remember(order.id) { "pay_${UUID.randomUUID().toString().take(8)}" }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var isDpMode by remember { mutableStateOf(false) }
  var amountText by remember { mutableStateOf(order.remainingAmount.toString()) }

  val parsedAmount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val changeAmount = if (!isDpMode && selectedMethod == PaymentMethod.CASH && parsedAmount > order.remainingAmount) {
    parsedAmount - order.remainingAmount
  } else {
    0L
  }
  val isValid = if (isDpMode) {
    parsedAmount in 1..order.remainingAmount
  } else {
    if (selectedMethod == PaymentMethod.CASH) {
      parsedAmount >= order.remainingAmount && order.remainingAmount > 0L
    } else {
      parsedAmount == order.remainingAmount && order.remainingAmount > 0L
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(tr("Catat Pembayaran • ${order.notaNumber}", "Record Payment • ${order.notaNumber}")) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          tr(
            "Total Tagihan: ${Formatters.formatRupiah(order.totalAmount)}\n" +
              "Sudah Dibayar: ${Formatters.formatRupiah(order.paidAmount)}\n" +
              "Sisa Tagihan: ${Formatters.formatRupiah(order.remainingAmount)}",
            "Total Bill: ${Formatters.formatRupiah(order.totalAmount)}\n" +
              "Already Paid: ${Formatters.formatRupiah(order.paidAmount)}\n" +
              "Remaining Bill: ${Formatters.formatRupiah(order.remainingAmount)}"
          ),
          style = MaterialTheme.typography.bodySmall
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = !isDpMode,
            onClick = {
              isDpMode = false
              amountText = order.remainingAmount.toString()
            },
            label = { Text(tr("Pelunasan", "Full Settlement")) }
          )
          FilterChip(
            selected = isDpMode,
            onClick = {
              isDpMode = true
              amountText = ""
            },
            label = { Text(tr("Cicil / DP", "Partial / DP")) }
          )
        }

        Text(tr("Metode Pembayaran", "Payment Method"), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = {
                selectedMethod = method
                if (!isDpMode && method != PaymentMethod.CASH) {
                  amountText = order.remainingAmount.toString()
                }
              },
              label = { Text(method.localizedLabel(isEn)) }
            )
          }
        }

        if (selectedMethod == PaymentMethod.QRIS) {
          Surface(
            color = WarningContainerLight,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              tr(
                "Catatan Manual QRIS: Aplikasi tidak memverifikasi pembayaran QRIS secara otomatis. Pastikan dana sudah masuk di mutasi sebelum menyimpan.",
                "Manual QRIS Note: The app does not verify QRIS payments automatically. Confirm funds arrived in your account before saving."
              ),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF78350F),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = { input -> amountText = input.filter { it.isDigit() } },
          label = {
            Text(
              if (isDpMode) tr("Nominal DP / Cicilan (Rp)", "Partial / DP Amount (Rp)")
              else tr("Uang Diterima (Rp)", "Amount Received (Rp)")
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
        )

        if (changeAmount > 0L) {
          Surface(
            color = EmeraldContainerLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              tr("Kembalian: ${Formatters.formatRupiah(changeAmount)}", "Change: ${Formatters.formatRupiah(changeAmount)}"),
              color = EmeraldAccent,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleSmall,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirmPayment(paymentRequestId, parsedAmount, selectedMethod, isDpMode)
        },
        enabled = isValid && !isBusy,
        modifier = Modifier.testTag("confirm_payment_button")
      ) {
        Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (isBusy) tr("Menyimpan...", "Saving...") else tr("Simpan Pembayaran", "Save Payment"))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isBusy) {
        Text(tr("Batal", "Cancel"))
      }
    }
  )
}

fun launchWhatsAppForOrder(
  context: android.content.Context,
  order: LaundryOrder,
  shopName: String,
  customTemplate: String = "",
  lang: AppLanguage = AppLanguage.ID,
  onOpened: (String) -> Unit,
  onInvalidPhone: (String) -> Unit
) {
  val isEn = lang == AppLanguage.EN
  val normalized = WhatsAppHelper.normalizeIndonesianPhone(order.customerPhone)
  if (normalized == null) {
    onInvalidPhone(
      if (isEn) "WhatsApp number '${order.customerPhone}' is empty or invalid. Please update the customer's number first."
      else "Nomor WhatsApp '${order.customerPhone}' kosong atau tidak valid. Perbarui nomor pelanggan terlebih dahulu."
    )
    return
  }
  val message = WhatsAppHelper.buildReadyWhatsAppMessage(order, shopName, customTemplate, lang)
  val uri = WhatsAppHelper.buildWhatsAppUri(normalized, message)
  val intent = Intent(Intent.ACTION_VIEW, uri)
  try {
    context.startActivity(intent)
    onOpened(normalized)
  } catch (_: Exception) {
    onInvalidPhone(
      if (isEn) "Unable to open WhatsApp or browser to send the message."
      else "Tidak dapat membuka aplikasi WhatsApp atau browser untuk mengirim pesan."
    )
  }
}

@Composable
fun WhatsAppInvalidDialog(
  message: String,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
    title = { Text(tr("Nomor WhatsApp Tidak Valid", "Invalid WhatsApp Number")) },
    text = { Text(message) },
    confirmButton = {
      Button(onClick = onDismiss) { Text(tr("Mengerti", "Got it")) }
    }
  )
}
