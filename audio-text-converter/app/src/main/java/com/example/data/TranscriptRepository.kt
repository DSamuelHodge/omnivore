package com.example.data

import kotlinx.coroutines.flow.Flow

class TranscriptRepository(private val transcriptDao: TranscriptDao) {
    val allNotes: Flow<List<TranscriptNote>> = transcriptDao.getAllNotes()

    suspend fun getNoteById(id: Int): TranscriptNote? {
        return transcriptDao.getNoteById(id)
    }

    suspend fun insert(note: TranscriptNote): Long {
        return transcriptDao.insertNote(note)
    }

    suspend fun update(note: TranscriptNote) {
        transcriptDao.updateNote(note)
    }

    suspend fun delete(id: Int) {
        transcriptDao.deleteNote(id)
    }

    suspend fun updateSyncStatus(id: Int, isSynced: Boolean) {
        transcriptDao.updateSyncStatus(id, isSynced)
    }
}
