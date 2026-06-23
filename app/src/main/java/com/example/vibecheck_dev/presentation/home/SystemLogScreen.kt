package com.example.vibecheck_dev.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vibecheck_dev.ui.theme.Y2KTypography
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// 🔴 FUNGSI BARU BUAT CONVERT UTC KE JAM LOKAL HP
fun formatToLocalTime(utcTimeString: String): String {
    return try {
        // Parser untuk baca format mentahan dari Vercel (UTC)
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(utcTimeString)

        // Formatter untuk ngeluarin output sesuai zona waktu HP
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.timeZone = TimeZone.getDefault() // Otomatis ngikutin jam HP

        if (date != null) formatter.format(date) else utcTimeString
    } catch (e: Exception) {
        // Kalau gagal baca, balikin cara lama
        utcTimeString.replace("T", " ").substringBefore(".")
    }
}

@Composable
fun SystemLogScreen(
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeEvent.FetchLogs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Green, RectangleShape)
                .background(Color.DarkGray.copy(alpha = 0.2f))
                .padding(16.dp)
        ) {
            Text(
                text = ">>> VIBECHECK_OS_AUDIT.log",
                style = Y2KTypography.titleMedium,
                color = Color.Green
            )
            Text(
                text = "Secure Connection established. Monitoring activity data stream...",
                style = Y2KTypography.bodySmall,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoadingLogs -> {
                    Box(modifier = Modifier.fillGrid(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Green)
                    }
                }
                uiState.logsError != null -> {
                    Text(
                        text = "CRITICAL_ERR: ${uiState.logsError}",
                        color = Color.Red,
                        style = Y2KTypography.bodyMedium
                    )
                }
                uiState.userLogs.isEmpty() -> {
                    Text(
                        text = "NO_JEJAK_TERDETEKSI // Sesi lu masih suci bersih.",
                        style = Y2KTypography.bodyMedium,
                        color = Color.Yellow,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    val groupedLogs = uiState.userLogs.groupBy { log ->
                        when (log.action) {
                            "SEC_LOGIN", "SEC_LOGOUT" -> "AUTH SESSIONS"
                            "UPDATE_PROFILE" -> "USERNAME UPDATES"
                            "SEC_UPDATE" -> "PASSWORD UPDATES"
                            "SYNC_CLOUD" -> "PROFILE PIC UPDATES"
                            "SYS_P2P", "SYS_TRANSFER" -> "SYSTEM & P2P"
                            else -> "OTHER AUDITS"
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        groupedLogs.forEach { (category, logs) ->
                            item {
                                Text(
                                    text = "--- [ $category ] ---",
                                    color = Color.Yellow,
                                    style = Y2KTypography.bodyMedium,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }

                            items(logs) { log ->
                                // 🔴 PANGGIL FUNGSI CONVERTER DI SINI
                                val cleanTime = formatToLocalTime(log.timestamp)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.DarkGray, RectangleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "[$cleanTime]", color = Color.Cyan, fontSize = 11.sp, style = Y2KTypography.bodySmall)
                                        Text(text = "<${log.action}>", color = Color.Magenta, fontSize = 11.sp, style = Y2KTypography.bodySmall)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "=> ${log.details}", color = Color.Green, style = Y2KTypography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.fillGrid(): Modifier = this.fillMaxWidth().fillMaxHeight()