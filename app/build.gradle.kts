plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val logosNativeRuntimeDirs = providers.gradleProperty("logosNativeDirs").orNull
    ?.split(';')?.map(String::trim)?.filter(String::isNotBlank).orEmpty()
val prepareLogosNativeRuntime by tasks.registering(Copy::class) {
    onlyIf { logosNativeRuntimeDirs.isNotEmpty() }
    logosNativeRuntimeDirs.forEach { directory -> from(file(directory)) { include("*.so") } }
    into(layout.buildDirectory.dir("generated/logosJniLibs/arm64-v8a"))
}

android {
    namespace = "com.op15.logos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.op15.logos"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Native GGUF runtime is opt-in: do not embed host-specific binaries in Git.
    // A release builder supplies an audited arm64-v8a directory through
    // -PlogosNativeDir=<directory>.
    sourceSets.getByName("main").jniLibs.srcDir(layout.buildDirectory.dir("generated/logosJniLibs"))
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }
    .configureEach { dependsOn(prepareLogosNativeRuntime) }

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
