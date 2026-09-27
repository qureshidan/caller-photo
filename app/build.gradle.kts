plugins { id("com.android.application") }
android {
    namespace = "local.callerphoto"
    compileSdk { version = release(36) { minorApiLevel = 1 } }
    defaultConfig {
        applicationId = "local.callerphoto"
        minSdk = 31
        targetSdk = 35
        versionCode = 5
        versionName = "0.5"
    }
}
