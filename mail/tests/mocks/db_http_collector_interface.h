#pragma once

#include <common/context.h>
#include <common/data_source.h>
#include <memory>

namespace yrpopper::mock {

struct HttpCollectorInterface
{
    future_http_folders loadHttpFolders(PlatformContextPtr /*ctx*/, popid_t /*popid*/)
    {
        promise_http_folders prom;
        prom.set(folders);
        return prom;
    }

    future_uint64_t updateOrCreateHttpFolder(
        PlatformContextPtr /*ctx*/,
        popid_t /*popid*/,
        http_folder_ptr folder)
    {
        lastUpdatedOrCreatedFolder = folder;

        promise_uint64_t prom;
        prom.set(0);
        return prom;
    }

    void setFolders(http_folders_ptr folders)
    {
        this->folders = folders;
    }

    http_folders_ptr folders = boost::make_shared<http_folders>();
    http_folder_ptr lastUpdatedOrCreatedFolder = nullptr;
};

struct InterfaceProvider
{
    static std::shared_ptr<HttpCollectorInterface> getHttpCollectorInterface()
    {
        return std::make_shared<HttpCollectorInterface>();
    }
};

}
