package com.ham.qso.ui.screens.logbook

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    viewModel: LogbookViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ADIF 文件导入选择器
    val adifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                val targetSessionId = uiState.selectedSessionId ?: uiState.sessions.firstOrNull()?.id ?: 1L
                viewModel.importAdifContent(content, targetSessionId)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    // 监听分享导出
    LaunchedEffect(uiState.exportShareContent) {
        uiState.exportShareContent?.let { (filename, content) ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_TITLE, filename)
                putExtra(Intent.EXTRA_SUBJECT, filename)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "导出与分享通联日志 ($filename)")
            context.startActivity(shareIntent)
            viewModel.clearExportShareContent()
        }
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
            Spacer(modifier = Modifier.height(8.dp))

            // 1. 顶部操作栏 (导出 ADIF / CSV / 导入 ADIF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "通联日志 (${uiState.qsoList.size} 条)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.exportAdif() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导出 ADIF", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.exportCsv() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("CSV", fontSize = 12.sp)
                    }

                    IconButton(onClick = { adifPickerLauncher.launch("*/*") }) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Import ADIF")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 搜索框
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("搜索呼号 / 网格 / 姓名 / 备注") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 过滤条 (会话选择 / 波段 / 模式)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 全部会话 Chip
                FilterChip(
                    selected = uiState.selectedSessionId == null,
                    onClick = { viewModel.onSessionFilterChanged(null) },
                    label = { Text("全部架台") }
                )
                // 各会话 Chip
                uiState.sessions.forEach { s ->
                    FilterChip(
                        selected = uiState.selectedSessionId == s.id,
                        onClick = { viewModel.onSessionFilterChanged(s.id) },
                        label = { Text(s.name) }
                    )
                }

                // 分隔
                Text("|", color = MaterialTheme.colorScheme.outline)

                // 波段过滤
                FilterChip(
                    selected = uiState.bandFilter == null,
                    onClick = { viewModel.onBandFilterChanged(null) },
                    label = { Text("全部波段") }
                )
                Band.entries.forEach { b ->
                    FilterChip(
                        selected = uiState.bandFilter == b,
                        onClick = { viewModel.onBandFilterChanged(b) },
                        label = { Text(b.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. QSO 列表
            if (uiState.qsoList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无匹配的通联日志",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.qsoList, key = { it.id }) { qso ->
                        QsoCardItem(
                            qso = qso,
                            onView = { viewModel.onViewQso(qso) },
                            onEdit = { viewModel.onEditQso(qso) },
                            onDelete = { viewModel.deleteQso(qso) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 详情/编辑弹窗
    uiState.editingQso?.let { qso ->
        EditQsoDialog(
            qso = qso,
            onDismiss = { viewModel.onEditQso(null) },
            onConfirm = { updated -> viewModel.updateQso(updated) }
        )
    }

    uiState.viewingQso?.let { qso ->
        ViewQsoDetailDialog(
            qso = qso,
            onDismiss = { viewModel.onViewQso(null) },
            onEdit = {
                viewModel.onViewQso(null)
                viewModel.onEditQso(qso)
            },
            onDelete = { viewModel.deleteQso(qso) }
        )
    }
}

@Composable
fun QsoCardItem(
    qso: QSOEntity,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateTimeFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    Card(
        onClick = onView,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = qso.callsign,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${qso.band.label} · ${qso.mode.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UTC: ${dateTimeFormat.format(Date(qso.timestampUtc))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "S:${qso.rstSent} / R:${qso.rstRcvd}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (qso.theirGrid.isNotBlank() || qso.qth.isNotBlank() || qso.theirName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        if (qso.theirGrid.isNotBlank()) append("网格: ${qso.theirGrid} ")
                        if (qso.theirName.isNotBlank()) append("OP: ${qso.theirName} ")
                        if (qso.qth.isNotBlank()) append("QTH: ${qso.qth}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ViewQsoDetailDialog(
    qso: QSOEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateTimeFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "QSO 详情: ${qso.callsign}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("时间: ${dateTimeFormat.format(Date(qso.timestampUtc))} UTC")
                Text("波段/模式: ${qso.band.label} (${qso.frequencyMhz} MHz) / ${qso.mode.label}")
                Text("信号报告 (RST): 发送 ${qso.rstSent} / 接收 ${qso.rstRcvd}")
                if (qso.theirGrid.isNotBlank()) Text("对方网格: ${qso.theirGrid}")
                if (qso.theirName.isNotBlank()) Text("对方姓名/OP: ${qso.theirName}")
                if (qso.qth.isNotBlank()) Text("对方 QTH: ${qso.qth}")
                if (qso.altitudeMeters != null) Text("海拔: ${qso.altitudeMeters} 米")
                if (qso.theirRig.isNotBlank()) Text("对方设备: ${qso.theirRig}")
                if (qso.theirAntenna.isNotBlank()) Text("对方天线: ${qso.theirAntenna}")
                if (qso.theirPowerWatts != null) Text("对方功率: ${qso.theirPowerWatts} W")
                if (qso.myCallsign.isNotBlank()) Text("本台呼号: ${qso.myCallsign} (${qso.txPowerWatts}W)")
                if (qso.myGrid.isNotBlank()) Text("本台网格: ${qso.myGrid}")
                if (qso.potaRef.isNotBlank()) Text("POTA: ${qso.potaRef}")
                if (qso.sotaRef.isNotBlank()) Text("SOTA: ${qso.sotaRef}")
                if (qso.comment.isNotBlank()) Text("备注: ${qso.comment}")
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("编辑")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )
}

@Composable
fun EditQsoDialog(
    qso: QSOEntity,
    onDismiss: () -> Unit,
    onConfirm: (QSOEntity) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var callsign by remember { mutableStateOf(qso.callsign) }
    var rstSent by remember { mutableStateOf(qso.rstSent) }
    var rstRcvd by remember { mutableStateOf(qso.rstRcvd) }
    var theirGrid by remember { mutableStateOf(qso.theirGrid) }
    var theirName by remember { mutableStateOf(qso.theirName) }
    var qth by remember { mutableStateOf(qso.qth) }
    var comment by remember { mutableStateOf(qso.comment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 QSO 记录") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = callsign,
                    onValueChange = { callsign = it.uppercase() },
                    label = { Text("呼号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rstSent,
                        onValueChange = { rstSent = it },
                        label = { Text("RST Sent") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    )
                    OutlinedTextField(
                        value = rstRcvd,
                        onValueChange = { rstRcvd = it },
                        label = { Text("RST Rcvd") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        })
                    )
                }
                OutlinedTextField(
                    value = theirGrid,
                    onValueChange = { theirGrid = it.uppercase() },
                    label = { Text("对方网格") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
                )
                OutlinedTextField(
                    value = theirName,
                    onValueChange = { theirName = it },
                    label = { Text("对方姓名") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
                )
                OutlinedTextField(
                    value = qth,
                    onValueChange = { qth = it },
                    label = { Text("对方 QTH 地名") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("备注") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        qso.copy(
                            callsign = callsign.trim(),
                            rstSent = rstSent.trim(),
                            rstRcvd = rstRcvd.trim(),
                            theirGrid = theirGrid.trim(),
                            theirName = theirName.trim(),
                            qth = qth.trim(),
                            comment = comment.trim()
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
