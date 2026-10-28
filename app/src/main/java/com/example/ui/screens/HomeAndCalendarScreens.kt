package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BirthdayItem
import com.example.data.model.CalendarEvent
import com.example.data.model.FamilyGoal
import com.example.data.model.ShoppingItem
import com.example.data.model.UiState
import com.example.ui.theme.CardBirthdaysBg
import com.example.ui.theme.CardCalendarBg
import com.example.ui.theme.CardChatBg
import com.example.ui.theme.CardContactsBg
import com.example.ui.theme.CardDocsBg
import com.example.ui.theme.CardGoalsBg
import com.example.ui.theme.CardPhotosBg
import com.example.ui.theme.CardShoppingBg
import com.example.ui.theme.RoseSecondary
import com.example.ui.viewmodel.FamilyScreen
import java.util.Calendar

private data class HomeMenuCardItem(
    val screen: FamilyScreen,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val backgroundColor: Color,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun HomeScreen(
    authorName: String,
    authorEmoji: String,
    eventsState: UiState<List<CalendarEvent>>,
    goalsState: UiState<List<FamilyGoal>>,
    shoppingState: UiState<List<ShoppingItem>>,
    birthdaysState: UiState<List<BirthdayItem>>,
    onNavigate: (FamilyScreen) -> Unit,
    onEditProfile: () -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour in 5..11 -> "Bom dia"
        hour in 12..18 -> "Boa tarde"
        else -> "Boa noite"
    }

    val pendingShoppingCount = (shoppingState as? UiState.Success)?.data?.count { !it.completed } ?: 0
    val goalsCount = (goalsState as? UiState.Success)?.data?.size ?: 0
    val currentMonthEventsCount = (eventsState as? UiState.Success)?.data?.count {
        it.month.toInt() == calendar.get(Calendar.MONTH) && it.year.toInt() == calendar.get(Calendar.YEAR)
    } ?: 0
    val birthdaysCount = (birthdaysState as? UiState.Success)?.data?.size ?: 0

    val menuItems = remember( currentMonthEventsCount, goalsCount, pendingShoppingCount, birthdaysCount ) {
        listOf(
            HomeMenuCardItem(
                FamilyScreen.CALENDARIO, "📅", "Calendário",
                "$currentMonthEventsCount eventos no mês", CardCalendarBg, Icons.Default.CalendarMonth, "menu_calendario"
            ),
            HomeMenuCardItem(
                FamilyScreen.MENSAGENS, "💬", "Mensagens",
                "Mural e chat", CardChatBg, Icons.AutoMirrored.Filled.Chat, "menu_mensagens"
            ),
            HomeMenuCardItem(
                FamilyScreen.METAS, "🎯", "Metas",
                "$goalsCount metas ativas", CardGoalsBg, Icons.Default.Flag, "menu_metas"
            ),
            HomeMenuCardItem(
                FamilyScreen.COMPRAS, "🛒", "Compras",
                "$pendingShoppingCount pendentes", CardShoppingBg, Icons.Default.ShoppingCart, "menu_compras"
            ),
            HomeMenuCardItem(
                FamilyScreen.ANIVERSARIOS, "🎂", "Aniversários",
                "$birthdaysCount datas salvas", CardBirthdaysBg, Icons.Default.Cake, "menu_aniversarios"
            ),
            HomeMenuCardItem(
                FamilyScreen.TELEFONES, "📞", "Telefones",
                "WhatsApp e chamadas", CardContactsBg, Icons.Default.Phone, "menu_telefones"
            ),
            HomeMenuCardItem(
                FamilyScreen.FOTOS, "🖼️", "Fotos",
                "Álbum compartilhado", CardPhotosBg, Icons.Default.PhotoLibrary, "menu_fotos"
            ),
            HomeMenuCardItem(
                FamilyScreen.DOCUMENTOS, "📄", "Documentos",
                "Arquivos e notas", CardDocsBg, Icons.Default.Description, "menu_documentos"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditProfile() }
                    .testTag("greeting_profile_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting,",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$authorName! $authorEmoji",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toque para personalizar seu apelido ou ícone familiar",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = authorEmoji, fontSize = 26.sp)
                        }
                    }
                }
            }
        }

        // Grade de 8 cartões em pares de 2 colunas
        items(menuItems.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { item ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(item.screen) }
                            .testTag(item.tag),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = item.backgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 22.dp, horizontal = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = item.emoji, fontSize = 34.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CalendarScreen(
    eventsState: UiState<List<CalendarEvent>>,
    onAddEvent: (Int, Int, Int, String) -> Unit,
    onUpdateEvent: (String, String) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    val todayCal = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableIntStateOf(todayCal.get(Calendar.DAY_OF_MONTH)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }

    val monthNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )
    val weekDays = listOf("D", "S", "T", "Q", "Q", "S", "S")

    val monthCal = remember(currentMonth, currentYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeekOffset = monthCal.get(Calendar.DAY_OF_WEEK) - 1

    val allEvents = (eventsState as? UiState.Success)?.data ?: emptyList()
    val monthEvents = allEvents.filter {
        it.month.toInt() == currentMonth && it.year.toInt() == currentYear
    }
    val daysWithEvents = monthEvents.map { it.day.toInt() }.toSet()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentMonth == 0) {
                                    currentMonth = 11
                                    currentYear -= 1
                                } else {
                                    currentMonth -= 1
                                }
                                selectedDay = 1
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
                        }

                        Text(
                            text = "${monthNames[currentMonth]} $currentYear",
                            style = MaterialTheme.typography.titleLarge
                        )

                        IconButton(
                            onClick = {
                                if (currentMonth == 11) {
                                    currentMonth = 0
                                    currentYear += 1
                                } else {
                                    currentMonth += 1
                                }
                                selectedDay = 1
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cabeçalho dos dias da semana
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekDays.forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val totalCells = firstDayOfWeekOffset + daysInMonth
                    val rows = (totalCells + 6) / 7
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - firstDayOfWeekOffset + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val isSelected = dayNumber == selectedDay
                                    val hasEvent = daysWithEvents.contains(dayNumber)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    hasEvent -> CardCalendarBg
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                                }
                                            )
                                            .clickable {
                                                selectedDay = dayNumber
                                            }
                                            .testTag("cal_day_$dayNumber"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (hasEvent) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else RoseSecondary)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Eventos de ${monthNames[currentMonth]}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Dia selecionado: $selectedDay de ${monthNames[currentMonth]}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("add_calendar_event_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dia $selectedDay")
                }
            }
        }

        when (eventsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    ErrorBanner(message = eventsState.message)
                }
            }
            is UiState.Success -> {
                if (monthEvents.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "📅",
                            title = "Nenhum evento neste mês",
                            subtitle = "Toque em um dia do calendário e clique em '+ Dia $selectedDay' para agendar um compromisso da família."
                        )
                    }
                } else {
                    items(monthEvents, key = { it.id }) { event ->
                        val isSelectedDay = event.day.toInt() == selectedDay
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelectedDay) CardCalendarBg else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Dia ${event.day}: ${event.description}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Por ${event.authorName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(onClick = { eventToEdit = event }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar evento",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteEvent(event.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Excluir evento",
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

    if (showAddDialog) {
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Novo Evento — Dia $selectedDay/${currentMonth + 1}") },
            text = {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição do evento (ex: Almoço de Domingo)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_event_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddEvent(selectedDay, currentMonth, currentYear, desc)
                        showAddDialog = false
                    },
                    enabled = desc.trim().isNotEmpty(),
                    modifier = Modifier.testTag("confirm_calendar_event_button")
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (eventToEdit != null) {
        val currentEvent = eventToEdit!!
        var desc by remember(currentEvent) { mutableStateOf(currentEvent.description) }
        AlertDialog(
            onDismissRequest = { eventToEdit = null },
            title = { Text("Editar Evento — Dia ${currentEvent.day}") },
            text = {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição do evento") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateEvent(currentEvent.id, desc)
                        eventToEdit = null
                    },
                    enabled = desc.trim().isNotEmpty()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmptyStateCard(
    emoji: String,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}
