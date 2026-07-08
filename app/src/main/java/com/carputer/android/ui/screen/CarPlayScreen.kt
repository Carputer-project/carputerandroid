package com.carputer.android.ui.screen

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.carputer.android.service.CameraService
import com.carputer.android.ui.PageNavController
import com.carputer.android.ui.theme.CarputerColors
import com.carputer.android.ui.theme.StatusGreen

@Composable
fun CarPlayScreen(
    connected: Boolean,
    deviceName: String,
    audioSource: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onTap: () -> Unit,
    colors: CarputerColors,
    cameraService: CameraService? = null,
    pageNavController: PageNavController? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusIndex = remember { mutableStateOf(-1) }

    SideEffect {
        pageNavController?.let { ctrl ->
            ctrl.navigateLeft = {
                if (focusIndex.value < 0) focusIndex.value = 0
                else if (focusIndex.value > 0) focusIndex.value--
            }
            ctrl.navigateRight = {
                if (focusIndex.value < 0) focusIndex.value = 0
            }
            ctrl.navigateUp = {
                if (focusIndex.value > 0) {
                    focusIndex.value--
                }
                true
            }
            ctrl.navigateDown = {
                if (focusIndex.value < 0) {
                    focusIndex.value = 0
                    true
                } else {
                    false
                }
            }
            ctrl.activateFocus = {
                if (focusIndex.value >= 0) {
                    if (connected) onDisconnect() else onConnect()
                }
            }
            ctrl.handleEscape = {
                focusIndex.value = -1
            }
            ctrl.focusIndex.intValue = focusIndex.value
            ctrl.focusActionName.value = if (focusIndex.value == 0) "CONNECT" else ""
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.bgDark)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Box(
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clip(RoundedCornerShape(8.dp)).background(colors.bgCard)
                    .border(1.dp, colors.carBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("REAR CAMERA / CARPLAY", color = colors.carBlue,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Camera preview area with TextureView
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)).background(Color.Black)
                    .border(2.dp, colors.bgPanel, RoundedCornerShape(12.dp))
            ) {
                if (connected && cameraService != null) {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(
                                        surface: SurfaceTexture, width: Int, height: Int
                                    ) {
                                        val surf = Surface(surface)
                                        cameraService.setPreviewSurface(surf, Size(width, height))
                                        cameraService.startStream()
                                    }

                                    override fun onSurfaceTextureSizeChanged(
                                        surface: SurfaceTexture, width: Int, height: Int
                                    ) {}

                                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                        cameraService.setPreviewSurface(null)
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Status overlay
                    Box(
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xAA000000))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("$deviceName \u2022 LIVE",
                            color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("\uD83D\uDCF9", fontSize = 64.sp)
                        Text("No signal", color = colors.textSecondary, fontSize = 20.sp)
                        Text("Tap to start camera", color = colors.textSecondary, fontSize = 14.sp)
                    }
                }
            }

            // Connection status bar
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (connected) StatusGreen else colors.textSecondary))
                        Text(
                            if (connected) "Connected: $deviceName" else "Not connected",
                            color = if (connected) StatusGreen else colors.textSecondary,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier.size(width = 80.dp, height = 32.dp)
                            .clip(RoundedCornerShape(6.dp)).background(colors.bgDark)
                            .clickable { if (connected) onDisconnect() else onConnect() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (connected) "DISCONNECT" else "CONNECT",
                            color = colors.carBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
