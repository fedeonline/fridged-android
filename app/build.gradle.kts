plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Release signing comes from ~/.gradle/gradle.properties, never from the repo.
val releaseSigningProperties = listOf(
    "fridged.keystore.file",
    "fridged.keystore.password",
    "fridged.key.alias",
    "fridged.key.password",
)
val releaseSigning = releaseSigningProperties.associateWith { providers.gradleProperty(it).orNull }
val missingReleaseSigning = releaseSigning.filterValues { it.isNullOrBlank() }.keys.toList()

android {
    namespace = "io.github.fedeonline.fridged"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.fedeonline.fridged"
        // Latest Android only (docs/PLAN.md section 2): no compatibility code for older versions.
        minSdk = 37
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (missingReleaseSigning.isEmpty()) {
            create("release") {
                storeFile = file(releaseSigning.getValue("fridged.keystore.file")!!)
                storePassword = releaseSigning.getValue("fridged.keystore.password")
                keyAlias = releaseSigning.getValue("fridged.key.alias")
                keyPassword = releaseSigning.getValue("fridged.key.password")
            }
        }
    }

    buildTypes {
        debug {
            // Lets debug and release coexist on the phone; registered as a second app in Firebase.
            applicationIdSuffix = ".debug"
            buildConfigField("String", "DEFAULT_DEVICE_ID", "\"fridge-sim\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "DEFAULT_DEVICE_ID", "\"fridge\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        // Fixtures shared with the data contract (docs/data-contract.md).
        getByName("test").resources.directories.add("../docs/fixtures")
    }

    androidResources {
        // Per-app language picker in system settings (English default, Italian).
        generateLocaleConfig = true
    }
}

// Fail release packaging with a clear message instead of producing an unsigned APK.
tasks.matching { it.name.matches(Regex("(package|assemble|bundle|install)Release")) }.configureEach {
    val missing = missingReleaseSigning
    doFirst {
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured. Add to ~/.gradle/gradle.properties: " +
                    missing.joinToString(", ") + " (see README.md).",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
