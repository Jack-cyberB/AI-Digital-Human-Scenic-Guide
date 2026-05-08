package com.jingqu.visitor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jingqu.visitor.data.model.ChatMessage
import com.jingqu.visitor.ui.components.ChatBubble
import com.jingqu.visitor.ui.components.ConnectionStatusBar
import com.jingqu.visitor.ui.components.NotificationDialog
import com.jingqu.visitor.ui.components.QuickQuestionsRow
import com.jingqu.visitor.ui.components.TypingIndicator
import com.jingqu.visitor.ui.theme.AIBubble
import com.jingqu.visitor.ui.theme.Background
import com.jingqu.visitor.ui.theme.GlassWhite
import com.jingqu.visitor.ui.theme.LineSoft
import com.jingqu.visitor.ui.theme.OnBackground
import com.jingqu.visitor.ui.theme.OnPrimary
import com.jingqu.visitor.ui.theme.Primary
import com.jingqu.visitor.ui.theme.ScenicBlue
import com.jingqu.visitor.ui.theme.ScenicGlow
import com.jingqu.visitor.ui.theme.ScenicMint
import com.jingqu.visitor.ui.theme.ScenicSky
import com.jingqu.visitor.ui.theme.Secondary
import com.jingqu.visitor.ui.theme.Surface
import com.jingqu.visitor.ui.theme.SurfaceSoft
import com.jingqu.visitor.ui.theme.TechPurple
import com.jingqu.visitor.ui.theme.WarningOrange

private enum class MainDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("首页", Icons.Default.Home),
    CHAT("对话", Icons.Default.Chat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, currentDestination) {
        if (currentDestination == MainDestination.CHAT && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            ScenicTopBar(isConnected = uiState.isConnected)
        },
        bottomBar = {
            Column {
                if (currentDestination == MainDestination.CHAT) {
                    MessageInputBar(
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        onSendClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = true
                    )
                }
                CompactNavigationBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = { currentDestination = it }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ScenicMint, Background, SurfaceSoft)
                    )
                )
        ) {
            ScenicDigitalBackground(modifier = Modifier.matchParentSize())

            when (currentDestination) {
                MainDestination.HOME -> {
                    HomeContent(
                        uiState = uiState,
                        onQuestionClick = {
                            viewModel.sendQuickQuestion(it)
                            currentDestination = MainDestination.CHAT
                        },
                        onOpenChat = { currentDestination = MainDestination.CHAT }
                    )
                }

                MainDestination.CHAT -> {
                    ChatContent(
                        uiState = uiState,
                        listState = listState,
                        onRetry = viewModel::reconnect
                    )
                }
            }
        }
    }

    uiState.currentNotification?.let { notification ->
        NotificationDialog(
            title = notification.title,
            content = notification.content,
            type = notification.type,
            onDismiss = { viewModel.dismissNotification() },
            onConfirm = { viewModel.dismissNotification() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenicTopBar(isConnected: Boolean) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(OnPrimary, ScenicSky)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "智能导览助手",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnPrimary
                    )
                    Text(
                        text = if (isConnected) "AI在线 · 实时导览" else "离线 · 等待连接",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnPrimary.copy(alpha = 0.82f)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary),
        actions = {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = OnPrimary
                )
            }
        }
    )
}

@Composable
private fun HomeContent(
    uiState: ChatUiState,
    onQuestionClick: (String) -> Unit,
    onOpenChat: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ConnectionStatusBar(
            isConnected = uiState.isConnected,
            isConnecting = uiState.isConnecting
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                ScenicSpotBanner(
                    scenicSpot = uiState.currentScenicSpot,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                DigitalGuideHero(
                    isConnected = uiState.isConnected,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                QuickQuestionsRow(
                    questions = uiState.quickQuestions,
                    onQuestionClick = onQuestionClick,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                HomeConversationCard(
                    latestMessage = uiState.messages.lastOrNull { !it.isFromUser },
                    messageCount = uiState.messages.size,
                    onOpenChat = onOpenChat,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ConnectionStatusBar(
            isConnected = uiState.isConnected,
            isConnecting = uiState.isConnecting
        )

        ScenicSpotBanner(
            scenicSpot = uiState.currentScenicSpot,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val connectionError = uiState.connectionError
        if (connectionError != null) {
            ErrorNotice(
                message = connectionError,
                onRetry = onRetry,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }

            if (uiState.isTyping) {
                item {
                    TypingIndicator()
                }
            }
        }
    }
}

@Composable
private fun CompactNavigationBar(
    currentDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        containerColor = Surface,
        tonalElevation = 0.dp
    ) {
        MainDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination == destination,
                onClick = { onDestinationSelected(destination) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Primary.copy(alpha = 0.14f),
                    selectedIconColor = Primary,
                    selectedTextColor = OnBackground,
                    unselectedIconColor = OnBackground.copy(alpha = 0.72f),
                    unselectedTextColor = OnBackground.copy(alpha = 0.72f)
                ),
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            )
        }
    }
}

@Composable
private fun HomeConversationCard(
    latestMessage: ChatMessage?,
    messageCount: Int,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenChat() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "对话中心",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnBackground
                    )
                    Text(
                        text = "最近 $messageCount 条互动记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBackground.copy(alpha = 0.6f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = TechPurple.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = TechPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "进入对话",
                            style = MaterialTheme.typography.labelMedium,
                            color = TechPurple
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = AIBubble,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = latestMessage?.content ?: "点击进入对话页，向数字人询问景点、路线、演出时间或服务信息。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ScenicDigitalBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = ScenicGlow.copy(alpha = 0.22f),
            radius = size.width * 0.36f,
            center = Offset(size.width * 0.86f, size.height * 0.08f)
        )
        drawCircle(
            color = ScenicSky.copy(alpha = 0.2f),
            radius = size.width * 0.28f,
            center = Offset(size.width * 0.08f, size.height * 0.44f)
        )

        val mountain = Path().apply {
            moveTo(0f, size.height * 0.28f)
            lineTo(size.width * 0.24f, size.height * 0.18f)
            lineTo(size.width * 0.48f, size.height * 0.30f)
            lineTo(size.width * 0.72f, size.height * 0.16f)
            lineTo(size.width, size.height * 0.28f)
            lineTo(size.width, size.height * 0.36f)
            lineTo(0f, size.height * 0.36f)
            close()
        }
        drawPath(mountain, color = Primary.copy(alpha = 0.07f))

        repeat(7) { index ->
            val y = size.height * (0.18f + index * 0.075f)
            drawLine(
                color = LineSoft,
                start = Offset(size.width * 0.06f, y),
                end = Offset(size.width * 0.94f, y + if (index % 2 == 0) 18f else -12f),
                strokeWidth = 1.4f
            )
        }
    }
}

@Composable
private fun ScenicSpotBanner(
    scenicSpot: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ScenicBlue.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "当前位置：$scenicSpot",
                style = MaterialTheme.typography.labelMedium,
                color = OnBackground
            )
        }
    }
}

@Composable
private fun ErrorNotice(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = WarningOrange.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = OnBackground,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun DigitalGuideHero(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(GlassWhite, ScenicBlue.copy(alpha = 0.8f))
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Primary, Secondary, TechPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "景区智慧驾驶舱",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "路线、景点、餐饮与求助服务一屏聚合，让游客端更像智能软件助手。",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackground.copy(alpha = 0.68f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip(icon = Icons.Default.Navigation, text = "智能路线")
                    HeroChip(
                        icon = Icons.Default.Map,
                        text = if (isConnected) "实时在线" else "离线模式"
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Primary
            )
        }
    }
}

@Composable
fun MessageInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 10.dp,
        color = Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                placeholder = {
                    Text(
                        text = "问路线、景点、演出时间...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceSoft,
                    unfocusedContainerColor = SurfaceSoft,
                    disabledContainerColor = SurfaceSoft,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                enabled = enabled
            )

            Spacer(modifier = Modifier.width(10.dp))

            FilledIconButton(
                onClick = onSendClick,
                enabled = enabled && inputText.isNotBlank(),
                modifier = Modifier.size(50.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.45f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送",
                    tint = OnPrimary
                )
            }
        }
    }
}
