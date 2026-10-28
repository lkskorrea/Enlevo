package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.UiState
import com.example.data.repository.FamilyRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BirthdaysScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.FamilyProfileDialog
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.PhotosScreen
import com.example.ui.screens.ShoppingScreen
import com.example.ui.screens.signOut
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FamilyScreen
import com.example.ui.viewmodel.FamilyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = { /* AuthStateListener atualiza currentUser automaticamente */ }
        )
    } else {
        AuthenticatedFamilyApp(
            currentUserId = user.uid,
            defaultDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Familiar"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedFamilyApp(
    currentUserId: String,
    defaultDisplayName: String
) {
    val viewModel: FamilyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FamilyViewModel(
                    repository = FamilyRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val showProfileDialog by viewModel.showProfileDialog.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val eventsState by viewModel.calendarEventsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val goalsState by viewModel.goalsState.collectAsStateWithLifecycle()
    val shoppingState by viewModel.shoppingItemsState.collectAsStateWithLifecycle()
    val birthdaysState by viewModel.birthdaysState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val photosState by viewModel.photosState.collectAsStateWithLifecycle()
    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    if (currentScreen != FamilyScreen.HOME) {
        BackHandler {
            viewModel.navigateTo(FamilyScreen.HOME)
        }
    }

    val currentProfile = (profileState as? UiState.Success)?.data
    val authorName = currentProfile?.displayName?.takeIf { it.isNotBlank() } ?: defaultDisplayName
    val authorEmoji = currentProfile?.avatarEmoji?.takeIf { it.isNotBlank() } ?: "😊"

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentScreen == FamilyScreen.HOME) "App Família" else currentScreen.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    if (currentScreen != FamilyScreen.HOME) {
                        IconButton(
                            onClick = { viewModel.navigateTo(FamilyScreen.HOME) },
                            modifier = Modifier.testTag("back_to_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar ao Início"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openProfileEditor() },
                        modifier = Modifier.testTag("open_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Editar Perfil Familiar"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sair da conta"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val pillTabs = listOf(
                    Triple(FamilyScreen.HOME, "Início", Icons.Default.Home),
                    Triple(FamilyScreen.MENSAGENS, "Mural", Icons.AutoMirrored.Filled.Chat),
                    Triple(FamilyScreen.CALENDARIO, "Agenda", Icons.Default.CalendarMonth),
                    Triple(FamilyScreen.METAS, "Metas", Icons.Default.Flag)
                )
                pillTabs.forEach { (screen, label, icon) ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { viewModel.navigateTo(screen) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                FamilyScreen.HOME -> HomeScreen(
                    authorName = authorName,
                    authorEmoji = authorEmoji,
                    eventsState = eventsState,
                    goalsState = goalsState,
                    shoppingState = shoppingState,
                    birthdaysState = birthdaysState,
                    onNavigate = { viewModel.navigateTo(it) },
                    onEditProfile = { viewModel.openProfileEditor() }
                )
                FamilyScreen.CALENDARIO -> CalendarScreen(
                    eventsState = eventsState,
                    onAddEvent = { d, m, y, desc -> viewModel.addCalendarEvent(d, m, y, desc) },
                    onUpdateEvent = { id, desc -> viewModel.updateCalendarEvent(id, desc) },
                    onDeleteEvent = { id -> viewModel.deleteCalendarEvent(id) }
                )
                FamilyScreen.MENSAGENS -> MessagesScreen(
                    currentUserId = currentUserId,
                    messagesState = messagesState,
                    onSendMessage = { text -> viewModel.sendMessage(text) },
                    onDeleteMessage = { id -> viewModel.deleteMessage(id) }
                )
                FamilyScreen.METAS -> GoalsScreen(
                    goalsState = goalsState,
                    onAddGoal = { name, emoji, total, saved -> viewModel.addGoal(name, emoji, total, saved) },
                    onUpdateGoal = { id, name, emoji, total, saved -> viewModel.updateGoal(id, name, emoji, total, saved) },
                    onDeleteGoal = { id -> viewModel.deleteGoal(id) }
                )
                FamilyScreen.COMPRAS -> ShoppingScreen(
                    shoppingState = shoppingState,
                    onAddItem = { name, qty -> viewModel.addShoppingItem(name, qty) },
                    onToggleItem = { id, done -> viewModel.toggleShoppingItem(id, done) },
                    onUpdateItem = { id, name, qty -> viewModel.updateShoppingItem(id, name, qty) },
                    onDeleteItem = { id -> viewModel.deleteShoppingItem(id) }
                )
                FamilyScreen.ANIVERSARIOS -> BirthdaysScreen(
                    birthdaysState = birthdaysState,
                    onAddBirthday = { name, day, month -> viewModel.addBirthday(name, day, month) },
                    onUpdateBirthday = { id, name, day, month -> viewModel.updateBirthday(id, name, day, month) },
                    onDeleteBirthday = { id -> viewModel.deleteBirthday(id) }
                )
                FamilyScreen.TELEFONES -> ContactsScreen(
                    contactsState = contactsState,
                    onAddContact = { name, phone, cat -> viewModel.addContact(name, phone, cat) },
                    onUpdateContact = { id, name, phone, cat -> viewModel.updateContact(id, name, phone, cat) },
                    onDeleteContact = { id -> viewModel.deleteContact(id) }
                )
                FamilyScreen.FOTOS -> PhotosScreen(
                    photosState = photosState,
                    onAddPhoto = { cap, b64 -> viewModel.addPhoto(cap, b64) },
                    onUpdateCaption = { id, cap -> viewModel.updatePhotoCaption(id, cap) },
                    onDeletePhoto = { id -> viewModel.deletePhoto(id) },
                    onFeedback = { msg -> viewModel.showFeedback(msg) }
                )
                FamilyScreen.DOCUMENTOS -> DocumentsScreen(
                    documentsState = documentsState,
                    onAddDocument = { t, n, fn, fs -> viewModel.addDocument(t, n, fn, fs) },
                    onUpdateDocument = { id, t, n, fn, fs -> viewModel.updateDocument(id, t, n, fn, fs) },
                    onDeleteDocument = { id -> viewModel.deleteDocument(id) }
                )
            }
        }
    }

    if (showProfileDialog) {
        FamilyProfileDialog(
            initialName = authorName,
            initialEmoji = authorEmoji,
            isDismissible = currentProfile != null,
            onDismiss = { viewModel.closeProfileEditor() },
            onSave = { newName, newEmoji ->
                viewModel.saveProfile(newName, newEmoji)
            }
        )
    }
}
