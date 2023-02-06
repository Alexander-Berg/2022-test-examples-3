#include <catch.hpp>
#include "net_server.h"

using namespace yplatform::time_traits;
using namespace boost::asio;
using namespace ymod_webserver;

class handler_mock : public handler
{
public:
    handler_mock(std::vector<websocket::output_stream_ptr>& stream_ptrs) : stream_ptrs_(stream_ptrs)
    {
    }

    void execute(request_ptr, response_ptr) override
    {
    }

    void upgrade_to_websocket(request_ptr) override
    {
    }

    void execute_websocket(websocket::output_stream_ptr stream_ptr) override
    {
        stream_ptrs_.push_back(stream_ptr);
    }

private:
    std::vector<websocket::output_stream_ptr>& stream_ptrs_;
};

struct t_yplatform_net_server
{
    t_yplatform_net_server() : io_data(io), endpoint("endpoint", "0.0.0.0", 12345)
    {
        set_websocket_streams_max(std::numeric_limits<size_t>::max());
    }

    void set_websocket_streams_max(size_t n)
    {
        settings_ptr = std::make_shared<settings>();
        settings_ptr->ws_max_streams = n;

        stats_ptr = std::make_shared<module_stats>();

        server_ptr = boost::make_shared<yplatform_net_server>(io_data, settings_ptr, stats_ptr);
        server_ptr->logger(YGLOBAL_LOGGER);
    }

    void create_websocket()
    {
        auto context_ptr = boost::make_shared<context>();
        auto request_ptr = boost::make_shared<request>(context_ptr);
        auto read_buffer = boost::make_shared<yplatform::zerocopy::streambuf>();
        auto socket = yplatform::net::tcp_socket(io);
        net_session_ptr session_ptr = boost::make_shared<session>(
            std::move(socket),
            *settings_ptr,
            endpoint,
            boost::make_shared<handler_mock>(stream_ptrs));
        auto starter_ptr = boost::make_shared<starter>(io, server_ptr, session_ptr, *settings_ptr);

        server_ptr->execute_websocket(starter_ptr.get(), request_ptr, read_buffer);
        io.run();
        io.reset();
    }

    boost::asio::io_service io;
    yplatform::net::io_data io_data;
    endpoint endpoint;
    std::shared_ptr<settings> settings_ptr;
    std::shared_ptr<module_stats> stats_ptr;
    boost::shared_ptr<yplatform_net_server> server_ptr;
    std::vector<websocket::output_stream_ptr> stream_ptrs;
};

TEST_CASE_METHOD(t_yplatform_net_server, "yplatform_net_server/websocket/limit/not_exceeded")
{
    set_websocket_streams_max(5);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 1);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 2);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 3);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 4);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 5);
}

TEST_CASE_METHOD(t_yplatform_net_server, "yplatform_net_server/websocket/limit/exceeded")
{
    set_websocket_streams_max(3);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 1);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 2);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 3);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 3);

    create_websocket();
    REQUIRE(stats_ptr->websocket_streams_count == 3);
}
