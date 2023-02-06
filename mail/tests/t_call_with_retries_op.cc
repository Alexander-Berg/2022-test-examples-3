#include <catch.hpp>
#include <fakes/balancing_call_op.h>
#include <ymod_httpclient/detail/call_with_retries_op.h>

using namespace ymod_httpclient;
using std::vector;

struct test_interpreter
{
    template <typename Handler>
    void operator()(response response, Handler&& handler)
    {
        handler(res, response);
    }

    response_status res = response_status::ok;
};

using call_with_retries_op = detail::call_with_retries_op<
    fakes::balancing_call_op,
    simple_call::callback_type,
    test_interpreter,
    response>;

struct t_call_with_retries_op
{
    t_call_with_retries_op() : balancing_call_op(make_shared<fakes::balancing_call_op>())
    {
    }

    void balancing_call(request req, options opt = {}, boost::optional<unsigned> max_attempts = {})
    {
        err = boost::system::error_code();
        resp = response();
        if (!stat)
        {
            stat = std::make_shared<detail::request_stat>(
                5, retry_settings.budget.count_connect_errors, 0);
        }
        call_with_retries_op::run(
            io,
            make_shared<settings>(http_settings),
            make_shared<::retry_settings>(retry_settings),
            stat,
            balancing_call_op,
            boost::optional<detail::backoff>(),
            boost::make_shared<yplatform::task_context>(),
            req,
            opt,
            max_attempts,
            interpreter,
            [this](boost::system::error_code err, response resp) {
                this->err = err;
                this->resp = resp;
            });
        io.run();
        io.reset();
    }

    auto current_stat()
    {
        return stat->errors_budget.get_current();
    }

    boost::asio::io_service io;
    settings http_settings;
    retry_settings retry_settings{ .max_attempts = 3 };
    shared_ptr<detail::request_stat> stat;
    shared_ptr<fakes::balancing_call_op> balancing_call_op;
    test_interpreter interpreter;
    boost::system::error_code err;
    response resp;
};

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/success")
{
    balancing_call_op->host = "status200";
    balancing_call(request::GET("/"));
    REQUIRE(!err);
    REQUIRE(resp.status == 200);
    REQUIRE(balancing_call_op->journal == vector<string>{ "status200/" });
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/retriable_status")
{
    balancing_call_op->host = "status500";
    balancing_call(request::GET("/"));
    REQUIRE(!err);
    REQUIRE(resp.status == 500);
    REQUIRE(
        balancing_call_op->journal == vector<string>({ "status500/", "status500/", "status500/" }));
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/interpreter_tmp_error")
{
    balancing_call_op->host = "status200";
    interpreter.res = response_status::tmp_error;
    balancing_call(request::GET("/"));
    REQUIRE(err == http_error::bad_response);
    REQUIRE(
        balancing_call_op->journal == vector<string>({ "status200/", "status200/", "status200/" }));
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/not_retriable_status")
{
    balancing_call_op->host = "status400";
    balancing_call(request::GET("/"));
    REQUIRE(!err);
    REQUIRE(resp.status == 400);
    REQUIRE(balancing_call_op->journal == vector<string>{ "status400/" });
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/retriable_ec")
{
    balancing_call_op->host = "timeout";
    balancing_call(request::GET("/"));
    REQUIRE(err == http_error::request_timeout);
    REQUIRE(balancing_call_op->journal == vector<string>({ "timeout/", "timeout/", "timeout/" }));
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/not_retriable_ec")
{
    balancing_call_op->host = "unsupported_scheme";
    balancing_call(request::GET("/"));
    REQUIRE(err == http_error::unsupported_scheme);
    REQUIRE(balancing_call_op->journal == vector<string>{ "unsupported_scheme/" });
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/retry_budget")
{
    balancing_call_op->host = "x";
    retry_settings.max_attempts = 15;
    retry_settings.budget.enabled = true;
    retry_settings.budget.value = 0.3;
    for (int i = 0; i < 9; ++i)
    {
        balancing_call(request::GET("/status200"));
    }
    balancing_call(request::GET("/status500"));
    // 10 tries (9 status 200, 1 status 500) and 3 retries
    REQUIRE(
        balancing_call_op->journal ==
        vector<string>({ "x/status200",
                         "x/status200",
                         "x/status200",
                         "x/status200",
                         "x/status200",
                         "x/status200",
                         "x/status200",
                         "x/status200",
                         "x/status200",
                         "x/status500",
                         "x/status500",
                         "x/status500",
                         "x/status500" }));
    REQUIRE(stat->retries_ratio == 0.3);
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/retry_budget/count_connect_error")
{
    balancing_call_op->host = "x";
    retry_settings.max_attempts = 5;
    retry_settings.budget.enabled = true;
    retry_settings.budget.value = 0.5;
    retry_settings.budget.count_connect_errors = true;
    balancing_call(request::GET("/status200"));
    balancing_call(request::GET("/connect_error"));
    REQUIRE(
        balancing_call_op->journal ==
        vector<string>({ "x/status200", "x/connect_error", "x/connect_error" }));
}

TEST_CASE_METHOD(
    t_call_with_retries_op,
    "call_with_retries_op/retry_budget/dont_count_connect_error")
{
    balancing_call_op->host = "x";
    retry_settings.max_attempts = 5;
    retry_settings.budget.enabled = false;
    retry_settings.budget.value = 0.5;
    retry_settings.budget.count_connect_errors = true;
    balancing_call(request::GET("/status200"));
    balancing_call(request::GET("/connect_error"));
    REQUIRE(
        balancing_call_op->journal ==
        vector<string>({ "x/status200",
                         "x/connect_error",
                         "x/connect_error",
                         "x/connect_error",
                         "x/connect_error",
                         "x/connect_error" }));
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/dont_split_timeout")
{
    balancing_call_op->host = "x";
    retry_settings.max_attempts = 5;
    retry_settings.budget.enabled = false;
    retry_settings.budget.value = 0.5;
    retry_settings.budget.count_connect_errors = true;
    options opt;
    opt.timeouts.total = yplatform::time_traits::milliseconds(100);
    balancing_call(request::GET("/status200"), opt);
    REQUIRE(balancing_call_op->opts.size() == 1);
    REQUIRE(balancing_call_op->opts[0].timeouts.total == opt.timeouts.total);
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/split_timeout")
{
    using yplatform::time_traits::milliseconds;
    balancing_call_op->host = "x";
    retry_settings.max_attempts = 4;
    retry_settings.budget.enabled = false;
    retry_settings.budget.value = 0.5;
    retry_settings.budget.count_connect_errors = true;
    retry_settings.split_timeout = true;
    options opt;
    opt.timeouts.total = milliseconds(100);
    balancing_call(request::GET("/status500"), opt);
    REQUIRE(balancing_call_op->opts.size() == 4);
    REQUIRE(balancing_call_op->opts[0].timeouts.total == opt.timeouts.total / 2);
    // We don't wait total/2 because in tests there is no delay,
    // so for first retry should be (total - 0)/3 instead of (total - total/2)/3.
    REQUIRE(opt.timeouts.total / 3 - balancing_call_op->opts[1].timeouts.total <= milliseconds(5));
    REQUIRE(opt.timeouts.total / 2 - balancing_call_op->opts[2].timeouts.total <= milliseconds(5));
    REQUIRE(opt.timeouts.total / 1 - balancing_call_op->opts[3].timeouts.total <= milliseconds(5));
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/stats/success")
{
    balancing_call_op->host = "status200";
    balancing_call(request::GET("/"));
    REQUIRE(current_stat().requests == 1);
    REQUIRE(current_stat().retries == 0);
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/stats/not_retriable_status")
{
    balancing_call_op->host = "status400";
    balancing_call(request::GET("/"));
    REQUIRE(current_stat().requests == 1);
    REQUIRE(current_stat().retries == 0);
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/stats/retriable_status")
{
    balancing_call_op->host = "status500";
    balancing_call(request::GET("/"));
    REQUIRE(current_stat().requests == 3);
    REQUIRE(current_stat().retries == 2);
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/stats/retriable_ec")
{
    balancing_call_op->host = "timeout";
    balancing_call(request::GET("/"));
    REQUIRE(current_stat().requests == 3);
    REQUIRE(current_stat().retries == 2);
}

TEST_CASE_METHOD(t_call_with_retries_op, "call_with_retries_op/stats/not_retriable_ec")
{
    balancing_call_op->host = "unsupported_scheme";
    balancing_call(request::GET("/"));
    REQUIRE(current_stat().requests == 1);
    REQUIRE(current_stat().retries == 0);
}
