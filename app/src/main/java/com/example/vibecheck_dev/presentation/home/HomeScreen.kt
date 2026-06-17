package com.example.vibecheck_dev.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vibecheck_dev.presentation.components.y2kBlinkEffect
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

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
    onNavigateToLogs: () -> Unit = {}, // 🔴 TAMBAHIN INI
    onLogout: () -> Unit = {},
    isGuestMode: Boolean = false,
    guestName: String = "GUEST",
    activeThemeName: String = "Y2K BRIGHT NEON",
    onThemeChanged: (String) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel() // Integrasi ViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Ambil UI State dari ViewModel
    val uiState by viewModel.uiState.collectAsState()

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

    var activeTheme by remember(activeThemeName) { mutableStateOf(activeThemeName) }

    // Dialog Edit Profile States
    var editUsername by remember { mutableStateOf(uiState.username) }
    var editPassword by remember { mutableStateOf("")}

    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }

    val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

    // Sinkronkan state lokal ke uiState
    LaunchedEffect(uiState.username) {
        editUsername = uiState.username
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(HomeEvent.UpdateProfileImage(it)) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.8f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = surfaceColor,
                drawerShape = RectangleShape,
                modifier = Modifier
                    .width(300.dp)
                    .border(2.dp, secondaryColor, RectangleShape)
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
                        color = primaryColor,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = secondaryColor, thickness = 2.dp)
                    Spacer(modifier = Modifier.height(32.dp))

                    val fontResId = context.resources.getIdentifier("press_start", "font", context.packageName)
                    val pixelFontFamily = if (fontResId != 0) FontFamily(Font(fontResId)) else FontFamily.Monospace

                    if (isGuestMode) {
                        // TAMPILAN GUEST
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(bgColor)
                                .border(4.dp, borderColor, RectangleShape)
                                .padding(6.dp)
                                .border(2.dp, secondaryColor, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Guest",
                                tint = borderColor,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = guestName.uppercase(),
                            color = onBgColor,
                            fontFamily = pixelFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            DrawerMenuItem(title = "THEME.ini", primaryColor, secondaryColor, onBgColor) {
                                coroutineScope.launch { drawerState.close() }
                                showThemeDialog = true
                            }
                            DrawerMenuItem(title = "ABOUT.txt", primaryColor, secondaryColor, onBgColor) {
                                coroutineScope.launch { drawerState.close() }
                                showAboutDialog = true
                            }
                        }
                    } else {
                        // TAMPILAN USER ASLI
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(bgColor)
                                .border(4.dp, borderColor, RectangleShape)
                                .padding(6.dp)
                                .border(2.dp, secondaryColor, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // 🔴 TRIK DEWA: Render Icon di belakang, warnanya dijamin gak bakal hitam!
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Empty Profile",
                                tint = primaryColor,
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )

                            if (uiState.profileImageUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(uiState.profileImageUri?.toString())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                    // error dan placeholder dihapus
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Tampilkan username dari ViewModel
                        Text(
                            text = uiState.username.uppercase(),
                            color = onBgColor,
                            fontFamily = pixelFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            DrawerMenuItem(title = "EDIT_PROFILE.exe", primaryColor, secondaryColor, onBgColor) {
                                coroutineScope.launch { drawerState.close() }
                                showEditProfileDialog = true
                            }
                            DrawerMenuItem(title = "THEME.ini", primaryColor, secondaryColor, onBgColor) {
                                coroutineScope.launch { drawerState.close() }
                                showThemeDialog = true
                            }
                            DrawerMenuItem(title = "SYS_AUDIT.log", primaryColor, secondaryColor, onBgColor) {
                                coroutineScope.launch { drawerState.close() }
                                onNavigateToLogs()
                            }
                            DrawerMenuItem(title = "ABOUT.txt", primaryColor, secondaryColor, onBgColor) {
                                coroutineScope.launch { drawerState.close() }
                                showAboutDialog = true
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showLogoutDialog = true
                            }
                            .background(errorColor.copy(alpha = 0.2f))
                            .border(2.dp, errorColor, RectangleShape)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "[ SYSTEM_LOGOUT ]",
                            color = errorColor,
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
                    title = { Text("VIBECHECK_OS", style = Y2KTypography.titleMedium, color = tertiaryColor) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = tertiaryColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor),
                    modifier = Modifier.drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        val y = size.height - strokeWidth / 2
                        drawLine(color = tertiaryColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = strokeWidth)
                    }
                )
            },
            containerColor = bgColor
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "SELECT ROLE", style = Y2KTypography.titleLarge, color = onBgColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Pilih perangkat ini mau jadi lensa kamera atau remote kontrol.", style = Y2KTypography.bodyMedium, color = borderColor, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(48.dp))

                RoleButton("HOST (CAMERA)", "Aktifin AI & Pancarin P2P", PIXEL_CAMERA, primaryColor, onBgColor, borderColor, onNavigateToCamera)
                Spacer(modifier = Modifier.height(24.dp))
                RoleButton("REMOTE (CTRL)", "Konek ke Host & Jepret", PIXEL_REMOTE, secondaryColor, onBgColor, borderColor, onNavigateToRemote)
            }
        }
    }

    if (showEditProfileDialog) {
        Y2KDialogWrapper(title = "EDIT_PROFILE", accentColor = primaryColor, bgColor = bgColor, textColor = onBgColor, borderColor = borderColor, onDismiss = { showEditProfileDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

                // --- FOTO PROFIL (Disable klik jika Google Login) ---
                // --- FOTO PROFIL (Disable klik jika Google Login) ---
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(bgColor)
                        .border(2.dp, primaryColor, RectangleShape)
                        .then(if (!uiState.isGoogleLogin) Modifier.clickable { imagePickerLauncher.launch("image/*") } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    // 🔴 TRIK DEWA: Render Icon di belakang
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Empty Profile",
                        tint = primaryColor,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )

                    if (uiState.profileImageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uiState.profileImageUri?.toString())
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                            // error dan placeholder dihapus
                        )
                    }

                    if (!uiState.isGoogleLogin) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd).background(primaryColor).padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text("EDIT", color = bgColor, style = Y2KTypography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- USERNAME ---
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.onEvent(HomeEvent.UpdateUsername(it)) },
                    label = { Text("> USERNAME", color = primaryColor, style = Y2KTypography.bodySmall) },
                    textStyle = Y2KTypography.bodyMedium.copy(color = onBgColor),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = secondaryColor, unfocusedBorderColor = borderColor),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                )

                // --- PASSWORD KHUSUS EMAIL LOGIN (OAuth / Google disembunyikan) ---
                if (!uiState.isGoogleLogin) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // OLD PASSWORD
                    OutlinedTextField(
                        value = uiState.oldPassword,
                        onValueChange = { viewModel.onEvent(HomeEvent.UpdateOldPassword(it)) },
                        label = { Text("> OLD PASSWORD", color = primaryColor, style = Y2KTypography.bodySmall) },
                        textStyle = Y2KTypography.bodyMedium.copy(color = onBgColor),
                        singleLine = true,
                        visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle old password", tint = primaryColor)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = secondaryColor, unfocusedBorderColor = borderColor),
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // NEW PASSWORD
                    OutlinedTextField(
                        value = uiState.newPassword,
                        onValueChange = { viewModel.onEvent(HomeEvent.UpdateNewPassword(it)) },
                        label = { Text("> NEW PASSWORD", color = primaryColor, style = Y2KTypography.bodySmall) },
                        textStyle = Y2KTypography.bodyMedium.copy(color = onBgColor),
                        singleLine = true,
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle new password", tint = primaryColor)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = secondaryColor, unfocusedBorderColor = borderColor),
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // --- TAMPILAN ERROR/SUCCESS ---
                if (uiState.saveError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "ERR: ${uiState.saveError}", color = errorColor, style = Y2KTypography.bodySmall, textAlign = TextAlign.Center)
                }
                if (uiState.saveSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "DATA SAVED SUCCESSFULLY", color = primaryColor, style = Y2KTypography.bodySmall, textAlign = TextAlign.Center)

                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1000)
                        showEditProfileDialog = false
                        viewModel.onEvent(HomeEvent.ResetSaveStatus)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.onEvent(HomeEvent.SaveProfile(context)) },
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = tertiaryColor, disabledContainerColor = borderColor),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.isSaving) "SAVING..." else "SAVE_CHANGES",
                        color = bgColor,
                        style = Y2KTypography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        Y2KDialogWrapper(title = "SELECT_THEME", accentColor = primaryColor, bgColor = bgColor, textColor = onBgColor, borderColor = borderColor, onDismiss = { showThemeDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeOptionItem("Y2K BRIGHT NEON", listOf(Color.Black, Color.Magenta, Color.Cyan), activeTheme == "Y2K BRIGHT NEON", onBgColor) { activeTheme = "Y2K BRIGHT NEON" }
                ThemeOptionItem("MATRIX TERMINAL", listOf(Color.Black, Color.Green, Color.DarkGray), activeTheme == "MATRIX TERMINAL", onBgColor) { activeTheme = "MATRIX TERMINAL" }
                ThemeOptionItem("UBUNTU DARK", listOf(Color(0xFF300A24), Color(0xFFE95420), Color.White), activeTheme == "UBUNTU DARK", onBgColor) { activeTheme = "UBUNTU DARK" }
                ThemeOptionItem("CYBER CHROME (LIGHT)", listOf(Color(0xFFE0E0E0), Color(0xFFFF5722), Color(0xFF00E5FF)), activeTheme == "CYBER CHROME (LIGHT)", onBgColor) { activeTheme = "CYBER CHROME (LIGHT)" }
                Spacer(modifier = Modifier.height(12.dp))
                Y2KDialogButton("APPLY", primaryColor, onBgColor) { onThemeChanged(activeTheme); showThemeDialog = false }
            }
        }
    }

    if (showAboutDialog) {
        Y2KDialogWrapper(title = "ABOUT_SYS", accentColor = tertiaryColor, bgColor = bgColor, textColor = onBgColor, borderColor = borderColor, onDismiss = { showAboutDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("VIBECHECK_OS v1.0.0", color = tertiaryColor, style = Y2KTypography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Engine: Jetpack Compose\nProtocol: Wi-Fi P2P Direct\nVision: ML Kit Pose Detecion", color = borderColor, style = Y2KTypography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coded with blood, sweat, and pure logic.", color = onBgColor, style = Y2KTypography.bodyMedium, modifier = Modifier.y2kBlinkEffect(1000))
                Spacer(modifier = Modifier.height(24.dp))
                Y2KDialogButton("CLOSE", tertiaryColor, onBgColor) { showAboutDialog = false }
            }
        }
    }

    if (showLogoutDialog) {
        Y2KDialogWrapper(title = "WARNING!", accentColor = errorColor, bgColor = bgColor, textColor = onBgColor, borderColor = borderColor, onDismiss = { showLogoutDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TERMINATE SESSION?", color = errorColor, style = Y2KTypography.titleLarge, modifier = Modifier.y2kBlinkEffect(500))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.weight(1f)) { Y2KDialogButton("NO / ABORT", borderColor, onBgColor) { showLogoutDialog = false } }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f)) { Y2KDialogButton("YES / KILL", errorColor, onBgColor) { showLogoutDialog = false; onLogout() } }
                }
            }
        }
    }
}

@Composable
fun RoleButton(title: String, description: String, pixelMatrix: List<String>, accentColor: Color, textColor: Color, borderColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clickable { onClick() }.background(borderColor.copy(alpha = 0.2f)).border(2.dp, accentColor, RectangleShape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(56.dp).aspectRatio(1f)) {
                val rows = pixelMatrix.size
                val cols = pixelMatrix.maxOf { it.length }
                val pixelWidth = size.width / cols
                val pixelHeight = size.height / rows
                for (r in 0 until rows) {
                    for (c in 0 until pixelMatrix[r].length) {
                        if (pixelMatrix[r][c] == 'X') {
                            drawRect(color = accentColor, topLeft = Offset(c * pixelWidth, r * pixelHeight), size = Size(pixelWidth, pixelHeight))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = Y2KTypography.bodyLarge, color = accentColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = Y2KTypography.bodySmall, color = textColor)
            }
        }
    }
}

@Composable
fun DrawerMenuItem(title: String, primaryColor: Color, secondaryColor: Color, textColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() }.background(secondaryColor.copy(alpha = 0.1f)).border(2.dp, secondaryColor, RectangleShape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "[", color = textColor, style = Y2KTypography.bodyMedium)
            Text(text = " RUN ", color = primaryColor, style = Y2KTypography.bodyMedium, modifier = Modifier.y2kBlinkEffect(800))
            Text(text = "] ", color = textColor, style = Y2KTypography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = Y2KTypography.bodyLarge, color = secondaryColor)
        }
    }
}

@Composable
fun Y2KDialogWrapper(title: String, accentColor: Color, bgColor: Color, textColor: Color, borderColor: Color, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().background(bgColor).border(3.dp, accentColor, RectangleShape).padding(2.dp).border(1.dp, borderColor, RectangleShape).padding(20.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("[$title]", color = accentColor, style = Y2KTypography.titleMedium)
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = borderColor, modifier = Modifier.clickable { onDismiss() })
                }
                HorizontalDivider(color = borderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun Y2KDialogButton(text: String, accentColor: Color, textColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clickable { onClick() }.background(accentColor.copy(alpha = 0.2f)).border(2.dp, accentColor, RectangleShape).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = textColor, style = Y2KTypography.bodyMedium)
    }
}

@Composable
fun ThemeOptionItem(name: String, colors: List<Color>, isActive: Boolean, textColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.background(if (isActive) textColor.copy(alpha = 0.2f) else Color.Transparent).border(1.dp, if (isActive) textColor else textColor.copy(alpha = 0.3f), RectangleShape).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            colors.forEach { color ->
                Box(modifier = Modifier.size(16.dp).background(color).border(1.dp, Color.White, RectangleShape))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name, color = textColor, style = Y2KTypography.bodyMedium)
        Spacer(modifier = Modifier.weight(1f))
        if (isActive) {
            Text("<", color = colors[1], style = Y2KTypography.bodyMedium, modifier = Modifier.y2kBlinkEffect(500))
        }
    }
}