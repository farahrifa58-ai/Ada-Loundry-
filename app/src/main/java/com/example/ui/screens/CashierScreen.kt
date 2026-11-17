package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.model.ActiveShopSession
import com.example.data.model.Customer
import com.example.data.model.LaundryOrder
import com.example.data.model.LaundryService
import com.example.data.model.OrderItem
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.ShopProfile
import com.example.data.repository.NewOrderRequest
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainerLight
import com.example.ui.theme.WarningContainerLight
import com.example.util.Formatters
import com.example.util.LocalAppLanguage
import com.example.util.WhatsAppHelper
import com.example.util.localizedUnit
import com.example.util.tr
import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
  session: ActiveShopSession,
  shopProfile: ShopProfile,
  services: List<LaundryService>,
  customers: List<Customer>,
  preselectedCustomer: Customer?,
  isBusy: Boolean,
  onClearPreselectedCustomer: () -> Unit,
  onSaveOrder: (NewOrderRequest, onSaved: (LaundryOrder) -> Unit) -> Unit,
  onOrderSavedShowReceipt: (LaundryOrder) -> Unit
) {
  val context = LocalContext.current
  val lang = LocalAppLanguage.current
  val isEn = lang == AppLanguage.EN

  var draftOrderId by remember { mutableStateOf(UUID.randomUUID().toString()) }
  var selectedCustomerId by remember { mutableStateOf<String?>(null) }
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var customerAddress by remember { mutableStateOf("") }
  var showCustomerPicker by remember { mutableStateOf(false) }

  LaunchedEffect(preselectedCustomer) {
    if (preselectedCustomer != null) {
      selectedCustomerId = preselectedCustomer.id
      customerName = preselectedCustomer.name
      customerPhone = preselectedCustomer.phone
      customerAddress = preselectedCustomer.address
      onClearPreselectedCustomer()
    }
  }

  var activeCategoryIndex by remember { mutableStateOf(0) }
  val categories = ServiceCategory.entries
  val selectedCategory = categories[activeCategoryIndex]

  val activeCatalog = remember(services, selectedCategory) {
    services.filter { it.active && !it.isExtraFee && it.categoryEnum == selectedCategory }
  }
  val extraFeeCatalog = remember(services) {
    services.filter { it.active && it.isExtraFee }
  }

  var selectedService by remember(activeCatalog) { mutableStateOf(activeCatalog.firstOrNull()) }
  var quantityText by remember(selectedCategory) { mutableStateOf("1") }

  val cartItems = remember { mutableStateListOf<OrderItem>() }

  var discountText by remember { mutableStateOf(if (shopProfile.defaultDiscountAmount > 0L) shopProfile.defaultDiscountAmount.toString() else "0") }
  var discountManuallyEdited by remember { mutableStateOf(false) }
  var extraFeeText by remember { mutableStateOf("0") }
  var extraFeeLabel by remember { mutableStateOf("") }
  var manualApplyTax by remember(shopProfile.taxEnabled, shopProfile.taxAutoApply) {
    mutableStateOf(shopProfile.taxEnabled && shopProfile.taxAutoApply)
  }
  var paymentStatus by remember { mutableStateOf(PaymentStatus.UNPAID) }
  var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var cashReceivedText by remember { mutableStateOf("") }
  var dpAmountText by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  var estimatedFinishMillis by remember {
    mutableStateOf(System.currentTimeMillis() + 2 * 86_400_000L)
  }
  var selectedQuickEstimateTag by remember { mutableStateOf("2d") }
  var useCustomPickupSchedule by remember { mutableStateOf(false) }

  var validationError by remember { mutableStateOf<String?>(null) }

  val cartSubtotalPreview = cartItems.sumOf { it.lineTotal }
  LaunchedEffect(cartSubtotalPreview, shopProfile.defaultDiscountPercent, shopProfile.defaultDiscountAmount) {
    if (!discountManuallyEdited) {
      val autoDisc = when {
        shopProfile.defaultDiscountPercent > 0.0 -> (cartSubtotalPreview * (shopProfile.defaultDiscountPercent / 100.0)).roundToLong()
        shopProfile.defaultDiscountAmount > 0L -> shopProfile.defaultDiscountAmount
        else -> 0L
      }
      discountText = autoDisc.toString()
    }
  }

  val discountValue = discountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val extraFeeValue = extraFeeText.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val activeTaxRate = if (shopProfile.taxEnabled && (shopProfile.taxAutoApply || manualApplyTax)) {
    shopProfile.taxRatePercent.coerceIn(0.0, 100.0)
  } else {
    0.0
  }

  val totalsBreakdown = Formatters.calculateOrderTotalsWithTax(
    items = cartItems,
    discount = discountValue,
    extraFee = extraFeeValue,
    taxRatePercent = activeTaxRate
  )
  val subtotal = totalsBreakdown.subtotal
  val safeDiscount = totalsBreakdown.discount
  val taxAmount = totalsBreakdown.taxAmount
  val totalAmount = totalsBreakdown.totalAmount

  val dpValue = dpAmountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val cashReceivedValue = cashReceivedText.filter { it.isDigit() }.toLongOrNull() ?: 0L

  val paidPreview = when (paymentStatus) {
    PaymentStatus.UNPAID -> 0L
    PaymentStatus.DP -> dpValue.coerceIn(0L, totalAmount)
    PaymentStatus.PAID -> totalAmount
  }
  val remainingPreview = (totalAmount - paidPreview).coerceAtLeast(0L)
  val changePreview = if (paymentStatus == PaymentStatus.PAID && paymentMethod == PaymentMethod.CASH && cashReceivedValue > totalAmount) {
    cashReceivedValue - totalAmount
  } else {
    0L
  }

  val canAccessCashier = session.permissions.canCreateOrder

  if (!canAccessCashier) {
    Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(tr("Akses Dibatasi", "Access Restricted"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(8.dp))
      Text(
        tr(
          "Akun kasir Anda belum memiliki izin untuk membuat transaksi baru. Hubungi Pemilik.",
          "Your cashier account does not have permission to create new orders. Please contact the Owner."
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    return
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Pelanggan Card
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(tr("1. Data Pelanggan", "1. Customer Details"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          OutlinedButton(
            onClick = { showCustomerPicker = true },
            modifier = Modifier.height(40.dp).testTag("pick_existing_customer_button")
          ) {
            Icon(Icons.Filled.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Pilih Pelanggan", "Select Customer"), style = MaterialTheme.typography.labelMedium)
          }
        }

        OutlinedTextField(
          value = customerName,
          onValueChange = {
            customerName = it
            selectedCustomerId = null
          },
          label = { Text(tr("Nama Pelanggan *", "Customer Name *")) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("cashier_customer_name_input")
        )
        OutlinedTextField(
          value = customerPhone,
          onValueChange = { customerPhone = it },
          label = { Text(tr("Nomor WhatsApp * (contoh: 08123456789)", "WhatsApp Number * (e.g. 08123456789)")) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          supportingText = {
            val normalized = WhatsAppHelper.normalizeIndonesianPhone(customerPhone)
            if (customerPhone.isNotBlank() && normalized == null) {
              Text(tr("Format nomor WhatsApp tidak valid", "Invalid WhatsApp number format"), color = MaterialTheme.colorScheme.error)
            } else if (normalized != null) {
              Text(tr("Format WA: $normalized", "WA Format: $normalized"), color = EmeraldAccent)
            }
          },
          modifier = Modifier.fillMaxWidth().testTag("cashier_customer_phone_input")
        )
        OutlinedTextField(
          value = customerAddress,
          onValueChange = { customerAddress = it },
          label = { Text(tr("Alamat (Opsional)", "Address (Optional)")) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    // 2. Pilih Layanan (Clean 2-Column Selectable Card Grid + Stepper)
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("2. Pilih Layanan Cucian", "2. Select Laundry Service"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        PrimaryTabRow(selectedTabIndex = activeCategoryIndex) {
          categories.forEachIndexed { idx, cat ->
            Tab(
              selected = activeCategoryIndex == idx,
              onClick = { activeCategoryIndex = idx },
              text = {
                Text(
                  when (cat) {
                    ServiceCategory.KILOAN -> tr("Kiloan", "By Weight")
                    ServiceCategory.SATUAN -> tr("Satuan", "Per Item")
                    ServiceCategory.KARPET -> tr("Karpet", "Carpet")
                  }
                )
              },
              modifier = Modifier.testTag("service_tab_${cat.name.lowercase()}")
            )
          }
        }

        if (selectedCategory == ServiceCategory.KARPET) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              tr(
                "Tarif karpet dihitung PER LEMBAR (bukan meter persegi).",
                "Carpet rate is billed PER SHEET (not per square meter)."
              ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        if (activeCatalog.isEmpty()) {
          Text(
            tr(
              "Belum ada layanan aktif pada kategori ini. Minta Pemilik menambahkan di menu Layanan.",
              "No active services in this category yet. Ask the Owner to add them under Services."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          // 2-Column Balanced Service Card Grid
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeCatalog.chunked(2).forEach { rowServices ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                rowServices.forEach { svc ->
                  val isSelected = selectedService?.id == svc.id
                  Surface(
                    onClick = { selectedService = svc },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    } else {
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                      } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                      }
                    ),
                    modifier = Modifier.weight(1f).height(76.dp)
                  ) {
                    Column(
                      modifier = Modifier.fillMaxSize().padding(10.dp),
                      verticalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                      ) {
                        Text(
                          text = svc.name,
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                          Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = tr("Terpilih", "Selected"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                          )
                        }
                      }
                      Text(
                        text = "${Formatters.formatRupiah(svc.price)}/${localizedUnit(svc.unitLabel, isEn)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }
                if (rowServices.size == 1) {
                  Spacer(Modifier.weight(1f))
                }
              }
            }
          }

          val currentSvc = selectedService ?: activeCatalog.first()
          val currentUnitLabel = localizedUnit(currentSvc.unitLabel, isEn)
          val parsedQty = if (selectedCategory == ServiceCategory.KILOAN) {
            quantityText.replace(",", ".").toDoubleOrNull() ?: 0.0
          } else {
            (quantityText.filter { it.isDigit() }.toIntOrNull() ?: 0).toDouble()
          }
          val previewLineTotal = Formatters.calculateLineTotal(parsedQty, currentSvc.price)

          // Clean Quantity Stepper + Add to Cart Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = {
                val step = if (selectedCategory == ServiceCategory.KILOAN) 0.5 else 1.0
                val next = (parsedQty - step).coerceAtLeast(if (selectedCategory == ServiceCategory.KILOAN) 0.5 else 1.0)
                quantityText = if (next % 1.0 == 0.0) next.toLong().toString() else next.toString()
              },
              contentPadding = PaddingValues(0.dp),
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.Filled.Remove, contentDescription = tr("Kurangi", "Decrease"))
            }

            OutlinedTextField(
              value = quantityText,
              onValueChange = { input ->
                quantityText = if (selectedCategory == ServiceCategory.KILOAN) {
                  input.filter { it.isDigit() || it == '.' || it == ',' }
                } else {
                  input.filter { it.isDigit() }
                }
              },
              label = {
                Text(
                  when (selectedCategory) {
                    ServiceCategory.KILOAN -> tr("Berat (kg)", "Weight (kg)")
                    ServiceCategory.SATUAN -> tr("Jumlah (pcs)", "Qty (pcs)")
                    ServiceCategory.KARPET -> tr("Jumlah (lembar)", "Qty (sheets)")
                  }
                )
              },
              keyboardOptions = KeyboardOptions(
                keyboardType = if (selectedCategory == ServiceCategory.KILOAN) {
                  KeyboardType.Decimal
                } else {
                  KeyboardType.Number
                }
              ),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("cashier_quantity_input")
            )

            OutlinedButton(
              onClick = {
                val step = if (selectedCategory == ServiceCategory.KILOAN) 0.5 else 1.0
                val next = parsedQty + step
                quantityText = if (next % 1.0 == 0.0) next.toLong().toString() else next.toString()
              },
              contentPadding = PaddingValues(0.dp),
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.Filled.Add, contentDescription = tr("Tambah jumlah", "Increase quantity"))
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "${Formatters.formatQuantity(parsedQty, currentUnitLabel)} x ${Formatters.formatRupiah(currentSvc.price)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                Formatters.formatRupiah(previewLineTotal),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Button(
            onClick = {
              if (parsedQty <= 0.0) {
                validationError = if (isEn) "Enter $currentUnitLabel greater than 0." else "Masukkan ${currentSvc.unitLabel} lebih dari 0."
                return@Button
              }
              cartItems.add(
                OrderItem(
                  serviceId = currentSvc.id,
                  serviceName = currentSvc.name,
                  category = currentSvc.category,
                  unitLabel = currentSvc.unitLabel,
                  quantity = parsedQty,
                  unitPrice = currentSvc.price,
                  lineTotal = previewLineTotal
                )
              )
              validationError = null
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_service_to_cart_button")
          ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(tr("Tambah Layanan ke Nota", "Add Service to Receipt"), fontWeight = FontWeight.Bold)
          }
        }

        if (cartItems.isNotEmpty()) {
          HorizontalDivider()
          Text(
            tr("Daftar Layanan dalam Nota (${cartItems.size}):", "Services on Receipt (${cartItems.size}):"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
          cartItems.forEachIndexed { index, item ->
            val unitLbl = localizedUnit(item.unitLabel, isEn)
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(item.serviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                  Text(
                    "${Formatters.formatQuantity(item.quantity, unitLbl)} x ${Formatters.formatRupiah(item.unitPrice)} = ${Formatters.formatRupiah(item.lineTotal)}",
                    style = MaterialTheme.typography.bodySmall
                  )
                }
                IconButton(onClick = { cartItems.removeAt(index) }) {
                  Icon(Icons.Filled.Delete, contentDescription = tr("Hapus item", "Remove item"), tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
    }

    // 3. Rincian Biaya & Estimasi Selesai (Organized cleanly into A & B subsections)
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          tr("3. Rincian Biaya & Estimasi Selesai", "3. Cost Breakdown & Pickup Estimate"),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        // Subsection A: Penyesuaian Biaya
        Text(
          tr("A. Penyesuaian Biaya (Diskon, Tambahan & PPN)", "A. Cost Adjustments (Discount, Extra Fee & Tax)"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )

        if (extraFeeCatalog.isNotEmpty()) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            extraFeeCatalog.forEach { extra ->
              FilterChip(
                selected = extraFeeLabel == extra.name && extraFeeValue == extra.price,
                onClick = {
                  if (extraFeeLabel == extra.name && extraFeeValue == extra.price) {
                    extraFeeLabel = ""
                    extraFeeText = "0"
                  } else {
                    extraFeeLabel = extra.name
                    extraFeeText = extra.price.toString()
                  }
                },
                label = { Text("${extra.name} (+${Formatters.formatRupiah(extra.price)})") }
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = discountText,
            onValueChange = {
              discountManuallyEdited = true
              discountText = it.filter { ch -> ch.isDigit() }
            },
            label = { Text(tr("Diskon (Rp)", "Discount (Rp)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("cashier_discount_input")
          )
          OutlinedTextField(
            value = extraFeeText,
            onValueChange = { extraFeeText = it.filter { ch -> ch.isDigit() } },
            label = { Text(tr("Biaya Tambahan (Rp)", "Extra Fee (Rp)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("cashier_extra_fee_input")
          )
        }

        if (extraFeeValue > 0L) {
          OutlinedTextField(
            value = extraFeeLabel,
            onValueChange = { extraFeeLabel = it },
            label = { Text(tr("Keterangan Biaya Tambahan (mis. Express)", "Extra Fee Label (e.g. Express)")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        if (shopProfile.taxEnabled) {
          val rateTxt = if (shopProfile.taxRatePercent % 1.0 == 0.0) {
            shopProfile.taxRatePercent.toInt().toString()
          } else {
            shopProfile.taxRatePercent.toString()
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (shopProfile.taxAutoApply) tr("PPN Otomatis ($rateTxt%)", "Automatic Tax ($rateTxt%)") else tr("Terapkan PPN Manual ($rateTxt%)", "Apply Manual Tax ($rateTxt%)"),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = tr("Nilai PPN: ${Formatters.formatRupiah(taxAmount)}", "Tax Amount: ${Formatters.formatRupiah(taxAmount)}"),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (!shopProfile.taxAutoApply) {
                Switch(
                  checked = manualApplyTax,
                  onCheckedChange = { manualApplyTax = it }
                )
              } else {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                  Text(
                    tr("AKTIF", "ACTIVE"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }

        HorizontalDivider()

        // Subsection B: Estimasi Selesai, Waktu Pengambilan & Catatan
        Text(
          tr("B. Estimasi Selesai & Waktu Pengambilan", "B. Completion Estimate & Pickup Time"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )

        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Text(
                if (useCustomPickupSchedule) tr("Jadwal Ambil", "Pickup Schedule") else tr("Estimasi Selesai", "Est. Ready"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              Formatters.formatDateTime(estimatedFinishMillis, lang),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        // Clean single-row 4-equal-width quick selection bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val quickOpts = listOf(
            "6h" to tr("+6 Jam", "+6 Hrs"),
            "1d" to tr("+1 Hari", "+1 Day"),
            "2d" to tr("+2 Hari", "+2 Days")
          )
          quickOpts.forEach { (tag, label) ->
            val selected = !useCustomPickupSchedule && selectedQuickEstimateTag == tag
            Surface(
              onClick = {
                useCustomPickupSchedule = false
                selectedQuickEstimateTag = tag
                estimatedFinishMillis = System.currentTimeMillis() + when (tag) {
                  "6h" -> 6 * 3_600_000L
                  "1d" -> 86_400_000L
                  else -> 2 * 86_400_000L
                }
              },
              shape = RoundedCornerShape(10.dp),
              color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
              ),
              modifier = Modifier.weight(1f).height(42.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1
                )
              }
            }
          }

          Surface(
            onClick = {
              val cal = Calendar.getInstance().apply { timeInMillis = estimatedFinishMillis }
              DatePickerDialog(
                context,
                { _, y, m, d ->
                  TimePickerDialog(
                    context,
                    { _, h, min ->
                      cal.set(y, m, d, h, min, 0)
                      estimatedFinishMillis = cal.timeInMillis
                      selectedQuickEstimateTag = "custom"
                      useCustomPickupSchedule = true
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                  ).show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
              ).show()
            },
            shape = RoundedCornerShape(10.dp),
            color = if (useCustomPickupSchedule) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
              1.dp,
              if (useCustomPickupSchedule) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            modifier = Modifier.weight(1f).height(42.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                tr("Pilih", "Custom"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (useCustomPickupSchedule) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
              )
            }
          }
        }

        // Optional explicit Day & Time pickup reminder scheduler
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  tr("Atur Hari & Jam Pengambilan (Opsional)", "Set Pickup Date & Time (Optional)"),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  tr("Pengingat suara berbunyi saat waktu pengambilan tiba", "Sound alarm rings when the pickup time arrives"),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Switch(
                checked = useCustomPickupSchedule,
                onCheckedChange = { checked ->
                  useCustomPickupSchedule = checked
                  if (!checked) {
                    selectedQuickEstimateTag = "2d"
                    estimatedFinishMillis = System.currentTimeMillis() + 2 * 86_400_000L
                  }
                },
                modifier = Modifier.testTag("toggle_custom_pickup_schedule")
              )
            }

            if (useCustomPickupSchedule) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = estimatedFinishMillis }
                    DatePickerDialog(
                      context,
                      { _, y, m, d ->
                        cal.set(Calendar.YEAR, y)
                        cal.set(Calendar.MONTH, m)
                        cal.set(Calendar.DAY_OF_MONTH, d)
                        estimatedFinishMillis = cal.timeInMillis
                      },
                      cal.get(Calendar.YEAR),
                      cal.get(Calendar.MONTH),
                      cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                  },
                  modifier = Modifier.weight(1f).height(48.dp).testTag("pick_pickup_date_button")
                ) {
                  Text(Formatters.formatDateOnly(estimatedFinishMillis, lang), maxLines = 1)
                }
                OutlinedButton(
                  onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = estimatedFinishMillis }
                    TimePickerDialog(
                      context,
                      { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, min)
                        cal.set(Calendar.SECOND, 0)
                        estimatedFinishMillis = cal.timeInMillis
                      },
                      cal.get(Calendar.HOUR_OF_DAY),
                      cal.get(Calendar.MINUTE),
                      true
                    ).show()
                  },
                  modifier = Modifier.weight(1f).height(48.dp).testTag("pick_pickup_time_button")
                ) {
                  Text(
                    if (isEn) "Time ${Formatters.formatTimeOnly(estimatedFinishMillis, lang)}"
                    else "Jam ${Formatters.formatTimeOnly(estimatedFinishMillis, lang)}",
                    maxLines = 1
                  )
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(tr("Catatan Khusus (Opsional)", "Special Notes (Optional)")) },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    // 4. Pembayaran & Ringkasan Nota
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("4. Pembayaran", "4. Payment"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PaymentStatus.entries.forEach { st ->
            FilterChip(
              selected = paymentStatus == st,
              onClick = {
                paymentStatus = st
                if (st == PaymentStatus.PAID && cashReceivedText.isBlank()) {
                  cashReceivedText = totalAmount.toString()
                }
              },
              label = { Text(st.localizedLabel(isEn)) },
              modifier = Modifier.testTag("payment_status_${st.name.lowercase()}")
            )
          }
        }

        if (paymentStatus != PaymentStatus.UNPAID) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethod.entries.forEach { method ->
              FilterChip(
                selected = paymentMethod == method,
                onClick = { paymentMethod = method },
                label = { Text(method.localizedLabel(isEn)) }
              )
            }
          }

          if (paymentMethod == PaymentMethod.QRIS) {
            Surface(color = WarningContainerLight, shape = RoundedCornerShape(8.dp)) {
              Text(
                tr(
                  "QRIS dicatat secara manual. Pastikan dana QRIS sudah masuk sebelum menandai lunas.",
                  "QRIS is recorded manually. Confirm funds arrived before marking as Paid."
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp)
              )
            }
          }

          if (paymentStatus == PaymentStatus.DP) {
            OutlinedTextField(
              value = dpAmountText,
              onValueChange = { dpAmountText = it.filter { ch -> ch.isDigit() } },
              label = { Text(tr("Nominal Uang Muka / DP (Rp)", "Down Payment Amount (Rp)")) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("cashier_dp_amount_input")
            )
          } else if (paymentMethod == PaymentMethod.CASH) {
            OutlinedTextField(
              value = cashReceivedText,
              onValueChange = { cashReceivedText = it.filter { ch -> ch.isDigit() } },
              label = { Text(tr("Uang Tunai Diterima (Rp)", "Cash Received (Rp)")) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("cashier_cash_received_input")
            )
          }
        }

        HorizontalDivider()
        SummaryRow(tr("Subtotal", "Subtotal"), Formatters.formatRupiah(subtotal))
        if (safeDiscount > 0L) SummaryRow(tr("Diskon", "Discount"), "-${Formatters.formatRupiah(safeDiscount)}")
        if (extraFeeValue > 0L) SummaryRow(extraFeeLabel.ifBlank { tr("Biaya Tambahan", "Extra Fee") }, Formatters.formatRupiah(extraFeeValue))
        if (taxAmount > 0L) {
          val rateTxt = if (activeTaxRate % 1.0 == 0.0) activeTaxRate.toInt().toString() else activeTaxRate.toString()
          SummaryRow(tr("PPN ($rateTxt%)", "Tax ($rateTxt%)"), Formatters.formatRupiah(taxAmount))
        }
        SummaryRow(tr("TOTAL TAGIHAN", "TOTAL BILL"), Formatters.formatRupiah(totalAmount), bold = true)
        SummaryRow(tr("Dibayar", "Paid"), Formatters.formatRupiah(paidPreview))
        SummaryRow(tr("Sisa Pembayaran", "Remaining"), Formatters.formatRupiah(remainingPreview), bold = true)
        if (changePreview > 0L) {
          Surface(color = EmeraldContainerLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
              tr("Kembalian: ${Formatters.formatRupiah(changePreview)}", "Change: ${Formatters.formatRupiah(changePreview)}"),
              color = EmeraldAccent,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (validationError != null) {
          Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
              validationError!!,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        Button(
          onClick = {
            if (customerName.trim().length < 2) {
              validationError = if (isEn) "Customer name must be at least 2 characters." else "Nama pelanggan wajib diisi minimal 2 karakter."
              return@Button
            }
            if (WhatsAppHelper.normalizeIndonesianPhone(customerPhone) == null) {
              validationError = if (isEn) "Invalid customer WhatsApp number (e.g. 08123456789)." else "Nomor WhatsApp pelanggan tidak valid (contoh: 08123456789)."
              return@Button
            }
            if (cartItems.isEmpty()) {
              validationError = if (isEn) "Please add at least 1 laundry service first." else "Tambahkan minimal 1 layanan cucian terlebih dahulu."
              return@Button
            }
            if (paymentStatus == PaymentStatus.DP && (dpValue <= 0L || dpValue > totalAmount)) {
              validationError = if (isEn) "Down payment must be between Rp1 and total bill." else "Nominal DP harus antara Rp1 dan total tagihan."
              return@Button
            }
            if (paymentStatus == PaymentStatus.PAID && paymentMethod == PaymentMethod.CASH && cashReceivedValue < totalAmount) {
              validationError = if (isEn) "Cash received is less than the total bill." else "Uang tunai diterima kurang dari total tagihan."
              return@Button
            }
            validationError = null

            val req = NewOrderRequest(
              draftOrderId = draftOrderId,
              customerId = selectedCustomerId,
              customerName = customerName,
              customerPhone = customerPhone,
              customerAddress = customerAddress,
              items = cartItems.toList(),
              discount = safeDiscount,
              extraFee = extraFeeValue,
              extraFeeLabel = extraFeeLabel,
              taxRatePercent = activeTaxRate,
              initialPaymentStatus = paymentStatus,
              paymentMethod = paymentMethod,
              receivedCashAmount = if (paymentMethod == PaymentMethod.CASH) cashReceivedValue else totalAmount,
              dpAmount = dpValue,
              notes = notes,
              estimatedFinishAtMillis = estimatedFinishMillis
            )

            onSaveOrder(req) { createdOrder ->
              draftOrderId = UUID.randomUUID().toString()
              selectedCustomerId = null
              customerName = ""
              customerPhone = ""
              customerAddress = ""
              cartItems.clear()
              discountManuallyEdited = false
              discountText = if (shopProfile.defaultDiscountAmount > 0L) shopProfile.defaultDiscountAmount.toString() else "0"
              extraFeeText = "0"
              extraFeeLabel = ""
              paymentStatus = PaymentStatus.UNPAID
              cashReceivedText = ""
              dpAmountText = ""
              notes = ""
              useCustomPickupSchedule = false
              selectedQuickEstimateTag = "2d"
              estimatedFinishMillis = System.currentTimeMillis() + 2 * 86_400_000L
              onOrderSavedShowReceipt(createdOrder)
            }
          },
          enabled = !isBusy,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("save_transaction_button")
        ) {
          if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(8.dp))
            Text(tr("Menyimpan Transaksi...", "Saving Transaction..."))
          } else {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tr("SIMPAN TRANSAKSI & BUAT NOTA", "SAVE TRANSACTION & RECEIPT"), fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Spacer(Modifier.height(16.dp))
  }

  if (showCustomerPicker) {
    CustomerPickerDialog(
      customers = customers,
      onDismiss = { showCustomerPicker = false },
      onSelect = { cust ->
        selectedCustomerId = cust.id
        customerName = cust.name
        customerPhone = cust.phone
        customerAddress = cust.address
        showCustomerPicker = false
      }
    )
  }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
  }
}

@Composable
private fun CustomerPickerDialog(
  customers: List<Customer>,
  onDismiss: () -> Unit,
  onSelect: (Customer) -> Unit
) {
  val isEn = com.example.util.isEnglish()
  var query by remember { mutableStateOf("") }
  val filtered = remember(customers, query) {
    val q = query.trim().lowercase()
    if (q.isEmpty()) customers else customers.filter {
      it.name.lowercase().contains(q) || it.phone.contains(q)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(tr("Pilih Pelanggan Terdaftar", "Select Registered Customer")) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text(tr("Cari nama atau WhatsApp", "Search name or WhatsApp")) },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        if (filtered.isEmpty()) {
          Text(tr("Belum ada data pelanggan cocok.", "No matching customers found."), style = MaterialTheme.typography.bodySmall)
        } else {
          LazyColumn(modifier = Modifier.height(260.dp)) {
            items(filtered, key = { it.id }) { cust ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelect(cust) }
                  .padding(vertical = 10.dp, horizontal = 4.dp)
              ) {
                Text(cust.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                  if (isEn) "${cust.phone} • ${cust.transactionCount} orders"
                  else "${cust.phone} • ${cust.transactionCount} transaksi",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              HorizontalDivider()
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(tr("Tutup", "Close")) }
    }
  )
}
