package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppLanguage
import com.example.data.local.ThemeMode
import com.example.data.local.ThemePreferences
import com.example.data.model.ActiveShopSession
import com.example.data.model.LaundryOrder
import com.example.data.model.UserRole
import com.example.data.repository.AuthSessionRepository
import com.example.data.repository.LaundryRepository
import com.example.data.repository.SessionResolution
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.ChooseRoleScreen
import com.example.ui.auth.PostLoginSetupScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.components.BusinessAvatarBadge
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.screens.CashierScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StaffManagementScreen
import com.example.ui.theme.AdaLaundryTheme
import com.example.ui.theme.BrandColorOption
import com.example.ui.viewmodel.LaundryViewModel
import com.example.util.Formatters
import com.example.util.LaundryReminderHelper
import com.example.util.LocalAppLanguage
import com.example.util.NetworkMonitor
import com.example.util.tr
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class AppRoute(
  val route: String,
  val title: String,
  val shortLabel: String,
  val titleEn: String,
  val shortLabelEn: String,
  val icon: ImageVector,
  val ownerOnly: Boolean = false,
  val requiresReportsPerm: Boolean = false
) {
  DASHBOARD("dashboard", "Dashboard", "Beranda", "Dashboard", "Home", Icons.Filled.Dashboard),
  CASHIER("cashier", "Kasir & Transaksi Baru", "Kasir", "Cashier & New Order", "Cashier", Icons.Filled.AddShoppingCart),
  ORDERS("orders", "Manajemen Pesanan", "Pesanan", "Order Management", "Orders", Icons.Filled.ReceiptLong),
  CUSTOMERS("customers", "Data Pelanggan", "Pelanggan", "Customers", "Customers", Icons.Filled.People),
  SERVICES("services", "Layanan & Harga", "Layanan", "Services & Pricing", "Services", Icons.Filled.CleaningServices, ownerOnly = true),
  EXPENSES("expenses", "Pengeluaran Usaha", "Pengeluaran", "Business Expenses", "Expenses", Icons.Filled.MoneyOff, ownerOnly = true),
  REPORTS("reports", "Laporan & Keuangan", "Laporan", "Reports & Finance", "Reports", Icons.Filled.Assessment, requiresReportsPerm = true),
  STAFF("staff", "Manajemen Karyawan", "Karyawan", "Staff Management", "Staff", Icons.Filled.Badge, ownerOnly = true),
  SETTINGS("settings", "Pengaturan Outlet", "Pengaturan", "Outlet Settings", "Settings", Icons.Filled.Settings);

  fun localizedTitle(language: AppLanguage): String = if (language == AppLanguage.EN) titleEn else title
  fun localizedShortLabel(language: AppLanguage): String = if (language == AppLanguage.EN) shortLabelEn else shortLabel
}

private fun FirebaseAuth.authStateFlow() = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AdaLaundryRoot()
    }
  }
}

@Composable
private fun AdaLaundryRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val themePrefs = remember(context) { ThemePreferences(context.applicationContext) }
  val themeMode by themePrefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
  val appLanguage by themePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ID)
  val isEn = appLanguage == AppLanguage.EN
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val sessionRepo = remember(context) { AuthSessionRepository(context.applicationContext) }

  val currentUser by remember { Firebase.auth.authStateFlow() }
    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  var showSplash by remember { mutableStateOf(true) }
  var autoSignInAttempted by remember { mutableStateOf(false) }
  var sessionRefreshTrigger by remember { mutableIntStateOf(0) }
  var isResolvingSession by remember { mutableStateOf(false) }
  var resolutionError by remember { mutableStateOf<String?>(null) }
  var resolution by remember { mutableStateOf<SessionResolution?>(null) }
  var selectedSession by remember { mutableStateOf<ActiveShopSession?>(null) }
  var isCreatingShop by remember { mutableStateOf(false) }
  var shopSetupError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    delay(1500L)
    showSplash = false
  }

  LaunchedEffect(Unit) {
    if (!autoSignInAttempted) {
      autoSignInAttempted = true
      if (Firebase.auth.currentUser == null) {
        attemptAutoSignIn(
          context = context,
          credentialManager = credentialManager,
          onAuthSuccess = { sessionRefreshTrigger++ },
          onUnauthenticated = {},
          scope = scope
        )
      }
    }
  }

  LaunchedEffect(currentUser?.uid, sessionRefreshTrigger) {
    val user: FirebaseUser? = currentUser
    if (user == null) {
      selectedSession = null
      resolution = null
      resolutionError = null
      isResolvingSession = false
      return@LaunchedEffect
    }
    isResolvingSession = true
    resolutionError = null
    selectedSession = null
    val res = sessionRepo.resolveCurrentSession()
    isResolvingSession = false
    res.onSuccess { resolved ->
      resolution = resolved
      if (resolved is SessionResolution.Ready) {
        selectedSession = resolved.session
      }
    }.onFailure { err ->
      resolutionError = err.message ?: if (isEn) "Failed to load user session." else "Gagal memuat sesi pengguna."
    }
  }

  CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
  AdaLaundryTheme(themeMode = themeMode) {
    when {
      showSplash -> {
        AdaLaundrySplashScreen()
      }
      currentUser == null -> {
        AuthScreen(onAuthSuccess = { sessionRefreshTrigger++ })
      }
      isResolvingSession -> {
        LoadingSessionScreen()
      }
      resolutionError != null -> {
        SessionErrorScreen(
          message = resolutionError!!,
          onRetry = { sessionRefreshTrigger++ },
          onSignOut = { signOut(credentialManager, scope) }
        )
      }
      selectedSession != null -> {
        val activeSession = selectedSession!!
        AdaLaundryAppShell(
          session = activeSession,
          themePreferences = themePrefs,
          onSignOut = { signOut(credentialManager, scope) }
        )
      }
      resolution is SessionResolution.ChooseRole -> {
        val choose = resolution as SessionResolution.ChooseRole
        ChooseRoleScreen(
          ownerSession = choose.ownerSession,
          cashierSession = choose.cashierSession,
          onSelectSession = { chosen -> selectedSession = chosen },
          onSignOut = { signOut(credentialManager, scope) }
        )
      }
      resolution is SessionResolution.NeedsShopSetup -> {
        val setup = resolution as SessionResolution.NeedsShopSetup
        PostLoginSetupScreen(
          setupInfo = setup,
          isSubmitting = isCreatingShop,
          errorMessage = shopSetupError,
          onCreateOwnerShop = { name, addr, phone, hours, prefix ->
            scope.launch {
              isCreatingShop = true
              shopSetupError = null
              val res = sessionRepo.createOwnerShopWithCatalog(name, addr, phone, hours, prefix)
              isCreatingShop = false
              res.onSuccess { ownerSession ->
                selectedSession = ownerSession
              }.onFailure { err ->
                shopSetupError = err.message ?: if (isEn) "Failed to create outlet." else "Gagal membuat outlet."
              }
            }
          },
          onRetryCheckInvite = { sessionRefreshTrigger++ },
          onSignOut = { signOut(credentialManager, scope) }
        )
      }
      else -> {
        LoadingSessionScreen()
      }
    }
  }
  }
}

@Composable
private fun AdaLaundrySplashScreen() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF0081FE),
            Color(0xFF5FB0FF),
            Color(0xFFEAF4FF),
            Color.White
          )
        )
      )
      .safeDrawingPadding()
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.padding(horizontal = 32.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color.White,
        shadowElevation = 10.dp
      ) {
        Image(
          painter = painterResource(R.drawable.img_brand_logo),
          contentDescription = "ADA LAUNDRY Logo",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(124.dp)
            .padding(6.dp)
            .clip(RoundedCornerShape(24.dp))
        )
      }
      Text(
        text = "ADA LAUNDRY",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF0A2E75)
      )
      Text(
        text = "by Rafline Dev",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1E40AF)
      )
      Spacer(Modifier.height(8.dp))
      CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        strokeWidth = 2.8.dp,
        color = Color(0xFF0081FE)
      )
    }
  }
}

@Composable
private fun LoadingSessionScreen() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .safeDrawingPadding(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      CircularProgressIndicator()
      Text(
        tr("Memuat data outlet ADA LAUNDRY...", "Loading ADA LAUNDRY outlet data..."),
        style = MaterialTheme.typography.bodyMedium
      )
    }
  }
}

@Composable
private fun SessionErrorScreen(
  message: String,
  onRetry: () -> Unit,
  onSignOut: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.widthIn(max = 420.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        tr("Gagal Memuat Outlet", "Failed to Load Outlet"),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
      Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(tr("Coba Lagi", "Try Again"))
      }
      OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(tr("Keluar", "Sign Out"))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdaLaundryAppShell(
  session: ActiveShopSession,
  themePreferences: ThemePreferences,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val viewModelKey = "${session.shopId}_${session.userUid}_${session.role.name}"
  val vm: LaundryViewModel = viewModel(
    key = viewModelKey,
    factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LaundryViewModel(
          repository = LaundryRepository(context.applicationContext),
          themePreferences = themePreferences,
          networkMonitor = NetworkMonitor(context.applicationContext),
          session = session
        ) as T
      }
    }
  )

  val isOnline by vm.isOnline.collectAsStateWithLifecycle()
  val themeMode by vm.themeMode.collectAsStateWithLifecycle()
  val appLanguage by vm.appLanguage.collectAsStateWithLifecycle()
  val savedPrinterName by vm.savedPrinterName.collectAsStateWithLifecycle()
  val paperSize by vm.paperSize.collectAsStateWithLifecycle()
  val dueSoundEnabled by vm.dueSoundEnabled.collectAsStateWithLifecycle()
  val shopProfile by vm.shopProfile.collectAsStateWithLifecycle()
  val services by vm.services.collectAsStateWithLifecycle()
  val customers by vm.customers.collectAsStateWithLifecycle()
  val orders by vm.orders.collectAsStateWithLifecycle()
  val expenses by vm.expenses.collectAsStateWithLifecycle()
  val expenseCategories by vm.expenseCategories.collectAsStateWithLifecycle()
  val staffInvites by vm.staffInvites.collectAsStateWithLifecycle()
  val activityLogs by vm.activityLogs.collectAsStateWithLifecycle()
  val dashboardPeriod by vm.dashboardPeriod.collectAsStateWithLifecycle()
  val dashboardSummary by vm.dashboardSummary.collectAsStateWithLifecycle()
  val isBusy by vm.isBusy.collectAsStateWithLifecycle()
  val actionMessage by vm.actionMessage.collectAsStateWithLifecycle()
  val actionError by vm.actionError.collectAsStateWithLifecycle()
  val preselectedCustomer by vm.preselectedCustomer.collectAsStateWithLifecycle()

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: AppRoute.DASHBOARD.route

  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedOrderForDetail by remember { mutableStateOf<LaundryOrder?>(null) }
  var receiptOrderToShow by remember { mutableStateOf<LaundryOrder?>(null) }
  var dueOrdersAlert by remember { mutableStateOf<List<LaundryOrder>>(emptyList()) }
  var notificationPermRequested by remember { mutableStateOf(false) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { }

  LaunchedEffect(appLanguage) {
    LaundryReminderHelper.ensureNotificationChannel(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      !notificationPermRequested &&
      !LaundryReminderHelper.hasNotificationPermission(context)
    ) {
      notificationPermRequested = true
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  LaunchedEffect(orders, dueSoundEnabled) {
    LaundryReminderHelper.scheduleNextDueAlarm(context, orders, dueSoundEnabled)
    while (true) {
      val newlyDue = LaundryReminderHelper.findNewlyDueOrders(context, orders)
      if (newlyDue.isNotEmpty()) {
        LaundryReminderHelper.notifyDueOrders(context, newlyDue, playSound = dueSoundEnabled)
        dueOrdersAlert = newlyDue
      }
      delay(15_000L)
    }
  }

  LaunchedEffect(actionMessage, actionError) {
    val msg = actionError ?: actionMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      vm.clearMessages()
    }
  }

  val isOwner = session.role == UserRole.OWNER
  val visibleRoutes = remember(session) {
    AppRoute.entries.filter { route ->
      when {
        route.ownerOnly -> isOwner
        route.requiresReportsPerm -> isOwner || session.permissions.canViewReports
        else -> true
      }
    }
  }

  fun navigateTopLevel(route: AppRoute) {
    navController.navigate(route.route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  val activeRouteEnum = AppRoute.entries.firstOrNull { it.route == currentRoute } ?: AppRoute.DASHBOARD

  val bottomNavRoutes = remember(isOwner) {
    listOf(
      AppRoute.DASHBOARD,
      AppRoute.CASHIER,
      AppRoute.ORDERS,
      AppRoute.CUSTOMERS,
      AppRoute.SETTINGS
    )
  }

  val headerColor = BrandColorOption.fromKey(shopProfile.brandColorKey).lightPrimary

  AdaLaundryTheme(
    themeMode = themeMode,
    brandColorKey = shopProfile.brandColorKey
  ) {
    ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        ModalDrawerSheet {
          Column(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(vertical = 16.dp, horizontal = 12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(horizontal = 12.dp)
            ) {
              BusinessAvatarBadge(shop = shopProfile, size = 44.dp)
              Column {
                Text(
                  text = shopProfile.businessName.ifBlank { "ADA LAUNDRY" },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "${session.userName} • ${session.role.localizedLabel(appLanguage == AppLanguage.EN)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            visibleRoutes.forEach { item ->
              NavigationDrawerItem(
                label = { Text(item.localizedTitle(appLanguage)) },
                icon = { Icon(item.icon, contentDescription = null) },
                selected = currentRoute == item.route,
                onClick = {
                  scope.launch { drawerState.close() }
                  navigateTopLevel(item)
                },
                modifier = Modifier.testTag("drawer_${item.route}")
              )
            }
          }
        }
      }
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
            TopAppBar(
              title = {
                Column {
                  Text(
                    text = activeRouteEnum.localizedTitle(appLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = shopProfile.businessName.ifBlank { "ADA LAUNDRY" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1
                  )
                }
              },
              navigationIcon = {
                if (currentRoute == AppRoute.STAFF.route && !useRail) {
                  IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                      Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = tr("Kembali", "Back"),
                      tint = Color.White
                    )
                  }
                } else {
                  IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.testTag("open_drawer_button")
                  ) {
                    Icon(
                      Icons.Filled.Menu,
                      contentDescription = tr("Menu Utama", "Main Menu"),
                      tint = Color.White
                    )
                  }
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = headerColor,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
              )
            )
          },
          bottomBar = {
            if (!useRail) {
              NavigationBar {
                bottomNavRoutes.forEach { item ->
                  val shortLabel = item.localizedShortLabel(appLanguage)
                  NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { navigateTopLevel(item) },
                    icon = { Icon(item.icon, contentDescription = shortLabel) },
                    label = { Text(shortLabel, maxLines = 1) },
                    modifier = Modifier.testTag("bottom_nav_${item.route}")
                  )
                }
              }
            }
          }
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            if (useRail) {
              NavigationRail(
                modifier = Modifier
                  .fillMaxHeight()
                  .verticalScroll(rememberScrollState())
              ) {
                visibleRoutes.forEach { item ->
                  val shortLabel = item.localizedShortLabel(appLanguage)
                  NavigationRailItem(
                    selected = currentRoute == item.route,
                    onClick = { navigateTopLevel(item) },
                    icon = { Icon(item.icon, contentDescription = shortLabel) },
                    label = { Text(shortLabel, maxLines = 1) },
                    modifier = Modifier.testTag("rail_nav_${item.route}")
                  )
                }
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
              contentAlignment = Alignment.TopCenter
            ) {
              NavHost(
                navController = navController,
                startDestination = AppRoute.DASHBOARD.route,
                modifier = Modifier
                  .widthIn(max = 960.dp)
                  .fillMaxSize()
              ) {
                composable(AppRoute.DASHBOARD.route) {
                  DashboardScreen(
                    session = session,
                    isOnline = isOnline,
                    appLanguage = appLanguage,
                    selectedPeriod = dashboardPeriod,
                    summary = dashboardSummary,
                    recentOrders = orders,
                    onSelectPeriod = { vm.setDashboardPeriod(it) },
                    onNewTransactionClick = { navigateTopLevel(AppRoute.CASHIER) },
                    onNavigateOrders = { navigateTopLevel(AppRoute.ORDERS) },
                    onOrderClick = { ord ->
                      selectedOrderForDetail = ord
                      navigateTopLevel(AppRoute.ORDERS)
                    }
                  )
                }

                composable(AppRoute.CASHIER.route) {
                  CashierScreen(
                    session = session,
                    shopProfile = shopProfile,
                    services = services,
                    customers = customers,
                    preselectedCustomer = preselectedCustomer,
                    isBusy = isBusy,
                    onClearPreselectedCustomer = { vm.setPreselectedCustomer(null) },
                    onSaveOrder = { req, onSaved -> vm.createOrder(req, onSaved) },
                    onOrderSavedShowReceipt = { created ->
                      receiptOrderToShow = created
                      navigateTopLevel(AppRoute.ORDERS)
                    }
                  )
                }

                composable(AppRoute.ORDERS.route) {
                  OrdersScreen(
                    session = session,
                    shopProfile = shopProfile,
                    orders = orders,
                    selectedOrderFromOutside = selectedOrderForDetail,
                    isBusy = isBusy,
                    onClearExternalOrder = { selectedOrderForDetail = null },
                    onUpdateStatus = { order, newStatus ->
                      vm.updateOrderStatus(order, newStatus)
                    },
                    onCancelOrder = { order, reason ->
                      vm.cancelOrder(order, reason)
                    },
                    onRecordPayment = { orderId, reqId, amount, method, isDp ->
                      vm.recordPayment(orderId, reqId, amount, method, isDp)
                    },
                    onLogWhatsAppOpened = { order, normalizedPhone ->
                      vm.logWhatsAppOpened(order, normalizedPhone)
                    },
                    onOpenReceipt = { order ->
                      receiptOrderToShow = order
                    }
                  )
                }

                composable(AppRoute.CUSTOMERS.route) {
                  CustomersScreen(
                    customers = customers,
                    orders = orders,
                    isBusy = isBusy,
                    onSaveCustomer = { cust, onSuccess -> vm.saveCustomer(cust, onSuccess) },
                    onCreateOrderForCustomer = { cust ->
                      vm.setPreselectedCustomer(cust)
                      navigateTopLevel(AppRoute.CASHIER)
                    },
                    onOpenOrder = { ord ->
                      selectedOrderForDetail = ord
                      navigateTopLevel(AppRoute.ORDERS)
                    }
                  )
                }

                composable(AppRoute.SERVICES.route) {
                  ServicesScreen(
                    session = session,
                    services = services,
                    isBusy = isBusy,
                    onSaveService = { svc, onSuccess -> vm.saveService(svc, onSuccess) },
                    onDeleteService = { svc -> vm.deleteService(svc) }
                  )
                }

                composable(AppRoute.EXPENSES.route) {
                  ExpensesScreen(
                    session = session,
                    expenses = expenses,
                    categories = expenseCategories,
                    isBusy = isBusy,
                    onSaveExpense = { exp, onSuccess -> vm.saveExpense(exp, onSuccess) },
                    onDeleteExpense = { id -> vm.deleteExpense(id) },
                    onAddCategory = { name, onSuccess -> vm.addExpenseCategory(name, onSuccess) }
                  )
                }

                composable(AppRoute.REPORTS.route) {
                  ReportsScreen(
                    session = session,
                    shopProfile = shopProfile,
                    orders = orders,
                    expenses = expenses,
                    onNotify = { vm.notifyMessage(it) }
                  )
                }

                composable(AppRoute.STAFF.route) {
                  StaffManagementScreen(
                    session = session,
                    staffInvites = staffInvites,
                    orders = orders,
                    activityLogs = activityLogs,
                    isBusy = isBusy,
                    onSaveInvite = { invite, onSuccess -> vm.saveStaffInvite(invite, onSuccess) },
                    onDeleteInvite = { invite -> vm.deleteStaffInvite(invite) }
                  )
                }

                composable(AppRoute.SETTINGS.route) {
                  SettingsScreen(
                    session = session,
                    shopProfile = shopProfile,
                    themeMode = themeMode,
                    appLanguage = appLanguage,
                    savedPrinterName = savedPrinterName,
                    paperSize = paperSize,
                    dueSoundEnabled = dueSoundEnabled,
                    staffInvites = staffInvites,
                    isBusy = isBusy,
                    onSelectThemeMode = { vm.setThemeMode(it) },
                    onSelectLanguage = { vm.setLanguage(it) },
                    onSavePrinter = { vm.setSavedPrinter(it) },
                    onSelectPaperSize = { vm.setPaperSize(it) },
                    onSetDueSoundEnabled = { vm.setDueSoundEnabled(it) },
                    onSaveShopProfile = { vm.updateShopProfile(it) },
                    onSaveStaffInvite = { invite, onSuccess -> vm.saveStaffInvite(invite, onSuccess) },
                    onDeleteStaffInvite = { invite -> vm.deleteStaffInvite(invite) },
                    onOpenStaffManagement = {
                      navController.navigate(AppRoute.STAFF.route) { launchSingleTop = true }
                    },
                    onOpenServices = { navigateTopLevel(AppRoute.SERVICES) },
                    onExportBackupJson = { onResult -> vm.exportBackupJson(onResult) },
                    onNotify = { vm.notifyMessage(it) },
                    onSignOut = onSignOut
                  )
                }
              }
            }
          }
        }
      }
    }

    if (dueOrdersAlert.isNotEmpty()) {
      AlertDialog(
        onDismissRequest = { dueOrdersAlert = emptyList() },
        icon = {
          Icon(
            Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
        },
        title = { Text(tr("Waktu Pengambilan Cucian Tiba", "Laundry Pickup Time Arrived"), fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              tr(
                "Cucian berikut sudah mencapai jadwal hari & jam pengambilan:",
                "The following laundry orders have reached their scheduled pickup date & time:"
              ),
              style = MaterialTheme.typography.bodySmall
            )
            dueOrdersAlert.take(4).forEach { ord ->
              Text(
                "• ${ord.notaNumber} - ${ord.customerName} (${Formatters.formatDateTime(ord.estimatedFinishAtMillis, appLanguage)})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              selectedOrderForDetail = dueOrdersAlert.firstOrNull()
              dueOrdersAlert = emptyList()
              navigateTopLevel(AppRoute.ORDERS)
            },
            modifier = Modifier.testTag("due_alert_view_orders_button")
          ) {
            Text(tr("Lihat Pesanan", "View Orders"))
          }
        },
        dismissButton = {
          TextButton(onClick = { dueOrdersAlert = emptyList() }) {
            Text(tr("Tutup", "Close"))
          }
        }
      )
    }

    if (receiptOrderToShow != null) {
      DigitalReceiptDialog(
        order = receiptOrderToShow!!,
        shop = shopProfile,
        paperSize = paperSize,
        onDismiss = { receiptOrderToShow = null },
        onNotify = { vm.notifyMessage(it) }
      )
    }
  }
}
