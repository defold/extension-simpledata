#include <memory.h>

#include <dmsdk/dlib/log.h>
#include <dmsdk/resource/resource.hpp>

#include "simpledata_ddf.h" // generated from the simpledata_ddf.proto
#include "res_simpledata.h"

namespace dmSimpleData
{
    static dmResource::Result ResourceCreate(const dmResource::ResourceCreateParams* params)
    {
        dmGameSystemDDF::SimpleDataDesc* desc;
        dmDDF::Result e  = dmDDF::LoadMessage(params->m_Buffer, params->m_BufferSize, &desc);
        if ( e != dmDDF::RESULT_OK )
        {
            return dmResource::RESULT_FORMAT_ERROR;
        }
        // We hand out a pointer to a pointer, in order to be able to recreate the assets
        // when hot reloading
        SimpleDataResource* resource = new SimpleDataResource;
        resource->m_DDF = desc;
        dmResource::SetResource(params->m_Resource, resource);
        return dmResource::RESULT_OK;
    }

    static dmResource::Result ResourceDestroy(const dmResource::ResourceDestroyParams* params)
    {
        SimpleDataResource* resource = (SimpleDataResource*) dmResource::GetResource(params->m_Resource);
        dmDDF::FreeMessage(resource->m_DDF);
        delete resource;
        return dmResource::RESULT_OK;
    }

    static dmResource::Result ResourceRecreate(const dmResource::ResourceRecreateParams* params)
    {
        dmGameSystemDDF::SimpleDataDesc* desc;
        dmDDF::Result e  = dmDDF::LoadMessage(params->m_Buffer, params->m_BufferSize, &desc);
        if ( e != dmDDF::RESULT_OK )
        {
            return dmResource::RESULT_FORMAT_ERROR;
        }
        SimpleDataResource* resource = (SimpleDataResource*) dmResource::GetResource(params->m_Resource);
        dmDDF::FreeMessage(resource->m_DDF);
        resource->m_DDF = desc;
        return dmResource::RESULT_OK;
    }

    static ResourceResult ResourceType_Register(HResourceTypeContext ctx, HResourceType type)
    {
        return (ResourceResult)dmResource::SetupType(ctx,
                                                     type,
                                                     0, // context
                                                     0, // preload
                                                     ResourceCreate,
                                                     0, // post create
                                                     ResourceDestroy,
                                                     ResourceRecreate);
    }
}

DM_DECLARE_RESOURCE_TYPE(ResourceTypeSimpleDataExt, "simpledatac", dmSimpleData::ResourceType_Register, 0);
