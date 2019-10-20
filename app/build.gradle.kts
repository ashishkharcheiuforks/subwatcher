import io.github.aouerfelli.subwatcher.Dependencies
import io.github.aouerfelli.subwatcher.Kotlin
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    id("com.android.application")
    kotlin(io.github.aouerfelli.subwatcher.Kotlin.android)
    kotlin(io.github.aouerfelli.subwatcher.Kotlin.kapt)
    id("com.squareup.sqldelight")
}

android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "io.github.aouerfelli.subwatcher"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        this as KotlinJvmOptions
        jvmTarget = "1.8"
        @Suppress("SuspiciousCollectionReassignment")
        freeCompilerArgs += listOf(
            "-progressive",
            "-XXLanguage:+NewInference",
            "-XXLanguage:+InlineClasses",
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlinx.coroutines.FlowPreview"
        )
    }
    viewBinding {
        isEnabled = true
    }
    packagingOptions {
        pickFirst("META-INF/kotlinx-coroutines-core.kotlin_module")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(kotlin(Kotlin.stdlib, KotlinCompilerVersion.VERSION))
    implementation(Dependencies.KotlinX.coroutines)

    implementation(Dependencies.AndroidX.appcompat)
    implementation(Dependencies.AndroidX.activity)
    implementation(Dependencies.AndroidX.fragment)
    implementation(Dependencies.AndroidX.core)
    implementation(Dependencies.AndroidX.constraintLayout)
    implementation(Dependencies.AndroidX.recyclerView)
    implementation(Dependencies.AndroidX.swipeRefreshLayout)
    implementation(Dependencies.AndroidX.browser)

    implementation(Dependencies.material)

    implementation(Dependencies.AndroidX.Lifecycle.liveData)
    implementation(Dependencies.AndroidX.Lifecycle.viewModel)
    implementation(Dependencies.AndroidX.Lifecycle.viewModelSavedState)

    implementation(Dependencies.coil)

    implementation(Dependencies.Dagger.runtime)
    kapt(Dependencies.Dagger.compiler)
    implementation(Dependencies.Dagger.androidRuntime)
    kapt(Dependencies.Dagger.androidCompiler)
    compileOnly(Dependencies.Dagger.AssistedInject.runtime)
    kapt(Dependencies.Dagger.AssistedInject.compiler)

    implementation(Dependencies.OkHttp.client)
    implementation(Dependencies.OkHttp.loggingInterceptor)
    implementation(Dependencies.Retrofit.client)
    implementation(Dependencies.Retrofit.moshiConverter)
    implementation(Dependencies.Moshi.runtime)
    kapt(Dependencies.Moshi.compiler)

    implementation(Dependencies.SqlDelight.android)
    implementation(Dependencies.SqlDelight.coroutines)

    implementation(Dependencies.timber)

    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
}