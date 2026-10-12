# Contrat mémoire local de Logos

La mémoire est stockée dans `logos_memory.db`, sur l’appareil. Elle ne dépend
ni de Firecrawl, ni de Mem0, ni de Graphiti.

| Type | Créateur actuel | Statut initial | Règle |
|---|---|---|---|
| `turn` | Dialogue | `declared` (utilisateur) ou `unverified` (modèle) | Un tour n’est pas une preuve. |
| `hypothesis` | Utilisateur | `proposed` | Doit recevoir un test avant confirmation ou réfutation. |
| `evidence` | Utilisateur / futur outil | `unverified` | Conserve la provenance indiquée ; une URL seule ne vaut pas validation. |
| `experiment` | Futur adaptateur de test | `unverified` | Doit joindre commande, environnement et résultat. |
| `action` | Futur planificateur | `proposed` | Nécessite une approbation avant toute exécution externe. |

Les statuts disponibles sont `declared`, `proposed`, `unverified`, `verified` et
`refuted`. Seul un vérificateur indépendant pourra plus tard produire
`verified` ou `refuted`.

## Liens et calculs

`memory_relations` conserve des liens `supporte`, `contredit` ou `depend_de`
entre deux identifiants de mémoire. Le graphe montre donc des associations
explicites sans transformer une association en vérité.

Le vérificateur arithmétique local accepte uniquement nombres, parenthèses et
opérateurs `+ - * /`. Lorsqu'il termine, le résultat est sauvegardé comme une
preuve `verified` avec la provenance `calculator`. Toute autre preuve reste
`unverified` jusqu'à son propre vérificateur.

## Affirmations critiques et audit

Une table `claims` conserve les affirmations séparément des souvenirs. Une
affirmation marquée **critique** doit pointer vers au moins une entrée de type
`evidence`, via `claim_evidence`. Son statut est alors `linked_evidence` : cela
prouve l'existence du lien, pas la véracité de la preuve liée.

Chaque ajout de mémoire, relation ou affirmation écrit un événement dans
`audit_events`. L'événement contient l'empreinte SHA-256 de l'événement
précédent et les détails sont réduits avant stockage (par exemple les jetons
Bearer). L'écran **Audit** vérifie localement cette chaîne.

Limite assumée : cette chaîne rend une altération détectable dans la base
présente ; elle n'est ni signée par une clé matérielle ni exportée vers un
serveur. La suppression d'une base entière reste possible par l'utilisateur ou
le système Android. Une future exportation signée doit rester une fonction
explicite et approuvée.

## Sources étudiées et décision

- **Jarvix Memory :** son cycle hypothèse/expérience/verdict est un contrat de
  données à reproduire, non une dépendance Python.
- **Mem0 :** sa séparation mémoire utilisateur/session/agent et sa recherche
  hybride sont des références ; Logos reste local et n’importe ni clé ni pile
  serveur. [Mem0](https://github.com/mem0ai/mem0)
- **Graphiti :** inspire les relations temporelles, mais son graphe de service
  reste hors APK jusqu’à une mesure de coût. [Graphiti](https://github.com/getzep/graphiti)
- **Firecrawl, Scrapling, Crawlee et Supadata :** futurs adaptateurs de collecte
  explicitement autorisés. Leurs sorties alimenteront `evidence`, jamais
  directement les faits vérifiés.

## Test d’acceptation

Créer une hypothèse et une preuve non vérifiée, redémarrer l’app, puis vérifier
que texte, type, statut et provenance sont conservés. Ce test ne démontre pas
la vérité du contenu ; il prouve seulement la persistance et la séparation
épistémique.
