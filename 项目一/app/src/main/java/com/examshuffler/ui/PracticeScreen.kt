package com.examshuffler.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examshuffler.model.PracticeSession
import com.examshuffler.viewmodel.QuestionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: QuestionViewModel,
    onBack: () -> Unit
) {
    val session by viewModel.practiceSession.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("乱序练习") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.endPractice()
                        onBack()
                    }) {
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
        val currentSession = session
        if (currentSession == null) {
            // No active session - show start options
            PracticeStartScreen(
                viewModel = viewModel,
                onBack = onBack,
                modifier = Modifier.padding(padding)
            )
        } else if (currentSession.isFinished) {
            // Results
            PracticeResultScreen(
                session = currentSession,
                onRestart = { viewModel.startPractice() },
                onBack = {
                    viewModel.endPractice()
                    onBack()
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            // Active question
            PracticeQuestionScreen(
                session = currentSession,
                onSubmitAnswer = { answer -> viewModel.submitAnswer(answer) },
                onNext = { viewModel.nextQuestion() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun PracticeStartScreen(
    viewModel: QuestionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val types by viewModel.questionTypes.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Icon(
            Icons.Default.Shuffle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "开始乱序练习",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "共 $totalCount 道题，题目将随机排列",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        // Practice all
        Button(
            onClick = {
                viewModel.startPractice()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = totalCount > 0
        ) {
            Text("全部题目乱序练习")
        }

        Spacer(Modifier.height(16.dp))

        // By type
        if (types.isNotEmpty()) {
            Text(
                text = "按题型练习",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            types.forEach { type ->
                OutlinedButton(
                    onClick = { viewModel.startPractice(typeFilter = type) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(type)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PracticeQuestionScreen(
    session: PracticeSession,
    onSubmitAnswer: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentQ = session.questions[session.currentIndex]
    var userInput by remember(session.currentIndex) { mutableStateOf("") }

    val options = currentQ.question.options?.let {
        com.google.gson.Gson().fromJson(it, List::class.java) as? List<String>
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = (session.currentIndex + 1).toFloat() / session.totalCount,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Progress text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${session.currentIndex + 1} / ${session.totalCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "正确: ${session.correctCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))

        // Question type badge
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = currentQ.question.type,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(12.dp))

        // Question content
        Text(
            text = currentQ.question.content,
            style = MaterialTheme.typography.titleMedium,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(16.dp))

        // Options (if multiple choice)
        if (options != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isSelected = userInput.isNotEmpty() &&
                            option.startsWith(userInput.first().toString(), ignoreCase = true)
                    val isAnswered = currentQ.isAnswered

                    if (!isAnswered) {
                        OutlinedCard(
                            onClick = {
                                val letter = option.first().toString()
                                userInput = letter
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (isSelected)
                                CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ) else CardDefaults.outlinedCardColors()
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        // Show result
                        val letter = option.first().toString()
                        val isCorrectAnswer = letter.equals(currentQ.question.answer, ignoreCase = true)
                        val isUserChoice = letter.equals(currentQ.userAnswer, ignoreCase = true)

                        val cardColor = when {
                            isCorrectAnswer -> MaterialTheme.colorScheme.primaryContainer
                            isUserChoice && !isCorrectAnswer -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val label = when {
                            isCorrectAnswer -> " ✓ 正确答案"
                            isUserChoice -> " ✗"
                            else -> ""
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardColor)
                        ) {
                            Text(
                                text = option + label,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCorrectAnswer || isUserChoice) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        } else {
            // Free text input for non-multiple-choice
            if (!currentQ.isAnswered) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("输入答案") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (userInput.isNotBlank()) {
                            onSubmitAnswer(userInput)
                        }
                    })
                )
            } else {
                // Show answer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentQ.isCorrect == true)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "你的答案: ${currentQ.userAnswer}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (currentQ.question.answer != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "正确答案: ${currentQ.question.answer}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom action button
        if (!currentQ.isAnswered) {
            Button(
                onClick = { onSubmitAnswer(userInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = userInput.isNotBlank()
            ) {
                Text("提交答案")
            }
        } else {
            Button(
                onClick = {
                    onNext()
                    userInput = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (session.currentIndex + 1 >= session.totalCount) "查看结果"
                    else "下一题"
                )
            }
        }
    }
}

@Composable
private fun PracticeResultScreen(
    session: PracticeSession,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rate = if (session.totalCount > 0) {
        session.correctCount * 100 / session.totalCount
    } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = if (rate >= 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = "练习完成！",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("总题数", style = MaterialTheme.typography.bodyMedium)
                    Text("${session.totalCount}", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("正确", color = MaterialTheme.colorScheme.primary)
                    Text("${session.correctCount}", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("错误", color = MaterialTheme.colorScheme.error)
                    Text("${session.incorrectCount}", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "正确率",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${rate}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (rate >= 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
            Button(onClick = onRestart) {
                Text("再来一次")
            }
        }

        // Wrong questions review
        if (session.incorrectCount > 0) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "错题回顾",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                session.questions.filter { it.isCorrect == false }.forEach { q ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = q.question.content,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2
                            )
                            Text(
                                text = "正确答案: ${q.question.answer}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
