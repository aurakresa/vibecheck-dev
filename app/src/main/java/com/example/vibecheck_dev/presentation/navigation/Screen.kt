package com.example.vibecheck_dev.presentation.navigation

// Matriks Piksel 10x10 untuk menggambar ikon retro tanpa file eksternal
object PixelIcons {
    val HOME = listOf(
        "    XX    ",
        "   XXXX   ",
        "  XX  XX  ",
        " XX    XX ",
        "XX      XX",
        "X XXXXXX X",
        "X X    X X",
        "X X    X X",
        "X XXXXXX X",
        "          "
    )

    val CAMERA = listOf(
        "          ",
        "  XXXXXX  ",
        " XX    XX ",
        " X  XX  X ",
        " X X  X X ",
        " X X  X X ",
        " X  XX  X ",
        " XX    XX ",
        "  XXXXXX  ",
        "          "
    )

    val REMOTE = listOf(
        "          ",
        "          ",
        " XXXXXXXX ",
        "XX  XX  XX",
        "X X XX X X",
        "XX  XX  XX",
        " XXXXXXXX ",
        "          ",
        "          ",
        "          "
    )

    val STUDIO = listOf(
        "    XX    ",
        "   XXXX   ",
        "  XX  XX  ",
        " XX    XX ",
        "XX  XX  XX",
        " XX    XX ",
        "  XX  XX  ",
        "   XXXX   ",
        "    XX    ",
        "          "
    )

    val PURIKURA = listOf(
        " XXXXXXXX ",
        " XX    XX ",
        " XX    XX ",
        " XXXXXXXX ",
        " XX    XX ",
        " XX    XX ",
        " XXXXXXXX ",
        " XX    XX ",
        " XX    XX ",
        " XXXXXXXX "
    )
}

// FIX: Tambahkan List<String>? = null agar layar yang tidak butuh ikon tidak error
sealed class Screen(val route: String, val title: String, val pixelMatrix: List<String>? = null) {

    // --- Layar Utama (Punya Ikon Pixel untuk Bottom Nav) ---
    object Home : Screen("home", "HOME", PixelIcons.HOME)
    object Camera : Screen("camera", "CAM.exe", PixelIcons.CAMERA)
    object Remote : Screen("remote", "CTRL.bat", PixelIcons.REMOTE)
    object Studio : Screen("studio", "STUDIO", PixelIcons.STUDIO)
    object Purikura : Screen("purikura", "PURIKURA", PixelIcons.PURIKURA)

    // --- Layar Auth & Setup (Tidak butuh ikon, cukup kosongin parameter ketiganya) ---
    object Onboarding : Screen("onboarding_screen", "ONBOARDING")
    object Auth : Screen("auth_screen", "AUTH")
    object Login : Screen("login_screen", "LOGIN")
    object ProfileSetup : Screen("profile_setup_screen", "PROFILE")
    object Permission : Screen("permission_screen", "PERMISSION")
    // ... rute lainnya ...
    // TAMBAHIN INI BRO:
    object Splash : Screen("splash_screen", "BOOT")
    object SystemLog : Screen("system_log_screen", "SYS_LOG")
}