package com.jingqu.visitor.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.json.JSONObject
import com.jingqu.visitor.ui.theme.*

@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = SurfaceSoft) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                    label = { Text("首页") }, selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }, colors = navBarColors()
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI助手") },
                    label = { Text("AI助手") }, selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }, colors = navBarColors()
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "路线导览") },
                    label = { Text("路线导览") }, selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }, colors = navBarColors()
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                    label = { Text("我的") }, selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }, colors = navBarColors()
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeTabScreen()
                1 -> AIAssistantScreen()
                2 -> MapRouteScreen()
                3 -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun navBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Primary, selectedTextColor = Primary,
    indicatorColor = Primary.copy(alpha = 0.12f)
)

@Composable
fun RouteGuideScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val routeJson = uiState.routeDataJson
    val mode = uiState.routeMode ?: "city"

    if (routeJson == null) {
        // 默认地图占位
        Box(modifier = Modifier.fillMaxSize()) {
            // 模拟地图背景
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("路线导览", style = MaterialTheme.typography.titleLarge, color = Primary)
                Text("北京市中心 · 等待路线加载", style = MaterialTheme.typography.labelMedium, color = OnBackground.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.weight(1f))
                // 地图占位区
                Box(modifier = Modifier.fillMaxWidth().height(300.dp).let { it }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(72.dp), tint = Primary.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("📍 北京中心 (39.9042°N, 116.4074°E)", style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("在AI助手中使用[路线规划]功能", style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.3f))
                        Text("路线将自动显示在这里", style = MaterialTheme.typography.labelSmall, color = OnBackground.copy(alpha = 0.2f))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    } else {
        // 解析并显示路线
        val points = remember(routeJson) { parseRoutePoints(routeJson) }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("游览路线", style = MaterialTheme.typography.titleLarge, color = Primary)
            Text(if (mode == "scenic") "景区步行路线" else "城市游览路线", style = MaterialTheme.typography.bodySmall, color = OnBackground.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            if (points.isEmpty()) {
                Text("暂无路线点", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(points) { group ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceSoft)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                group.forEachIndexed { idx, pt ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${idx + 1}.", style = MaterialTheme.typography.labelMedium, color = Primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(pt.first, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(pt.second, style = MaterialTheme.typography.labelSmall, color = OnBackground.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseRoutePoints(json: String): List<List<Pair<String, String>>> {
    try {
        val root = org.json.JSONObject(json)
        val routes = root.getJSONArray("dailyRoutes")
        val result = mutableListOf<List<Pair<String, String>>>()
        for (i in 0 until routes.length()) {
            val day = routes.getJSONObject(i)
            val pts = day.getJSONArray("points")
            val dayPoints = mutableListOf<Pair<String, String>>()
            for (j in 0 until pts.length()) {
                val pt = pts.getJSONObject(j)
                dayPoints.add(Pair(pt.getString("keyword"), pt.getString("city")))
            }
            result.add(dayPoints)
        }
        return result
    } catch (e: Exception) {
        return emptyList()
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSpot = uiState.currentScenicSpot

    val spots = listOf(
        ScenicSpotData("灵山胜境", "无锡市", "🏔", "佛教圣地，灵山大佛"),
        ScenicSpotData("黄山", "黄山市", "⛰", "奇松怪石，云海温泉"),
        ScenicSpotData("故宫", "北京市", "🏯", "皇家宫殿，六百年辉煌"),
        ScenicSpotData("西湖", "杭州市", "🌊", "淡妆浓抹总相宜"),
        ScenicSpotData("张家界", "张家界市", "🏞", "峰林奇观，人间仙境")
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 头部
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Primary, Primary.copy(alpha = 0.7f)))).padding(24.dp),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("游客", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("智慧景区导览", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            }
        }

        // 景区选择区
        Text("当前景区", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(top = 12.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spots.forEach { spot ->
                val sel = currentSpot == spot.name
                Card(
                    modifier = Modifier.width(100.dp).clickable { viewModel.updateScenicSpot(spot.name) },
                    colors = CardDefaults.cardColors(containerColor = if (sel) Primary.copy(alpha = 0.1f) else SurfaceSoft),
                    border = if (sel) androidx.compose.foundation.BorderStroke(2.dp, Primary) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(spot.emoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(spot.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        if (sel) Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        // 当前景区简介
        val sel = spots.firstOrNull { it.name == currentSpot }
        if (sel != null) {
            Text("📍 ${sel.city} · ${sel.desc}", style = MaterialTheme.typography.labelSmall, color = OnBackground.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 功能列表
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceSoft)) {
            Column {
                ProfileMenuItem(Icons.Default.History, "历史对话", "查看过去的游览规划记录")
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                ProfileMenuItem(Icons.Default.Favorite, "我的收藏", "收藏的景点和路线")
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                ProfileMenuItem(Icons.Default.Map, "离线地图", "下载离线地图包")
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                ProfileMenuItem(Icons.Default.Settings, "设置", "通知、语音、字体等")
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                ProfileMenuItem(Icons.Default.Info, "关于", "版本 v1.0.0")
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                ProfileMenuItem(Icons.Default.Logout, "退出登录", "清除数据返回首页", color = Color(0xFFE74C3C))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("© 2026 智慧景区导览", style = MaterialTheme.typography.labelSmall, color = OnBackground.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, color: Color = OnBackground) {
    Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = OnBackground.copy(alpha = 0.4f))
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCCCCCC))
    }
}

data class ScenicSpotData(val name: String, val city: String, val emoji: String, val desc: String)
