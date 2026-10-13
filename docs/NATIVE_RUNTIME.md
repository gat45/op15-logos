# Runtime GGUF natif — contrat d’intégration

Logos vise en priorité le runtime `ggml-hexagon` de JZ pour le OnePlus 15 :
FastRPC + pool ION/mempool est la voie locale cible pour les modèles et tailles
compatibles. Ce choix ne rend pas `dspqueue` ou CPU faux : ils restent des
variantes de comparaison et de repli explicite. Le runtime n’est pas commité
avec l’application : les bibliothèques `arm64-v8a` sont des artefacts de build
à valider par version, ABI et licence.

Le build de livraison doit fournir un répertoire contenant au minimum :

```text
libllama.so
libggml-hexagon.so
libllama-jni.so
libggml-htp-v81.so
```

Puis construire explicitement :

```text
gradlew :app:assembleDebug -PlogosNativeDirs="C:\chemin\vers\runtime;C:\chemin\vers\bridge"
```

Le pont charge les bibliothèques dans l’ordre `llama`, `ggml-hexagon`, puis
`llama-jni`. S’il échoue, Logos doit afficher l’indisponibilité du runtime ; il
ne doit pas déclarer un HTP actif ni le remplacer silencieusement par CPU.

Le staging Gradle transforme les répertoires plats d’artefacts en arborescence
APK `lib/arm64-v8a/`. Un build local a confirmé la présence de
`libllama.so`, `libggml-hexagon.so`, `libggml-htp-v81.so` et
`libllama-jni.so`. Cette vérification ne démontre pas leur compatibilité
mutuelle au chargement ni la disponibilité d’une session HTP.

## Garde-fous avant un build utilisable sur OP15

1. Vérifier les dépendances ELF et le nom SONAME des quatre bibliothèques.
2. Confirmer que les host libraries et le skel `v81` viennent du même build.
3. Retirer toute synchronisation du skel qui exige `/data/local/tmp` ou root :
   une application Android normale ne peut pas s’y fier.
4. Ajouter l’import explicite du GGUF via Storage Access Framework, puis une
   copie contrôlée vers un emplacement lisible par le runtime.
5. Construire le backend JZ et son skel v81 à partir du **même commit**, puis
   produire un répertoire `arm64-v8a` sans dépendance à `/data/local/tmp` ni
   root.
6. Exécuter le même GGUF avec FastRPC/mempool (priorité), `dspqueue` et CPU,
   en mesurant pré-remplissage, décodage, RAM, stabilité et échec/fallback.
7. Ne promouvoir FastRPC/mempool dans Logos qu'après une preuve backend : nom
   du backend, périphérique HTP, résultats de génération et mesures stockés
   ensemble. Un fallback doit être affiché et journalisé, jamais silencieux.

## Politique de sélection Logos

1. **JZ FastRPC/mempool** — choix par défaut uniquement après compatibilité
   validée pour le GGUF choisi et session HTP réellement ouverte.
2. **JZ dspqueue** — comparaison utile, notamment pour le pré-remplissage ;
   ne remplace la voie par défaut que sur résultats reproduits à budget égal.
3. **CPU** — mode de récupération explicitement signalé si HTP est indisponible
   ou si le modèle dépasse les limites mémoire du backend choisi.

L’interface doit donc afficher `backend demandé`, `backend réellement actif`,
`modèle`, `pré-remplissage`, `décodage`, `RAM` et `motif de repli`. Sans ces
données, elle affiche « non mesuré » plutôt qu’un chiffre ou un HTP fictif.

Le code JNI existant a déjà corrigé des fuites de contexte/batch, mais son
état est process-global : Logos devra sérialiser toute session native et ne
pas proposer de génération concurrente avant cette validation.
