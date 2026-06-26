package com.jingqu.visitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingqu.visitor.data.model.PoiDetailData
import com.jingqu.visitor.data.model.ReviewCard
import com.jingqu.visitor.ui.components.ImageCarousel
import com.jingqu.visitor.ui.components.ReviewCardList
import com.jingqu.visitor.ui.components.TagChipRow

/**
 * Full-screen immersive place detail screen.
 * Shown as an overlay when user taps "查看详情" from a marker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    poi: PoiDetailData,
    aiDescription: String?,
    aiReviews: List<ReviewCard>,
    isLoadingAi: Boolean,
    onBack: () -> Unit,
    onNavigate: () -> Unit,
    onLoadAiEnrich: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地点详情", fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* share */ }) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "分享",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Add to route */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加到路线")
                    }
                    Button(
                        onClick = onNavigate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导航到这里")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Image Carousel
            ImageCarousel(
                photos = poi.photos,
                modifier = Modifier.fillMaxWidth(),
                height = 240.dp,
                placeholderText = "📷 暂无景区图片"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content area
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Category tags - deduplicated from Amap type hierarchy
                val tags = mutableListOf<String>()
                if (poi.category.isNotBlank()) tags.add(poi.category)
                if (poi.categoryTag.isNotBlank()) {
                    poi.categoryTag.split(";")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it != poi.category }
                        .take(3)
                        .forEach { tags.add(it) }
                }
                if (tags.isNotEmpty()) {
                    TagChipRow(tags = tags)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Name + Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        poi.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    poi.rating?.let { rating ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF9500).copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("★", color = Color(0xFFFF9500), fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        String.format("%.1f", rating),
                                        color = Color(0xFFFF9500),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                    if (poi.reviewCount != null) {
                                        Text(
                                            "${poi.reviewCount} 评价",
                                            fontSize = 10.sp,
                                            color = Color(0xFF999999)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(12.dp))

                // Info rows
                InfoCard(icon = "📍", label = "地址", value = poi.address.ifBlank { "暂无地址信息" })
                if (poi.openingHours.isNotBlank()) {
                    InfoCard(icon = "🕐", label = "开放时间", value = poi.openingHours)
                }
                if (poi.cost.isNotBlank()) {
                    InfoCard(icon = "💰", label = "费用", value = poi.cost)
                }
                if (poi.phone.isNotBlank()) {
                    InfoCard(icon = "📞", label = "电话", value = poi.phone)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Description Section
                Text(
                    "📖 景点介绍",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (aiDescription != null) {
                    Text(
                        aiDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF333333),
                        lineHeight = 24.sp
                    )
                } else if (isLoadingAi) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI正在生成介绍...", fontSize = 13.sp, color = Color(0xFF999999))
                    }
                } else {
                    OutlinedButton(onClick = onLoadAiEnrich) {
                        Text("🤖 AI生成详细讲解")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reviews Section
                if (aiReviews.isNotEmpty()) {
                    Text(
                        "💬 游客点评",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReviewCardList(reviews = aiReviews)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(icon: String, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    label,
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
