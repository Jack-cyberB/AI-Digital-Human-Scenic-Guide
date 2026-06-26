package com.jingqu.visitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingqu.visitor.data.model.PoiDetailData

/**
 * Mini POI card shown when a category marker is tapped on the map.
 * Compact, non-blocking card at the bottom of the screen.
 */
@Composable
fun PlaceCard(
    poi: PoiDetailData,
    onViewDetail: () -> Unit,
    onDismiss: () -> Unit,
    categoryColor: Long = 0xFFFF6B35,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(categoryColor))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    poi.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (poi.address.isNotBlank()) {
                    Text(
                        poi.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }

            poi.rating?.let { rating ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "★ ${String.format("%.1f", rating)}",
                        color = Color(0xFFFF9500),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            TextButton(onClick = onViewDetail) {
                Text("查看详情", fontSize = 13.sp)
            }
        }
    }
}
