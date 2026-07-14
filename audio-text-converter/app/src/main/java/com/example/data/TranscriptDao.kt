package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcript_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<TranscriptNote>>

    @Query("SELECT * FROM transcript_notes WHERE id = :id")
    suspend fun getNoteById(id: Int): TranscriptNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TranscriptNote): Long

    @Update
    suspend fun updateNote(note: TranscriptNote)

    @Query("DELETE FROM transcript_notes WHERE id = :id")
    suspend fun deleteNote(id: Int)

    @Query("UPDATE transcript_notes SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, isSynced: Boolean)
}
