package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.ReceiptPaperSize
import com.example.data.local.ThemeMode
import com.example.data.model.ActiveShopSession
import com.example.data.model.LaundryOrder
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.ShopProfile
import com.example.data.model.StaffInvite
import com.example.data.model.StaffPermissions
import com.example.data.model.UserRole
import com.example.ui.components.BusinessAvatarBadge
import com.example.ui.components.BusinessIconOption
import com.example.ui.components.ReceiptPaperPreviewCard
import com.example.ui.theme.BrandColorOption
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainerLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainerLight
import com.example.util.LaundryReminderHelper
import com.example.util.LocalAppLanguage
import com.example.util.ReceiptAndReportExporter
import com.example.util.WhatsAppHelper
import com.example.util.isEnglish
import com.example.util.tr
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsModalShell(
  title: String,
  onDismiss: () -> Unit,
  content: @Composable () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 18.dp)
    ) {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = tr("Tutup", "Close"))
          }
        }
        HorizontalDivider()
        content()
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopProfileSettingsDialog(
  shopProfile: ShopProfile,
  isOwner: Boolean,
  isBusy: Boolean,
  onSave: (ShopProfile) -> Unit,
  onNotify: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isEn = isEnglish()
  var businessName by remember(shopProfile) { mutableStateOf(shopProfile.businessName) }
  var address by remember(shopProfile) { mutableStateOf(shopProfile.address) }
  var phone by remember(shopProfile) { mutableStateOf(shopProfile.phone) }
  var operatingHours by remember(shopProfile) { mutableStateOf(shopProfile.operatingHours) }
  var notaPrefix by remember(shopProfile) { mutableStateOf(shopProfile.notaPrefix) }
  var logoUri by remember(shopProfile) { mutableStateOf(shopProfile.logoUri) }
  var iconKey by remember(shopProfile) { mutableStateOf(shopProfile.businessIconKey) }
  var brandColorKey by remember(shopProfile) { mutableStateOf(shopProfile.brandColorKey) }

  val logoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val destFile = File(context.filesDir, "shop_logo_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
        logoUri = Uri.fromFile(destFile).toString()
        onNotify(
          if (isEn) "Shop photo selected. Tap Save Shop Profile."
          else "Foto usaha dipilih. Tekan Simpan Profil Usaha."
        )
      } catch (e: Exception) {
        onNotify(
          if (isEn) "Failed to load photo: ${e.message}"
          else "Gagal memuat foto: ${e.message}"
        )
      }
    }
  }

  SettingsModalShell(title = tr("Profil Usaha", "Shop Profile"), onDismiss = onDismiss) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      BusinessAvatarBadge(
        shop = shopProfile.copy(
          logoUri = logoUri,
          businessIconKey = iconKey,
          brandColorKey = brandColorKey
        ),
        size = 64.dp
      )
      if (isOwner) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          OutlinedButton(
            onClick = {
              logoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.height(44.dp).testTag("pick_logo_button")
          ) {
            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Upload Foto Usaha", "Upload Shop Photo"))
          }
          if (logoUri.isNotBlank()) {
            TextButton(onClick = { logoUri = "" }) {
              Text(tr("Hapus Foto & Pakai Icon", "Remove Photo & Use Icon"))
            }
          }
        }
      }
    }

    Text(tr("Pilih Icon Usaha", "Choose Shop Icon"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      BusinessIconOption.entries.forEach { opt ->
        val selected = iconKey == opt.key
        val optLabel = opt.localizedLabel(isEn)
        Surface(
          onClick = { if (isOwner) { iconKey = opt.key; logoUri = "" } },
          shape = RoundedCornerShape(10.dp),
          color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (opt.icon != null) {
              Icon(opt.icon, contentDescription = optLabel, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            } else {
              Image(
                painter = painterResource(R.drawable.img_brand_logo),
                contentDescription = optLabel,
                modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
              )
            }
            Text(optLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    Text(tr("Warna Background & Aksen Usaha", "Theme Color & Shop Accent"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      BrandColorOption.entries.forEach { colorOpt ->
        val selected = brandColorKey == colorOpt.key
        val colorLabel = colorOpt.localizedLabel(isEn)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.clickable(enabled = isOwner) { brandColorKey = colorOpt.key }
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(colorOpt.lightPrimary)
              .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.White, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            if (selected) {
              Icon(Icons.Filled.Check, contentDescription = colorLabel, tint = Color.White, modifier = Modifier.size(20.dp))
            }
          }
          Text(colorLabel, style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    OutlinedTextField(
      value = businessName,
      onValueChange = { if (isOwner) businessName = it },
      enabled = isOwner,
      label = { Text(tr("Nama Usaha Laundry", "Laundry Shop Name")) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("settings_business_name_input")
    )
    OutlinedTextField(
      value = address,
      onValueChange = { if (isOwner) address = it },
      enabled = isOwner,
      label = { Text(tr("Alamat Usaha", "Shop Address")) },
      modifier = Modifier.fillMaxWidth().testTag("settings_address_input")
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        value = phone,
        onValueChange = { if (isOwner) phone = it },
        enabled = isOwner,
        label = { Text(tr("Telepon / WA Outlet", "Outlet Phone / WA")) },
        singleLine = true,
        modifier = Modifier.weight(1f)
      )
      OutlinedTextField(
        value = notaPrefix,
        onValueChange = { if (isOwner) notaPrefix = it.uppercase().take(12) },
        enabled = isOwner,
        label = { Text(tr("Prefix Nota", "Receipt Prefix")) },
        singleLine = true,
        modifier = Modifier.weight(0.6f).testTag("settings_prefix_input")
      )
    }
    OutlinedTextField(
      value = operatingHours,
      onValueChange = { if (isOwner) operatingHours = it },
      enabled = isOwner,
      label = { Text(tr("Jam Operasional", "Operating Hours")) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    if (isOwner) {
      Button(
        onClick = {
          onSave(
            shopProfile.copy(
              businessName = businessName.trim(),
              address = address.trim(),
              phone = phone.trim(),
              operatingHours = operatingHours.trim(),
              notaPrefix = notaPrefix.trim().ifBlank { "ADA" },
              logoUri = logoUri,
              businessIconKey = iconKey,
              brandColorKey = brandColorKey
            )
          )
          onDismiss()
        },
        enabled = !isBusy && businessName.trim().length >= 2,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_shop_profile_button")
      ) {
        Icon(Icons.Filled.Save, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(tr("Simpan Profil Usaha", "Save Shop Profile"))
      }
    } else {
      Text(
        tr("Hanya Owner yang dapat mengubah profil usaha.", "Only the Owner can edit the shop profile."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeAndLanguageDialog(
  themeMode: ThemeMode,
  appLanguage: AppLanguage,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onDismiss: () -> Unit
) {
  val isEn = isEnglish()
  SettingsModalShell(title = tr("Tema & Bahasa", "Theme & Language"), onDismiss = onDismiss) {
    Text(tr("Tema Tampilan", "Appearance Theme"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ThemeMode.entries.forEach { mode ->
        FilterChip(
          selected = themeMode == mode,
          onClick = { onSelectThemeMode(mode) },
          label = { Text(mode.localizedLabel(isEn)) },
          modifier = Modifier.testTag("theme_${mode.name.lowercase()}_chip")
        )
      }
    }

    HorizontalDivider()
    Text(tr("Bahasa Aplikasi", "App Language"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AppLanguage.entries.forEach { lang ->
        FilterChip(
          selected = appLanguage == lang,
          onClick = { onSelectLanguage(lang) },
          label = { Text(lang.label) },
          modifier = Modifier.testTag("language_${lang.name.lowercase()}_chip")
        )
      }
    }

    Button(
      onClick = onDismiss,
      modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
      Text(tr("Selesai", "Done"))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaxAndDiscountDialog(
  shopProfile: ShopProfile,
  isOwner: Boolean,
  isBusy: Boolean,
  onSave: (ShopProfile) -> Unit,
  onDismiss: () -> Unit
) {
  var taxEnabled by remember(shopProfile) { mutableStateOf(shopProfile.taxEnabled) }
  var taxAutoApply by remember(shopProfile) { mutableStateOf(shopProfile.taxAutoApply) }
  var taxRateText by remember(shopProfile) {
    mutableStateOf(
      if (shopProfile.taxRatePercent % 1.0 == 0.0) shopProfile.taxRatePercent.toInt().toString()
      else shopProfile.taxRatePercent.toString()
    )
  }
  var discountPercentText by remember(shopProfile) {
    mutableStateOf(
      if (shopProfile.defaultDiscountPercent % 1.0 == 0.0) shopProfile.defaultDiscountPercent.toInt().toString()
      else shopProfile.defaultDiscountPercent.toString()
    )
  }
  var discountAmountText by remember(shopProfile) { mutableStateOf(shopProfile.defaultDiscountAmount.toString()) }

  SettingsModalShell(title = tr("PPN & Diskon", "Tax & Discount"), onDismiss = onDismiss) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(tr("Aktifkan PPN", "Enable Tax"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
          tr("Tambahkan perhitungan PPN pada transaksi kasir dan struk.", "Include tax calculation in cashier transactions and receipts."),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Switch(checked = taxEnabled, onCheckedChange = { if (isOwner) taxEnabled = it })
    }

    if (taxEnabled) {
      Text(tr("Mode Perhitungan PPN", "Tax Calculation Mode"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = taxAutoApply,
          onClick = { if (isOwner) taxAutoApply = true },
          label = { Text(tr("Otomatis", "Automatic")) }
        )
        FilterChip(
          selected = !taxAutoApply,
          onClick = { if (isOwner) taxAutoApply = false },
          label = { Text(tr("Manual (Kasir Pilih)", "Manual (Cashier Choice)")) }
        )
      }
      OutlinedTextField(
        value = taxRateText,
        onValueChange = { if (isOwner) taxRateText = it.filter { c -> c.isDigit() || c == '.' } },
        enabled = isOwner,
        label = { Text(tr("Tarif PPN (%)", "Tax Rate (%)")) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("settings_tax_rate_input")
      )
    }

    HorizontalDivider()
    Text(tr("Penambahan Diskon Default (Kasir)", "Default Cashier Discounts"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    Text(
      tr(
        "Nilai diskon di bawah ini otomatis terisi pada menu Kasir (masih bisa disesuaikan kasir per transaksi).",
        "The discount values below are auto-filled in the Cashier screen (cashiers can still adjust per order)."
      ),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        value = discountPercentText,
        onValueChange = { if (isOwner) discountPercentText = it.filter { c -> c.isDigit() || c == '.' } },
        enabled = isOwner,
        label = { Text(tr("Diskon (%)", "Discount (%)")) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.weight(1f)
      )
      OutlinedTextField(
        value = discountAmountText,
        onValueChange = { if (isOwner) discountAmountText = it.filter { c -> c.isDigit() } },
        enabled = isOwner,
        label = { Text(tr("Diskon Tetap (Rp)", "Fixed Discount (Rp)")) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.weight(1f)
      )
    }

    if (isOwner) {
      Button(
        onClick = {
          val rate = (taxRateText.toDoubleOrNull() ?: 11.0).coerceIn(0.0, 100.0)
          val discPct = (discountPercentText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
          val discAmt = discountAmountText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
          onSave(
            shopProfile.copy(
              taxEnabled = taxEnabled,
              taxAutoApply = taxAutoApply,
              taxRatePercent = rate,
              defaultDiscountPercent = discPct,
              defaultDiscountAmount = discAmt
            )
          )
          onDismiss()
        },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_tax_discount_button")
      ) {
        Text(tr("Simpan Pengaturan PPN & Diskon", "Save Tax & Discount Settings"))
      }
    }
  }
}

@Composable
fun ReceiptCustomizationDialog(
  shopProfile: ShopProfile,
  paperSize: ReceiptPaperSize,
  session: ActiveShopSession,
  isOwner: Boolean,
  isBusy: Boolean,
  onSave: (ShopProfile) -> Unit,
  onDismiss: () -> Unit
) {
  val isEn = isEnglish()
  var showLogo by remember(shopProfile) { mutableStateOf(shopProfile.receiptShowLogo) }
  var showName by remember(shopProfile) { mutableStateOf(shopProfile.receiptShowName) }
  var showAddress by remember(shopProfile) { mutableStateOf(shopProfile.receiptShowAddress) }
  var showPhone by remember(shopProfile) { mutableStateOf(shopProfile.receiptShowPhone) }
  var showCustomer by remember(shopProfile) { mutableStateOf(shopProfile.receiptShowCustomer) }
  var footerMsg by remember(shopProfile) { mutableStateOf(shopProfile.receiptFooterMessage) }

  val previewProfile = shopProfile.copy(
    receiptShowLogo = showLogo,
    receiptShowName = showName,
    receiptShowAddress = showAddress,
    receiptShowPhone = showPhone,
    receiptShowCustomer = showCustomer,
    receiptFooterMessage = footerMsg
  )
  val sampleOrder = remember(shopProfile, session, isEn) {
    LaundryOrder(
      id = "simulasi",
      notaNumber = "${shopProfile.notaPrefix.ifBlank { "ADA" }}-${if (isEn) "PREVIEW" else "SIMULASI"}-001",
      customerName = "Budi Santoso",
      customerPhone = "628123456789",
      items = listOf(
        OrderItem(
          "s1",
          if (isEn) "Regular Wash & Fold" else "Cuci Komplit Reguler",
          ServiceCategory.KILOAN.name,
          "kg",
          3.0,
          7000L,
          21000L
        ).toMap()
      ),
      subtotal = 21000L,
      taxRatePercent = if (shopProfile.taxEnabled) shopProfile.taxRatePercent else 0.0,
      taxAmount = if (shopProfile.taxEnabled) (21000L * (shopProfile.taxRatePercent / 100.0)).toLong() else 0L,
      totalAmount = 21000L + if (shopProfile.taxEnabled) (21000L * (shopProfile.taxRatePercent / 100.0)).toLong() else 0L,
      paidAmount = 21000L + if (shopProfile.taxEnabled) (21000L * (shopProfile.taxRatePercent / 100.0)).toLong() else 0L,
      remainingAmount = 0L,
      paymentStatus = PaymentStatus.PAID.name,
      paymentMethod = PaymentMethod.CASH.name,
      orderStatus = OrderStatus.READY.name,
      transactionAtMillis = System.currentTimeMillis(),
      estimatedFinishAtMillis = System.currentTimeMillis() + 86400_000L,
      cashierName = session.userName
    )
  }

  SettingsModalShell(title = tr("Kustomisasi & Simulasi Struk", "Receipt Customization & Preview"), onDismiss = onDismiss) {
    Text(tr("Simulasi Tampilan Struk", "Live Receipt Preview"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    ReceiptPaperPreviewCard(
      order = sampleOrder,
      shop = previewProfile,
      paperSize = paperSize
    )

    HorizontalDivider()
    ReceiptToggleRow(tr("Tampilkan Icon / Logo Usaha", "Show Shop Icon / Logo"), showLogo) { if (isOwner) showLogo = it }
    ReceiptToggleRow(tr("Tampilkan Nama Usaha", "Show Shop Name"), showName) { if (isOwner) showName = it }
    ReceiptToggleRow(tr("Tampilkan Alamat Usaha", "Show Shop Address"), showAddress) { if (isOwner) showAddress = it }
    ReceiptToggleRow(tr("Tampilkan Nomor HP / WA Usaha", "Show Shop Phone / WA"), showPhone) { if (isOwner) showPhone = it }
    ReceiptToggleRow(tr("Tampilkan Data Pelanggan", "Show Customer Info"), showCustomer) { if (isOwner) showCustomer = it }

    OutlinedTextField(
      value = footerMsg,
      onValueChange = { if (isOwner) footerMsg = it.take(160) },
      enabled = isOwner,
      label = { Text(tr("Pesan Footer Struk", "Receipt Footer Message")) },
      modifier = Modifier.fillMaxWidth().testTag("receipt_footer_input")
    )

    if (isOwner) {
      Button(
        onClick = {
          onSave(previewProfile.copy(receiptFooterMessage = footerMsg.trim()))
          onDismiss()
        },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_receipt_custom_button")
      ) {
        Text(tr("Simpan Kustomisasi Struk", "Save Receipt Customization"))
      }
    }
  }
}

@Composable
private fun ReceiptToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun UserSecuritySettingsDialog(
  session: ActiveShopSession,
  shopProfile: ShopProfile,
  staffInvites: List<StaffInvite>,
  isBusy: Boolean,
  onSaveShopProfile: (ShopProfile) -> Unit,
  onSaveStaffInvite: (StaffInvite, onSuccess: () -> Unit) -> Unit,
  onDeleteStaffInvite: (StaffInvite) -> Unit,
  onOpenFullStaffScreen: () -> Unit,
  onDismiss: () -> Unit
) {
  val isEn = isEnglish()
  val isOwner = session.role == UserRole.OWNER
  var pinInput by remember(shopProfile) { mutableStateOf(shopProfile.ownerPin) }
  var remoteName by remember { mutableStateOf("") }
  var remoteEmail by remember { mutableStateOf("") }
  var formError by remember { mutableStateOf<String?>(null) }

  val remoteOwners = remember(staffInvites) { staffInvites.filter { it.role == "OWNER" } }
  val cashiers = remember(staffInvites) { staffInvites.filter { it.role != "OWNER" } }

  SettingsModalShell(title = tr("Manajemen Pengguna & Keamanan", "User Management & Security"), onDismiss = onDismiss) {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tr("Pembagian Hak Akses", "Role Access Overview"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(
          tr(
            "• Kasir / Karyawan (${cashiers.size}): hanya melayani transaksi, status pesanan, pelanggan & WhatsApp.",
            "• Cashier / Staff (${cashiers.size}): handles transactions, order status, customers & WhatsApp."
          ),
          style = MaterialTheme.typography.bodySmall
        )
        Text(
          tr(
            "• Owner (Utama + ${remoteOwners.size}/2 Jarak Jauh): akses penuh seluruh dashboard, harga, pengeluaran, laporan & pengaturan.",
            "• Owner (Primary + ${remoteOwners.size}/2 Remote): full access to dashboard, pricing, expenses, reports & settings."
          ),
          style = MaterialTheme.typography.bodySmall
        )
      }
    }

    if (isOwner) {
      Text(tr("PIN Keamanan Owner (4–6 Digit)", "Owner Security PIN (4–6 Digits)"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = pinInput,
          onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(6) },
          label = { Text(tr("PIN Keamanan (kosongkan untuk nonaktif)", "Security PIN (leave blank to disable)")) },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("settings_owner_pin_input")
        )
        Button(
          onClick = {
            if (pinInput.isNotEmpty() && pinInput.length < 4) {
              formError = if (isEn) "PIN must be at least 4 digits or leave empty to disable." else "PIN minimal 4 digit atau kosongkan untuk menonaktifkan."
            } else {
              formError = null
              onSaveShopProfile(shopProfile.copy(ownerPin = pinInput))
            }
          },
          enabled = !isBusy,
          modifier = Modifier.height(52.dp)
        ) {
          Text(tr("Simpan PIN", "Save PIN"))
        }
      }

      HorizontalDivider()
      Text(
        tr("Hubungkan 1–2 Akun Owner Jarak Jauh (${remoteOwners.size}/2)", "Connect 1–2 Remote Owner Accounts (${remoteOwners.size}/2)"),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
      )
      Text(
        tr(
          "Pemilik dapat memantau omzet, laporan, dan pesanan secara real-time dari hingga 2 akun Google tambahan.",
          "Owners can monitor revenue, reports, and orders in real time from up to 2 additional Google accounts."
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      remoteOwners.forEach { ownerInv ->
        Surface(
          color = EmeraldContainerLight.copy(alpha = 0.6f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(ownerInv.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
              Text(ownerInv.email, style = MaterialTheme.typography.bodySmall)
              Text(
                if (ownerInv.staffUid.isNotBlank()) {
                  tr("Tersambung • Pemantauan Aktif", "Connected • Monitoring Active")
                } else {
                  tr("Menunggu login Google", "Awaiting Google sign-in")
                },
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldAccent
              )
            }
            IconButton(onClick = { onDeleteStaffInvite(ownerInv) }) {
              Icon(Icons.Filled.Delete, contentDescription = tr("Putuskan akun", "Disconnect account"), tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      }

      if (remoteOwners.size < 2) {
        OutlinedTextField(
          value = remoteName,
          onValueChange = { remoteName = it },
          label = { Text(tr("Nama Pemantau / Owner ke-${remoteOwners.size + 1}", "Remote Owner #${remoteOwners.size + 1} Name")) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = remoteEmail,
          onValueChange = { remoteEmail = it },
          label = { Text(tr("Email Google Owner Jarak Jauh", "Remote Owner Google Email")) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
          onClick = {
            if (remoteName.trim().length < 2) {
              formError = if (isEn) "Remote owner name must be at least 2 characters." else "Nama pemilik jarak jauh minimal 2 karakter."
              return@OutlinedButton
            }
            val clean = remoteEmail.trim().lowercase()
            if (!clean.contains("@") || !clean.contains(".")) {
              formError = if (isEn) "Enter a valid Google email." else "Masukkan email Google yang valid."
              return@OutlinedButton
            }
            formError = null
            onSaveStaffInvite(
              StaffInvite(
                name = remoteName.trim(),
                email = clean,
                role = "OWNER",
                active = true,
                permissions = StaffPermissions.ownerAll().toMap()
              )
            ) {
              remoteName = ""
              remoteEmail = ""
            }
          },
          enabled = !isBusy,
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Icon(Icons.Filled.PersonAdd, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(tr("Sambungkan Akun Owner (${remoteOwners.size}/2)", "Connect Owner Account (${remoteOwners.size}/2)"))
        }
      }

      if (formError != null) {
        Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      }

      HorizontalDivider()
      Button(
        onClick = {
          onDismiss()
          onOpenFullStaffScreen()
        },
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_staff_management_button")
      ) {
        Icon(Icons.Filled.Badge, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(tr("Kelola Akun Kasir & Log Aktivitas", "Manage Cashier Accounts & Activity Log"))
      }
    } else {
      Text(
        tr(
          "Anda login sebagai Kasir. Manajemen PIN, akun karyawan, dan akun pemantau hanya dapat diubah oleh Owner.",
          "You are signed in as a Cashier. PIN, staff accounts, and remote owners can only be changed by the Owner."
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrinterAndPaperSettingsDialog(
  shopProfile: ShopProfile,
  savedPrinterName: String,
  paperSize: ReceiptPaperSize,
  session: ActiveShopSession,
  isOwner: Boolean,
  onSavePrinter: (String) -> Unit,
  onSelectPaperSize: (ReceiptPaperSize) -> Unit,
  onSaveShopProfile: (ShopProfile) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  var printerInput by remember(savedPrinterName) { mutableStateOf(savedPrinterName) }
  var selectedPaper by remember(paperSize) { mutableStateOf(paperSize) }

  SettingsModalShell(title = tr("Printer & Ukuran Kertas Struk", "Printer & Paper Size"), onDismiss = onDismiss) {
    Surface(
      color = EmeraldContainerLight.copy(alpha = 0.65f),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Filled.CloudDone, contentDescription = null, tint = EmeraldAccent)
        Column {
          Text(
            text = if (printerInput.isNotBlank()) {
              "${tr("Printer Terpilih", "Selected Printer")}: $printerInput"
            } else {
              tr("Layanan Cetak Android Siap", "Android Print Service Ready")
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF064E3B)
          )
          Text(
            tr(
              "Mendukung printer thermal Bluetooth/Wi-Fi melalui Android Print Service dan ekspor PDF.",
              "Supports Bluetooth/Wi-Fi thermal printers via Android Print Service and PDF export."
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF064E3B)
          )
        }
      }
    }

    OutlinedTextField(
      value = printerInput,
      onValueChange = { printerInput = it },
      label = { Text(tr("Nama Printer Tersambung", "Connected Printer Name")) },
      placeholder = { Text(tr("Contoh: Thermal 58mm Kasir", "e.g. Cashier Thermal 58mm")) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    Text(tr("Pilih Ukuran Kertas Struk", "Select Receipt Paper Size"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ReceiptPaperSize.entries.forEach { size ->
        FilterChip(
          selected = selectedPaper == size,
          onClick = { selectedPaper = size },
          label = { Text(size.localizedLabel(isEn)) },
          modifier = Modifier.testTag("paper_size_${size.name.lowercase()}")
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = {
          val previewOrder = LaundryOrder(
            id = "test_print",
            notaNumber = "${shopProfile.notaPrefix.ifBlank { "ADA" }}-TEST-001",
            customerName = if (isEn) "Sample Customer" else "Contoh Pelanggan",
            customerPhone = "628123456789",
            items = listOf(
              OrderItem(
                "preview",
                if (isEn) "Regular Wash & Fold" else "Cuci Komplit Reguler",
                ServiceCategory.KILOAN.name,
                "kg",
                3.0,
                7000L,
                21000L
              ).toMap()
            ),
            subtotal = 21000L,
            totalAmount = 21000L,
            paidAmount = 21000L,
            remainingAmount = 0L,
            paymentStatus = PaymentStatus.PAID.name,
            paymentMethod = PaymentMethod.CASH.name,
            orderStatus = OrderStatus.READY.name,
            transactionAtMillis = System.currentTimeMillis(),
            estimatedFinishAtMillis = System.currentTimeMillis() + 86400_000L,
            cashierName = session.userName
          )
          ReceiptAndReportExporter.printReceipt(context, previewOrder, shopProfile, selectedPaper, lang)
        },
        modifier = Modifier.weight(1f).height(48.dp).testTag("test_print_button")
      ) {
        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(tr("Tes Cetak", "Test Print"))
      }
      Button(
        onClick = {
          onSavePrinter(printerInput.trim())
          onSelectPaperSize(selectedPaper)
          if (isOwner) {
            onSaveShopProfile(
              shopProfile.copy(
                printerName = printerInput.trim(),
                paperSize = selectedPaper.name
              )
            )
          }
          onDismiss()
        },
        modifier = Modifier.weight(1f).height(48.dp)
      ) {
        Text(tr("Simpan", "Save"))
      }
    }
  }
}

@Composable
fun TutorialGuideDialog(onDismiss: () -> Unit) {
  val isEn = isEnglish()
  val steps = if (isEn) {
    listOf(
      "1. Add Services & Pricing" to "Open Services & Pricing (Owner only), tap Add Service, pick Kilogram (kg), Per Item (pcs), or Carpet (sheet), then save the rate.",
      "2. Record Cashier Orders" to "Open Cashier (or tap NEW ORDER on Dashboard), enter/pick customer & WhatsApp number, select services and weight/qty, set discount/tax/pickup estimate, then save to create a receipt.",
      "3. Update Status & Send WhatsApp" to "Open Orders. Below each order card you can step through Received, Processing, Ready for Pickup, and Picked Up. Once Ready, tap Send WA Ready to notify the customer.",
      "4. Receipt, PIN & Remote Owners" to "In Settings > Receipt Customization you can inspect the live receipt preview and edit footer notes. In User Management you can set an Owner PIN and connect up to 2 remote Owner accounts.",
      "5. Print Receipts & Export Reports" to "Use Printer & Paper Size to select 58mm, 80mm, or A4. The Reports screen lets the Owner download/share financial summaries in PDF or Excel (.xlsx)."
    )
  } else {
    listOf(
      "1. Menambah Layanan & Harga" to "Buka menu Layanan & Harga (khusus Owner), tekan Tambah Layanan, pilih kategori Kiloan (kg), Satuan (pcs), atau Karpet (lembar), lalu simpan tarifnya.",
      "2. Mencatat Transaksi Kasir" to "Buka menu Kasir (atau tombol TRANSAKSI BARU di Beranda), isi/pilih pelanggan & nomor WhatsApp, pilih layanan dan berat/jumlah, atur diskon/PPN/estimasi, lalu simpan untuk membuat nota.",
      "3. Ubah Status & Kirim WhatsApp" to "Buka menu Pesanan. Di bawah setiap kartu transaksi tersedia tombol Diterima, Diproses, Siap Diambil, dan Sudah Diambil. Saat status Siap Diambil, tekan Kirim WA untuk memberitahu pelanggan.",
      "4. Pengaturan Struk, PIN & Pemantau" to "Di Pengaturan > Kustomisasi Struk Anda dapat melihat simulasi struk langsung dan mengatur pesan footer. Di Manajemen Pengguna Anda bisa mengaktifkan PIN Owner serta menautkan hingga 2 akun Owner jarak jauh.",
      "5. Cetak Nota & Ekspor Laporan" to "Gunakan kartu Printer untuk memilih kertas 58mm, 80mm, atau A4. Menu Laporan memungkinkan Owner mengunduh/membagikan rekap keuangan dalam format PDF maupun Excel (.xlsx)."
    )
  }

  SettingsModalShell(title = tr("Tutorial Penggunaan Singkat", "Quick User Guide"), onDismiss = onDismiss) {
    steps.forEach { (heading, body) ->
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
      }
    }
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) {
      Text(tr("Saya Mengerti", "Got It"))
    }
  }
}

@Composable
fun AboutAppDialog(
  isOwner: Boolean,
  isBusy: Boolean,
  onExportBackupJson: (onResult: (String) -> Unit) -> Unit,
  onNotify: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN

  SettingsModalShell(title = tr("Tentang Aplikasi", "About App"), onDismiss = onDismiss) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Image(
        painter = painterResource(R.drawable.img_brand_logo),
        contentDescription = tr("Logo ADA LAUNDRY", "ADA LAUNDRY Logo"),
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp))
      )
      Column {
        Text("ADA LAUNDRY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("${tr("Versi", "Version")} ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text("Developer: Rafline Dev", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    Text(
      tr(
        "Aplikasi kasir dan manajemen operasional usaha laundry modern untuk membantu pemilik dan karyawan mencatat transaksi kiloan, satuan, dan karpet per lembar, mengelola status pesanan, mengirim notifikasi WhatsApp, serta memantau laporan keuangan secara real-time.",
        "Modern laundry POS and operations management app helping owners and staff record kiloan, per-item, and carpet orders, track order statuses, send WhatsApp pickup alerts, and monitor real-time financial reports."
      ),
      style = MaterialTheme.typography.bodySmall
    )

    if (isOwner) {
      HorizontalDivider()
      Text(tr("Backup Data Outlet (JSON)", "Outlet Data Backup (JSON)"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
          onClick = {
            onExportBackupJson { json ->
              ReceiptAndReportExporter.saveBackupJsonToDownloads(context, json)
                .onSuccess { path ->
                  onNotify(if (isEn) "JSON backup saved to $path" else "Backup JSON disimpan di $path")
                }
                .onFailure { err ->
                  onNotify(if (isEn) "Failed to save backup: ${err.message}" else "Gagal menyimpan backup: ${err.message}")
                }
            }
          },
          enabled = !isBusy,
          modifier = Modifier.weight(1f).height(46.dp).testTag("export_backup_downloads_button")
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(tr("Simpan JSON", "Save JSON"))
        }
        OutlinedButton(
          onClick = {
            onExportBackupJson { json ->
              ReceiptAndReportExporter.shareBackupJson(context, json, lang)
            }
          },
          enabled = !isBusy,
          modifier = Modifier.weight(1f).height(46.dp).testTag("share_backup_button")
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(tr("Bagikan", "Share"))
        }
      }
    }

    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) {
      Text(tr("Tutup", "Close"))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppTemplateSettingsDialog(
  shopProfile: ShopProfile,
  session: ActiveShopSession,
  isOwner: Boolean,
  isBusy: Boolean,
  onSave: (ShopProfile) -> Unit,
  onDismiss: () -> Unit
) {
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  var templateInput by remember(shopProfile, lang) {
    mutableStateOf(shopProfile.whatsappTemplate.ifBlank { WhatsAppHelper.defaultTemplateFor(lang) })
  }

  val sampleOrder = remember(shopProfile, session, isEn) {
    LaundryOrder(
      id = "preview_wa",
      notaNumber = "${shopProfile.notaPrefix.ifBlank { "ADA" }}-20261117-0001",
      customerName = "Ibu Sari",
      customerPhone = "628123456789",
      items = listOf(
        OrderItem(
          "p1",
          if (isEn) "Regular Wash & Fold" else "Cuci Komplit Reguler",
          ServiceCategory.KILOAN.name,
          "kg",
          4.0,
          7000L,
          28000L
        ).toMap()
      ),
      subtotal = 28000L,
      totalAmount = 28000L,
      paidAmount = 10000L,
      remainingAmount = 18000L,
      paymentStatus = PaymentStatus.DP.name,
      orderStatus = OrderStatus.READY.name,
      estimatedFinishAtMillis = System.currentTimeMillis() + 3_600_000L,
      cashierName = session.userName
    )
  }

  SettingsModalShell(title = tr("Template Chat WhatsApp", "WhatsApp Chat Template"), onDismiss = onDismiss) {
    Text(
      tr(
        "Atur isi pesan WhatsApp otomatis saat cucian selesai & siap diambil. Ketuk tag variabel untuk menyisipkannya ke pesan.",
        "Customize the WhatsApp message sent when laundry is ready for pickup. Tap variable tags to insert them."
      ),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
      value = templateInput,
      onValueChange = { if (it.length <= 1500) templateInput = it },
      enabled = isOwner,
      label = { Text(tr("Isi Template Chat WhatsApp", "WhatsApp Message Template")) },
      minLines = 6,
      maxLines = 12,
      modifier = Modifier.fillMaxWidth().testTag("whatsapp_template_input")
    )

    if (isOwner) {
      Text(tr("Sisipkan Variabel Otomatis:", "Insert Dynamic Variables:"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WhatsAppHelper.localizedPlaceholders(lang).forEach { (tag, label) ->
          FilterChip(
            selected = templateInput.contains(tag),
            onClick = {
              templateInput = if (templateInput.endsWith(" ") || templateInput.endsWith("\n") || templateInput.isEmpty()) {
                templateInput + tag
              } else {
                "$templateInput $tag"
              }
            },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
          )
        }
      }
    }

    Text(tr("Pratinjau Pesan WhatsApp:", "WhatsApp Message Preview:"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    Surface(
      color = Color(0xFFDCF8C6),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, Color(0xFF86EFAC)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = WhatsAppHelper.buildReadyWhatsAppMessage(
          order = sampleOrder,
          shopName = shopProfile.businessName.ifBlank { "ADA LAUNDRY" },
          customTemplate = templateInput,
          lang = lang
        ),
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF0F172A),
        modifier = Modifier.padding(12.dp)
      )
    }

    if (isOwner) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
          onClick = { templateInput = WhatsAppHelper.defaultTemplateFor(lang) },
          modifier = Modifier.weight(1f).height(48.dp)
        ) {
          Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(tr("Reset Default", "Reset Default"))
        }
        Button(
          onClick = {
            val cleaned = templateInput.trim()
            val stored = if (
              cleaned == WhatsAppHelper.DEFAULT_WHATSAPP_TEMPLATE.trim() ||
              cleaned == WhatsAppHelper.DEFAULT_WHATSAPP_TEMPLATE_EN.trim()
            ) {
              ""
            } else {
              cleaned
            }
            onSave(shopProfile.copy(whatsappTemplate = stored))
            onDismiss()
          },
          enabled = !isBusy && templateInput.isNotBlank(),
          modifier = Modifier.weight(1f).height(48.dp).testTag("save_whatsapp_template_button")
        ) {
          Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(tr("Simpan", "Save"))
        }
      }
    } else {
      Text(
        tr("Hanya Owner yang dapat mengubah template WhatsApp outlet.", "Only the Owner can edit the outlet WhatsApp template."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun DueSoundNotificationDialog(
  dueSoundEnabled: Boolean,
  onSetDueSoundEnabled: (Boolean) -> Unit,
  onNotify: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isEn = isEnglish()
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasPermission by remember { mutableStateOf(LaundryReminderHelper.hasNotificationPermission(context)) }
  var hasExactAlarm by remember { mutableStateOf(LaundryReminderHelper.canScheduleExactAlarms(context)) }
  var batteryRestricted by remember { mutableStateOf(LaundryReminderHelper.isPowerSaveRestricted(context)) }

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasPermission = LaundryReminderHelper.hasNotificationPermission(context)
        hasExactAlarm = LaundryReminderHelper.canScheduleExactAlarms(context)
        batteryRestricted = LaundryReminderHelper.isPowerSaveRestricted(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
    onNotify(
      if (granted) {
        if (isEn) "System notification permission granted." else "Izin notifikasi sistem diberikan."
      } else {
        if (isEn) "Notification permission denied; sound plays while app is open." else "Izin notifikasi ditolak; suara tetap berjalan saat aplikasi terbuka."
      }
    )
  }

  SettingsModalShell(title = tr("Notifikasi Suara Cucian Tepat Waktu", "Laundry Pickup Sound Alarm"), onDismiss = onDismiss) {
    Text(
      tr(
        "Aplikasi akan membunyikan alarm suara dan menampilkan pengingat otomatis saat cucian mencapai waktu pengambilan—termasuk ketika layar mati dan HP terkunci.",
        "The app plays an audible alarm and shows a reminder automatically when laundry reaches pickup time—even when locked and the screen is off."
      ),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(tr("Aktifkan Notifikasi Suara", "Enable Sound Alarm"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
          if (dueSoundEnabled) {
            tr("Berbunyi saat waktu pengambilan tiba", "Rings when pickup time arrives")
          } else {
            tr("Notifikasi suara dimatikan", "Sound reminders turned off")
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Switch(
        checked = dueSoundEnabled,
        onCheckedChange = onSetDueSoundEnabled,
        modifier = Modifier.testTag("switch_due_sound_enabled")
      )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            tr(
              "Izin notifikasi Android belum aktif. Aktifkan agar pengingat tetap muncul saat layar mati atau aplikasi tertutup.",
              "Android notification permission is disabled. Enable it so reminders show while locked or in the background."
            ),
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedButton(
            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            modifier = Modifier.fillMaxWidth().height(44.dp)
          ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Izinkan Notifikasi Sistem", "Allow System Notifications"))
          }
        }
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarm) {
      Surface(
        color = WarningContainerLight,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            tr(
              "Izin Alarm Tepat Waktu (Alarms & reminders) belum aktif. Tanpa izin ini, Android dapat menunda bunyi notifikasi saat HP terkunci dan layar mati.",
              "Exact Alarm permission (Alarms & reminders) is disabled. Without it, Android may delay alarm sounds while locked with the screen off."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF78350F)
          )
          OutlinedButton(
            onClick = { LaundryReminderHelper.openExactAlarmSettings(context) },
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("open_exact_alarm_settings_button")
          ) {
            Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(18.dp), tint = WarningAmber)
            Spacer(Modifier.width(6.dp))
            Text(tr("Aktifkan Alarm Tepat Waktu", "Allow Exact Alarms"))
          }
        }
      }
    }

    if (batteryRestricted) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            tr(
              "Penghemat baterai aktif untuk aplikasi ini. Agar alarm tetap berbunyi saat layar mati & HP terkunci, pilih Tanpa Pembatasan (Unrestricted).",
              "Battery optimization may restrict background alarms while locked & screen off. Set battery usage to Unrestricted for reliable sound alerts."
            ),
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedButton(
            onClick = { LaundryReminderHelper.openBatteryOptimizationSettings(context) },
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("open_battery_settings_button")
          ) {
            Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Atur Baterai Tanpa Pembatasan", "Set Unrestricted Battery"))
          }
        }
      }
    }

    if (hasPermission && hasExactAlarm && !batteryRestricted) {
      Surface(
        color = EmeraldContainerLight.copy(alpha = 0.7f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Filled.Check, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
          Text(
            tr(
              "Semua izin alarm layar terkunci sudah aktif (Notifikasi, Alarm Tepat Waktu & Baterai Tanpa Pembatasan).",
              "All lock-screen alarm permissions are active (Notifications, Exact Alarms & Unrestricted Battery)."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF064E3B)
          )
        }
      }
    }

    OutlinedButton(
      onClick = { LaundryReminderHelper.openAppNotificationSettings(context) },
      modifier = Modifier.fillMaxWidth().height(44.dp)
    ) {
      Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text(tr("Pengaturan Saluran Notifikasi Android", "Android Notification Channel Settings"))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = {
          LaundryReminderHelper.playAlertSound(context)
          onNotify(
            if (isEn) "Playing laundry pickup reminder alarm..."
            else "Memutar suara pengingat cucian tepat waktu..."
          )
        },
        modifier = Modifier.weight(1f).height(48.dp).testTag("test_due_sound_button")
      ) {
        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(tr("Tes Suara", "Test Sound"))
      }
      Button(
        onClick = onDismiss,
        modifier = Modifier.weight(1f).height(48.dp)
      ) {
        Text(tr("Selesai", "Done"))
      }
    }
  }
}
