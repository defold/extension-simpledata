# Component

[Api Documentation](https://defold.com/ref/stable/dmGameObject/)

[Api Source](https://github.com/defold/defold/blob/dev/engine/gameobject/src/dmsdk/gameobject/component.h)

## Registering the component type

Since the number of functions for a component type is large, we instead require the component extension to register only a Create/Destroy function.

    DM_DECLARE_COMPONENT_TYPE(ComponentTypeSimpleDataExt, "simpledatac", dmSimpleData::CompTypeSimpleDataCreate, dmSimpleData::CompTypeSimpleDataDestroy);

Note here that the matching file type needs to be specified here!

## The base structures

### The context

It's not strictly necessary to create a context.
However, for some global settings, e.g from reading from the settings file, the context is a good place to store this info.

    MyComponentContext* context = new MyComponentContext;
    context->m_MaxComponentsPerWorld = dmConfigFile::GetInt(ctx->m_Config, "mycomponent.max_count", 1024);

### The world

For each created collection in the game, the registered component types get a call to the "NewWorld" function (`CompSimpleDataNewWorld` in this example).
You create your own instance, and will store information relevant to that particular component type, such as the instances.

In this example we create an `dmObjectPool` to store our component instances in.
It allows us to have an array of a known max size, while at the same time allocate/deallocate objects at random, without the need to compact the array etc.

### The component

This is also a custom type which is allocated during the component "Create" function (here `CompSimpleDataCreate`).

You may choose to return a pointer, but since we are using an object pool, these instances are actually an `uint32_t`, which makes it easier to manage the object pool.

This however also means that we have to implement the `CompSimpleDataGetComponent()` which is responsible for translating our internal representation (our `uint32_t`) into an actual component pointer. It is mainly used in helper functions when implementing a Lua scripting api ([example](https://github.com/defold/extension-spine/blob/e7f4670193a4f6eec7a81d91beaaf07b87a7d724/defold-spine/src/script_spine.cpp#L278)).


## The functions

There are many functions available to implement, depending on what the desired feature set will be.

For a full example, it might be good to look at the [comp_sprite.cpp](https://github.com/defold/defold/blob/dev/engine/gamesys/src/gamesys/components/comp_sprite.cpp) or its [likes](https://github.com/defold/defold/tree/dev/engine/gamesys/src/gamesys/components).

### Getting properties

In this example, we only required to implement a get function.

We get a call to the `CompSimpleDataGetProperty` function, with a request for the value of `params.m_PropertyId`. It is of type `dmhash_t`. Use e.g. `dmHashString64()`/`dmHashBuffer64()` to produce such hashes.

Note that the `params.m_Options` (type `dmGameObject::PropertyOptions`) allows the requester to ask for an index or a key.

If we get a valid request, then we fill out the `out_value.m_Variant` with a value, of type `dmGameObject::PropertyVar`, then we return `dmGameObject::PROPERTY_RESULT_OK`.



