package com.op15.logos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import java.security.MessageDigest

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
data class ClaimRecord(val id: Long, val text: String, val critical: Boolean, val status: String, val evidenceIds: List<Long>, val createdAt: Long)
data class AuditEvent(val id: Long, val timestamp: Long, val action: String, val details: String, val previousHash: String, val eventHash: String)
data class AuditReport(val valid: Boolean, val message: String)
data class SessionRecord(val id: Long, val title: String, val state: String, val createdAt: Long, val updatedAt: Long)

class LogosMemoryDb(context: Context) : SQLiteOpenHelper(context, "logos_memory.db", null, 4) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                content TEXT NOT NULL,
                status TEXT NOT NULL,
                source TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                session_id INTEGER NOT NULL DEFAULT 1
            )"""
        )
        db.execSQL("CREATE INDEX memories_kind_time ON memories(kind, created_at DESC)")
        createRelations(db)
        createGovernance(db)
        createSessions(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createRelations(db)
        if (oldVersion < 3) createGovernance(db)
        if (oldVersion < 4) {
            createSessions(db)
            db.execSQL("ALTER TABLE memories ADD COLUMN session_id INTEGER NOT NULL DEFAULT 1")
        }
    }

    fun append(kind: String, content: String, status: String, source: String, sessionId: Long = 1): Long {
        require(kind in setOf("turn", "note", "hypothesis", "evidence", "experiment", "action"))
        require(status in setOf("declared", "unverified", "verified", "refuted", "proposed"))
        val id = writableDatabase.insertOrThrow(
            "memories", null,
            ContentValues().apply {
                put("kind", kind)
                put("content", content.trim())
                put("status", status)
                put("source", source)
                put("created_at", System.currentTimeMillis())
                put("session_id", sessionId)
            }
        )
        appendAudit("memory.append", "kind=$kind;id=$id;status=$status")
        return id
    }

    fun turns(sessionId: Long = 1, limit: Int = 100): List<Turn> = query("kind = ? AND session_id = ?", arrayOf("turn", sessionId.toString()), limit)
        .asReversed().map { Turn(it.source, it.content, it.createdAt) }

    fun records(limit: Int = 100): List<MemoryRecord> = query("kind != ?", arrayOf("turn"), limit)

    fun sessions(): List<SessionRecord> = readableDatabase.query("sessions", arrayOf("id", "title", "state", "created_at", "updated_at"), null, null, null, null, "updated_at DESC").use { c -> buildList { while (c.moveToNext()) add(SessionRecord(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4))) } }
    fun createSession(title: String): Long { val now = System.currentTimeMillis(); return writableDatabase.insertOrThrow("sessions", null, ContentValues().apply { put("title", title.trim().ifBlank { "Nouvelle session" }); put("state", "idle"); put("created_at", now); put("updated_at", now) }) }

    fun clearTurns() {
        val removed = writableDatabase.delete("memories", "kind = ?", arrayOf("turn"))
        appendAudit("memory.clear_turns", "count=$removed")
    }

    fun link(fromId: Long, toId: Long, relation: String): Long {
        require(fromId > 0 && toId > 0 && fromId != toId)
        require(relation in setOf("supporte", "contredit", "depend_de"))
        require(memoryExists(fromId) && memoryExists(toId)) { "Les deux éléments de mémoire doivent exister." }
        val id = writableDatabase.insertOrThrow("memory_relations", null, ContentValues().apply {
            put("from_id", fromId); put("to_id", toId); put("relation", relation); put("created_at", System.currentTimeMillis())
        })
        appendAudit("memory.link", "id=$id;from=$fromId;to=$toId;relation=$relation")
        return id
    }

    fun links(limit: Int = 100): List<MemoryLink> = readableDatabase.query(
        "memory_relations", arrayOf("id", "from_id", "to_id", "relation", "created_at"),
        null, null, null, null, "created_at DESC", limit.toString()
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(MemoryLink(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2), cursor.getString(3), cursor.getLong(4))) } }

    fun addClaim(text: String, critical: Boolean, evidenceIds: List<Long>): Long {
        val distinctEvidence = evidenceIds.distinct()
        require(text.isNotBlank()) { "Claim vide." }
        require(!critical || distinctEvidence.isNotEmpty()) { "Une claim critique exige au moins une preuve liée." }
        distinctEvidence.forEach { id ->
            readableDatabase.query("memories", arrayOf("kind"), "id = ?", arrayOf(id.toString()), null, null, null).use {
                require(it.moveToFirst() && it.getString(0) == "evidence") { "Preuve introuvable : #$id" }
            }
        }
        val now = System.currentTimeMillis()
        val claimId = writableDatabase.insertOrThrow("claims", null, ContentValues().apply {
            put("text", text.trim()); put("critical", if (critical) 1 else 0)
            put("status", if (distinctEvidence.isEmpty()) "unverified" else "linked_evidence"); put("created_at", now)
        })
        distinctEvidence.forEach { evidenceId -> writableDatabase.insertOrThrow("claim_evidence", null, ContentValues().apply { put("claim_id", claimId); put("evidence_id", evidenceId) }) }
        appendAudit("claim.add", "id=$claimId;critical=$critical;evidence=${distinctEvidence.joinToString(",")}")
        return claimId
    }

    fun claims(limit: Int = 100): List<ClaimRecord> = readableDatabase.query(
        "claims", arrayOf("id", "text", "critical", "status", "created_at"), null, null, null, null, "created_at DESC", limit.toString()
    ).use { cursor -> buildList { while (cursor.moveToNext()) {
        val claimId = cursor.getLong(0)
        val evidence = readableDatabase.query("claim_evidence", arrayOf("evidence_id"), "claim_id = ?", arrayOf(claimId.toString()), null, null, null).use { join -> buildList { while (join.moveToNext()) add(join.getLong(0)) } }
        add(ClaimRecord(claimId, cursor.getString(1), cursor.getInt(2) == 1, cursor.getString(3), evidence, cursor.getLong(4)))
    } } }

    fun auditEvents(limit: Int = 100): List<AuditEvent> = readableDatabase.query(
        "audit_events", arrayOf("id", "timestamp", "action", "details", "previous_hash", "event_hash"), null, null, null, null, "id DESC", limit.toString()
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(AuditEvent(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5))) } }

    fun verifyAudit(): AuditReport {
        var previous = "GENESIS"
        readableDatabase.query("audit_events", arrayOf("timestamp", "action", "details", "previous_hash", "event_hash"), null, null, null, null, "id ASC").use { cursor ->
            while (cursor.moveToNext()) {
                val timestamp = cursor.getLong(0); val action = cursor.getString(1); val details = cursor.getString(2)
                val storedPrevious = cursor.getString(3); val storedHash = cursor.getString(4)
                if (storedPrevious != previous) return AuditReport(false, "Chaîne rompue.")
                val expected = auditHash(timestamp, action, details, storedPrevious)
                if (storedHash != expected) return AuditReport(false, "Hash d'événement invalide.")
                previous = storedHash
            }
        }
        return AuditReport(true, "Chaîne d'audit valide.")
    }

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

    private fun memoryExists(id: Long): Boolean = readableDatabase.query(
        "memories", arrayOf("id"), "id = ?", arrayOf(id.toString()), null, null, null, "1"
    ).use { it.moveToFirst() }

    private fun createGovernance(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS claims (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, critical INTEGER NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS claim_evidence (claim_id INTEGER NOT NULL, evidence_id INTEGER NOT NULL, UNIQUE(claim_id, evidence_id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS audit_events (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp INTEGER NOT NULL, action TEXT NOT NULL, details TEXT NOT NULL, previous_hash TEXT NOT NULL, event_hash TEXT NOT NULL)")
    }

    private fun createSessions(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, state TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("INSERT OR IGNORE INTO sessions (id, title, state, created_at, updated_at) VALUES (1, 'Accueil', 'idle', 0, 0)")
    }

    private fun appendAudit(action: String, rawDetails: String) {
        val db = writableDatabase
        val previous = db.query("audit_events", arrayOf("event_hash"), null, null, null, null, "id DESC", "1").use { if (it.moveToFirst()) it.getString(0) else "GENESIS" }
        val timestamp = System.currentTimeMillis()
        val details = redact(rawDetails)
        db.insertOrThrow("audit_events", null, ContentValues().apply {
            put("timestamp", timestamp); put("action", action); put("details", details); put("previous_hash", previous); put("event_hash", auditHash(timestamp, action, details, previous))
        })
    }

    private fun redact(value: String): String = value.replace(Regex("(?i)(sk-|token=|authorization:)[^\\s;]+"), "[redacted]")
    private fun auditHash(timestamp: Long, action: String, details: String, previous: String): String = sha256("$timestamp|$action|$details|$previous")
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
