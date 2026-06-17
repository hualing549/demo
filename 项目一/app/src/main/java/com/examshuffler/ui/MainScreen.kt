package com.examshuffler.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examshuffler.model.Question
import com.examshuffler.viewmodel.QuestionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: QuestionViewModel,
    onStartFloating: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val questions by viewModel.allQuestions.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("题目乱序") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FAB: Floating icon
                SmallFloatingActionButton(
                    onClick = onStartFloating,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "启动悬浮图标")
                }

                // FAB: Start practice
                FloatingActionButton(
                    onClick = {
                        if (questions.isNotEmpty()) {
                            viewModel.startPractice()
                            onNavigateToPractice()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = "乱序练习")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header stats bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "共 $totalCount 道题",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (questions.isNotEmpty()) {
                        TextButton(onClick = {
                            viewModel.startPractice()
                            onNavigateToPractice()
                        }) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("开始乱序练习")
                        }
                    }
                }
            }

            if (questions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LibraryBooks,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "题库为空",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "点击下方相机图标启动悬浮图标\n在任意页面点击图标即可截屏识别题目",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(24.dp))
                        FilledTonalButton(onClick = onStartFloating) {
                            Icon(Icons.Default.Videocam, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("启动悬浮图标")
                        }
                    }
                }
            } else {
                // Question list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(questions, key = { it.id }) { question ->
                        QuestionCard(
                            question = question,
                            onDelete = { viewModel.deleteQuestion(question.id) }
                        )
                    }

                    item {
                        Spacer(Modifier.height(80.dp)) // Space for FABs
                    }
                }
            }
        }
    }

    // Delete all dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("清空题库") },
            text = { Text("确定要删除所有题目吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllQuestions()
                    showDeleteAllDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = question.type,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = question.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (expanded && question.options != null) {
                Spacer(Modifier.height(8.dp))
                val options = com.google.gson.Gson().fromJson(question.options, List::class.java) as? List<String>
                options?.forEach { option ->
                    Text(
                        text = "  $option",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded && question.answer != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "答案: ${question.answer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
