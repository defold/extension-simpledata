# How the project is organized

### <extension>/include

This folder contains headers that will be made available to other extensions.
Think of them as the "public api" for this extension.
Using this is not required, but may be good for sharing functionality between extensions.

### <extension>/src

This folder contains the C++/Java/C++ code that is used internally in the engine

### <extension>/commonsrc

This is where the code (C++, .proto files) is stored that is going to be shared with both the engine extension
and the plugin code.


### <extension>/pluginsrc

This is where code that is used with bob/editor.
The content pipeline code should be Java, to make it easier to integrate into bob/editor.
The java code may call into C++ code as well.

We don't build this code on-the-fly, but you need to call the script
[build_plugins.sh](./utils/build/build_plugins.sh) to generate the files (.jar and optionally shared libraries)

### <extension>/plugins

This is where the ready made plugins are checked in.
Both Bob and Editor knows how to find these .jar files when loading a project.

The folder structure follows this pattern:

    ./share/            -- contains the .jar files
    ./lib/x86-osx/      -- contains the .dylib files
    ./lib/x86-win32/    -- contains the .dll files
    ./lib/x86-linux/    -- contains the .so files

### <extension>/editor/src

This is where the Editor plugin code is stored.
It should be in Clojure (`.clj`) and contain a `return-plugin` function:

    (defn return-plugin []
      (fn [workspace] (load-my-plugin workspace)))
    (return-plugin)

### <extension>/editor/resources

Here we can store resources that will be accessible for the Editor, such as icons and templates


