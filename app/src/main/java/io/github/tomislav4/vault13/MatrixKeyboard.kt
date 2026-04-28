package io.github.tomislav4.vault13

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tomislav4.vault13.ui.theme.MatrixBlack
import io.github.tomislav4.vault13.ui.theme.MatrixGreen

@Composable
fun MatrixKeyboard(
    onKeyClick: (String) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M", ",", ".", "?")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MatrixBlack)
            .padding(4.dp)
            .border(1.dp, MatrixGreen.copy(alpha = 0.5f))
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    KeyButton(text = key, onClick = { onKeyClick(key) }, modifier = Modifier.weight(1f))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeyButton(text = "SPACE", onClick = onSpace, modifier = Modifier.weight(3f))
            KeyButton(text = "DEL", onClick = onDelete, modifier = Modifier.weight(1f))
            KeyButton(text = "ENTER", onClick = onEnter, modifier = Modifier.weight(1.5f))
        }
    }
}

@Composable
fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(45.dp)
            .border(0.5.dp, MatrixGreen.copy(alpha = 0.7f))
            .background(MatrixBlack)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MatrixGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}
