#include "rate_limit.h"
#include <catch.hpp>
#include <thread>

namespace ymod_webserver {

using params_map = std::map<string, string>;
using headers_map = std::map<string, string>;
using rate_limit_attr_type = enum rate_limit_settings::request_attr::type;

struct fake_url
{
    params_map params;
};

struct fake_context
{
    std::map<string, string> custom_log_data;
};

struct fake_request
{
    headers_map headers;
    fake_url url;
    std::shared_ptr<fake_context> context = std::make_shared<fake_context>();
};

struct fake_stream
{
    fake_stream(boost::asio::io_service& io) : io(io)
    {
    }

    void set_code(codes::code code)
    {
        status = code;
    }

    void result_body(const std::string& body)
    {
        this->body = body;
    }

    fake_request* request()
    {
        return &req;
    }

    boost::asio::io_service& get_io_service()
    {
        return io;
    }

    boost::asio::io_service& io;
    fake_request req;
    codes::code status = static_cast<codes::code>(0);
    std::string body;
};

fake_stream run_request(
    const std::vector<std::shared_ptr<rate_limiter>>& limiters,
    const params_map& params = {},
    const headers_map& headers = {})
{
    auto handler = rate_limit_wrapper(
        limiters, [](fake_stream* stream) mutable { stream->set_code(codes::ok); });
    boost::asio::io_service io;
    fake_stream stream(io);
    stream.req.url.params = params;
    stream.req.headers = headers;
    io.post(std::bind(handler, &stream));
    io.run();
    return stream;
}

TEST_CASE("rate_limit/limit")
{
    rate_limit_settings settings;
    settings.limit = 5;
    auto limiter = std::make_shared<rate_limiter>(settings);
    for (int i = 0; i < 10; ++i)
    {
        REQUIRE(
            run_request({ limiter }).status == (i < 5 ? codes::ok : codes::service_unavailable));
    }
}

TEST_CASE("rate_limit/recovery")
{
    rate_limit_settings settings;
    settings.limit = 2;
    settings.recovery_rate = 1;
    settings.recovery_interval_ms = 50;
    auto limiter = std::make_shared<rate_limiter>(settings);

    REQUIRE(run_request({ limiter }).status == codes::ok);
    REQUIRE(run_request({ limiter }).status == codes::ok);
    REQUIRE(run_request({ limiter }).status == codes::service_unavailable);

    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(run_request({ limiter }).status == codes::ok);
    REQUIRE(run_request({ limiter }).status == codes::service_unavailable);

    std::this_thread::sleep_for(time_traits::milliseconds(100));
    REQUIRE(run_request({ limiter }).status == codes::ok);
    REQUIRE(run_request({ limiter }).status == codes::ok);
    REQUIRE(run_request({ limiter }).status == codes::service_unavailable);
}

TEST_CASE("rate_limit/zero_limit")
{
    rate_limit_settings settings;
    settings.limit = 0;
    auto limiter = std::make_shared<rate_limiter>(settings);
    REQUIRE(run_request({ limiter }).status == codes::service_unavailable);
}

TEST_CASE("rate_limit/status")
{
    rate_limit_settings settings;
    settings.response_status = codes::code::too_many_requests;
    auto limiter = std::make_shared<rate_limiter>(settings);
    REQUIRE(run_request({ limiter }).status == codes::too_many_requests);
}

TEST_CASE("rate_limit/response_body")
{
    rate_limit_settings settings;
    settings.response_body = "Test body";
    auto limiter = std::make_shared<rate_limiter>(settings);
    REQUIRE(run_request({ limiter }).body == settings.response_body);
}

TEST_CASE("rate_limit/log_exceeded")
{
    rate_limit_settings settings;
    settings.name = "test_limit";
    auto limiter = std::make_shared<rate_limiter>(settings);
    auto stream = run_request({ limiter });
    REQUIRE(stream.status == codes::service_unavailable);
    REQUIRE(stream.req.context->custom_log_data["rate_limit"] == settings.name);
}

TEST_CASE("rate_limit/param")
{
    rate_limit_settings settings;
    settings.limit = 1;
    settings.limiting_attr = { rate_limit_attr_type::url_param, "uid" };
    auto limiter = std::make_shared<rate_limiter>(settings);
    for (int i = 0; i < 2; ++i)
    {
        REQUIRE(
            run_request({ limiter }, { { "uid", "1" } }).status ==
            (i < 1 ? codes::ok : codes::service_unavailable));
    }
    for (int i = 0; i < 2; ++i)
    {
        REQUIRE(
            run_request({ limiter }, { { "uid", "2" } }).status ==
            (i < 1 ? codes::ok : codes::service_unavailable));
    }
}

TEST_CASE("rate_limit/header")
{
    rate_limit_settings settings;
    settings.limit = 1;
    settings.limiting_attr = { rate_limit_attr_type::header, "DNT" };
    auto limiter = std::make_shared<rate_limiter>(settings);
    for (int i = 0; i < 2; ++i)
    {
        REQUIRE(
            run_request({ limiter }, {}, { { "DNT", "0" } }).status ==
            (i < 1 ? codes::ok : codes::service_unavailable));
    }
    for (int i = 0; i < 2; ++i)
    {
        REQUIRE(
            run_request({ limiter }, {}, { { "DNT", "1" } }).status ==
            (i < 1 ? codes::ok : codes::service_unavailable));
    }
}

TEST_CASE("rate_limit/filter_by_param")
{
    rate_limit_settings settings;
    settings.limit = 0;
    settings.limiting_attr = { rate_limit_attr_type::url_param, "uid" };
    settings.filters.push_back(
        { { rate_limit_attr_type::url_param, "service" }, boost::regex("mail|disk") });
    auto limiter = std::make_shared<rate_limiter>(settings);
    REQUIRE(
        run_request({ limiter }, { { "service", "mail" } }).status == codes::service_unavailable);
    REQUIRE(run_request({ limiter }, { { "service", "taxi" } }).status == codes::ok);
    REQUIRE(
        run_request({ limiter }, { { "service", "disk" } }).status == codes::service_unavailable);
    REQUIRE(run_request({ limiter }, { { "service", "" } }).status == codes::ok);
    REQUIRE(run_request({ limiter }).status == codes::ok);
}

TEST_CASE("rate_limit/filter_by_header")
{
    rate_limit_settings settings;
    settings.limit = 0;
    settings.filters.push_back({ { rate_limit_attr_type::header, "DNT" }, boost::regex("1") });
    auto limiter = std::make_shared<rate_limiter>(settings);

    REQUIRE(run_request({ limiter }, {}, { { "DNT", "1" } }).status == codes::service_unavailable);
    REQUIRE(run_request({ limiter }, {}, { { "DNT", "0" } }).status == codes::ok);
    REQUIRE(run_request({ limiter }).status == codes::ok);
}

TEST_CASE("rate_limit/filter_by_header_and_param")
{
    rate_limit_settings settings;
    settings.limit = 0;
    settings.filters.push_back(
        { { rate_limit_attr_type::url_param, "service" }, boost::regex("mail|disk") });
    settings.filters.push_back({ { rate_limit_attr_type::header, "DNT" }, boost::regex("1") });
    auto limiter = std::make_shared<rate_limiter>(settings);

    REQUIRE(run_request({ limiter }).status == codes::ok);
    REQUIRE(run_request({ limiter }, {}, { { "DNT", "1" } }).status == codes::ok);
    REQUIRE(run_request({ limiter }, { { "service", "disk" } }).status == codes::ok);
    REQUIRE(
        run_request({ limiter }, { { "service", "disk" } }, { { "DNT", "1" } }).status ==
        codes::service_unavailable);
}

TEST_CASE("rate_limit/filter_with_empty_value")
{
    rate_limit_settings settings;
    settings.limit = 0;
    settings.limiting_attr = { rate_limit_attr_type::url_param, "uid" };
    settings.filters.push_back(
        { { rate_limit_attr_type::url_param, "service" }, boost::regex("") });
    auto limiter = std::make_shared<rate_limiter>(settings);
    REQUIRE(run_request({ limiter }).status == codes::service_unavailable);
    REQUIRE(run_request({ limiter }, { { "service", "" } }).status == codes::service_unavailable);
    REQUIRE(run_request({ limiter }, { { "service", "mail" } }).status == codes::ok);
}

TEST_CASE("rate_limit/matching_by_path")
{
    rate_limit_settings settings;
    settings.path.emplace_back("/ping");
    settings.path.emplace_back("/v1/.*");
    settings.path.emplace_back("/v2/.*/ping");
    settings.path.emplace_back("/v3/(foo|bar)");
    settings.path.emplace_back("/v4/\\w+");
    rate_limiter limiter(settings);
    REQUIRE(limiter.match_by_path("/ping"));
    REQUIRE(limiter.match_by_path("/v1/"));
    REQUIRE(limiter.match_by_path("/v1/store"));
    REQUIRE(limiter.match_by_path("/v2/service/ping"));
    REQUIRE(limiter.match_by_path("/v2//ping"));
    REQUIRE(limiter.match_by_path("/v3/foo"));
    REQUIRE(limiter.match_by_path("/v3/bar"));
    REQUIRE(limiter.match_by_path("/v4/foo"));
    REQUIRE(!limiter.match_by_path("/ping_pong"));
    REQUIRE(!limiter.match_by_path("/v2/ping"));
    REQUIRE(!limiter.match_by_path("/v2/service/pong"));
    REQUIRE(!limiter.match_by_path("/v3/baz"));
    REQUIRE(!limiter.match_by_path("/v4/foo/bar"));
    REQUIRE(!limiter.match_by_path("/v5/ping"));
    REQUIRE(!limiter.match_by_path("/v5/store"));
    REQUIRE(!limiter.match_by_path("/api/v1/store"));
}

TEST_CASE("rate_limit/combination")
{
    std::vector<std::shared_ptr<rate_limiter>> limiters;
    rate_limit_settings settings;
    settings.limit = 2;
    settings.recovery_rate = 1;
    settings.recovery_interval_ms = 50;
    limiters.emplace_back(std::make_shared<rate_limiter>(settings));

    settings.limit = 1;
    settings.recovery_interval_ms = 100;
    settings.limiting_attr = { rate_limit_attr_type::url_param, "uid" };
    settings.response_status = codes::too_many_requests;
    limiters.emplace_back(std::make_shared<rate_limiter>(settings));

    REQUIRE(run_request(limiters, { { "uid", "1" } }).status == codes::ok);
    REQUIRE(run_request(limiters, { { "uid", "1" } }).status == codes::too_many_requests);
    REQUIRE(run_request(limiters, { { "uid", "2" } }).status == codes::ok);
    auto status = run_request(limiters, { { "uid", "2" } }).status;
    REQUIRE((status == codes::too_many_requests || status == codes::service_unavailable));
    REQUIRE(run_request(limiters, { { "uid", "3" } }).status == codes::service_unavailable);

    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(run_request(limiters, { { "uid", "1" } }).status == codes::too_many_requests);
    REQUIRE(run_request(limiters, { { "uid", "2" } }).status == codes::too_many_requests);
    REQUIRE(run_request(limiters, { { "uid", "3" } }).status == codes::ok);
}

TEST_CASE("rate_limit/recovery_with_param_and_header")
{
    std::vector<std::shared_ptr<rate_limiter>> limiters;
    rate_limit_settings settings;
    settings.limit = 2;
    settings.recovery_rate = 1;
    settings.recovery_interval_ms = 50;
    settings.limiting_attr = { rate_limit_attr_type::header, "DNT" };
    limiters.emplace_back(std::make_shared<rate_limiter>(settings));

    settings.limit = 1;
    settings.recovery_interval_ms = 100;
    settings.limiting_attr = { rate_limit_attr_type::url_param, "uid" };
    settings.response_status = codes::too_many_requests;
    limiters.emplace_back(std::make_shared<rate_limiter>(settings));

    REQUIRE(run_request(limiters, { { "uid", "1" } }, { { "DNT", "0" } }).status == codes::ok);
    REQUIRE(run_request(limiters, { { "uid", "2" } }, { { "DNT", "0" } }).status == codes::ok);
    REQUIRE(
        run_request(limiters, { { "uid", "3" } }, { { "DNT", "0" } }).status ==
        codes::service_unavailable);

    std::this_thread::sleep_for(time_traits::milliseconds(50));
    REQUIRE(run_request(limiters, { { "uid", "3" } }, { { "DNT", "0" } }).status == codes::ok);
    REQUIRE(
        run_request(limiters, { { "uid", "3" } }, { { "DNT", "1" } }).status ==
        codes::too_many_requests);

    std::this_thread::sleep_for(time_traits::milliseconds(100));
    REQUIRE(run_request(limiters, { { "uid", "3" } }, { { "DNT", "1" } }).status == codes::ok);
    REQUIRE(run_request(limiters, { { "uid", "2" } }, { { "DNT", "0" } }).status == codes::ok);
}

}
