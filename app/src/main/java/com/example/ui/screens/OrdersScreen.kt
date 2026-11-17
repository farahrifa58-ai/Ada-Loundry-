package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppLanguage
import com.example.data.model.ActiveShopSession
import com.example.data.model.LaundryOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ReportPeriod
import com.example.data.model.ShopProfile
import com.example.data.model.UserRole
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.components.WhatsAppInvalidDialog
import com.example.ui.components.launchWhatsAppForOrder
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainerLight
import com.example.util.Formatters
import com.example.util.LocalAppLanguage
import com.example.util.localizedUnit
import com.example.util.tr

@Composable
fun OrdersScreen(
  session: ActiveShopSession,
  shopProfile: ShopProfile,
  orders: List<LaundryOrder>,
  selectedOrderFromOutside: LaundryOrder?,
  isBusy: Boolean,
  onClearExternalOrder: () -> Unit,
  onUpdateStatus: (LaundryOrder, OrderStatus) -> Unit,
  onCancelOrder: (LaundryOrder, String) -> Unit,
  onRecordPayment: (orderId: String, reqId: String, amount: Long, method: PaymentMethod, isDp: Boolean) -> Unit,
  onLogWhatsAppOpened: (LaundryOrder, String) -> Unit,
  onOpenReceipt: (LaundryOrder) -> Unit
) {
  val context = LocalContext.current
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  val isOwner = session.role == UserRole.OWNER
  val canUpdateStatus = isOwner || session.permissions.canUpdateStatus
  val canRecordPay = isOwner || session.permissions.canRecordPayment
  val canSendWa = isOwner || session.permissions.canSendWhatsApp

  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
  var dateFilter by remember { mutableStateOf<ReportPeriod?>(null) }
  var cardInvalidWaMsg by remember { mutableStateOf<String?>(null) }
  var quickPayOrder by remember { mutableStateOf<LaundryOrder?>(null) }

  var activeOrderId by remember { mutableStateOf<String?>(null) }
  if (selectedOrderFromOutside != null && activeOrderId == null) {
    activeOrderId = selectedOrderFromOutside.id
    onClearExternalOrder()
  }

  val activeOrder = remember(orders, activeOrderId) {
    orders.firstOrNull { it.id == activeOrderId }
  }

  val filteredOrders = remember(orders, searchQuery, statusFilter, dateFilter) {
    val q = searchQuery.trim().lowercase()
    val now = System.currentTimeMillis()
    val minMillis = dateFilter?.let { Formatters.startOfPeriodMillis(it, now) } ?: 0L

    orders.filter { ord ->
      val matchQuery = if (q.isEmpty()) true else {
        ord.notaNumber.lowercase().contains(q) ||
          ord.customerName.lowercase().contains(q) ||
          ord.customerPhone.contains(q)
      }
      val matchStatus = statusFilter == null || ord.orderStatusEnum == statusFilter
      val matchDate = dateFilter == null || ord.transactionAtMillis >= minMillis
      matchQuery && matchStatus && matchDate
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      label = { Text(tr("Cari No. Nota, Nama Pelanggan, atau WhatsApp", "Search Receipt No., Customer Name, or WhatsApp")) },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("orders_search_input")
    )

    // Single-line side-by-side status filter row
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      item {
        FilterChip(
          selected = statusFilter == null,
          onClick = { statusFilter = null },
          label = { Text(tr("Semua (${orders.size})", "All (${orders.size})"), maxLines = 1) }
        )
      }
      items(
        listOf(
          OrderStatus.RECEIVED,
          OrderStatus.PROCESSING,
          OrderStatus.READY,
          OrderStatus.COMPLETED,
          OrderStatus.CANCELLED
        )
      ) { st ->
        FilterChip(
          selected = statusFilter == st,
          onClick = { statusFilter = if (statusFilter == st) null else st },
          label = { Text(st.localizedLabel(isEn), maxLines = 1) }
        )
      }
    }

    // Single-line side-by-side date filter row
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      item {
        FilterChip(
          selected = dateFilter == null,
          onClick = { dateFilter = null },
          label = { Text(tr("Semua Tanggal", "All Dates"), maxLines = 1) }
        )
      }
      items(listOf(ReportPeriod.DAILY, ReportPeriod.WEEKLY, ReportPeriod.MONTHLY)) { period ->
        FilterChip(
          selected = dateFilter == period,
          onClick = { dateFilter = if (dateFilter == period) null else period },
          label = { Text(period.localizedLabel(isEn), maxLines = 1) }
        )
      }
    }

    if (filteredOrders.isEmpty()) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          tr("Tidak ada pesanan yang cocok dengan filter saat ini.", "No orders match the current filter."),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(24.dp)
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(filteredOrders, key = { it.id }) { order ->
          OrderCardItem(
            order = order,
            canUpdateStatus = canUpdateStatus,
            canRecordPay = canRecordPay,
            canSendWa = canSendWa,
            isBusy = isBusy,
            onClick = { activeOrderId = order.id },
            onQuickStatusChange = { newStatus -> onUpdateStatus(order, newStatus) },
            onQuickPayClick = { quickPayOrder = order },
            onQuickWhatsApp = {
              launchWhatsAppForOrder(
                context = context,
                order = order,
                shopName = shopProfile.businessName.ifBlank { "ADA LAUNDRY" },
                customTemplate = shopProfile.whatsappTemplate,
                lang = lang,
                onOpened = { normalized -> onLogWhatsAppOpened(order, normalized) },
                onInvalidPhone = { err -> cardInvalidWaMsg = err }
              )
            },
            onOpenReceipt = { onOpenReceipt(order) }
          )
        }
        item { Spacer(Modifier.height(16.dp)) }
      }
    }
  }

  if (quickPayOrder != null) {
    val latestOrder = orders.firstOrNull { it.id == quickPayOrder!!.id } ?: quickPayOrder!!
    RecordPaymentDialog(
      order = latestOrder,
      isBusy = isBusy,
      onDismiss = { quickPayOrder = null },
      onConfirmPayment = { reqId, amount, method, isDp ->
        onRecordPayment(latestOrder.id, reqId, amount, method, isDp)
        quickPayOrder = null
      }
    )
  }

  if (activeOrder != null) {
    OrderDetailDialog(
      order = activeOrder,
      session = session,
      shopProfile = shopProfile,
      isBusy = isBusy,
      onDismiss = { activeOrderId = null },
      onUpdateStatus = { newStatus -> onUpdateStatus(activeOrder, newStatus) },
      onCancelOrder = { reason -> onCancelOrder(activeOrder, reason) },
      onRecordPayment = { reqId, amt, method, isDp ->
        onRecordPayment(activeOrder.id, reqId, amt, method, isDp)
      },
      onLogWhatsAppOpened = { normalized -> onLogWhatsAppOpened(activeOrder, normalized) },
      onOpenReceipt = { onOpenReceipt(activeOrder) }
    )
  }

  if (cardInvalidWaMsg != null) {
    WhatsAppInvalidDialog(message = cardInvalidWaMsg!!, onDismiss = { cardInvalidWaMsg = null })
  }
}

@Composable
private fun OrderCardItem(
  order: LaundryOrder,
  canUpdateStatus: Boolean,
  canRecordPay: Boolean,
  canSendWa: Boolean,
  isBusy: Boolean,
  onClick: () -> Unit,
  onQuickStatusChange: (OrderStatus) -> Unit,
  onQuickPayClick: () -> Unit,
  onQuickWhatsApp: () -> Unit,
  onOpenReceipt: () -> Unit
) {
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  val latestHistory = order.parsedStatusHistory.lastOrNull()
  val currentStatus = order.orderStatusEnum
  val waSent = order.parsedWhatsAppLogs.isNotEmpty()
  val stepStatuses = listOf(
    OrderStatus.RECEIVED to OrderStatus.RECEIVED.localizedTitleCase(isEn),
    OrderStatus.PROCESSING to OrderStatus.PROCESSING.localizedTitleCase(isEn),
    OrderStatus.READY to OrderStatus.READY.localizedTitleCase(isEn),
    OrderStatus.COMPLETED to OrderStatus.COMPLETED.localizedTitleCase(isEn)
  )

  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("order_card_${order.notaNumber}")
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Top metadata row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          order.notaNumber,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          PaymentStatusChip(order.paymentStatusEnum)
          OrderStatusChip(order.orderStatusEnum)
        }
      }

      // Main Customer + Amount Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(order.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            "WA: ${order.customerPhone}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            order.parsedItems.joinToString(", ") {
              "${it.serviceName} (${Formatters.formatQuantity(it.quantity, localizedUnit(it.unitLabel, isEn))})"
            },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            Formatters.formatRupiah(order.totalAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          if (order.remainingAmount > 0L) {
            Text(
              tr("Sisa: ${Formatters.formatRupiah(order.remainingAmount)}", "Remaining: ${Formatters.formatRupiah(order.remainingAmount)}"),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Dates & Latest History Summary
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            if (isEn) {
              "In: ${Formatters.formatDateTime(order.transactionAtMillis, lang)}  •  Pickup: ${Formatters.formatDateTime(order.estimatedFinishAtMillis, lang)}"
            } else {
              "Masuk: ${Formatters.formatDateTime(order.transactionAtMillis, lang)}  •  Ambil: ${Formatters.formatDateTime(order.estimatedFinishAtMillis, lang)}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (latestHistory != null) {
            val statusStr = OrderStatus.fromString(latestHistory.status).localizedLabel(isEn)
            val timeStr = Formatters.formatDateTime(latestHistory.changedAtMillis, lang)
            Text(
              if (isEn) "Latest update: $statusStr ($timeStr) by ${latestHistory.changedByName}"
              else "Riwayat terakhir: $statusStr ($timeStr) oleh ${latestHistory.changedByName}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Status Controls placed neatly BELOW each transaction's summary/history
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      if (!currentStatus.isTerminal && canUpdateStatus) {
        Text(
          tr("Ubah Status Pesanan:", "Update Order Status:"),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          stepStatuses.forEach { (st, shortTitle) ->
            val isCurrent = currentStatus == st
            val isPassed = st.stepIndex < currentStatus.stepIndex
            val isSelectable = !isCurrent && !isPassed && st.stepIndex > currentStatus.stepIndex
            Surface(
              onClick = { if (isSelectable && !isBusy) onQuickStatusChange(st) },
              enabled = isSelectable && !isBusy,
              shape = RoundedCornerShape(10.dp),
              color = when {
                isCurrent && st == OrderStatus.READY -> EmeraldAccent
                isCurrent -> MaterialTheme.colorScheme.primary
                isPassed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
              },
              border = when {
                isCurrent -> null
                isPassed -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
              },
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("card_status_${order.notaNumber}_${st.name.lowercase()}")
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                if (isCurrent) {
                  Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(Modifier.width(3.dp))
                }
                Text(
                  text = shortTitle,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                  color = when {
                    isCurrent -> Color.White
                    isPassed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.onSurface
                  },
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              }
            }
          }
        }
      } else {
        Text(
          if (isEn) "Final Status: ${order.orderStatusEnum.localizedLabel(true)} (History Locked)"
          else "Status Final: ${order.orderStatusEnum.label} (Riwayat Terkunci)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Payment status & instant pay row right after status (especially when READY / UNPAID / DP)
      if (!currentStatus.isTerminal && order.remainingAmount > 0L && canRecordPay) {
        Surface(
          color = if (currentStatus == OrderStatus.READY) {
            com.example.ui.theme.WarningContainerLight.copy(alpha = 0.78f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isEn) "Payment Status: ${order.paymentStatusEnum.localizedLabel(true)}" else "Status Bayar: ${order.paymentStatusEnum.label}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (currentStatus == OrderStatus.READY) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isEn) "Remaining Bill: ${Formatters.formatRupiah(order.remainingAmount)}" else "Sisa Tagihan: ${Formatters.formatRupiah(order.remainingAmount)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
              )
            }
            Button(
              onClick = onQuickPayClick,
              enabled = !isBusy,
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              modifier = Modifier.height(40.dp).testTag("card_pay_${order.notaNumber}")
            ) {
              Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(5.dp))
              Text(tr("Bayar", "Pay"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
          }
        }
      } else if (currentStatus == OrderStatus.READY && order.remainingAmount == 0L) {
        Surface(
          color = EmeraldContainerLight.copy(alpha = 0.65f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
            Text(
              text = tr("Status Bayar: Lunas • Siap diserahkan ke pelanggan", "Payment Status: Paid • Ready to hand over"),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF064E3B)
            )
          }
        }
      }

      // Quick Action Row: WhatsApp Ready (with sent checkmark) & Nota Digital
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (order.orderStatusEnum == OrderStatus.READY && canSendWa) {
          Button(
            onClick = onQuickWhatsApp,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (waSent) Color(0xFF047857) else EmeraldAccent,
              contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).height(44.dp).testTag("card_wa_${order.notaNumber}")
          ) {
            Icon(
              imageVector = if (waSent) Icons.Filled.CheckCircle else Icons.Filled.Chat,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = if (waSent) tr("WA Terkirim ✓", "WA Sent ✓") else tr("Kirim WA Siap", "Send WA Ready"),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
          }
        }
        OutlinedButton(
          onClick = onOpenReceipt,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f).height(44.dp).testTag("card_receipt_${order.notaNumber}")
        ) {
          Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(tr("Lihat Nota", "View Receipt"), style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
private fun OrderDetailDialog(
  order: LaundryOrder,
  session: ActiveShopSession,
  shopProfile: ShopProfile,
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onUpdateStatus: (OrderStatus) -> Unit,
  onCancelOrder: (String) -> Unit,
  onRecordPayment: (reqId: String, amount: Long, method: PaymentMethod, isDp: Boolean) -> Unit,
  onLogWhatsAppOpened: (String) -> Unit,
  onOpenReceipt: () -> Unit
) {
  val context = LocalContext.current
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN
  val isOwner = session.role == UserRole.OWNER
  val canUpdateStatus = isOwner || session.permissions.canUpdateStatus
  val canRecordPay = isOwner || session.permissions.canRecordPayment
  val canSendWa = isOwner || session.permissions.canSendWhatsApp
  val canCancel = isOwner || session.permissions.canCancelOrder

  var showPayDialog by remember { mutableStateOf(false) }
  var showCancelDialog by remember { mutableStateOf(false) }
  var invalidWaMsg by remember { mutableStateOf<String?>(null) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 14.dp)
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
          Column(modifier = Modifier.weight(1f)) {
            Text(order.notaNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("${order.customerName} • ${order.customerPhone}", style = MaterialTheme.typography.bodySmall)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = tr("Tutup", "Close"))
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OrderStatusChip(order.orderStatusEnum)
          PaymentStatusChip(order.paymentStatusEnum)
        }

        // PROMINENT WHATSAPP BUTTON WHEN READY ("SIAP DIAMBIL")
        if (order.orderStatusEnum == OrderStatus.READY && canSendWa) {
          val waLogs = order.parsedWhatsAppLogs
          Card(
            colors = CardDefaults.cardColors(containerColor = EmeraldContainerLight),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                tr("Cucian Siap Diambil — Beritahu Pelanggan", "Laundry Ready for Pickup — Notify Customer"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldAccent
              )
              Button(
                onClick = {
                  launchWhatsAppForOrder(
                    context = context,
                    order = order,
                    shopName = shopProfile.businessName.ifBlank { "ADA LAUNDRY" },
                    customTemplate = shopProfile.whatsappTemplate,
                    lang = lang,
                    onOpened = { normalized -> onLogWhatsAppOpened(normalized) },
                    onInvalidPhone = { err -> invalidWaMsg = err }
                  )
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (waLogs.isNotEmpty()) Color(0xFF047857) else EmeraldAccent,
                  contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("send_whatsapp_ready_button")
              ) {
                Icon(
                  imageVector = if (waLogs.isNotEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Chat,
                  contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  if (waLogs.isEmpty()) tr("KIRIM WHATSAPP — CUCIAN SELESAI", "SEND WHATSAPP — LAUNDRY READY")
                  else tr("WA Terkirim ✓ (Kirim Ulang)", "WA Sent ✓ (Resend)"),
                  fontWeight = FontWeight.Bold
                )
              }
              Text(
                tr(
                  "Catatan: Setelah WhatsApp terbuka, tekan tombol KIRIM di WhatsApp sendiri.",
                  "Note: Once WhatsApp opens, tap SEND inside WhatsApp."
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF064E3B)
              )
              if (waLogs.isNotEmpty()) {
                val lastWa = waLogs.last()
                Text(
                  if (isEn) "WhatsApp link last opened: ${Formatters.formatDateTime(lastWa.openedAtMillis, lang)} by ${lastWa.openedByName}"
                  else "Tautan WhatsApp terakhir dibuka: ${Formatters.formatDateTime(lastWa.openedAtMillis, lang)} oleh ${lastWa.openedByName}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF064E3B)
                )
              }
            }
          }
        }

        HorizontalDivider()
        Text(tr("Rincian Layanan", "Service Details"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        order.parsedItems.forEach { item ->
          val unitLbl = localizedUnit(item.unitLabel, isEn)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
              Text(item.serviceName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
              Text("${Formatters.formatQuantity(item.quantity, unitLbl)} x ${Formatters.formatRupiah(item.unitPrice)}", style = MaterialTheme.typography.bodySmall)
            }
            Text(Formatters.formatRupiah(item.lineTotal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
          }
        }

        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(tr("Subtotal", "Subtotal"), style = MaterialTheme.typography.bodySmall)
          Text(Formatters.formatRupiah(order.subtotal), style = MaterialTheme.typography.bodySmall)
        }
        if (order.discount > 0L) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Diskon", "Discount"), style = MaterialTheme.typography.bodySmall)
            Text("-${Formatters.formatRupiah(order.discount)}", style = MaterialTheme.typography.bodySmall)
          }
        }
        if (order.extraFee > 0L) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(order.extraFeeLabel.ifBlank { tr("Biaya Tambahan", "Extra Fee") }, style = MaterialTheme.typography.bodySmall)
            Text(Formatters.formatRupiah(order.extraFee), style = MaterialTheme.typography.bodySmall)
          }
        }
        if (order.taxAmount > 0L) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val rTxt = if (order.taxRatePercent % 1.0 == 0.0) order.taxRatePercent.toInt().toString() else order.taxRatePercent.toString()
            Text(tr("PPN ($rTxt%)", "Tax ($rTxt%)"), style = MaterialTheme.typography.bodySmall)
            Text(Formatters.formatRupiah(order.taxAmount), style = MaterialTheme.typography.bodySmall)
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(tr("Total Tagihan", "Total Bill"), fontWeight = FontWeight.Bold)
          Text(Formatters.formatRupiah(order.totalAmount), fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(tr("Sudah Dibayar", "Already Paid"), style = MaterialTheme.typography.bodySmall)
          Text(Formatters.formatRupiah(order.paidAmount), style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(tr("Sisa Pembayaran", "Remaining Balance"), fontWeight = FontWeight.Bold, color = if (order.remainingAmount > 0L) MaterialTheme.colorScheme.error else EmeraldAccent)
          Text(Formatters.formatRupiah(order.remainingAmount), fontWeight = FontWeight.Bold, color = if (order.remainingAmount > 0L) MaterialTheme.colorScheme.error else EmeraldAccent)
        }

        // Actions: Payment, Receipt, Cancel
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          if (order.remainingAmount > 0L && !order.orderStatusEnum.isTerminal && canRecordPay) {
            Button(
              onClick = { showPayDialog = true },
              enabled = !isBusy,
              modifier = Modifier.weight(1f).height(48.dp).testTag("open_payment_dialog_button")
            ) {
              Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(tr("Bayar / Lunas", "Pay / Settle"))
            }
          }
          OutlinedButton(
            onClick = onOpenReceipt,
            modifier = Modifier.weight(1f).height(48.dp).testTag("view_digital_receipt_button")
          ) {
            Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Nota Digital", "Digital Receipt"))
          }
        }

        if (!order.orderStatusEnum.isTerminal && canCancel) {
          OutlinedButton(
            onClick = { showCancelDialog = true },
            enabled = !isBusy,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Batalkan Pesanan", "Cancel Order"))
          }
        }

        // History timelines first
        HorizontalDivider()
        Text(tr("Riwayat Status", "Status History"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        order.parsedStatusHistory.forEach { h ->
          val noteLocalized = h.localizedNote(isEn)
          Text(
            "• ${OrderStatus.fromString(h.status).localizedLabel(isEn)} — ${Formatters.formatDateTime(h.changedAtMillis, lang)} (${h.changedByName})${if (noteLocalized.isNotBlank()) ": $noteLocalized" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (order.parsedPaymentHistory.isNotEmpty()) {
          Text(tr("Riwayat Pembayaran", "Payment History"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          order.parsedPaymentHistory.forEach { p ->
            Text(
              "• ${Formatters.formatRupiah(p.amount)} (${PaymentMethod.fromString(p.method).localizedLabel(isEn)}) — ${Formatters.formatDateTime(p.paidAtMillis, lang)} (${p.recordedByName})",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Status management placed neatly BELOW Riwayat Status
        HorizontalDivider()
        if (!order.orderStatusEnum.isTerminal && canUpdateStatus) {
          Text(tr("Ubah Status Pesanan", "Update Order Status"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val currentIdx = order.orderStatusEnum.stepIndex
            listOf(OrderStatus.RECEIVED, OrderStatus.PROCESSING, OrderStatus.READY, OrderStatus.COMPLETED).forEach { st ->
              val isCurrent = order.orderStatusEnum == st
              val isSelectable = !isCurrent && st.stepIndex > currentIdx
              FilterChip(
                selected = isCurrent,
                enabled = isSelectable && !isBusy,
                onClick = { onUpdateStatus(st) },
                label = { Text(st.localizedLabel(isEn), maxLines = 1) },
                modifier = Modifier.testTag("change_status_${st.name.lowercase()}")
              )
            }
          }
        } else if (order.orderStatusEnum.isTerminal) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              if (isEn) "Status ${order.orderStatusEnum.localizedLabel(true)} is final and locked to preserve audit history."
              else "Status ${order.orderStatusEnum.label} bersifat final dan terkunci untuk menjaga keutuhan riwayat.",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }
  }

  if (showPayDialog) {
    RecordPaymentDialog(
      order = order,
      isBusy = isBusy,
      onDismiss = { showPayDialog = false },
      onConfirmPayment = { reqId, amount, method, isDp ->
        onRecordPayment(reqId, amount, method, isDp)
        showPayDialog = false
      }
    )
  }

  if (showCancelDialog) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showCancelDialog = false },
      title = { Text(tr("Konfirmasi Pembatalan", "Confirm Cancellation")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            tr(
              "Pesanan ${order.notaNumber} akan ditandai DIBATALKAN dan tidak dapat diubah kembali.",
              "Order ${order.notaNumber} will be marked CANCELLED and cannot be undone."
            )
          )
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text(tr("Alasan pembatalan (wajib)", "Cancellation reason (required)")) },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onCancelOrder(reason)
            showCancelDialog = false
          },
          enabled = reason.trim().length >= 3 && !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text(tr("Ya, Batalkan", "Yes, Cancel"))
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelDialog = false }) { Text(tr("Kembali", "Back")) }
      }
    )
  }

  if (invalidWaMsg != null) {
    WhatsAppInvalidDialog(message = invalidWaMsg!!, onDismiss = { invalidWaMsg = null })
  }
}
