package com.ham.qso.ui.screens.log

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
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.ui.components.*
import com.ham.qso.ui.theme.CallsignStyle
import com.ham.qso.ui.theme.GridStyle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LoggingScreen(
    viewModel: LoggingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val callsignFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
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

            Spacer(modifier = Modifier.height(8.dp))

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
                                    viewModel.logQSO()
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
                                RecentQsoChip(qso = qso)
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun RecentQsoChip(qso: QSOEntity) {
    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
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
    }
}
