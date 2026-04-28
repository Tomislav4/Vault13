package io.github.tomislav4.vault13

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteRepository(application)
    
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> get() = _notes

    val selectedNotes = mutableStateListOf<String>()
    val isSelectionMode = mutableStateOf(false)

    init {
        loadNotes()
    }

    private fun loadNotes() {
        _notes.clear()
        _notes.addAll(repository.loadNotes().sortedByDescending { it.timestamp })
    }

    fun addNote(content: String) {
        val newNote = Note(content = content)
        _notes.add(0, newNote)
        repository.saveNotes(_notes)
    }

    fun updateNote(id: String, content: String) {
        val index = _notes.indexOfFirst { it.id == id }
        if (index != -1) {
            val updatedNote = _notes[index].copy(content = content, timestamp = System.currentTimeMillis())
            _notes[index] = updatedNote
            _notes.sortByDescending { it.timestamp }
            repository.saveNotes(_notes)
        }
    }

    fun deleteSelectedNotes() {
        _notes.removeAll { it.id in selectedNotes }
        selectedNotes.clear()
        isSelectionMode.value = false
        repository.saveNotes(_notes)
    }

    fun toggleSelection(id: String) {
        if (selectedNotes.contains(id)) {
            selectedNotes.remove(id)
            if (selectedNotes.isEmpty()) {
                isSelectionMode.value = false
            }
        } else {
            selectedNotes.add(id)
            isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        selectedNotes.clear()
        isSelectionMode.value = false
    }
}
