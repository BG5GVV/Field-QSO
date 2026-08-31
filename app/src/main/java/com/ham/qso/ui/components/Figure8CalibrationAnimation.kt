package com.ham.qso.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * 谷歌地图同款 8 字形（∞ 无穷符号）空间转动手机校准动画组件
 */
@Composable
fun Figure8CalibrationAnimation(
    modifier: Modifier = Modifier,
    isCalibrated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Figure8Transition")

    // 0.0 ~ 1.0 周期循环参数 t (周期约 3.6 秒，与人体手腕自然翻转节奏相符)
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // 光斑与脉冲动画
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = Color(0xFF00E676)
    val activeColor = if (isCalibrated) successColor else primaryColor

    // 伯努利/利萨如 8 字轨迹参数
    val t = progress * 2.0 * PI
    val sinT = sin(t).toFloat()
    val cosT = cos(t).toFloat()
    val sin2T = sin(2.0 * t).toFloat()
    val cos2T = cos(2.0 * t).toFloat()

    // 归一化位置 (-1.0 ~ +1.0)
    val normX = sinT
    val normY = 0.55f * sin2T

    // 速度矢量计算切线方向与 3D 姿态偏转
    val vx = cosT
    val vy = 1.1f * cos2T
    val tangentAngleDeg = (atan2(vy, vx) * 180f / PI).toFloat()

    // 3D 空间姿态角（模拟手腕在空中翻转 8 字时的 Roll、Pitch、Yaw 真实姿态）
    val rotZ = tangentAngleDeg * 0.45f
    val rotY = -sinT * 28f // 左右盘旋时机身侧倾
    val rotX = 14f + cos2T * 18f // 经过中心与上下弯折时机身俯仰

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current

        // ── 1. 绘制 8 字空间发光轨迹线与动态流动粒子 ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rx = size.width * 0.38f // 8 字横向半宽
            val ry = size.height * 0.36f // 8 字纵向半高

            val path = Path()
            val steps = 100
            for (i in 0..steps) {
                val stepT = (i.toFloat() / steps) * 2.0 * PI
                val px = cx + rx * sin(stepT).toFloat()
                val py = cy + ry * (0.55f * sin(2.0 * stepT).toFloat())
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }

            // 轨迹发光底层
            drawPath(
                path = path,
                color = activeColor.copy(alpha = 0.12f),
                style = Stroke(width = 12f)
            )

            // 轨迹主虚线
            drawPath(
                path = path,
                color = activeColor.copy(alpha = 0.45f),
                style = Stroke(
                    width = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), progress * 50f)
                )
            )

            // 绘制 8 字轨迹上的几个流光关键光标点
            for (k in 0..3) {
                val trailT = ((progress - k * 0.06f + 1f) % 1f) * 2.0 * PI
                val dotX = cx + rx * sin(trailT).toFloat()
                val dotY = cy + ry * (0.55f * sin(2.0 * trailT).toFloat())
                val alpha = (1f - k * 0.28f) * pulseAlpha
                drawCircle(
                    color = activeColor.copy(alpha = alpha),
                    radius = (7f - k * 1.5f).coerceAtLeast(2f),
                    center = Offset(dotX, dotY)
                )
            }
        }

        // ── 2. 运动中的 3D 拟真智能手机模型 ──
        // 映射当前位置坐标
        val boxWidthPx = with(density) { 320.dp.toPx() }
        val boxHeightPx = with(density) { 210.dp.toPx() }
        val posX = normX * (boxWidthPx * 0.38f)
        val posY = normY * (boxHeightPx * 0.36f)

        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { posX.toDp() },
                    y = with(density) { posY.toDp() }
                )
                .graphicsLayer {
                    rotationZ = rotZ
                    rotationY = rotY
                    rotationX = rotX
                    cameraDistance = 16f * density.density
                }
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), spotColor = activeColor)
                .size(width = 68.dp, height = 110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C3440),
                            Color(0xFF161B22),
                            Color(0xFF0D1117)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            activeColor,
                            activeColor.copy(alpha = 0.5f),
                            Color(0xFF4A5568)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // 手机屏幕内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部听筒 / 灵动岛微型孔
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(width = 18.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                )

                // 屏幕中央电子罗盘雷达波与箭头
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = activeColor.copy(alpha = 0.2f),
                            radius = size.width / 2f
                        )
                        drawCircle(
                            color = activeColor.copy(alpha = 0.5f),
                            radius = size.width / 3f,
                            style = Stroke(width = 1.5f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                rotationZ = -rotZ * 1.5f
                            }
                    )
                }

                // 底部指示条
                Box(
                    modifier = Modifier
                        .padding(bottom = 3.dp)
                        .size(width = 22.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                )
            }
        }

        // ── 3. 底部悬浮提示标签 ──
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                activeColor.copy(alpha = 0.35f)
            ),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "手持手机在空中平滑画“8”字（∞）并转动手腕",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
            }
        }
    }
}
