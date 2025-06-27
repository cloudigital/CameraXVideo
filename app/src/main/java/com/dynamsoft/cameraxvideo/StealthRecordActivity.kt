plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.serialization'
}

android {
    namespace 'com.bitcom.BerlinUhr'
    compileSdk 34

    defaultConfig {
        applicationId "com.bitcom.BerlinUhr"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = '1.8'
    }

    lint {
        abortOnError true
        baseline = file("lint-baseline.xml") // cho phép tạo baseline nếu cần
    }
}

dependencies {
    // Kotlin + coroutine + serialization
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.8.10"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2"

    // AndroidX core
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.core:core-splashscreen:1.0.1'

    // Google Material
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'com.google.android.gms:play-services-tasks:18.0.2'

    // CameraX
    def camerax_version = "1.3.0"
    implementation "androidx.camera:camera-core:$camerax_version"
    implementation "androidx.camera:camera-camera2:$camerax_version"
    implementation "androidx.camera:camera-lifecycle:$camerax_version"
    implementation "androidx.camera:camera-video:$camerax_version"
    implementation "androidx.camera:camera-view:$camerax_version"

    // Media playback (MediaSessionCompat)
    implementation 'androidx.media:media:1.7.0' // ✅ đủ dùng cho MediaStyle

    // JavaCV & FFmpeg
    implementation 'org.bytedeco:javacv:1.5.9'
    implementation 'org.bytedeco:ffmpeg:6.0-1.5.9'
    implementation 'org.bytedeco:ffmpeg:6.0-1.5.9:android-arm64'
    implementation 'org.bytedeco:ffmpeg:6.0-1.5.9:android-x86_64'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
