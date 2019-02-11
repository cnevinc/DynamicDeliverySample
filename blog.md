Firefox Lite is a browser targeting small APK size, tailored made for data plan sensitive users.
Adding features without increasing the initial APK size, but also having a feature-rich browser, is our ultimate goal.
This is where Dynamic Feature comes to play. Users can have the benifit of both world.

This blog post describe an issue I encountered when I tried to integrate with Dynamic Feature. I want to write it down for myself.

tldr; Service Loader in Dynamic Feature works un-expectedly. See the "Workaround" session if that's the only thing that interests you.

# Pre-reading
Basic understanding of Dynamic Feature and how it works.
ServiceLoader
ClassLoader
DexPathList


# Background
We want to add a feature module called history, which will use FirefoxAccount library to get your browsing history under your FxAccount.
It has some Rust code, and use Kotlin Coroutine as a wrapper.

After I make it a dynamic module, I start an activity in the dynamic module, it crashed. Said: FileNotFound Exception, can't find

I looked at the source code, the exception is thrown by Dispatcher, it want to find at least an implementation of MainDispatcherFactory, wich is AndroidDispatcherFactory. I'm not familiar with CoroutineContext. So I dig into the Coroutine source code. I found when you want to launch a Coroutine, you need to specify the thread(Dispatcher) it will run on. And kotlin-coroutine-android has define it's own implementation of Dispatcher implementation.

So the flow diagram is like this:

Activity in Feature module -> CoroutineContext -> Dispatcher.Main -> MainDispatcherFactory(inteface) -> AndroidDispatcherFactory(implemenation)

How do our app it's implemetation?
This decision happens in compile time, this is when ServiceLoader comes to play.


# ServiceLoader
To see it's official denfinition, it's <link>
To me, it's a wrapper around ClassLoader. It lookup the class (resrouce) in ServiceLoader's DexPathList, where all your dex files resides(in installed APKs). ClassLoader only load classes when they are used. whe we call ServiceLoader.load(), it'll lookup the class in the dex files and return the an iterator with all the implementaion it found.

Normally the dex path is under /data/app/your_package_name-<some number>.apk


# How Dynamic modules are installed/side-loaded?
First, the Play Store download your base apk + configuration apk + any APKs that's "must-needed" for a user. 
See <Play Document>.
For feature modules, when you call SplitInfoManager.requestInstall(), it'll talks to Play Core libraries and download the feature apks for you, then install them.

How do we simulate this process?

We can use `install-multiple` command. So we can have a debuggable version and see this error.
If you go through Play Store, since it's signed with release key, it's not debuggable. And you also don't have the root access, you can't see what's upder /data/app.


# Reproduce the crash
I feel CoroutineContext and the Dispatcher loading process is too complicated for a sample project, I've wrote a demo app using com.example.myapplication
that uses a simple ServiceLoader directly.
You need : 
1. An emulator without Play Store (so you can run adb root)
2. clone the sample project
3. Go throw the README or just find the errors there.



# Workaround
1. Call ServiceLoader.load() after installing the dynamic module and before using it in the feature module. or
2. Only use the ServiceLoader.load() in the feature module.

# Extend exploratio

1. When using ServiceLoader, if we've already found the implementation in the base module, can we update the implementation in the feature module?
2. Does above happens in run-time? or can we restart the app to make it apply that change? (I don't know how to really restart an application though)
3. Duplicated dependency problems: Modularization

Previously, when we want to modularize the our project, it goes like this:

app -> module1
    -> module2
    -> module3

The app(base) module knows everything about the feature modules, but not vice versa.
If we want to modules to talk to each other, we need to use callback or observer patter.

Now in Dynamic Feature world, the situation changes:

dynamic feature module1 -> app -> normal feature module1
dynamic feature module2        -> normal feature module2
dynamic feature module3        -> normal feature module3


dynamic feature modules will knows everything about app module, and if you use "api" instead of "implementation" when you declaure your depenedncies from app module to normal feature modules in app/build.gradle file, dynamic feature modules can even sees the normal feature modules.

Circular dependencies issue may also occur.

For Example:

dynamic module and normal module both uses kotlinx-android-coroutine, what will happen if 
dynamic module -> kotlinx-android-coroutine:1.1.1
normal module -> kotlinx-android-coroutine:1.0.1

Questions
1. Will it blow up the apk size?
2. What implentation will they be used in each module? What's the expected behavior


There are some basics design principle
1. if dynamic module and base module used the same dependency : put the dependency in the base module.
2. if dynamic module and normal module used the same dependency : put the dependency in the normal module.
=> always put the dependency in the leaf module???

