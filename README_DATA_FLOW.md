# The data flow

## The properties

Each component type is connected to a file type.
E.g. for a Sprite component, that file type is ".sprite"
This file contains the properties of the sprite.

However, it's not necessary to use a file on disc to represent the
data. We also support something we call "Embedded components".

This means that we embed the properties directly in the .collection file.

After build time though, these properties are still written out to disc
and will thus be required to be read as a resource (See [res_simpledata.cpp](defold-simpledata/src/res_simpledata.cpp))

## .proto files

The `.proto` format is referring to Google's [Protocol Buffers](https://developers.google.com/protocol-buffers) format (a.k.a `protobuf`).
It allows you to specify structs with member variables and types.

We use protobuf extensively in Defold as the main data format. Most of our source file formats are defined this way.

In this example, we will use a `.proto` file to define what properties we want in our component.

At build time, this `.proto` file will generate C++/Java files that will be usable in the plugins.

## The plugins

We create a Java plugin ( `.jar`) that both Bob and Editor will find and load dynamically at project startup.
In this plugin, you need to add things that is relevant for each toolchain.
E.g. for Bob, you need to supply a `Builder` class, that transforms the data format into the correct output format.

The Java plugin also has the possibility to load native (C++) code from the shared libraries you provide.

## The Editor

For the Editor, the plugin need to provide the necessary information (if needed).
It might for instance be to provide all the vertices/normals for a model.

The editor plugin `.clj` allows for presenting a UI representing the component type.
It may choose different ways of editing the properties (e.g. color picker etc.)

When creating a new component, the corresponding `template.<fileformat>` is used.
This template is supplied when registering the resource type in the editor.

The editor will also be responsible for producing the save data.
It may choose to call into the Java plugin in order to do so.

## Bob

Bob is used as a standalone tool, and it's also used by the Editor whenever you choose the `Bundle` option.

In Bob, you register a `Builder` class together with some input/output file formats, specified with the `BuilderParams` decorator.

In our example, we use a ready made `ProtoBuilder` base class, which makes use of the `ProtoParams` decorator.
It transforms the protobuf data from text to binary format. No extra processing is needed in our example.

## Engine extension

The `DM_DECLARE_EXTENSION` allows you to hook in a generic extension with life cycle functions for init/exit and updates.
This is usually the place where to register any custom Lua modules.

## Engine resources

In the engine, each game object or component is stored as a resource.
This resource contains the properties of that gameobject/component.

If two instances share the same set of properties, they will also share the same resource.

The resource loading will be found in the [res_simpledata.cpp](./defold-simpledata/src/res_simpledata.cpp).
It allows you to create a resource, as well as handle hot reloading of the same resource.

Note that this resource (pointer) is given to the components, so when dealing with hot reloading, you should be aware of this fact
and deal with any indirection issues.

Since the resource is always referred to by the collection containing it, it will not go out of scope until the collection is unloaded.

## Engine component

The component type itself handles the creation of a global component context, component "Worlds" (one per collection), and component instances.
There are many life cycle functions of a component that you may implement.

The component type is paired with the resource type, so when a new component is created, the corresponding resource is also passed to it.
You may store this pointer, and use it during the life span of the resource.





