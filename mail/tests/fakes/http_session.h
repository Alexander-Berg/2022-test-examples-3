#pragma once

#include "request.h"
#include <ymod_httpclient/errors.h>
#include <ymod_httpclient/types.h>
#include <yplatform/net/io_data.h>

namespace ymod_httpclient::fakes {

struct http_session
{
    using handler_type = std::function<void(http_error::code, const string&)>;

    http_session()
    {
    }

    http_session(
        yplatform::net::io_data& io_data,
        const remote_point_info&,
        const yplatform::log::source&,
        const ymod_httpclient::settings&)
        : io(io_data.get_io())
    {
    }

    template <typename Handler>
    void connect(yplatform::task_context_ptr, const time_traits::duration&, Handler&& handler)
    {
        connected = (connect_result == http_error::success);
        handler(connect_result, "");
    }

    void run(request_data_ptr /*req*/, const handler_type& handler)
    {
        if (io)
        {
            io->post([handler] { handler({}, {}); });
        }
        else
        {
            handler({}, {});
        }
    }

    void idle(const handler_type& handler)
    {
        idle_handler = handler;
        idling = true;
    }

    void stop_idle()
    {
        idling = false;
        if (idle_handler)
        {
            idle_handler({}, "");
            idle_handler = {};
        }
    }

    bool is_reusable()
    {
        return reusable && !closed;
    }

    void close()
    {
        if (closed) return;
        closed = true;
        if (idle_handler)
        {
            idle_handler(http_error::eof_error, "");
            idle_handler = {};
        }
    }

    void shutdown()
    {
    }

    std::uint64_t get_next_request_number()
    {
        return 0;
    }

    void const* id() const
    {
        return nullptr;
    }

    void close_from_remote()
    {
        closed = false;
        if (idling)
        {
            idling = false;
            if (idle_handler)
            {
                idle_handler(http_error::session_closed_error, "");
                idle_handler = {};
            }
        }
    }

    const auto& stats()
    {
        return session_stats;
    }

    struct
    {
        std::size_t requests_processed = 0;
        time_traits::duration resolve_time = {};
        time_traits::duration connect_time = {};
        time_traits::duration tls_time = {};
    } session_stats;

    boost::asio::io_service* io = nullptr;
    http_error::code connect_result = http_error::success;
    bool connected = false;
    bool closed = false;
    bool reusable = false;
    bool idling = false;
    handler_type idle_handler;
};

using http_session_ptr = shared_ptr<http_session>;

auto create_reusable_session = [](boost::asio::io_service&) {
    auto ret = make_shared<http_session>();
    ret->reusable = true;
    return ret;
};

auto create_not_reusable_session = [](boost::asio::io_service&) {
    auto ret = make_shared<http_session>();
    ret->reusable = false;
    return ret;
};

auto create_broken_session = [](boost::asio::io_service&) {
    auto ret = make_shared<http_session>();
    ret->connect_result = http_error::connection_timeout;
    return ret;
};

}
