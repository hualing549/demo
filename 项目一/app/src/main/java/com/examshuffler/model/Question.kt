package com.examshuffler.model

data class Question(
    val id: Long = 0,
    val content: String,          // 题目内容（题干）
    val options: String? = null,  // 选项（A/B/C/D等），JSON格式存储
    val answer: String? = null,   // 正确答案
    val type: String = "未知",     // 题型：单选题、多选题、判断题、填空题
    val sourcePage: String? = null, // 来源页面/截图信息
    val imagePath: String? = null,  // 原始截图路径
    val rawOcrText: String? = null, // OCR原始识别文本
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 用于练习时的展示状态
 */
data class PracticeQuestion(
    val question: Question,
    val userAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val isAnswered: Boolean = false
)

/**
 * 练习会话
 */
data class PracticeSession(
    val questions: List<PracticeQuestion>,
    val currentIndex: Int = 0,
    val totalCount: Int,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val isFinished: Boolean = false
)
