package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MessageRole
import com.example.data.model.StudyMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeSessions(userId: String): Flow<List<ChatSession>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/sessions"
        emitAll(
            db.collection("users").document(userId).collection("sessions")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(userId: String, sessionId: String): Flow<List<ChatMessage>> = flow {
        require(userId.isNotBlank() && sessionId.isNotBlank()) { "IDs must not be blank" }
        val path = "users/$userId/sessions/$sessionId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("sessions").document(sessionId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createSession(
        title: String = "New Conversation",
        mode: StudyMode = StudyMode.GENERAL,
        sessionId: String = UUID.randomUUID().toString()
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().ifBlank { "New Conversation" }.take(120)
            val docRef = db.collection("users").document(uid)
                .collection("sessions").document(sessionId)
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "mode" to mode.name,
                "lastMessagePreview" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(sessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSessionMetadata(
        sessionId: String,
        title: String? = null,
        mode: StudyMode? = null,
        lastMessagePreview: String? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            require(sessionId.isNotBlank()) { "sessionId must not be blank" }
            val docRef = db.collection("users").document(uid)
                .collection("sessions").document(sessionId)
            docPath = docRef.path

            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (title != null) {
                updates["title"] = title.trim().ifBlank { "New Conversation" }.take(120)
            }
            if (mode != null) {
                updates["mode"] = mode.name
            }
            if (lastMessagePreview != null) {
                updates["lastMessagePreview"] = lastMessagePreview.trim().take(300)
            }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            require(sessionId.isNotBlank()) { "sessionId must not be blank" }
            val sessionRef = db.collection("users").document(uid)
                .collection("sessions").document(sessionId)
            docPath = sessionRef.path

            val messagesSnap = sessionRef.collection("messages").get().await()
            if (!messagesSnap.isEmpty) {
                val batch = db.batch()
                for (doc in messagesSnap.documents) {
                    batch.delete(doc.reference)
                }
                batch.delete(sessionRef)
                batch.commit().await()
            } else {
                sessionRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addMessage(
        sessionId: String,
        role: MessageRole,
        content: String,
        mode: StudyMode,
        attachmentNames: List<String> = emptyList(),
        errorFlag: Boolean = false,
        messageId: String = UUID.randomUUID().toString()
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sessions/$sessionId/messages/$messageId"
        return try {
            val uid = requireUserId()
            require(sessionId.isNotBlank()) { "sessionId must not be blank" }
            val safeContent = content.take(32000)
            val safeAttachments = attachmentNames.take(4).map { it.take(120) }

            val docRef = db.collection("users").document(uid)
                .collection("sessions").document(sessionId)
                .collection("messages").document(messageId)
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "sessionId" to sessionId,
                "role" to role.name,
                "content" to safeContent,
                "mode" to mode.name,
                "attachmentNames" to safeAttachments,
                "errorFlag" to errorFlag,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(messageId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(sessionId: String, messageId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sessions/$sessionId/messages/$messageId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("sessions").document(sessionId)
                .collection("messages").document(messageId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
