package com.example.vibecheck_dev.presentation.components

import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.vibecheck_dev.presentation.navigation.Screen
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@Composable
fun VibeBottomNav(navController: NavController) {
    // 1. UPDATE: Rute Bottom Nav sekarang Home, Studio, dan Vault
    val items = listOf(Screen.Home, Screen.Studio, Screen.Vault)

    NavigationBar(
        containerColor = Color.Black,
        modifier = Modifier.border(2.dp, Color.White) // Border tegas ala retro Y2K
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    // Pakai !! karena kita yakin 3 menu di atas pasti punya icon
                    Icon(
                        imageVector = screen.icon!!,
                        contentDescription = screen.title,
                        tint = if (isSelected) Color.Magenta else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = Y2KTypography.bodyMedium,
                        color = if (isSelected) Color.Magenta else Color.Gray
                    )
                },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.DarkGray // Background kotak saat icon dipilih
                ),
                onClick = {
                    if (!isSelected) {
                        navController.navigate(screen.route) {
                            // Menghindari penumpukan layar di backstack
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}