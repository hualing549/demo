package com.examshuffler.parser

import com.examshuffler.model.Question
import com.google.gson.Gson

/**
 * 试题解析器，从OCR文本中提取题目和答案
 * 支持多种试题格式：
 * - 单选题：1. 题干 A. 选项 B. 选项 ... 答案：A
 * - 多选题：1. 题干 A. 选项 ... 答案：ABC
 * - 判断题：1. 题干 答案：√/×
 * - 填空题：1. ____ 答案：内容
 */
class QuestionParser(
    private val enableAiParser: Boolean = false,
    private val apiKey: String? = null,
    private val apiEndpoint: String = "https://api.openai.com/v1/chat/completions"
) {

    private val gson = Gson()

    fun parse(ocrText: String): List<Question> {
        val questions = mutableListOf<Question>()

        // 优先使用AI解析（如果启用）
        if (enableAiParser && !apiKey.isNullOrBlank()) {
            // AI解析为异步操作，此处使用同步规则解析
        }

        // 规则解析
        questions.addAll(parseByRules(ocrText))

        return questions
    }

    private fun parseByRules(text: String): List<Question> {
        val questions = mutableListOf<Question>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 按题目编号分割（支持 数字. 和 数字、 格式）
        val questionBlocks = splitIntoQuestionBlocks(lines)

        for (block in questionBlocks) {
            val question = parseSingleQuestion(block)
            if (question != null) {
                questions.add(question)
            }
        }

        // 如果没有按编号解析成功，尝试用答案行反推
        if (questions.isEmpty()) {
            questions.addAll(parseByAnswerPattern(lines))
        }

        return questions
    }

    private fun splitIntoQuestionBlocks(lines: List<String>): List<List<String>> {
        // 匹配题目编号： "1." "1、" "(1)" "一、" "①"
        val questionStartRegex = Regex(
            """^(?:\d+[\.、）)]|[（(]\d+[）)]|[一二三四五六七八九十]+[、]|①|②|③|④|【.*?题】)""".trimMargin()
        )
        // 匹配答案行
        val answerRegex = Regex("""^(?:答案|正确答案|参考答案)[：:]\s*""")

        val blocks = mutableListOf<MutableList<String>>()
        var currentBlock = mutableListOf<String>()

        for (line in lines) {
            if (questionStartRegex.containsMatchIn(line) && !answerRegex.containsMatchIn(line)) {
                if (currentBlock.isNotEmpty()) {
                    blocks.add(currentBlock)
                }
                currentBlock = mutableListOf(line)
            } else {
                currentBlock.add(line)
            }
        }
        if (currentBlock.isNotEmpty()) {
            blocks.add(currentBlock)
        }

        return blocks
    }

    private fun parseSingleQuestion(block: List<String>): Question? {
        if (block.isEmpty()) return null

        val fullText = block.joinToString("\n")
        val content = StringBuilder()
        val optionLines = mutableListOf<String>()
        var answer: String? = null
        var inOptions = false

        for (line in block) {
            // Check for answer line
            val answerMatch = Regex("""^(?:答案|正确答案|参考答案)[：:]\s*(.*)""").find(line)
            if (answerMatch != null) {
                answer = answerMatch.groupValues[1].trim()
                continue
            }

            // Check for option line
            val optionMatch = Regex("""^([A-Za-z①②③④])[.、）)]?\s*(.*)""").find(line)
            if (optionMatch != null) {
                inOptions = true
                optionLines.add(line)
            } else if (!inOptions) {
                if (content.isNotEmpty()) content.append("\n")
                content.append(line)
            }
        }

        val questionContent = content.toString().trim()
        if (questionContent.isBlank()) return null

        // 清理编号前缀
        val cleanContent = questionContent.replace(Regex("""^\d+[\.、）)]?\s*"""), "").trim()
        val optionsJson = if (optionLines.isNotEmpty()) {
            gson.toJson(optionLines)
        } else null

        // 判断题型
        val type = detectType(optionLines, answer, cleanContent)

        return Question(
            content = cleanContent,
            options = optionsJson,
            answer = answer,
            type = type,
            sourcePage = null,
            imagePath = null,
            rawOcrText = fullText
        )
    }

    private fun detectType(optionLines: List<String>, answer: String?, content: String): String {
        if (optionLines.size >= 2) {
            // Check if it's multiple choice (answer has multiple letters)
            if (answer != null && answer.length > 1 && answer.all { it.isLetter() }) {
                return "多选题"
            }
            return "单选题"
        }
        if (content.contains("___") || content.contains("____") || content.contains("（ ）") || content.contains("( )")) {
            return "填空题"
        }
        if (answer != null && (answer.contains("√") || answer.contains("×") || answer.contains("正确") || answer.contains("错误"))) {
            return "判断题"
        }
        if (optionLines.size == 1) {
            return "单选题"
        }
        return "问答题"
    }

    private fun parseByAnswerPattern(lines: List<String>): List<Question> {
        val questions = mutableListOf<Question>()
        var currentContent = StringBuilder()
        var currentAnswer: String? = null

        for (line in lines) {
            val answerMatch = Regex("""^(?:答案|正确答案|参考答案)[：:]\s*(.*)""").find(line)
            if (answerMatch != null) {
                currentAnswer = answerMatch.groupValues[1].trim()
                if (currentContent.isNotBlank()) {
                    questions.add(
                        Question(
                            content = currentContent.toString().trim(),
                            answer = currentAnswer,
                            type = "未知"
                        )
                    )
                    currentContent = StringBuilder()
                    currentAnswer = null
                }
            } else {
                if (currentAnswer == null) {
                    if (currentContent.isNotEmpty()) currentContent.append("\n")
                    currentContent.append(line)
                }
            }
        }

        // 处理最后一题
        if (currentContent.isNotBlank() && currentAnswer != null) {
            questions.add(
                Question(
                    content = currentContent.toString().trim(),
                    answer = currentAnswer,
                    type = "未知"
                )
            )
        }

        return questions
    }

    /**
     * AI解析器（需要网络和API Key）
     */
    suspend fun parseWithAI(ocrText: String): List<Question> {
        if (apiKey.isNullOrBlank()) return emptyList()

        return try {
            val prompt = """
                你是一个试题解析助手。请从以下OCR文本中提取所有题目和答案。
                每个题目请提取：content（题干）, options（选项数组）, answer（答案）, type（题型）。
                题型分类：单选题、多选题、判断题、填空题、问答题。
                
                请以JSON格式返回，格式为：
                {
                    "questions": [
                        {
                            "content": "题干内容",
                            "options": ["A. 选项1", "B. 选项2"],
                            "answer": "A",
                            "type": "单选题"
                        }
                    ]
                }
                
                OCR文本：
                $ocrText
            """.trimIndent()

            val requestBody = gson.toJson(mapOf(
                "model" to "gpt-3.5-turbo",
                "messages" to listOf(
                    mapOf("role" to "system", "content" to "你是一个试题解析助手，擅长从文本中提取试题。"),
                    mapOf("role" to "user", "content" to prompt)
                ),
                "temperature" to 0.3
            ))

            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url(apiEndpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json")!!,
                    requestBody
                ))
                .build()

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val body = response.body()?.string()
                if (body != null) {
                    parseAIResponse(body)
                } else emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseAIResponse(jsonResponse: String): List<Question> {
        return try {
            val root = gson.fromJson(jsonResponse, Map::class.java)
            val choices = root["choices"] as? List<Map<String, Any>> ?: return emptyList()
            val message = choices.firstOrNull()?.get("message") as? Map<String, Any> ?: return emptyList()
            val content = message["content"] as? String ?: return emptyList()

            // Extract JSON from response (handle markdown code blocks)
            val jsonStr = content.substringAfter("```json").substringBefore("```")
                .substringAfter("```").trim()
                .ifBlank { content.trim() }

            val parsed = gson.fromJson(jsonStr, Map::class.java)
            val questionsList = parsed["questions"] as? List<Map<String, Any>> ?: return emptyList()

            questionsList.mapNotNull { q ->
                try {
                    Question(
                        content = q["content"] as? String ?: return@mapNotNull null,
                        options = (q["options"] as? List<*>)?.let {
                            gson.toJson(it)
                        },
                        answer = q["answer"] as? String,
                        type = q["type"] as? String ?: "未知"
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
