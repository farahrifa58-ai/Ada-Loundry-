package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.model.ActiveShopSession
import com.example.data.model.LaundryOrder
import com.example.data.model.ReportPeriod
import com.example.data.model.ReportSummary
import com.example.data.model.UserRole
import com.example.ui.components.BusinessAvatarBadge
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.ui.components.SyncStatusBanner
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.WarningAmber
import com.example.util.Formatters

@Composable
fun DashboardScreen(
  session: ActiveShopSession,
  isOnline: Boolean,
  appLanguage: AppLanguage = AppLanguage.ID,
  selectedPeriod: ReportPeriod,
  summary: ReportSummary,
  recentOrders: List<LaundryOrder>,
  onSelectPeriod: (ReportPeriod) -> Unit,
  onNewTransactionClick: () -> Unit,
  onNavigateOrders: () -> Unit,
  onOrderClick: (LaundryOrder) -> Unit
) {
  val isOwner = session.role == UserRole.OWNER
  val isEn = appLanguage == AppLanguage.EN

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    SyncStatusBanner(isOnline = isOnline)

    // Hero Banner with Primary CTA: TRANSAKSI BARU
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.large)
        .background(
          Brush.linearGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary,
              MaterialTheme.colorScheme.secondary
            )
          )
        )
        .padding(18.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            BusinessAvatarBadge(shop = session.shopProfile, size = 44.dp)
            Column {
              Text(
                text = session.shopProfile.businessName.ifBlank { "ADA LAUNDRY" },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${session.userName} • ${session.role.localizedLabel(isEn)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
              )
            }
          }
          Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = selectedPeriod.localizedLabel(isEn),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }

        Button(
          onClick = onNewTransactionClick,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
          ),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("dashboard_new_transaction_button")
        ) {
          Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
          Spacer(Modifier.width(10.dp))
          Text(
            if (isEn) "NEW TRANSACTION" else "TRANSAKSI BARU",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // Period filter in a clean single horizontal row
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(ReportPeriod.entries) { period ->
        FilterChip(
          selected = selectedPeriod == period,
          onClick = { onSelectPeriod(period) },
          label = { Text(period.localizedLabel(isEn), maxLines = 1) },
          modifier = Modifier.testTag("dashboard_period_${period.name.lowercase()}")
        )
      }
    }

    // Main KPI Grid (all numbers derived strictly from real Firestore database)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(
          title = if (isEn) "Income (${selectedPeriod.localizedLabel(true)})" else "Pendapatan (${selectedPeriod.label})",
          value = Formatters.formatRupiah(summary.totalPaidIncome),
          subtitle = if (isEn) "Gross: ${Formatters.formatRupiah(summary.totalOmzet)}" else "Omzet: ${Formatters.formatRupiah(summary.totalOmzet)}",
          icon = Icons.Filled.Wallet,
          accent = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f)
        )
        KpiCard(
          title = if (isEn) "Transactions" else "Jumlah Transaksi",
          value = "${summary.totalTransactions}",
          subtitle = if (isEn) "Cancelled: ${summary.cancelledOrders}" else "Batal: ${summary.cancelledOrders}",
          icon = Icons.Filled.ReceiptLong,
          accent = CyanSecondary,
          modifier = Modifier.weight(1f)
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(
          title = if (isEn) "New Orders" else "Pesanan Baru",
          value = "${summary.receivedOrders}",
          subtitle = if (isEn) "Received" else "Diterima",
          icon = Icons.Filled.Inventory2,
          accent = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f)
        )
        KpiCard(
          title = if (isEn) "In Progress" else "Sedang Diproses",
          value = "${summary.processingOrders}",
          subtitle = if (isEn) "Wash/Iron" else "Cuci/Setrika",
          icon = Icons.Filled.LocalLaundryService,
          accent = CyanSecondary,
          modifier = Modifier.weight(1f)
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(
          title = if (isEn) "Ready for Pickup" else "Siap Diambil",
          value = "${summary.readyOrders}",
          subtitle = if (isEn) "Ready for WA notify" else "Siap WA pelanggan",
          icon = Icons.Filled.CheckCircle,
          accent = EmeraldAccent,
          modifier = Modifier.weight(1f)
        )
        KpiCard(
          title = if (isEn) "Picked Up" else "Sudah Diambil",
          value = "${summary.completedOrders}",
          subtitle = if (isEn) "Completed" else "Selesai",
          icon = Icons.Filled.CheckCircle,
          accent = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f)
        )
      }

      if (isOwner) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          KpiCard(
            title = if (isEn) "Expenses (${selectedPeriod.localizedLabel(true)})" else "Pengeluaran (${selectedPeriod.label})",
            value = Formatters.formatRupiah(summary.totalExpenses),
            subtitle = if (isEn) "Recorded" else "Tercatat di kas",
            icon = Icons.Filled.MoneyOff,
            accent = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
          )
          KpiCard(
            title = if (isEn) "Estimated Profit" else "Estimasi Keuntungan",
            value = Formatters.formatRupiah(summary.estimatedNetProfit),
            subtitle = if (isEn) "Cash in - expenses" else "Kas masuk - pengeluaran",
            icon = Icons.Filled.TrendingUp,
            accent = if (summary.estimatedNetProfit >= 0) EmeraldAccent else WarningAmber,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Recent Orders
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        if (isEn) "Recent Orders" else "Pesanan Terbaru",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        if (isEn) "View All" else "Lihat Semua",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onNavigateOrders)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }

    if (recentOrders.isEmpty()) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            Icons.Filled.LocalLaundryService,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp)
          )
          Text(
            if (isEn) "No transactions yet" else "Belum ada transaksi tercatat",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            if (isEn) {
              "Tap NEW TRANSACTION above to record your first customer laundry order."
            } else {
              "Tekan tombol TRANSAKSI BARU di atas untuk mencatat cucian pelanggan pertama."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      recentOrders.take(5).forEach { order ->
        Card(
          onClick = { onOrderClick(order) },
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
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
              Text(order.notaNumber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              OrderStatusChip(order.orderStatusEnum)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(order.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                  order.parsedItems.joinToString(", ") { it.serviceName },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  Formatters.formatRupiah(order.totalAmount),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                PaymentStatusChip(order.paymentStatusEnum)
              }
            }
          }
        }
      }
    }

    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun KpiCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
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
        Text(
          text = title,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1
        )
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
    }
  }
}
