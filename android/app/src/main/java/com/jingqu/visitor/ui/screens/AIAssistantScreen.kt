package com.jingqu.visitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import com.jingqu.visitor.ui.components.*
import com.jingqu.visitor.ui.theme.*
import com.live2d.demo.full.LAppDelegate
import com.live2d.demo.full.LAppLive2DManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isDigitalHumanMode by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Scaffold(
        containerColor = if (isDigitalHumanMode) Color.White else Background,
        topBar = {
            if (!isDigitalHumanMode) {
                TopAppBar(
                    title = { Text("AI助手", color = OnPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary),
                    actions = {
                        IconButton(onClick = { viewModel.toggleMute() }) {
                            Icon(
                                if (uiState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (uiState.isMuted) "取消静音" else "静音",
                                tint = OnPrimary
                            )
                        }
                        IconButton(onClick = { isDigitalHumanMode = true }) {
                            Icon(Icons.Default.Face, "数字人模式", tint = OnPrimary)
                        }
                    }
                )
            }
        },
        bottomBar = {
            MessageInputBar(
                inputText = inputText, onInputChange = { inputText = it },
                onSendClick = { if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = "" } },
                onVoiceClick = {}, enabled = uiState.isConnected
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(if (isDigitalHumanMode) Color.White else Color.Transparent)) {
            if (isDigitalHumanMode) {
                val models = remember { listOf("Haru", "Hiyori", "Mao") }
                var currentModelIdx by remember { mutableIntStateOf(2) } // Mao is default (index 2 alphabetically)

                Column(modifier = Modifier.fillMaxSize()) {
                    // Live2D + 关闭按钮 + 模型切换
                    Box(modifier = Modifier.fillMaxWidth().weight(0.55f)) {
                        Live2DModelCard(modifier = Modifier.fillMaxSize())

                        // 右上角：静音 + 关闭 + 模型切换
                        Column(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalAlignment = Alignment.End) {
                            // 静音按钮
                            IconButton(
                                onClick = { viewModel.toggleMute() },
                                modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    if (uiState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (uiState.isMuted) "取消静音" else "静音",
                                    tint = Color(0xFF333333),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            IconButton(
                                onClick = { isDigitalHumanMode = false },
                                modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.15f), CircleShape)
                            ) { Icon(Icons.Default.Close, "关闭", tint = Color(0xFF333333), modifier = Modifier.size(20.dp)) }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 模型切换按钮
                            models.forEachIndexed { idx, name ->
                                val sel = idx == currentModelIdx
                                Surface(
                                    onClick = {
                                        currentModelIdx = idx
                                        LAppDelegate.switchToModel(idx)
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (sel) Color(0xFF333333) else Color.Black.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        name,
                                        color = if (sel) Color.White else Color(0xFF333333),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // 消息气泡
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(0.45f).background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        items(uiState.messages.filter { it.isFromUser || it.content.isNotEmpty() }, key = { it.id }) { msg ->
                            Text(
                                text = if (msg.isFromUser) "👤 ${msg.content}" else "🤖 ${msg.content}",
                                color = Color(0xFF333333),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp).background(Color.Black.copy(alpha = 0.06f), MaterialTheme.shapes.small).padding(8.dp)
                            )
                        }
                    }
                }
            } else {
                // 文本模式：Live2D隐藏 + 服务网格 + 聊天
                Live2DModelCard(modifier = Modifier.size(0.dp))

                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ScenicMint, Background, SurfaceSoft)))) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ConnectionStatusBar(isConnected = uiState.isConnected, isConnecting = uiState.isConnecting)
                        ServiceGrid(onServiceClick = { viewModel.sendQuickQuestion(it) }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), scenicSpot = uiState.currentScenicSpot)
                        val connectionError = uiState.connectionError
                        if (connectionError != null) { ErrorNotice(message = connectionError, onRetry = viewModel::reconnect, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(uiState.messages.filter { it.isFromUser || it.content.isNotEmpty() }, key = { it.id }) { msg -> ChatBubble(message = msg) }
                            if (uiState.isTyping) item { TypingIndicator() }
                        }
                    }
                }
            }
        }
    }
}

private data class SvcItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val color: Color, val prompt: String)

@Composable
private fun ServiceGrid(onServiceClick: (String) -> Unit, modifier: Modifier = Modifier, scenicSpot: String = "景区入口") {
    val s = if (scenicSpot == "景区入口") "" else scenicSpot
    val spt = if (scenicSpot == "景区入口") "" else scenicSpot
    val prefix = if (spt.isEmpty()) "" else "在${spt}"
    val services = listOf(
        SvcItem(Icons.Default.Navigation, "路线规划", Color(0xFF3498DB), "${prefix}请为我规划一份详细的游览路线，包括上午和下午的景点安排、交通方式和美食推荐".trim()),
        SvcItem(Icons.Default.Place, "景点讲解", Color(0xFFE74C3C), "${prefix}请详细介绍一下这里的著名景点，包括历史背景和游览建议".trim()),
        SvcItem(Icons.Default.Restaurant, "餐饮推荐", Color(0xFFF39C12), "${prefix}附近有什么特色餐厅推荐？请具体到店名和招牌菜".trim()),
        SvcItem(Icons.Default.DirectionsBus, "交通指引", Color(0xFF2ECC71), "${prefix}各个景点之间怎么走最方便？推荐交通方式".trim()),
        SvcItem(Icons.Default.Info, "景区公告", Color(0xFF1ABC9C), "${prefix}今天有什么特别活动或公告吗？".trim()),
        SvcItem(Icons.Default.Warning, "紧急求助", Color(0xFFE74C3C), "${prefix}我需要帮助，附近的服务点在哪里？".trim())
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        services.forEach { svc ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onServiceClick(svc.prompt) }.padding(4.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(svc.color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(svc.icon, svc.label, tint = svc.color, modifier = Modifier.size(20.dp))
                }
                Text(svc.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
