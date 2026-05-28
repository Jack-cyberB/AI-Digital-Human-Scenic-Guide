package com.jingqu.visitor.ui.screens



import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.live2d.demo.full.GLRenderer
import com.live2d.demo.full.LAppDelegate
import com.jingqu.visitor.ui.components.*
import com.jingqu.visitor.ui.theme.*



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun MainScreen(

    viewModel: MainViewModel = hiltViewModel()

) {

    val uiState by viewModel.uiState.collectAsState()

    var inputText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()



    LaunchedEffect(uiState.messages.size) {

        if (uiState.messages.isNotEmpty()) {

            listState.animateScrollToItem(uiState.messages.size - 1)

        }

    }



    Scaffold(

        containerColor = Background,

        topBar = {

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

                                text = if (uiState.isConnected) "AI在线 · 实时导览" else "离线 · 正在等待连接",

                                style = MaterialTheme.typography.labelSmall,

                                color = OnPrimary.copy(alpha = 0.82f)

                            )

                        }

                    }

                },

                colors = TopAppBarDefaults.topAppBarColors(

                    containerColor = Primary

                ),

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

        },

        bottomBar = {

            MessageInputBar(

                inputText = inputText,

                onInputChange = { inputText = it },

                onSendClick = {

                    if (inputText.isNotBlank()) {

                        viewModel.sendMessage(inputText)

                        inputText = ""

                    }

                },

                onVoiceClick = { /* TODO: 语音输入后续接入 */ },
                enabled = uiState.isConnected

            )

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



            Column(modifier = Modifier.fillMaxSize()) {

                ConnectionStatusBar(
                    isConnected = uiState.isConnected,
                    isConnecting = uiState.isConnecting
                )

                ScenicSpotBanner(
                    scenicSpot = uiState.currentScenicSpot,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Live2DModelCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(220.dp)
                )



                QuickQuestionsRow(

                    questions = uiState.quickQuestions,

                    onQuestionClick = { viewModel.sendQuickQuestion(it) },

                    modifier = Modifier.padding(bottom = 8.dp)

                )



                val connectionError = uiState.connectionError

                if (connectionError != null) {

                    ErrorNotice(

                        message = connectionError,

                        onRetry = viewModel::reconnect,

                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

                    )

                }



                LazyColumn(

                    state = listState,

                    modifier = Modifier

                        .weight(1f)

                        .fillMaxWidth(),

                    contentPadding = PaddingValues(vertical = 8.dp)

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



@Composable
private fun Live2DModelCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? android.app.Activity
    val glSurfaceView = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(GLRenderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner) {
        if (activity != null) {
            LAppDelegate.getInstance().onStart(activity)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glSurfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    glSurfaceView.onPause()
                    LAppDelegate.getInstance().onPause()
                }
                Lifecycle.Event.ON_STOP -> LAppDelegate.getInstance().onStop()
                Lifecycle.Event.ON_DESTROY -> LAppDelegate.getInstance().onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Live2D 数字人",
                style = MaterialTheme.typography.titleMedium,
                color = OnBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                factory = { glSurfaceView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
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
                    text = "Live2D 数字人",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(

                    text = "数字人已接入，支持后续动作、语音与问答联动。",

                    style = MaterialTheme.typography.bodySmall,

                    color = OnBackground.copy(alpha = 0.68f)

                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    HeroChip(icon = Icons.Default.Navigation, text = "待机动画")

                    HeroChip(icon = Icons.Default.Map, text = if (isConnected) "在线" else "离线")

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

    onVoiceClick: () -> Unit,

    enabled: Boolean,

    modifier: Modifier = Modifier

) {

    Surface(

        modifier = modifier.fillMaxWidth(),

        shadowElevation = 12.dp,

        color = Surface

    ) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(horizontal = 16.dp, vertical = 10.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Box(modifier = Modifier.weight(1f)) {
            TextField(

                value = inputText,

                onValueChange = onInputChange,

                modifier = Modifier.fillMaxWidth(),

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
            }



            Spacer(modifier = Modifier.width(10.dp))



            FilledIconButton(

                onClick = onVoiceClick,
                enabled = enabled,
                modifier = Modifier.size(50.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ScenicSky,
                    disabledContainerColor = ScenicSky.copy(alpha = 0.45f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardVoice,
                    contentDescription = "语音输入",
                    tint = OnPrimary
                )
            }

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

