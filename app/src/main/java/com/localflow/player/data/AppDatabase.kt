package com.localflow.player.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites", primaryKeys = ["mediaId", "kind"])
data class FavoriteEntity(val mediaId: Long, val kind: String, val addedAt: Long = System.currentTimeMillis())

@Entity(tableName = "playlists")
data class PlaylistEntity(@androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "playlist_entries", primaryKeys = ["playlistId", "mediaId", "kind"])
data class PlaylistEntryEntity(val playlistId: Long, val mediaId: Long, val kind: String, val position: Int)

@Dao interface LibraryDao {
    @Query("SELECT * FROM favorites") fun favorites(): Flow<List<FavoriteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addFavorite(item: FavoriteEntity)
    @Delete suspend fun removeFavorite(item: FavoriteEntity)
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") fun playlists(): Flow<List<PlaylistEntity>>
    @Insert suspend fun createPlaylist(item: PlaylistEntity): Long
    @Query("UPDATE playlists SET name = :name WHERE id = :id") suspend fun renamePlaylist(id: Long, name: String)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: Long)
    @Query("DELETE FROM playlist_entries WHERE playlistId = :id") suspend fun deleteEntries(id: Long)
    @Query("SELECT * FROM playlist_entries WHERE playlistId = :id ORDER BY position") suspend fun entries(id: Long): List<PlaylistEntryEntity>
    @Query("SELECT * FROM playlist_entries ORDER BY position") fun allEntries(): Flow<List<PlaylistEntryEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addEntry(item: PlaylistEntryEntity)
    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND mediaId = :mediaId AND kind = :kind") suspend fun removeEntry(playlistId: Long, mediaId: Long, kind: String)
    @Transaction suspend fun addMedia(id: Long, media: List<PlaylistEntryEntity>) {
        var position = (entries(id).maxOfOrNull { it.position } ?: -1) + 1
        media.distinctBy { it.mediaId to it.kind }.forEach { addEntry(it.copy(playlistId = id, position = position++)) }
    }
    @Transaction suspend fun deleteCollection(id: Long) { deleteEntries(id); deletePlaylist(id) }
    @Query("SELECT COUNT(*) FROM favorites WHERE mediaId = :id AND kind = :kind") suspend fun isFavorite(id: Long, kind: String): Int
    @Transaction suspend fun toggleFavorite(item: FavoriteEntity) { if (isFavorite(item.mediaId, item.kind) > 0) removeFavorite(item) else addFavorite(item) }
}

@Database(entities = [FavoriteEntity::class, PlaylistEntity::class, PlaylistEntryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() { abstract fun libraryDao(): LibraryDao }
