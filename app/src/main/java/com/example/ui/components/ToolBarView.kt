package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ToolType

@Composable
fun ToolBarView(
    currentLevel: Int,
    toolInventory: Map<ToolType, Int>,
    onUseTool: (ToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolType.values().forEach { tool ->
            val isUnlocked = currentLevel >= tool.unlockLevel
            val count = toolInventory[tool] ?: 0

            ToolButton(
                tool = tool,
                isUnlocked = isUnlocked,
                count = count,
                onClick = { onUseTool(tool) }
            )
        }
    }
}

@Composable
fun ToolButton(
    tool: ToolType,
    isUnlocked: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    val buttonBg = if (isUnlocked) {
        Brush.verticalGradient(
            listOf(Color(0xFF37474F), Color(0xFF212121))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFF1E1E1E), Color(0xFF121212))
        )
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(buttonBg)
            .border(
                width = 1.5.dp,
                color = if (isUnlocked) Color(0xFFFFD54F).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = isUnlocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = tool.icon,
                fontSize = 20.sp
            )
            if (!isUnlocked) {
                Text(
                    text = "Lv${tool.unlockLevel}",
                    color = Color(0xFFAAAAAA),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quantity Badge (Top Right)
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
