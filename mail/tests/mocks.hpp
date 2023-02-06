#pragma once

#include <ymod_webserver/server.h>
#include <gmock/gmock.h>

namespace ymod_webserver {
namespace helpers {
namespace tests {

using namespace testing;

struct MockedResponse : public ymod_webserver::response {
    MOCK_METHOD(void, send_client_stream, (const yplatform::net::buffers::const_chunk_buffer&), (override));
    MOCK_METHOD(void, send_client_stream2, (const yplatform::net::buffers::const_chunk_buffer&, bool), (override));
    MOCK_METHOD(yplatform::net::streamer_wrapper, client_stream, (), (override));
    MOCK_METHOD(bool, is_open, (), (const, override));

    MOCK_METHOD(yplatform::time_traits::timer_ptr, make_timer, (), (const, override));

    MOCK_METHOD(void, result, (ymod_webserver::codes::code, const std::string&), ());
    MOCK_METHOD(void, result, (ymod_webserver::codes::code, const std::string&, const std::string&), ());

    MOCK_METHOD(void, begin_poll_connect, (), (override));
    MOCK_METHOD(void, cancel_poll_connect, (), (override));

    MOCK_METHOD(void, set_code, (ymod_webserver::codes::code, const std::string&), (override));
    MOCK_METHOD(void, add_header, (const std::string&, const std::string&), (override));
    MOCK_METHOD(void, add_header, (const std::string&, std::time_t), (override));
    MOCK_METHOD(void, set_content_type, (const std::string&), (override));
    MOCK_METHOD(void, set_content_type, (const std::string&, const std::string&), (override));
    MOCK_METHOD(void, set_cache_control, (ymod_webserver::cache_response_header, const std::string&), (override));
    MOCK_METHOD(void, set_connection, (bool), (override));

    MOCK_METHOD(void, result_body, (const std::string&), (override));
    MOCK_METHOD(yplatform::net::streamable_ptr, result_stream, (const std::size_t), (override));
    MOCK_METHOD(yplatform::net::streamable_ptr, result_chunked, (), (override));

    MOCK_METHOD(void, add_error_handler, (const error_handler& handler), (override));
    MOCK_METHOD(void, on_error, (const boost::system::error_code& e), (override));
    MOCK_METHOD(ymod_webserver::request_ptr, request, (), (const, override));
    MOCK_METHOD(ymod_webserver::context_ptr, ctx, (), (const, override));
    MOCK_METHOD(ymod_webserver::codes::code, result_code, (), (const, override));

    MOCK_METHOD(bool, is_secure, (), (const, override));
    MOCK_METHOD(boost::asio::io_service&, get_io_service, (), (override));
    MOCK_METHOD(const boost::asio::ip::address&, remote_addr, (), (const, override));
};

} // namespace tests
} // namespace helpers
} // namespace ymod_webserver
