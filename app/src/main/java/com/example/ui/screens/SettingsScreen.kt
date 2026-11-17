package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.AppLanguage
import com.example.data.local.ReceiptPaperSize
import com.example.data.local.ThemeMode
import com.example.data.model.ActiveShopSession
import com.example.data.model.ShopProfile
import com.example.data.model.StaffInvite
import com.example.data.model.UserRole
import com.example.ui.components.BusinessAvatarBadge
import com.example.ui.components.PinVerificationDialog
import com.example.ui.theme.BrandColorOption
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.WarningAmber
import com.example.util.tr

private enum class ActiveSettingsDialog {
  SHOP_PROFILE,
  THEME_LANGUAGE,
  TAX_DISCOUNT,
  RECEIPT_CUSTOM,
  WHATSAPP_TEMPLATE,
  SOUND_REMINDER,
  USER_SECURITY,
  PRINTER_PAPER,
  TUTORIAL,
  ABOUT
}

@Composable
fun SettingsScreen(
  session: ActiveShopSession,
  shopProfile: ShopProfile,
  themeMode: ThemeMode,
  appLanguage: AppLanguage,
  savedPrinterName: String,
  paperSize: ReceiptPaperSize,
  dueSoundEnabled: Boolean,
  staffInvites: List<StaffInvite>,
  isBusy: Boolean,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onSavePrinter: (String) -> Unit,
  onSelectPaperSize: (ReceiptPaperSize) -> Unit,
  onSetDueSoundEnabled: (Boolean) -> Unit,
  onSaveShopProfile: (ShopProfile) -> Unit,
  onSaveStaffInvite: (StaffInvite, onSuccess: () -> Unit) -> Unit,
  onDeleteStaffInvite: (StaffInvite) -> Unit,
  onOpenStaffManagement: () -> Unit,
  onOpenServices: () -> Unit,
  onExportBackupJson: (onResult: (String) -> Unit) -> Unit,
  onNotify: (String) -> Unit,
  onSignOut: () -> Unit
) {
  val isEn = appLanguage == AppLanguage.EN
  val isOwner = session.role == UserRole.OWNER
  var openDialog by remember { mutableStateOf<ActiveSettingsDialog?>(null) }
  var showPinPromptForDialog by remember { mutableStateOf<ActiveSettingsDialog?>(null) }

  fun openGuardedDialog(target: ActiveSettingsDialog, requiresPin: Boolean = false) {
    if (requiresPin && shopProfile.ownerPin.isNotBlank()) {
      showPinPromptForDialog = target
    } else {
      openDialog = target
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Active Account Header Card
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          BusinessAvatarBadge(shop = shopProfile, size = 50.dp)
          Column(modifier = Modifier.weight(1f)) {
            Text(session.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(session.userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "${tr("Peran", "Role")}: ${session.role.localizedLabel(isEn)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          if (isOwner) {
            OutlinedButton(
              onClick = onOpenServices,
              modifier = Modifier.weight(1f).height(44.dp).testTag("open_services_from_settings_button")
            ) {
              Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(tr("Layanan & Harga", "Services & Pricing"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          OutlinedButton(
            onClick = onSignOut,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f).height(44.dp).testTag("sign_out_button")
          ) {
            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Keluar / Ganti", "Sign Out / Switch"), maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    // 1. Profil Usaha
    val brandColor = BrandColorOption.fromKey(shopProfile.brandColorKey)
    SettingsMenuCard(
      title = tr("Profil Usaha", "Shop Profile"),
      subtitle = "${shopProfile.businessName.ifBlank { "ADA LAUNDRY" }} • ${brandColor.localizedLabel(isEn)}",
      icon = Icons.Filled.Storefront,
      iconBg = brandColor.lightPrimary,
      testTag = "settings_card_shop_profile",
      onClick = { openGuardedDialog(ActiveSettingsDialog.SHOP_PROFILE) }
    )

    // 2. Tema & Bahasa
    SettingsMenuCard(
      title = tr("Tema & Bahasa", "Theme & Language"),
      subtitle = "${themeMode.localizedLabel(isEn)} • ${appLanguage.label}",
      icon = Icons.Filled.Palette,
      iconBg = CyanSecondary,
      testTag = "settings_card_theme_language",
      onClick = { openGuardedDialog(ActiveSettingsDialog.THEME_LANGUAGE) }
    )

    // 3. PPN & Diskon
    val taxRateTxt = if (shopProfile.taxRatePercent % 1.0 == 0.0) shopProfile.taxRatePercent.toInt().toString() else shopProfile.taxRatePercent.toString()
    val taxSummary = if (shopProfile.taxEnabled) {
      if (isEn) {
        "Tax $taxRateTxt% (${if (shopProfile.taxAutoApply) "Auto" else "Manual"})"
      } else {
        "PPN $taxRateTxt% (${if (shopProfile.taxAutoApply) "Otomatis" else "Manual"})"
      }
    } else {
      tr("PPN Nonaktif", "Tax Disabled")
    }
    SettingsMenuCard(
      title = tr("PPN & Diskon", "Tax & Discount"),
      subtitle = tr(
        "$taxSummary • Diskon default ${shopProfile.defaultDiscountPercent.toInt()}% / Rp${shopProfile.defaultDiscountAmount}",
        "$taxSummary • Default discount ${shopProfile.defaultDiscountPercent.toInt()}% / Rp${shopProfile.defaultDiscountAmount}"
      ),
      icon = Icons.Filled.Percent,
      iconBg = WarningAmber,
      testTag = "settings_card_tax_discount",
      onClick = { openGuardedDialog(ActiveSettingsDialog.TAX_DISCOUNT) }
    )

    // 4. Kustomisasi Struk (Simulasi Struk)
    SettingsMenuCard(
      title = tr("Kustomisasi Struk", "Receipt Customization"),
      subtitle = tr(
        "Simulasi struk, icon, nama, alamat, nomor HP, pelanggan & footer",
        "Receipt preview, icon, shop name, address, phone, customer & footer"
      ),
      icon = Icons.Filled.ReceiptLong,
      iconBg = EmeraldAccent,
      testTag = "settings_card_receipt_custom",
      onClick = { openGuardedDialog(ActiveSettingsDialog.RECEIPT_CUSTOM) }
    )

    // 5. Template Chat WhatsApp
    SettingsMenuCard(
      title = tr("Template Chat WhatsApp", "WhatsApp Chat Template"),
      subtitle = if (shopProfile.whatsappTemplate.isNotBlank()) {
        tr("Menggunakan template kustom outlet", "Using custom outlet template")
      } else {
        tr("Menggunakan template standar ADA LAUNDRY", "Using default ADA LAUNDRY template")
      },
      icon = Icons.Filled.Chat,
      iconBg = Color(0xFF16A34A),
      testTag = "settings_card_whatsapp_template",
      onClick = { openGuardedDialog(ActiveSettingsDialog.WHATSAPP_TEMPLATE) }
    )

    // 6. Notifikasi Suara Cucian Tepat Waktu
    SettingsMenuCard(
      title = tr("Notifikasi Suara Pengambilan", "Pickup Sound Alarm"),
      subtitle = if (dueSoundEnabled) {
        tr("Aktif • Bunyi pengingat saat cucian tepat waktu", "Active • Alarm sounds when laundry reaches pickup time")
      } else {
        tr("Nonaktif • Pengingat suara dimatikan", "Disabled • Sound reminders are turned off")
      },
      icon = Icons.Filled.NotificationsActive,
      iconBg = Color(0xFFEA580C),
      testTag = "settings_card_sound_reminder",
      onClick = { openGuardedDialog(ActiveSettingsDialog.SOUND_REMINDER) }
    )

    // 7. Manajemen Pengguna & Keamanan
    val cashierCount = staffInvites.count { it.role != "OWNER" }
    val remoteOwnerCount = staffInvites.count { it.role == "OWNER" }
    SettingsMenuCard(
      title = tr("Manajemen Pengguna & Keamanan", "User Management & Security"),
      subtitle = tr(
        "${if (shopProfile.ownerPin.isNotBlank()) "PIN Aktif" else "Tanpa PIN"} • $cashierCount Kasir • $remoteOwnerCount/2 Owner Jarak Jauh",
        "${if (shopProfile.ownerPin.isNotBlank()) "PIN Active" else "No PIN"} • $cashierCount Cashiers • $remoteOwnerCount/2 Remote Owners"
      ),
      icon = Icons.Filled.ManageAccounts,
      iconBg = Color(0xFF4F46E5),
      testTag = "settings_card_user_security",
      onClick = { openGuardedDialog(ActiveSettingsDialog.USER_SECURITY, requiresPin = true) }
    )

    // 8. Printer & Kertas Struk
    SettingsMenuCard(
      title = tr("Printer & Kertas Struk", "Printer & Paper Size"),
      subtitle = "${savedPrinterName.ifBlank { tr("Printer Sistem Android", "Android System Printer") }} • ${paperSize.localizedLabel(isEn)}",
      icon = Icons.Filled.Print,
      iconBg = Color(0xFF0284C7),
      testTag = "settings_card_printer",
      onClick = { openGuardedDialog(ActiveSettingsDialog.PRINTER_PAPER) }
    )

    // 9. Tutorial Penggunaan Singkat
    SettingsMenuCard(
      title = tr("Tutorial Penggunaan Singkat", "Quick User Guide"),
      subtitle = tr(
        "Cara menambah layanan, kasir, status, WhatsApp & laporan",
        "How to add services, run cashier, update status, WhatsApp & reports"
      ),
      icon = Icons.AutoMirrored.Filled.HelpOutline,
      iconBg = Color(0xFF0D9488),
      testTag = "settings_card_tutorial",
      onClick = { openGuardedDialog(ActiveSettingsDialog.TUTORIAL) }
    )

    // 10. Tentang Aplikasi (Bottom-most)
    SettingsMenuCard(
      title = tr("Tentang Aplikasi", "About App"),
      subtitle = "ADA LAUNDRY v${BuildConfig.VERSION_NAME} • Developer Rafline Dev",
      icon = Icons.Filled.Info,
      iconBg = Color(0xFF475569),
      testTag = "settings_card_about",
      onClick = { openGuardedDialog(ActiveSettingsDialog.ABOUT) }
    )

    Spacer(Modifier.height(16.dp))
  }

  if (showPinPromptForDialog != null) {
    PinVerificationDialog(
      expectedPin = shopProfile.ownerPin,
      onVerified = {
        val target = showPinPromptForDialog!!
        showPinPromptForDialog = null
        openDialog = target
      },
      onDismiss = { showPinPromptForDialog = null }
    )
  }

  when (openDialog) {
    ActiveSettingsDialog.SHOP_PROFILE -> {
      ShopProfileSettingsDialog(
        shopProfile = shopProfile,
        isOwner = isOwner,
        isBusy = isBusy,
        onSave = onSaveShopProfile,
        onNotify = onNotify,
        onDismiss = { openDialog = null }
      )
    }
    ActiveSettingsDialog.THEME_LANGUAGE -> {
      ThemeAndLanguageDialog(
        themeMode = themeMode,
        appLanguage = appLanguage,
        onSelectThemeMode = onSelectThemeMode,
        onSelectLanguage = onSelectLanguage,
        onDismiss = { openDialog = null }
      )
    }
    ActiveSettingsDialog.TAX_DISCOUNT -> {
      TaxAndDiscountDialog(
        shopProfile = shopProfile,
        isOwner = isOwner,
        isBusy = isBusy,
        onSave = onSaveShopProfile,
        onDismiss = { openDialog = null }
      )
    }
    ActiveSettingsDialog.RECEIPT_CUSTOM -> {
      ReceiptCustomizationDialog(
        shopProfile = shopProfile,
        paperSize = paperSize,
        session = session,
        isOwner = isOwner,
        isBusy = isBusy,
        onSave = onSaveShopProfile,
        onDismiss = { openDialog = null }
      )
    }
    ActiveSettingsDialog.WHATSAPP_TEMPLATE -> {
      WhatsAppTemplateSettingsDialog(
        shopProfile = shopProfile,
        session = session,
        isOwner = isOwner,
        isBusy = isBusy,
        onSave = onSaveShopProfile,
        onDismiss = { openDialog = null }
      )
    }
    ActiveSettingsDialog.SOUND_REMINDER -> {
      DueSoundNotificationDialog(
        dueSoundEnabled = dueSoundEnabled,
        onSetDueSoundEnabled = onSetDueSoundEnabled,
        onNotify = onNotify,
        onDismiss = { openDialog = null }
      )
    }
    ActiveSettingsDialog.USER_SECURITY -> {
      UserSecuritySettingsDialog(
        session = session,
        shopProfile = shopProfile,
        staffInvites = staffInvites,
        isBusy = isBusy,
        onSaveShopProfile = onSaveShopProfile,
        onSaveStaffInvite = onSaveStaffInvite,
        onDeleteStaffInvite = onDeleteStaffInvite,
        onOpenFullStaffScreen = onOpenStaffManagement,
        onDismiss = { openDialog = null }
      )
    }
    ActiveSettingsDialog.PRINTER_PAPER -> {
      PrinterAndPaperSettingsDialog(
        shopProfile = shopProfile,
        savedPrinterName = savedPrinterName,
        paperSize = paperSize,
        session = session,
        isOwner = isOwner,
        onSavePrinter = onSavePrinter,
        onSelectPaperSize = onSelectPaperSize,
        onSaveShopProfile = onSaveShopProfile,
        onDismiss = { openDialog = null }
      )
    }
    ActiveSettingsDialog.TUTORIAL -> {
      TutorialGuideDialog(onDismiss = { openDialog = null })
    }
    ActiveSettingsDialog.ABOUT -> {
      AboutAppDialog(
        isOwner = isOwner,
        isBusy = isBusy,
        onExportBackupJson = onExportBackupJson,
        onNotify = onNotify,
        onDismiss = { openDialog = null }
      )
    }
    null -> Unit
  }
}

@Composable
private fun SettingsMenuCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconBg: Color,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(iconBg.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(22.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "${tr("Buka", "Open")} $title",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
