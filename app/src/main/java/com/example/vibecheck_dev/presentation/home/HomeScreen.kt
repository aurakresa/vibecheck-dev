package com.example.vibecheck_dev.presentation.home

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Matriks Piksel Raksasa untuk Menu Utama
val PIXEL_CAMERA = listOf(
    "                  ",
    "     XXXXXX       ",
    "   XXX    XXX     ",
    "  XXXXXXXXXXXXXX  ",
    "  XX          XX  ",
    "  XX   XXXX   XX  ",
    "  XX  XX  XX  XX  ",
    "  XX  XX  XX  XX  ",
    "  XX   XXXX   XX  ",
    "  XX          XX  ",
    "  XXXXXXXXXXXXXX  ",
    "                  "
)

val PIXEL_REMOTE = listOf(
    "                  ",
    "      XXXXXX      ",
    "     XX    XX     ",
    "     XX XX XX     ",
    "     XX    XX     ",
    "     XXXXXXXX     ",
    "     XX    XX     ",
    "     XX XX XX     ",
    "     XX    XX     ",
    "      XXXXXX      ",
    "                  "
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToRemote: () -> Unit,
    onLogout: () -> Unit = {},
    isGuestMode: Boolean = false,
    guestName: String = "GUEST",
    activeThemeName: String = "Y2K BRIGHT NEON",
    onThemeChanged: (String) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State untuk masing-masing Dialog
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val errorColor = MaterialTheme.colorScheme.error
    val borderColor = onBgColor.copy(alpha = 0.3f)

    // Tambahkan state ini buat nyimpen pilihan tema
    var activeTheme by remember(activeThemeName) { mutableStateOf(activeThemeName) }

    // State Data Profil
    // Jika User Asli (bukan Guest), set default name ke guestName yang didapat dari DataStore (kalau ada)
    var editUsername by remember { mutableStateOf(if (isGuestMode) guestName else "ADMIN / USER") }
    var editPassword by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { profileImageUri = it } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.8f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = surfaceColor, // DIUBAH KE TEMA
                drawerShape = RectangleShape,
                modifier = Modifier
                    .width(300.dp)
                    .border(2.dp, secondaryColor, RectangleShape) // DIUBAH KE TEMA
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SYS_MENU.exe",
                        style = Y2KTypography.titleLarge,
                        color = primaryColor, // DIUBAH KE TEMA
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = secondaryColor, thickness = 2.dp) // DIUBAH KE TEMA
                    Spacer(modifier = Modifier.height(32.dp))

                    val fontResId =
                        context.resources.getIdentifier("press_start", "font", context.packageName)
                    val pixelFontFamily =
                        if (fontResId != 0) FontFamily(Font(fontResId)) else FontFamily.Monospace

                    // --- LOGIKA TAMPILAN SIDEBAR (GUEST vs USER ASLI) ---
                    if (isGuestMode) {
                        // TAMPILAN GUEST: Foto Dummy, Nama Guest, Tanpa Edit Profile
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(bgColor) // DIUBAH KE TEMA
                                .border(4.dp, borderColor, RectangleShape) // DIUBAH KE TEMA
                                .padding(6.dp)
                                .border(2.dp, secondaryColor, RectangleShape), // DIUBAH KE TEMA
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Guest",
                                tint = borderColor, // DIUBAH KE TEMA
                                modifier = Modifier.size(80.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = guestName.uppercase(),
                            color = onBgColor, // DIUBAH KE TEMA
                            fontFamily = pixelFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            DrawerMenuItem(
                                title = "THEME.ini",
                                primaryColor,
                                secondaryColor,
                                onBgColor
                            ) {
                                coroutineScope.launch { drawerState.close() }
                                showThemeDialog = true
                            }
                            DrawerMenuItem(
                                title = "ABOUT.txt",
                                primaryColor,
                                secondaryColor,
                                onBgColor
                            ) {
                                coroutineScope.launch { drawerState.close() }
                                showAboutDialog = true
                            }
                        }

                    } else {
                        // TAMPILAN USER ASLI: Bisa ganti foto, muncul Edit Profile
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(bgColor) // DIUBAH KE TEMA
                                .border(4.dp, borderColor, RectangleShape) // DIUBAH KE TEMA
                                .padding(6.dp)
                                .border(2.dp, secondaryColor, RectangleShape) // DIUBAH KE TEMA
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            NativeProfileImage(
                                profileImageUri,
                                primaryColor
                            ) // Pakai NativeProfileImage
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = editUsername.uppercase(),
                            color = onBgColor, // DIUBAH KE TEMA
                            fontFamily = pixelFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // EDIT PROFILE HANYA MUNCUL DI SINI (USER ASLI)
                            DrawerMenuItem(
                                title = "EDIT_PROFILE.exe",
                                primaryColor,
                                secondaryColor,
                                onBgColor
                            ) {
                                coroutineScope.launch { drawerState.close() }
                                showEditProfileDialog = true
                            }
                            DrawerMenuItem(
                                title = "THEME.ini",
                                primaryColor,
                                secondaryColor,
                                onBgColor
                            ) {
                                coroutineScope.launch { drawerState.close() }
                                showThemeDialog = true
                            }
                            DrawerMenuItem(
                                title = "ABOUT.txt",
                                primaryColor,
                                secondaryColor,
                                onBgColor
                            ) {
                                coroutineScope.launch { drawerState.close() }
                                showAboutDialog = true
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Tombol Logout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showLogoutDialog = true
                            }
                            .background(errorColor.copy(alpha = 0.2f)) // DIUBAH KE TEMA
                            .border(2.dp, errorColor, RectangleShape) // DIUBAH KE TEMA
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "[ SYSTEM_LOGOUT ]",
                            color = errorColor, // DIUBAH KE TEMA
                            style = Y2KTypography.bodyMedium
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "VIBECHECK_OS",
                            style = Y2KTypography.titleMedium,
                            color = tertiaryColor
                        ) // DIUBAH KE TEMA
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = tertiaryColor // DIUBAH KE TEMA
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bgColor // DIUBAH KE TEMA
                    ),
                    modifier = Modifier.drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color = tertiaryColor, // DIUBAH KE TEMA
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
                )
            },
            containerColor = bgColor // DIUBAH KE TEMA
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT ROLE",
                    style = Y2KTypography.titleLarge,
                    color = onBgColor // DIUBAH KE TEMA
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pilih perangkat ini mau jadi lensa kamera atau remote kontrol.",
                    style = Y2KTypography.bodyMedium,
                    color = borderColor, // DIUBAH KE TEMA
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))

                RoleButton(
                    title = "HOST (CAMERA)",
                    description = "Aktifin AI & Pancarin P2P",
                    pixelMatrix = PIXEL_CAMERA,
                    accentColor = primaryColor, // DIUBAH KE TEMA
                    textColor = onBgColor, // DIUBAH KE TEMA
                    borderColor = borderColor, // DIUBAH KE TEMA
                    onClick = onNavigateToCamera
                )

                Spacer(modifier = Modifier.height(24.dp))

                RoleButton(
                    title = "REMOTE (CTRL)",
                    description = "Konek ke Host & Jepret",
                    pixelMatrix = PIXEL_REMOTE,
                    accentColor = secondaryColor, // DIUBAH KE TEMA
                    textColor = onBgColor, // DIUBAH KE TEMA
                    borderColor = borderColor, // DIUBAH KE TEMA
                    onClick = onNavigateToRemote
                )
            }
        }
    }

    // --- IMPLEMENTASI KUMPULAN DIALOG Y2K ---

    if (showEditProfileDialog) {
        Y2KDialogWrapper(
            title = "EDIT_PROFILE",
            accentColor = primaryColor, // DIUBAH KE TEMA
            bgColor = bgColor, // DIUBAH KE TEMA
            textColor = onBgColor, // DIUBAH KE TEMA
            borderColor = borderColor, // DIUBAH KE TEMA
            onDismiss = { showEditProfileDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(bgColor) // DIUBAH KE TEMA
                        .border(2.dp, primaryColor, RectangleShape) // DIUBAH KE TEMA
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    NativeProfileImage(profileImageUri, primaryColor)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(primaryColor) // DIUBAH KE TEMA
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "EDIT",
                            color = bgColor,
                            style = Y2KTypography.bodySmall
                        ) // DIUBAH KE TEMA
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = editUsername,
                    onValueChange = { editUsername = it },
                    label = {
                        Text(
                            "> USERNAME",
                            color = primaryColor, // DIUBAH KE TEMA
                            style = Y2KTypography.bodySmall
                        )
                    },
                    textStyle = Y2KTypography.bodyMedium.copy(color = onBgColor), // DIUBAH KE TEMA
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor, // DIUBAH KE TEMA
                        unfocusedBorderColor = borderColor // DIUBAH KE TEMA
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editPassword,
                    onValueChange = { editPassword = it },
                    label = {
                        Text(
                            "> PASSWORD",
                            color = primaryColor, // DIUBAH KE TEMA
                            style = Y2KTypography.bodySmall
                        )
                    },
                    textStyle = Y2KTypography.bodyMedium.copy(color = onBgColor), // DIUBAH KE TEMA
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor, // DIUBAH KE TEMA
                        unfocusedBorderColor = borderColor // DIUBAH KE TEMA
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Y2KDialogButton("SAVE_CHANGES", tertiaryColor, onBgColor) {
                    showEditProfileDialog = false
                } // DIUBAH KE TEMA
            }
        }
    }

    if (showThemeDialog) {
        Y2KDialogWrapper(
            title = "SELECT_THEME",
            accentColor = primaryColor, // DIUBAH KE TEMA
            bgColor = bgColor, // DIUBAH KE TEMA
            textColor = onBgColor, // DIUBAH KE TEMA
            borderColor = borderColor, // DIUBAH KE TEMA
            onDismiss = { showThemeDialog = false }) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ThemeOptionItem(
                    name = "Y2K BRIGHT NEON",
                    colors = listOf(Color.Black, Color.Magenta, Color.Cyan),
                    isActive = activeTheme == "Y2K BRIGHT NEON",
                    textColor = onBgColor // DIUBAH KE TEMA
                ) { activeTheme = "Y2K BRIGHT NEON" }

                ThemeOptionItem(
                    name = "MATRIX TERMINAL",
                    colors = listOf(Color.Black, Color.Green, Color.DarkGray),
                    isActive = activeTheme == "MATRIX TERMINAL",
                    textColor = onBgColor // DIUBAH KE TEMA
                ) { activeTheme = "MATRIX TERMINAL" }

                ThemeOptionItem(
                    name = "UBUNTU DARK",
                    colors = listOf(Color(0xFF300A24), Color(0xFFE95420), Color.White),
                    isActive = activeTheme == "UBUNTU DARK",
                    textColor = onBgColor // DIUBAH KE TEMA
                ) { activeTheme = "UBUNTU DARK" }

                ThemeOptionItem(
                    name = "CYBER CHROME (LIGHT)",
                    colors = listOf(Color(0xFFE0E0E0), Color(0xFFFF5722), Color(0xFF00E5FF)),
                    isActive = activeTheme == "CYBER CHROME (LIGHT)",
                    textColor = onBgColor // DIUBAH KE TEMA
                ) { activeTheme = "CYBER CHROME (LIGHT)" }

                Spacer(modifier = Modifier.height(12.dp))

                Y2KDialogButton("APPLY", primaryColor, onBgColor) { // DIUBAH KE TEMA
                    onThemeChanged(activeTheme)
                    showThemeDialog = false
                }
            }
        }
    }

    if (showAboutDialog) {
        Y2KDialogWrapper(
            title = "ABOUT_SYS",
            accentColor = tertiaryColor, // DIUBAH KE TEMA
            bgColor = bgColor, // DIUBAH KE TEMA
            textColor = onBgColor, // DIUBAH KE TEMA
            borderColor = borderColor, // DIUBAH KE TEMA
            onDismiss = { showAboutDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "VIBECHECK_OS v1.0.0",
                    color = tertiaryColor,
                    style = Y2KTypography.titleMedium
                ) // DIUBAH KE TEMA
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Engine: Jetpack Compose\nProtocol: Wi-Fi P2P Direct\nVision: ML Kit Pose Detecion",
                    color = borderColor, // DIUBAH KE TEMA
                    style = Y2KTypography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Coded with blood, sweat, and pure logic.",
                    color = onBgColor, // DIUBAH KE TEMA
                    style = Y2KTypography.bodyMedium,
                    modifier = Modifier.y2kBlinkEffect(1000)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Y2KDialogButton("CLOSE", tertiaryColor, onBgColor) {
                    showAboutDialog = false
                } // DIUBAH KE TEMA
            }
        }
    }

    if (showLogoutDialog) {
        Y2KDialogWrapper(
            title = "WARNING!",
            accentColor = errorColor, // DIUBAH KE TEMA
            bgColor = bgColor, // DIUBAH KE TEMA
            textColor = onBgColor, // DIUBAH KE TEMA
            borderColor = borderColor, // DIUBAH KE TEMA
            onDismiss = { showLogoutDialog = false }) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "TERMINATE SESSION?",
                    color = errorColor, // DIUBAH KE TEMA
                    style = Y2KTypography.titleLarge,
                    modifier = Modifier.y2kBlinkEffect(500)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Y2KDialogButton("NO / ABORT", borderColor, onBgColor) {
                            showLogoutDialog = false
                        } // DIUBAH KE TEMA
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        Y2KDialogButton("YES / KILL", errorColor, onBgColor) { // DIUBAH KE TEMA
                            showLogoutDialog = false
                            onLogout()
                        }
                    }
                }
            }
        }
    }
}

// --- SUBKOMPONEN UI ---

@Composable
fun RoleButton(
    title: String,
    description: String,
    pixelMatrix: List<String>,
    accentColor: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(borderColor.copy(alpha = 0.2f)) // DIUBAH KE TEMA
            .border(2.dp, accentColor, RectangleShape) // DIUBAH KE TEMA
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(56.dp)
                    .aspectRatio(1f)
            ) {
                val rows = pixelMatrix.size
                val cols = pixelMatrix.maxOf { it.length }
                val pixelWidth = size.width / cols
                val pixelHeight = size.height / rows

                for (r in 0 until rows) {
                    for (c in 0 until pixelMatrix[r].length) {
                        if (pixelMatrix[r][c] == 'X') {
                            drawRect(
                                color = accentColor, // DIUBAH KE TEMA
                                topLeft = Offset(c * pixelWidth, r * pixelHeight),
                                size = Size(pixelWidth, pixelHeight)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = Y2KTypography.bodyLarge,
                    color = accentColor
                ) // DIUBAH KE TEMA
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = Y2KTypography.bodySmall,
                    color = textColor
                ) // DIUBAH KE TEMA
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    title: String,
    primaryColor: Color,
    secondaryColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .background(secondaryColor.copy(alpha = 0.1f)) // DIUBAH KE TEMA
            .border(2.dp, secondaryColor, RectangleShape) // DIUBAH KE TEMA
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "[", color = textColor, style = Y2KTypography.bodyMedium) // DIUBAH KE TEMA
            Text(
                text = " RUN ",
                color = primaryColor, // DIUBAH KE TEMA
                style = Y2KTypography.bodyMedium,
                modifier = Modifier.y2kBlinkEffect(800)
            )
            Text(text = "] ", color = textColor, style = Y2KTypography.bodyMedium) // DIUBAH KE TEMA
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = Y2KTypography.bodyLarge,
                color = secondaryColor
            ) // DIUBAH KE TEMA
        }
    }
}

@Composable
fun Y2KDialogWrapper(
    title: String,
    accentColor: Color,
    bgColor: Color,
    textColor: Color,
    borderColor: Color,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor) // DIUBAH KE TEMA
                .border(3.dp, accentColor, RectangleShape) // DIUBAH KE TEMA
                .padding(2.dp)
                .border(1.dp, borderColor, RectangleShape) // DIUBAH KE TEMA
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "[$title]",
                        color = accentColor,
                        style = Y2KTypography.titleMedium
                    ) // DIUBAH KE TEMA
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = borderColor, // DIUBAH KE TEMA
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                HorizontalDivider(color = borderColor, thickness = 1.dp) // DIUBAH KE TEMA
                Spacer(modifier = Modifier.height(16.dp))

                content()
            }
        }
    }
}

@Composable
fun Y2KDialogButton(text: String, accentColor: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(accentColor.copy(alpha = 0.2f)) // DIUBAH KE TEMA
            .border(2.dp, accentColor, RectangleShape) // DIUBAH KE TEMA
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor, // DIUBAH KE TEMA
            style = Y2KTypography.bodyMedium
        )
    }
}

@Composable
fun ThemeOptionItem(
    name: String,
    colors: List<Color>,
    isActive: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isActive) textColor.copy(alpha = 0.2f) else Color.Transparent) // DIUBAH KE TEMA
            .border(
                1.dp,
                if (isActive) textColor else textColor.copy(alpha = 0.3f),
                RectangleShape
            ) // DIUBAH KE TEMA
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Render 3 Kotak Warna Preview
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(color)
                        .border(1.dp, Color.White, RectangleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = name,
            color = textColor, // DIUBAH KE TEMA
            style = Y2KTypography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isActive) {
            Text(
                "<",
                color = colors[1], // Panah mengikuti warna tema
                style = Y2KTypography.bodyMedium,
                modifier = Modifier.y2kBlinkEffect(500)
            )
        }
    }
}

@Composable
fun NativeProfileImage(uri: Uri?, primaryColor: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmapState by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

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
                    Log.e("PROFILE_IMG", "Gagal load foto profil", e)
                }
            }
        } else {
            bitmapState = null
        }
    }

    if (bitmapState != null) {
        Image(
            bitmap = bitmapState!!,
            contentDescription = "Profile Photo",
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Empty Profile",
            tint = primaryColor, // DIUBAH KE TEMA
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}