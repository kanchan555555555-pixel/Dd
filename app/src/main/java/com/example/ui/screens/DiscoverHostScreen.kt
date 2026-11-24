package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.DiscoveredHost
import com.example.network.HotspotArcadeServer
import com.example.ui.theme.ArcadeMagenta
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.VoltLime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverHostScreen(
    strings: AppStrings,
    asPlayer2: Boolean,
    defaultGatewayIp: String,
    discoveredHosts: List<DiscoveredHost>,
    isScanning: Boolean,
    connectionError: String?,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    onConnectToHost: (String, Int) -> Unit,
) {
    BackHandler(onBack = onBack)

    var manualIp by remember(defaultGatewayIp) { mutableStateOf(defaultGatewayIp) }

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.discoverHostTitle,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("discover_back_button"),
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
                    .widthIn(max = 700.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MidnightSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (asPlayer2) VoltLime.copy(alpha = 0.5f) else ArcadeMagenta.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = if (asPlayer2) strings.joinP2Btn else strings.tvScreenModeBtn,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (asPlayer2) VoltLime else ArcadeMagenta,
                            )
                            Text(
                                text = if (asPlayer2) strings.p2ModeExplain else strings.tvModeExplain,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = ElectricCyan,
                                    )
                                    Text(
                                        text = strings.scanningHosts,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan,
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                OutlinedButton(
                                    onClick = onRescan,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("rescan_hosts_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = strings.rescanHosts, color = Color.White)
                                }
                            }
                        }
                    }
                }

                if (discoveredHosts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MidnightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WifiFind,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(36.dp),
                                )
                                Text(
                                    text = strings.noHostsFound,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(discoveredHosts, key = { it.ipAddress }) { host ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MidnightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Devices,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Column {
                                        Text(
                                            text = host.hostName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "${host.ipAddress}:${host.tcpPort}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricCyan,
                                        )
                                    }
                                }
                                Button(
                                    onClick = { onConnectToHost(host.ipAddress, host.tcpPort) },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("connect_discovered_host_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VoltLime,
                                        contentColor = ObsidianBg,
                                    ),
                                ) {
                                    Text(
                                        text = strings.connectBtn,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                // Manual IP Connect Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MidnightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedBorder),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it },
                                label = { Text(strings.manualIpLabel) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_ip_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = ElevatedBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                            )
                            if (connectionError != null) {
                                Text(
                                    text = connectionError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoralAlert,
                                )
                            }
                            Button(
                                onClick = {
                                    if (manualIp.isNotBlank()) {
                                        onConnectToHost(manualIp.trim(), HotspotArcadeServer.TCP_PORT)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("connect_manual_ip_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBg,
                                ),
                            ) {
                                Text(
                                    text = strings.connectBtn,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
