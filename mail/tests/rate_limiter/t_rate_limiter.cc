#include <src/web/rate_limiter.h>

#include <yplatform/ptree.h>
#include <yplatform/log.h>

#include <boost/asio.hpp>
#include <catch.hpp>

#include <chrono>
#include <memory>
#include <thread>

namespace ph = std::placeholders;

struct rate_limiter_test
{
    using rate_limiter_ptr = std::shared_ptr<xeno::web::rate_limiter>;
    using error = xeno::error;
    using acquire_result = xeno::web::rate_limiter::acquire_result;

    rate_limiter_test()
        : rate_limiter_(std::make_shared<xeno::web::rate_limiter>(
              &io_,
              make_limiter_cfg(),
              yplatform::log::source()))
    {
        YLOG_GLOBAL(info) << "rate_limiter_test() -----------------------------";
    }

    yplatform::ptree make_arg(
        const std::string name,
        int expire_interval = 1,
        int attemps_limit = 2)
    {
        yplatform::ptree arg;
        arg.add("name", name);
        arg.add("expire_interval", expire_interval);
        arg.add("attemps_limit", attemps_limit);
        return arg;
    }

    yplatform::ptree make_limiter_cfg()
    {
        yplatform::ptree cfg;
        cfg.add("cleanup_interval", 1);
        cfg.push_back(std::make_pair("args", make_arg("ip")));
        cfg.push_back(std::make_pair("args", make_arg("email")));
        return cfg;
    }

    void success_acquire_cb(error ec, const acquire_result& result)
    {
        REQUIRE(!ec);
        REQUIRE(result.success);
    };

    void rate_limit_exeeded_cb(error ec, const acquire_result& result)
    {
        REQUIRE(!ec);
        REQUIRE(!result.success);
    };

    void run()
    {
        io_.run();
        io_.reset();
    }

    boost::asio::io_service io_;
    rate_limiter_ptr rate_limiter_;
};

TEST_CASE_METHOD(rate_limiter_test, "test clean rate limiter internal cache")
{
    auto key = "ip";
    auto values = { "1", "2", "3", "4", "5" };

    REQUIRE(!rate_limiter_->attemps_size(key));
    io_.post([this, key, values]() {
        for (auto& value : values)
        {
            rate_limiter_->try_acquire(
                key,
                value,
                std::bind(&rate_limiter_test::success_acquire_cb, this, ph::_1, ph::_2));
            rate_limiter_->try_acquire(
                key,
                value,
                std::bind(&rate_limiter_test::success_acquire_cb, this, ph::_1, ph::_2));
            rate_limiter_->try_acquire(
                key,
                value,
                std::bind(&rate_limiter_test::rate_limit_exeeded_cb, this, ph::_1, ph::_2));
        }
        REQUIRE(rate_limiter_->attemps_size(key) == 5);
    });

    // wait for cleanup timer
    std::this_thread::sleep_for(std::chrono::seconds(1));
    REQUIRE(!rate_limiter_->attemps_size(key));
}

TEST_CASE_METHOD(rate_limiter_test, "test rate limit")
{
    auto key = "ip";
    auto value = "127.0.0.1";
    rate_limiter_->try_acquire(
        key, value, std::bind(&rate_limiter_test::success_acquire_cb, this, ph::_1, ph::_2));
    rate_limiter_->try_acquire(
        key, value, std::bind(&rate_limiter_test::success_acquire_cb, this, ph::_1, ph::_2));
    rate_limiter_->try_acquire(
        key, value, std::bind(&rate_limiter_test::rate_limit_exeeded_cb, this, ph::_1, ph::_2));

    // wait for expired
    std::this_thread::sleep_for(std::chrono::seconds(1));

    rate_limiter_->try_acquire(
        key, value, std::bind(&rate_limiter_test::success_acquire_cb, this, ph::_1, ph::_2));
}
