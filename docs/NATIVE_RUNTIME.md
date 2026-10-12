# Runtime GGUF natif — contrat d’intégration

Logos peut embarquer le pont JNI OP15 existant, mais ce runtime n’est pas
commité avec l’application : les bibliothèques `arm64-v8a` sont des artefacts
de build à valider par version, ABI et licence.

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
5. Exécuter un test appareil avec le même modèle pour `dspqueue`, FastRPC et
   CPU, en mesurant pré-remplissage, décodage, RAM et échec/fallback.

Le code JNI existant a déjà corrigé des fuites de contexte/batch, mais son
état est process-global : Logos devra sérialiser toute session native et ne
pas proposer de génération concurrente avant cette validation.
