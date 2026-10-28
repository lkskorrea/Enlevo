package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val DEFAULT_FAMILY_ID = "familia_principal"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class FamilyProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val avatarEmoji: String = "😊",
    val familyId: String = DEFAULT_FAMILY_ID,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CalendarEvent(
    @DocumentId val id: String = "",
    val familyId: String = DEFAULT_FAMILY_ID,
    val day: Long = 1L,
    val month: Long = 0L,
    val year: Long = 2026L,
    val description: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FamilyMessage(
    @DocumentId val id: String = "",
    val familyId: String = DEFAULT_FAMILY_ID,
    val text: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorEmoji: String = "💬",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FamilyGoal(
    @DocumentId val id: String = "",
    val familyId: String = DEFAULT_FAMILY_ID,
    val name: String = "",
    val iconEmoji: String = "🎯",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val authorUid: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShoppingItem(
    @DocumentId val id: String = "",
    val familyId: String = DEFAULT_FAMILY_ID,
    val name: String = "",
    val quantityNote: String = "",
    val completed: Boolean = false,
    val authorUid: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BirthdayItem(
    @DocumentId val id: String = "",
    val familyId: String = DEFAULT_FAMILY_ID,
    val personName: String = "",
    val day: Long = 1L,
    val monthName: String = "Janeiro",
    val authorUid: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UsefulContact(
    @DocumentId val id: String = "",
    val familyId: String = DEFAULT_FAMILY_ID,
    val name: String = "",
    val phoneNumber: String = "",
    val category: String = "Geral",
    val authorUid: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FamilyPhoto(
    @DocumentId val id: String = "",
    val familyId: String = DEFAULT_FAMILY_ID,
    val caption: String = "",
    val imageBase64: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FamilyDocument(
    @DocumentId val id: String = "",
    val familyId: String = DEFAULT_FAMILY_ID,
    val title: String = "",
    val notesOrUrl: String = "",
    val fileName: String = "",
    val fileSummary: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
