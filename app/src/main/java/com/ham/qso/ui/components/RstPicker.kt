package com.ham.qso.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ham.qso.data.model.Mode

@Composable
fun RstPicker(
    mode: Mode,
    rstSent: String,
    onRstSentChange: (String) -> Unit,
    rstRcvd: String,
    onRstRcvdChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = when (mode) {
        Mode.CW -> listOf("599", "579", "559", "539")
        Mode.FT8, Mode.FT4, Mode.RTTY, Mode.PSK31 -> listOf("-05", "-10", "-15", "-20", "+00", "+05")
        else -> listOf("59", "58", "57", "55", "53", "44")
    }

    val isDigital = mode in listOf(Mode.FT8, Mode.FT4, Mode.RTTY, Mode.PSK31)
    val rstKeyboardType = if (isDigital) KeyboardType.Ascii else KeyboardType.Number

    Column(modifier = modifier.fillMaxWidth()) {
        // 两个 44dp 紧凑型信号报告输入框
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 我给 (Sent)
            HamInputField(
                label = "我给 (Sent)",
                value = rstSent,
                onValueChange = onRstSentChange,
                keyboardOptions = KeyboardOptions(
                    keyboardType = rstKeyboardType,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )

            // 对方给 (Rcvd)
            HamInputField(
                label = "对方给 (Rcvd)",
                value = rstRcvd,
                onValueChange = onRstRcvdChange,
                keyboardOptions = KeyboardOptions(
                    keyboardType = rstKeyboardType,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 快捷预设胶囊列表
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "快捷:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            presets.forEach { preset ->
                SuggestionChip(
                    onClick = {
                        onRstSentChange(preset)
                        onRstRcvdChange(preset)
                    },
                    label = {
                        Text(
                            text = preset,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}
