#include <yplatform/util/safe_call.h>
#include <catch.hpp>

using namespace yplatform;

void normal_handler(int& i)
{
    ++i;
}

int int_result_handler(int& i)
{
    return ++i;
}

void handler_throws_exception(int&)
{
    throw std::runtime_error("test exception");
}

int int_result_handler_throws_int(int& i)
{
    throw i;
}

TEST_CASE("safe_call/normal_handler", "")
{
    task_context_ptr ctx = boost::make_shared<task_context>();
    int i = 0;
    REQUIRE_NOTHROW(safe_call(&normal_handler, i));
    REQUIRE(i == 1);
    REQUIRE_NOTHROW(safe_call("message", &normal_handler, i));
    REQUIRE(i == 2);
    REQUIRE_NOTHROW(safe_call(ctx, &normal_handler, i));
    REQUIRE(i == 3);
    REQUIRE_NOTHROW(safe_call(ctx, "message", &normal_handler, i));
    REQUIRE(i == 4);
}

TEST_CASE("safe_call/handler_throws_exception", "")
{
    task_context_ptr ctx = boost::make_shared<task_context>();
    int i = 0;
    REQUIRE_NOTHROW(safe_call(&handler_throws_exception, i));
    REQUIRE(i == 0);
    REQUIRE_NOTHROW(safe_call("message", &handler_throws_exception, i));
    REQUIRE(i == 0);
    REQUIRE_NOTHROW(safe_call(ctx, &handler_throws_exception, i));
    REQUIRE(i == 0);
    REQUIRE_NOTHROW(safe_call(ctx, "message", &handler_throws_exception, i));
    REQUIRE(i == 0);
}

TEST_CASE("safe_call/int_result_handler_throws_int", "")
{
    task_context_ptr ctx = boost::make_shared<task_context>();
    int i = 0;
    REQUIRE_NOTHROW(safe_call(&int_result_handler_throws_int, i));
    REQUIRE(i == 0);
    REQUIRE_NOTHROW(safe_call("message", &int_result_handler_throws_int, i));
    REQUIRE(i == 0);
    REQUIRE_NOTHROW(safe_call(ctx, &int_result_handler_throws_int, i));
    REQUIRE(i == 0);
    REQUIRE_NOTHROW(safe_call(ctx, "message", &int_result_handler_throws_int, i));
    REQUIRE(i == 0);
}

TEST_CASE("safe_call/int_result_handler/result", "")
{
    int i = 0;
    auto res = safe_call(&int_result_handler, i);
    REQUIRE(res);
    REQUIRE(*res == 1);
}

TEST_CASE("safe_call/int_result_handler_throws_int/result", "")
{
    int i = 0;
    auto res = safe_call(&int_result_handler_throws_int, i);
    REQUIRE(!res);
}
