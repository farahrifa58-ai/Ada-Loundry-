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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.data.model.ActiveShopSession
import com.example.data.model.LaundryService
import com.example.data.model.ServiceCategory
import com.example.data.model.UserRole
import com.example.util.Formatters
import com.example.util.isEnglish
import com.example.util.localizedUnit
import com.example.util.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServicesScreen(
  session: ActiveShopSession,
  services: List<LaundryService>,
  isBusy: Boolean,
  onSaveService: (LaundryService, onSuccess: () -> Unit) -> Unit,
  onDeleteService: (LaundryService) -> Unit
) {
  val isEn = isEnglish()
  val isOwner = session.role == UserRole.OWNER
  var filterCategory by remember { mutableStateOf<ServiceCategory?>(null) }
  var editingService by remember { mutableStateOf<LaundryService?>(null) }

  val filtered = remember(services, filterCategory) {
    if (filterCategory == null) services else services.filter { it.categoryEnum == filterCategory }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    if (!isOwner) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Text(
            tr(
              "Pengaturan layanan dan harga hanya dapat diubah oleh Pemilik (dibatasi oleh database).",
              "Services and pricing can only be modified by the Owner (enforced by database rules)."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          tr("Perubahan tarif tidak mengubah transaksi lama.", "Rate changes do not affect existing transactions."),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f)
        )
        Button(
          onClick = { editingService = LaundryService() },
          modifier = Modifier.height(48.dp).testTag("add_service_button")
        ) {
          Icon(Icons.Filled.Add, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(tr("Tambah Layanan", "Add Service"))
        }
      }
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = filterCategory == null,
        onClick = { filterCategory = null },
        label = { Text(tr("Semua (${services.size})", "All (${services.size})")) }
      )
      ServiceCategory.entries.forEach { cat ->
        FilterChip(
          selected = filterCategory == cat,
          onClick = { filterCategory = if (filterCategory == cat) null else cat },
          label = { Text(cat.localizedLabel(isEn)) }
        )
      }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items(filtered, key = { it.id }) { svc ->
        val unitLbl = localizedUnit(svc.unitLabel, isEn)
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(
            containerColor = if (svc.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                svc.name + if (svc.isExtraFee) tr(" (Biaya Tambahan)", " (Extra Fee)") else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                "${svc.categoryEnum.localizedLabel(isEn)} • ${Formatters.formatRupiah(svc.price)} / $unitLbl",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
              )
              if (svc.description.isNotBlank()) {
                Text(svc.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              if (!svc.active) {
                Text(tr("Nonaktif", "Inactive"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
              }
            }
            if (isOwner) {
              Switch(
                checked = svc.active,
                onCheckedChange = { checked ->
                  onSaveService(svc.copy(active = checked)) {}
                }
              )
              IconButton(onClick = { editingService = svc }) {
                Icon(Icons.Filled.Edit, contentDescription = tr("Ubah Layanan", "Edit Service"))
              }
              IconButton(onClick = { onDeleteService(svc) }) {
                Icon(Icons.Filled.Delete, contentDescription = tr("Hapus Layanan", "Delete Service"), tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
      item { Spacer(Modifier.height(16.dp)) }
    }
  }

  if (editingService != null && isOwner) {
    val target = editingService!!
    var name by remember(target) { mutableStateOf(target.name) }
    var category by remember(target) { mutableStateOf(target.categoryEnum) }
    var priceText by remember(target) { mutableStateOf(if (target.price > 0L) target.price.toString() else "") }
    var isExtraFee by remember(target) { mutableStateOf(target.isExtraFee) }
    var active by remember(target) { mutableStateOf(target.active) }
    var description by remember(target) { mutableStateOf(target.description) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { editingService = null },
      title = {
        Text(
          if (target.id.isBlank()) tr("Tambah Layanan / Tarif", "Add Service / Rate")
          else tr("Ubah Layanan / Tarif", "Edit Service / Rate")
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(tr("Nama Layanan / Barang Satuan *", "Service / Item Name *")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("service_name_input")
          )
          Text(tr("Kategori (Satuan Harga Otomatis)", "Category (Auto Billing Unit)"), style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ServiceCategory.entries.forEach { cat ->
              FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text("${cat.localizedLabel(isEn)} (/${cat.localizedUnitLabel(isEn)})") }
              )
            }
          }
          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
            label = { Text(tr("Harga per ${category.unitLabel} (Rp) *", "Price per ${category.localizedUnitLabel(true)} (Rp) *")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("service_price_input")
          )
          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(tr("Keterangan (Opsional)", "Description (Optional)")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isExtraFee, onCheckedChange = { isExtraFee = it })
            Text(tr("Tandai sebagai Biaya Tambahan (Express / Antar)", "Mark as Extra Fee (Express / Delivery)"), style = MaterialTheme.typography.bodySmall)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = active, onCheckedChange = { active = it })
            Text(tr("Layanan Aktif", "Active Service"), style = MaterialTheme.typography.bodySmall)
          }
          if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val priceVal = priceText.toLongOrNull() ?: -1L
            if (name.trim().length < 2) {
              errorMsg = if (isEn) "Service name must be at least 2 characters." else "Nama layanan minimal 2 karakter."
              return@Button
            }
            if (priceVal < 0L) {
              errorMsg = if (isEn) "Price cannot be negative." else "Harga tidak boleh negatif."
              return@Button
            }
            onSaveService(
              target.copy(
                name = name,
                category = category.name,
                unitLabel = category.unitLabel,
                price = priceVal,
                isExtraFee = isExtraFee,
                active = active,
                description = description
              )
            ) {
              editingService = null
            }
          },
          enabled = !isBusy,
          modifier = Modifier.testTag("save_service_button")
        ) { Text(tr("Simpan", "Save")) }
      },
      dismissButton = {
        TextButton(onClick = { editingService = null }) { Text(tr("Batal", "Cancel")) }
      }
    )
  }
}
