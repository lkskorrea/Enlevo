package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BirthdayItem
import com.example.data.model.CalendarEvent
import com.example.data.model.FamilyDocument
import com.example.data.model.FamilyGoal
import com.example.data.model.FamilyMessage
import com.example.data.model.FamilyPhoto
import com.example.data.model.FamilyProfile
import com.example.data.model.ShoppingItem
import com.example.data.model.UiState
import com.example.data.model.UsefulContact
import com.example.data.repository.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FamilyScreen(val title: String) {
    HOME("Início"),
    CALENDARIO("O Nosso Mês"),
    MENSAGENS("Mural / Chat"),
    METAS("Metas da Família"),
    COMPRAS("Lista de Compras"),
    ANIVERSARIOS("Aniversários"),
    TELEFONES("Telefones Úteis"),
    FOTOS("Álbuns de Fotos"),
    DOCUMENTOS("Documentos")
}

class FamilyViewModel(
    private val repository: FamilyRepository,
    val currentUserId: String,
    private val defaultDisplayName: String
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(FamilyScreen.HOME)
    val currentScreen: StateFlow<FamilyScreen> = _currentScreen.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog: StateFlow<Boolean> = _showProfileDialog.asStateFlow()

    val profileState: StateFlow<UiState<FamilyProfile?>> = repository.observeProfile(currentUserId)
        .map<FamilyProfile?, UiState<FamilyProfile?>> { profile ->
            if (profile == null) {
                _showProfileDialog.value = true
            }
            UiState.Success(profile)
        }
        .catch { error ->
            Log.w(TAG, "Erro ao observar perfil", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar perfil"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val calendarEventsState: StateFlow<UiState<List<CalendarEvent>>> = repository.observeCalendarEvents()
        .map<List<CalendarEvent>, UiState<List<CalendarEvent>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Erro ao observar calendário", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar calendário"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val messagesState: StateFlow<UiState<List<FamilyMessage>>> = repository.observeMessages()
        .map<List<FamilyMessage>, UiState<List<FamilyMessage>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Erro ao observar mensagens", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar mural"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val goalsState: StateFlow<UiState<List<FamilyGoal>>> = repository.observeGoals()
        .map<List<FamilyGoal>, UiState<List<FamilyGoal>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Erro ao observar metas", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar metas"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val shoppingItemsState: StateFlow<UiState<List<ShoppingItem>>> = repository.observeShoppingItems()
        .map<List<ShoppingItem>, UiState<List<ShoppingItem>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Erro ao observar compras", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar compras"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val birthdaysState: StateFlow<UiState<List<BirthdayItem>>> = repository.observeBirthdays()
        .map<List<BirthdayItem>, UiState<List<BirthdayItem>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Erro ao observar aniversários", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar aniversários"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val contactsState: StateFlow<UiState<List<UsefulContact>>> = repository.observeContacts()
        .map<List<UsefulContact>, UiState<List<UsefulContact>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Erro ao observar telefones", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar telefones"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val photosState: StateFlow<UiState<List<FamilyPhoto>>> = repository.observePhotos()
        .map<List<FamilyPhoto>, UiState<List<FamilyPhoto>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Erro ao observar fotos", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar fotos"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val documentsState: StateFlow<UiState<List<FamilyDocument>>> = repository.observeDocuments()
        .map<List<FamilyDocument>, UiState<List<FamilyDocument>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Erro ao observar documentos", error)
            emit(UiState.Error(error.localizedMessage ?: "Falha ao carregar documentos"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun currentAuthorName(): String {
        val profile = (profileState.value as? UiState.Success)?.data
        return profile?.displayName?.takeIf { it.isNotBlank() }
            ?: defaultDisplayName.takeIf { it.isNotBlank() }
            ?: "Familiar"
    }

    fun currentAuthorEmoji(): String {
        val profile = (profileState.value as? UiState.Success)?.data
        return profile?.avatarEmoji?.takeIf { it.isNotBlank() } ?: "😊"
    }

    fun navigateTo(screen: FamilyScreen) {
        _currentScreen.value = screen
    }

    fun openProfileEditor() {
        _showProfileDialog.value = true
    }

    fun closeProfileEditor() {
        _showProfileDialog.value = false
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun saveProfile(displayName: String, avatarEmoji: String) {
        viewModelScope.launch {
            repository.saveProfile(displayName, avatarEmoji)
                .onSuccess {
                    _showProfileDialog.value = false
                    _feedbackMessage.value = "Perfil familiar atualizado!"
                }
                .onFailure { e ->
                    _feedbackMessage.value = "Erro ao salvar perfil: ${e.localizedMessage ?: "Tente novamente"}"
                }
        }
    }

    // Calendar Actions
    fun addCalendarEvent(day: Int, month: Int, year: Int, description: String) {
        if (description.isBlank()) return
        viewModelScope.launch {
            repository.addCalendarEvent(day, month, year, description, currentAuthorName())
                .onSuccess { _feedbackMessage.value = "Evento adicionado no dia $day!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao criar evento: ${e.localizedMessage}" }
        }
    }

    fun updateCalendarEvent(eventId: String, description: String) {
        if (description.isBlank()) return
        viewModelScope.launch {
            repository.updateCalendarEvent(eventId, description)
                .onSuccess { _feedbackMessage.value = "Evento atualizado!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao atualizar evento: ${e.localizedMessage}" }
        }
    }

    fun deleteCalendarEvent(eventId: String) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(eventId)
                .onSuccess { _feedbackMessage.value = "Evento removido." }
                .onFailure { e -> _feedbackMessage.value = "Erro ao remover evento: ${e.localizedMessage}" }
        }
    }

    // Message Actions
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(text, currentAuthorName(), currentAuthorEmoji())
                .onFailure { e -> _feedbackMessage.value = "Erro ao enviar mensagem: ${e.localizedMessage}" }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
                .onSuccess { _feedbackMessage.value = "Mensagem apagada." }
                .onFailure { e -> _feedbackMessage.value = "Apenas o autor pode apagar esta mensagem." }
        }
    }

    // Goal Actions
    fun addGoal(name: String, iconEmoji: String, targetAmount: Double, savedAmount: Double) {
        if (name.isBlank() || targetAmount <= 0) return
        viewModelScope.launch {
            repository.addGoal(name, iconEmoji, targetAmount, savedAmount, currentAuthorName())
                .onSuccess { _feedbackMessage.value = "Meta adicionada!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao criar meta: ${e.localizedMessage}" }
        }
    }

    fun updateGoal(goalId: String, name: String, iconEmoji: String, targetAmount: Double, savedAmount: Double) {
        if (name.isBlank() || targetAmount <= 0) return
        viewModelScope.launch {
            repository.updateGoal(goalId, name, iconEmoji, targetAmount, savedAmount)
                .onSuccess { _feedbackMessage.value = "Meta atualizada!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao atualizar meta: ${e.localizedMessage}" }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
                .onSuccess { _feedbackMessage.value = "Meta excluída." }
                .onFailure { e -> _feedbackMessage.value = "Erro ao excluir meta: ${e.localizedMessage}" }
        }
    }

    // Shopping Actions
    fun addShoppingItem(name: String, quantityNote: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addShoppingItem(name, quantityNote, currentAuthorName())
                .onSuccess { _feedbackMessage.value = "Item adicionado à lista!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao adicionar item: ${e.localizedMessage}" }
        }
    }

    fun toggleShoppingItem(itemId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleShoppingItemCompleted(itemId, completed)
                .onFailure { e -> _feedbackMessage.value = "Erro ao atualizar item: ${e.localizedMessage}" }
        }
    }

    fun updateShoppingItem(itemId: String, name: String, quantityNote: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateShoppingItem(itemId, name, quantityNote)
                .onSuccess { _feedbackMessage.value = "Item atualizado!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao atualizar item: ${e.localizedMessage}" }
        }
    }

    fun deleteShoppingItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteShoppingItem(itemId)
                .onSuccess { _feedbackMessage.value = "Item removido da lista." }
                .onFailure { e -> _feedbackMessage.value = "Erro ao remover item: ${e.localizedMessage}" }
        }
    }

    // Birthday Actions
    fun addBirthday(personName: String, day: Int, monthName: String) {
        if (personName.isBlank()) return
        viewModelScope.launch {
            repository.addBirthday(personName, day, monthName, currentAuthorName())
                .onSuccess { _feedbackMessage.value = "Aniversário registrado!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao salvar aniversário: ${e.localizedMessage}" }
        }
    }

    fun updateBirthday(birthdayId: String, personName: String, day: Int, monthName: String) {
        if (personName.isBlank()) return
        viewModelScope.launch {
            repository.updateBirthday(birthdayId, personName, day, monthName)
                .onSuccess { _feedbackMessage.value = "Aniversário atualizado!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao atualizar aniversário: ${e.localizedMessage}" }
        }
    }

    fun deleteBirthday(birthdayId: String) {
        viewModelScope.launch {
            repository.deleteBirthday(birthdayId)
                .onSuccess { _feedbackMessage.value = "Aniversário removido." }
                .onFailure { e -> _feedbackMessage.value = "Erro ao remover aniversário: ${e.localizedMessage}" }
        }
    }

    // Contact Actions
    fun addContact(name: String, phoneNumber: String, category: String) {
        if (name.isBlank() || phoneNumber.isBlank()) return
        viewModelScope.launch {
            repository.addContact(name, phoneNumber, category, currentAuthorName())
                .onSuccess { _feedbackMessage.value = "Contato adicionado!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao salvar contato: ${e.localizedMessage}" }
        }
    }

    fun updateContact(contactId: String, name: String, phoneNumber: String, category: String) {
        if (name.isBlank() || phoneNumber.isBlank()) return
        viewModelScope.launch {
            repository.updateContact(contactId, name, phoneNumber, category)
                .onSuccess { _feedbackMessage.value = "Contato atualizado!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao atualizar contato: ${e.localizedMessage}" }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
                .onSuccess { _feedbackMessage.value = "Contato removido." }
                .onFailure { e -> _feedbackMessage.value = "Erro ao remover contato: ${e.localizedMessage}" }
        }
    }

    // Photo Actions
    fun addPhoto(caption: String, imageBase64: String) {
        if (imageBase64.isBlank()) return
        viewModelScope.launch {
            repository.addPhoto(caption, imageBase64, currentAuthorName())
                .onSuccess { _feedbackMessage.value = "Foto publicada no álbum da família!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao enviar foto: ${e.localizedMessage}" }
        }
    }

    fun updatePhotoCaption(photoId: String, caption: String) {
        viewModelScope.launch {
            repository.updatePhotoCaption(photoId, caption)
                .onSuccess { _feedbackMessage.value = "Legenda atualizada!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao atualizar legenda: ${e.localizedMessage}" }
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            repository.deletePhoto(photoId)
                .onSuccess { _feedbackMessage.value = "Foto excluída." }
                .onFailure { e -> _feedbackMessage.value = "Erro ao excluir foto: ${e.localizedMessage}" }
        }
    }

    // Document Actions
    fun addDocument(title: String, notesOrUrl: String, fileName: String, fileSummary: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addDocument(title, notesOrUrl, fileName, fileSummary, currentAuthorName())
                .onSuccess { _feedbackMessage.value = "Documento guardado!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao salvar documento: ${e.localizedMessage}" }
        }
    }

    fun updateDocument(docId: String, title: String, notesOrUrl: String, fileName: String, fileSummary: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updateDocument(docId, title, notesOrUrl, fileName, fileSummary)
                .onSuccess { _feedbackMessage.value = "Documento atualizado!" }
                .onFailure { e -> _feedbackMessage.value = "Erro ao atualizar documento: ${e.localizedMessage}" }
        }
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch {
            repository.deleteDocument(docId)
                .onSuccess { _feedbackMessage.value = "Documento removido." }
                .onFailure { e -> _feedbackMessage.value = "Erro ao remover documento: ${e.localizedMessage}" }
        }
    }

    private companion object {
        const val TAG = "FamilyVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
