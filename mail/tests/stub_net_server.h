#include <net_server.h>
#include <http_stream.h>
#include <websocket_stream.h>
#include <ymod_webserver/context.h>
#include <ymod_webserver/settings.h>

#include <catch.hpp>
#include <boost/asio.hpp>

namespace ymod_webserver {

struct stub_server
    : public net_server
    , public boost::enable_shared_from_this<stub_server>
{
    boost::asio::io_service io_;
    bool finished_ = false;
    bool starter_finished_ = false;
    bool starter_failed_ = false;
    bool execute_http_ = false;
    bool execute_websocket_ = false;
    bool starter_destroyed_ = false;
    bool websocket_stream_destroyed_ = false;
    bool http_stream_destroyed_ = false;
    bool session_destroyed_ = false;
    bool request_destroyed_ = false;
    handler_ptr handler_;

    stub_server() = default;

    void on_destroy(starter* starter, const process_result& result)
    {
        REQUIRE_FALSE(finished_);
        starter_destroyed_ = true;
        finished_ = true;
        switch (result.state)
        {
        case process_result::continue_http:
            REQUIRE(result.request);
            REQUIRE(result.read_buffer);
            if (handler_)
            {
                REQUIRE(starter);
                auto stream = boost::make_shared<http_stream>(
                    io_,
                    shared_from_this(),
                    result.request->context,
                    starter->get_session(),
                    result.request,
                    result.read_buffer);
                stream->init();
                handler_->execute(result.request, stream);
            }
            execute_http_ = true;
            break;
        case process_result::continue_websocket:
            REQUIRE(result.request);
            REQUIRE(result.read_buffer);
            execute_websocket_ = true;
            break;
        case process_result::failed:
            starter_failed_ = true;
            break;
        case process_result::finished:
            starter_finished_ = true;
            break;
        case process_result::inited:
            break;
        case process_result::processing:
            break;
        }
    }
    void on_destroy(websocket::websocket_stream*)
    {
        websocket_stream_destroyed_ = true;
    }
    void on_destroy(http_stream*)
    {
        http_stream_destroyed_ = true;
    }
    void on_destroy(net_session*)
    {
        session_destroyed_ = true;
    }
    void on_destroy(request*)
    {
        request_destroyed_ = true;
    }
};

}
