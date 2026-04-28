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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
        
        // 3. Cryptographic Warden: Initialize hardware keys
        SecurityManager.init(this)
        
        enableEdgeToEdge()
        setContent {
            Vault13Theme {
                MainContent()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Requirement: Prevent running in background / Ask for password every time
        SecurityManager.clearSession()
        finishAndRemoveTask() // Kill the activity so it must restart and re-auth
    }
}

@Composable
fun MainContent() {
    val isAuthenticated = remember { mutableStateOf(!SecurityManager.isLocked()) }

    if (!isAuthenticated.value) {
        AuthScreen(onAuthenticated = { isAuthenticated.value = true })
    } else {
        NoteScreen()
    }
}

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val isFirstTime = remember { !SecurityManager.isPasswordSet(context) }
    
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MatrixBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                if (isFirstTime) Icons.Default.Warning else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isFirstTime && !isConfirming) Color.Yellow else MatrixGreen,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val titleText = when {
                isFirstTime && !isConfirming -> "INITIALIZE_VAULT_PROTOCOL"
                isFirstTime && isConfirming -> "CONFIRM_SECURITY_PHRASE"
                else -> "IDENTITY_VERIFICATION_REQUIRED"
            }

            Text(
                titleText,
                color = MatrixGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            
            if (isFirstTime && !isConfirming) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "WARNING: NO_RECOVERY_MECHANISM_EXISTS.\nIF_PHRASE_IS_LOST, ALL_DATA_WILL_BE_PERMANENTLY_PURGED.",
                    color = Color.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val currentInput = if (isConfirming) confirmPassword else password

            // Password Display (Masked)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, if (error.isNotEmpty()) Color.Red else MatrixGreen)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "*".repeat(currentInput.length) + if (currentInput.length < 16) "_" else "",
                    color = MatrixGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp
                )
            }
            
            if (error.isNotEmpty()) {
                Text(
                    error,
                    color = Color.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            MatrixKeyboard(
                onKeyClick = { 
                    if (!isConfirming) {
                        if (password.length < 16) password += it
                    } else {
                        if (confirmPassword.length < 16) confirmPassword += it
                    }
                    error = "" 
                },
                onDelete = { 
                    if (!isConfirming) {
                        if (password.isNotEmpty()) password = password.dropLast(1)
                    } else {
                        if (confirmPassword.isNotEmpty()) confirmPassword = confirmPassword.dropLast(1)
                    }
                },
                onSpace = { /* No space in pwd */ },
                onEnter = {
                    if (isFirstTime) {
                        if (!isConfirming) {
                            if (password.length < 4) {
                                error = "PHRASE_TOO_SHORT (MIN 4)"
                            } else {
                                isConfirming = true
                            }
                        } else {
                            if (password == confirmPassword) {
                                SecurityManager.setPassword(password, context)
                                onAuthenticated()
                            } else {
                                error = "MISMATCH_DETECTED"
                                confirmPassword = ""
                                // Optional: reset first password too if you want to be strict
                                // password = ""; isConfirming = false
                            }
                        }
                    } else {
                        if (SecurityManager.verifyPassword(password, context)) {
                            onAuthenticated()
                        } else {
                            error = "ACCESS_DENIED"
                            password = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel = viewModel()) {
    val showAddDialog = remember { mutableStateOf(false) }
    val editingNote = remember { mutableStateOf<Note?>(null) }
    val noteText = remember { mutableStateOf("") }

    // Reload notes once authenticated since they couldn't be decrypted before
    LaunchedEffect(Unit) {
        viewModel.refreshNotes()
    }

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
            if (!viewModel.isSelectionMode.value && !showAddDialog.value) {
                FloatingActionButton(
                    onClick = { 
                        noteText.value = ""
                        editingNote.value = null
                        showAddDialog.value = true 
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
                                    editingNote.value = note
                                    noteText.value = note.content
                                    showAddDialog.value = true
                                }
                            }
                        )
                    }
                }
            }

            if (showAddDialog.value) {
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
                            if (editingNote.value == null) "> NEW_ENTRY" else "> EDIT_ENTRY",
                            color = MatrixGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(1.dp, MatrixGreen)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = noteText.value + "_",
                                color = MatrixGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        MatrixKeyboard(
                            onKeyClick = { noteText.value += it },
                            onDelete = { if (noteText.value.isNotEmpty()) noteText.value = noteText.value.dropLast(1) },
                            onSpace = { noteText.value += " " },
                            onEnter = {
                                if (editingNote.value == null) {
                                    viewModel.addNote(noteText.value)
                                } else {
                                    viewModel.updateNote(editingNote.value!!.id, noteText.value)
                                }
                                editingNote.value = null
                                showAddDialog.value = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { 
                                editingNote.value = null
                                showAddDialog.value = false
                            }) {
                                Text("[ ABORT ]", color = MatrixGreen, fontFamily = FontFamily.Monospace)
                            }
                            
                            TextButton(onClick = {
                                if (editingNote.value == null) {
                                    viewModel.addNote(noteText.value)
                                } else {
                                    viewModel.updateNote(editingNote.value!!.id, noteText.value)
                                }
                                editingNote.value = null
                                showAddDialog.value = false
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
