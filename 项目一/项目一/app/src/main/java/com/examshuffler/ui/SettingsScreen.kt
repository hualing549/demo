package com.examshuffler.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("exam_shuffler", Context.MODE_PRIVATE)

    var aiApiKey by remember { mutableStateOf(prefs.getString("ai_api_key", "") ?: "") }
    var aiEndpoint by remember { mutableStateOf(prefs.getString("ai_endpoint", "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions") }
    var enableAi by remember { mutableStateOf(prefs.getBoolean("enable_ai_parser", false)) }
    var showApiKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Usage guide
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("使用说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1. 点击主页右下角的相机图标\n" +
                                "2. 授权悬浮窗和截屏权限\n" +
                                "3. 在练习/考试页面点击悬浮图标\n" +
                                "4. 应用会自动截屏并识别题目\n" +
                                "5. 识别到的题目将保存到题库\n" +
                                "6. 点击乱序练习即可随机答题",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }

            // AI Parser settings
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AI 解析（可选）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "开启后使用AI解析复杂格式的试题，需要API Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用AI解析")
                        Switch(
                            checked = enableAi,
                            onCheckedChange = {
                                enableAi = it
                                prefs.edit().putBoolean("enable_ai_parser", it).apply()
                            }
                        )
                    }

                    if (enableAi) {
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = aiApiKey,
                            onValueChange = {
                                aiApiKey = it
                                prefs.edit().putString("ai_api_key", it).apply()
                            },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showApiKey)
                                androidx.compose.ui.text.input.VisualTransformation.None
                            else
                                androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showApiKey) "隐藏" else "显示"
                                    )
                                }
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = aiEndpoint,
                            onValueChange = {
                                aiEndpoint = it
                                prefs.edit().putString("ai_endpoint", it).apply()
                            },
                            label = { Text("API 地址") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // About
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("题目乱序 v1.0", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "一款通过截屏识别试题并进行乱序练习的辅助工具",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
