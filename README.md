# UselessGradlePlugin

Plugin: [![](https://jitpack.io/v/luiqn2007/UselessGradlePlugin.svg)](https://jitpack.io/#luiqn2007/UselessGradlePlugin)

Lib: [![](https://jitpack.io/v/luiqn2007/UselessPluginLib.svg)](https://jitpack.io/#luiqn2007/UselessPluginLib)

How to use:

1. Add plugin to project, you can find early version from jitpack.

   ```groovy

   buildscript {
       repositories {
           maven { url 'https://jitpack.io' }
       }
   }
   
   plugins {
       id "io.github.luiqn2007.gradle_plugins.build_tasks" version "1.2.4"
   }
   
   ```

2. Add lib to project

   ```groovy
    
   allprojects {
       repositories {
           maven { url 'https://jitpack.io' }
       }
   }
    
   dependencies {
       compileOnly 'in.lq2007:UselessPluginLib:1.2.0'
   }
    
   ```

3. Create source and implement `lq2007.plugins.gradle_plugin.support.ISourcePlugin`, you should put all classes to one
   package, and declare at `sourceTasks` block.

`sourceTasks`

|     parameter      |         value_type         |       default_value       | description                                 |
|:------------------:|:--------------------------:|:-------------------------:|---------------------------------------------|
|       output       |     `File`(dictionary)     | `/build/gen_task/classes` | dictionary to save the compiled file        |
|        log         | `File`(file or dictionary) |       `gen/log.txt`       | file to save log                            |
|    packageName     |          `String`          |            ``             | task source class package                   |
|     sourceSet      |          `String`          |         `"main"`          | sourceSet to find package                   |
| cleanCompiledFiles |         `boolean`          |          `true`           | true if delete compiled file while finished |
|      extTasks      |     `Iterable<String>`     |           `[]`            | add task class from dependency              |

 - `classpaths` is removed since 1.2.0, use `compileOnly` in project dependencies instead.

**Plugin version a.b.x must use lib version a.b.y**
