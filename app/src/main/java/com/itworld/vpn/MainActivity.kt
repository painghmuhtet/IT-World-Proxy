package com.itworld.vpn

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Navy = Color(0xFF07111F)
private val Panel = Color(0xFF0E1D30)
private val Accent = Color(0xFFFF8A00)
private val Green = Color(0xFF38E27D)
private val Muted = Color(0xFF8EA0B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ITWorldProxyApp() }
    }
}

@Composable
private fun ITWorldProxyApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { WireGuardController(context) }

    var connected by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("Singapore") }
    var seconds by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            scope.launch {
                connecting = true
                errorMessage = null
                try {
                    withContext(Dispatchers.IO) { controller.connect(location) }
                    connected = true
                    seconds = 0L
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Unable to connect."
                } finally {
                    connecting = false
                }
            }
        } else {
            errorMessage = "VPN permission was not granted."
        }
    }

    LaunchedEffect(connected) {
        while (connected) {
            kotlinx.coroutines.delay(1000)
            seconds++
        }
    }

    fun toggleConnection() {
        if (connecting) return

        if (connected) {
            scope.launch {
                connecting = true
                try {
                    withContext(Dispatchers.IO) { controller.disconnect() }
                    connected = false
                    seconds = 0L
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Unable to disconnect."
                } finally {
                    connecting = false
                }
            }
            return
        }

        errorMessage = null

        if (controller.isVpnPermissionRequired()) {
            val intent = android.net.VpnService.prepare(context)
            if (intent != null) {
                vpnPermissionLauncher.launch(intent)
                return
            }
        }

        scope.launch {
            connecting = true
            try {
                withContext(Dispatchers.IO) { controller.connect(location) }
                connected = true
                seconds = 0L
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to connect."
            } finally {
                connecting = false
            }
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "IT WORLD",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "PROXY",
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (connected) formatTime(seconds) else "00:00:00",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .background(Panel, RoundedCornerShape(50.dp))
                    .clickable(enabled = !connected && !connecting) {
                        location = if (location == "Singapore") "Malaysia" else "Singapore"
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (location == "Singapore") "🇸🇬  Singapore" else "🇲🇾  Malaysia",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(28.dp))

            ReactorButton(
                connected = connected,
                busy = connecting,
                onClick = ::toggleConnection
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = when {
                    connecting -> "CONNECTING..."
                    connected -> "CONNECTED"
                    else -> "TAP TO CONNECT"
                },
                color = when {
                    connected -> Green
                    connecting -> Accent
                    else -> Color.White
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("PUBLIC IP", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (connected) "Connected — IP check next" else "Not connected",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("DOWNLOAD", "—", Modifier.weight(1f))
                StatCard("PING", "—", Modifier.weight(1f))
                StatCard("UPLOAD", "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReactorButton(
    connected: Boolean,
    busy: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "reactor")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (busy) 500 else 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(230.dp)
            .scale(if (connected || busy) pulse else 1f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(230.dp)) {
            val c = center
            val base = if (connected) Green else Accent

            drawCircle(
                color = base.copy(alpha = 0.08f),
                radius = size.minDimension * 0.47f
            )
            drawCircle(
                color = base.copy(alpha = 0.22f),
                radius = size.minDimension * 0.41f,
                style = Stroke(width = 7.dp.toPx())
            )

            // Segmented outer energy ring.
            for (i in 0 until 8) {
                drawArc(
                    color = if (connected) Green else Color(0xFF5B6A7D),
                    startAngle = i * 45f + 7f,
                    sweepAngle = 28f,
                    useCenter = false,
                    style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner cyan reactor ring.
            for (i in 0 until 6) {
                drawArc(
                    color = if (connected) Green else Color(0xFF19D9FF),
                    startAngle = i * 60f + 12f,
                    sweepAngle = 38f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            drawCircle(
                color = Panel,
                radius = size.minDimension * 0.25f
            )
            drawCircle(
                color = if (connected) Green else Color(0xFF19D9FF),
                radius = size.minDimension * 0.19f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = size.minDimension * 0.09f
            )
            drawCircle(
                color = base.copy(alpha = 0.45f),
                radius = size.minDimension * 0.29f,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Text(
            text = when {
                busy -> "..."
                connected -> "OFF"
                else -> "GO"
            },
            color = Navy,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTime(total: Long): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
