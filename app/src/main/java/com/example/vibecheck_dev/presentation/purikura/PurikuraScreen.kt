package com.example.vibecheck_dev.presentation.purikura

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Output
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurikuraScreen(viewModel: PurikuraViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var currentSlotIndex by remember { mutableIntStateOf(-1) }

    // --- SEDOT WARNA DINAMIS DARI TEMA (HANYA UNTUK UI, BUKAN UNTUK ISI FOTO) ---
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val errorColor = MaterialTheme.colorScheme.error
    val borderColor = onBgColor.copy(alpha = 0.3f)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && currentSlotIndex != -1) {
            viewModel.onEvent(PurikuraEvent.AddPhoto(currentSlotIndex, uri))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PURIKURA.exe",
                        style = Y2KTypography.titleMedium,
                        color = primaryColor
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (uiState.photos.isNotEmpty()) {
                                viewModel.onEvent(PurikuraEvent.ToggleActionDialog(true))
                            } else {
                                Toast.makeText(context, "Pilih foto dulu bro!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Output,
                            contentDescription = "Execute",
                            tint = if (uiState.isSaving) borderColor else secondaryColor,
                            modifier = if (uiState.isSaving) Modifier.y2kBlinkEffect(500) else Modifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor),
                modifier = Modifier.drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(primaryColor, Offset(0f, y), Offset(size.width, y), strokeWidth)
                }
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- AREA CANVAS PREVIEW ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {

                    Box(
                        modifier = Modifier
                            .background(uiState.frameColor)
                            .drawBehind {
                                val patternAlpha = 0.2f
                                // FIX: Warna kontras motif murni berdasarkan warna absolut frame, bukan tema aplikasi
                                val composeContrast =
                                    if (uiState.frameColor == Color.Black) Color.White else Color.Black

                                when (uiState.selectedStyle) {
                                    FrameStyle.GRID -> {
                                        for (i in 0..size.width.toInt() step 40) drawLine(
                                            composeContrast.copy(alpha = patternAlpha),
                                            Offset(i.toFloat(), 0f),
                                            Offset(i.toFloat(), size.height),
                                            4f
                                        )
                                        for (i in 0..size.height.toInt() step 40) drawLine(
                                            composeContrast.copy(alpha = patternAlpha),
                                            Offset(0f, i.toFloat()),
                                            Offset(size.width, i.toFloat()),
                                            4f
                                        )
                                    }

                                    FrameStyle.STRIPES -> {
                                        for (i in -size.height.toInt()..size.width.toInt() step 60) {
                                            drawLine(
                                                composeContrast.copy(alpha = patternAlpha),
                                                Offset(i.toFloat(), 0f),
                                                Offset((i + size.height), size.height),
                                                12f
                                            )
                                        }
                                    }

                                    FrameStyle.DOTS -> {
                                        for (x in 10..size.width.toInt() step 30) {
                                            for (y in 10..size.height.toInt() step 30) {
                                                drawCircle(
                                                    composeContrast.copy(alpha = patternAlpha),
                                                    4f,
                                                    Offset(x.toFloat(), y.toFloat())
                                                )
                                            }
                                        }
                                    }

                                    FrameStyle.SOLID -> {}
                                }

                                drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas

                                    val fontResId = context.resources.getIdentifier(
                                        "press_start",
                                        "font",
                                        context.packageName
                                    )
                                    val customTypeface = if (fontResId != 0) {
                                        ResourcesCompat.getFont(context, fontResId)
                                            ?: android.graphics.Typeface.MONOSPACE
                                    } else {
                                        android.graphics.Typeface.DEFAULT_BOLD
                                    }

                                    val textPaint = android.graphics.Paint().apply {
                                        color = uiState.textColor.toArgb()
                                        textSize = 45f
                                        typeface = customTypeface
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }

                                    val iconPaint = android.graphics.Paint(textPaint).apply {
                                        textSize = 65f
                                    }

                                    nativeCanvas.drawText(
                                        "VIBECHECK",
                                        size.width / 2f,
                                        90f,
                                        textPaint
                                    )

                                    val iconY = size.height - 35f
                                    when (uiState.selectedStyle) {
                                        FrameStyle.GRID -> {
                                            nativeCanvas.drawText("👾", 70f, iconY, iconPaint)
                                            nativeCanvas.drawText(
                                                "🕹️",
                                                size.width - 70f,
                                                iconY,
                                                iconPaint
                                            )
                                        }

                                        FrameStyle.STRIPES -> {
                                            nativeCanvas.drawText("⚠", 70f, iconY, iconPaint)
                                            nativeCanvas.drawText(
                                                "🛑",
                                                size.width - 70f,
                                                iconY,
                                                iconPaint
                                            )
                                        }

                                        FrameStyle.DOTS -> {
                                            nativeCanvas.drawText("✿", 70f, iconY, iconPaint)
                                            nativeCanvas.drawText(
                                                "❤",
                                                size.width - 70f,
                                                iconY,
                                                iconPaint
                                            )
                                        }

                                        FrameStyle.SOLID -> {
                                            nativeCanvas.drawText("★", 70f, iconY, iconPaint)
                                            nativeCanvas.drawText(
                                                "📼",
                                                size.width - 70f,
                                                iconY,
                                                iconPaint
                                            )
                                        }
                                    }
                                }
                            }
                            .border(2.dp, borderColor, RectangleShape)
                            .padding(top = 55.dp, bottom = 65.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (r in 0 until uiState.selectedGrid.rows) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    for (c in 0 until uiState.selectedGrid.cols) {
                                        val index = r * uiState.selectedGrid.cols + c
                                        val uri = uiState.photos[index]

                                        val slotSize =
                                            if (uiState.selectedGrid.cols == 1) 160.dp else 120.dp

                                        // FIX: Border foto juga pakai logika absolut, bukan tema
                                        val composeContrast =
                                            if (uiState.frameColor == Color.Black) Color.White else Color.Black
                                        Box(
                                            modifier = Modifier.border(
                                                2.dp,
                                                composeContrast,
                                                RectangleShape
                                            )
                                        ) {
                                            PurikuraNativeSlot(
                                                uri = uri,
                                                sizeDp = slotSize,
                                                // FIX: Kotak kosong dikembalikan ke DarkGray murni (representasi ruang kosong foto)
                                                surfaceColor = Color.DarkGray,
                                                borderColor = Color.LightGray,
                                                onClick = {
                                                    currentSlotIndex = index
                                                    imagePickerLauncher.launch("image/*")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- TOMBOL TOGGLE HIDE/SHOW PANEL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = if (uiState.isPanelVisible) "[ HIDE PANEL ]" else "[ SHOW PANEL ]",
                    color = primaryColor,
                    style = Y2KTypography.bodySmall,
                    modifier = Modifier.clickable { viewModel.onEvent(PurikuraEvent.TogglePanel) }
                )
            }

            // --- AREA KONTROL BAWAH ---
            AnimatedVisibility(visible = uiState.isPanelVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .background(surfaceColor)
                        .border(2.dp, borderColor, RectangleShape)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ROW 1: Layout
                    Text("> SELECT LAYOUT", color = onBgColor, style = Y2KTypography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GridType.values().forEach { grid ->
                            val isSelected = uiState.selectedGrid == grid
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) primaryColor else bgColor)
                                    .border(1.dp, primaryColor, RectangleShape)
                                    .clickable { viewModel.onEvent(PurikuraEvent.ChangeGrid(grid)) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    grid.label,
                                    color = if (isSelected) bgColor else primaryColor,
                                    style = Y2KTypography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ROW 2: Frame Motif
                    Text("> FRAME PATTERN", color = onBgColor, style = Y2KTypography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FrameStyle.values().forEach { style ->
                            val isSelected = uiState.selectedStyle == style
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) secondaryColor else bgColor)
                                    .border(1.dp, secondaryColor, RectangleShape)
                                    .clickable {
                                        viewModel.onEvent(
                                            PurikuraEvent.ChangeFrameStyle(
                                                style
                                            )
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    style.label,
                                    color = if (isSelected) bgColor else secondaryColor,
                                    style = Y2KTypography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ROW 3: Background Frame Color
                    Text("> FRAME COLOR", color = onBgColor, style = Y2KTypography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // FIX: Dikembalikan ke warna mutlak (Absolut) untuk Purikura Frame!
                        val absoluteColors = listOf(
                            Color.White,
                            Color.Black,
                            Color.Magenta,
                            Color.Cyan,
                            Color.Green,
                            Color.Yellow
                        )
                        absoluteColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(color)
                                    .border(
                                        2.dp,
                                        if (uiState.frameColor == color) errorColor else borderColor,
                                        RectangleShape
                                    )
                                    .clickable {
                                        viewModel.onEvent(
                                            PurikuraEvent.ChangeFrameColor(
                                                color
                                            )
                                        )
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ROW 4: Text Color & Clear All
                    Text("> TEXT & ICON COLOR", color = onBgColor, style = Y2KTypography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // FIX: Dikembalikan ke warna mutlak (Absolut)
                        val absoluteTextColors =
                            listOf(Color.White, Color.Black, Color.Cyan, Color.Magenta)
                        absoluteTextColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(color)
                                    .border(
                                        2.dp,
                                        if (uiState.textColor == color) errorColor else borderColor,
                                        RectangleShape
                                    )
                                    .clickable {
                                        viewModel.onEvent(
                                            PurikuraEvent.ChangeTextColor(
                                                color
                                            )
                                        )
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "[ CLEAR ALL ]",
                            color = errorColor,
                            style = Y2KTypography.bodySmall,
                            modifier = Modifier.clickable { viewModel.onEvent(PurikuraEvent.ClearAll) }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG AKSI (SAVE VS PRINT ASLI) ---
    if (uiState.showActionDialog) {
        Dialog(onDismissRequest = { viewModel.onEvent(PurikuraEvent.ToggleActionDialog(false)) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .border(3.dp, primaryColor, RectangleShape)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "SELECT_OUTPUT.exe",
                        color = primaryColor,
                        style = Y2KTypography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.onEvent(PurikuraEvent.SavePurikura(context) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                        shape = RectangleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, onBgColor, RectangleShape)
                    ) {
                        Text("SAVE TO VAULT", color = bgColor, style = Y2KTypography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Membuka Printer Spooler...",
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.onEvent(PurikuraEvent.PrintPurikura(context))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        shape = RectangleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, primaryColor, RectangleShape)
                    ) {
                        Text("PRINT NOW (NATIVE)", style = Y2KTypography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun PurikuraNativeSlot(
    uri: Uri?,
    sizeDp: androidx.compose.ui.unit.Dp,
    surfaceColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmapState by remember(uri) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(
            null
        )
    }

    LaunchedEffect(uri) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source =
                            android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    bitmapState =
                        bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, false).asImageBitmap()
                } catch (e: Exception) {
                    Log.e("PURIKURA_NATIVE", "Gagal memproses bitmap", e)
                }
            }
        } else {
            bitmapState = null
        }
    }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .background(surfaceColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null && bitmapState != null) {
            Image(
                bitmap = bitmapState!!,
                contentDescription = "Slot Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Add, "", tint = borderColor, modifier = Modifier.y2kBlinkEffect(800))
        }
    }
}