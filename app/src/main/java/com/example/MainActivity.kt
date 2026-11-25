package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.repository.QaydRepository
import com.example.media.WhatsAppWitnessHelper
import com.example.ui.components.CreateTaskDialog
import com.example.ui.components.WitnessConfigDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChamberScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.PhotoGalleryScreen
import com.example.ui.screens.ProtocolScreen
import com.example.ui.theme.BurnishedAmber
import com.example.ui.theme.CrimsonBreach
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.QaydTheme
import com.example.ui.viewmodel.QaydViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WhatsAppDispatchEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_CHAMBER = "chamber"
private const val ROUTE_GALLERY = "gallery"
private const val ROUTE_LEDGER = "ledger"
private const val ROUTE_PROTOCOL = "protocol"

private data class QaydTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val QaydTabs = listOf(
    QaydTab(ROUTE_CHAMBER, R.string.nav_chamber, Icons.Filled.Lock, Icons.Outlined.Lock),
    QaydTab(ROUTE_GALLERY, R.string.nav_gallery, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    QaydTab(ROUTE_LEDGER, R.string.nav_ledger, Icons.Filled.HistoryEdu, Icons.Outlined.HistoryEdu),
    QaydTab(ROUTE_PROTOCOL, R.string.nav_protocol, Icons.Filled.Shield, Icons.Outlined.Shield),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ObsidianVoid.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(ObsidianSurface.toArgb()),
        )
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                QaydTheme {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedQaydShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: "مُلتزم",
                userEmail = user.email ?: "",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedQaydShell(
    currentUserId: String,
    displayName: String,
    userEmail: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: QaydViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                QaydViewModel(
                    repository = QaydRepository(db),
                    currentUserId = currentUserId,
                    displayName = displayName,
                )
            }
        },
    )

    val chamberState by viewModel.chamberState.collectAsStateWithLifecycle()
    val ledgerState by viewModel.ledgerState.collectAsStateWithLifecycle()
    val photoEntriesState by viewModel.photoEntriesState.collectAsStateWithLifecycle()
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val whatsAppEvent by viewModel.whatsAppEvent.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(whatsAppEvent) {
        val event = whatsAppEvent ?: return@LaunchedEffect
        val result = when (event) {
            is WhatsAppDispatchEvent.ProofPhoto -> WhatsAppWitnessHelper.sendProofPhotoToWitness(
                context = context,
                witnessPhone = event.witnessPhone,
                witnessName = event.witnessName,
                taskTitle = event.taskTitle,
                proofNote = event.proofNote,
                photoPath = event.photoPath,
            )
            is WhatsAppDispatchEvent.PunishmentAlert -> WhatsAppWitnessHelper.sendPunishmentAlertToWitness(
                context = context,
                witnessPhone = event.witnessPhone,
                witnessName = event.witnessName,
                taskTitle = event.taskTitle,
                targetTimeLabel = event.targetTimeLabel,
            )
        }
        viewModel.consumeWhatsAppEvent()
        snackbarHostState.showSnackbar(result.statusMessage)
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CHAMBER

    var showCreateDialog by remember { mutableStateOf(false) }
    var showWitnessDialog by remember { mutableStateOf(false) }

    val snapshot = (chamberState as? UiState.Success)?.data
    val activeOverdue = snapshot?.activeTask?.let {
        it.deadlineEpochMs > 0L && nowMillis > it.deadlineEpochMs
    } == true
    val isRestricted = snapshot?.profile?.restrictedMode == true || activeOverdue

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = ObsidianVoid,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            floatingActionButton = {
                if (currentRoute == ROUTE_CHAMBER) {
                    FloatingActionButton(
                        onClick = {
                            if (isRestricted) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "ممنوع إضافة مهام! صوّر المهمة الحالية أو نفّذ كفّارة لرفع القيود أولاً."
                                    )
                                }
                            } else {
                                showCreateDialog = true
                            }
                        },
                        containerColor = if (isRestricted) CrimsonBreach else BurnishedAmber,
                        contentColor = if (isRestricted) MaterialTheme.colorScheme.onError else ObsidianVoid,
                        modifier = Modifier.testTag("fab_add_task"),
                    ) {
                        Icon(
                            imageVector = if (isRestricted) Icons.Filled.Block else Icons.Filled.Add,
                            contentDescription = if (isRestricted) "إضافة المهام مجمّدة بسبب القيود" else "إضافة قيد مصوّر",
                        )
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                    ) {
                        QaydTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = tab.labelRes),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BurnishedAmber,
                                    selectedTextColor = BurnishedAmber,
                                    indicatorColor = ObsidianElevated,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        QaydTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes),
                                    )
                                },
                                label = {
                                    Text(text = stringResource(id = tab.labelRes))
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BurnishedAmber,
                                    selectedTextColor = BurnishedAmber,
                                    indicatorColor = ObsidianElevated,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_CHAMBER,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(ROUTE_CHAMBER) {
                        ChamberScreen(
                            state = chamberState,
                            nowMillis = nowMillis,
                            isMutating = isMutating,
                            onOpenCreateTask = { showCreateDialog = true },
                            onDeleteDraft = viewModel::deleteDraft,
                            onMoveDraft = viewModel::moveDraft,
                            onSealContract = viewModel::sealContract,
                            onCompleteActiveTaskWithPhoto = viewModel::completeActiveTaskWithPhoto,
                            onBreachActiveTask = viewModel::breachActiveTask,
                            onSpawnRecoverySprint = viewModel::spawnRecoverySprint,
                            onOpenWitnessConfig = { showWitnessDialog = true },
                            onNotifyWitnessPunishment = viewModel::notifyWitnessPunishment,
                        )
                    }
                    composable(ROUTE_GALLERY) {
                        PhotoGalleryScreen(state = photoEntriesState)
                    }
                    composable(ROUTE_LEDGER) {
                        LedgerScreen(state = ledgerState)
                    }
                    composable(ROUTE_PROTOCOL) {
                        ProtocolScreen(
                            chamberState = chamberState,
                            userEmail = userEmail,
                            onOpenWitnessConfig = { showWitnessDialog = true },
                            onSignOut = {
                                signOut(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, duration, targetTimeLabel ->
                showCreateDialog = false
                viewModel.addTask(
                    title = title,
                    description = desc,
                    durationMinutes = duration,
                    targetTimeLabel = targetTimeLabel,
                )
            },
        )
    }

    if (showWitnessDialog) {
        WitnessConfigDialog(
            initialName = snapshot?.profile?.witnessName,
            initialPhone = snapshot?.profile?.witnessPhone,
            initialAutoWhatsApp = snapshot?.profile?.autoWhatsApp ?: true,
            onDismiss = { showWitnessDialog = false },
            onSave = { name, phone, auto ->
                showWitnessDialog = false
                viewModel.saveWhatsAppWitness(
                    witnessName = name,
                    witnessPhone = phone,
                    autoWhatsApp = auto,
                )
            },
        )
    }
}
