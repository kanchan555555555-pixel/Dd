package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.i18n.AppStrings
import com.example.model.ConnectedClient
import com.example.model.HotspotInfo
import com.example.network.HotspotArcadeServer
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.VoltLime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectTvScreen(
    strings: AppStrings,
    hotspotInfo: HotspotInfo,
    hotspotError: String?,
    isServerRunning: Boolean,
    connectedClients: List<ConnectedClient>,
    onBack: () -> Unit,
    onRefreshIp: () -> Unit,
    onStartLocalHotspot: () -> Unit,
    onStopLocalHotspot: () -> Unit,
    onToggleServer: () -> Unit,
    onOpenGamepad: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copiedBanner by remember { mutableStateOf(false) }
    var permissionTip by remember { mutableStateOf(false) }

    val requiredHotspotPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.NEARBY_WIFI_DEVICES
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionTip = false
            onStartLocalHotspot()
        } else {
            permissionTip = true
        }
    }

    val tvBrowserUrl = hotspotInfo.localIp?.let { "http://$it:${HotspotArcadeServer.HTTP_PORT}" }

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.connectTvTitle,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("connect_tv_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.backLabel,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = ElectricCyan,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Step 1: Turn on Hotspot
                item {
                    StepCard(
                        title = strings.step1Title,
                        description = strings.step1Desc,
                        accent = ElectricCyan,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    if (hotspotInfo.localOnlyActive) {
                                        onStopLocalHotspot()
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            requiredHotspotPermission,
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            permissionTip = false
                                            onStartLocalHotspot()
                                        } else {
                                            permissionLauncher.launch(requiredHotspotPermission)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("local_hotspot_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hotspotInfo.localOnlyActive) CoralAlert else ElectricCyan,
                                    contentColor = ObsidianBg,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WifiTethering,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hotspotInfo.localOnlyActive) strings.stopLocalHotspot else strings.createLocalHotspot,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                    }.onFailure {
                                        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("system_hotspot_settings_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.openHotspotSettings,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                )
                            }
                        }

                        AnimatedVisibility(visible = hotspotInfo.localOnlyActive && hotspotInfo.localOnlySsid != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.6f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = strings.hotspotCredentials,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VoltLime,
                                    )
                                    Text(
                                        text = "${strings.wifiName}: ${hotspotInfo.localOnlySsid}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "${strings.wifiPassword}: ${hotspotInfo.localOnlyPassword ?: "-"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan,
                                    )
                                }
                            }
                        }

                        if (permissionTip || hotspotError != null) {
                            Text(
                                text = if (permissionTip) strings.permissionNeededMsg else strings.localHotspotError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SolarAmber,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }

                // Step 2: Smart TV Browser URL & IP
                item {
                    StepCard(
                        title = strings.step2Title,
                        description = strings.step2Desc,
                        accent = ArcadeMagenta,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianBg,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.55f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = strings.tvBrowserUrlLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = tvBrowserUrl ?: strings.noHotspotIpHint,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (tvBrowserUrl != null) ElectricCyan else SolarAmber,
                                    modifier = Modifier.testTag("tv_browser_url_text"),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (tvBrowserUrl != null) {
                                        Button(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(tvBrowserUrl))
                                                copiedBanner = true
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("copy_tv_url_button"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ElevatedCard,
                                                contentColor = ElectricCyan,
                                            ),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (copiedBanner) strings.urlCopied else strings.copyUrl,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            copiedBanner = false
                                            onRefreshIp()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("refresh_ip_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = strings.refreshIp,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 3: Server Status & Connected TVs + Launch Gamepad
                item {
                    StepCard(
                        title = strings.step3Title,
                        description = strings.step3Desc,
                        accent = VoltLime,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isServerRunning) VoltLime else CoralAlert)
                                )
                                Text(
                                    text = if (isServerRunning) strings.serverRunning else strings.serverStopped,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isServerRunning) VoltLime else CoralAlert,
                                )
                            }
                            OutlinedButton(
                                onClick = onToggleServer,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("toggle_server_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                            ) {
                                Text(
                                    text = if (isServerRunning) strings.stopServerBtn else strings.startServerBtn,
                                    color = Color.White,
                                )
                            }
                        }

                        // Connected Devices List
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "${strings.connectedDevices} (${connectedClients.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (connectedClients.isEmpty()) {
                                    Text(
                                        text = strings.waitingForTv,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SolarAmber,
                                    )
                                } else {
                                    Text(
                                        text = strings.tvConnectedSuccess,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VoltLime,
                                    )
                                    connectedClients.forEach { client ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (client.playerSlot == 2) Icons.Filled.Gamepad else Icons.Filled.Tv,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                text = "${client.name} (${client.kind})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = VoltLime,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenGamepad,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("open_gamepad_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VoltLime,
                                contentColor = ObsidianBg,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gamepad,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.openGamepad,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    title: String,
    description: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MidnightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
