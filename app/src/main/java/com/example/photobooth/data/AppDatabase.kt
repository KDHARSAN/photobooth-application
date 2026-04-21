package com.example.photobooth.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "photo_history")
data class PhotoRecord(
    @PrimaryKey val id: String, 
    val userId: String, // Added for security isolation
    val localUri: String,
    val remoteUrl: String?, 
    val timestamp: Long,
    val filterUsed: String,
    val isSynced: Boolean = false
)

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistory(userId: String): Flow<List<PhotoRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PhotoRecord)

    @Query("UPDATE photo_history SET remoteUrl = :remoteUrl, isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String, remoteUrl: String)
    
    @Delete
    suspend fun deleteRecord(record: PhotoRecord)
}

@Database(entities = [PhotoRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photobooth_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
