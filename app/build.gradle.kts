plugins {
    id("com.android.application")
}

val signingStorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val signingStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val signingKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val signingKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = !signingStorePath.isNullOrBlank()
        && !signingStorePassword.isNullOrBlank()
        && !signingKeyAlias.isNullOrBlank()
        && !signingKeyPassword.isNullOrBlank()

android {
    namespace = "com.hissecointarayici.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hissecointarayici.app"
        minSdk = 23
        targetSdk = 36
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 28)
        versionName = "28.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("releaseStable") {
                storeFile = file(signingStorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("releaseStable")
            }
        }
    }
}
