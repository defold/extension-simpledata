
#include <dmsdk/sdk.h>

#include "comp_simple_data.h"

static const char* SIMPLEDATA_EXT = "simpledatac";

static int SimpleDataComp_GetArray(lua_State* L)
{
    dmSimpleData::SimpleDataComponent* component = 0;
    dmGameObject::GetComponentFromLua(L, 1, SIMPLEDATA_EXT, 0, (void**)&component, 0);
    assert(component);

    float* data    = 0;
    uint32_t count = 0;
    dmSimpleData::GetArrayData(component, &data, &count);

    lua_createtable(L, count, 0);
    for(int i=0; i < count; i++)
    {
        lua_pushinteger(L, i+1);
        lua_pushnumber(L, data[i]);
        lua_settable(L, -3);
    }
    return 1;
}

static dmExtension::Result AppInitializeSimpleData(dmExtension::AppParams* params)
{
    return dmExtension::RESULT_OK;
}

static const luaL_reg SIMPLEDATA_COMP_FUNCTIONS[] =
{
    {"get_array", SimpleDataComp_GetArray},
    {0, 0}
};

static dmExtension::Result InitializeSimpleData(dmExtension::Params* params)
{
    dmLogInfo("Registered extension-simpledata\n");

    luaL_register(params->m_L, "simpledata", SIMPLEDATA_COMP_FUNCTIONS);
    lua_pop(params->m_L, 1);
    
    return dmExtension::RESULT_OK;
}

static dmExtension::Result AppFinalizeSimpleData(dmExtension::AppParams* params)
{
    return dmExtension::RESULT_OK;
}

static dmExtension::Result FinalizeSimpleData(dmExtension::Params* params)
{
    return dmExtension::RESULT_OK;
}

DM_DECLARE_EXTENSION(SimpleDataExt, "SimpleDataExt", AppInitializeSimpleData, AppFinalizeSimpleData, InitializeSimpleData, 0, 0, FinalizeSimpleData);
