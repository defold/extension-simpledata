# How the project is organized


### &lt;extension&gt;/ext.manifest

The manifest file specifies the C++ main symbol which is used to be able to link the extension.

The manifest also specifies other potential settings that may be needed when building on the cloud builder.

### &lt;extension&gt;/include

This folder contains headers that will be made available to other extensions.
Think of them as the "public api" for this extension.
Using this is not required, but may be good for sharing functionality between extensions.

### &lt;extension&gt;/src

This folder contains the C++/Java/C++ code that is used internally in the engine

### &lt;extension&gt;/commonsrc

This is where the code (C++, .proto files) is stored that is going to be shared with both the engine extension
and the plugin code.


### &lt;extension&gt;/pluginsrc

This is where code that is used with bob/editor.
The content pipeline code should be Java, to make it easier to integrate into bob/editor.
The java code may call into C++ code as well.

We don't build this code on-the-fly, but you need to call the script
[build_plugins.sh](./utils/build/build_plugins.sh) to generate the files (.jar and optionally shared libraries)

### &lt;extension&gt;/plugins

This is where the ready made plugins are checked in.
Both Bob and Editor knows how to find these .jar files when loading a project.

The folder structure follows this pattern:

    ./share/            -- contains the .jar files
    ./lib/x86-osx/      -- contains the .dylib files
    ./lib/x86-win32/    -- contains the .dll files
    ./lib/x86-linux/    -- contains the .so files

### &lt;extension&gt;/editor/src

This is where the Editor plugin code is stored.
It should be in Clojure (`.clj`) and contain a `return-plugin` function:

    (defn return-plugin []
      (fn [workspace] (load-my-plugin workspace)))
    (return-plugin)

### &lt;extension&gt;/editor/resources

Here we can store resources that will be accessible for the Editor, such as icons and templates


