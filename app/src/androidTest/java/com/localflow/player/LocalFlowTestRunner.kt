package com.localflow.player

import android.app.Instrumentation
import android.os.Bundle
import androidx.room.Room
import com.localflow.player.data.*
import kotlinx.coroutines.runBlocking

/** Device tests use an isolated in-memory database; production user data is untouched. */
class LocalFlowTestRunner : Instrumentation() {
    private var arguments=Bundle()
    override fun onCreate(arguments: Bundle?) { this.arguments=arguments ?: Bundle(); start() }
    override fun onStart() {
        val result=Bundle()
        try {
            val db=Room.inMemoryDatabaseBuilder(targetContext,AppDatabase::class.java).build()
            try { runBlocking {
                val dao=db.libraryDao()
                val id=dao.createPlaylist(PlaylistEntity(name="Teste"))
                dao.addMedia(id,listOf(PlaylistEntryEntity(id,1,"AUDIO",0),PlaylistEntryEntity(id,2,"AUDIO",0)))
                dao.addMedia(id,listOf(PlaylistEntryEntity(id,1,"AUDIO",0)))
                check(dao.entries(id).map { it.mediaId }==listOf(1L,2L)) { "Duplicação ou reordenação indevida" }
                dao.removeEntry(id,1,"AUDIO")
                dao.addMedia(id,listOf(PlaylistEntryEntity(id,3,"VIDEO",0)))
                check(dao.entries(id).map { it.position }.distinct().size==2) { "Posições duplicadas após remoção" }
                dao.renamePlaylist(id,"Renomeada")
                val favorite=FavoriteEntity(3,"VIDEO")
                dao.toggleFavorite(favorite); check(dao.isFavorite(3,"VIDEO")==1)
                dao.toggleFavorite(favorite); check(dao.isFavorite(3,"VIDEO")==0)
                dao.deleteCollection(id); check(dao.entries(id).isEmpty())
                result.putString("database","PASS: create, add, duplicate, remove, order, rename, favorites, delete")
            } } finally { db.close() }
            if(arguments.getString("fixtures")=="true") {
                TestMedia.create(targetContext)
                result.putString("fixtures","Created audio with embedded artwork and video with audio in Music and Movies/LocalFlow-QA")
            }
            result.putString("stream","\nLocalFlow device checks PASSED\n"+result.getString("database"))
            finish(android.app.Activity.RESULT_OK,result)
        } catch(error: Throwable) {
            result.putString("stream",error.stackTraceToString()); finish(android.app.Activity.RESULT_CANCELED,result)
        }
    }
}
