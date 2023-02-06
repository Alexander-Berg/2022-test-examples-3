#pragma once

#include <common/types.h>
#include <yplatform/json.h>
#include <yplatform/util/split.h>

namespace fan {

struct test_send_task
{
    string id;
    string account_slug;
    string campaign_slug;
    string from_email;
    vector<string> recipients;

    static test_send_task from_json(const yplatform::json_value& json)
    {
        test_send_task ret;
        ret.id = std::to_string(json["id"].to_int64());
        ret.account_slug = json["account_slug"].to_string();
        ret.campaign_slug = json["campaign_slug"].to_string();
        ret.from_email = json["from_email"].to_string();
        for (auto it = json["recipients"].array_begin(); it != json["recipients"].array_end(); ++it)
        {
            ret.recipients.emplace_back((*it).to_string());
        }
        return ret;
    }
};

}
