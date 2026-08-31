package com.ham.qso.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.domain.utils.MaidenheadUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * 全功能 QSO 通联记录编辑对话框
 * 支持修改呼号、RST、时间、波段、模式、频率、对方网格/姓名/QTH/设备/天线/功率/海拔、我方信息及 POTA/SOTA/备注等全部字段
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQsoDialog(
    qso: QSOEntity,
    onDismiss: () -> Unit,
    onConfirm: (QSOEntity) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val utcDateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    // ── 基础信息 ──
    var callsign by remember { mutableStateOf(qso.callsign) }
    var rstSent by remember { mutableStateOf(qso.rstSent) }
    var rstRcvd by remember { mutableStateOf(qso.rstRcvd) }
    var timeUtcText by remember { mutableStateOf(utcDateFormat.format(Date(qso.timestampUtc))) }

    // ── 频段与模式 ──
    var band by remember { mutableStateOf(qso.band) }
    var mode by remember { mutableStateOf(qso.mode) }
    var freqMhzText by remember { mutableStateOf(qso.frequencyMhz.toString()) }

    // ── 对方信息 ──
    var theirGrid by remember { mutableStateOf(qso.theirGrid) }
    var theirName by remember { mutableStateOf(qso.theirName) }
    var qth by remember { mutableStateOf(qso.qth) }
    var theirRig by remember { mutableStateOf(qso.theirRig) }
    var theirAntenna by remember { mutableStateOf(qso.theirAntenna) }
    var theirPowerWattsText by remember { mutableStateOf(qso.theirPowerWatts?.toString() ?: "") }
    var altitudeMetersText by remember { mutableStateOf(qso.altitudeMeters?.toString() ?: "") }

    // ── 本台与活动 ──
    var myCallsign by remember { mutableStateOf(qso.myCallsign) }
    var myGrid by remember { mutableStateOf(qso.myGrid) }
    var txPowerWattsText by remember { mutableStateOf(qso.txPowerWatts.toString()) }
    var potaRef by remember { mutableStateOf(qso.potaRef) }
    var sotaRef by remember { mutableStateOf(qso.sotaRef) }

    // ── 备注 ──
    var comment by remember { mutableStateOf(qso.comment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "编辑通联记录",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = callsign.ifBlank { "QSO" },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── 1. 基础信息与双方 RST ──
                SectionTitle(title = "基础信息 & 信号报告", icon = Icons.Default.Podcasts)

                OutlinedTextField(
                    value = callsign,
                    onValueChange = { callsign = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '/' } },
                    label = { Text("对方呼号 *") },
                    placeholder = { Text("例如: BA1AA, BH4XYZ/7") },
                    singleLine = true,
                    isError = callsign.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // RST 双方报告
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rstSent,
                        onValueChange = { rstSent = it },
                        label = { Text("我发 RST") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = rstRcvd,
                        onValueChange = { rstRcvd = it },
                        label = { Text("对方 RST") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                // 快捷 RST 填入按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf("59", "599", "-10", "-05", "57", "55", "579")
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = {
                                rstSent = preset
                                rstRcvd = preset
                            },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                // 通联时间 (UTC)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = timeUtcText,
                        onValueChange = { timeUtcText = it },
                        label = { Text("通联时间 (UTC)") },
                        placeholder = { Text("yyyy-MM-dd HH:mm:ss") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedButton(
                        onClick = {
                            timeUtcText = utcDateFormat.format(Date())
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("当前时间", fontSize = 12.sp)
                    }
                }

                // ── 2. 波段、模式与频率 ──
                SectionTitle(title = "波段 · 模式 · 频率", icon = Icons.Default.Tune)

                // 波段选择
                Text("波段 (Band)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Band.entries.forEach { b ->
                        FilterChip(
                            selected = band == b,
                            onClick = {
                                band = b
                                freqMhzText = "%.3f".format(b.frequencyMhz)
                            },
                            label = { Text(b.label, fontSize = 11.sp) }
                        )
                    }
                }

                // 模式选择
                Text("模式 (Mode)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Mode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(m.label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = freqMhzText,
                    onValueChange = { freqMhzText = it },
                    label = { Text("工作频率 (MHz)") },
                    placeholder = { Text("例如: 7.050, 14.074") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── 3. 对方电台与地理位置 ──
                SectionTitle(title = "对方台站 & 位置信息", icon = Icons.Default.LocationOn)

                OutlinedTextField(
                    value = theirGrid,
                    onValueChange = { theirGrid = it.uppercase().trim() },
                    label = { Text("对方梅登黑德网格 (Grid)") },
                    placeholder = { Text("例如: PM95, OL72ab") },
                    singleLine = true,
                    trailingIcon = {
                        if (theirGrid.isNotBlank()) {
                            val isValid = MaidenheadUtils.isValidGrid(theirGrid)
                            Icon(
                                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isValid) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = theirName,
                        onValueChange = { theirName = it },
                        label = { Text("对方姓名 / OP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = qth,
                        onValueChange = { qth = it },
                        label = { Text("对方 QTH 地名") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = theirRig,
                        onValueChange = { theirRig = it },
                        label = { Text("对方电台设备") },
                        placeholder = { Text("如: IC-705") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = theirAntenna,
                        onValueChange = { theirAntenna = it },
                        label = { Text("对方天线") },
                        placeholder = { Text("如: PAC-12") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = theirPowerWattsText,
                        onValueChange = { theirPowerWattsText = it },
                        label = { Text("对方功率 (W)") },
                        placeholder = { Text("如: 10") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = altitudeMetersText,
                        onValueChange = { altitudeMetersText = it },
                        label = { Text("海拔高度 (m)") },
                        placeholder = { Text("如: 380") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── 4. 我方信息与活动关联 ──
                SectionTitle(title = "本台信息 & POTA / SOTA", icon = Icons.Default.Terrain)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = myCallsign,
                        onValueChange = { myCallsign = it.uppercase() },
                        label = { Text("本台呼号") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedTextField(
                        value = myGrid,
                        onValueChange = { myGrid = it.uppercase().trim() },
                        label = { Text("本台网格") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = txPowerWattsText,
                        onValueChange = { txPowerWattsText = it },
                        label = { Text("本台功率 (W)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = potaRef,
                        onValueChange = { potaRef = it.uppercase().trim() },
                        label = { Text("POTA 编号") },
                        placeholder = { Text("如: CN-0001") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                OutlinedTextField(
                    value = sotaRef,
                    onValueChange = { sotaRef = it.uppercase().trim() },
                    label = { Text("SOTA 编号") },
                    placeholder = { Text("如: BV/TP-001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    )
                )

                // ── 5. 通联备注 ──
                SectionTitle(title = "备注备忘 (Comment)", icon = Icons.Default.Comment)

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("通联备忘 / QTH 临时记录") },
                    placeholder = { Text("输入任何需要备忘的信息...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
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
                    val finalCall = callsign.trim().uppercase()
                    if (finalCall.isBlank()) return@Button

                    val parsedTimestamp = try {
                        utcDateFormat.parse(timeUtcText.trim())?.time ?: qso.timestampUtc
                    } catch (e: Exception) {
                        qso.timestampUtc
                    }

                    val parsedFreq = freqMhzText.trim().toDoubleOrNull() ?: band.frequencyMhz
                    val parsedTheirPower = theirPowerWattsText.trim().toIntOrNull()
                    val parsedAltitude = altitudeMetersText.trim().toIntOrNull()
                    val parsedTxPower = txPowerWattsText.trim().toIntOrNull() ?: qso.txPowerWatts

                    val updated = qso.copy(
                        callsign = finalCall,
                        rstSent = rstSent.trim().ifBlank { "59" },
                        rstRcvd = rstRcvd.trim().ifBlank { "59" },
                        timestampUtc = parsedTimestamp,
                        band = band,
                        mode = mode,
                        frequencyMhz = parsedFreq,
                        theirGrid = theirGrid.trim(),
                        theirName = theirName.trim(),
                        qth = qth.trim(),
                        theirRig = theirRig.trim(),
                        theirAntenna = theirAntenna.trim(),
                        theirPowerWatts = parsedTheirPower,
                        altitudeMeters = parsedAltitude,
                        myCallsign = myCallsign.trim(),
                        myGrid = myGrid.trim(),
                        txPowerWatts = parsedTxPower,
                        potaRef = potaRef.trim(),
                        sotaRef = sotaRef.trim(),
                        comment = comment.trim()
                    )

                    onConfirm(updated)
                },
                enabled = callsign.isNotBlank()
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("保存更新", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
