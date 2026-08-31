package com.ham.qso.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode

@Composable
fun BandModeSelector(
    selectedBand: Band,
    onBandSelected: (Band) -> Unit,
    selectedMode: Mode,
    onModeSelected: (Mode) -> Unit,
    frequencyMhz: String,
    onFrequencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var bandExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 波段选择框 (高度 44dp)
            Box(modifier = Modifier.weight(1f)) {
                HamInputField(
                    label = "波段",
                    value = selectedBand.label,
                    onValueChange = {},
                    readOnly = true,
                    textColor = MaterialTheme.colorScheme.primary,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { bandExpanded = true }
                )

                DropdownMenu(
                    expanded = bandExpanded,
                    onDismissRequest = { bandExpanded = false }
                ) {
                    Band.entries.forEach { band ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = band.label,
                                        fontWeight = if (band == selectedBand) FontWeight.Bold else FontWeight.Normal,
                                        color = if (band == selectedBand) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "%.3f MHz".format(band.frequencyMhz),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            onClick = {
                                onBandSelected(band)
                                bandExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. 模式选择框 (高度 44dp)
            Box(modifier = Modifier.weight(1f)) {
                HamInputField(
                    label = "模式",
                    value = selectedMode.label,
                    onValueChange = {},
                    readOnly = true,
                    textColor = MaterialTheme.colorScheme.secondary,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { modeExpanded = true }
                )

                DropdownMenu(
                    expanded = modeExpanded,
                    onDismissRequest = { modeExpanded = false }
                ) {
                    Mode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.label,
                                    fontWeight = if (mode == selectedMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mode == selectedMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onModeSelected(mode)
                                modeExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. 频率输入框 (高度 44dp)
            HamInputField(
                label = "频率(MHz)",
                value = frequencyMhz,
                onValueChange = onFrequencyChanged,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}
