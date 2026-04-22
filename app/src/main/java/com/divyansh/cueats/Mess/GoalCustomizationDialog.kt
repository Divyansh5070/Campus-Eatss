package com.divyansh.cueats.Mess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun GoalCustomizationDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    primaryOrange: Color,
    surfaceColor: Color,
    textColor: Color
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    val presetGoals = listOf(1500, 2000, 2500, 3000)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Set Daily Calorie Goal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontFamily = poppinsFont
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose a preset or enter custom value",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = playfairFont
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Preset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetGoals.forEach { preset ->
                        Button(
                            onClick = { goalText = preset.toString() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (goalText == preset.toString()) 
                                    primaryOrange 
                                else 
                                    Color.LightGray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$preset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (goalText == preset.toString()) 
                                    Color.White 
                                else 
                                    textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Input
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { 
                        // Only allow digits
                        if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                            goalText = it
                        }
                    },
                    label = { Text("Custom Goal (calories)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryOrange,
                        focusedLabelColor = primaryOrange,
                        cursorColor = primaryOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textColor
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val newGoal = goalText.toIntOrNull()
                            if (newGoal != null && newGoal > 0) {
                                onSave(newGoal)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryOrange
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = goalText.toIntOrNull() != null && goalText.toInt() > 0
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
