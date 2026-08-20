package com.ham.qso.ui.screens.history

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.domain.utils.MaidenheadUtils
import com.ham.qso.ui.components.QSOCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    // ADIF 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importAdifFromUri(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "通联历史 (${state.filteredQSOs.size})",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (state.currentSession != null) {
                            Text(
                                text = "活动: ${state.currentSession?.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "操作菜单")
                    }

                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("分享 ADIF 日志 (.adi)") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                val file = viewModel.exportAdif(context)
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "分享 ADIF 日志文件"))
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("分享 CSV 表格 (.csv)") },
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                val file = viewModel.exportCsv(context)
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/comma-separated-values"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "分享 CSV 表格文件"))
                                }
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("导入 ADIF 记录") },
                            leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // ── 提示信息 ──
            if (state.exportSuccessMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.exportSuccessMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = { viewModel.clearMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── 搜索框 ──
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("搜索呼号、网格、QTH 或备注...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 筛选过滤 Chips ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.filterGridEmptyOnly,
                    onClick = { viewModel.onToggleFilterGridEmptyOnly() },
                    label = { Text("仅未定网格", fontSize = 12.sp) },
                    leadingIcon = {
                        if (state.filterGridEmptyOnly) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                )

                FilterChip(
                    selected = state.filterBand == null,
                    onClick = { viewModel.onFilterBandChange(null) },
                    label = { Text("全部频段", fontSize = 12.sp) }
                )

                Band.values().forEach { band ->
                    FilterChip(
                        selected = state.filterBand == band,
                        onClick = {
                            viewModel.onFilterBandChange(if (state.filterBand == band) null else band)
                        },
                        label = { Text(band.label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 通联列表 ──
            if (state.filteredQSOs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ListAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.searchQuery.isNotBlank() || state.filterBand != null || state.filterGridEmptyOnly)
                                "没有匹配的通联记录"
                            else
                                "当前会话暂无通联记录，请前往录入",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.filteredQSOs, key = { it.id }) { qso ->
                        QSOCard(
                            qso = qso,
                            onDelete = { viewModel.deleteQSO(qso) },
                            onEdit = { viewModel.openEditDialog(qso) }
                        )
                    }
                }
            }
        }
    }

    // ── 编辑 QSO / 网格换算 弹窗对话框 ──
    if (state.editingQSO != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissEditDialog() },
            title = {
                Text(
                    text = "编辑通联记录 (${state.editCallsign})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 呼号与 RST
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.editCallsign,
                            onValueChange = { viewModel.onEditCallsignChange(it) },
                            label = { Text("呼号") },
                            modifier = Modifier.weight(1.4f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.editRstSent,
                            onValueChange = { viewModel.onEditRstSentChange(it) },
                            label = { Text("Sent") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.editRstRcvd,
                            onValueChange = { viewModel.onEditRstRcvdChange(it) },
                            label = { Text("Rcvd") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // 频段与模式下拉/Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Band.values().forEach { band ->
                            FilterChip(
                                selected = state.editBand == band,
                                onClick = { viewModel.onEditBandChange(band) },
                                label = { Text(band.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // 对方网格（核心可后期编辑换算字段）
                    OutlinedTextField(
                        value = state.editTheirGrid,
                        onValueChange = { viewModel.onEditTheirGridChange(it) },
                        label = { Text("对方梅登黑德网格 (Grid)") },
                        placeholder = { Text("如: OL72, PM95, BL11...") },
                        trailingIcon = {
                            if (state.editTheirGrid.isNotBlank()) {
                                val isValid = MaidenheadUtils.isValidGrid(state.editTheirGrid)
                                if (isValid) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "网格有效", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.Error, contentDescription = "格式不符", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 对方姓名与 QTH 地点
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.editTheirName,
                            onValueChange = { viewModel.onEditTheirNameChange(it) },
                            label = { Text("对方姓名") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.editQth,
                            onValueChange = { viewModel.onEditQthChange(it) },
                            label = { Text("QTH 地点") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                    }

                    // 海拔高度与发射功率
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.editAltitudeMeters,
                            onValueChange = { viewModel.onEditAltitudeChange(it) },
                            label = { Text("海拔 (m)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = state.editTheirPowerWatts,
                            onValueChange = { viewModel.onEditTheirPowerChange(it) },
                            label = { Text("功率 (W)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // 对方设备与天线
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.editTheirRig,
                            onValueChange = { viewModel.onEditTheirRigChange(it) },
                            label = { Text("对方设备") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.editTheirAntenna,
                            onValueChange = { viewModel.onEditTheirAntennaChange(it) },
                            label = { Text("对方天线") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // 备注
                    OutlinedTextField(
                        value = state.editComment,
                        onValueChange = { viewModel.onEditCommentChange(it) },
                        label = { Text("备注 (Comment)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.saveEditedQSO() }) {
                    Text("保存更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEditDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}
