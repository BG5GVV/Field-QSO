package com.ham.qso.ui.screens.log

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.ui.components.*
import com.ham.qso.ui.theme.CallsignStyle
import com.ham.qso.ui.theme.GridStyle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LoggingScreen(
    viewModel: LoggingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val callsignFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ── 录音与通知权限管理 (Android 16 专属原生) ──
    val audioPermissions = remember {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    }
    var showAudioRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsGuideDialog by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            viewModel.startAudioRecording(context)
        } else {
            val activity = context as? Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
            } ?: false
            if (!showRationale) {
                showSettingsGuideDialog = true
            }
        }
    }

    fun handleStartRecording() {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasNotif = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudio && hasNotif) {
            viewModel.startAudioRecording(context)
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

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "修改更多详情",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                uiState.lastLoggedQso?.let { q ->
                    viewModel.onEditQso(q)
                }
            }
            viewModel.dismissSuccessMessage()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── 1. 置顶 UTC 时钟 + 当前架台 + 会话通联统计 (Pinned Top) ───────────
            UtcClockHeader(
                activeSessionName = uiState.currentSession?.name,
                totalQsoCount = uiState.totalQsoCount,
                uniqueCallCount = uiState.uniqueCallCount
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── 通联录音随行控制胶囊条 ──
            RecordingStatusCapsule(
                recState = recordingState,
                onStart = { handleStartRecording() },
                onStop = { viewModel.stopAudioRecording(context) },
                onMark = { viewModel.triggerAudioMark(context) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── 2. 中间自适应滚动操作区 (Scrollable Content, weight=1f) ───────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. 并列波段 (Band) + 模式 (Mode) + 频率 (MHz) 紧凑选择器
                item {
                    BandModeSelector(
                        selectedBand = uiState.band,
                        onBandSelected = { viewModel.onBandChanged(it) },
                        selectedMode = uiState.mode,
                        onModeSelected = { viewModel.onModeChanged(it) },
                        frequencyMhz = uiState.frequencyMhz,
                        onFrequencyChanged = { viewModel.onFrequencyChanged(it) }
                    )
                }

                // 2. 对方呼号输入框（位于波段与双方信号报告中间）
                item {
                    HamInputField(
                        label = "对方呼号 (Callsign)",
                        value = uiState.callsign,
                        onValueChange = { viewModel.onCallsignChanged(it) },
                        placeholder = "例如: BH4XYZ, BA1AA/7",
                        height = 48.dp,
                        isError = uiState.isDuplicate,
                        focusRequester = callsignFocusRequester,
                        textStyle = CallsignStyle.copy(
                            fontSize = 18.sp,
                            color = if (uiState.isDuplicate) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Podcasts,
                                contentDescription = "Radio Call",
                                tint = if (uiState.isDuplicate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.callsign.isNotBlank()) {
                                IconButton(onClick = { viewModel.onCallsignChanged("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (uiState.callsign.isNotBlank()) {
                                    viewModel.logQSO(context)
                                }
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )
                    )
                }

                // 3. RST 发送与接收 (双方信号报告)
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            RstPicker(
                                mode = uiState.mode,
                                rstSent = uiState.rstSent,
                                onRstSentChange = { viewModel.onRstSentChanged(it) },
                                rstRcvd = uiState.rstRcvd,
                                onRstRcvdChange = { viewModel.onRstRcvdChanged(it) }
                            )
                        }
                    }
                }

                // 高级扩展字段折叠面板 (Grid, Name, QTH, Rig, Antenna, Power, Comments)
                item {
                    OutlinedCard(
                        onClick = { viewModel.toggleAdvancedFields() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "详细信息 (对方网格/QTH/设备/备注)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = if (uiState.showAdvancedFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = uiState.showAdvancedFields,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HamInputField(
                                    label = "对方网格 (Grid)",
                                    value = uiState.theirGrid,
                                    onValueChange = { viewModel.onTheirGridChanged(it) },
                                    placeholder = "例如: PM95",
                                    textStyle = GridStyle.copy(fontSize = 15.sp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        autoCorrect = false,
                                        keyboardType = KeyboardType.Ascii,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                HamInputField(
                                    label = "对方姓名 / OP",
                                    value = uiState.theirName,
                                    onValueChange = { viewModel.onTheirNameChanged(it) },
                                    placeholder = "姓名/昵称",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HamInputField(
                                label = "对方 QTH 地名描述",
                                value = uiState.qth,
                                onValueChange = { viewModel.onQthChanged(it) },
                                placeholder = "城市/景点/村落 (便于后期换算网格)",
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HamInputField(
                                    label = "对方电台 (Rig)",
                                    value = uiState.theirRig,
                                    onValueChange = { viewModel.onTheirRigChanged(it) },
                                    placeholder = "例如: IC-705",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.weight(1f)
                                )
                                HamInputField(
                                    label = "对方天线",
                                    value = uiState.theirAntenna,
                                    onValueChange = { viewModel.onTheirAntennaChanged(it) },
                                    placeholder = "例如: EFHW / GP",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HamInputField(
                                    label = "对方功率 (W)",
                                    value = uiState.theirPowerWatts,
                                    onValueChange = { viewModel.onTheirPowerChanged(it) },
                                    placeholder = "例如: 10",
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                HamInputField(
                                    label = "海拔高度 (m)",
                                    value = uiState.altitudeMeters,
                                    onValueChange = { viewModel.onAltitudeChanged(it) },
                                    placeholder = "例如: 520",
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HamInputField(
                                label = "备注信息 (Comment)",
                                value = uiState.comment,
                                onValueChange = { viewModel.onCommentChanged(it) },
                                placeholder = "户外通联备忘信息...",
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 最近通联记录条
                if (uiState.recentQSOs.isNotEmpty()) {
                    item {
                        Text(
                            text = "最近通联记录 (Recent)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.recentQSOs, key = { it.id }) { qso ->
                                RecentQsoChip(
                                    qso = qso,
                                    onClick = { viewModel.onEditQso(qso) }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(6.dp)) }
            }

            // ── 3. 底部固定“记录通联”按钮与防重警示 (Pinned Bottom) ────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // 防重告警
                DupeAlertCard(
                    isDuplicate = uiState.isDuplicate,
                    callsign = uiState.callsign,
                    band = uiState.band
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 固定大按钮
                Button(
                    onClick = {
                        viewModel.logQSO()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    enabled = uiState.callsign.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.callsign.isBlank()) "请输入呼号" else "记录通联 (LOG QSO)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 详情/编辑弹窗
        uiState.editingQso?.let { qso ->
            EditQsoDialog(
                qso = qso,
                onDismiss = { viewModel.onEditQso(null) },
                onConfirm = { updated -> viewModel.updateQso(updated) }
            )
        }

        // 录音与通知权限理由解释弹窗
        if (showAudioRationaleDialog) {
            AudioPermissionRationaleDialog(
                onConfirm = {
                    showAudioRationaleDialog = false
                    audioPermissionLauncher.launch(audioPermissions)
                },
                onDismiss = { showAudioRationaleDialog = false }
            )
        }

        // 永久拒绝时设置引导弹窗
        if (showSettingsGuideDialog) {
            PermissionSettingsGuideDialog(
                title = "需要开启麦克风录音权限",
                message = "Field QSO 无法获取麦克风权限。请在系统应用详情设置中手动允许“麦克风”与“通知”权限，以在户外通联时记录声音轨迹。",
                onDismiss = { showSettingsGuideDialog = false }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun RecentQsoChip(
    qso: QSOEntity,
    onClick: () -> Unit
) {
    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = qso.callsign,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${qso.band.label} ${qso.mode.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${timeFormat.format(Date(qso.timestampUtc))} UTC · S:${qso.rstSent} R:${qso.rstRcvd}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (qso.audioFilePath != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "含录音",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "修改信息",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun RecordingStatusCapsule(
    recState: com.ham.qso.service.QsoAudioRecordingState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onMark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSec = recState.durationMs / 1000
    val mm = totalSec / 60
    val ss = totalSec % 60
    val timeFormatted = "%02d:%02d".format(mm, ss)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (recState.isRecording) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (recState.isRecording) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (recState.isRecording) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = null,
                    tint = if (recState.isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (recState.isRecording) {
                    Text(
                        text = "🔴 录音中 $timeFormatted · 锚点: ${recState.markerCount}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = "通联录音未开启 (回车可自动记录声音轨迹)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (recState.isRecording) {
                    FilledTonalButton(
                        onClick = onMark,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("＋打点", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("结束", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onStart,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("开启录音", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
