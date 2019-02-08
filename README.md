ServiceLoaders work un-expectedly in the feature module
=====
# App structure
- app: base module
- histort: feature module
- myserviceloader: Service Loader Interface
- depedency: history--(implmentation)-->app---(api)-->myserviceloader
- diagram
```
-------app--------------- MainActivity
    |
    |    
    |--history----------- HistoryActivity
    |
    |
    |--myserviceloader--- com.example.myserviceloader.IFactory                        (interface)
                       |- com.example.myserviceloader.FactoryImplGecko                (implementaion1)
                       |- com.example.myserviceloader.FactoryImplWebkit               (implementaion2)
                       |- META-INF/services/com.example.myserviceloader.IFactory      (configuration)
```
# How to reproduce the issue
## Happy path:
- run below command to install base apk
```
rm -rf ./app/build/outputs/bundle/debug/ && \
./gradlew bundleDebug && \
bundletool build-apks --bundle=./app/build/outputs/bundle/debug/app.aab --output=./app/build/outputs/bundle/debug/app.apks && \
mkdir ./app/build/outputs/bundle/debug/s && \
bundletool extract-apks --apks=./app/build/outputs/bundle/debug/app.apks --output-dir=./app/build/outputs/bundle/debug/s --device-spec=./d.json --modules=history && \
adb install -r ./app/build/outputs/bundle/debug/s/base-master.apk
```
- start the app, click "Load", see a toast "Gecko Implemenation"
- click "Request Install", see a toast "install fail...."
- install the feature module mannually
```
adb install-multiple --dont-kill -p com.example.myapplication ./app/build/outputs/bundle/debug/s/history-master.apk
```
- click "Request Install" again, a new activity popped up, you'll see `Gecko Implemetnation`


## Problem path:
- run below command to install base apk
```
rm -rf ./app/build/outputs/bundle/debug/ && \
./gradlew bundleDebug && \
bundletool build-apks --bundle=./app/build/outputs/bundle/debug/app.aab --output=./app/build/outputs/bundle/debug/app.apks && \
mkdir ./app/build/outputs/bundle/debug/s && \
bundletool extract-apks --apks=./app/build/outputs/bundle/debug/app.apks --output-dir=./app/build/outputs/bundle/debug/s --device-spec=./d.json --modules=history && \
adb install -r ./app/build/outputs/bundle/debug/s/base-master.apk
```
- click "Request Install", see a toast "install fail...."
- install the feature module mannually
```
adb install-multiple --dont-kill -p com.example.myapplication ./app/build/outputs/bundle/debug/s/history-master.apk
```
- click "Request Install" again, a new activity popped up, you'll see `No Implementation found....` means no implmenation's loaded by ServiceLoader.
- press back, click "Load" button on MainActivity. You'll see a toast "can't find any implementation. Check the DexPathList?" 
- After app restart, it'll work

# Dive depper
- If the ServiceLoader didn't find any implementaion, it'll only throw exception, the app won't crash. But some library (e.g. CoroutineContext, will throw an IllegalStateException: Module with the Main dispatcher is missing. Add dependency providing the Main dispatcher, e.g. 'kotlinx-coroutines-android') will want to crash the app.
- Look at the ClassLoader the ServiceLoader used:
```
DexPathList[
	[
		zip file "/data/app/com.example.myapplication-PJFTWN7HgF29OEEyZUEqig==/base.apk", 
		zip file "/data/app/com.example.myapplication-Oj-zgB0U9iIxRSItgdLwhQ==/split_history.apk"
	],
	nativeLibraryDirectories=[/data/app/com.example.myapplication-PJFTWN7HgF29OEEyZUEqig==/lib/x86, /system/lib]
]
```
- And the crash trace
```
Unable to open zip file: /data/app/com.example.myapplication-PJFTWN7HgF29OEEyZUEqig==/base.apk
java.io.FileNotFoundException: File doesn't exist: /data/app/com.example.myapplication-PJFTWN7HgF29OEEyZUEqig==/base.apk
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
        at com.example.history.HistoryActivity.onCreate(HistoryActivity.kt:24)
```
- And in rooted device, you will find `base.apk`, `lib` and `split_history.apk` are in `/data/app/com.example.myapplication-Oj-zgB0U9iIxRSItgdLwhQ==/` folder
- I found when you first install the base.apk, the path was `PJFTWN7HgF29OEEyZUEqig`. After installing featude.apk, it changed to `Oj-zgB0U9iIxRSItgdLwhQ`. I think that's the reason ServiceLoader can't find `base.apk`

- the happy path will work because the class loader has the implementation before the feature module installed.
- for the problem path to work, you can change the path to the original name `PJFTWN7HgF29OEEyZUEqig` after install the feature module before launching it, the ServiceLoader can work.




