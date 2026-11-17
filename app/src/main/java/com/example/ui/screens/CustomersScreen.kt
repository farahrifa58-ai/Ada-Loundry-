package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppLanguage
import com.example.data.model.Customer
import com.example.data.model.LaundryOrder
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.util.Formatters
import com.example.util.LocalAppLanguage
import com.example.util.WhatsAppHelper
import com.example.util.tr

@Composable
fun CustomersScreen(
  customers: List<Customer>,
  orders: List<LaundryOrder>,
  isBusy: Boolean,
  onSaveCustomer: (Customer, onSuccess: () -> Unit) -> Unit,
  onCreateOrderForCustomer: (Customer) -> Unit,
  onOpenOrder: (LaundryOrder) -> Unit
) {
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  var searchQuery by remember { mutableStateOf("") }
  var editingCustomer by remember { mutableStateOf<Customer?>(null) }
  var selectedCustomerForHistory by remember { mutableStateOf<Customer?>(null) }

  val filtered = remember(customers, searchQuery) {
    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) customers else customers.filter {
      it.name.lowercase().contains(q) || it.phone.contains(q) || it.address.lowercase().contains(q)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text(tr("Cari nama / nomor WhatsApp", "Search name / WhatsApp")) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.weight(1f).testTag("customers_search_input")
      )
      Button(
        onClick = { editingCustomer = Customer() },
        modifier = Modifier.height(54.dp).testTag("add_customer_button")
      ) {
        Icon(Icons.Filled.Add, contentDescription = tr("Tambah Pelanggan", "Add Customer"))
      }
    }

    if (filtered.isEmpty()) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          tr(
            "Belum ada data pelanggan. Data otomatis tersimpan saat transaksi dibuat atau tambah secara manual.",
            "No customer data yet. Customers are saved automatically when orders are created, or add them manually."
          ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(24.dp)
        )
      }
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filtered, key = { it.id }) { cust ->
          Card(
            onClick = { selectedCustomerForHistory = cust },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Column {
                    Text(cust.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("WA: ${cust.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                IconButton(onClick = { editingCustomer = cust }) {
                  Icon(Icons.Filled.Edit, contentDescription = tr("Ubah Data Pelanggan", "Edit Customer"))
                }
              }
              if (cust.address.isNotBlank()) {
                Text(tr("Alamat: ${cust.address}", "Address: ${cust.address}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  if (isEn) "${cust.transactionCount} orders" else "${cust.transactionCount} transaksi",
                  style = MaterialTheme.typography.labelMedium
                )
                Text("Total: ${Formatters.formatRupiah(cust.totalSpent)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              }
              if (cust.lastTransactionAtMillis > 0L) {
                Text(
                  tr("Transaksi terakhir: ${Formatters.formatDateTime(cust.lastTransactionAtMillis, lang)}", "Last order: ${Formatters.formatDateTime(cust.lastTransactionAtMillis, lang)}"),
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

  if (editingCustomer != null) {
    val target = editingCustomer!!
    var name by remember(target) { mutableStateOf(target.name) }
    var phone by remember(target) { mutableStateOf(target.phone) }
    var address by remember(target) { mutableStateOf(target.address) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { editingCustomer = null },
      title = {
        Text(
          if (target.id.isBlank()) tr("Tambah Pelanggan", "Add Customer")
          else tr("Ubah Data Pelanggan", "Edit Customer")
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(tr("Nama Pelanggan *", "Customer Name *")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(tr("Nomor WhatsApp *", "WhatsApp Number *")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(tr("Alamat (Opsional)", "Address (Optional)")) },
            modifier = Modifier.fillMaxWidth()
          )
          if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.trim().length < 2) {
              errorMsg = if (isEn) "Name must be at least 2 characters." else "Nama minimal 2 karakter."
              return@Button
            }
            if (WhatsAppHelper.normalizeIndonesianPhone(phone) == null) {
              errorMsg = if (isEn) "Invalid WhatsApp number." else "Nomor WhatsApp tidak valid."
              return@Button
            }
            onSaveCustomer(target.copy(name = name, phone = phone, address = address)) {
              editingCustomer = null
            }
          },
          enabled = !isBusy
        ) { Text(tr("Simpan", "Save")) }
      },
      dismissButton = {
        TextButton(onClick = { editingCustomer = null }) { Text(tr("Batal", "Cancel")) }
      }
    )
  }

  if (selectedCustomerForHistory != null) {
    val cust = selectedCustomerForHistory!!
    val custOrders = remember(orders, cust) {
      orders.filter { it.customerId == cust.id || it.customerPhone == cust.phone }
    }
    Dialog(
      onDismissRequest = { selectedCustomerForHistory = null },
      properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 16.dp)
      ) {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(cust.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
              Text("WA: ${cust.phone}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { selectedCustomerForHistory = null }) {
              Icon(Icons.Filled.Close, contentDescription = tr("Tutup", "Close"))
            }
          }
          if (cust.address.isNotBlank()) {
            Text(tr("Alamat: ${cust.address}", "Address: ${cust.address}"), style = MaterialTheme.typography.bodySmall)
          }
          Button(
            onClick = {
              selectedCustomerForHistory = null
              onCreateOrderForCustomer(cust)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tr("Transaksi Baru untuk Pelanggan Ini", "New Order for This Customer"))
          }
          HorizontalDivider()
          Text(tr("Riwayat Pesanan (${custOrders.size})", "Order History (${custOrders.size})"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          if (custOrders.isEmpty()) {
            Text(tr("Belum ada pesanan tercatat.", "No orders recorded yet."), style = MaterialTheme.typography.bodySmall)
          } else {
            custOrders.forEach { ord ->
              Card(
                onClick = {
                  selectedCustomerForHistory = null
                  onOpenOrder(ord)
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ord.notaNumber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OrderStatusChip(ord.orderStatusEnum)
                  }
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Formatters.formatDateTime(ord.transactionAtMillis, lang), style = MaterialTheme.typography.bodySmall)
                    Text(Formatters.formatRupiah(ord.totalAmount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                  }
                  PaymentStatusChip(ord.paymentStatusEnum)
                }
              }
            }
          }
        }
      }
    }
  }
}
