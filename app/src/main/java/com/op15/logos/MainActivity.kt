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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
    fun setEndpoint(value: String) = prefs.edit().putString("endpoint", value.trim()).apply()
    fun loadTurns(): List<Turn> = memory.turns()
    fun appendTurn(turn: Turn) = memory.append("turn", turn.text, if (turn.role == "Utilisateur") "declared" else "unverified", turn.role)
    fun loadMemoryRecords(): List<MemoryRecord> = memory.records()
    fun addHypothesis(text: String) = memory.append("hypothesis", text, "proposed", "Utilisateur")
    fun clearTurns() = memory.clearTurns()
}

private object OpenAiClient {
    fun complete(endpoint: String, prompt: String): String {
        require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) { "Renseigne une URL http(s) complète." }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 12_000; readTimeout = 90_000
            setRequestProperty("Content-Type", "application/json"); doOutput = true
        }
        val body = JSONObject().put("model", "local").put("stream", false).put("messages", JSONArray().put(
            JSONObject().put("role", "system").put("content", "Tu es Logos. Réponds en français. Distingue faits, hypothèses et limites. N'affiche pas de raisonnement caché.")
        ).put(JSONObject().put("role", "user").put("content", prompt))).toString()
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
    var tab by remember { mutableIntStateOf(0) }
    var endpoint by remember { mutableStateOf(store.endpoint()) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var analysis by remember { mutableStateOf(Analysis(emptyList(), emptyList(), EvidenceStatus.UNVERIFIED)) }
    var memoryRecords by remember { mutableStateOf(store.loadMemoryRecords()) }
    val tabs = listOf("Dialogue", "Plan", "Raisonnement", "Mémoire", "Preuves", "Réglages")
    fun submit() {
        val text = draft.trim(); if (text.isEmpty() || busy) return
        analysis = ReasoningController.assess(text)
        val userTurn = Turn("Utilisateur", text)
        turns += userTurn; store.appendTurn(userTurn); draft = ""; busy = true
        scope.launch {
            val answer = withContext(Dispatchers.IO) { runCatching { OpenAiClient.complete(endpoint, text) }.getOrElse { "Connexion modèle impossible : ${it.message}" } }
            val modelTurn = Turn("Logos", answer)
            turns += modelTurn; store.appendTurn(modelTurn); memoryRecords = store.loadMemoryRecords(); busy = false
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("OP15 Logos") }, actions = { Text("mémoire locale", Modifier.padding(end = 16.dp), style = MaterialTheme.typography.labelSmall) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) { tabs.forEachIndexed { i, name -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(name) }) } }
            when (tab) {
                0 -> ChatPanel(turns, draft, { draft = it }, busy, ::submit)
                1 -> PlanPanel(analysis)
                2 -> ReasoningPanel(analysis)
                3 -> MemoryPanel(turns, memoryRecords, { text -> store.addHypothesis(text); memoryRecords = store.loadMemoryRecords() }, { turns.clear(); store.clearTurns() })
                4 -> EvidencePanel(analysis)
                5 -> SettingsPanel(endpoint, { endpoint = it }, { store.setEndpoint(endpoint) })
            }
        }
    }
}

@Composable private fun PlanPanel(analysis: Analysis) = Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Plan avant action", style = MaterialTheme.typography.titleLarge)
    Text("Le plan est une proposition. Logos ne possède pas encore d'outil externe à exécuter dans cette application.")
    analysis.plan.forEachIndexed { index, step -> Card { Text("${index + 1}. $step", Modifier.padding(12.dp)) } }
    Text("Avocat du diable", style = MaterialTheme.typography.titleMedium)
    analysis.objections.forEach { Card { Text(it, Modifier.padding(12.dp)) } }
}

@Composable private fun ChatPanel(turns: List<Turn>, draft: String, setDraft: (String) -> Unit, busy: Boolean, send: () -> Unit) = Column(Modifier.fillMaxSize().padding(12.dp)) {
    Text("Le modèle est un adaptateur : configure son URL dans Réglages.", style = MaterialTheme.typography.labelSmall)
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(turns) { turn -> Card { Column(Modifier.padding(10.dp)) { Text(turn.role, style = MaterialTheme.typography.labelMedium); Text(turn.text) } } } }
    OutlinedTextField(value = draft, onValueChange = setDraft, modifier = Modifier.fillMaxWidth(), label = { Text("Message") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), singleLine = false)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button(onClick = send, enabled = !busy) { Text(if (busy) "Réponse…" else "Envoyer") } }
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
@Composable private fun EvidencePanel(analysis: Analysis) = Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Preuves et vérification", style = MaterialTheme.typography.titleLarge)
    Text("Aucun oracle n'est encore connecté. Toute conclusion du modèle reste non vérifiée.")
    Text("Dernier statut du contrôleur : ${analysis.status.name}")
    Text("Prochaine intégration : calculateur déterministe, recherche sourcée et tests d'outils sous permission.")
}

@Composable private fun SettingsPanel(endpoint: String, setEndpoint: (String) -> Unit, save: () -> Unit) = Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Réglages", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(value = endpoint, onValueChange = setEndpoint, modifier = Modifier.fillMaxWidth(), label = { Text("URL OpenAI compatible") }, placeholder = { Text("http://192.168.x.x:8080/v1/chat/completions") })
    Button(onClick = save) { Text("Enregistrer l’URL") }
    Text("OP15 Logos ne lance pas de navigateur, terminal ni écriture de fichiers sans permission explicite.", style = MaterialTheme.typography.bodySmall)
}
