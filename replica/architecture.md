# Architecture mobile — OP15 Logos

Date : 2026-10-13. Cette architecture reprend le cycle `replica-recon →
architect → build → test` du pack fourni, mais vise un assistant Android
local-first, non une reproduction de ChatGPT.

## Slice vertical prioritaire

`ouvrir Chat → choisir une session → écrire → transmettre le contexte récent
au serveur local → afficher la réponse → conserver le tour localement`.

La version actuelle accomplit ce flux avec un endpoint OpenAI-compatible et
SQLite. Les actions, le chargement GGUF, le flux de tokens et l'annulation ne
sont pas encore inclus ; ils restent des critères d'acceptation, pas des
capacités annoncées.

## Couches

| Couche | Choix | Raison |
|---|---|---|
| UI | Kotlin + Jetpack Compose Material 3 | Interface native, accessible et adaptée aux petits écrans. |
| État | État Compose + `HarnessStore` | Une source UI simple ; aucune donnée critique dans un composant seul. |
| Persistance | SQLite locale | Sessions, tours, mémoire, liens, affirmations et journal auditable. |
| Raisonnement | Contrôleur déterministe léger | Oriente les questions sans faire passer une hypothèse pour une preuve. |
| Modèle | API locale compatible OpenAI, JNI optionnel | Permet un serveur local aujourd'hui et un runtime GGUF lorsque validé. |
| Gouvernance | Affirmations, preuves et chaîne d'audit | Sépare déclaration, calcul vérifié et conclusion. |

## Navigation

Les cinq destinations basse priorité sont : **Chat**, **Mémoire**,
**Raisonnement**, **Harnais**, **Réglages**. Les fonctions peu fréquentes
(sessions, plan, graphe, preuves, affirmations, audit) sont regroupées dans
Harnais afin de ne pas imposer une barre de dix onglets sur téléphone.

## Données et limites

- Les 12 derniers tours de la session sont injectés dans la requête modèle ;
  cette mémoire de contexte est bornée.
- Les tours, éléments de mémoire, preuves, liens et affirmations sont isolés
  par session. La chaîne d'audit reste globale par conception : elle décrit
  l'historique local de l'application, pas la mémoire d'un profil.
- Une URL ou un lien de mémoire est une provenance déclarée, jamais une
  preuve vérifiée sans contrôleur dédié.

## Risques à traiter avant une publication Play

1. Validation sur appareil du bundle natif `arm64-v8a` et du chargement GGUF.
2. Flux token par token, annulation et libération JNI prouvées par test.
3. Permissions explicites avant Web, fichiers, shell, Git ou MCP.
4. Tests visuels ADB et parcours clavier/lecteur d'écran.
