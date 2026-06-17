package com.examshuffler.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions ORDER BY created_at DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY created_at DESC")
    suspend fun getAllQuestionsOnce(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Insert
    suspend fun insert(question: QuestionEntity): Long

    @Insert
    suspend fun insertAll(questions: List<QuestionEntity>): List<Long>

    @Update
    suspend fun update(question: QuestionEntity)

    @Delete
    suspend fun delete(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    @Query("SELECT DISTINCT type FROM questions ORDER BY type")
    fun getAllTypes(): Flow<List<String>>
}
