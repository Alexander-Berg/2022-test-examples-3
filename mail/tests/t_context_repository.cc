#include <ymod_webserver/server.h>
#include <yplatform/app_service.h>
#include <catch.hpp>
#include <thread>

const int PORT = 8080;

using namespace boost::asio;

TEST_CASE("context_repository", "should add requests to context repository")
{
    io_service io;
    ymod_webserver::settings settings;
    settings.endpoints.emplace("", ymod_webserver::endpoint("", "::", PORT));
    ymod_webserver::server server(io, settings);
    std::atomic_bool called = false;
    server.bind("", { "/" }, [&io, &called](ymod_webserver::http::stream_ptr) {
        called = true;
        REQUIRE(yplatform::get_context_repository(io).get_contexts()->size() == 1);
    });
    server.start();
    std::thread t([&io] { io.run(); });
    ip::tcp::socket socket(io);
    socket.connect(ip::tcp::endpoint(ip::address::from_string("127.0.0.1"), PORT));
    write(socket, buffer("GET / HTTP/1.1\n\n"));
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
    REQUIRE(yplatform::get_context_repository(io).get_contexts()->size() == 0);
}
