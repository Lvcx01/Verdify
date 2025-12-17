import org.gradle.api.GradleException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val placeholderApiKeyPattern = Regex(
    "\\b(your[- ]?api[- ]?key|insert[- ]?key|replace|example|dummy|test|todo|placeholder|changeme|fake|mock|sample|null|none|empty|default)\\b",
    RegexOption.IGNORE_CASE
)
private const val MINIMUM_API_KEY_LENGTH = 20

android {
    namespace = "com.example.ids"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ids"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw GradleException("GEMINI_API_KEY in local.properties is missing or empty. Retrieve a valid Gemini API key from Google AI Studio and set it in that file.")
        if (placeholderApiKeyPattern.containsMatchIn(geminiApiKey)) {
            throw GradleException("GEMINI_API_KEY in local.properties appears to be a placeholder; please supply a real key from Google AI Studio.")
        }
        if (geminiApiKey.length < MINIMUM_API_KEY_LENGTH) {
            throw GradleException("GEMINI_API_KEY in local.properties seems too short; please double-check the value from Google AI Studio.")
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.generativeai)
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.google.android.gms:play-services-location:19.0.1")
    implementation(libs.androidx.activity)
    implementation(libs.generativeai)
    implementation(libs.androidx.rules)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
