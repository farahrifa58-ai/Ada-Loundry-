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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.local.AppLanguage
import com.example.data.model.ActiveShopSession
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ReportPeriod
import com.example.data.model.UserRole
import com.example.util.Formatters
import com.example.util.LocalAppLanguage
import com.example.util.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
  session: ActiveShopSession,
  expenses: List<Expense>,
  categories: List<ExpenseCategory>,
  isBusy: Boolean,
  onSaveExpense: (Expense, onSuccess: () -> Unit) -> Unit,
  onDeleteExpense: (String) -> Unit,
  onAddCategory: (String, onSuccess: () -> Unit) -> Unit
) {
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN

  if (session.role != UserRole.OWNER) {
    Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
      Spacer(Modifier.height(10.dp))
      Text(tr("Akses Pengeluaran Khusus Pemilik", "Expenses Access Is Owner-Only"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
        tr(
          "Data pengeluaran usaha hanya dapat diakses dan dikelola oleh Pemilik.",
          "Business expense records can only be viewed and managed by the Owner."
        ),
        style = MaterialTheme.typography.bodySmall
      )
    }
    return
  }

  var periodFilter by remember { mutableStateOf<ReportPeriod?>(ReportPeriod.MONTHLY) }
  var showAddExpense by remember { mutableStateOf(false) }
  var showAddCategory by remember { mutableStateOf(false) }

  val filteredExpenses = remember(expenses, periodFilter) {
    val now = System.currentTimeMillis()
    val start = periodFilter?.let { Formatters.startOfPeriodMillis(it, now) } ?: 0L
    expenses.filter { it.dateMillis >= start }
  }
  val totalFiltered = filteredExpenses.sumOf { it.amount }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)),
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val periodName = periodFilter?.localizedLabel(isEn) ?: tr("Semua", "All")
        Text(tr("Total Pengeluaran ($periodName)", "Total Expenses ($periodName)"), style = MaterialTheme.typography.labelLarge)
        Text(
          Formatters.formatRupiah(totalFiltered),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.error
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      Button(
        onClick = { showAddExpense = true },
        modifier = Modifier.weight(1f).height(48.dp).testTag("add_expense_button")
      ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(tr("Catat Pengeluaran", "Record Expense"))
      }
      OutlinedButton(
        onClick = { showAddCategory = true },
        modifier = Modifier.height(48.dp)
      ) {
        Icon(Icons.Filled.Category, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(tr("Kategori", "Category"))
      }
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = periodFilter == null,
        onClick = { periodFilter = null },
        label = { Text(tr("Semua", "All")) }
      )
      ReportPeriod.entries.forEach { period ->
        FilterChip(
          selected = periodFilter == period,
          onClick = { periodFilter = period },
          label = { Text(period.localizedLabel(isEn)) }
        )
      }
    }

    if (filteredExpenses.isEmpty()) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          tr("Belum ada pengeluaran tercatat pada periode ini.", "No expenses recorded for this period."),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(24.dp)
        )
      }
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filteredExpenses, key = { it.id }) { exp ->
          Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(exp.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(Formatters.formatDateTime(exp.dateMillis, lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (exp.notes.isNotBlank()) {
                  Text(exp.notes, style = MaterialTheme.typography.bodySmall)
                }
              }
              Text(
                Formatters.formatRupiah(exp.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
              )
              IconButton(onClick = { onDeleteExpense(exp.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = tr("Hapus Pengeluaran", "Delete Expense"), tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
        item { Spacer(Modifier.height(16.dp)) }
      }
    }
  }

  if (showAddExpense) {
    val categoryNames = remember(categories, isEn) {
      if (categories.isNotEmpty()) categories.map { it.name } else if (isEn) listOf(
        "Detergent", "Fragrance", "Laundry bags", "Electricity", "Water", "Staff salary", "Rent", "Machine maintenance", "Other expenses"
      ) else listOf(
        "Deterjen", "Pewangi", "Plastik laundry", "Listrik", "Air", "Gaji karyawan", "Sewa tempat", "Perawatan mesin", "Pengeluaran lainnya"
      )
    }
    var selectedCategory by remember { mutableStateOf(categoryNames.first()) }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showAddExpense = false },
      title = { Text(tr("Catat Pengeluaran", "Record Expense")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(tr("Pilih Kategori", "Select Category"), style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            categoryNames.forEach { cat ->
              FilterChip(
                selected = selectedCategory == cat,
                onClick = { selectedCategory = cat },
                label = { Text(cat) }
              )
            }
          }
          OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = { Text(tr("Nominal Pengeluaran (Rp) *", "Expense Amount (Rp) *")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
          )
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(tr("Catatan (Opsional)", "Notes (Optional)")) },
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
            val amt = amountText.toLongOrNull() ?: 0L
            if (amt <= 0L) {
              errorMsg = if (isEn) "Expense amount must be greater than Rp0." else "Nominal pengeluaran harus lebih dari Rp0."
              return@Button
            }
            onSaveExpense(
              Expense(
                category = selectedCategory,
                amount = amt,
                dateMillis = System.currentTimeMillis(),
                notes = notes
              )
            ) {
              showAddExpense = false
            }
          },
          enabled = !isBusy,
          modifier = Modifier.testTag("save_expense_button")
        ) { Text(tr("Simpan", "Save")) }
      },
      dismissButton = {
        TextButton(onClick = { showAddExpense = false }) { Text(tr("Batal", "Cancel")) }
      }
    )
  }

  if (showAddCategory) {
    var catName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddCategory = false },
      title = { Text(tr("Tambah Kategori Pengeluaran", "Add Expense Category")) },
      text = {
        OutlinedTextField(
          value = catName,
          onValueChange = { catName = it },
          label = { Text(tr("Nama Kategori Baru", "New Category Name")) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onAddCategory(catName) { showAddCategory = false }
          },
          enabled = catName.trim().length >= 2 && !isBusy
        ) { Text(tr("Tambah", "Add")) }
      },
      dismissButton = {
        TextButton(onClick = { showAddCategory = false }) { Text(tr("Batal", "Cancel")) }
      }
    )
  }
}
