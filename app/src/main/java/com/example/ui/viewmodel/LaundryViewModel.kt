package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppLanguage
import com.example.data.local.ReceiptPaperSize
import com.example.data.local.ThemeMode
import com.example.data.local.ThemePreferences
import com.example.data.model.ActiveShopSession
import com.example.data.model.ActivityLog
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.LaundryOrder
import com.example.data.model.LaundryService
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.ReportPeriod
import com.example.data.model.ReportSummary
import com.example.data.model.ShopProfile
import com.example.data.model.StaffInvite
import com.example.data.model.UserRole
import com.example.data.repository.LaundryRepository
import com.example.data.repository.NewOrderRequest
import com.example.util.Formatters
import com.example.util.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class LaundryViewModel(
  private val repository: LaundryRepository,
  private val themePreferences: ThemePreferences,
  networkMonitor: NetworkMonitor,
  val session: ActiveShopSession
) : ViewModel() {

  private val shopId: String = session.shopId
  private val isOwner: Boolean = session.role == UserRole.OWNER
  private val isEn: Boolean get() = appLanguage.value == AppLanguage.EN

  private fun <T> observeSafe(
    idLabel: String,
    enLabel: String,
    flow: kotlinx.coroutines.flow.Flow<T>,
    initial: T
  ): StateFlow<T> {
    return flow
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $enLabel", rootError)
        _actionError.value = rootError.message
          ?: if (isEn) "Failed to load $enLabel" else "Gagal memuat $idLabel"
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = initial
      )
  }

  val isOnline: StateFlow<Boolean> = networkMonitor.observeIsOnline()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

  val themeMode: StateFlow<ThemeMode> = themePreferences.themeModeFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemeMode.LIGHT)

  val appLanguage: StateFlow<AppLanguage> = themePreferences.languageFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.ID)

  val savedPrinterName: StateFlow<String> = themePreferences.savedPrinterFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

  val paperSize: StateFlow<ReceiptPaperSize> = themePreferences.paperSizeFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ReceiptPaperSize.MM_58)

  val dueSoundEnabled: StateFlow<Boolean> = themePreferences.dueSoundEnabledFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

  val shopProfile: StateFlow<ShopProfile> = observeSafe(
    "profil usaha",
    "business profile",
    repository.observeShop(shopId).map { it ?: session.shopProfile },
    session.shopProfile
  )

  val services: StateFlow<List<LaundryService>> = observeSafe(
    "layanan",
    "services",
    repository.observeServices(shopId),
    emptyList()
  )

  val customers: StateFlow<List<Customer>> = observeSafe(
    "pelanggan",
    "customers",
    repository.observeCustomers(shopId),
    emptyList()
  )

  val orders: StateFlow<List<LaundryOrder>> = observeSafe(
    "pesanan",
    "orders",
    repository.observeOrders(shopId),
    emptyList()
  )

  val expenses: StateFlow<List<Expense>> = if (isOwner) {
    observeSafe("pengeluaran", "expenses", repository.observeExpenses(shopId), emptyList())
  } else {
    MutableStateFlow(emptyList())
  }

  val expenseCategories: StateFlow<List<ExpenseCategory>> = if (isOwner) {
    observeSafe("kategori pengeluaran", "expense categories", repository.observeExpenseCategories(shopId), emptyList())
  } else {
    MutableStateFlow(emptyList())
  }

  val staffInvites: StateFlow<List<StaffInvite>> = if (isOwner) {
    observeSafe("karyawan", "staff", repository.observeStaffInvites(session.userUid), emptyList())
  } else {
    MutableStateFlow(emptyList())
  }

  val activityLogs: StateFlow<List<ActivityLog>> = if (isOwner) {
    observeSafe("aktivitas", "activity logs", repository.observeActivityLogs(shopId), emptyList())
  } else {
    MutableStateFlow(emptyList())
  }

  private val _dashboardPeriod = MutableStateFlow(ReportPeriod.DAILY)
  val dashboardPeriod: StateFlow<ReportPeriod> = _dashboardPeriod.asStateFlow()

  val dashboardSummary: StateFlow<ReportSummary> = combine(
    orders,
    expenses,
    _dashboardPeriod
  ) { orderList, expenseList, period ->
    val now = System.currentTimeMillis()
    val start = Formatters.startOfPeriodMillis(period, now)
    val end = Formatters.endOfDayMillis(now)
    Formatters.computeReportSummary(orderList, expenseList, start, end)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ReportSummary())

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _actionError = MutableStateFlow<String?>(null)
  val actionError: StateFlow<String?> = _actionError.asStateFlow()

  // Pre-selected customer when launching "Transaksi Baru" from Customers screen
  private val _preselectedCustomer = MutableStateFlow<Customer?>(null)
  val preselectedCustomer: StateFlow<Customer?> = _preselectedCustomer.asStateFlow()

  fun setDashboardPeriod(period: ReportPeriod) {
    _dashboardPeriod.value = period
  }

  fun setPreselectedCustomer(customer: Customer?) {
    _preselectedCustomer.value = customer
  }

  fun clearMessages() {
    _actionMessage.value = null
    _actionError.value = null
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      themePreferences.setThemeMode(mode)
    }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch {
      themePreferences.setLanguage(language)
      _actionMessage.value = if (language == AppLanguage.EN) {
        "Language switched to English"
      } else {
        "Bahasa diubah ke Indonesia"
      }
    }
  }

  fun setSavedPrinter(name: String) {
    viewModelScope.launch {
      themePreferences.setSavedPrinter(name)
      _actionMessage.value = if (isEn) "Printer settings saved." else "Pengaturan printer disimpan."
    }
  }

  fun setPaperSize(size: ReceiptPaperSize) {
    viewModelScope.launch {
      themePreferences.setPaperSize(size)
      _actionMessage.value = if (isEn) {
        "Receipt paper size (${size.localizedLabel(true)}) saved."
      } else {
        "Ukuran kertas struk (${size.label}) disimpan."
      }
    }
  }

  fun setDueSoundEnabled(enabled: Boolean) {
    viewModelScope.launch {
      themePreferences.setDueSoundEnabled(enabled)
      _actionMessage.value = if (enabled) {
        if (isEn) "On-time laundry pickup alarm sound enabled."
        else "Notifikasi suara cucian tepat waktu diaktifkan."
      } else {
        if (isEn) "On-time laundry pickup alarm sound disabled."
        else "Notifikasi suara cucian tepat waktu dinonaktifkan."
      }
    }
  }

  fun createOrder(
    request: NewOrderRequest,
    onCreated: (LaundryOrder) -> Unit
  ) {
    if (_isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.createOrder(
        shopId = shopId,
        notaPrefix = shopProfile.value.notaPrefix,
        actorName = session.userName,
        actorRole = session.role,
        request = request
      )
      _isBusy.value = false
      res.onSuccess { created ->
        _preselectedCustomer.value = null
        _actionMessage.value = if (isEn) {
          "Transaction ${created.notaNumber} saved successfully."
        } else {
          "Transaksi ${created.notaNumber} berhasil disimpan."
        }
        onCreated(created)
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to save transaction." else "Gagal menyimpan transaksi."
      }
    }
  }

  fun updateOrderStatus(
    order: LaundryOrder,
    newStatus: OrderStatus,
    note: String = "",
    onSuccess: () -> Unit = {}
  ) {
    if (_isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateOrderStatus(
        shopId = shopId,
        order = order,
        newStatus = newStatus,
        actorName = session.userName,
        actorRole = session.role,
        note = note
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) {
          "Status for ${order.notaNumber} updated to ${newStatus.localizedLabel(true)}."
        } else {
          "Status ${order.notaNumber} diperbarui ke ${newStatus.label}."
        }
        onSuccess()
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to update status." else "Gagal memperbarui status."
      }
    }
  }

  fun cancelOrder(order: LaundryOrder, reason: String, onSuccess: () -> Unit = {}) {
    if (_isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.cancelOrder(
        shopId = shopId,
        order = order,
        reason = reason,
        actorName = session.userName,
        actorRole = session.role
      )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) {
          "Order ${order.notaNumber} cancelled."
        } else {
          "Pesanan ${order.notaNumber} dibatalkan."
        }
        onSuccess()
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to cancel order." else "Gagal membatalkan pesanan."
      }
    }
  }

  fun recordPayment(
    orderId: String,
    paymentRequestId: String,
    receivedAmount: Long,
    method: PaymentMethod,
    isDpPayment: Boolean,
    onSuccess: (PaymentRecord) -> Unit = {}
  ) {
    if (_isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.recordPayment(
        shopId = shopId,
        orderId = orderId,
        paymentRequestId = paymentRequestId,
        receivedAmount = receivedAmount,
        method = method,
        isDpPayment = isDpPayment,
        actorName = session.userName,
        actorRole = session.role
      )
      _isBusy.value = false
      res.onSuccess { rec ->
        val changeInfo = if (rec.changeAmount > 0L) {
          if (isEn) " (Change: ${Formatters.formatRupiah(rec.changeAmount)})"
          else " (Kembalian: ${Formatters.formatRupiah(rec.changeAmount)})"
        } else ""
        _actionMessage.value = if (isEn) {
          "Payment of ${Formatters.formatRupiah(rec.amount)} recorded$changeInfo."
        } else {
          "Pembayaran ${Formatters.formatRupiah(rec.amount)} dicatat$changeInfo."
        }
        onSuccess(rec)
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to record payment." else "Gagal mencatat pembayaran."
      }
    }
  }

  fun logWhatsAppOpened(order: LaundryOrder, normalizedPhone: String) {
    viewModelScope.launch {
      val res = repository.logWhatsAppOpened(
        shopId = shopId,
        order = order,
        normalizedPhone = normalizedPhone,
        actorName = session.userName,
        actorRole = session.role
      )
      res.onFailure { err ->
        Log.w(TAG, "Failed to log WhatsApp open", err)
      }
    }
  }

  fun saveService(service: LaundryService, onSuccess: () -> Unit = {}) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.saveService(shopId, service, session.userName)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) {
          "Service '${service.name}' saved."
        } else {
          "Layanan '${service.name}' berhasil disimpan."
        }
        onSuccess()
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to save service." else "Gagal menyimpan layanan."
      }
    }
  }

  fun deleteService(service: LaundryService) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.deleteService(shopId, service.id)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) {
          "Service '${service.name}' deleted."
        } else {
          "Layanan '${service.name}' dihapus."
        }
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to delete service." else "Gagal menghapus layanan."
      }
    }
  }

  fun saveCustomer(customer: Customer, onSuccess: () -> Unit = {}) {
    if (_isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.saveCustomer(shopId, customer)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) {
          "Customer '${customer.name}' saved."
        } else {
          "Data pelanggan '${customer.name}' disimpan."
        }
        onSuccess()
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to save customer." else "Gagal menyimpan data pelanggan."
      }
    }
  }

  fun saveExpense(expense: Expense, onSuccess: () -> Unit = {}) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.saveExpense(shopId, expense, session.userName)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) "Expense recorded." else "Pengeluaran berhasil dicatat."
        onSuccess()
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to record expense." else "Gagal mencatat pengeluaran."
      }
    }
  }

  fun deleteExpense(expenseId: String) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.deleteExpense(shopId, expenseId)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) "Expense deleted." else "Pengeluaran dihapus."
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to delete expense." else "Gagal menghapus pengeluaran."
      }
    }
  }

  fun addExpenseCategory(name: String, onSuccess: () -> Unit = {}) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.addExpenseCategory(shopId, name)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) "Category '$name' added." else "Kategori '$name' ditambahkan."
        onSuccess()
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to add category." else "Gagal menambah kategori."
      }
    }
  }

  fun saveStaffInvite(invite: StaffInvite, onSuccess: () -> Unit = {}) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.saveStaffInvite(shopId, invite, session.userName)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) {
          "Account '${invite.name}' saved."
        } else {
          "Akun kasir '${invite.name}' berhasil disimpan."
        }
        onSuccess()
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to save account." else "Gagal menyimpan akun kasir."
      }
    }
  }

  fun deleteStaffInvite(invite: StaffInvite) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.deleteStaffInvite(shopId, invite)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) {
          "Account '${invite.name}' deleted."
        } else {
          "Akun kasir '${invite.name}' dihapus."
        }
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to delete account." else "Gagal menghapus akun kasir."
      }
    }
  }

  fun updateShopProfile(profile: ShopProfile, onSuccess: () -> Unit = {}) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateShopProfile(shopId, profile)
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = if (isEn) {
          "Business settings updated."
        } else {
          "Pengaturan usaha berhasil diperbarui."
        }
        onSuccess()
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to save settings." else "Gagal menyimpan pengaturan."
      }
    }
  }

  fun exportBackupJson(onResult: (String) -> Unit) {
    if (!isOwner || _isBusy.value) return
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.exportShopBackupJson(shopId)
      _isBusy.value = false
      res.onSuccess { json ->
        onResult(json)
      }.onFailure { err ->
        _actionError.value = err.message ?: if (isEn) "Failed to export backup." else "Gagal mengekspor backup data."
      }
    }
  }

  fun notifyMessage(message: String) {
    _actionMessage.value = message
  }

  fun notifyError(error: String) {
    _actionError.value = error
  }

  private companion object {
    const val TAG = "LaundryViewModel"
  }
}
