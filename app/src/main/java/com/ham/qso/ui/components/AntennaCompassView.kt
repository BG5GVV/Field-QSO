package com.ham.qso.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ham.qso.domain.sensor.CompassState
import com.ham.qso.domain.sensor.rememberCompassState
import kotlin.math.*

/**
 * 业余无线电天线大圆方位角瞄准电子罗盘组件
 *
 * @param targetAzimuth 理论计算的天线指向大圆角 (0.0° ~ 360.0°)，为 null 时仅展示普通指南针
 * @param targetGrid 目标电台网格代码 (如 "PM95", "OL72ab")
 */
@Composable
fun AntennaCompassView(
    targetAzimuth: Double?,
    targetGrid: String? = null,
    modifier: Modifier = Modifier
) {
    val compassState = rememberCompassState()
    val haptic = LocalHapticFeedback.current

    // 计算设备当前朝向与天线目标角度的相对差角 (-180° ~ +180°)
    val deltaAngle = remember(compassState.azimuth, targetAzimuth) {
        if (targetAzimuth == null) null
        else {
            val rawDiff = ((targetAzimuth.toFloat() - compassState.azimuth + 540f) % 360f) - 180f
            rawDiff
        }
    }

    // 判定是否精确对准 (偏差在 ±3.5° 以内)
    val isAligned = deltaAngle != null && abs(deltaAngle) <= 3.5f

    // 触发对齐瞬间的触觉反馈
    var lastAlignedState by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !lastAlignedState) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastAlignedState = isAligned
    }

    val alignedColor = Color(0xFF00E676)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val ringBorderColor by animateColorAsState(
        targetValue = if (isAligned) alignedColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        animationSpec = tween(300),
        label = "ringBorderColor"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 顶部标题与状态栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = if (isAligned) alignedColor else primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "天线波束方位角瞄准罗盘",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (!compassState.isSupported) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SensorsOff, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("无地磁传感器 (静态图示)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                } else if (targetAzimuth != null) {
                    Surface(
                        color = if (isAligned) alignedColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isAligned) "★ 已对准目标波束 ★" else "目标指向: %.1f°".format(targetAzimuth),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAligned) alignedColor else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 罗盘盘面 ──
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .border(2.dp, ringBorderColor, CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 绘制 360° 刻度盘与方位
                CompassDialCanvas(
                    deviceAzimuth = if (compassState.isSupported) compassState.azimuth else 0f,
                    targetAzimuth = targetAzimuth?.toFloat(),
                    isAligned = isAligned,
                    modifier = Modifier.fillMaxSize()
                )

                // 顶部 12 点钟位置固定参考红色三角游标 (表示手机屏幕顶端正前方)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    val w = size.width
                    val arrowPath = Path().apply {
                        moveTo(w / 2f, 0f)
                        lineTo(w / 2f - 7f, 16f)
                        lineTo(w / 2f + 7f, 16f)
                        close()
                    }
                    drawPath(arrowPath, color = Color(0xFFFF5252))
                }

                // 中心数显仪表盘 (当前朝向与主方位)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {
                    if (compassState.isSupported) {
                        Text(
                            text = "%03d°".format(compassState.azimuth.toInt()),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isAligned) alignedColor else primaryColor
                            )
                        )
                        Text(
                            text = compassState.cardinalDirection,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceVariant
                            ),
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "正北基准",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor
                        )
                        Text(
                            text = "静态示意",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 底部对准指导建议栏 ──
            if (targetAzimuth != null) {
                Surface(
                    color = if (isAligned) alignedColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isAligned) alignedColor else Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Navigation,
                                contentDescription = null,
                                tint = if (isAligned) alignedColor else primaryColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(if (deltaAngle != null && !isAligned) deltaAngle else 0f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isAligned) "天线已精准对准目标大圆波束！"
                                    else if (deltaAngle != null) {
                                        if (deltaAngle > 0) "向右旋转身体 / 天线 %.0f°".format(deltaAngle)
                                        else "向左旋转身体 / 天线 %.0f°".format(abs(deltaAngle))
                                    } else "请旋转天线至指定角度",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isAligned) alignedColor else onSurface
                                )
                                if (!targetGrid.isNullOrBlank()) {
                                    Text(
                                        text = "目标网格: $targetGrid (理论天线方位: %.1f°)".format(targetAzimuth),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (isAligned) {
                            Text(
                                text = "±0°",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = alignedColor,
                                fontSize = 14.sp
                            )
                        } else if (deltaAngle != null) {
                            Text(
                                text = "%+.0f°".format(deltaAngle),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = primaryColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "在上方输入两地网格即可自动在此显示天线波束指向射线与瞄准提示",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 罗盘刻度盘与目标射线 Canvas 绘制逻辑
 */
@Composable
private fun CompassDialCanvas(
    deviceAzimuth: Float,
    targetAzimuth: Float?,
    isAligned: Boolean,
    modifier: Modifier = Modifier
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val alignedColor = Color(0xFF00E676)
    val targetBeamColor = if (isAligned) alignedColor else Color(0xFFFFB300)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // 旋转刻度盘（使正北刻度跟随设备偏转：手机朝向 12 点钟位置即为当前 heading）
        rotate(-deviceAzimuth, pivot = center) {
            val textPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 28f
                color = onSurfaceColor.toArgb()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val northPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 32f
                color = Color(0xFFFF3D00).toArgb() // 正北鲜红
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val numPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 20f
                color = outlineColor.toArgb()
            }

            // 绘制 360° 刻度 (每 5° 一个短刻度，每 15° 一个中刻度，每 30° 一个主刻度与数字)
            for (angle in 0 until 360 step 5) {
                val angleRad = Math.toRadians(angle.toDouble())
                val isMajor = angle % 30 == 0
                val isMedium = angle % 15 == 0

                val tickLen = when {
                    isMajor -> 18f
                    isMedium -> 12f
                    else -> 7f
                }

                val tickColor = when {
                    angle == 0 -> Color(0xFFFF3D00)
                    isMajor -> onSurfaceColor.copy(alpha = 0.8f)
                    else -> outlineColor.copy(alpha = 0.4f)
                }

                val startX = center.x + (radius - 8f - tickLen) * sin(angleRad).toFloat()
                val startY = center.y - (radius - 8f - tickLen) * cos(angleRad).toFloat()
                val endX = center.x + (radius - 8f) * sin(angleRad).toFloat()
                val endY = center.y - (radius - 8f) * cos(angleRad).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 3f else 1.5f
                )

                // 主刻度绘制方位字符 / 度数文字
                if (isMajor) {
                    val textRadius = radius - 38f
                    val textX = center.x + textRadius * sin(angleRad).toFloat()
                    val textY = center.y - textRadius * cos(angleRad).toFloat() + 10f

                    when (angle) {
                        0 -> drawContext.canvas.nativeCanvas.drawText("N", textX, textY, northPaint)
                        90 -> drawContext.canvas.nativeCanvas.drawText("E", textX, textY, textPaint)
                        180 -> drawContext.canvas.nativeCanvas.drawText("S", textX, textY, textPaint)
                        270 -> drawContext.canvas.nativeCanvas.drawText("W", textX, textY, textPaint)
                        else -> drawContext.canvas.nativeCanvas.drawText("$angle", textX, textY, numPaint)
                    }
                }
            }

            // ── 绘制目标天线指向角 (Target Antenna Beam) ──
            if (targetAzimuth != null) {
                val targetRad = Math.toRadians(targetAzimuth.toDouble())

                // 目标波束高亮光锥 (扇形指示)
                val coneAngle = 6.0
                val leftRad = Math.toRadians(targetAzimuth - coneAngle)
                val rightRad = Math.toRadians(targetAzimuth + coneAngle)

                val conePath = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(
                        center.x + (radius - 12f) * sin(leftRad).toFloat(),
                        center.y - (radius - 12f) * cos(leftRad).toFloat()
                    )
                    lineTo(
                        center.x + (radius - 12f) * sin(rightRad).toFloat(),
                        center.y - (radius - 12f) * cos(rightRad).toFloat()
                    )
                    close()
                }
                drawPath(conePath, color = targetBeamColor.copy(alpha = 0.25f))

                // 目标主射线
                val targetEndX = center.x + (radius - 10f) * sin(targetRad).toFloat()
                val targetEndY = center.y - (radius - 10f) * cos(targetRad).toFloat()

                drawLine(
                    color = targetBeamColor,
                    start = center,
                    end = Offset(targetEndX, targetEndY),
                    strokeWidth = 4f
                )

                // 目标外圈锚点大圆点
                drawCircle(
                    color = targetBeamColor,
                    radius = 8f,
                    center = Offset(targetEndX, targetEndY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = Offset(targetEndX, targetEndY)
                )
            }
        }
    }
}
