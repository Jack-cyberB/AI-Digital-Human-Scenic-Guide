package com.jingqu.visitor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingqu.visitor.data.model.PoiCategory

@Composable
fun CategoryToggleBar(
    activeCategories: Set<PoiCategory>,
    onToggle: (PoiCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PoiCategory.entries.forEach { cat ->
                val isActive = cat in activeCategories
                Surface(
                    onClick = { onToggle(cat) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isActive) Color(cat.color).copy(alpha = 0.15f) else Color.Transparent,
                    border = if (isActive) BorderStroke(1.5.dp, Color(cat.color)) else BorderStroke(1.dp, Color(0xFFDDDDDD))
                ) {
                    Text(
                        "${cat.emoji} ${cat.label}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) Color(cat.color) else Color(0xFF888888)
                    )
                }
            }
        }
    }
}
