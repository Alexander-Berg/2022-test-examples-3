#include <yplatform/future/future.hpp>
#include <yplatform/yield.h>
#include <catch.hpp>

using yplatform::future::future;
using yplatform::future::promise;

TEST_CASE("future/init")
{
    promise<int> promise;
    future<int> future = promise;
    REQUIRE(!future.ready());
    REQUIRE(!future.has_value());
    REQUIRE(!future.has_exception());
}

TEST_CASE("future/has_value_and_no_exception_after_promise_set")
{
    promise<int> promise;
    future<int> future = promise;
    promise.set(1);
    REQUIRE(future.ready());
    REQUIRE(future.has_value());
    REQUIRE(!future.has_exception());
}

TEST_CASE("future/has_no_value_and_exception_after_promise_set_exception")
{
    promise<int> promise;
    future<int> future = promise;
    promise.set_exception(std::runtime_error(""));
    REQUIRE(future.ready());
    REQUIRE(!future.has_value());
    REQUIRE(future.has_exception());
}

TEST_CASE("future/set_and_get_same_value")
{
    promise<int> promise;
    future<int> future = promise;
    promise.set(1);
    REQUIRE(future.ready());
    REQUIRE(future.get() == 1);
}

TEST_CASE("future/throws_same_exception")
{
    promise<int> promise;
    future<int> future = promise;
    promise.set_exception(std::logic_error(""));
    REQUIRE_THROWS_AS(future.get(), std::logic_error);
}

TEST_CASE("future/set_value_twice_stores_first_value")
{
    promise<int> promise;
    future<int> future = promise;
    promise.set(1);
    promise.set(2);
    REQUIRE(future.ready());
    REQUIRE(future.get() == 1);
}

TEST_CASE("future/set_broken_promise_exception")
{
    auto promise = std::make_unique<yplatform::future::promise<int>>();
    future<int> future = *promise;
    promise.reset();
    REQUIRE(future.ready());
    REQUIRE(!future.has_value());
    REQUIRE(future.has_exception());
}

TEST_CASE("future/has_value_only_after_set_exception_and_then_value")
{
    promise<int> promise;
    future<int> future = promise;
    promise.set(1);
    promise.set_exception(std::runtime_error(""));
    REQUIRE(future.ready());
    REQUIRE(future.has_value());
    REQUIRE(!future.has_exception());
}

TEST_CASE("future/has_exception_only_after_set_exception_and_then_value")
{
    promise<int> promise;
    future<int> future = promise;
    promise.set_exception(std::runtime_error(""));
    promise.set(1);
    REQUIRE(future.ready());
    REQUIRE(!future.has_value());
    REQUIRE(future.has_exception());
}

TEST_CASE("future/then/proxies_value")
{
    promise<int> promise;
    future<int> future = promise;
    auto outer_future = future.then([](auto future) { return future.get(); });
    promise.set(1);
    REQUIRE(outer_future.ready());
    REQUIRE(outer_future.get() == 1);
}

TEST_CASE("future/then/proxies_exceptions")
{
    promise<int> promise;
    future<int> future = promise;
    auto outer_future =
        future.then([](auto /*future*/) -> int { throw std::runtime_error("error"); });
    promise.set(1);
    REQUIRE(outer_future.ready());
    REQUIRE(outer_future.has_exception());
}

TEST_CASE("future/then/async_operations_chain_finished_after_all_promises_are_set")
{
    promise<int> promise1;
    promise<double> promise2;
    auto op1 = [promise1]() -> future<int> { return promise1; };
    auto op2 = [promise2](future<int>) -> future<double> { return promise2; };
    auto final_future = op1().then(op2);
    REQUIRE(!final_future.ready());
    promise1.set(1);
    REQUIRE(!final_future.ready());
    promise2.set(1.0);
    REQUIRE(final_future.ready());
    REQUIRE(final_future.get() == 1.0);
}

TEST_CASE("future/then/async_chain_with_future_void")
{
    promise<void> promise1;
    promise<void> promise2;
    auto op1 = [promise1]() -> future<void> { return promise1; };
    auto op2 = [promise2](future<void>) -> future<void> { return promise2; };
    auto final_future = op1().then(op2);
    promise1.set();
    promise2.set();
    REQUIRE(final_future.ready());
}

TEST_CASE("future/then/catch_exception_from_async_chain")
{
    promise<void> promise1;
    promise<void> promise2;
    auto op1 = [promise1]() -> future<void> { return promise1; };
    auto op2 = [promise2](future<void> f) -> future<void> {
        f.get();
        return promise2;
    };
    auto final_future = op1().then(op2);
    promise1.set_exception(std::runtime_error("error"));
    REQUIRE(!future<void>(promise2).ready());
    REQUIRE(final_future.ready());
    REQUIRE(final_future.has_exception());
}

TEST_CASE("future/then/pass_exception_through_async_chain")
{
    promise<void> promise1;
    promise<void> promise2;
    auto op1 = [promise1]() -> future<void> { return promise1; };
    auto op2 = [promise2](future<void> f) mutable -> future<void> {
        try
        {
            f.get();
            promise2.set();
        }
        catch (const std::exception&)
        {
            promise2.set_current_exception();
        }
        return promise2;
    };
    auto final_future = op1().then(op2);
    promise1.set_exception(std::runtime_error("error"));
    REQUIRE(future<void>(promise2).ready());
    REQUIRE(future<void>(promise2).has_exception());
    REQUIRE(final_future.ready());
    REQUIRE(final_future.has_exception());
}

TEST_CASE("future/assign_to_empty_promise")
{
    promise<void> promise1;
    promise<void> promise2;
    future<void> future = promise1;
    REQUIRE(!future.ready());
    REQUIRE(!future.has_exception());
    REQUIRE_NOTHROW(promise1 = promise2);
    REQUIRE(future.ready());
    REQUIRE(future.has_exception());
}

TEST_CASE("future/reset_promise")
{
    promise<void> promise;
    future<void> future = promise;
    REQUIRE(!future.ready());
    REQUIRE(!future.has_exception());
    REQUIRE_NOTHROW(promise.reset());
    REQUIRE(future.ready());
    REQUIRE(future.has_exception());
}
