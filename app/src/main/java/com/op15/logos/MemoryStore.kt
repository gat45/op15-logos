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

data class MemoryLink(val id: Long, val fromId: Long, val toId: Long, val relation: String, val createdAt: Long)

class LogosMemoryDb(context: Context) : SQLiteOpenHelper(context, "logos_memory.db", null, 2) {
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
        createRelations(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createRelations(db)
    }

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

    fun link(fromId: Long, toId: Long, relation: String): Long {
        require(fromId > 0 && toId > 0 && fromId != toId)
        require(relation in setOf("supporte", "contredit", "depend_de"))
        return writableDatabase.insertOrThrow("memory_relations", null, ContentValues().apply {
            put("from_id", fromId); put("to_id", toId); put("relation", relation); put("created_at", System.currentTimeMillis())
        })
    }

    fun links(limit: Int = 100): List<MemoryLink> = readableDatabase.query(
        "memory_relations", arrayOf("id", "from_id", "to_id", "relation", "created_at"),
        null, null, null, null, "created_at DESC", limit.toString()
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(MemoryLink(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2), cursor.getString(3), cursor.getLong(4))) } }

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

    private fun createRelations(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS memory_relations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                from_id INTEGER NOT NULL,
                to_id INTEGER NOT NULL,
                relation TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(from_id, to_id, relation)
            )"""
        )
    }
}
