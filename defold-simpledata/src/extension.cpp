
#include <dmsdk/sdk.h>

#include "comp_simple_data.h"

static const char* SIMPLEDATA_EXT = "simpledatac";

/*# gets array data from a simpledata component
* Gets array data from a simpledata component from the array_f32 property into a lua table.
*
* @name simpledata.get_array_f32
* @param url [type:string|hash|url] the simpledata component to get array data from
* @examples
*
* Print all float values from the array property:
*
* ```lua
* function init(self)
*   local data_tbl = simpledata.get_array_f32("#simpledata")
*   for k,v in pairs(data_tbl) do
*       print(k .. ": " .. v)
*   end
* end
* ```
*/
static int SimpleDataComp_GetArrayF32(lua_State* L)
{
    dmSimpleData::SimpleDataComponent* component = 0;
    dmGameObject::GetComponentFromLua(L, 1, SIMPLEDATA_EXT, 0, (void**)&component, 0);
    assert(component);

    float* data    = 0;
    uint32_t count = 0;
    dmSimpleData::GetArrayF32Data(component, &data, &count);

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
    {"get_array_f32", SimpleDataComp_GetArrayF32},
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
