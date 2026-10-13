package com.op15.logos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.op15.toolkit.LlamaJni
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { LogosApp(HarnessStore(applicationContext)) } }
    }
}

private enum class EvidenceStatus { HYPOTHESIS, UNVERIFIED, VERIFIED, REFUTED }
data class Turn(val role: String, val text: String, val time: Long = System.currentTimeMillis())
private data class Analysis(
    val lenses: List<String>,
    val questions: List<String>,
    val status: EvidenceStatus,
    val plan: List<String> = emptyList(),
    val objections: List<String> = emptyList()
)

/** A small, deterministic controller: it selects questions, never fabricates a proof. */
private object ReasoningController {
    fun assess(message: String): Analysis {
        val lower = message.lowercase()
        val lenses = buildList {
            if (listOf("pourquoi", "cause", "car", "donc").any(lower::contains)) add("Causalité")
            if (listOf("vrai", "faux", "contradiction", "si ").any(lower::contains)) add("Logique")
            if (listOf("calcul", "pourcent", "mesure", "plus", "moins").any(lower::contains)) add("Calcul")
            if (listOf("ironie", "veut dire", "sous-entendu", "blague").any(lower::contains)) add("Pragmatique")
            if (listOf("dois", "choix", "risque", "budget").any(lower::contains)) add("Décision")
        }.take(3)
        val questions = mutableListOf<String>()
        if ("Logique" in lenses) questions += "Quelles prémisses et quelles conclusions sont réellement établies ?"
        if ("Causalité" in lenses) questions += "Quelle observation départagerait les causes possibles ?"
        if ("Calcul" in lenses) questions += "Quelle unité et quel calcul indépendant faut-il vérifier ?"
        if ("Pragmatique" in lenses) questions += "Quelles interprétations du ton ou de la référence restent plausibles ?"
        if ("Décision" in lenses) questions += "Quelle action est réversible et apporte le plus d'information ?"
        val plan = listOf(
            "Clarifier l'objectif et distinguer les faits des hypothèses.",
            "Collecter la preuve la moins coûteuse qui peut départager les options.",
            "Soumettre toute action externe ou irréversible à validation humaine.",
            "Journaliser le résultat et corriger seulement ce que le test réfute."
        )
        val objections = listOf(
            "Quelle prémisse, si elle est fausse, invalide la conclusion ?",
            "Quelle interprétation concurrente explique les mêmes observations ?",
            "Quel effet de bord ou coût caché cette décision peut-elle produire ?"
        )
        return Analysis(lenses, questions, EvidenceStatus.UNVERIFIED, plan, objections)
    }
}

private class HarnessStore(context: Context) {
    private val prefs = context.getSharedPreferences("logos_harness", Context.MODE_PRIVATE)
    private val memory = LogosMemoryDb(context)
    fun endpoint(): String = prefs.getString("endpoint", "") ?: ""
    fun activeSession(): Long = prefs.getLong("active_session", 1)
    fun selectSession(id: Long) = prefs.edit().putLong("active_session", id).apply()
    fun sessions(): List<SessionRecord> = memory.sessions()
    fun createSession(title: String): Long = memory.createSession(title)
    fun setEndpoint(value: String) = prefs.edit().putString("endpoint", value.trim()).apply()
    fun loadTurns(): List<Turn> = memory.turns(activeSession())
    fun appendTurn(turn: Turn) = memory.append("turn", turn.text, if (turn.role == "Utilisateur") "declared" else "unverified", turn.role, activeSession())
    fun loadMemoryRecords(): List<MemoryRecord> = memory.records()
    fun addHypothesis(text: String) = memory.append("hypothesis", text, "proposed", "Utilisateur")
    fun addEvidence(text: String, source: String) = memory.append("evidence", text, "unverified", source.ifBlank { "Utilisateur" })
    fun addCalculation(expression: String, value: Double) = memory.append("evidence", "Calcul vérifié : $expression = $value", "verified", "calculator")
    fun loadLinks(): List<MemoryLink> = memory.links()
    fun link(fromId: Long, toId: Long, relation: String) = memory.link(fromId, toId, relation)
    fun addClaim(text: String, critical: Boolean, evidenceIds: List<Long>) = memory.addClaim(text, critical, evidenceIds)
    fun loadClaims(): List<ClaimRecord> = memory.claims()
    fun loadAuditEvents(): List<AuditEvent> = memory.auditEvents()
    fun verifyAudit(): AuditReport = memory.verifyAudit()
    fun clearTurns() = memory.clearTurns()
}

private object OpenAiClient {
    fun complete(endpoint: String, history: List<Turn>, prompt: String): String {
        require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) { "Renseigne une URL http(s) complète." }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 12_000; readTimeout = 90_000
            setRequestProperty("Content-Type", "application/json"); doOutput = true
        }
        val messages = JSONArray().put(
            JSONObject().put("role", "system").put("content", "Tu es Logos, un assistant local francophone. Utilise l'historique récent fourni pour garder le fil de la discussion, sans prétendre que chaque souvenir est un fait vérifié. Distingue faits, hypothèses et limites. N'affiche pas de raisonnement caché.")
        )
        // Bounded context prevents a long session from silently overflowing the model context window.
        history.takeLast(12).forEach { turn ->
            messages.put(JSONObject().put("role", if (turn.role == "Utilisateur") "user" else "assistant").put("content", turn.text.take(2_000)))
        }
        messages.put(JSONObject().put("role", "user").put("content", prompt))
        val body = JSONObject().put("model", "local").put("stream", false).put("messages", messages).toString()
        connection.outputStream.bufferedWriter().use { it.write(body) }
        val code = connection.responseCode
        val text = (if (code in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
        check(code in 200..299) { "Serveur HTTP $code : ${text.take(240)}" }
        return JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogosApp(store: HarnessStore) {
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val turns = remember { mutableStateListOf<Turn>().also { it.addAll(store.loadTurns()) } }
    var sessions by remember { mutableStateOf(store.sessions()) }
    var tab by remember { mutableIntStateOf(0) }
    var endpoint by remember { mutableStateOf(store.endpoint()) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var analysis by remember { mutableStateOf(Analysis(emptyList(), emptyList(), EvidenceStatus.UNVERIFIED)) }
    var memoryRecords by remember { mutableStateOf(store.loadMemoryRecords()) }
    var memoryLinks by remember { mutableStateOf(store.loadLinks()) }
    var claims by remember { mutableStateOf(store.loadClaims()) }
    var auditEvents by remember { mutableStateOf(store.loadAuditEvents()) }
    var auditReport by remember { mutableStateOf(store.verifyAudit()) }
    fun refreshGovernance() {
        memoryRecords = store.loadMemoryRecords()
        memoryLinks = store.loadLinks()
        claims = store.loadClaims()
        auditEvents = store.loadAuditEvents()
        auditReport = store.verifyAudit()
    }
    fun submit() {
        val text = draft.trim(); if (text.isEmpty() || busy) return
        analysis = ReasoningController.assess(text)
        val recentHistory = turns.toList()
        val userTurn = Turn("Utilisateur", text)
        turns += userTurn; store.appendTurn(userTurn); draft = ""; busy = true
        scope.launch {
            val answer = withContext(Dispatchers.IO) { runCatching { OpenAiClient.complete(endpoint, recentHistory, text) }.getOrElse { "Connexion modèle impossible : ${it.message}" } }
            val modelTurn = Turn("Logos", answer)
            turns += modelTurn; store.appendTurn(modelTurn); refreshGovernance(); busy = false
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("OP15 Logos") }, actions = { Text("local", Modifier.padding(end = 16.dp), style = MaterialTheme.typography.labelSmall) }) },
        bottomBar = {
            NavigationBar {
                listOf(
                    0 to "Chat",
                    4 to "Mémoire",
                    3 to "Raisonnement",
                    10 to "Harnais",
                    9 to "Réglages"
                ).forEach { (destination, label) ->
                    NavigationBarItem(
                        selected = tab == destination,
                        onClick = { tab = destination },
                        icon = { Text(when (destination) { 0 -> "●"; 4 -> "◆"; 3 -> "◇"; 10 -> "≡"; else -> "⚙" }) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> ChatPanel(turns, draft, { draft = it }, busy, ::submit)
                1 -> SessionPanel(sessions, store.activeSession(), { title -> store.createSession(title); sessions = store.sessions() }, { id -> store.selectSession(id); turns.clear(); turns.addAll(store.loadTurns()) })
                2 -> PlanPanel(analysis)
                3 -> ReasoningPanel(analysis)
                4 -> MemoryPanel(turns, memoryRecords, { text -> store.addHypothesis(text); refreshGovernance() }, { turns.clear(); store.clearTurns(); refreshGovernance() })
                5 -> GraphPanel(memoryRecords, memoryLinks) { from, to, relation -> store.link(from, to, relation); refreshGovernance() }
                6 -> EvidencePanel(analysis, memoryRecords, { text, source -> store.addEvidence(text, source); refreshGovernance() }, { expression, value -> store.addCalculation(expression, value); refreshGovernance() })
                7 -> ClaimPanel(memoryRecords, claims) { text, critical, evidenceIds -> store.addClaim(text, critical, evidenceIds); refreshGovernance() }
                8 -> AuditPanel(auditEvents, auditReport, ::refreshGovernance)
                9 -> SettingsPanel(endpoint, { endpoint = it }, { store.setEndpoint(endpoint) }, { LlamaJni.probe().detail })
                10 -> HarnessHub { destination -> tab = destination }
            }
        }
    }
}

@Composable
private fun HarnessHub(open: (Int) -> Unit) = Column(
    Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    Text("Harnais", style = MaterialTheme.typography.titleLarge)
    Text("Fonctions avancées. Chaque donnée reste locale ; aucune action externe n'est exécutée depuis cet écran.")
    listOf(
        1 to ("Sessions" to "Créer ou reprendre une conversation."),
        2 to ("Plan" to "Préparer une action réversible."),
        5 to ("Graphe" to "Relier des éléments mémorisés."),
        6 to ("Preuves" to "Conserver observations et calculs."),
        7 to ("Affirmations" to "Relier une affirmation à ses preuves."),
        8 to ("Audit" to "Vérifier la chaîne locale d'événements.")
    ).forEach { (destination, item) ->
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) { Text(item.first, style = MaterialTheme.typography.titleMedium); Text(item.second, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { open(destination) }) { Text("Ouvrir") }
            }
        }
    }
}

@Composable private fun SessionPanel(sessions: List<SessionRecord>, activeId: Long, create: (String) -> Unit, select: (Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Sessions", style = MaterialTheme.typography.titleLarge)
        Text("Chaque session conserve son propre historique de dialogue. Les preuves et l’audit restent communs tant que leur isolation n’est pas achevée.")
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Nom de session") })
        Button(onClick = { create(title); title = "" }, enabled = title.isNotBlank()) { Text("Créer") }
        LazyColumn(Modifier.weight(1f)) { items(sessions) { session -> Card { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (session.id == activeId) "● ${session.title}" else session.title); Button(onClick = { select(session.id) }, enabled = session.id != activeId) { Text("Ouvrir") } } } } }
    }
}

@Composable private fun GraphPanel(records: List<MemoryRecord>, links: List<MemoryLink>, addLink: (Long, Long, String) -> Unit) {
    var from by remember { mutableStateOf("") }; var to by remember { mutableStateOf("") }; var relation by remember { mutableStateOf("supporte") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Graphe d'associations", style = MaterialTheme.typography.titleLarge)
        Text("Relie des éléments mémorisés. Un lien décrit une relation, pas une preuve de vérité.")
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(records) { Card { Text("#${it.id} · ${it.kind} · ${it.status}\n${it.content}", Modifier.padding(8.dp)) } } }
        OutlinedTextField(value = from, onValueChange = { from = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Identifiant source") })
        OutlinedTextField(value = to, onValueChange = { to = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Identifiant cible") })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("supporte", "contredit", "depend_de").forEach { AssistChip(onClick = { relation = it }, label = { Text(it) }) } }
        Button(onClick = { runCatching { addLink(from.toLong(), to.toLong(), relation) }.onSuccess { error = ""; from = ""; to = "" }.onFailure { error = it.message ?: "Lien invalide" } }, enabled = from.isNotBlank() && to.isNotBlank()) { Text("Créer le lien") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        links.take(12).forEach { Text("#${it.fromId} ${it.relation} #${it.toId}", style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable private fun PlanPanel(analysis: Analysis) = Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Plan avant action", style = MaterialTheme.typography.titleLarge)
    Text("Le plan est une proposition. Logos ne possède pas encore d'outil externe à exécuter dans cette application.")
    analysis.plan.forEachIndexed { index, step -> Card { Text("${index + 1}. $step", Modifier.padding(12.dp)) } }
    Text("Avocat du diable", style = MaterialTheme.typography.titleMedium)
    analysis.objections.forEach { Card { Text(it, Modifier.padding(12.dp)) } }
}

@Composable private fun ChatPanel(turns: List<Turn>, draft: String, setDraft: (String) -> Unit, busy: Boolean, send: () -> Unit) = Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
        Text("Logos", style = MaterialTheme.typography.titleMedium)
        Text("Assistant local · mémoire et preuves séparées", style = MaterialTheme.typography.labelMedium)
    } }
    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (turns.isEmpty()) item { Card { Column(Modifier.padding(16.dp)) { Text("Commencer une session", style = MaterialTheme.typography.titleMedium); Text("Décris un objectif. Logos distinguera ce qui est déclaré, inféré et vérifié.") } } }
        items(turns) { turn -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(turn.role, style = MaterialTheme.typography.labelMedium); Text(turn.text) } } }
    }
    OutlinedTextField(value = draft, onValueChange = setDraft, modifier = Modifier.fillMaxWidth(), label = { Text("Écrire à Logos") }, placeholder = { Text("Pose une question ou donne un objectif…") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { if (!busy && draft.isNotBlank()) send() }), singleLine = false)
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (busy) "Réponse en cours" else "Les 12 derniers messages sont transmis au modèle", style = MaterialTheme.typography.labelSmall); Button(onClick = send, enabled = !busy && draft.isNotBlank()) { Text(if (busy) "Réponse…" else "Envoyer") } }
}

@Composable private fun ReasoningPanel(analysis: Analysis) = Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Analyse du contrôleur", style = MaterialTheme.typography.titleLarge)
    Text("Statut : ${analysis.status.name.lowercase()}. Les lentilles orientent la réponse, elles ne prouvent rien.")
    if (analysis.lenses.isEmpty()) Text("Aucune lentille approfondie nécessaire pour le dernier message.") else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { analysis.lenses.forEach { AssistChip(onClick = {}, label = { Text(it) }) } }
    analysis.questions.forEach { Card { Text(it, Modifier.padding(12.dp)) } }
}

@Composable private fun MemoryPanel(turns: List<Turn>, records: List<MemoryRecord>, addHypothesis: (String) -> Unit, clear: () -> Unit) {
    var note by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Mémoire de conversation", style = MaterialTheme.typography.titleLarge)
    Text("${turns.size} échanges et ${records.size} éléments structurés stockés localement en SQLite. Un souvenir n'est pas une preuve.")
    OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Hypothèse à conserver") })
    Button(onClick = { if (note.isNotBlank()) { addHypothesis(note); note = "" } }, enabled = note.isNotBlank()) { Text("Enregistrer comme hypothèse") }
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(records) { record -> Card { Column(Modifier.padding(10.dp)) { Text("${record.kind} · ${record.status}", style = MaterialTheme.typography.labelMedium); Text(record.content); Text("source : ${record.source}", style = MaterialTheme.typography.labelSmall) } } }
    }
    Button(onClick = clear, enabled = turns.isNotEmpty()) { Text("Effacer les échanges") }
    }
}
@Composable private fun EvidencePanel(analysis: Analysis, records: List<MemoryRecord>, addEvidence: (String, String) -> Unit, addCalculation: (String, Double) -> Unit) {
    var statement by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var formula by remember { mutableStateOf("") }
    var calculationMessage by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Preuves et vérification", style = MaterialTheme.typography.titleLarge)
    Text("Ajoute une observation ou une source. Elle reste non vérifiée jusqu'à l'exécution d'un vérificateur indépendant.")
    OutlinedTextField(value = statement, onValueChange = { statement = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Observation ou affirmation") })
    OutlinedTextField(value = source, onValueChange = { source = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Source ou URL") })
    Button(onClick = { if (statement.isNotBlank()) { addEvidence(statement, source); statement = ""; source = "" } }, enabled = statement.isNotBlank()) { Text("Conserver comme preuve non vérifiée") }
    OutlinedTextField(value = formula, onValueChange = { formula = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Calcul à vérifier, ex. (7+3)*2") })
    Button(onClick = { CalculatorVerifier.evaluate(formula).onSuccess { addCalculation(formula, it.value); calculationMessage = "Calcul vérifié : ${it.value}" }.onFailure { calculationMessage = it.message ?: "Calcul invalide" } }, enabled = formula.isNotBlank()) { Text("Vérifier le calcul") }
    if (calculationMessage.isNotBlank()) Text(calculationMessage)
    Text("Dernier statut du contrôleur : ${analysis.status.name}")
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(records.filter { it.kind == "evidence" }) { record -> Card { Column(Modifier.padding(10.dp)) { Text(record.status, style = MaterialTheme.typography.labelMedium); Text(record.content); Text("provenance : ${record.source}", style = MaterialTheme.typography.labelSmall) } } }
    }
    Text("Prochaine intégration : calculateur déterministe, recherche sourcée et tests d'outils sous permission.")
    }
}

@Composable private fun ClaimPanel(records: List<MemoryRecord>, claims: List<ClaimRecord>, addClaim: (String, Boolean, List<Long>) -> Unit) {
    var claimText by remember { mutableStateOf("") }
    var evidenceIdsText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    fun submit(critical: Boolean) {
        val ids = evidenceIdsText.split(',').mapNotNull { it.trim().toLongOrNull() }
        runCatching { addClaim(claimText.trim(), critical, ids) }
            .onSuccess { claimText = ""; evidenceIdsText = ""; error = "" }
            .onFailure { error = it.message ?: "Affirmation invalide" }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Affirmations et preuves", style = MaterialTheme.typography.titleLarge)
        Text("Une affirmation critique doit référencer une ou plusieurs preuves locales. Le lien est traçable, mais ne transforme pas une source non vérifiée en fait.")
        OutlinedTextField(value = claimText, onValueChange = { claimText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Affirmation") })
        OutlinedTextField(value = evidenceIdsText, onValueChange = { evidenceIdsText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Identifiants des preuves, ex. 4, 7") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { submit(false) }, enabled = claimText.isNotBlank()) { Text("Enregistrer") }
            Button(onClick = { submit(true) }, enabled = claimText.isNotBlank() && evidenceIdsText.isNotBlank()) { Text("Marquer critique") }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Text("Preuves disponibles : ${records.count { it.kind == "evidence" }}", style = MaterialTheme.typography.labelMedium)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(claims) { claim -> Card { Column(Modifier.padding(10.dp)) {
                Text(if (claim.critical) "critique · ${claim.status}" else claim.status, style = MaterialTheme.typography.labelMedium)
                Text(claim.text)
                Text("preuves liées : ${claim.evidenceIds.joinToString().ifBlank { "aucune" }}", style = MaterialTheme.typography.labelSmall)
            } }
        }
    }
}
}

@Composable private fun AuditPanel(events: List<AuditEvent>, report: AuditReport, refresh: () -> Unit) = Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Journal local", style = MaterialTheme.typography.titleLarge)
    Text(if (report.valid) "Intégrité : valide" else "Intégrité : invalide", color = if (report.valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    Text(report.message, style = MaterialTheme.typography.bodySmall)
    Text("Chaque événement conserve le hachage de l'événement précédent. Le contenu sensible est réduit avant écriture dans le journal.")
    Button(onClick = refresh) { Text("Actualiser et vérifier") }
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events) { event -> Card { Column(Modifier.padding(10.dp)) {
            Text(event.action, style = MaterialTheme.typography.labelMedium)
            Text(event.details)
            Text("empreinte : ${event.eventHash.take(16)}…", style = MaterialTheme.typography.labelSmall)
        } }
    }
}
}

@Composable private fun SettingsPanel(endpoint: String, setEndpoint: (String) -> Unit, save: () -> Unit, probeNative: () -> String) {
    var nativeStatus by remember { mutableStateOf("Runtime natif non vérifié.") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Réglages", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(value = endpoint, onValueChange = setEndpoint, modifier = Modifier.fillMaxWidth(), label = { Text("URL OpenAI compatible") }, placeholder = { Text("http://192.168.x.x:8080/v1/chat/completions") })
    Button(onClick = save) { Text("Enregistrer l’URL") }
    Text("Runtime GGUF local", style = MaterialTheme.typography.titleMedium)
    Text(nativeStatus, style = MaterialTheme.typography.bodySmall)
    Button(onClick = { nativeStatus = probeNative() }) { Text("Vérifier le runtime natif") }
    Text("OP15 Logos ne lance pas de navigateur, terminal ni écriture de fichiers sans permission explicite.", style = MaterialTheme.typography.bodySmall)
    }
}
