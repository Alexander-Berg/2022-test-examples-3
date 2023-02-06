#pragma once

#include "call.h"
#include <ymod_httpclient/detail/balancing_call_op.h>

namespace ymod_httpclient::fakes {

using namespace ymod_httpclient::detail;

namespace ph = std::placeholders;

struct balancing_call_op : call
{
    template <typename Handler>
    void operator()(
        yplatform::task_context_ptr ctx,
        request req,
        options opt,
        continuation_ptr,
        Handler handler)
    {
        req.url = host + req.url;
        call::async_run(
            ctx,
            std::move(req),
            std::move(opt),
            std::bind(handler, ph::_1, ph::_2, make_shared<continuation_token>()));
    }

    std::string host;
};

}
