#pragma once

#include <common/types.h>
#include <ymod_httpclient/call.h>

namespace fan {

struct fake_http_client
{
    template <typename Handler>
    void async_run(task_context_ptr /*ctx*/, yhttp::request /*req*/, Handler handler)
    {
        handler(error_code(), response);
    }

    yhttp::response response;
};

}
