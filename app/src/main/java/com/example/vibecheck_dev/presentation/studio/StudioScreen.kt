package com.example.vibecheck_dev.presentation.studio

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.vibecheck_dev.domain.model.Y2KPreset
import com.example.vibecheck_dev.domain.model.defaultPresets
import com.example.vibecheck_dev.domain.util.ColorMatrixUtil
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(StudioEvent.LoadImage(it, context)) }
    }

    // Engine Peracik Warna Compose untuk Live Preview Layar Besar
    val composeColorMatrix = remember(
        uiState.currentBrightness,
        uiState.currentContrast,
        uiState.currentSaturation,
        uiState.currentWarmth
    ) {
        val androidMatrix = ColorMatrixUtil.createAndroidColorMatrix(
            uiState.currentBrightness,
            uiState.currentContrast,
            uiState.currentSaturation,
            uiState.currentWarmth
        )
        androidx.compose.ui.graphics.ColorMatrix(androidMatrix.array)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VIBE_STUDIO.exe", style = Y2KTypography.titleMedium, color = Color.Magenta) },
                actions = {
                    IconButton(
                        onClick = {
                            if (uiState.loadedAndroidBitmap != null && !uiState.isSaving) {
                                Toast.makeText(context, "Rendering image...", Toast.LENGTH_SHORT).show()
                                viewModel.onEvent(StudioEvent.SaveImage(context) { success ->
                                    if (success) {
                                        Toast.makeText(context, "IMAGE SAVED TO VAULT!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "ERR: Render failed", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            } else if (uiState.loadedAndroidBitmap == null) {
                                Toast.makeText(context, "Load gambar dulu bro!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Export",
                            tint = if (uiState.isSaving) Color.Gray else Color.Green,
                            modifier = if (uiState.isSaving) Modifier.y2kBlinkEffect(500) else Modifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                modifier = Modifier.drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(color = Color.Magenta, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = strokeWidth)
                }
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // --- AREA PREVIEW GAMBAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0A0A0A))
                    .border(2.dp, Color.White, RectangleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.loadedImageBitmap != null) {
                    Image(
                        bitmap = uiState.loadedImageBitmap!!,
                        contentDescription = "Edit Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.colorMatrix(composeColorMatrix)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = "Load", tint = Color.Cyan, modifier = Modifier.size(48.dp).y2kBlinkEffect(800))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("TAP TO LOAD IMAGE", color = Color.Cyan, style = Y2KTypography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TAB NAVIGASI TERMINAL ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StudioTabButton("PRESETS", uiState.selectedTab == 0) { viewModel.onEvent(StudioEvent.ChangeTab(0)) }
                Spacer(modifier = Modifier.width(8.dp))
                StudioTabButton("MANUAL TWEAKS", uiState.selectedTab == 1) { viewModel.onEvent(StudioEvent.ChangeTab(1)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- AREA KONTROL BAWAH ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .border(2.dp, Color.DarkGray, RectangleShape)
                    .background(Color(0xFF111111))
                    .padding(16.dp)
            ) {
                if (uiState.selectedTab == 0) {
                    // TAB 0: DAFTAR PRESET
                    val allPresets = defaultPresets + uiState.savedCustomPresets
                    Row(
                        modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allPresets.forEach { preset ->
                            PresetCard(preset, uiState.loadedImageBitmap) {
                                viewModel.onEvent(StudioEvent.ApplyPreset(preset))
                            }
                        }
                    }
                } else {
                    // TAB 1: SLIDER MANUAL
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Y2KSlider("BRIGHTNESS", uiState.currentBrightness, 0.5f, 1.5f) { viewModel.onEvent(StudioEvent.UpdateBrightness(it)) }
                        Y2KSlider("CONTRAST", uiState.currentContrast, 0.5f, 1.5f) { viewModel.onEvent(StudioEvent.UpdateContrast(it)) }
                        Y2KSlider("SATURATION", uiState.currentSaturation, 0f, 2f) { viewModel.onEvent(StudioEvent.UpdateSaturation(it)) }
                        Y2KSlider("CCD WARMTH", uiState.currentWarmth, -1f, 1f) { viewModel.onEvent(StudioEvent.UpdateWarmth(it)) }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.DarkGray)
                                .border(2.dp, Color.Cyan, RectangleShape)
                                .clickable { viewModel.onEvent(StudioEvent.ToggleSaveDialog(true)) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SAVE AS CUSTOM PRESET >>", color = Color.Cyan, style = Y2KTypography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG SIMPAN PRESET KUSTOM ---
    if (uiState.showSaveDialog) {
        Dialog(onDismissRequest = { viewModel.onEvent(StudioEvent.ToggleSaveDialog(false)) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .border(3.dp, Color.Cyan, RectangleShape)
                    .padding(20.dp)
            ) {
                Column {
                    Text("[ SAVE_PRESET.cfg ]", color = Color.Cyan, style = Y2KTypography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.newPresetName,
                        onValueChange = { viewModel.onEvent(StudioEvent.UpdateNewPresetName(it)) },
                        textStyle = Y2KTypography.bodyMedium.copy(color = Color.White),
                        placeholder = { Text("PRESET_NAME", color = Color.Gray, style = Y2KTypography.bodySmall) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Magenta,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "CANCEL",
                            color = Color.Red,
                            style = Y2KTypography.bodyMedium,
                            modifier = Modifier.clickable { viewModel.onEvent(StudioEvent.ToggleSaveDialog(false)) }.padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "SAVE",
                            color = Color.Green,
                            style = Y2KTypography.bodyMedium,
                            modifier = Modifier.clickable {
                                viewModel.onEvent(StudioEvent.SaveCustomPreset)
                            }.padding(8.dp).drawBehind {
                                drawLine(Color.Green, Offset(0f, size.height), Offset(size.width, size.height), 2f)
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- KOMPONEN BANTUAN UI ---

@Composable
fun RowScope.StudioTabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color.Magenta else Color.Black
    val textColor = if (isSelected) Color.White else Color.Gray
    val borderColor = if (isSelected) Color.Magenta else Color.DarkGray

    Box(
        modifier = Modifier
            .weight(1f)
            .background(bgColor)
            .border(2.dp, borderColor, RectangleShape)
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = textColor, style = Y2KTypography.bodyMedium)
    }
}

@Composable
fun PresetCard(preset: Y2KPreset, previewBitmap: androidx.compose.ui.graphics.ImageBitmap?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp)
            .background(Color.Black)
            .border(2.dp, Color.White, RectangleShape)
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(40.dp).background(Color.DarkGray).border(1.dp, Color.Cyan, RectangleShape), contentAlignment = Alignment.Center) {
                if (previewBitmap != null) {
                    val previewMatrix = remember(preset) {
                        val matrix = ColorMatrixUtil.createAndroidColorMatrix(preset.brightness, preset.contrast, preset.saturation, preset.warmth)
                        androidx.compose.ui.graphics.ColorMatrix(matrix.array)
                    }
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(previewMatrix)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(preset.name, color = Color.White, style = Y2KTypography.bodySmall, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun Y2KSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("> $label", color = Color.Cyan, style = Y2KTypography.bodySmall)
            Text(String.format(java.util.Locale.US, "%.2f", value), color = Color.Magenta, style = Y2KTypography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = Color.Magenta,
                activeTrackColor = Color.Cyan,
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.height(30.dp)
        )
    }
}