#pragma once

#include <include/context.hpp>
#include <include/common.hpp>
#include <include/expected.hpp>

namespace apq_tester::server::handlers {

struct Ping
{
    expected<std::string> operator()(request_ptr request, response_ptr response, ContextPtr) const
    {
        if (request->method != mth_get)
        {
            response->result(method_not_allowed);
            return {};
        }
        return std::string(R"({"result":"pong"})");
    }
};

}
