package com.examshuffler.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "options") val options: String? = null,
    @ColumnInfo(name = "answer") val answer: String? = null,
    @ColumnInfo(name = "type") val type: String = "未知",
    @ColumnInfo(name = "source_page") val sourcePage: String? = null,
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    @ColumnInfo(name = "raw_ocr_text") val rawOcrText: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
