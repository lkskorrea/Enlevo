package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndGetProfile_authenticatedOwner_succeedsAndBlocksCrossUserRead() = runBlocking {
        val aliceUid = signInTestUser("alice_family@test.com")
        val aliceRepo = FamilyRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveProfile("Mãe Alice", "👩")
        }
        assertTrue(saveResult.isSuccess)

        val ownProfileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getProfileByUserId(aliceUid)
        }
        assertTrue(ownProfileResult.isSuccess)
        assertEquals("Mãe Alice", ownProfileResult.getOrNull()?.displayName)

        // Sign in as Bob and verify Bob cannot read Alice's private profile document
        signInTestUser("bob_family@test.com")
        val bobRepo = FamilyRepository(firestore, auth)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProfileByUserId(aliceUid)
        }
        assertTrue(crossReadResult.isFailure)
        val firestoreEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun createAndObserveSharedFamilyItems_authenticatedMembers_emitsRealtimeUpdates() = runBlocking {
        signInTestUser("alice_family@test.com")
        val repository = FamilyRepository(firestore, auth)

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addGoal("Férias na Praia", "🏖️", 5000.0, 1200.0, "Mãe Alice").getOrThrow()
        }
        assertTrue(goalId.isNotEmpty())

        val shoppingId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addShoppingItem("Pão Francês", "10 unidades", "Mãe Alice").getOrThrow()
        }
        assertTrue(shoppingId.isNotEmpty())

        // Switch to Bob in the same family and verify Bob can observe and update shared goal & shopping item
        signInTestUser("bob_family@test.com")
        val bobRepo = FamilyRepository(firestore, auth)

        val emittedGoals = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeGoals().first { list -> list.any { it.id == goalId } }
        }
        assertTrue(emittedGoals.any { it.id == goalId && it.name == "Férias na Praia" })

        val updateGoalResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.updateGoal(goalId, "Férias na Praia", "🏖️", 5000.0, 2000.0)
        }
        assertTrue(updateGoalResult.isSuccess)

        val toggleShoppingResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.toggleShoppingItemCompleted(shoppingId, true)
        }
        assertTrue(toggleShoppingResult.isSuccess)
    }

    @Test
    fun observeGoals_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FamilyRepository(firestore, auth)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeGoals().first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }

        assertNotNull("Expected exception when observing goals unauthenticated", caughtException)
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
