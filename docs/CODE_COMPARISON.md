# Comparaison de code — Logos et références de raisonnement

Date : 2026-10-12. Cette note compare les clones locaux placés dans
`vendor-audit/` avec le code exécutable de Logos. Un clone est une source
d'audit, **pas** une dépendance embarquée dans l'APK.

## Verdict honnête

`OP15 Logos` est actuellement un client de modèle local avec un routeur léger
et une mémoire de conversation. Ce n'est pas encore un moteur de preuves, de
logique formelle, ni une mémoire expérimentale. Les écrans « Preuves » et
« Raisonnement » présentent correctement cette limite ; ils ne doivent pas
afficher une validation inexistante.

## Comparaison vérifiée dans le code

| Besoin | Logos actuel | Référence inspectée | Décision Android |
|---|---|---|---|
| Décider quels cadres activer | `ReasoningController.assess` choisit au plus trois lentilles à partir de mots-clés (`MainActivity.kt:71-98`). | Jarvix `RuleBasedProvider.route` produit une distribution et conserve la décision (`vendor-audit/jarvix-memory/src/jarvix_memory/decision/jev.py:38-66, 193-206`). | Remplacer les simples étiquettes par un `DecisionRecord` local : options, score, motif, horodatage et version du routeur. |
| Mémoire de dialogue | SQLite locale : tours, hypothèses, preuves, liens et affirmations sont isolés par `session_id` (`MemoryStore.kt`). | Jarvix possède des mémoires typées et des transitions de statut ; les actions passent par `proposed → approved → executed/failed/skipped` (`action/layer.py:12-46`). | Ajouter l'isolement de la chaîne d'audit ou documenter explicitement son périmètre global, puis tester la migration d'une base existante. |
| Hypothèses et mémoire négative | Aucun objet hypothèse ou expérience. | `ExperimentTracker` lie une hypothèse aux essais, et marque `do_not_repeat` après réfutation (`experiment/tracker.py:18-107`). | Intégrer ce cycle, mais uniquement après un résultat d'outil ou un test réellement exécuté. Jamais sur simple texte du modèle. |
| Vérification | `EvidenceStatus` est toujours `UNVERIFIED`; aucun oracle (`MainActivity.kt:60, 199-203`). | Jarvix peut agréger des statistiques de preuves, mais ce n'est pas un oracle externe (`decision/jev.py:82-97`). | Créer des adaptateurs `CalculatorVerifier`, `HttpSourceVerifier` et `ToolTestVerifier`; une source doit garder URL, extrait, date et résultat. |
| Raisonnement formel | Aucun format intermédiaire ni solveur. | Logic-LM charge une formalisation déjà produite puis choisit Prover9, Pyke, CSP ou Z3 selon le jeu de données (`logic-lm/models/logic_inference.py:11-49`). Les erreurs de parsing/exécution utilisent un secours (`:31-40`). | Ne pas intégrer Logic-LM : pile Python et solveurs externes. Définir un contrat `Formalization` et ne marquer une conclusion « vérifiée » que si un solveur retourne un résultat et que la formalisation est montrée à l'utilisateur. |
| Évaluer la traduction langue → logique | Pas de test dédié. | FOLIO fournit des prémisses et conclusions naturelles avec annotations FOL et labels, mais aucun moteur (`vendor-audit/folio/README.md:15-21`). | Ajouter FOLIO comme benchmark PC séparé, pas comme fonctionnalité ni données incluses dans l'APK. Sa licence CC-BY-SA impose une revue avant redistribution. |
| Arguments et objections | Trois objections génériques par demande (`MainActivity.kt:90-96`). | Argdown a un modèle d'arguments, énoncés et relations (`argdown-core/src/model/model.ts:61,185,265,454`) et valide les structures prémisse–inférence–conclusion. | Reprendre un modèle Kotlin minimal de graphe d'arguments, sans runtime Node/TypeScript. Une objection doit cibler une prémisse ou une conclusion identifiée. |
| Preuves mathématiques Lean | Absent. | LeanDojo-v2 embarque un pipeline Lean/Python avec Torch, DeepSpeed et une toolchain Lean (`pyproject.toml:25-38`; `README.md:43-58, 69-100`). | Hors APK. À utiliser éventuellement côté PC pour l'évaluation de théorèmes. Le clone présente aussi une incohérence à clarifier : `LICENSE` Apache-2.0 et `pyproject.toml` MIT. |
| Runtime local et streaming | `LlamaJni` peut charger/initaliser des bibliothèques, mais le chat utilise encore une URL HTTP compatible OpenAI (`LlamaJni.kt`, `MainActivity.kt`). | llama.cpp Android expose `InferenceEngine.loadModel`, `sendUserPrompt(): Flow<String>`, benchmark et nettoyage (`vendor-audit/llama.cpp/examples/llama.android/lib/src/main/java/com/arm/aichat/InferenceEngine.kt`). Le client Flutter audité ajoute timeout/annulation et contrôle de mémoire (`vendor-audit/cross-platform-llm-client/lib/services/`). | Créer un contrat Kotlin Logos de cycle de vie `Loading → Ready → Generating → Cancelled/Error`, implémenté par HTTP puis JNI. Aucun token/benchmark ne doit être présenté sans mesure réelle. |
| Backend OP15 | Le staging Gradle sait inclure des bibliothèques `arm64-v8a`, mais aucune session HTP de Logos n'est encore mesurée. | Le fork JZ `ggml-hexagon` est la cible du runtime OP15 : comparer FastRPC/mempool, dspqueue et CPU sur le même GGUF et le même commit. | Faire de **JZ FastRPC/mempool** la préférence conditionnelle ; afficher backend demandé/actif et tout fallback. Aucun binaire JZ n'est intégré avant validation ABI, skel v81 et test sur appareil. |
| Validation et actions | Aucun test de cohérence backend ni garde d'action partagée. | PhiloHarness local fournit `AcceleratorValidator` et `RiskGate`. | Portage autorisé : `RuntimeValidation.kt` refuse un HTP qui diverge du CPU ; `RiskGate.kt` exige confirmation ou bloque une action irréversible. |
| Outils système | Aucun outil système n'est exposé dans Logos. | Private Agent exécute intents, SMS, Shizuku et automatisation écran (`vendor-audit/private-agent/lib/services/action_handler.dart`) et son README déclare le projet modifiable ; DroidDesk gère root/chroot et terminal (`vendor-audit/DroidDesk/app/lib/state/app_state.dart`). | Ajouter plus tard un `AndroidActionAdapter` personnel, désactivé par défaut : permissions Android, aperçu d'arguments, confirmation de l'utilisateur et journal local. Commencer par ouvrir une application ; exclure Telegram, root/chroot, Shizuku, shell, ADB et SMS automatique tant qu'ils ne possèdent pas de contrat d'approbation séparé. |

## Écarts prioritaires, dans l'ordre

1. **Persistance structurée :** remplacer le JSON de conversation par Room,
   avec provenance et statut. C'est le préalable à toute « mémoire longue ».
2. **Cycle expérimental :** permettre de proposer, approuver, exécuter et
   conclure un test. Un essai réfuté doit être conservé et réutilisé comme
   contre-exemple.
3. **Registre de preuves :** une preuve doit référencer l'outil ou la source
   ayant réellement produit le résultat. Les sorties du LLM restent des
   hypothèses.
4. **Graphe d'arguments :** stocker `supporte`, `contredit`, `dépend de` entre
   revendications et preuves, puis rendre ce graphe lisible dans l'UI.
5. **Contrat de solveur :** d'abord un calculateur déterministe local ; les
   formaliseurs et solveurs complets restent des services PC optionnels.
6. **Évaluation reproductible :** tests unitaires de transitions + jeu FOLIO
   exécuté hors APK + mesure comparative routeur activé/désactivé.

## Ce qui ne doit pas être fait

- Copier Jarvix Memory dans l'APK : son implémentation est Python/MCP ; Logos
  doit implémenter un adaptateur de données compatible, pas deux mémoires.
- Déclarer une réponse validée parce qu'un LLM ou un score heuristique est
  confiant.
- Embarquer Logic-LM, Argdown ou LeanDojo comme dépendances Android : leurs
  runtimes et coûts ne correspondent pas à l'APK.
- Traiter le benchmark FOLIO comme une base de connaissance de production.

## Critères d'acceptation de la prochaine version

- Une hypothèse, un essai et son verdict survivent à un redémarrage.
- Une action non approuvée ne peut pas être exécutée.
- Un verdict `VERIFIED` contient au moins un identifiant de preuve dont le
  vérificateur a retourné succès.
- Une contradiction affiche les deux revendications et leurs sources au lieu
  d'écraser l'ancienne.
- Les tests couvrent transition invalide, échec d'un vérificateur et garde-fou
  `do_not_repeat`.
