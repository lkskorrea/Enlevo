package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyGoal
import com.example.data.model.ShoppingItem
import com.example.data.model.UiState
import com.example.ui.theme.MintTertiary
import java.util.Locale

@Composable
fun GoalsScreen(
    goalsState: UiState<List<FamilyGoal>>,
    onAddGoal: (String, String, Double, Double) -> Unit,
    onUpdateGoal: (String, String, String, Double, Double) -> Unit,
    onDeleteGoal: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var iconEmoji by remember { mutableStateOf("🏖️") }
    var totalStr by remember { mutableStateOf("") }
    var savedStr by remember { mutableStateOf("") }

    var goalForDeposit by remember { mutableStateOf<FamilyGoal?>(null) }
    var goalForEdit by remember { mutableStateOf<FamilyGoal?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("goals_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Nova Meta da Família",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = iconEmoji,
                            onValueChange = { iconEmoji = it },
                            label = { Text("Emoji") },
                            modifier = Modifier.width(90.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome da Meta (ex: Férias)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("goal_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = totalStr,
                            onValueChange = { totalStr = it },
                            label = { Text("Valor Total (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("goal_total_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = savedStr,
                            onValueChange = { savedStr = it },
                            label = { Text("Já Guardado (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("goal_saved_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Button(
                        onClick = {
                            val total = totalStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val saved = savedStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && total > 0) {
                                onAddGoal(name, iconEmoji.ifBlank { "🎯" }, total, saved)
                                name = ""
                                totalStr = ""
                                savedStr = ""
                            }
                        },
                        enabled = name.isNotBlank() && (totalStr.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_goal_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Meta")
                    }
                }
            }
        }

        when (goalsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item { ErrorBanner(goalsState.message) }
            }
            is UiState.Success -> {
                if (goalsState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "🎯",
                            title = "Nenhuma meta cadastrada",
                            subtitle = "Adicione uma meta acima para acompanhar o progresso das conquistas da família!"
                        )
                    }
                } else {
                    items(goalsState.data, key = { it.id }) { goal ->
                        val rawProgress = if (goal.targetAmount > 0) {
                            (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                        } else 0f
                        val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "goal_progress")
                        val percentage = (rawProgress * 100).toInt()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = goal.iconEmoji, fontSize = 26.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = goal.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Criada por ${goal.authorName} • $percentage%",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = String.format(Locale.getDefault(), "R$ %.0f / R$ %.0f", goal.savedAmount, goal.targetAmount),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MintTertiary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    color = MintTertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = { goalForDeposit = goal },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Savings,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Guardar Valor")
                                    }

                                    Row {
                                        IconButton(onClick = { goalForEdit = goal }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Editar meta",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Excluir meta",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (goalForDeposit != null) {
        val current = goalForDeposit!!
        var addAmountStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { goalForDeposit = null },
            title = { Text("Guardar valor em ${current.iconEmoji} ${current.name}") },
            text = {
                OutlinedTextField(
                    value = addAmountStr,
                    onValueChange = { addAmountStr = it },
                    label = { Text("Valor a somar (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val extra = addAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (extra > 0) {
                            onUpdateGoal(
                                current.id,
                                current.name,
                                current.iconEmoji,
                                current.targetAmount,
                                current.savedAmount + extra
                            )
                        }
                        goalForDeposit = null
                    }
                ) {
                    Text("Somar ao Saldo")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalForDeposit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (goalForEdit != null) {
        val current = goalForEdit!!
        var editName by remember(current) { mutableStateOf(current.name) }
        var editEmoji by remember(current) { mutableStateOf(current.iconEmoji) }
        var editTotal by remember(current) { mutableStateOf(current.targetAmount.toString()) }
        var editSaved by remember(current) { mutableStateOf(current.savedAmount.toString()) }

        AlertDialog(
            onDismissRequest = { goalForEdit = null },
            title = { Text("Editar Meta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editEmoji,
                        onValueChange = { editEmoji = it },
                        label = { Text("Emoji") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nome da Meta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTotal,
                        onValueChange = { editTotal = it },
                        label = { Text("Valor Total (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSaved,
                        onValueChange = { editSaved = it },
                        label = { Text("Valor Já Guardado (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val total = editTotal.replace(",", ".").toDoubleOrNull() ?: current.targetAmount
                        val saved = editSaved.replace(",", ".").toDoubleOrNull() ?: current.savedAmount
                        onUpdateGoal(current.id, editName, editEmoji, total, saved)
                        goalForEdit = null
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalForEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ShoppingScreen(
    shoppingState: UiState<List<ShoppingItem>>,
    onAddItem: (String, String) -> Unit,
    onToggleItem: (String, Boolean) -> Unit,
    onUpdateItem: (String, String, String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf("") }
    var itemToEdit by remember { mutableStateOf<ShoppingItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shopping_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Adicionar à Lista de Compras",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Ex: Pão, Leite, Frutas") },
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("shopping_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = itemQuantity,
                            onValueChange = { itemQuantity = it },
                            label = { Text("Qtd / Obs") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                onAddItem(itemName, itemQuantity)
                                itemName = ""
                                itemQuantity = ""
                            }
                        },
                        enabled = itemName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_shopping_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adicionar Item")
                    }
                }
            }
        }

        when (shoppingState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item { ErrorBanner(shoppingState.message) }
            }
            is UiState.Success -> {
                if (shoppingState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "🛒",
                            title = "Lista de compras vazia",
                            subtitle = "Adicione itens acima para que qualquer familiar veja no mercado em tempo real."
                        )
                    }
                } else {
                    items(shoppingState.data, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.completed) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.completed,
                                    onCheckedChange = { checked ->
                                        onToggleItem(item.id, checked)
                                    }
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "🛒 ${item.name}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val sub = buildString {
                                        if (item.quantityNote.isNotBlank()) {
                                            append("${item.quantityNote} • ")
                                        }
                                        append("Pedido por ${item.authorName}")
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { itemToEdit = item }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar item",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDeleteItem(item.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remover item",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (itemToEdit != null) {
        val current = itemToEdit!!
        var editName by remember(current) { mutableStateOf(current.name) }
        var editQty by remember(current) { mutableStateOf(current.quantityNote) }
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Editar Item de Compra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nome do Item") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editQty,
                        onValueChange = { editQty = it },
                        label = { Text("Quantidade / Observação") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateItem(current.id, editName, editQty)
                        itemToEdit = null
                    },
                    enabled = editName.trim().isNotEmpty()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
