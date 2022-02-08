# UselessGradlePlugin

Plugin: [![](https://jitpack.io/v/luiqn2007/UselessGradlePlugin.svg)](https://jitpack.io/#luiqn2007/UselessGradlePlugin)

Lib: [![](https://jitpack.io/v/luiqn2007/UselessPluginLib.svg)](https://jitpack.io/#luiqn2007/UselessPluginLib)

How to use: 

 1. Add plugin to project

    ```groovy

    buildscript() {
        repositories {
            maven { url 'https://jitpack.io' }
        }

        dependencies {
            classpath 'com.github.luiqn2007:UselessGradlePlugin:1.1.2'
        }
    }
    
    apply plugin: 'io.github.luiqn2007.gradle_plugins.build_tasks'
    
    ```

  2. Add lib to project

     ```groovy
     allprojects {
         repositories {
             maven { url 'https://jitpack.io' }
         }
	 }
     
     pendencies {
         compileOnly 'com.github.luiqn2007:UselessPluginLib:1.1.0'
     }
     ```
     
  3. Create source to package `lq2007.plugins.gradle_plugin`
and implement `lq2007.plugins.gradle_plugin.support.ISourcePlugin` interface

**Plugin version a.b.x must use lib version a.b.y**
