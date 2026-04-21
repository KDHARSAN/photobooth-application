package com.example.photobooth.data

import android.net.Uri
import android.util.Log
import com.example.photobooth.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class SyncRepository(
    private val authManager: AuthManager,
    private val photoDao: PhotoDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun saveAndSyncPhotoStrip(
        localUri: Uri,
        filterUsed: String
    ): PhotoRecord? {
        val user = authManager.auth.currentUser ?: return null
        val uid = user.uid
        val recordId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // 1. Create Local Record
        val record = PhotoRecord(
            id = recordId,
            userId = uid, // Ensure record is tied to current user
            localUri = localUri.toString(),
            remoteUrl = null,
            timestamp = timestamp,
            filterUsed = filterUsed,
            isSynced = false
        )
        photoDao.insertRecord(record)

        // 2. Upload to Firebase Storage
        val storageRef = storage.reference.child("users/$uid/strips/$recordId.jpg")
        
        return withContext(Dispatchers.IO) {
            try {
                val uploadTask = storageRef.putFile(localUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // 3. Mark in Firestore
                val docData = hashMapOf(
                    "id" to recordId,
                    "remoteUrl" to downloadUrl,
                    "timestamp" to timestamp,
                    "filterUsed" to filterUsed
                )
                db.collection("users").document(uid).collection("strips")
                    .document(recordId).set(docData).await()

                // 4. Update Local DB
                photoDao.markAsSynced(recordId, downloadUrl)
                record.copy(remoteUrl = downloadUrl, isSynced = true)
            } catch (e: Exception) {
                Log.e("SyncRepo", "Error syncing photo: ${e.message}", e)
                record // Return unsynced record
            }
        }
    }

    suspend fun deletePhotoStrip(record: PhotoRecord) {
        val user = authManager.auth.currentUser ?: return
        val uid = user.uid
        val recordId = record.id

        withContext(Dispatchers.IO) {
            try {
                // 1. Delete from Firebase Storage
                val storageRef = storage.reference.child("users/$uid/strips/$recordId.jpg")
                storageRef.delete().await()

                // 2. Delete from Firestore
                db.collection("users").document(uid).collection("strips")
                    .document(recordId).delete().await()
                
                // 3. Delete Local File (if exists)
                val file = File(Uri.parse(record.localUri).path ?: "")
                if (file.exists()) {
                    file.delete()
                }

                // 4. Delete from Room DB
                photoDao.deleteRecord(record)
            } catch (e: Exception) {
                Log.e("SyncRepo", "Error deleting photo: ${e.message}", e)
                // Even if remote deletion fails, we'll try to delete locally to ensure UI updates
                photoDao.deleteRecord(record)
            }
        }
    }

    suspend fun fetchRemoteHistory() {
        val user = authManager.auth.currentUser ?: return
        val uid = user.uid

        try {
            val snapshot = db.collection("users").document(uid).collection("strips")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()

            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: continue
                val remoteUrl = doc.getString("remoteUrl") ?: continue
                val timestamp = doc.getLong("timestamp") ?: 0L
                val filterUsed = doc.getString("filterUsed") ?: "Normal"

                // Insert into Room DB (Assuming Coil will load from remoteUrl if localUri is a placeholder)
                // Note: Realistically, you'd check if it already exists before inserting to avoid overwriting localUri
                val record = PhotoRecord(
                    id = id,
                    userId = uid, // Ensure record is tied to current user
                    localUri = remoteUrl, 
                    remoteUrl = remoteUrl,
                    timestamp = timestamp,
                    filterUsed = filterUsed,
                    isSynced = true
                )
                photoDao.insertRecord(record)
            }
        } catch (e: Exception) {
            Log.e("SyncRepo", "Error fetching remote history: ${e.message}", e)
        }
    }
}
