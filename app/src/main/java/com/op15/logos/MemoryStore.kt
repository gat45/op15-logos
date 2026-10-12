package com.op15.logos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

/**
 * Local, typed memory. It deliberately keeps claimed, observed and verified
 * information separate: a model response cannot become proof by being stored.
 */
data class MemoryRecord(
    val id: Long,
    val kind: String,
    val content: String,
    val status: String,
    val source: String,
    val createdAt: Long
)

class LogosMemoryDb(context: Context) : SQLiteOpenHelper(context, "logos_memory.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                content TEXT NOT NULL,
                status TEXT NOT NULL,
                source TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX memories_kind_time ON memories(kind, created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun append(kind: String, content: String, status: String, source: String): Long {
        require(kind in setOf("turn", "note", "hypothesis", "evidence", "experiment", "action"))
        require(status in setOf("declared", "unverified", "verified", "refuted", "proposed"))
        return writableDatabase.insertOrThrow(
            "memories", null,
            ContentValues().apply {
                put("kind", kind)
                put("content", content.trim())
                put("status", status)
                put("source", source)
                put("created_at", System.currentTimeMillis())
            }
        )
    }

    fun turns(limit: Int = 100): List<Turn> = query("kind = ?", arrayOf("turn"), limit)
        .asReversed().map { Turn(it.source, it.content, it.createdAt) }

    fun records(limit: Int = 100): List<MemoryRecord> = query("kind != ?", arrayOf("turn"), limit)

    fun clearTurns() = writableDatabase.delete("memories", "kind = ?", arrayOf("turn")).let { Unit }

    private fun query(selection: String, args: Array<String>, limit: Int): List<MemoryRecord> {
        return readableDatabase.query(
            "memories", arrayOf("id", "kind", "content", "status", "source", "created_at"),
            selection, args, null, null, "created_at DESC", limit.toString()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    MemoryRecord(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getLong(5)
                    )
                )
            }
        }
    }
}
