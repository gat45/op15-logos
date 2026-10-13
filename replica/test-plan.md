# Plan de test — OP15 Logos

Créé avec la méthode `replica-test`. Les cas non exécutés restent marqués à
réaliser : aucun résultat n'est inventé.

| ID | Cas | Attendu | État |
|---|---|---|---|
| F01-H1 | Créer une session, envoyer un message, revenir au chat | Le tour est affiché et persiste après relance | À tester sur appareil |
| F01-E1 | Envoyer un champ vide | Aucun appel réseau, bouton inactif | À automatiser |
| F01-E2 | Message long avec accents, emoji et retour ligne | JSON valide, réponse ou erreur affichée sans crash | À automatiser |
| F01-E3 | Serveur local indisponible | Erreur compréhensible, historique utilisateur conservé | À automatiser |
| F01-E4 | Deux sessions | Le modèle ne reçoit que l'historique de la session active | À automatiser |
| F02-H1 | Ajouter un calcul | Résultat marqué calcul vérifié avec provenance `calculator` | Tests unitaires réussis le 2026-10-13 |
| F03-H1 | Parcourir Chat, Mémoire, Raisonnement, Harnais, Réglages | Chaque destination s'ouvre sans défilement horizontal de navigation | À tester visuellement sur appareil |
| F04-N1 | Demander une action Web/fichier | Aucune action externe ne part sans adaptateur et permission | À automatiser |

## Résultat de construction

`testDebugUnitTest` et `assembleDebug` : réussis le 2026-10-13. Cela ne
remplace pas les tests ADB, JNI ou les captures d'écran listés ci-dessus.
