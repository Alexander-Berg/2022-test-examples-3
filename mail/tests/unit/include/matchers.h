#pragma once

#include <gmock/gmock.h>

#include <mail/spaniel/service/include/task_params.h>
#include <butil/http/arguments.h>


namespace spaniel::tests {

MATCHER_P(UpdateTaskArgsAre, args, "task args must contain uids") {
    *result_listener << "actual: " << *(arg.t);
    return args == parseOrganizationUpdateParams(ymod_queuedb::Task { .taskArgs = *(arg.t) } ).second;
}

MATCHER_P(WithUidInUrl, uid, "url must contain uid") {
    *result_listener << "actual: " << arg.request.url;
    HttpArguments dict;
    dict.fromUrl(arg.request.url);
    return dict.arguments["uid"].front() == std::to_string(uid.t);
}

MATCHER_P(WithOrgId, orgId, "url must contain org_id") {
    *result_listener << "actual: " << arg.request.url;
    return arg.request.url.find(std::to_string(orgId)) != std::string::npos;
}

MATCHER_P2(WithQueryParam, key, value, "url must contain param with key and value") {
    *result_listener << "actual: " << arg.request.url;
    HttpArguments dict;
    dict.fromUrl(arg.request.url);
    return dict.arguments[key].front() == value;
}

MATCHER_P(WithHost, host, "url must contain host") {
    *result_listener << "actual: " << arg.request.url;
    return boost::starts_with(arg.request.url, host);
}

MATCHER_P(WithMids, mids, "url should contains all mids") {
    *result_listener << "url: " << arg.request.url;
    for (const auto& mid: mids) {
        if (arg.request.url.find(std::to_string(mid)) == std::string::npos) {
            return false;
        }
    }
    return true;
}

}
