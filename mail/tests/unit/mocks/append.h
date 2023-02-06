#pragma once

#include <src/backend/append/append.h>
#include <src/backend/backend.h>

using namespace yimap;
using namespace yimap::backend;

struct TestAppendBackend : AppendBackend
{
    Future<AppendResult> append(AppendRequest&& request) override
    {
        appendRequests.emplace_back(std::move(request), Promise<AppendResult>());
        return appendRequests.back().second;
    }

    auto& getAppendRequests()
    {
        return appendRequests;
    }

    std::vector<std::pair<AppendRequest, Promise<AppendResult>>> appendRequests;
};
