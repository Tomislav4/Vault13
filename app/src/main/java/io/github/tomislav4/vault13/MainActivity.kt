package io.github.tomislav4.vault13

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tomislav4.vault13.ui.theme.MatrixBlack
import io.github.tomislav4.vault13.ui.theme.MatrixGreen
import io.github.tomislav4.vault13.ui.theme.Vault13Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 4. Local Execution Agent: Implement FLAG_SECURE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // 3. Cryptographic Warden: Initialize with Context for StrongBox check
        SecurityManager.init(this)
        
        enableEdgeToEdge()
        setContent {
            Vault13Theme {
                NoteScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel = viewModel()) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var noteText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VAULT13_TERMINAL", fontFamily = FontFamily.Monospace) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MatrixBlack,
                    titleContentColor = MatrixGreen
                ),
                actions = {
                    if (viewModel.isSelectionMode.value) {
                        IconButton(onClick = { viewModel.deleteSelectedNotes() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MatrixGreen)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!viewModel.isSelectionMode.value && !showAddDialog) {
                FloatingActionButton(
                    onClick = { 
                        noteText = ""
                        editingNote = null
                        showAddDialog = true 
                    },
                    containerColor = MatrixGreen,
                    contentColor = MatrixBlack
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        },
        containerColor = MatrixBlack
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(viewModel.notes, key = { it.id }) { note ->
                        val isSelected = viewModel.selectedNotes.contains(note.id)
                        NoteItem(
                            note = note,
                            isSelected = isSelected,
                            onLongClick = { viewModel.toggleSelection(note.id) },
                            onClick = {
                                if (viewModel.isSelectionMode.value) {
                                    viewModel.toggleSelection(note.id)
                                } else {
                                    editingNote = note
                                    noteText = note.content
                                    showAddDialog = true
                                }
                            }
                        )
                    }
                }
            }

            if (showAddDialog) {
                // 1. Input Isolation Agent: Render overlay to bypass standard UI logging
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MatrixBlack.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            if (editingNote == null) "> NEW_ENTRY" else "> EDIT_ENTRY",
                            color = MatrixGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Terminal Display Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(1.dp, MatrixGreen)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = noteText + "_",
                                color = MatrixGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 1. Input Isolation Agent: Custom Matrix Keyboard (No IME)
                        MatrixKeyboard(
                            onKeyClick = { noteText += it },
                            onDelete = { if (noteText.isNotEmpty()) noteText = noteText.dropLast(1) },
                            onSpace = { noteText += " " },
                            onEnter = {
                                if (editingNote == null) {
                                    viewModel.addNote(noteText)
                                } else {
                                    viewModel.updateNote(editingNote!!.id, noteText)
                                }
                                showAddDialog = false
                                editingNote = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { 
                                showAddDialog = false
                                editingNote = null
                            }) {
                                Text("[ ABORT ]", color = MatrixGreen, fontFamily = FontFamily.Monospace)
                            }
                            
                            TextButton(onClick = {
                                if (editingNote == null) {
                                    viewModel.addNote(noteText)
                                } else {
                                    viewModel.updateNote(editingNote!!.id, noteText)
                                }
                                showAddDialog = false
                                editingNote = null
                            }) {
                                Text("[ COMMIT ]", color = MatrixGreen, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, if (isSelected) MatrixGreen else MatrixGreen.copy(alpha = 0.3f))
            .background(if (isSelected) MatrixGreen.copy(alpha = 0.1f) else MatrixBlack)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.content,
                color = MatrixGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ID: ${note.id.take(8)}... | TIMESTAMP: ${note.timestamp}",
                color = MatrixGreen.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}
