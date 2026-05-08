package com.jingqu.visitor.ui.components



import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.unit.dp

import com.jingqu.visitor.data.model.QuickQuestion

import com.jingqu.visitor.ui.theme.*



@Composable

fun QuickQuestionCard(

    quickQuestion: QuickQuestion,

    onClick: (String) -> Unit,

    modifier: Modifier = Modifier

) {

    Card(

        modifier = modifier.clickable { onClick(quickQuestion.question) },

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(containerColor = GlassWhite),

        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)

    ) {

        Column(

            modifier = Modifier

                .background(

                    Brush.verticalGradient(

                        colors = listOf(Surface, ScenicBlue.copy(alpha = 0.45f))

                    )

                )

                .padding(horizontal = 8.dp, vertical = 12.dp)

                .fillMaxWidth(),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center

        ) {

            Box(

                modifier = Modifier

                    .size(38.dp)

                    .clip(CircleShape)

                    .background(Primary.copy(alpha = 0.1f)),

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    imageVector = getIconForQuestion(quickQuestion.icon),

                    contentDescription = quickQuestion.title,

                    modifier = Modifier.size(22.dp),

                    tint = Primary

                )

            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(

                text = quickQuestion.title,

                style = MaterialTheme.typography.labelMedium,

                color = OnSurface

            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(

                text = "AI快捷入口",

                style = MaterialTheme.typography.labelSmall,

                color = OnSurface.copy(alpha = 0.5f)

            )

        }

    }

}



@Composable

fun QuickQuestionsRow(

    questions: List<QuickQuestion>,

    onQuestionClick: (String) -> Unit,

    modifier: Modifier = Modifier

) {

    Column(modifier = modifier.fillMaxWidth()) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(horizontal = 16.dp, vertical = 8.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Column {

                Text(

                    text = "快捷服务矩阵",

                    style = MaterialTheme.typography.titleSmall,

                    color = OnBackground

                )

                Text(

                    text = "一键唤起高频导览能力",

                    style = MaterialTheme.typography.labelSmall,

                    color = OnBackground.copy(alpha = 0.55f)

                )

            }

            Surface(

                shape = RoundedCornerShape(50),

                color = TechPurple.copy(alpha = 0.1f)

            ) {

                Text(

                    text = "Smart Guide",

                    style = MaterialTheme.typography.labelSmall,

                    color = TechPurple,

                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)

                )

            }

        }



        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(horizontal = 12.dp),

            horizontalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            questions.forEach { question ->

                QuickQuestionCard(

                    quickQuestion = question,

                    onClick = onQuestionClick,

                    modifier = Modifier.weight(1f)

                )

            }

        }

    }

}



@Composable

fun ConnectionStatusBar(

    isConnected: Boolean,

    isConnecting: Boolean,

    modifier: Modifier = Modifier

) {

    val backgroundColor = when {

        isConnecting -> WarningOrange

        isConnected -> Primary

        else -> DisconnectedRed

    }



    val text = when {

        isConnecting -> "正在连接智慧导览服务..."

        isConnected -> "导览服务已连接 · WebSocket实时同步"

        else -> "导览服务未连接"

    }



    Surface(

        modifier = modifier.fillMaxWidth(),

        color = backgroundColor.copy(alpha = 0.92f)

    ) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(horizontal = 16.dp, vertical = 5.dp),

            horizontalArrangement = Arrangement.Center,

            verticalAlignment = Alignment.CenterVertically

        ) {

            if (isConnecting) {

                CircularProgressIndicator(

                    modifier = Modifier.size(12.dp),

                    strokeWidth = 2.dp,

                    color = OnPrimary

                )

                Spacer(modifier = Modifier.width(6.dp))

            } else {

                Box(

                    modifier = Modifier

                        .size(7.dp)

                        .clip(CircleShape)

                        .background(OnPrimary)

                )

                Spacer(modifier = Modifier.width(6.dp))

            }

            Text(

                text = text,

                style = MaterialTheme.typography.labelSmall,

                color = OnPrimary

            )

        }

    }

}



@Composable

fun NotificationDialog(

    title: String,

    content: String,

    type: String,

    onDismiss: () -> Unit,

    onConfirm: () -> Unit

) {

    val backgroundColor = when (type.uppercase()) {

        "EMERGENCY" -> Error

        "UPDATE" -> InfoBlue

        else -> Primary

    }



    AlertDialog(

        onDismissRequest = onDismiss,

        icon = {

            Icon(

                imageVector = when (type.uppercase()) {

                    "EMERGENCY" -> Icons.Default.Warning

                    "UPDATE" -> Icons.Default.Update

                    else -> Icons.Default.Info

                },

                contentDescription = null,

                tint = backgroundColor,

                modifier = Modifier.size(32.dp)

            )

        },

        title = {

            Text(

                text = title,

                style = MaterialTheme.typography.titleLarge

            )

        },

        text = {

            Text(

                text = content,

                style = MaterialTheme.typography.bodyMedium,

                maxLines = 6,

                overflow = TextOverflow.Ellipsis

            )

        },

        confirmButton = {

            Button(

                onClick = onConfirm,

                colors = ButtonDefaults.buttonColors(

                    containerColor = backgroundColor

                )

            ) {

                Text("我知道了")

            }

        },

        containerColor = Surface

    )

}



private fun getIconForQuestion(iconName: String): ImageVector {

    return when (iconName.lowercase()) {

        "spot", "景点" -> Icons.Default.Place

        "route", "路线" -> Icons.Default.Route

        "restaurant", "餐饮" -> Icons.Default.Restaurant

        "help", "帮助" -> Icons.Default.Help

        else -> Icons.Default.QuestionMark

    }

}

