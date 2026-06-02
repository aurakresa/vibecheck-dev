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
    guestName: String = "GUEST"
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State untuk masing-masing Dialog
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

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
                drawerContainerColor = Color(0xFF050505),
                drawerShape = RectangleShape,
                modifier = Modifier
                    .width(300.dp)
                    .border(2.dp, Color.Cyan, RectangleShape)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SYS_MENU.exe",
                        style = Y2KTypography.titleLarge,
                        color = Color.Magenta,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Cyan, thickness = 2.dp)
                    Spacer(modifier = Modifier.height(32.dp))

                    val fontResId = context.resources.getIdentifier("press_start", "font", context.packageName)
                    val pixelFontFamily = if (fontResId != 0) FontFamily(Font(fontResId)) else FontFamily.Monospace

                    // --- LOGIKA TAMPILAN SIDEBAR (GUEST vs USER ASLI) ---
                    if (isGuestMode) {
                        // TAMPILAN GUEST: Foto Dummy, Nama Guest, Tanpa Edit Profile
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(Color.Black)
                                .border(4.dp, Color.DarkGray, RectangleShape)
                                .padding(6.dp)
                                .border(2.dp, Color.Cyan, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Guest", tint = Color.DarkGray, modifier = Modifier.size(80.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = guestName.uppercase(),
                            color = Color.White,
                            fontFamily = pixelFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            DrawerMenuItem(title = "THEME.ini") {
                                coroutineScope.launch { drawerState.close() }
                                showThemeDialog = true
                            }
                            DrawerMenuItem(title = "ABOUT.txt") {
                                coroutineScope.launch { drawerState.close() }
                                showAboutDialog = true
                            }
                        }

                    } else {
                        // TAMPILAN USER ASLI: Bisa ganti foto, muncul Edit Profile
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(Color.Black)
                                .border(4.dp, Color.DarkGray, RectangleShape)
                                .padding(6.dp)
                                .border(2.dp, Color.Cyan, RectangleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            NativeProfileImage(profileImageUri) // Pakai NativeProfileImage (bisa load dari galeri)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = editUsername.uppercase(),
                            color = Color.White,
                            fontFamily = pixelFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // EDIT PROFILE HANYA MUNCUL DI SINI (USER ASLI)
                            DrawerMenuItem(title = "EDIT_PROFILE.exe") {
                                coroutineScope.launch { drawerState.close() }
                                showEditProfileDialog = true
                            }
                            DrawerMenuItem(title = "THEME.ini") {
                                coroutineScope.launch { drawerState.close() }
                                showThemeDialog = true
                            }
                            DrawerMenuItem(title = "ABOUT.txt") {
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
                            .background(Color.Red.copy(alpha = 0.2f))
                            .border(2.dp, Color.Red, RectangleShape)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("[ SYSTEM_LOGOUT ]", color = Color.White, style = Y2KTypography.bodyMedium)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("VIBECHECK_OS", style = Y2KTypography.titleMedium, color = Color.Green)
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Green)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black
                    ),
                    modifier = Modifier.drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color = Color.Green,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
                )
            },
            containerColor = Color.Black
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
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pilih perangkat ini mau jadi lensa kamera atau remote kontrol.",
                    style = Y2KTypography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))

                RoleButton(
                    title = "HOST (CAMERA)",
                    description = "Aktifin AI & Pancarin P2P",
                    pixelMatrix = PIXEL_CAMERA,
                    color = Color.Magenta,
                    onClick = onNavigateToCamera
                )

                Spacer(modifier = Modifier.height(24.dp))

                RoleButton(
                    title = "REMOTE (CTRL)",
                    description = "Konek ke Host & Jepret",
                    pixelMatrix = PIXEL_REMOTE,
                    color = Color.Cyan,
                    onClick = onNavigateToRemote
                )
            }
        }
    }

    // --- IMPLEMENTASI KUMPULAN DIALOG Y2K ---

    if (showEditProfileDialog) {
        Y2KDialogWrapper(title = "EDIT_PROFILE", borderColor = Color.Magenta, onDismiss = { showEditProfileDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.Black)
                        .border(2.dp, Color.Magenta, RectangleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    NativeProfileImage(profileImageUri)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.Magenta)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("EDIT", color = Color.White, style = Y2KTypography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = editUsername,
                    onValueChange = { editUsername = it },
                    label = { Text("> USERNAME", color = Color.Magenta, style = Y2KTypography.bodySmall) },
                    textStyle = Y2KTypography.bodyMedium.copy(color = Color.White),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Cyan,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editPassword,
                    onValueChange = { editPassword = it },
                    label = { Text("> PASSWORD", color = Color.Magenta, style = Y2KTypography.bodySmall) },
                    textStyle = Y2KTypography.bodyMedium.copy(color = Color.White),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Cyan,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Y2KDialogButton("SAVE_CHANGES", Color.Green) { showEditProfileDialog = false }
            }
        }
    }

    if (showThemeDialog) {
        Y2KDialogWrapper(title = "SELECT_THEME", borderColor = Color.Magenta, onDismiss = { showThemeDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeOptionItem("Y2K BRIGHT NEON", true)
                ThemeOptionItem("MATRIX TERMINAL", false)
                ThemeOptionItem("UBUNTU DARK", false)
                Spacer(modifier = Modifier.height(12.dp))
                Y2KDialogButton("APPLY", Color.Magenta) { showThemeDialog = false }
            }
        }
    }

    if (showAboutDialog) {
        Y2KDialogWrapper(title = "ABOUT_SYS", borderColor = Color.Green, onDismiss = { showAboutDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("VIBECHECK_OS v1.0.0", color = Color.Green, style = Y2KTypography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Engine: Jetpack Compose\nProtocol: Wi-Fi P2P Direct\nVision: ML Kit Pose Detecion", color = Color.LightGray, style = Y2KTypography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coded with blood, sweat, and pure logic.", color = Color.White, style = Y2KTypography.bodyMedium, modifier = Modifier.y2kBlinkEffect(1000))
                Spacer(modifier = Modifier.height(24.dp))
                Y2KDialogButton("CLOSE", Color.Green) { showAboutDialog = false }
            }
        }
    }

    if (showLogoutDialog) {
        Y2KDialogWrapper(title = "WARNING!", borderColor = Color.Red, onDismiss = { showLogoutDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TERMINATE SESSION?", color = Color.Red, style = Y2KTypography.titleLarge, modifier = Modifier.y2kBlinkEffect(500))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.weight(1f)) {
                        Y2KDialogButton("NO / ABORT", Color.DarkGray) { showLogoutDialog = false }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        Y2KDialogButton("YES / KILL", Color.Red) {
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
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.DarkGray.copy(alpha = 0.3f))
            .border(2.dp, color, RectangleShape)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(56.dp).aspectRatio(1f)) {
                val rows = pixelMatrix.size
                val cols = pixelMatrix.maxOf { it.length }
                val pixelWidth = size.width / cols
                val pixelHeight = size.height / rows

                for (r in 0 until rows) {
                    for (c in 0 until pixelMatrix[r].length) {
                        if (pixelMatrix[r][c] == 'X') {
                            drawRect(
                                color = color,
                                topLeft = Offset(c * pixelWidth, r * pixelHeight),
                                size = Size(pixelWidth, pixelHeight)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = Y2KTypography.bodyLarge, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = Y2KTypography.bodySmall, color = Color.White)
            }
        }
    }
}

@Composable
fun DrawerMenuItem(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .background(Color(0xFF001A1A))
            .border(2.dp, Color.Cyan, RectangleShape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "[", color = Color.White, style = Y2KTypography.bodyMedium)
            Text(text = " RUN ", color = Color.Magenta, style = Y2KTypography.bodyMedium, modifier = Modifier.y2kBlinkEffect(800))
            Text(text = "] ", color = Color.White, style = Y2KTypography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = Y2KTypography.bodyLarge, color = Color.Cyan)
        }
    }
}

@Composable
fun Y2KDialogWrapper(
    title: String,
    borderColor: Color,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .border(3.dp, borderColor, RectangleShape)
                .padding(2.dp)
                .border(1.dp, Color.DarkGray, RectangleShape)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("[$title]", color = borderColor, style = Y2KTypography.titleMedium)
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                content()
            }
        }
    }
}

@Composable
fun Y2KDialogButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(color.copy(alpha = 0.2f))
            .border(2.dp, color, RectangleShape)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if(color == Color.DarkGray) Color.LightGray else color, style = Y2KTypography.bodyMedium)
    }
}

@Composable
fun ThemeOptionItem(name: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if(isActive) Color.DarkGray else Color.Transparent).border(1.dp, if(isActive) Color.White else Color.DarkGray, RectangleShape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(16.dp).background(if(isActive) Color.Magenta else Color.Black).border(1.dp, Color.White, RectangleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, color = if(isActive) Color.White else Color.Gray, style = Y2KTypography.bodyMedium)
    }
}

@Composable
fun NativeProfileImage(uri: Uri?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmapState by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    bitmapState = bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, false).asImageBitmap()
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
            tint = Color.Magenta,
            modifier = modifier.fillMaxSize().padding(16.dp)
        )
    }
}