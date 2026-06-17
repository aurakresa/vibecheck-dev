package com.example.vibecheck_dev.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.vibecheck_dev.presentation.navigation.Screen
import com.example.vibecheck_dev.ui.theme.Y2KTypography

// Engine Penggambar Ikon Piksel Native
@Composable
fun PixelArtIcon(matrix: List<String>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val rows = matrix.size
        val cols = matrix.maxOf { it.length }
        val pixelWidth = size.width / cols
        val pixelHeight = size.height / rows

        for (r in 0 until rows) {
            for (c in 0 until matrix[r].length) {
                if (matrix[r][c] == 'X') {
                    drawRect(
                        color = color,
                        topLeft = Offset(c * pixelWidth, r * pixelHeight),
                        size = Size(pixelWidth, pixelHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun VibeBottomNav(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.Studio,
        Screen.Purikura
    )

    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onBgColor = MaterialTheme.colorScheme.onBackground

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    // Kontainer Bottom Nav Ala Retro Terminal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(bgColor)
            .border(2.dp, secondaryColor, RectangleShape),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            // Logika Warna Y2K: Kalau dipilih jadi Magenta, kalau nggak Cyan/Abu
            val iconColor = if (isSelected) primaryColor else onBgColor.copy(alpha = 0.5f)
            val textColor = if (isSelected) primaryColor else onBgColor.copy(alpha = 0.5f)
            val itemBgColor = if (isSelected) onBgColor.copy(alpha = 0.1f) else Color.Transparent
            val borderModifier = if (isSelected) Modifier.border(1.dp, primaryColor, RectangleShape) else Modifier

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(itemBgColor)
                    .then(borderModifier)
                    .clickable {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // FIX: Gunakan safe call (?.let) untuk memastikan data tidak null sebelum digambar
                screen.pixelMatrix?.let { matrix ->
                    PixelArtIcon(
                        matrix = matrix,
                        color = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Teks Menu
                Text(
                    text = screen.title,
                    color = textColor,
                    style = Y2KTypography.bodySmall.copy(fontSize = 8.sp)
                )
            }
        }
    }
}