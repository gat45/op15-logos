# Croisement — NXUS Agent Harness MVP et OP15 Logos

Date : 2026-10-12. Source étudiée : artefacts fournis sous `D:/temp/`.
Cette note compare du code réel ; elle ne copie ni le package Python NXUS ni
ses documents dans l'APK.

## Conclusion

NXUS est un **plan de contrôle d'exécution** local-first : contrat de tâche,
machine d'états, manifeste, policy, approbations, outils bornés, vérification
et audit. Logos est aujourd'hui un **noyau de dialogue et de mémoire** :
SQLite, hypothèses, preuves, associations et calcul vérifié. Les deux sont
complémentaires ; Logos ne doit pas ouvrir des outils externes avant d'adopter
les invariants NXUS pertinents.

## Comparaison vérifiable

| Sujet | NXUS, code inspecté | Logos actuel | Décision |
|---|---|---|---|
| Contrat de tâche | `D:/temp/models.py:95-144` valide objectif, critères, profil, risque, chemins et outils autorisés. | Une requête de chat n'a ni périmètre ni critère d'acceptation. | Ajouter un `TaskContract` Kotlin avant tout outil externe. |
| Plan et réflexion | `planner.py:11-63` génère un manifeste lié par hash et bloque un plan hors périmètre. | Le panneau Plan est informatif ; aucune opération ne peut être approuvée. | Créer un plan local versionné, séparé du texte du modèle. |
| Approbation précise | `approvals.py:32-69` lie acteur, run, manifeste, outil, arguments, expiration et consommation unique. | Aucun outil externe n'est actuellement activé. | Prérequis obligatoire avant navigateur, fichiers ou MCP. Pas d'approbation générale. |
| Politique fail-closed | `policy.py:35-64` refuse hors état, risque, profil, contrat, manifeste ou chemin. | Logos ne possède pas de gateway d'outils ; l'endpoint de modèle est configurable. | Garder zéro outil par défaut ; installer une policy avant l'activation de R2+. |
| Preuves et claims | `models.py:208-248` définit Evidence/Claim/VerificationResult ; `evidence.py:11-48` bloque les claims critiques sans identifiants de preuve. | Logos conserve une preuve avec provenance et statut, mais ne lie pas encore une claim atomique à des IDs de preuve. | Ajouter `Claim` et table de jonction `claim_evidence`. Un lien de graphe n'est pas une citation. |
| Audit | `audit.py:38-102` écrit JSONL append-only avec hash précédent et redaction. | Aucun journal d'audit des créations/relations/calculs. | Ajouter un audit local redacted avant les actions ; un hash chaîné détecte une altération après coup mais ne remplace pas une signature. |
| Modèle local | `ollama_adapter.py:18-62` accepte seulement loopback HTTP, modèle épinglé et JSON structuré ; les tool calls ne sont pas exécutés. | `OpenAiClient` cible une URL OpenAI compatible configurable, sans modèle épinglé ni schéma de sortie. | Introduire des profils de modèle : local direct, LAN explicitement autorisé, et aucun tool-call exécutable. |
| Arrêt et rollback | `PolicyEngine.stop()` + révocation de grants ; runbooks d'arrêt et rollback fournis. | Pas de run exécutable à annuler. | À ajouter avec le premier gateway d'outil, pas avant. |

## Éléments déjà cohérents

- Logos conserve les statuts `unverified`, `verified` et `refuted` plutôt que
  de présenter le texte du modèle comme une preuve.
- Le calculateur local est une vérification indépendante et sa sortie conserve
  la provenance `calculator`.
- Les associations `supporte`, `contredit`, `depend_de` restent distinctes des
  preuves de vérité.
- NXUS et Logos convergent sur le principe : le modèle propose ; le noyau
  déterministe autorise et vérifie.

## Ordre d'intégration sûr

1. `Claim` + `claim_evidence` et validation des claims critiques.
2. Journal d'audit local redacted et chaîné pour mémoire, preuves, calculs et
   futures approbations.
3. `TaskContract` + plan/manifeste pour toute demande d'action.
4. Service d'approbation à usage unique et kill switch.
5. Gateway d'outils avec registre fermé ; d'abord lecture locale R0, puis web
   R2 sous permission. MCP, navigateur et écriture de fichiers restent bloqués
   jusqu'à cette étape.

## Critères de validation

- Modifier un argument après approbation doit être refusé.
- Une claim critique sans preuve liée doit être bloquée.
- Modifier un événement du journal doit rompre sa vérification.
- Le kill switch doit invalider les approbations non consommées.
- Un résultat du modèle ne peut jamais appeler directement le gateway.

## Hors intégration

Le code NXUS est Python et le projet Android est Kotlin/Compose. Logos reprend
donc les contrats et scénarios de test, pas les fichiers source, dépendances ou
artefacts d'exécution NXUS.
