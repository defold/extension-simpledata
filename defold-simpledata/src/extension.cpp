
#include <dmsdk/sdk.h>

static dmExtension::Result AppInitializeSimpleData(dmExtension::AppParams* params)
{
    return dmExtension::RESULT_OK;
}

static dmExtension::Result InitializeSimpleData(dmExtension::Params* params)
{
    dmLogInfo("Registered extension-simpledata\n");
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


// DM_DECLARE_EXTENSION(symbol, name, app_init, app_final, init, update, on_event, final)
DM_DECLARE_EXTENSION(SimpleDataExt, "SimpleDataExt", AppInitializeSimpleData, AppFinalizeSimpleData, InitializeSimpleData, 0, 0, FinalizeSimpleData);
