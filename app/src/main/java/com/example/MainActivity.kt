package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StaffPinDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CustomersKhataScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.OnlineStoreScreen
import com.example.ui.screens.OrdersBillingScreen
import com.example.ui.screens.PosRegisterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ErpScreen
import com.example.ui.viewmodel.ErpViewModel
import com.example.ui.viewmodel.ErpViewModelFactory
import kotlinx.coroutines.launch

data class DrawerNavItem(
    val screen: ErpScreen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private val viewModel: ErpViewModel by viewModels {
        ErpViewModelFactory((application as NkkErpApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NkkErpApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NkkErpApp(viewModel: ErpViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val allStaff by viewModel.allStaff.collectAsState()
    val staffAuthError by viewModel.staffAuthError.collectAsState()
    val heldOrders by viewModel.heldOrders.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showStaffDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle Android system back press
    BackHandler(enabled = currentScreen != ErpScreen.POS_REGISTER) {
        viewModel.navigateTo(ErpScreen.POS_REGISTER)
    }

    val drawerItems = listOf(
        DrawerNavItem(ErpScreen.POS_REGISTER, "POS Register (Fast Page)", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "nav_pos"),
        DrawerNavItem(ErpScreen.INVENTORY, "Inventory Management", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_inventory"),
        DrawerNavItem(ErpScreen.ORDERS_BILLING, "Orders & Billing History", Icons.Filled.Receipt, Icons.Outlined.Receipt, "nav_orders"),
        DrawerNavItem(ErpScreen.CUSTOMERS_KHATA, "Customers & Khata Ledger", Icons.Filled.People, Icons.Outlined.People, "nav_khata"),
        DrawerNavItem(ErpScreen.ANALYTICS, "Dashboard Sale Reports (3 Reports)", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_analytics"),
        DrawerNavItem(ErpScreen.ONLINE_STORE, "Customer Online Store", Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_online"),
        DrawerNavItem(ErpScreen.SETTINGS, "System Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.testTag("slide_navigation_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = settings.businessName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Operator: ${settings.currentStaffName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                drawerItems.forEach { item ->
                    val isSelected = currentScreen == item.screen
                    val badgeCount = when (item.screen) {
                        ErpScreen.POS_REGISTER -> heldOrders.size
                        ErpScreen.INVENTORY -> lowStockProducts.size
                        else -> 0
                    }

                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (badgeCount > 0) {
                                    Badge { Text("$badgeCount") }
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            viewModel.navigateTo(item.screen)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag(item.testTag)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                ErpScreen.POS_REGISTER -> "NKK POS — Fast Register"
                                ErpScreen.INVENTORY -> "Inventory & Stock"
                                ErpScreen.ORDERS_BILLING -> "Orders & Billing"
                                ErpScreen.CUSTOMERS_KHATA -> "Customers & Khata"
                                ErpScreen.ANALYTICS -> "Dashboard Sale Reports"
                                ErpScreen.ONLINE_STORE -> "Online Storefront"
                                ErpScreen.SETTINGS -> "ERP Settings"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.testTag("hamburger_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Slide Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    ErpScreen.POS_REGISTER -> PosRegisterScreen(
                        viewModel = viewModel,
                        onOpenStaffDialog = { showStaffDialog = true }
                    )
                    ErpScreen.INVENTORY -> InventoryScreen(
                        viewModel = viewModel
                    )
                    ErpScreen.ORDERS_BILLING -> OrdersBillingScreen(
                        viewModel = viewModel
                    )
                    ErpScreen.CUSTOMERS_KHATA -> CustomersKhataScreen(
                        viewModel = viewModel
                    )
                    ErpScreen.ANALYTICS -> AnalyticsScreen(
                        viewModel = viewModel
                    )
                    ErpScreen.ONLINE_STORE -> OnlineStoreScreen(
                        viewModel = viewModel
                    )
                    ErpScreen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onOpenStaffDialog = { showStaffDialog = true }
                    )
                }
            }
        }
    }

    if (showStaffDialog) {
        StaffPinDialog(
            staffList = allStaff,
            errorMessage = staffAuthError,
            onPinEntered = { pin ->
                viewModel.switchStaffWithPin(pin) {
                    showStaffDialog = false
                }
            },
            onClearError = { viewModel.clearStaffAuthError() },
            onDismiss = {
                viewModel.clearStaffAuthError()
                showStaffDialog = false
            }
        )
    }
}
