package com.ham.qso.ui.components

import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ham.qso.domain.sensor.CompassSensorManager
import com.ham.qso.domain.sensor.CompassState

/**
 * 地磁罗盘 8 字校准教程与实时传感器状态模态弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassCalibrationBottomSheet(
    compassState: CompassState,
    onDismissRequest: () -> Unit
) {
    val isHighAccuracy = compassState.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    val isMediumAccuracy = compassState.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
    val isLowOrUnreliable = compassState.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW

    val haptic = LocalHapticFeedback.current

    // 当精度从低变为高时，触发轻触震动反馈
    var previousAccuracy by remember { mutableIntStateOf(compassState.accuracy) }
    LaunchedEffect(compassState.accuracy) {
        if (compassState.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH && previousAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousAccuracy = compassState.accuracy
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 顶部标题栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "地磁罗盘校准教程",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "转动手机以校正天线大圆波束瞄准精度",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 8 字形 3D 轨迹翻转动画 ──
            Figure8CalibrationAnimation(
                isCalibrated = isHighAccuracy
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── 实时传感器状态指示卡片 ──
            val statusBgColor by animateColorAsState(
                targetValue = when {
                    isHighAccuracy -> Color(0xFF00E676).copy(alpha = 0.12f)
                    isMediumAccuracy -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                },
                label = "statusBgColor"
            )

            val statusStrokeColor by animateColorAsState(
                targetValue = when {
                    isHighAccuracy -> Color(0xFF00E676)
                    isMediumAccuracy -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                },
                label = "statusStrokeColor"
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = statusBgColor),
                border = BorderStroke(1.dp, statusStrokeColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    isHighAccuracy -> Icons.Default.CheckCircle
                                    isMediumAccuracy -> Icons.Default.Info
                                    else -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = when {
                                    isHighAccuracy -> Color(0xFF00C853)
                                    isMediumAccuracy -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "传感器精度: ${CompassSensorManager.getAccuracyLabel(compassState.accuracy)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    isHighAccuracy -> Color(0xFF00C853)
                                    isMediumAccuracy -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        }

                        // 实时度数
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "%03d° %s".format(compassState.azimuth.toInt(), compassState.cardinalDirection),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            isHighAccuracy -> "✓ 地磁传感器处于极佳状态，天线方位瞄准基准精确可靠。"
                            isMediumAccuracy -> "当前精度良好。若在野外复杂地形，建议继续转动以获得最高精度。"
                            else -> "检测到地磁读数可能存在偏差，请手持手机按上方动画多角度转动手腕校准。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 3 步操作教程说明 ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalibrationStepItem(
                    stepNumber = 1,
                    icon = Icons.Default.Smartphone,
                    title = "握持设备置于开阔处",
                    desc = "手持手机水平或自然抬起置于胸前，确保周围无近距离金属干扰。"
                )

                CalibrationStepItem(
                    stepNumber = 2,
                    icon = Icons.Default.Sync,
                    title = "空中画“8”字（∞）翻转手腕",
                    desc = "平滑在空中沿横向 8 字挥动 2~3 次，同时翻动左右手腕，使手机 X/Y/Z 三轴充分感知地磁矢量。"
                )

                CalibrationStepItem(
                    stepNumber = 3,
                    icon = Icons.Default.Sensors,
                    title = "规避电台与强磁干扰源",
                    desc = "远离大功率开关电源、车载磁铁吸盘底座、铁塔支架及电动机等强磁场区域。"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 底部完成按钮 ──
            Button(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHighAccuracy) "完成校准并返回" else "我知道了 / 完成",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CalibrationStepItem(
    stepNumber: Int,
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$stepNumber",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}
