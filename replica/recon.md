# OP15 Logos — reconnaissance initiale

Date : 2026-10-13

## Écrans présents

| Écran | État réel | Preuve / limite |
|---|---|---|
| Chat | Partiel | Écran principal ; envoie vers une URL OpenAI-compatible avec les 12 derniers tours de la session ; pas de streaming ni annulation. |
| Sessions | Partiel | Création/sélection SQLite et historique de dialogue isolé. Preuves, graphe et audit restent communs. |
| Plan / raisonnement | Partiel | Lentilles déterministes locales ; pas de plan exécutable. |
| Mémoire / graphe / preuves | Partiel | SQLite, relations, calculateur et provenance déclarée ; pas de RAG ni source web vérifiée. |
| Affirmations / audit | Partiel | Lien affirmation-preuve et chaîne locale SHA-256 ; pas de signature matérielle. |
| Réglages | Partiel | URL modèle et probe JNI ; import GGUF et profils runtime absents. |
| Harnais | Partiel | Hub des écrans avancés pour conserver une navigation mobile lisible. |

## Fonctions critiques

- Le runtime GGUF JNI est empaquetable avec un bundle `arm64-v8a`, mais non validé sur appareil.
- Aucun outil Web, fichiers, shell, Git, MCP, microphone ou root n'est réellement activé.
- L'UI n'a pas encore été capturée sur appareil ; aucune fidélité visuelle ne peut être affirmée.

## Prochain flux à terminer

**Choisir un GGUF → vérifier le runtime → charger le modèle → répondre en flux → annuler → afficher métriques et erreur.**

Ce flux doit être fini avant d'ajouter un autre écran d'agent.
