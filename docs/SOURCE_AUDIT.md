# Registre d'audit des sources — OP15 Logos

Date de départ : 2026-10-12. Ce registre transforme les URL de recherche en
décisions vérifiables. Une URL présente ici n'est **pas** une dépendance, une
intégration ni une preuve de performance.

Le catalogue mécanique est [URL_REGISTRY.csv](URL_REGISTRY.csv). Le skill
local [research-url-audit](../.agents/skills/research-url-audit/SKILL.md)
conserve les statuts `known`, `opened`, `audited`, `experimented`, `integrated`
et `rejected` : il interdit de transformer une URL collectée en intégration
affirmée sans audit ni test.

## Disponibilité réseau — contrôle du 2026-10-13

Les **101 URLs actuellement présentes** dans le registre ont été contrôlées par
requête HTTP, avec redirections limitées. Résultat : **99 accessibles**, une
erreur HTTP et une source sans résolution DNS. Ce contrôle ne lit pas le
contenu, ne vérifie ni licence ni compatibilité, et ne change donc aucun statut
de décision dans le registre.

| URL | Résultat | Décision de registre |
|---|---|---|
| https://github.com/allenai/ProofWriter | HTTP 404 | Conserver l'historique, retrouver une source active avant audit sémantique. |
| https://trolleybench.com/ | DNS indisponible au contrôle | Conserver comme référence signalée ; ne pas l'utiliser comme dépendance ou preuve. |

## Règle de décision

Pour chaque source : vérifier le propriétaire, la licence, le code réel, les
tests, la compatibilité Android, le coût RAM/batterie et l'interface possible.
Ne copier aucun code avant cette vérification. Les sources lourdes sont appelées
depuis un adaptateur PC/service ; seules les briques Kotlin/Android légères sont
éligibles à une intégration embarquée.

## Lot A — consulté le 2026-10-12

| Source | Rôle réel confirmé | Licence / limite | Décision Logos |
|---|---|---|---|
| https://github.com/teacherpeterpan/Logic-LLM | Pipeline LLM → programme logique → solveur, avec raffinement. | MIT ; résultats publiés à reproduire ; la traduction NL→logique reste le risque. | Référence pour le contrat `Formalization`, pas de copie ni embarquement Android. |
| https://github.com/Yale-LILY/FOLIO | Jeu de données de raisonnement en logique du premier ordre, avec prémisses et formules annotées. | CC-BY-SA-4.0 ; jeu d'évaluation, pas moteur. | Benchmark hors ligne de fidélité de formalisation. |
| https://github.com/leanprover/lean4 | Langage et vérificateur de théorèmes. | Vérifier la licence/version avant distribution ; outil formel, pas interprète de langage naturel. | Adaptateur PC futur ; aucun exécutable Lean dans l'APK à ce stade. |
| https://github.com/Z3Prover/z3 | Solveur SMT. | Vérifier le paquet Android/ABI avant usage ; ne valide que la formalisation fournie. | Service de vérification déterministe futur. |
| https://github.com/argdown/argdown | Syntaxe, parser et cartes d'arguments. | MIT ; stack TypeScript/Node, non native Android. | Adopter son modèle prémisse/conclusion/objection, réimplémenter un format Kotlin minimal. |
| https://github.com/lean-dojo/LeanDojo-v2 | Entraînement et évaluation d'agents de preuve Lean. | Apache-2.0 ; Python, Lean, CUDA et stockage important. | Référence de protocole d'évaluation, hors cible Android embarquée. |
| https://github.com/mem0ai/mem0 | Infrastructure mémoire agentique, recherche hybride et liens d'entités. | Apache-2.0 ; les chiffres du README dépendent de leur pile gérée et ne valent pas validation locale. | Référence de schéma mémoire ; pas de dépendance cloud dans Logos. |
| https://github.com/getzep/graphiti | Graphe de connaissance temporel pour agents. | Licence, coûts et voie locale à vérifier avant intégration. | Étudier la provenance temporelle et les contradictions ; ne pas embarquer sans mesure. |
| https://github.com/google-ai-edge/LiteRT-LM | Inférence LLM edge. | Compatibilité modèles/accélérateurs et licence à vérifier par version. | Candidat d'adaptateur de modèle local Android. |
| https://github.com/aohp-os/aohp | Harnais agentique au niveau système. | Surface de permissions et compatibilité à auditer. | Référence de gouvernance, jamais de privilèges système implicites. |
| https://github.com/gat45/jarvix-memory | Mémoire SQLite locale multi-couches, MCP et vérification déterministe. | MIT ; implémentation Python, donc non embarquable telle quelle dans l'APK. | Réutiliser son contrat provenance/hypothèse/expérience ; définir un pont API avant toute synchronisation. |
| https://github.com/gat45/op15-adaptive-harness | Spécification et prototype de harnais local vérifiable. | Le dépôt doit être audité fichier par fichier avant réutilisation de code. | Logos reste une application indépendante ; partager seulement des contrats stables. |
| https://github.com/eibragaa/droid-harness | Exécution de harnais de code et llama.cpp sur Android Snapdragon. | Dépendances terminal/llama.cpp et permissions à auditer. | Référence de déploiement local, pas de dépendance automatique. |
| https://nxus.fr/cas-usage-agents-ia/mode-reflexion-avant-action-agent | Méthode : plan, risques, alternatives, validation humaine puis rapport post-action. | Cas d'usage méthodologique, pas un composant logiciel. | Contrat `Plan → approbation → action → résultat` dans Logos. |
| https://nxus.fr/cas-usage-agents-ia/preparer-decision-avocat-du-diable | Méthode de contre-argumentation avant décision. | Ne remplace pas une source ou un oracle indépendant. | Registre d'objections distinct des conclusions. |
| https://nxus.fr/blog/setup-ia-agentique-memoire-outils-securite | Séparation contexte, outils, contrôles humains et permissions limitées. | Retour d'expérience, pas une garantie de sécurité. | Mémoire lisible, permissions minimales, preuve post-action. |

## Lot B — collecte web et médias, consulté le 2026-10-12

| Source | Rôle réel confirmé | Licence / limite | Décision Logos |
|---|---|---|---|
| https://github.com/firecrawl/firecrawl | Plateforme de recherche, extraction, interaction et crawl ; les appels d'API exigent une clé. | AGPL-3.0 pour le dépôt ; service/API externe et coût d'usage à contrôler. | Adaptateur PC/MCP optionnel, jamais une clé embarquée dans l'APK. Toute page importée devient une `evidence` avec URL et date. |
| https://github.com/firecrawl/cli | CLI et skills de Firecrawl. | Runtime Node et clé de service ; pas Android natif. | Utilisable par l'environnement PC autorisé, pas depuis le processus Android. |
| https://docs.firecrawl.dev | Documentation de l'API de crawl/extraction. | API réseau ; respecter limites, conditions et droits des sites. | Contrat d'adaptateur futur, désactivé par défaut. |
| https://github.com/kernelmethod/scrapling | Petit crawler Go avec test dans le dépôt. | MIT ; binaire Go à auditer par plateforme avant installation. | Candidat PC seulement ; aucune exécution automatique depuis l'APK. |
| https://github.com/supadata-ai | Organisation liée à une API d'extraction média. | Service tiers : clé, coût et politique de données à vérifier par endpoint. | Source ouverte, non intégrée. |
| https://github.com/supadata-ai/skills | Skills d'intégration Supadata. | Vérifier chaque skill et ses permissions avant installation. | Référence pour un adaptateur média explicitement autorisé. |
| https://supadata.ai | API de transcriptions, métadonnées et analyse de vidéos sociales. | API hébergée, données externes et clé : pas de mémoire automatique. | Une transcription importée reste non vérifiée et cite URL + date + fournisseur. |
| https://crawlee.dev | Documentation d'un framework de crawl. | Runtime et licence à vérifier par version avant réutilisation. | Référence PC ; aucun crawler embarqué dans l'APK. |

## Lot C — mémoire et exécution Android, approfondi le 2026-10-12

| Source | Chemin inspecté / rôle réel | Décision Logos | Test d'intégration requis |
|---|---|---|---|
| https://github.com/mem0ai/mem0 | README : mémoire utilisateur/session/agent, extraction et récupération hybrides ; dépôt Apache-2.0 mais piles Python/Node et service optionnel. | **data contract** : conserver origine, temps et statut ; pas de SDK ou clé Mem0 dans l'APK. | Persister puis retrouver une hypothèse locale sans réseau. |
| https://github.com/getzep/graphiti | Dépôt de graphe de connaissances temps réel pour agents ; infrastructure de service, à évaluer avant toute synchronisation. | **reference only** : modèle futur de relations temporelles. | Benchmark de relations sur export SQLite avant un adaptateur. |
| https://github.com/google-ai-edge/LiteRT-LM | Framework d'inférence LLM edge de Google. | **adapter** futur : un `ModelAdapter` local séparé du contrôleur. | Réponse locale avec métriques de latence et repli contrôlé. |
| https://github.com/eibragaa/droid-harness | Harnais Android Snapdragon avec llama.cpp portable. | **reference only** : étudier son déploiement, sans reprendre ses permissions ni son runtime. | Démontrer un modèle local Logos sans accès système implicite. |

## File d'audit priorisée

### Logique, preuve et arguments

- https://arxiv.org/abs/2305.12295
- https://aclanthology.org/2023.findings-emnlp.248/
- https://github.com/leanprover/lean4
- https://lean-lang.org/
- https://lean-lang.org/doc/reference/latest/
- https://github.com/lean-dojo/LeanDojo-v2
- https://github.com/lean-dojo/LeanAgent
- https://arxiv.org/abs/2502.06243
- https://github.com/Z3Prover/z3
- https://microsoft.github.io/z3guide/
- https://smt-lib.org/
- https://github.com/allenai/ProofWriter
- https://arxiv.org/abs/2012.13048
- https://github.com/argdown/argdown
- https://argdown.org/
- https://github.com/mxthematic/openproof
- https://aristotle.harmonic.fun/
- https://github.com/septract/lean-aristotle-mcp
- https://github.com/Vilin97/Clawristotle

### Paradoxes, morale et robustesse argumentative

- https://github.com/liweijiang/delphi
- https://arxiv.org/abs/2206.09515
- https://github.com/DaseinB612/trolleybench
- https://trolleybench.com/
- https://github.com/demelin/moral_stories
- https://huggingface.co/datasets/demelin/moral_stories
- https://aclanthology.org/2021.naacl-main.186/
- https://aclanthology.org/2024.lrec-main.1243/
- https://aclanthology.org/2025.emnlp-main.1411/
- https://doi.org/10.1038/s41598-025-18489-7
- https://github.com/The-Responsible-AI-Initiative/LLM_Ethics_Benchmark
- https://aclanthology.org/2024.emnlp-main.1224/
- https://aclanthology.org/2024.lrec-main.726/
- https://plato.stanford.edu/entries/paradoxes-contemporary-logic/
- https://plato.stanford.edu/entries/liar-paradox/
- https://plato.stanford.edu/entries/logic-paraconsistent/
- https://plato.stanford.edu/entries/logic-fuzzy/
- https://plato.stanford.edu/entries/logic-classical/

### Mémoire, révision et agents

- https://github.com/gat45/jarvix-memory
- https://github.com/letta-ai/letta
- https://docs.letta.com/
- https://github.com/letta-ai/letta-agent-sdk
- https://github.com/mem0ai/mem0
- https://docs.mem0.ai/
- https://github.com/getzep/graphiti
- https://help.getzep.com/graphiti
- https://github.com/langchain-ai/langmem
- https://langchain-ai.github.io/langmem/
- https://github.com/topoteretes/cognee
- https://docs.cognee.ai/
- https://github.com/jacobycoffin/cortex-memory
- https://github.com/engram-memory/engram
- https://arxiv.org/abs/2304.03442
- https://github.com/joonspk-research/generative_agents
- https://arxiv.org/abs/2303.11366
- https://github.com/noahshinn/reflexion
- https://github.com/snap-research/locomo
- https://arxiv.org/search/?query=LongMemEval&searchtype=all

### Android, modèle local et système

- https://github.com/aohp-os/aohp
- https://arxiv.org/abs/2606.23449
- https://github.com/tokenarc/open-jarvis
- https://github.com/NickMonrad/kernel-ai-assistant
- https://github.com/flagdizero/jenny-android-ai-agent
- https://github.com/ModalityDance/PalmClaw
- https://github.com/natanloterio/Brownie
- https://github.com/imoonkey/closepaw
- https://github.com/inehemiasm/AiAssistant
- https://github.com/Quor-a/ZorvAI
- https://github.com/google/adk-kotlin
- https://github.com/eibragaa/droid-harness
- https://github.com/google-ai-edge/LiteRT-LM
- https://ai.google.dev/edge/litert-lm
- https://developer.android.com/topic/architecture
- https://developer.android.com/topic/architecture/data-layer
- https://developer.android.com/training/data-storage/room
- https://www.sqlite.org/docs.html
- https://developer.android.com/topic/libraries/architecture/workmanager

## Architecture déduite à tester, pas encore validée

```text
UI Android
  -> Controller (ambiguïtés, budget, permissions)
  -> Argument store (SQLite : prémisses, objections, provenance, révisions)
  -> Model adapter (LiteRT-LM ou serveur local)
  -> Verification adapter (calcul / Z3 / Lean distant)
  -> Evidence ledger (verified | unverified | refuted)
```

Le modèle formule ; le contrôleur demande les vérifications ; un adaptateur ne
doit jamais marquer une sortie LLM comme preuve sans résultat externe.

## Triage GitHub et lot Android — 2026-10-13

Le script local `triage_github_repositories.ps1` a interrogé l'API publique
pour les 51 URLs de dépôt identifiables dans le registre (les URLs
d'organisation ou de documentation ne sont pas des dépôts). Il conserve les
métadonnées de 43 dépôts déjà collectées et marque 8 nouveaux dépôts comme
non disponibles au triage quand l'API GitHub limite les requêtes.
`allenai/ProofWriter` n'est plus disponible à cette adresse. Le fichier
machine lisible est [GITHUB_TRIAGE.csv](GITHUB_TRIAGE.csv).
Ce triage ne modifie pas le statut d'audit : il ne remplace ni l'inspection du
code, ni les tests, ni l'examen des licences effectives.

| Source | Ce qui a été ouvert | Contrainte vérifiée | Décision Logos | Étape d'acceptation |
|---|---|---|---|---|
| https://github.com/google/adk-kotlin | README, modules Android/LiteRT-LM et documentation d'agents/outils. | Apache-2.0 ; modules cloud et LiteRT-LM facultatifs. | **adapter à évaluer** : conserver l'interface `ModelAdapter`, ne pas remplacer JNI/llama.cpp sans mesure OP15. | Prototype isolé sans clé cloud, test de génération et mémoire sur appareil. |
| https://github.com/tokenarc/open-jarvis | README public : agent Android local, voix et contrôle appareil annoncés. | MIT d'après le triage ; la surface de permissions doit être auditée avant toute réutilisation. | **reference only** à ce stade : étudier ses parcours de permission et non son code comme dépendance. | Lire manifeste, service d'accessibilité, tests et licence dans une passe dédiée. |
| https://github.com/NickMonrad/kernel-ai-assistant | README public : assistant Android local-first, mémoire sémantique et framework de skills annoncés. | Apache-2.0 d'après le triage. | **reference only** : comparer son découpage mémoire/skills au contrat SQLite Logos. | Inspecter schéma, stockage, limites de permissions et tests. |
| https://github.com/eibragaa/droid-harness | README public et audit existant : harnais de code Android autour d'un environnement local. | MIT ; cible un environnement de développement, pas une API Android embarquée. | **reference only** : utile pour le protocole de build/observabilité, pas pour embarquer un shell dans l'APK. | Reproduire seulement une mesure ou un test sans privilège. |

Les projets marqués `reference only` ne sont ni téléchargés ni exécutés dans
Logos. Leur statut de registre reste `opened` jusqu'à l'inspection prévue par
le skill.

## Lot D — stack locale, voix et Android — 2026-10-13

| Source | Rôle observé | Licence / contrainte | Décision Logos | Vérification requise |
|---|---|---|---|---|
| https://github.com/jaredrhod/fullstack-agent | Installateur Claude Code qui assemble mémoire, voix, visualisation et actions optionnelles. | AGPL-3.0 ; dépend de Claude Code et d'un abonnement, hors modèle Android local. | **reference only** : conserver la séparation optionnelle des capacités, ne pas reprendre le code ou l'installateur. | Aucun code tiers dans l'APK ; concevoir une fiche d'activation/permission par capacité. |
| https://github.com/jaredrhod/ai-memory-vault | Mémoire persistante Markdown/Obsidian, templates et « priming » par tâche. | CC-BY-SA-4.0, conçu pour un agent de terminal et des fichiers Markdown. | **data contract** : futur export/import Markdown explicite depuis SQLite ; aucun template ou identité tierce distribué. | Exporter un souvenir Logos avec provenance, puis le réimporter sans perdre statut/date. |
| https://github.com/jaredrhod/backtalk | Interface voix locale annoncée pour un agent de terminal. | Licence et dépendances audio à auditer avant toute réutilisation. | **reference only** : retenir le pipeline écoute → transcription → réponse → synthèse. | Prototype voix Android distinct, avec consentement micro et interruption testée. |
| https://github.com/ggml-org/llama.cpp | Runtime local GGUF. | Licence et ABI à vérifier par version ; compilation NDK et bibliothèques natives nécessaires. | **adapter** : `LlamaJni` reste l'unique frontière native de Logos. | Charger un GGUF réel sur OP15, mesurer TTFT/tok/s et libérer la session après annulation. |
| https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md | Instructions Android publiques de llama.cpp. | Documentation, pas un artefact prêt à intégrer. | **reference only** pour le build NDK/Android de l'adaptateur. | Reproduire un build propre, sans chemins root ni fichiers système imposés. |
| https://developer.android.com/ndk | Documentation officielle NDK. | Outil de compilation, pas runtime de modèle. | **reference only** ; source normative pour le build JNI. | CI de compilation `arm64-v8a` et test de chargement sur appareil. |
| https://developer.android.com/reference/android/accessibilityservice/AccessibilityService | API Android puissante d'accessibilité. | Permission sensible ; politiques Play et consentement explicite requis. | **reject** pour l'automatisation générale. Une future aide d'accessibilité ne peut servir que son objectif déclaré et rester désactivée par défaut. | Revue de politique, écran de consentement et test manuel avant tout ajout. |
| https://huggingface.co/ | Catalogue de modèles et jeux de données. | Chaque modèle possède ses propres licence, taille et conditions. | **reference only** : un modèle n'est sélectionnable qu'après fiche de licence et mesure locale. | Registre de modèles avec hash, licence, taille, backend et métriques. |
| https://github.com/orailnoor/private-agent | Projet signalé pour agent privé local. | Code, licence et manifeste à ouvrir. | **reference only** tant que l'audit n'est pas fait. | Lire licence, stockage, permissions et tests. |
| https://github.com/orailnoor/cross-platform-llm-client | Client LLM multiplateforme signalé. | Code, licence et architecture à ouvrir. | **reference only**. | Comparer sa frontière modèle à `ModelAdapter` sans dépendance directe. |
| https://github.com/orailnoor/DroidDesk | Projet Android signalé. | Code, licence et permissions à ouvrir. | **reference only**. | Auditer manifeste et flux UI avant toute comparaison. |

Les pages Android générales sur activités et architecture sont ajoutées au
registre comme sources normatives de conception, mais n'entraînent pas de
dépendance. Les fonctions d'accessibilité ne sont jamais considérées comme une
autorisation de contrôler l'appareil.

## Prochaine passe mesurable

1. Auditer Jarvix Memory et le code OP15 avant d'ajouter une seconde mémoire.
2. Construire un format Kotlin d'argument inspiré d'Argdown, avec provenance et
   historique de révision.
3. Ajouter un adaptateur calculateur déterministe, puis un test FOLIO réduit
   avant d'étudier Lean/Z3.
