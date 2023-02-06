#pragma once

#include <yxiva/core/types.h>
#include <ymod_webserver/codes.h>
#include <ymod_webserver/request.h>

namespace yxiva {

struct stream_mock
{
    stream_mock()
        : ctx_ptr(boost::make_shared<ymod_webserver::context>())
        , req_ptr(boost::make_shared<ymod_webserver::request>(ctx_ptr))
    {
    }

    ymod_webserver::context_ptr ctx() const
    {
        return ctx_ptr;
    }

    ymod_webserver::request_ptr request() const
    {
        return req_ptr;
    }

    void set_code(ymod_webserver::codes::code cd, const string& /*reason*/ = "")
    {
        this->code = cd;
    }

    void result_body(const string& body)
    {
        this->body = body;
    }

    void add_header(const string& /*name*/, const string& /*value*/)
    {
    }

    void result(ymod_webserver::codes::code code, const string& body = "")
    {
        set_code(code);
        result_body(body);
    }

    ymod_webserver::context_ptr ctx_ptr;
    ymod_webserver::request_ptr req_ptr;
    std::optional<ymod_webserver::codes::code> code;
    std::optional<string> body;
};

}