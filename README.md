# VideoOverlay
[![](https://jitpack.io/v/Rohit-Paneliya/VideoOverlay.svg)](https://jitpack.io/#Rohit-Paneliya/VideoOverlay)

This library will help you to add image/view overlay on video in easiest way.

# About
* Supports Android 10 and above
* Handled scoped storage
* You can set overlay position using local image or view object

# Gradle
Add the following to your `build.gradle` to use:

```gradle
allprojects {
  repositories {
	 maven { url 'https://jitpack.io' }
  }
}

dependencies {
   implementation 'com.github.Rohit-Paneliya:VideoOverlay:1.0'
}
```
# Usage
```
VideoOverlay.Builder(this)
                .setMainVideoFilePath(sampleVideoPath)
                .setOverlayImagePosition(OverlayPosition.BOTTOM_CENTER)
                .setOverlayImage(imageView)
                .setOutputFolderName("Output")
                .setListener(this)
                .build()
                .start()
```
