package com.ham.qso.ui.screens.tools

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ham.qso.domain.audio.AudioPlayerManager
import com.ham.qso.domain.audio.RecordingFileInfo
import com.ham.qso.domain.model.QCodeItem
import com.ham.qso.service.QsoAudioRecorderService
import com.ham.qso.ui.components.*
import com.ham.qso.ui.theme.GridStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showAboutDialog by remember { mutableStateOf(false) }

    // 播放器状态
    val playerManager = remember { AudioPlayerManager.getInstance(context) }
    val playerState by playerManager.playerState.collectAsState()

    // ── 1. GPS 权限管理 ──
    val locationPermissions = remember {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    var showLocationRationaleDialog by remember { mutableStateOf(false) }
    var showLocationSettingsGuideDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.requestCurrentLocation(context)
        } else {
            val activity = context as? Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
            if (!showRationale) {
                showLocationSettingsGuideDialog = true
            }
        }
    }

    fun handleRequestLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.requestCurrentLocation(context)
        } else {
            val activity = context as? Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false

            if (showRationale) {
                showLocationRationaleDialog = true
            } else {
                locationPermissionLauncher.launch(locationPermissions)
            }
        }
    }

    // ── 2. 录音权限管理 ──
    val audioPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }
    var showAudioRationaleDialog by remember { mutableStateOf(false) }
    var showAudioSettingsGuideDialog by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            QsoAudioRecorderService.start(context, "FieldQSO")
        } else {
            val activity = context as? Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
            } ?: false
            if (!showRationale) {
                showAudioSettingsGuideDialog = true
            }
        }
    }

    fun handleStartRecording() {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (hasAudio && hasNotif) {
            QsoAudioRecorderService.start(context, "FieldQSO")
        } else {
            val activity = context as? Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
            } ?: false

            if (showRationale) {
                showAudioRationaleDialog = true
            } else {
                audioPermissionLauncher.launch(audioPermissions)
            }
        }
    }

    // 进入页面时自动扫描录音文件
    LaunchedEffect(Unit) {
        viewModel.loadRecordings(context)
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissInfoMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Text(
                    text = "无线电实用工具箱",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GPS 梅登黑德网格定位、天线方位角测算、通联录音管理与常用 Q 简语速查",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── 0. 通联录音引擎与存储空间管理卡片 ──
            item {
                val recState by QsoAudioRecorderService.recordingState.collectAsState()
                var showRecordingsList by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (recState.isRecording) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (recState.isRecording) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = null,
                                    tint = if (recState.isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "通联录音与存储空间管理",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (recState.isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (recState.isRecording) "正在录音" else "待机",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (recState.isRecording) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (recState.isRecording) {
                            val totalSec = recState.durationMs / 1000
                            val mm = totalSec / 60
                            val ss = totalSec % 60
                            val timeStr = "%02d:%02d".format(mm, ss)
                            Text(
                                text = "🔴 正在录音 · 已录制: $timeStr · 标记打点: ${recState.markerCount} 次",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { QsoAudioRecorderService.mark(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("＋标记打点")
                                }
                                Button(
                                    onClick = {
                                        QsoAudioRecorderService.stop(context)
                                        viewModel.loadRecordings(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("结束录音")
                                }
                            }
                        } else {
                            Text(
                                text = "采用 AAC 48kbps 高保真人声编码（每小时仅 21.6MB）。录音期间录入 QSO 自动生成毫秒时间锚点，回听时自动前置 3 秒播放。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { handleStartRecording() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开启通联声音轨迹录音")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // ── 存储空间统计条与管理入口 ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "已占用空间: ${uiState.totalStorageFormatted}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "共 ${uiState.recordings.size} 个录音文件" + if (uiState.orphanCount > 0) " · ${uiState.orphanCount} 个无关联孤儿" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.orphanCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { viewModel.loadRecordings(context) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "刷新", modifier = Modifier.size(18.dp))
                                }

                                if (uiState.orphanCount > 0) {
                                    FilledTonalButton(
                                        onClick = { viewModel.requestCleanOrphans() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("清理孤儿(${uiState.orphanCount})", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 展开/折叠录音列表按钮
                        OutlinedButton(
                            onClick = { showRecordingsList = !showRecordingsList },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (showRecordingsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showRecordingsList) "收起录音文件详情" else "展开录音文件列表 (${uiState.recordings.size})",
                                fontSize = 13.sp
                            )
                        }

                        // 展开的录音文件列表
                        AnimatedVisibility(
                            visible = showRecordingsList,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.recordings.isEmpty()) {
                                    Text(
                                        text = "暂无本地通联录音文件",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    uiState.recordings.forEach { recItem ->
                                        RecordingItemCard(
                                            item = recItem,
                                            isPlaying = playerState.isPlaying && playerState.currentFilePath == recItem.filePath,
                                            onPlayToggle = {
                                                if (playerState.isPlaying && playerState.currentFilePath == recItem.filePath) {
                                                    playerManager.playPause()
                                                } else {
                                                    playerManager.playRecordingFile(recItem.filePath)
                                                }
                                            },
                                            onShare = { viewModel.shareRecording(context, recItem) },
                                            onDelete = { viewModel.requestDeleteRecording(recItem) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 1. GPS 网格定位换算卡片 ──
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GPS -> 梅登黑德网格换算",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (uiState.isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.grid6.isNotBlank() || uiState.grid4.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "6位高精网格: ${uiState.grid6}",
                                        style = GridStyle.copy(fontSize = 20.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "4位粗略网格: ${uiState.grid4}",
                                        style = GridStyle.copy(fontSize = 15.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { viewModel.applyGpsGridToCurrentSession() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("应用到当前会话", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "点击下方按钮获取当前 GPS 坐标，自动离线计算 Maidenhead 网格。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.locationError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.locationError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { handleRequestLocation() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLocating
                        ) {
                            Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isLocating) "正在定位中..." else "获取当前 GPS 定位")
                        }
                    }
                }
            }

            // ── 2. 天线指向角 (Bearing / Azimuth) 与大圆距离测算卡片 ──
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "天线指向角 (Bearing) & 大圆距离",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HamInputField(
                                label = "我的网格 (My Grid)",
                                value = uiState.calcFromGrid,
                                onValueChange = { viewModel.onCalcFromGridChanged(it) },
                                placeholder = "如 OL72ab",
                                textStyle = GridStyle.copy(fontSize = 15.sp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            HamInputField(
                                label = "对方网格 (Target Grid)",
                                value = uiState.calcToGrid,
                                onValueChange = { viewModel.onCalcToGridChanged(it) },
                                placeholder = "如 PM95",
                                textStyle = GridStyle.copy(fontSize = 15.sp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.isCalcValid && uiState.distanceKm != null && uiState.bearingDeg != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("大圆距离 (Great Circle)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "%.1f km".format(uiState.distanceKm),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }

                                        VerticalDivider(modifier = Modifier.height(36.dp))

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("天线方位角 (Azimuth)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "%.1f°".format(uiState.bearingDeg),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                            Text(
                                                text = getBearingCardinal(uiState.bearingDeg ?: 0.0),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 硬件电子罗盘与天线指向射线
                                    AntennaCompassView(
                                        targetAzimuth = uiState.bearingDeg,
                                        targetGrid = uiState.calcToGrid
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "请输入正确的 4 位或 6 位梅登黑德网格 (如: OL72, PM95xm)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ── 3. 常用业余无线电 Q 简语速查词典 ──
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "常用无线电 Q 简语速查词典",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.qCodeQuery,
                            onValueChange = { viewModel.onQCodeQueryChanged(it) },
                            placeholder = { Text("搜索 Q 简语，如 QTH, QSL, 73, 88...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.qCodeQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.onQCodeQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清除")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            items(uiState.qCodeResults, key = { it.code }) { item ->
                QCodeItemCard(item = item)
            }

            // ── 4. 关于 Field QSO ──
            item {
                Card(
                    onClick = { showAboutDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "关于 Field QSO (版本与开发者信息)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "v1.0.0 · 开发者信息 · 联系方式 · 开源协议",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ── 弹窗集合 ──

        // 1. 关于软件弹窗
        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }

        // 2. GPS 权限理由解释弹窗
        if (showLocationRationaleDialog) {
            LocationPermissionRationaleDialog(
                onConfirm = {
                    showLocationRationaleDialog = false
                    locationPermissionLauncher.launch(locationPermissions)
                },
                onDismiss = { showLocationRationaleDialog = false }
            )
        }

        // 3. GPS 设置引导弹窗
        if (showLocationSettingsGuideDialog) {
            PermissionSettingsGuideDialog(
                title = "需要开启精确定位权限",
                message = "Field QSO 无法获取系统定位。请在系统应用详情设置中手动允许“位置信息”权限，以便自动换算梅登黑德网格定位符。",
                onDismiss = { showLocationSettingsGuideDialog = false }
            )
        }

        // 4. 录音权限理由解释弹窗
        if (showAudioRationaleDialog) {
            AudioPermissionRationaleDialog(
                onConfirm = {
                    showAudioRationaleDialog = false
                    audioPermissionLauncher.launch(audioPermissions)
                },
                onDismiss = { showAudioRationaleDialog = false }
            )
        }

        // 5. 录音设置引导弹窗
        if (showAudioSettingsGuideDialog) {
            PermissionSettingsGuideDialog(
                title = "需要开启麦克风录音权限",
                message = "Field QSO 无法获取麦克风权限。请在系统应用详情设置中手动允许“麦克风”与“通知”权限以开启通联录音功能。",
                onDismiss = { showAudioSettingsGuideDialog = false }
            )
        }

        // 6. 删除单条录音确认弹窗
        uiState.deleteConfirmTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("确认删除录音文件？", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("文件名: ${target.fileName}")
                        Text("占用空间: ${target.formattedSize}")
                        if (target.qsoCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ 注意: 该录音文件已关联 ${target.qsoCount} 条 QSO 通联记录，删除后这些记录将无法继续回听录音。",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeleteRecording(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("确认删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                        Text("取消")
                    }
                }
            )
        }

        // 7. 清理孤儿文件确认弹窗
        if (uiState.showCleanOrphansDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCleanOrphansDialog() },
                icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("清理无关联孤儿录音", fontWeight = FontWeight.Bold) },
                text = {
                    Text("系统检测到 ${uiState.orphanCount} 个未关联任何 QSO 通联记录的废弃录音文件。清理后将彻底释放本地存储空间，此操作不可撤销。")
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmCleanOrphans(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("确认清理 (${uiState.orphanCount} 个)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCleanOrphansDialog() }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 录音条目展示卡片
 */
@Composable
fun RecordingItemCard(
    item: RecordingFileInfo,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.formattedDate} · ${item.formattedSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.isOrphan) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "无关联孤儿",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "已关联 ${item.qsoCount} 条 QSO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = onPlayToggle,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "暂停" else "试听",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "分享音频", modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除音频", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QCodeItemCard(item: QCodeItem) {
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getBearingCardinal(bearing: Double): String {
    val directions = listOf("正北 (N)", "东北 (NE)", "正东 (E)", "东南 (SE)", "正南 (S)", "西南 (SW)", "正西 (W)", "西北 (NW)", "正北 (N)")
    val index = Math.round(bearing / 45.0).toInt() % 8
    return directions[index]
}
