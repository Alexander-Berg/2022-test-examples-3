#pragma once

#include <ymod_httpclient/call.h>
#include <vector>

namespace ymod_httpclient::fakes {

struct call
{
    void init(yplatform::reactor_ptr, const settings&)
    {
    }

    template <typename Handler>
    void async_run(yplatform::task_context_ptr, request req, options opt, Handler handler)
    {
        journal.emplace_back(req.url);
        opts.emplace_back(opt);
        boost::system::error_code err;
        response resp;
        if (req.url.find("timeout") != string::npos)
        {
            err = http_error::request_timeout;
        }
        else if (req.url.find("unsupported_scheme") != string::npos)
        {
            err = http_error::unsupported_scheme;
        }
        else if (req.url.find("connect_error") != string::npos)
        {
            err = http_error::connect_error;
        }
        auto pos = req.url.find("status");
        if (pos != string::npos)
        {
            resp.status = std::stoi(req.url.substr(pos + 6, 3));
        }
        handler(err, resp);
    }

    std::vector<string> journal;
    std::vector<options> opts;
};

}
