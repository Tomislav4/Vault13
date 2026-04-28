package io.github.tomislav4.vault13

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class NoteRepository(private val context: Context) {
    private val gson = Gson()
    private val fileName = "encrypted_notes.dat"
    private val file = File(context.filesDir, fileName)

    fun saveNotes(notes: List<Note>) {
        val json = gson.toJson(notes)
        val encrypted = SecurityManager.encrypt(json)
        file.writeText(encrypted)
    }

    fun loadNotes(): List<Note> {
        if (!file.exists()) return emptyList()
        return try {
            val encrypted = file.readText()
            val decrypted = SecurityManager.decrypt(encrypted)
            val type = object : TypeToken<List<Note>>() {}.type
            gson.fromJson(decrypted, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
