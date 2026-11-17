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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.model.ActiveShopSession
import com.example.data.model.Expense
import com.example.data.model.LaundryOrder
import com.example.data.model.ReportPeriod
import com.example.data.model.ShopProfile
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldAccent
import com.example.util.Formatters
import com.example.util.LocalAppLanguage
import com.example.util.ReceiptAndReportExporter
import com.example.util.localizedUnit
import com.example.util.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
  session: ActiveShopSession,
  shopProfile: ShopProfile,
  orders: List<LaundryOrder>,
  expenses: List<Expense>,
  onNotify: (String) -> Unit
) {
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  val isAllowed = session.role == UserRole.OWNER || session.permissions.canViewReports
  if (!isAllowed) {
    Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
      Spacer(Modifier.height(10.dp))
      Text(tr("Akses Laporan Dibatasi", "Reports Access Restricted"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(tr("Anda tidak memiliki izin untuk melihat laporan keuangan.", "You do not have permission to view financial reports."), style = MaterialTheme.typography.bodySmall)
    }
    return
  }

  val context = LocalContext.current
  var period by remember { mutableStateOf(ReportPeriod.MONTHLY) }
  var searchQuery by remember { mutableStateOf("") }

  val now = System.currentTimeMillis()
  val startMillis = remember(period, now) { Formatters.startOfPeriodMillis(period, now) }
  val endMillis = remember(now) { Formatters.endOfDayMillis(now) }

  val summary = remember(orders, expenses, startMillis, endMillis, searchQuery) {
    Formatters.computeReportSummary(orders, expenses, startMillis, endMillis, searchQuery)
  }

  val periodOrders = remember(orders, startMillis, endMillis, searchQuery) {
    val q = searchQuery.trim().lowercase()
    orders.filter { ord ->
      ord.transactionAtMillis in startMillis..endMillis &&
        (q.isEmpty() || ord.notaNumber.lowercase().contains(q) || ord.customerName.lowercase().contains(q))
    }
  }
  val periodExpenses = remember(expenses, startMillis, endMillis) {
    expenses.filter { it.dateMillis in startMillis..endMillis }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ReportPeriod.entries.forEach { p ->
        FilterChip(
          selected = period == p,
          onClick = { period = p },
          label = { Text(p.localizedLabel(isEn)) }
        )
      }
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      label = { Text(tr("Cari nota / pelanggan / layanan", "Search receipt / customer / service")) },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    // Financial Overview
    val periodLabel = period.localizedLabel(isEn)
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tr("Ringkasan Keuangan ($periodLabel)", "Financial Summary ($periodLabel)"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ReportLine(tr("Total Omzet (Nilai Transaksi)", "Total Gross Revenue (Order Value)"), Formatters.formatRupiah(summary.totalOmzet), bold = true)
        ReportLine(
          tr("Total Transaksi Aktif", "Total Active Transactions"),
          if (isEn) "${summary.totalTransactions} orders" else "${summary.totalTransactions} transaksi"
        )
        HorizontalDivider()
        ReportLine(tr("Pembayaran Tunai", "Cash Payments"), Formatters.formatRupiah(summary.totalCashPaid))
        ReportLine(tr("Pembayaran Transfer Bank", "Bank Transfer Payments"), Formatters.formatRupiah(summary.totalTransferPaid))
        ReportLine(tr("Pencatatan Manual QRIS", "Manual QRIS Records"), Formatters.formatRupiah(summary.totalQrisPaid))
        ReportLine(tr("Total Pembayaran Masuk", "Total Cash Inflow"), Formatters.formatRupiah(summary.totalPaidIncome), bold = true)
        HorizontalDivider()
        ReportLine(tr("Total Pembayaran Lunas", "Total Full Payments"), Formatters.formatRupiah(summary.totalLunasAmount))
        ReportLine(tr("Total Uang Muka (DP)", "Total Down Payments"), Formatters.formatRupiah(summary.totalDpAmount))
        ReportLine(tr("Total Sisa Tagihan Pelanggan", "Total Customer Receivables"), Formatters.formatRupiah(summary.totalUnpaidReceivables))
        HorizontalDivider()
        ReportLine(tr("Total Pengeluaran Usaha", "Total Business Expenses"), Formatters.formatRupiah(summary.totalExpenses))
        Surface(
          color = if (summary.estimatedNetProfit >= 0L) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(tr("Estimasi Laba Bersih", "Estimated Net Profit"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(tr("Kas masuk aktual dikurangi pengeluaran", "Actual cash inflow minus expenses"), style = MaterialTheme.typography.labelSmall)
            }
            Text(
              Formatters.formatRupiah(summary.estimatedNetProfit),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = if (summary.estimatedNetProfit >= 0L) EmeraldAccent else MaterialTheme.colorScheme.error
            )
          }
        }
      }
    }

    // By Service Category
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tr("Laporan Per Kategori Layanan", "Report by Service Category"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ReportLine(
          if (isEn) "By Weight (${summary.kiloanCount} orders)" else "Laundry Kiloan (${summary.kiloanCount} transaksi)",
          Formatters.formatRupiah(summary.kiloanRevenue)
        )
        ReportLine(
          if (isEn) "Per Item (${summary.satuanCount} orders)" else "Laundry Satuan (${summary.satuanCount} transaksi)",
          Formatters.formatRupiah(summary.satuanRevenue)
        )
        ReportLine(
          if (isEn) "Carpet Per Sheet (${summary.karpetCount} orders)" else "Laundry Karpet Per Lembar (${summary.karpetCount} transaksi)",
          Formatters.formatRupiah(summary.karpetRevenue)
        )
      }
    }

    // Top Services
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tr("Layanan Paling Banyak Digunakan", "Most Popular Services"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (summary.topServices.isEmpty()) {
          Text(tr("Belum ada data layanan pada periode ini.", "No service data for this period."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
          summary.topServices.take(8).forEachIndexed { idx, stat ->
            val unitLbl = localizedUnit(stat.unitLabel, isEn)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${idx + 1}. ${stat.serviceName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                  if (isEn) "${stat.orderCount}x • ${Formatters.formatQuantity(stat.totalQuantity, unitLbl)}"
                  else "${stat.orderCount} kali • ${Formatters.formatQuantity(stat.totalQuantity, unitLbl)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(Formatters.formatRupiah(stat.totalRevenue), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Exports: PDF & Excel (.xlsx)
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Ekspor Laporan", "Export Report"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = {
              ReceiptAndReportExporter.exportReportPdf(
                context, shopProfile, periodLabel, summary, periodOrders, periodExpenses, shareAfterSave = false, lang = lang
              ).onSuccess { path ->
                onNotify(if (isEn) "PDF report saved to $path" else "Laporan PDF disimpan ke $path")
              }.onFailure { err ->
                onNotify(if (isEn) "Failed to export PDF: ${err.message}" else "Gagal ekspor PDF: ${err.message}")
              }
            },
            modifier = Modifier.weight(1f).height(48.dp).testTag("export_report_pdf_button")
          ) {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Simpan PDF", "Save PDF"))
          }
          OutlinedButton(
            onClick = {
              ReceiptAndReportExporter.exportReportPdf(
                context, shopProfile, periodLabel, summary, periodOrders, periodExpenses, shareAfterSave = true, lang = lang
              ).onFailure { err ->
                onNotify(if (isEn) "Failed to share PDF: ${err.message}" else "Gagal bagikan PDF: ${err.message}")
              }
            },
            modifier = Modifier.weight(1f).height(48.dp)
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Bagikan PDF", "Share PDF"))
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = {
              ReceiptAndReportExporter.exportReportExcelXlsx(
                context, shopProfile, periodLabel, summary, periodOrders, periodExpenses, shareAfterSave = false, lang = lang
              ).onSuccess { path ->
                onNotify(if (isEn) "Excel report (.xlsx) saved to $path" else "Laporan Excel (.xlsx) disimpan ke $path")
              }.onFailure { err ->
                onNotify(if (isEn) "Failed to export Excel: ${err.message}" else "Gagal ekspor Excel: ${err.message}")
              }
            },
            modifier = Modifier.weight(1f).height(48.dp).testTag("export_report_excel_button")
          ) {
            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Simpan Excel", "Save Excel"))
          }
          OutlinedButton(
            onClick = {
              ReceiptAndReportExporter.exportReportExcelXlsx(
                context, shopProfile, periodLabel, summary, periodOrders, periodExpenses, shareAfterSave = true, lang = lang
              ).onFailure { err ->
                onNotify(if (isEn) "Failed to share Excel: ${err.message}" else "Gagal bagikan Excel: ${err.message}")
              }
            },
            modifier = Modifier.weight(1f).height(48.dp)
          ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Bagikan Excel", "Share Excel"))
          }
        }
      }
    }

    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun ReportLine(label: String, value: String, bold: Boolean = false) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
  }
}
