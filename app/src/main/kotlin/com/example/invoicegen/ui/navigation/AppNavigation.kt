package com.example.invoicegen.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.invoicegen.ui.history.InvoiceHistoryScreen
import com.example.invoicegen.ui.input.InvoiceInputScreen
import com.example.invoicegen.ui.settings.BusinessInfoScreen

sealed class Screen(val route: String, val label: String) {
    object History : Screen("history", "History")
    object NewInvoice : Screen("input", "New Invoice")
    object Settings : Screen("settings", "Settings")
    object EditInvoice : Screen("input/{invoiceId}", "Edit Invoice") {
        fun createRoute(id: Long) = "input/$id"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(Screen.History, Screen.NewInvoice, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == screen.route || (screen == Screen.NewInvoice && it.route == Screen.NewInvoice.route)
                    } == true
                    NavigationBarItem(
                        icon = {
                            when (screen) {
                                Screen.History -> Icon(Icons.Default.History, contentDescription = screen.label)
                                Screen.NewInvoice -> Icon(Icons.Default.Add, contentDescription = screen.label)
                                Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = screen.label)
                                else -> {}
                            }
                        },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.History.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.History.route) {
                InvoiceHistoryScreen(
                    onEditInvoice = { id ->
                        navController.navigate(Screen.EditInvoice.createRoute(id))
                    }
                )
            }
            composable(Screen.NewInvoice.route) {
                InvoiceInputScreen(onSaved = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.History.route) { inclusive = false }
                    }
                })
            }
            composable(
                route = Screen.EditInvoice.route,
                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
            ) {
                InvoiceInputScreen(onSaved = {
                    navController.popBackStack()
                })
            }
            composable(Screen.Settings.route) {
                BusinessInfoScreen()
            }
        }
    }
}
