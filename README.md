# Android Document Scanner Library 

A powerful and efficient Android library that leverages the capabilities of OpenCV and MLKit to provide fast and precise document scanning functionality.

Please ⭐ this library if you found it useful and/or want to support its development.

## Features

- [x] Fast and accurate document detection
- [x] Perspective correction
- [x] Image enhancement
- [x] Cropping support



## Installation

### Method 1: Clone the demo app 

1. Clone the repository using the following command:

```bash
git clone https://github.com/entropyconquers/android-document-scanner-library.git
```

2. Open the project in Android Studio and run the app module.

### Method 2: Add the `scanLib` module to your project

1. Clone the repository using the following command:

```bash
git clone https://github.com/entropyconquers/android-document-scanner-library.git
```

2. Open your project in Android Studio and select `File -> New -> Import Module`

3. Select the `scanLib` module from the cloned repository and click `Finish`

4. Add the following lines to your app's `build.gradle` file:

```groovy
dependencies {
    implementation project(':scanLib')
}
```

5. Add mavencentral and jitpack repositories to your project's `build.gradle` | `settings.gradle` files:

```groovy

// build.gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url 'https://jitpack.io' }
    }
}

// settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url 'https://jitpack.io' }
    }
}
```

6. Sync your project with gradle files

## Usage

### 1. Initialize the Activity as `BaseScannerActivity`

```kotlin

class MainActivity : BaseScannerActivity() {
    override fun onError(throwable: Throwable) {
        Log.d(ContentValues.TAG, "ERROR A")
        when (throwable) {
            is NullCorners -> Toast.makeText(
                this,
                "R.string.null_corners", Toast.LENGTH_LONG
            )
                .show()
            else -> Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDocumentAccepted(bitmap: Bitmap) {
    }

    override fun onClose() {
        finish()
    }
}
```

The `BaseScannerActivity` class provides the following abstract methods that you need to implement:

```kotlin
abstract fun onError(throwable: Throwable) // Called when an error occurs
abstract fun onDocumentAccepted(bitmap: Bitmap) // Called when a document is accepted, returns the scanned bitmap
abstract fun onClose() // Called when the user presses the close button
```

## Credits

This library would not have been possible without the following projects:

[Kuama-IT, Android Document Scanner](https://github.com/Kuama-IT/android-document-scanner)

## License

```
MIT License
```




