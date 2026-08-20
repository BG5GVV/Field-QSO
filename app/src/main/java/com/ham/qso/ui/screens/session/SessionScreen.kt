package com.ham.qso.ui.screens.session

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.ham.qso.data.model.SessionEntity
import com.ham.qso.ui.theme.GridStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

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
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "架台与活动会话管理",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "管理每次户外通联出台属性，自动继承呼号、网格与设备参数",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无架台会话，点击右下方按钮创建",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.sessions, key = { it.session.id }) { item ->
                        SessionCardItem(
                            item = item,
                            isActive = uiState.activeSession?.id == item.session.id,
                            onActivate = { viewModel.activateSession(item.session.id) },
                            onEdit = { viewModel.openEditDialog(item.session) },
                            onDelete = { viewModel.deleteSession(item.session) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { viewModel.openCreateDialog() },
            icon = { Icon(Icons.Default.Add, contentDescription = "New Session") },
            text = { Text("新建架台") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 新建架台弹窗
    if (uiState.showCreateDialog) {
        CreateOrEditSessionDialog(
            session = null,
            onDismiss = { viewModel.closeCreateDialog() },
            onSave = { name, call, grid, qth, pwr, rig, ant, pota, sota, wwff, isCurrent ->
                viewModel.saveNewSession(name, call, grid, qth, pwr, rig, ant, pota, sota, wwff, isCurrent)
            }
        )
    }

    // 编辑架台弹窗
    uiState.editingSession?.let { session ->
        CreateOrEditSessionDialog(
            session = session,
            onDismiss = { viewModel.closeEditDialog() },
            onSave = { name, call, grid, qth, pwr, rig, ant, pota, sota, wwff, _ ->
                viewModel.updateSession(
                    session.copy(
                        name = name,
                        myCallsign = call,
                        myGrid = grid,
                        myQth = qth,
                        txPowerWatts = pwr,
                        rigModel = rig,
                        antenna = ant,
                        potaRef = pota,
                        sotaRef = sota,
                        wwffRef = wwff
                    )
                )
            }
        )
    }
}

@Composable
fun SessionCardItem(
    item: SessionWithStats,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val session = item.session

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = if (isActive) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ) else CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "当前架台",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 属性标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (session.myCallsign.isNotBlank()) {
                    Text(
                        text = "本台: ${session.myCallsign}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (session.myGrid.isNotBlank()) {
                    Text(
                        text = "网格: ${session.myGrid}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "功率: ${session.txPowerWatts}W",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (session.rigModel.isNotBlank() || session.antenna.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        if (session.rigModel.isNotBlank()) "电台: ${session.rigModel}" else null,
                        if (session.antenna.isNotBlank()) "天线: ${session.antenna}" else null
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session.potaRef.isNotBlank() || session.sotaRef.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (session.potaRef.isNotBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("POTA: ${session.potaRef}", fontSize = 11.sp) }
                        )
                    }
                    if (session.sotaRef.isNotBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("SOTA: ${session.sotaRef}", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // 底部统计与激活按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总通联: ${item.qsoCount} 条  |  独立呼号: ${item.uniqueCallCount} 个",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!isActive) {
                    FilledTonalButton(
                        onClick = onActivate,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("设为当前", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateOrEditSessionDialog(
    session: SessionEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, call: String, grid: String, qth: String, pwr: Int, rig: String, ant: String, pota: String, sota: String, wwff: String, isCurrent: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(session?.name ?: "") }
    var myCallsign by remember { mutableStateOf(session?.myCallsign ?: "") }
    var myGrid by remember { mutableStateOf(session?.myGrid ?: "") }
    var myQth by remember { mutableStateOf(session?.myQth ?: "") }
    var txPowerWatts by remember { mutableStateOf(session?.txPowerWatts?.toString() ?: "100") }
    var rigModel by remember { mutableStateOf(session?.rigModel ?: "") }
    var antenna by remember { mutableStateOf(session?.antenna ?: "") }
    var potaRef by remember { mutableStateOf(session?.potaRef ?: "") }
    var sotaRef by remember { mutableStateOf(session?.sotaRef ?: "") }
    var wwffRef by remember { mutableStateOf(session?.wwffRef ?: "") }
    var setAsCurrent by remember { mutableStateOf(session?.isCurrent ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (session == null) "新建架台会话" else "编辑架台会话") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("架台活动名称 *") },
                    placeholder = { Text("例如: 莲花山 POTA CN-0123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = myCallsign,
                        onValueChange = { myCallsign = it.uppercase() },
                        label = { Text("我的呼号") },
                        placeholder = { Text("例如: BH4XYZ") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                    OutlinedTextField(
                        value = myGrid,
                        onValueChange = { myGrid = it.uppercase() },
                        label = { Text("我的网格") },
                        placeholder = { Text("例如: OL72ab") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = GridStyle,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                }

                OutlinedTextField(
                    value = myQth,
                    onValueChange = { myQth = it },
                    label = { Text("我的 QTH 地点描述") },
                    placeholder = { Text("例如: 深圳市福田区莲花山山顶") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = txPowerWatts,
                        onValueChange = { txPowerWatts = it },
                        label = { Text("发射功率 (W)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = rigModel,
                        onValueChange = { rigModel = it },
                        label = { Text("电台型号") },
                        placeholder = { Text("IC-705 / FT-891") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = antenna,
                    onValueChange = { antenna = it },
                    label = { Text("架设天线") },
                    placeholder = { Text("PAC-12 / 端馈天线 / 正V") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = potaRef,
                        onValueChange = { potaRef = it.uppercase() },
                        label = { Text("POTA 编号") },
                        placeholder = { Text("CN-0123") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                    OutlinedTextField(
                        value = sotaRef,
                        onValueChange = { sotaRef = it.uppercase() },
                        label = { Text("SOTA 编号") },
                        placeholder = { Text("BV/NW-001") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                }

                OutlinedTextField(
                    value = wwffRef,
                    onValueChange = { wwffRef = it.uppercase() },
                    label = { Text("WWFF/BOTA 编号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )

                if (session == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = setAsCurrent,
                            onCheckedChange = { setAsCurrent = it }
                        )
                        Text(
                            text = "创建后直接设为当前活动架台",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pwr = txPowerWatts.toIntOrNull() ?: 100
                    onSave(name, myCallsign, myGrid, myQth, pwr, rigModel, antenna, potaRef, sotaRef, wwffRef, setAsCurrent)
                },
                enabled = name.isNotBlank()
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
