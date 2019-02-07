ServiceLoader work un-expectedly in the feature module
=====

# Goal
This project is to demo the problem to use the ServiceLoader in the featuer module.

# Sympton
app module has a dependency that uses ServiceLoader. But if we install the feature module and call the servcieLoader there, the app will crash and throw a FileNotFound exception. (On Android Vitals on Google Play it's ClassNotFoundException)

For example, app module has a dependency that uses `kotlinx-coroutines-android library`, which will use ServiceLoader to load `MainDispatcherFactory`(which implmentation is `AndroidDispatcherFactory`), the app will crash if we don't call the Serviceloader before installing the feature mpdule. [source code](https://github.com/Kotlin/kotlinx.coroutines/blob/63b4673d65f2c7836a23d3bf5a6d1a8aeea862d9/core/kotlinx-coroutines-core/src/internal/MainDispatchers.kt#L15)

The crash stack
```
Unable to open zip file: /data/app/com.example.myapplication-A5NKQCQp_U8CmibwVYekXw==/base.apk
java.io.FileNotFoundException: File doesn't exist: /data/app/com.example.myapplication-rFWlExb6yWYKdy1zvCu9Uw==/base.apk
        at java.util.zip.ZipFile.<init>(ZipFile.java:215)
        at java.util.zip.ZipFile.<init>(ZipFile.java:152)
        at java.util.jar.JarFile.<init>(JarFile.java:160)
        at java.util.jar.JarFile.<init>(JarFile.java:97)
        at libcore.io.ClassPathURLStreamHandler.<init>(ClassPathURLStreamHandler.java:47)
        at dalvik.system.DexPathList$Element.maybeInit(DexPathList.java:702)
        at dalvik.system.DexPathList$Element.findResource(DexPathList.java:729)
        at dalvik.system.DexPathList.findResources(DexPathList.java:526)
        at dalvik.system.BaseDexClassLoader.findResources(BaseDexClassLoader.java:174)
        at java.lang.ClassLoader.getResources(ClassLoader.java:839)
        at java.util.ServiceLoader$LazyIterator.hasNextService(ServiceLoader.java:349)
        at java.util.ServiceLoader$LazyIterator.hasNext(ServiceLoader.java:402)
        at java.util.ServiceLoader$1.hasNext(ServiceLoader.java:488)
        at kotlin.collections.CollectionsKt___CollectionsKt.toCollection(_Collections.kt:1132)
        at kotlin.collections.CollectionsKt___CollectionsKt.toMutableList(_Collections.kt:1165)
        at kotlin.collections.CollectionsKt___CollectionsKt.toList(_Collections.kt:1156)
        at kotlinx.coroutines.MainDispatcherLoader.<clinit>(Dispatchers.kt:96)
        at kotlinx.coroutines.Dispatchers.getMain(Dispatchers.kt:53)
        at com.example.history.HistoryActivity.getCoroutineContext(HistoryActivity.kt:19)
```
Some debugger information : the MainDispatcherFactory's classLoader:
```
dalvik.system.PathClassLoader[
	DexPathList[
		[
			zip file "/data/app/com.example.myapplication-A5NKQCQp_U8CmibwVYekXw==/base.apk", 
			zip file "/data/app/com.example.myapplication-DUm7FI9ZjDHMN-H_V4UgOQ==/split_history.apk"
		],  nativeLibraryDirectories=[/system/lib]
	]
]
```

Looks like it's looking for the base.apk, but the path for the base.apk has changed. (See STR #5 & #6)

# STR

1. make sure Line 50 in /app/build.gralde is enabled and run `./gradlew bundleDebug`
2. `bundletool build-apks --bundle=./app/build/outputs/bundle/debug/app.aab  --output=./app/build/outputs/bundle/debug/app.apks`
3. `mkdir ./app/build/outputs/bundle/debug/s`
4. `bundletool extract-apks --apks=./app/build/outputs/bundle/debug/app.apks --output-dir=./app/build/outputs/bundle/debug/s --device-spec=./d.json --modules=history`
5. `adb install -r ./app/build/outputs/bundle/debug/s/base-master.apk` # at this time, the file path for base apk is `/data/app/com.example.myapplication-A5NKQCQp_U8CmibwVYekXw==/base.apk`
6. `adb install-multiple --dont-kill -p com.example.myapplication ./app/build/outputs/bundle/debug/s/history-master.apk` # at this time, the file path for base apk is `/data/app/com.example.myapplication-DUm7FI9ZjDHMN-H_V4UgOQ==/base.apk`, which is the same with `split_history.apk`

note:
if we change the name of the path in file system from `/data/app/com.example.myapplication-DUm7FI9ZjDHMN-H_V4UgOQ==/base.apk` to `/data/app/com.example.myapplication-A5NKQCQp_U8CmibwVYekXw==/base.apk` , the app won't crash 

# Project structure
```
-----app--------MainActivity (depdends on library "org.mozilla.components:browser-domains:0.39.0")
 | 			  
 |---history----HistoryActivity implement CoroutineContext) (depends on "kotlinx-coroutines-android:1.0.1")
```
	

# Work around
My on-demand feature module depends on `kotlinx-coroutines-android:1.0.1`

My library `org.mozilla.components:browser-domains:0.39.0` also depends on `kotlinx-coroutines-android:1.0.1`

If I use ServiceLoader to load `MainDispatcherFactory`	's implementation in app module before I install the feature module, the app won't crash.

See: https://github.com/cnevinc/DynamicDeliverySample/tree/workaround







