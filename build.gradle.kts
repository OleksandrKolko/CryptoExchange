plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
}