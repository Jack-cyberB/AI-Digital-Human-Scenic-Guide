package com.jingqu.visitor.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingqu.visitor.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel

data class ScenicCarouselItem(
    val title: String,
    val subtitle: String,
    val gradient: List<Color>
)

data class ServiceItem(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val prompt: String = ""
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTabScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSpot = uiState.currentScenicSpot
    val prefix = if (currentSpot != "景区入口") "在${currentSpot}" else ""

    val carouselItems = listOf(
        ScenicCarouselItem("黄 山", "奇松怪石 · 云海温泉", listOf(Color(0xFF1A5276), Color(0xFF2E86C1))),
        ScenicCarouselItem("故 宫", "皇家宫殿 · 六百年辉煌", listOf(Color(0xFF922B21), Color(0xFFC0392B))),
        ScenicCarouselItem("西 湖", "淡妆浓抹总相宜", listOf(Color(0xFF1B5E20), Color(0xFF43A047))),
        ScenicCarouselItem("张 家 界", "峰林奇观 · 人间仙境", listOf(Color(0xFF4A235A), Color(0xFF8E44AD)))
    )

    val services = listOf(
        ServiceItem(Icons.Default.Place, "景点介绍", Color(0xFFE74C3C), "${prefix}请详细介绍一下这里的著名景点，包括历史背景和游览建议".trim()),
        ServiceItem(Icons.Default.Navigation, "路线规划", Color(0xFF3498DB), "${prefix}请为我规划一份详细的游览路线，包括上午和下午的景点安排、交通方式和美食推荐".trim()),
        ServiceItem(Icons.Default.Restaurant, "餐饮服务", Color(0xFFF39C12), "${prefix}附近有什么特色餐厅推荐？请具体到店名和招牌菜".trim()),
        ServiceItem(Icons.Default.Hotel, "住宿推荐", Color(0xFF9B59B6), "${prefix}附近有什么推荐的住宿？包括酒店和民宿".trim()),
        ServiceItem(Icons.Default.Info, "景区公告", Color(0xFF1ABC9C), "${prefix}今天有什么特别活动或公告吗？".trim()),
        ServiceItem(Icons.Default.Warning, "紧急求助", Color(0xFFE74C3C), "${prefix}我需要帮助，附近的服务点在哪里？".trim()),
        ServiceItem(Icons.Default.DirectionsBus, "交通导览", Color(0xFF2ECC71), "${prefix}各个景点之间怎么走最方便？推荐交通方式".trim()),
        ServiceItem(Icons.Default.CameraAlt, "拍照打卡", Color(0xFFE91E63), "${prefix}有哪些值得拍照打卡的地方？请推荐一些拍照角度".trim())
    )

    val pagerState = rememberPagerState(pageCount = { carouselItems.size })

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 顶部景区轮播
        Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            HorizontalPager(state = pagerState) { page ->
                val item = carouselItems[page]
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(item.gradient)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
                    }
                }
            }
            // 轮播指示器
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(carouselItems.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 20.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 快捷服务标题
        Text(
            "快捷服务",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 服务网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.padding(horizontal = 12.dp).height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(services.size) { index ->
                val s = services[index]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.sendMessage(s.prompt) }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(s.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(s.icon, contentDescription = s.label, tint = s.color, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(s.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 底部标语
        Text(
            "智慧景区 · 畅游无忧",
            style = MaterialTheme.typography.bodyMedium,
            color = OnBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )
    }
}
