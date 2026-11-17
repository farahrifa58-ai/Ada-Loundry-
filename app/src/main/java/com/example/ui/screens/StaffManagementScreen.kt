package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.model.ActiveShopSession
import com.example.data.model.ActivityLog
import com.example.data.model.LaundryOrder
import com.example.data.model.OrderStatus
import com.example.data.model.StaffInvite
import com.example.data.model.StaffPermissions
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainerLight
import com.example.util.Formatters
import com.example.util.LocalAppLanguage
import com.example.util.isEnglish
import com.example.util.tr

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
  session: ActiveShopSession,
  staffInvites: List<StaffInvite>,
  orders: List<LaundryOrder>,
  activityLogs: List<ActivityLog>,
  isBusy: Boolean,
  onSaveInvite: (StaffInvite, onSuccess: () -> Unit) -> Unit,
  onDeleteInvite: (StaffInvite) -> Unit
) {
  val lang = LocalAppLanguage.current
  val isEn = isEnglish()

  if (session.role != UserRole.OWNER) {
    Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
      Spacer(Modifier.height(10.dp))
      Text(
        tr(
          "Manajemen Karyawan & Kasir hanya tersedia untuk Pemilik (Owner).",
          "Staff & Cashier Management is available to the Owner only."
        ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    return
  }

  var activeTab by remember { mutableStateOf(0) }
  var editingInvite by remember { mutableStateOf<StaffInvite?>(null) }
  var deletingInvite by remember { mutableStateOf<StaffInvite?>(null) }
  var logFilter by remember { mutableStateOf<String?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    PrimaryTabRow(selectedTabIndex = activeTab) {
      Tab(
        selected = activeTab == 0,
        onClick = { activeTab = 0 },
        text = { Text(tr("Akun Kasir (${staffInvites.size})", "Cashier Accounts (${staffInvites.size})")) },
        icon = { Icon(Icons.Filled.Badge, contentDescription = null) },
        modifier = Modifier.testTag("staff_accounts_tab")
      )
      Tab(
        selected = activeTab == 1,
        onClick = { activeTab = 1 },
        text = { Text(tr("Aktivitas (${activityLogs.size})", "Activity (${activityLogs.size})")) },
        icon = { Icon(Icons.Filled.History, contentDescription = null) },
        modifier = Modifier.testTag("staff_activity_tab")
      )
    }

    if (activeTab == 0) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              tr("Manajemen Akun Kasir & Owner Pemantau", "Cashier & Remote Owner Account Management"),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              tr(
                "• Kasir / Karyawan: fokus melayani transaksi, status pesanan, pelanggan & WhatsApp.\n" +
                  "• Owner (Maks. 2 akun tambahan): akses penuh seluruh fitur termasuk pantau jarak jauh dari perangkat lain.",
                "• Cashier / Staff: handles transactions, order status, customers & WhatsApp.\n" +
                  "• Owner (Max. 2 extra accounts): full access to all features including remote monitoring from other devices."
              ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            tr("Daftar Pengguna (${staffInvites.size})", "User List (${staffInvites.size})"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Button(
            onClick = { editingInvite = StaffInvite() },
            modifier = Modifier.height(48.dp).testTag("add_staff_button")
          ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Tambah Akun", "Add Account"))
          }
        }

        if (staffInvites.isEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(36.dp))
              Text(tr("Belum ada akun tambahan", "No additional accounts yet"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
              Text(
                tr(
                  "Tekan Tambah Akun untuk mendaftarkan Kasir atau Owner pemantau jarak jauh (maks. 2).",
                  "Tap Add Account to register a Cashier or remote monitoring Owner (max. 2)."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(staffInvites, key = { it.id }) { invite ->
              val handledOrders = remember(orders, invite) {
                orders.filter { ord ->
                  (invite.staffUid.isNotBlank() && ord.cashierUid == invite.staffUid) ||
                    ord.cashierName.equals(invite.name, ignoreCase = true)
                }
              }
              val activeHandled = handledOrders.filter { it.orderStatusEnum != OrderStatus.CANCELLED }
              val totalHandledValue = activeHandled.sumOf { it.totalAmount }
              val perms = invite.parsedPermissions
              val isRemoteOwner = invite.role == "OWNER"

              Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                  containerColor = if (invite.active) {
                    MaterialTheme.colorScheme.surface
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                  }
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(invite.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                          color = if (isRemoteOwner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                          shape = RoundedCornerShape(6.dp)
                        ) {
                          Text(
                            text = if (isRemoteOwner) "OWNER" else tr("KASIR", "CASHIER"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRemoteOwner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(invite.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                      color = if (invite.active) EmeraldContainerLight else MaterialTheme.colorScheme.errorContainer,
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text(
                        text = if (invite.active) tr("AKTIF", "ACTIVE") else tr("NONAKTIF", "INACTIVE"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (invite.active) EmeraldAccent else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (invite.staffUid.isNotBlank()) {
                        tr("Terhubung (dapat diakses jarak jauh)", "Connected (remote access ready)")
                      } else {
                        tr("Menunggu login Google pengguna", "Awaiting Google sign-in")
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = "${activeHandled.size} ${tr("transaksi", "orders")} • ${Formatters.formatRupiah(totalHandledValue)}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold
                    )
                  }

                  if (isRemoteOwner) {
                    Text(
                      tr(
                        "Akses penuh seluruh fitur (Dashboard, Kasir, Layanan, Keuangan, Laporan & Pengaturan).",
                        "Full access to all features (Dashboard, Cashier, Services, Expenses, Reports & Settings)."
                      ),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  } else {
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      PermissionBadge(tr("Buat Order", "Create Order"), perms.canCreateOrder)
                      PermissionBadge(tr("Ubah Status", "Update Status"), perms.canUpdateStatus)
                      PermissionBadge(tr("Pembayaran", "Payments"), perms.canRecordPayment)
                      PermissionBadge(tr("Pelanggan", "Customers"), perms.canManageCustomers)
                      PermissionBadge("WhatsApp", perms.canSendWhatsApp)
                      PermissionBadge(tr("Batalkan", "Cancel Order"), perms.canCancelOrder)
                      PermissionBadge(tr("Lihat Laporan", "View Reports"), perms.canViewReports)
                    }
                  }

                  HorizontalDivider()

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text(tr("Status Aktif", "Active Status"), style = MaterialTheme.typography.bodySmall)
                      Switch(
                        checked = invite.active,
                        onCheckedChange = { checked ->
                          onSaveInvite(invite.copy(active = checked)) {}
                        }
                      )
                    }
                    Row {
                      IconButton(onClick = { editingInvite = invite }) {
                        Icon(Icons.Filled.Edit, contentDescription = tr("Ubah Akun", "Edit Account"))
                      }
                      IconButton(onClick = { deletingInvite = invite }) {
                        Icon(Icons.Filled.Delete, contentDescription = tr("Hapus Akun", "Delete Account"), tint = MaterialTheme.colorScheme.error)
                      }
                    }
                  }
                }
              }
            }
            item { Spacer(Modifier.height(16.dp)) }
          }
        }
      }
    } else {
      val actionTypes = remember(activityLogs) {
        activityLogs.map { it.actionType }.filter { it.isNotBlank() }.distinct()
      }
      val filteredLogs = remember(activityLogs, logFilter) {
        if (logFilter == null) activityLogs else activityLogs.filter { it.actionType == logFilter }
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = logFilter == null,
            onClick = { logFilter = null },
            label = { Text(tr("Semua (${activityLogs.size})", "All (${activityLogs.size})")) }
          )
          actionTypes.forEach { type ->
            FilterChip(
              selected = logFilter == type,
              onClick = { logFilter = if (logFilter == type) null else type },
              label = { Text(formatActionLabel(type, isEn)) }
            )
          }
        }

        if (filteredLogs.isEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              tr("Belum ada catatan aktivitas karyawan.", "No staff activity logs yet."),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(20.dp)
            )
          }
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredLogs, key = { it.id }) { log ->
              val roleLabel = UserRole.entries.firstOrNull { it.name == log.actorRole }?.localizedLabel(isEn) ?: log.actorRole
              Card(
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      "${log.actorName} ($roleLabel)",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold
                    )
                    Surface(
                      color = MaterialTheme.colorScheme.primaryContainer,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = formatActionLabel(log.actionType, isEn),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(log.localizedDescription(isEn), style = MaterialTheme.typography.bodySmall)
                  val timeMillis = log.createdAt?.toDate()?.time ?: 0L
                  if (timeMillis > 0L) {
                    Text(
                      Formatters.formatDateTime(timeMillis, lang),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
            item { Spacer(Modifier.height(16.dp)) }
          }
        }
      }
    }
  }

  if (editingInvite != null) {
    val target = editingInvite!!
    var name by remember(target) { mutableStateOf(target.name) }
    var email by remember(target) { mutableStateOf(target.email) }
    var role by remember(target) { mutableStateOf(if (target.role == "OWNER") "OWNER" else "KASIR") }
    var active by remember(target) { mutableStateOf(target.active) }
    var perms by remember(target) { mutableStateOf(target.parsedPermissions) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val otherOwnerInvitesCount = staffInvites.count { it.role == "OWNER" && it.id != target.id }

    AlertDialog(
      onDismissRequest = { editingInvite = null },
      title = {
        Text(
          if (target.id.isBlank()) tr("Tambah Akun Pengguna", "Add User Account")
          else tr("Ubah Akun Pengguna", "Edit User Account")
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(tr("Pilih Peran Pengguna", "Select User Role"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = role == "KASIR",
              onClick = { role = "KASIR" },
              label = { Text(tr("Kasir / Karyawan", "Cashier / Staff")) }
            )
            FilterChip(
              selected = role == "OWNER",
              onClick = { role = "OWNER" },
              label = { Text(tr("Owner Pemantau (${otherOwnerInvitesCount}/2)", "Remote Owner (${otherOwnerInvitesCount}/2)")) }
            )
          }

          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = {
              Text(
                if (role == "OWNER") tr("Nama Owner *", "Owner Name *")
                else tr("Nama Karyawan / Kasir *", "Cashier / Staff Name *")
              )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("staff_name_input")
          )
          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(tr("Email Akun Google *", "Google Account Email *")) },
            placeholder = { Text(tr("pengguna@gmail.com", "user@gmail.com")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("staff_email_input")
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(tr("Akun Aktif", "Account Active"), style = MaterialTheme.typography.titleSmall)
            Switch(
              checked = active,
              onCheckedChange = { active = it },
              modifier = Modifier.testTag("staff_active_switch")
            )
          }
          HorizontalDivider()
          if (role == "OWNER") {
            Text(
              tr(
                "Owner Pemantau Jarak Jauh memiliki akses penuh ke seluruh fitur outlet dari perangkat berbeda (maksimal 2 akun Owner tambahan).",
                "Remote Owners have full access to all outlet features from another device (max. 2 additional Owner accounts)."
              ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Text(tr("Izin Akses Kasir (Divalidasi Database)", "Cashier Permissions (Database Validated)"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            PermissionSwitchRow(tr("Buat Transaksi Baru", "Create New Orders"), perms.canCreateOrder) {
              perms = perms.copy(canCreateOrder = it)
            }
            PermissionSwitchRow(tr("Ubah Status Cucian", "Update Laundry Status"), perms.canUpdateStatus) {
              perms = perms.copy(canUpdateStatus = it)
            }
            PermissionSwitchRow(tr("Catat Pembayaran", "Record Payments"), perms.canRecordPayment) {
              perms = perms.copy(canRecordPayment = it)
            }
            PermissionSwitchRow(tr("Kelola Data Pelanggan", "Manage Customers"), perms.canManageCustomers) {
              perms = perms.copy(canManageCustomers = it)
            }
            PermissionSwitchRow(tr("Kirim Pemberitahuan WhatsApp", "Send WhatsApp Notifications"), perms.canSendWhatsApp) {
              perms = perms.copy(canSendWhatsApp = it)
            }
            PermissionSwitchRow(tr("Batalkan Pesanan", "Cancel Orders"), perms.canCancelOrder) {
              perms = perms.copy(canCancelOrder = it)
            }
            PermissionSwitchRow(tr("Lihat Laporan Keuangan", "View Financial Reports"), perms.canViewReports) {
              perms = perms.copy(canViewReports = it)
            }
          }
          if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.trim().length < 2) {
              errorMsg = if (isEn) "User name must be at least 2 characters." else "Nama pengguna minimal 2 karakter."
              return@Button
            }
            val cleanEmail = email.trim()
            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
              errorMsg = if (isEn) "Enter a valid Google email address." else "Masukkan alamat email Google yang valid."
              return@Button
            }
            if (role == "OWNER" && otherOwnerInvitesCount >= 2) {
              errorMsg = if (isEn) "Maximum 2 additional Owner accounts for remote monitoring." else "Maksimal 2 akun Owner tambahan untuk pemantauan jarak jauh."
              return@Button
            }
            val finalPerms = if (role == "OWNER") StaffPermissions.ownerAll() else perms
            onSaveInvite(
              target.copy(
                name = name.trim(),
                email = cleanEmail.lowercase(),
                role = role,
                active = active,
                permissions = finalPerms.toMap()
              )
            ) {
              editingInvite = null
            }
          },
          enabled = !isBusy,
          modifier = Modifier.testTag("save_staff_button")
        ) { Text(tr("Simpan", "Save")) }
      },
      dismissButton = {
        TextButton(onClick = { editingInvite = null }) { Text(tr("Batal", "Cancel")) }
      }
    )
  }

  if (deletingInvite != null) {
    val target = deletingInvite!!
    AlertDialog(
      onDismissRequest = { deletingInvite = null },
      title = { Text(tr("Hapus Akun Pengguna?", "Delete User Account?")) },
      text = {
        Text(
          tr(
            "Akun ${target.name} (${target.email}) tidak akan dapat mengakses outlet ini lagi.",
            "Account ${target.name} (${target.email}) will no longer be able to access this outlet."
          )
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteInvite(target)
            deletingInvite = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text(tr("Hapus", "Delete")) }
      },
      dismissButton = {
        TextButton(onClick = { deletingInvite = null }) { Text(tr("Batal", "Cancel")) }
      }
    )
  }
}

@Composable
private fun PermissionBadge(label: String, enabled: Boolean) {
  Surface(
    color = if (enabled) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant
    },
    shape = RoundedCornerShape(6.dp)
  ) {
    Text(
      text = if (enabled) "✓ $label" else "✕ $label",
      style = MaterialTheme.typography.labelSmall,
      color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
    )
  }
}

@Composable
private fun PermissionSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

private fun formatActionLabel(type: String, isEn: Boolean): String = when (type) {
  "CREATE_ORDER" -> if (isEn) "New Order" else "Transaksi Baru"
  "UPDATE_STATUS" -> if (isEn) "Status Update" else "Ubah Status"
  "RECORD_PAYMENT" -> if (isEn) "Payment" else "Pembayaran"
  "CANCEL_ORDER" -> if (isEn) "Cancelled" else "Pembatalan"
  "UPDATE_PRICE" -> if (isEn) "Services/Pricing" else "Layanan/Harga"
  "STAFF_UPDATE" -> if (isEn) "Staff" else "Karyawan"
  "WHATSAPP_OPENED" -> "WhatsApp"
  "EXPENSE_CREATE" -> if (isEn) "Expense" else "Pengeluaran"
  else -> type
}
