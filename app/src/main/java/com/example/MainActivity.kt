package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BookishViewModel

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.vectorResource
import com.example.R

class MainActivity : ComponentActivity() {
    private val viewModel: BookishViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create notification channel safely
        try {
            com.example.receiver.ReminderScheduler.createNotificationChannel(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Request POST_NOTIFICATIONS permission on Android 13+ safely
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(permission), 101)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            val user by viewModel.userState.collectAsState()
            val themeMode = user?.themeMode ?: "system"
            val themeCombo = user?.themeCombo ?: "default"
            val systemInDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemInDark
            }

            MyApplicationTheme(darkTheme = useDarkTheme, themeCombo = themeCombo) {
                BookishApp(viewModel = viewModel)
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun BookishApp(viewModel: BookishViewModel) {
    val navController = rememberNavController()
    val alertMessage by viewModel.alertMessage.collectAsState()

    alertMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAlertMessage() },
            title = { Text("Skip Notice") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { viewModel.clearAlertMessage() }) {
                    Text("OK")
                }
            }
        )
    }
    
    val navItems = listOf(
        NavigationItem("subscriptions", "Subs", Icons.Default.AutoStories, "nav_subscriptions"),
        NavigationItem("preorders", "Preorders", ImageVector.vectorResource(R.drawable.ic_book_5), "nav_preorders"),
        NavigationItem("home", "Home", Icons.Default.Home, "nav_home"),
        NavigationItem("bookstores", "Stores", Icons.Default.Storefront, "nav_bookstores"),
        NavigationItem("profile", "Profile", Icons.Default.Person, "nav_profile")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { item ->
                    val isHome = item.route == "home"
                    val isSelected = currentRoute == item.route || (isHome && currentRoute == "upcoming_calendar")
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (item.route == "preorders") {
                                viewModel.preorderIsCalendarView.value = false
                            } else if (item.route == "subscriptions") {
                                viewModel.subOverviewIsCalendarView.value = false
                                viewModel.subIsCalendarView.value = false
                                if (viewModel.subSelectedTab.value == 1) {
                                    viewModel.subSelectedTab.value = 0
                                }
                            }
                            if (currentRoute == "upcoming_calendar") {
                                viewModel.resetHomeScreenToTop()
                                navController.popBackStack()
                            }
                            if (isHome) {
                                viewModel.resetHomeScreenToTop()
                                if (currentRoute != "home" && currentRoute != "upcoming_calendar") {
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = false
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            } else if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = if (isHome) Modifier.size(28.dp) else Modifier
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontWeight = if (isHome) androidx.compose.ui.text.font.FontWeight.Bold else null
                            )
                        },
                        colors = if (isHome) {
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        } else {
                            NavigationBarItemDefaults.colors()
                        },
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSubscriptions = {
                        navController.navigate("subscriptions") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToUpcomingCalendar = {
                        navController.navigate("upcoming_calendar")
                    }
                )
            }
            composable("upcoming_calendar") {
                UpcomingCalendarScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("subscriptions") {
                SubscriptionsScreen(viewModel = viewModel)
            }
            composable("preorders") {
                PreordersScreen(viewModel = viewModel)
            }
            composable("bookstores") {
                BookstoresScreen(viewModel = viewModel)
            }
            composable("profile") {
                ProfileScreen(viewModel = viewModel)
            }
        }
    }
}
