package com.examshuffler.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.examshuffler.database.AppDatabase
import com.examshuffler.model.PracticeQuestion
import com.examshuffler.model.PracticeSession
import com.examshuffler.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuestionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).questionDao()

    // 所有题目
    val allQuestions: StateFlow<List<Question>> = dao.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 题型列表
    val questionTypes: StateFlow<List<String>> = dao.getAllTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 练习会话
    private val _practiceSession = MutableStateFlow<PracticeSession?>(null)
    val practiceSession: StateFlow<PracticeSession?> = _practiceSession.asStateFlow()

    // 总题目数
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    init {
        viewModelScope.launch {
            _totalCount.value = dao.count()
        }
    }

    fun refreshCount() {
        viewModelScope.launch {
            _totalCount.value = dao.count()
        }
    }

    /**
     * 开始乱序练习
     */
    fun startPractice(typeFilter: String? = null) {
        viewModelScope.launch {
            val entities = if (typeFilter != null) {
                dao.getAllQuestionsOnce().filter { it.type == typeFilter }
            } else {
                dao.getAllQuestionsOnce()
            }

            val questions = entities.shuffled().map {
                Question(
                    id = it.id,
                    content = it.content,
                    options = it.options,
                    answer = it.answer,
                    type = it.type,
                    sourcePage = it.sourcePage,
                    imagePath = it.imagePath,
                    rawOcrText = it.rawOcrText,
                    createdAt = it.createdAt
                )
            }

            _practiceSession.value = PracticeSession(
                questions = questions.map { PracticeQuestion(question = it) },
                currentIndex = 0,
                totalCount = questions.size,
                correctCount = 0,
                incorrectCount = 0,
                isFinished = false
            )
        }
    }

    /**
     * 提交答案
     */
    fun submitAnswer(answer: String) {
        val session = _practiceSession.value ?: return
        if (session.isFinished) return

        val currentQ = session.questions[session.currentIndex]
        val isCorrect = currentQ.question.answer?.let { correctAnswer ->
            answer.trim().equals(correctAnswer.trim(), ignoreCase = true)
        } ?: false

        val updatedQuestions = session.questions.toMutableList()
        updatedQuestions[session.currentIndex] = currentQ.copy(
            userAnswer = answer,
            isCorrect = isCorrect,
            isAnswered = true
        )

        val nextIndex = session.currentIndex + 1
        val isFinished = nextIndex >= session.totalCount

        _practiceSession.value = session.copy(
            questions = updatedQuestions,
            currentIndex = if (isFinished) session.currentIndex else nextIndex,
            correctCount = session.correctCount + if (isCorrect) 1 else 0,
            incorrectCount = session.incorrectCount + if (isCorrect) 0 else 1,
            isFinished = isFinished
        )
    }

    /**
     * 跳到下一题
     */
    fun nextQuestion() {
        val session = _practiceSession.value ?: return
        if (session.isFinished) return

        val nextIndex = session.currentIndex + 1
        if (nextIndex >= session.totalCount) {
            _practiceSession.value = session.copy(isFinished = true)
        } else {
            _practiceSession.value = session.copy(currentIndex = nextIndex)
        }
    }

    /**
     * 结束练习
     */
    fun endPractice() {
        _practiceSession.value = null
    }

    /**
     * 删除题目
     */
    fun deleteQuestion(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
            _totalCount.value = dao.count()
        }
    }

    /**
     * 清空所有题目
     */
    fun deleteAllQuestions() {
        viewModelScope.launch {
            dao.deleteAll()
            _totalCount.value = 0
        }
    }
}
