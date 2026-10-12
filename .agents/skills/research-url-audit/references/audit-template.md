# Fiche de compréhension d’une source

Utiliser une fiche par URL `audited`. Réponses courtes, observables et sourcées.

1. **Fonction réelle** — quel problème l’outil résout-il et quel résultat
   produit-il ?
2. **Entrées et sorties** — données, protocole, fichiers, API, modèle ou
   processus requis ; structure exacte du résultat si elle est publique.
3. **Chemin inspecté** — fichier/symbole ou section de méthode réellement lu,
   et test ou exemple qui couvre ce chemin.
4. **Limites et coût** — licence, réseau, clés, permissions, RAM/CPU, runtime,
   données sensibles, plate-forme et erreurs connues.
5. **Relation à Logos** — `adapter`, `data contract`, `benchmark`, `reference
   only` ou `reject`; ne jamais choisir plusieurs décisions sans les séparer.
6. **Preuve d’intégration** — test minimal qui démontrerait que l’adaptation
   fonctionne, ce qu’elle ne démontre pas, et la condition de rollback.

Exemple de conclusion correcte : « Argdown : *data contract*. Son modèle
contient des arguments, énoncés et relations ; Logos peut reprendre ces trois
entités en Kotlin. Test : créer une prémisse, une conclusion et une objection,
puis redémarrer l’application et vérifier que les liens persistent. Cela ne
vérifie pas la vérité de la conclusion. »
