package com.jingqu.visitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jingqu.visitor.data.model.PoiDetailData
import com.jingqu.visitor.data.model.ReviewCard

@Composable
fun PlaceDetailSheet(
    poi: PoiDetailData,
    aiDescription: String?,
    aiReviews: List<ReviewCard>,
    isLoadingAi: Boolean,
    onViewDetail: () -> Unit,
    onLoadAiEnrich: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCCCCCC))
            )
        }

        // Photos row
        if (poi.photos.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(poi.photos.take(6)) { photoUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = poi.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(160.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0))
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Category tags
        if (poi.category.isNotBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF6B35).copy(alpha = 0.12f)
                ) {
                    Text(
                        poi.category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B35)
                    )
                }
                if (poi.categoryTag.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1890FF).copy(alpha = 0.12f)
                    ) {
                        Text(
                            poi.categoryTag,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = Color(0xFF1890FF)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Name + rating
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                poi.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            poi.rating?.let { rating ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF9500).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("★", color = Color(0xFFFF9500), fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            String.format("%.1f", rating),
                            color = Color(0xFFFF9500),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color(0xFFF0F0F0))
        Spacer(modifier = Modifier.height(8.dp))

        // Info rows
        if (poi.address.isNotBlank()) {
            InfoRow(icon = "📍", text = poi.address)
        }
        if (poi.openingHours.isNotBlank()) {
            InfoRow(icon = "🕐", text = poi.openingHours)
        }
        if (poi.cost.isNotBlank()) {
            InfoRow(icon = "💰", text = poi.cost)
        }
        if (poi.phone.isNotBlank()) {
            InfoRow(icon = "📞", text = poi.phone)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Description section
        Text("📖 景点介绍", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        if (aiDescription != null) {
            Text(
                aiDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF333333),
                lineHeight = 22.sp
            )
        } else if (isLoadingAi) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI正在生成介绍...", fontSize = 13.sp, color = Color(0xFF999999))
            }
        } else {
            TextButton(onClick = onLoadAiEnrich) {
                Text("🤖 查看AI讲解", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reviews section
        if (aiReviews.isNotEmpty()) {
            Text("💬 游客点评", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            aiReviews.forEach { review ->
                ReviewCardItem(review)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onViewDetail,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("查看完整详情")
            }
        }
    }
}

@Composable
private fun InfoRow(icon: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun ReviewCardItem(review: ReviewCard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.userName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(review.rating) {
                            Text("★", fontSize = 10.sp, color = Color(0xFFFF9500))
                        }
                    }
                }
                Text(
                    "♥ ${formatLikeCount(review.likeCount)}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                review.content,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF555555),
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatLikeCount(count: Int): String {
    return when {
        count >= 10000 -> String.format("%.1fw", count / 10000.0)
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> count.toString()
    }
}
