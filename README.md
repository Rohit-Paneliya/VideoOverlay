# VideoOverlay
[![](https://jitpack.io/v/Rohit-Paneliya/VideoOverlay.svg)](https://jitpack.io/#Rohit-Paneliya/VideoOverlay)

This library will help you to add image/view overlay on video in easiest way.

# About
* Supports Android 10 and above
* Handled scoped storage
* You can set overlay position using local image or view object
* JAR file size is ~60kb

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
            .setMainVideoFilePath(sampleVideoPath) // String file path for source video
            .setOverlayImagePosition(OverlayPosition.BOTTOM_CENTER) // Overlay image position (Optional)
            .setOverlayImage(imageView) // View object or file path of the image 
            .setOutputFolderName("Output") // Optional
            .setListener(this) // mandatory to set the listener
            .build()
            .start()        
```

# Listener
You must need to implement the interface `VideoOverlayCallBack` to get the following callbacks:
```
override fun showLoader() { }
override fun hideLoader() { }
override fun progressStatistics(statistics: ProgressStatistics) { }
override fun progressLogs(executionLogs: ExecutionLogs) { }
override fun success(outputFileUri:Uri) { } // OUTPUT: Video Uri after the successful operation
override fun failed() { }
```
