package com.example.ui.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.ActiveShopSession
import com.example.data.repository.SessionResolution
import com.example.util.tr

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.background
          )
        )
      )
      .safeDrawingPadding()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 460.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.size(104.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_brand_logo),
          contentDescription = "ADA LAUNDRY Logo",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "ADA LAUNDRY",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = tr(
            "Sistem Kasir & Manajemen Usaha Laundry Profesional",
            "Professional Laundry Cashier & Business Management System"
          ),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
      }

      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = tr("Masuk ke Outlet Anda", "Sign In to Your Outlet"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          RoleInfoRow(
            icon = Icons.Filled.AdminPanelSettings,
            title = tr("Pemilik (Owner)", "Owner"),
            desc = tr(
              "Akses penuh dashboard, transaksi, layanan & tarif, pengeluaran, laporan, karyawan, dan pengaturan.",
              "Full access to dashboard, transactions, services & pricing, expenses, reports, staff, and settings."
            )
          )
          RoleInfoRow(
            icon = Icons.Filled.Badge,
            title = tr("Kasir", "Cashier"),
            desc = tr(
              "Masuk menggunakan email Google yang didaftarkan oleh Pemilik di menu Manajemen Karyawan.",
              "Sign in with a Google email registered by the Owner in the Staff Management menu."
            )
          )

          if (errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          Button(
            onClick = {
              isLoading = true
              errorMessage = null
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = {
                  isLoading = false
                }
              )
            },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("google_sign_in_button"),
            shape = MaterialTheme.shapes.medium
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
              )
              Spacer(Modifier.width(10.dp))
              Text(tr("Memproses...", "Signing in..."))
            } else {
              Icon(Icons.Filled.Login, contentDescription = null)
              Spacer(Modifier.width(10.dp))
              Text(tr("Masuk dengan Google", "Sign in with Google"))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RoleInfoRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  desc: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun PostLoginSetupScreen(
  setupInfo: SessionResolution.NeedsShopSetup,
  isSubmitting: Boolean,
  errorMessage: String?,
  onCreateOwnerShop: (businessName: String, address: String, phone: String, hours: String, prefix: String) -> Unit,
  onRetryCheckInvite: () -> Unit,
  onSignOut: () -> Unit
) {
  var businessName by remember { mutableStateOf("ADA LAUNDRY") }
  var address by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var hours by remember { mutableStateOf("07:00 - 21:00") }
  var prefix by remember { mutableStateOf("ADA") }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .safeDrawingPadding()
      .padding(20.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            tr("Akun Google Terhubung", "Google Account Connected"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Text(
            text = "${setupInfo.userName} (${setupInfo.userEmail})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          if (setupInfo.inactiveInviteMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
              text = setupInfo.inactiveInviteMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      }

      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // Option 1: Kasir check
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(tr("Masuk Sebagai Kasir", "Sign In as Cashier"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          Text(
            tr(
              "Jika Anda adalah kasir, minta Pemilik usaha mendaftarkan email ${setupInfo.userEmail} di menu Manajemen Karyawan, lalu tekan Muat Ulang.",
              "If you are a cashier, ask the business Owner to register ${setupInfo.userEmail} in Staff Management, then tap Check Invite."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedButton(
            onClick = onRetryCheckInvite,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("retry_cashier_invite_button")
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tr("Cek Undangan Kasir", "Check Cashier Invite"))
          }
        }
      }

      // Option 2: Create Owner Shop
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(tr("Daftar Outlet Baru (Pemilik / Owner)", "Register New Outlet (Owner)"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          Text(
            tr(
              "Lengkapi profil outlet untuk mulai mengelola ADA LAUNDRY sebagai Pemilik.",
              "Complete your outlet profile to start managing ADA LAUNDRY as Owner."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text(tr("Nama Usaha Laundry", "Laundry Business Name")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("setup_business_name_input")
          )
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(tr("Alamat Outlet", "Outlet Address")) },
            modifier = Modifier.fillMaxWidth().testTag("setup_address_input")
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text(tr("No. Telepon Outlet", "Outlet Phone")) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("setup_phone_input")
            )
            OutlinedTextField(
              value = prefix,
              onValueChange = { prefix = it.uppercase().take(10) },
              label = { Text(tr("Prefix Nota", "Receipt Prefix")) },
              singleLine = true,
              modifier = Modifier.weight(0.6f).testTag("setup_prefix_input")
            )
          }
          OutlinedTextField(
            value = hours,
            onValueChange = { hours = it },
            label = { Text(tr("Jam Operasional", "Operating Hours")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Button(
            onClick = { onCreateOwnerShop(businessName, address, phone, hours, prefix) },
            enabled = !isSubmitting && businessName.trim().length >= 2,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_owner_shop_button")
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
              Spacer(Modifier.width(8.dp))
              Text(tr("Menyiapkan Outlet...", "Setting up outlet..."))
            } else {
              Icon(Icons.Filled.CheckCircle, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(tr("Buat Outlet & Masuk sebagai Pemilik", "Create Outlet & Enter as Owner"))
            }
          }
        }
      }

      OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
      ) {
        Icon(Icons.Filled.Logout, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(tr("Ganti Akun Google", "Switch Google Account"))
      }
    }
  }
}

@Composable
fun ChooseRoleScreen(
  ownerSession: ActiveShopSession,
  cashierSession: ActiveShopSession,
  onSelectSession: (ActiveShopSession) -> Unit,
  onSignOut: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .safeDrawingPadding()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text(
        tr("Pilih Mode Masuk", "Choose Sign-In Mode"),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        tr(
          "Akun Anda terdaftar sebagai Pemilik outlet dan juga Kasir.",
          "Your account is registered as both an outlet Owner and a Cashier."
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Button(
        onClick = { onSelectSession(ownerSession) },
        modifier = Modifier.fillMaxWidth().height(56.dp)
      ) {
        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(tr("Masuk sebagai Pemilik (${ownerSession.shopProfile.businessName})", "Enter as Owner (${ownerSession.shopProfile.businessName})"))
      }
      OutlinedButton(
        onClick = { onSelectSession(cashierSession) },
        modifier = Modifier.fillMaxWidth().height(56.dp)
      ) {
        Icon(Icons.Filled.Badge, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(tr("Masuk sebagai Kasir (${cashierSession.shopProfile.businessName})", "Enter as Cashier (${cashierSession.shopProfile.businessName})"))
      }
      OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth().height(48.dp)
      ) {
        Icon(Icons.Filled.Logout, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(tr("Keluar", "Sign Out"))
      }
    }
  }
}
