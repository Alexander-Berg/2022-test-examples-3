#pragma once

#include <src/backend/mbody/body_structure_data.h>
#include <src/backend/backend.h>

using namespace yimap;
using namespace yimap::backend;

struct TestMbodyBackend : MbodyBackend
{
    struct MessageRequest
    {
        std::string stid;
        std::string part;
        macs::MimeParts mimeParts;
        Handler handler;
    };

    struct BodyStructureRequest
    {
        std::string stid;
        macs::MimeParts mimeParts;
        Promise<BodyStructurePtr> promise;
    };

    std::deque<MessageRequest> loadRequests;
    std::deque<MessageRequest> loadBodyRequests;
    std::deque<MessageRequest> loadHeaderRequests;
    std::deque<BodyStructureRequest> loadBodyStructureRequests;

    void loadMessage(
        const std::string& stid,
        const std::string& part,
        const macs::MimeParts& mimeParts,
        const Handler& handler) override
    {
        loadRequests.push_back({ stid, part, mimeParts, handler });
    }

    void loadHeader(
        const std::string& stid,
        const std::string& part,
        const macs::MimeParts& mimeParts,
        const Handler& handler) override
    {
        loadHeaderRequests.push_back({ stid, part, mimeParts, handler });
    }

    void loadBody(
        const std::string& stid,
        const std::string& part,
        const macs::MimeParts& mimeParts,
        const Handler& handler) override
    {
        loadBodyRequests.push_back({ stid, part, mimeParts, handler });
    }

    Future<std::shared_ptr<BodyStructure>> loadBodyStructure(
        const string& stid,
        const macs::MimeParts& mimeParts,
        const std::vector<string>& /*rawHeadersList*/) override
    {
        loadBodyStructureRequests.push_back({ stid, mimeParts, {} });
        return loadBodyStructureRequests.back().promise;
    }
};
