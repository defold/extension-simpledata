#include <string.h> // memset

#include <dmsdk/dlib/log.h>
#include <dmsdk/dlib/math.h>
#include <dmsdk/dlib/object_pool.h>

#include <dmsdk/gameobject/component.h>
#include <dmsdk/gamesys/property.h>

#include <gameobject/gameobject_ddf.h>

#include "comp_simple_data.h"
#include "simpledata_ddf.h" // generated from the simpledata_ddf.proto
#include "res_simpledata.h"

namespace dmSimpleData
{
    // using namespace dmVMath;
    static const dmhash_t PROP_NAME = dmHashString64("name");
    static const dmhash_t PROP_F32 = dmHashString64("f32");
    static const dmhash_t PROP_U32 = dmHashString64("u32");
    static const dmhash_t PROP_I32 = dmHashString64("i32");
    static const dmhash_t PROP_U64 = dmHashString64("u64");
    static const dmhash_t PROP_I64 = dmHashString64("i64");
    static const dmhash_t PROP_V3 = dmHashString64("v3");
    static const dmhash_t PROP_ARRAY_F32 = dmHashString64("array_f32");

    static const char* PROJECT_PROPERTY_MAX_COUNT = "simpledata.max_count";

    // One context for the life time of the app
    struct SimpleDataContext
    {
        SimpleDataContext()
        {
            memset(this, 0, sizeof(*this));
        }
        // ...
        uint32_t m_MaxComponentsPerWorld;
    };

    // One world per loaded collection
    struct SimpleDataWorld
    {
        SimpleDataContext*                  m_Context;
        dmObjectPool<SimpleDataComponent>   m_Components;
    };

    dmGameObject::CreateResult CompSimpleDataNewWorld(const dmGameObject::ComponentNewWorldParams& params)
    {
        SimpleDataContext* context = (SimpleDataContext*)params.m_Context;
        SimpleDataWorld* world = new SimpleDataWorld();
        world->m_Context = context;

        uint32_t comp_count = dmMath::Min(context->m_MaxComponentsPerWorld, params.m_MaxComponentInstances);
        world->m_Components.SetCapacity(comp_count);
        memset(world->m_Components.m_Objects.Begin(), 0, sizeof(SimpleDataComponent) * comp_count);
        *params.m_World = world;
        return dmGameObject::CREATE_RESULT_OK;
    }

    dmGameObject::CreateResult CompSimpleDataDeleteWorld(const dmGameObject::ComponentDeleteWorldParams& params)
    {
        SimpleDataWorld* world = (SimpleDataWorld*)params.m_World;
        delete world;
        return dmGameObject::CREATE_RESULT_OK;
    }

    dmGameObject::CreateResult CompSimpleDataCreate(const dmGameObject::ComponentCreateParams& params)
    {
        SimpleDataWorld* world = (SimpleDataWorld*)params.m_World;

        if (world->m_Components.Full())
        {
            dmLogError("SimpleData component could not be created since the buffer is full (%d). See '%s' in game.project", world->m_Components.Capacity(), PROJECT_PROPERTY_MAX_COUNT);
            return dmGameObject::CREATE_RESULT_UNKNOWN_ERROR;
        }

        uint32_t index = world->m_Components.Alloc();
        SimpleDataComponent* component = &world->m_Components.Get(index);
        memset(component, 0, sizeof(SimpleDataComponent)); // Don't memset if it contains non-pod types
        component->m_Resource = (SimpleDataResource*)params.m_Resource;
        *params.m_UserData = index;
        return dmGameObject::CREATE_RESULT_OK;
    }

    static inline SimpleDataComponent* GetComponentFromIndex(SimpleDataWorld* world, int index)
    {
        return &world->m_Components.Get(index);
    }

    static void* CompSimpleDataGetComponent(const dmGameObject::ComponentGetParams& params)
    {
        SimpleDataWorld* world = (SimpleDataWorld*)params.m_World;
        uint32_t index = (uint32_t)*params.m_UserData;
        return GetComponentFromIndex(world, index);
    }

    static void DestroyComponent(SimpleDataWorld* world, uint32_t index)
    {
        SimpleDataComponent* component = &world->m_Components.Get(index);

        world->m_Components.Free(index, true);
    }

    dmGameObject::CreateResult CompSimpleDataDestroy(const dmGameObject::ComponentDestroyParams& params)
    {
        //SimpleDataContext* ctx = (SimpleDataContext*)params.m_Context;
        SimpleDataWorld* world = (SimpleDataWorld*)params.m_World;
        uint32_t index = *params.m_UserData;
        DestroyComponent(world, index);
        return dmGameObject::CREATE_RESULT_OK;
    }

    // This extension doesn't need an update
    // dmGameObject::UpdateResult CompSimpleDataUpdate(const dmGameObject::ComponentsUpdateParams& params, dmGameObject::ComponentsUpdateResult& update_result)
    // {
    //     return dmGameObject::UPDATE_RESULT_OK;
    // }

    static bool OnResourceReloaded(SimpleDataWorld* world, SimpleDataComponent* component, int index)
    {
        // Handle the updating of the properties if they were reloaded
        return true;
    }

    void CompSimpleDataOnReload(const dmGameObject::ComponentOnReloadParams& params)
    {
        SimpleDataWorld* world = (SimpleDataWorld*)params.m_World;
        int index = *params.m_UserData;
        SimpleDataComponent* component = GetComponentFromIndex(world, index);
        component->m_Resource = (SimpleDataResource*)params.m_Resource;
        (void)OnResourceReloaded(world, component, index);
    }

    dmGameObject::PropertyResult CompSimpleDataGetProperty(const dmGameObject::ComponentGetPropertyParams& params, dmGameObject::PropertyDesc& out_value)
    {
        //SimpleDataContext* context = (SimpleDataContext*)params.m_Context;
        SimpleDataWorld* world = (SimpleDataWorld*)params.m_World;
        SimpleDataComponent* component = GetComponentFromIndex(world, *params.m_UserData);
        dmGameSystemDDF::SimpleDataDesc* ddf = component->m_Resource->m_DDF;

    #define HANDLE_PROP(NAME, VALUE) \
        if (params.m_PropertyId == NAME) \
        { \
            out_value.m_Variant = dmGameObject::PropertyVar((VALUE)); \
            return dmGameObject::PROPERTY_RESULT_OK; \
        }

    #define HANDLE_ARRAY_PROP(NAME, VALUE) \
        if (params.m_PropertyId == NAME) \
        { \
            if (params.m_Options.m_HasKey) \
                return dmGameObject::PROPERTY_RESULT_INVALID_INDEX; \
            if (params.m_Options.m_Index < 0 || params.m_Options.m_Index >= (VALUE).m_Count) \
            { \
                dmLogError("Index %u is out of bounds. Array %s only has %u elements.", params.m_Options.m_Index, dmHashReverseSafe64(params.m_PropertyId), (VALUE).m_Count); \
                return dmGameObject::PROPERTY_RESULT_INVALID_INDEX; \
            } \
            float value = (VALUE).m_Data[params.m_Options.m_Index]; \
            out_value.m_Variant = dmGameObject::PropertyVar(value); \
            return dmGameObject::PROPERTY_RESULT_OK; \
        }

        HANDLE_PROP(PROP_NAME, dmHashString64(ddf->m_Name));
        HANDLE_PROP(PROP_F32, ddf->m_F32);
        HANDLE_PROP(PROP_U32, (float)ddf->m_U32);
        HANDLE_PROP(PROP_I32, (float)ddf->m_I32);
        HANDLE_PROP(PROP_U64, (double)ddf->m_U64);
        HANDLE_PROP(PROP_I64, (double)ddf->m_I64);

        HANDLE_PROP(PROP_V3, ddf->m_V3); // dmVMath::Vector3

        // Indices are already converted from Lua 1-based to C 0-based
        HANDLE_ARRAY_PROP(PROP_ARRAY_F32, ddf->m_ArrayF32);

    #undef HANDLE_PROP
    #undef HANDLE_ARRAY_PROP
        return dmGameObject::PROPERTY_RESULT_NOT_FOUND;
    }

    static dmGameObject::Result CompTypeSimpleDataCreate(const dmGameObject::ComponentTypeCreateCtx* ctx, dmGameObject::ComponentType* type)
    {
        SimpleDataContext* component_context = new SimpleDataContext;

        component_context->m_MaxComponentsPerWorld = dmConfigFile::GetInt(ctx->m_Config, PROJECT_PROPERTY_MAX_COUNT, 1024);

        // Component type setup
        ComponentTypeSetPrio(type, 1050);

        ComponentTypeSetContext(type, component_context);
        ComponentTypeSetHasUserData(type, true);
        ComponentTypeSetReadsTransforms(type, false);

        ComponentTypeSetNewWorldFn(type, CompSimpleDataNewWorld);
        ComponentTypeSetDeleteWorldFn(type, CompSimpleDataDeleteWorld);
        ComponentTypeSetCreateFn(type, CompSimpleDataCreate);
        ComponentTypeSetDestroyFn(type, CompSimpleDataDestroy);
            // ComponentTypeSetInitFn(type, CompSimpleDataInit);
            // ComponentTypeSetFinalFn(type, CompSimpleDataFinal);
            // ComponentTypeSetAddToUpdateFn(type, CompSimpleDataAddToUpdate);
            // ComponentTypeSetUpdateFn(type, CompSimpleDataUpdate);
            // ComponentTypeSetRenderFn(type, CompSimpleDataRender);
            // ComponentTypeSetOnMessageFn(type, CompSimpleDataOnMessage);
            // ComponentTypeSetOnInputFn(type, CompSimpleDataOnInput);
        ComponentTypeSetOnReloadFn(type, CompSimpleDataOnReload);
            // ComponentTypeSetSetPropertiesFn(type, CompSimpleDataSetProperties);
        ComponentTypeSetGetPropertyFn(type, CompSimpleDataGetProperty);
            // ComponentTypeSetSetPropertyFn(type, CompSimpleDataSetProperty);
            // ComponentTypeSetPropertyIteratorFn(type, CompSimpleDataIterProperties);
            // ComponentTypeSetGetFn(type, CompSimpleDataGetComponent);

        return dmGameObject::RESULT_OK;
    }

    static dmGameObject::Result CompTypeSimpleDataDestroy(const dmGameObject::ComponentTypeCreateCtx* ctx, dmGameObject::ComponentType* type)
    {
        SimpleDataContext* component_context = (SimpleDataContext*)ComponentTypeGetContext(type);
        delete component_context;
        return dmGameObject::RESULT_OK;
    }

    void GetArrayData(SimpleDataComponent* component, float** data, int* count)
    {
        dmGameSystemDDF::SimpleDataDesc* ddf = component->m_Resource->m_DDF;
        assert(ddf);
        *data = ddf->m_ArrayF32.m_Data;
        *count = ddf->m_ArrayF32.m_Count;
    }
}

DM_DECLARE_COMPONENT_TYPE(ComponentTypeSimpleDataExt, "simpledatac", dmSimpleData::CompTypeSimpleDataCreate, dmSimpleData::CompTypeSimpleDataDestroy);
