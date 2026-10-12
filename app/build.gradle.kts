plugins {
    id("com.android.application")
}

android {
    namespace = "com.hissecointarayici.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hissecointarayici.app"
        minSdk = 23
        targetSdk = 36
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 14)
        versionName = "14.0"
    }
}
