package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BirthdayItem
import com.example.data.model.CalendarEvent
import com.example.data.model.DEFAULT_FAMILY_ID
import com.example.data.model.FamilyDocument
import com.example.data.model.FamilyGoal
import com.example.data.model.FamilyMessage
import com.example.data.model.FamilyPhoto
import com.example.data.model.FamilyProfile
import com.example.data.model.ShoppingItem
import com.example.data.model.UsefulContact
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FamilyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário deve estar autenticado com Google antes de acessar o banco de dados.")
    }

    // --- 1. PERFIL DO MEMBRO ---

    fun observeProfile(userId: String): Flow<FamilyProfile?> {
        val path = "profiles/$userId"
        return db.collection("profiles").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        FamilyProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun getProfileByUserId(userId: String): Result<FamilyProfile?> = runCatching {
        val docRef = db.collection("profiles").document(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(FamilyProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveProfile(displayName: String, avatarEmoji: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanName = displayName.trim().take(60).ifEmpty { "Familiar" }
        val cleanEmoji = avatarEmoji.trim().take(16).ifEmpty { "😊" }
        val docRef = db.collection("profiles").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mapOf(
                    "displayName" to cleanName,
                    "avatarEmoji" to cleanEmoji,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "uid" to uid,
                    "displayName" to cleanName,
                    "avatarEmoji" to cleanEmoji,
                    "familyId" to DEFAULT_FAMILY_ID,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- 2. CALENDÁRIO ---

    fun observeCalendarEvents(): Flow<List<CalendarEvent>> {
        val path = "calendar_events"
        return db.collection(path)
            .whereEqualTo("familyId", DEFAULT_FAMILY_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CalendarEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy({ it.year }, { it.month }, { it.day }))
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addCalendarEvent(
        day: Int,
        month: Int,
        year: Int,
        description: String,
        authorName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("calendar_events")
        val payload = mapOf(
            "familyId" to DEFAULT_FAMILY_ID,
            "day" to day.toLong(),
            "month" to month.toLong(),
            "year" to year.toLong(),
            "description" to description.trim().take(200),
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifEmpty { "Familiar" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateCalendarEvent(eventId: String, description: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("calendar_events").document(eventId)
        try {
            docRef.update(
                mapOf(
                    "description" to description.trim().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCalendarEvent(eventId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("calendar_events").document(eventId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 3. MURAL / MENSAGENS ---

    fun observeMessages(): Flow<List<FamilyMessage>> {
        val path = "messages"
        return db.collection(path)
            .whereEqualTo("familyId", DEFAULT_FAMILY_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FamilyMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendMessage(
        text: String,
        authorName: String,
        authorEmoji: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("messages")
        val payload = mapOf(
            "familyId" to DEFAULT_FAMILY_ID,
            "text" to text.trim().take(500),
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifEmpty { "Familiar" },
            "authorEmoji" to authorEmoji.trim().take(16).ifEmpty { "💬" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("messages").document(messageId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 4. METAS DA FAMÍLIA ---

    fun observeGoals(): Flow<List<FamilyGoal>> {
        val path = "goals"
        return db.collection(path)
            .whereEqualTo("familyId", DEFAULT_FAMILY_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FamilyGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addGoal(
        name: String,
        iconEmoji: String,
        targetAmount: Double,
        savedAmount: Double,
        authorName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("goals")
        val payload = mapOf(
            "familyId" to DEFAULT_FAMILY_ID,
            "name" to name.trim().take(100),
            "iconEmoji" to iconEmoji.trim().take(16).ifEmpty { "🎯" },
            "targetAmount" to targetAmount.coerceAtLeast(1.0),
            "savedAmount" to savedAmount.coerceAtLeast(0.0),
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifEmpty { "Familiar" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateGoal(
        goalId: String,
        name: String,
        iconEmoji: String,
        targetAmount: Double,
        savedAmount: Double
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("goals").document(goalId)
        try {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(100),
                    "iconEmoji" to iconEmoji.trim().take(16).ifEmpty { "🎯" },
                    "targetAmount" to targetAmount.coerceAtLeast(1.0),
                    "savedAmount" to savedAmount.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("goals").document(goalId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 5. LISTA DE COMPRAS ---

    fun observeShoppingItems(): Flow<List<ShoppingItem>> {
        val path = "shopping_items"
        return db.collection(path)
            .whereEqualTo("familyId", DEFAULT_FAMILY_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ShoppingItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<ShoppingItem> { it.completed }.thenByDescending { it.createdAt ?: Timestamp(0, 0) })
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addShoppingItem(
        name: String,
        quantityNote: String,
        authorName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("shopping_items")
        val payload = mapOf(
            "familyId" to DEFAULT_FAMILY_ID,
            "name" to name.trim().take(120),
            "quantityNote" to quantityNote.trim().take(60),
            "completed" to false,
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifEmpty { "Familiar" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun toggleShoppingItemCompleted(itemId: String, completed: Boolean): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("shopping_items").document(itemId)
        try {
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateShoppingItem(itemId: String, name: String, quantityNote: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("shopping_items").document(itemId)
        try {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(120),
                    "quantityNote" to quantityNote.trim().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteShoppingItem(itemId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("shopping_items").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 6. ANIVERSÁRIOS ---

    fun observeBirthdays(): Flow<List<BirthdayItem>> {
        val path = "birthdays"
        return db.collection(path)
            .whereEqualTo("familyId", DEFAULT_FAMILY_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BirthdayItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy({ monthOrder(it.monthName) }, { it.day }))
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addBirthday(
        personName: String,
        day: Int,
        monthName: String,
        authorName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("birthdays")
        val payload = mapOf(
            "familyId" to DEFAULT_FAMILY_ID,
            "personName" to personName.trim().take(100),
            "day" to day.coerceIn(1, 31).toLong(),
            "monthName" to monthName.trim().take(30),
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifEmpty { "Familiar" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateBirthday(birthdayId: String, personName: String, day: Int, monthName: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("birthdays").document(birthdayId)
        try {
            docRef.update(
                mapOf(
                    "personName" to personName.trim().take(100),
                    "day" to day.coerceIn(1, 31).toLong(),
                    "monthName" to monthName.trim().take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBirthday(birthdayId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("birthdays").document(birthdayId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 7. TELEFONES ÚTEIS ---

    fun observeContacts(): Flow<List<UsefulContact>> {
        val path = "contacts"
        return db.collection(path)
            .whereEqualTo("familyId", DEFAULT_FAMILY_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UsefulContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addContact(
        name: String,
        phoneNumber: String,
        category: String,
        authorName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("contacts")
        val payload = mapOf(
            "familyId" to DEFAULT_FAMILY_ID,
            "name" to name.trim().take(100),
            "phoneNumber" to phoneNumber.trim().take(40),
            "category" to category.trim().take(40).ifEmpty { "Geral" },
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifEmpty { "Familiar" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateContact(contactId: String, name: String, phoneNumber: String, category: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("contacts").document(contactId)
        try {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(100),
                    "phoneNumber" to phoneNumber.trim().take(40),
                    "category" to category.trim().take(40).ifEmpty { "Geral" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteContact(contactId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("contacts").document(contactId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 8. FOTOS ---

    fun observePhotos(): Flow<List<FamilyPhoto>> {
        val path = "photos"
        return db.collection(path)
            .whereEqualTo("familyId", DEFAULT_FAMILY_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FamilyPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addPhoto(
        caption: String,
        imageBase64: String,
        authorName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("photos")
        val payload = mapOf(
            "familyId" to DEFAULT_FAMILY_ID,
            "caption" to caption.trim().take(120).ifEmpty { "Momento em Família" },
            "imageBase64" to imageBase64.take(580000),
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifEmpty { "Familiar" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updatePhotoCaption(photoId: String, caption: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("photos").document(photoId)
        try {
            docRef.update(
                mapOf(
                    "caption" to caption.trim().take(120).ifEmpty { "Momento em Família" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePhoto(photoId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("photos").document(photoId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 9. DOCUMENTOS ---

    fun observeDocuments(): Flow<List<FamilyDocument>> {
        val path = "documents"
        return db.collection(path)
            .whereEqualTo("familyId", DEFAULT_FAMILY_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FamilyDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addDocument(
        title: String,
        notesOrUrl: String,
        fileName: String,
        fileSummary: String,
        authorName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("documents")
        val payload = mapOf(
            "familyId" to DEFAULT_FAMILY_ID,
            "title" to title.trim().take(120).ifEmpty { "Documento" },
            "notesOrUrl" to notesOrUrl.trim().take(500),
            "fileName" to fileName.trim().take(120),
            "fileSummary" to fileSummary.trim().take(45000),
            "authorUid" to uid,
            "authorName" to authorName.trim().take(60).ifEmpty { "Familiar" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val doc = colRef.add(payload).await()
            doc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateDocument(
        docId: String,
        title: String,
        notesOrUrl: String,
        fileName: String,
        fileSummary: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("documents").document(docId)
        try {
            docRef.update(
                mapOf(
                    "title" to title.trim().take(120).ifEmpty { "Documento" },
                    "notesOrUrl" to notesOrUrl.trim().take(500),
                    "fileName" to fileName.trim().take(120),
                    "fileSummary" to fileSummary.trim().take(45000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDocument(docId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("documents").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    private fun monthOrder(monthName: String): Int {
        return when (monthName.lowercase()) {
            "janeiro" -> 1
            "fevereiro" -> 2
            "março", "marco" -> 3
            "abril" -> 4
            "maio" -> 5
            "junho" -> 6
            "julho" -> 7
            "agosto" -> 8
            "setembro" -> 9
            "outubro" -> 10
            "novembro" -> 11
            "dezembro" -> 12
            else -> 13
        }
    }
}
