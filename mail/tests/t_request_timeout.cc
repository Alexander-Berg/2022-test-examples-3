#include <ymod_webserver/server.h>
#include <catch.hpp>
#include <thread>
#include <boost/algorithm/string/trim.hpp>

const int PORT = 8080;

using namespace yplatform::time_traits;
using namespace boost::asio;

TEST_CASE("request_timeout", "should fill context deadline from x-request-timeout header")
{
    io_service io;
    ymod_webserver::settings settings;
    settings.endpoints.emplace("", ymod_webserver::endpoint("", "::", PORT));
    ymod_webserver::server server(io, settings);
    std::atomic_bool called = false;
    server.bind("", { "/" }, [&called](ymod_webserver::http::stream_ptr stream) {
        called = true;
        REQUIRE(stream->ctx()->deadline() != time_point::max());
        auto timeout =
            duration_cast<milliseconds>(stream->ctx()->deadline() - clock::now()).count();
        REQUIRE(timeout >= 4000);
        REQUIRE(timeout <= 5000);
    });
    server.start();
    std::thread t([&io] { io.run(); });
    ip::tcp::socket socket(io);
    socket.connect(ip::tcp::endpoint(ip::address::from_string("127.0.0.1"), PORT));
    write(socket, buffer("GET / HTTP/1.1\nX-Request-Timeout: 5000\n\n"));
    auto write_ts = std::chrono::steady_clock::now();
    while (!called && std::chrono::steady_clock::now() - write_ts < std::chrono::seconds(10))
    {
    }
    io.post([&io, &server] {
        server.stop();
        io.stop();
    });
    t.join();
    REQUIRE(called);
}

TEST_CASE("invalid request_timeout", "should reject timeouts less than min_acceptable_timeout")
{
    io_service io;
    ymod_webserver::settings settings;
    settings.min_acceptable_timeout = seconds(1);
    settings.endpoints.emplace("", ymod_webserver::endpoint("", "::", PORT));
    ymod_webserver::server server(io, settings);
    std::atomic_bool called = false;
    server.bind(
        "", { "/" }, [&called](ymod_webserver::http::stream_ptr /*stream*/) { called = true; });
    server.start();
    std::thread t([&io] { io.run(); });
    ip::tcp::socket socket(io);
    socket.connect(ip::tcp::endpoint(ip::address::from_string("127.0.0.1"), PORT));
    boost::asio::streambuf read_buf;
    write(socket, buffer("GET / HTTP/1.1\nX-Request-Timeout: 50\n\n"));
    read_until(socket, read_buf, '\n');
    io.post([&io, &server] {
        server.stop();
        io.stop();
    });
    t.join();
    std::istream is(&read_buf);
    std::string response;
    std::getline(is, response);
    REQUIRE(!called);
    REQUIRE(boost::trim_copy(response) == "HTTP/1.1 400 BadRequest");
}
