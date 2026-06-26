package com.jingqu.visitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.jingqu.visitor.data.model.ReviewCard

@Composable
fun ReviewCardList(
    reviews: List<ReviewCard>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        reviews.forEach { review ->
            ReviewCardItemExpanded(review)
        }
    }
}

@Composable
private fun ReviewCardItemExpanded(review: ReviewCard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        review.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { idx ->
                            Text(
                                if (idx < review.rating) "★" else "☆",
                                fontSize = 10.sp,
                                color = Color(0xFFFF9500)
                            )
                        }
                    }
                }
                Text(
                    "♥ ${formatLikeCount(review.likeCount)}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                review.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF555555),
                lineHeight = 20.sp
            )
        }
    }
}

private fun formatLikeCount(count: Int): String = when {
    count >= 10000 -> String.format("%.1fw", count / 10000.0)
    count >= 1000 -> String.format("%.1fk", count / 1000.0)
    else -> count.toString()
}
