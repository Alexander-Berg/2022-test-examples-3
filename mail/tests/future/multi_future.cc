#include <yplatform/future/multi_future.hpp>

#include <gtest/gtest.h>

#include <vector>

namespace {

using namespace testing;
using namespace yplatform::future;

const std::string error = "promise 0 error";

TEST(future_multi_and, empty)
{
    std::vector<future<void>> futures;
    future<void> acc = future_multi_and(futures);
    ASSERT_THROW(acc.get(), broken_promise);
}

TEST(future_multi_and, ok_one)
{
    promise<void> prom;
    std::vector<future<void>> futures = { prom };
    future<void> one_acc = future_multi_and(futures);
    prom.set();
    one_acc.get();
}

TEST(future_multi_and, ok_several)
{
    std::vector<promise<void>> proms(10);
    std::vector<future<void>> futures(proms.begin(), proms.end());
    future<void> several_acc = future_multi_and(futures);
    for (std::size_t i = 0; i < proms.size(); i++)
        proms[i].set();
    several_acc.get();
}

TEST(future_multi_and, not_ready_several)
{
    std::vector<promise<void>> proms(10);
    std::vector<future<void>> futures(proms.begin(), proms.end());
    future<void> several_acc = future_multi_and(futures);
    for (std::size_t i = 1; i < proms.size(); i++)
        proms[i].set();
    ASSERT_EQ(several_acc.ready(), false);
}

TEST(future_multi_and, first_with_exception_then_several_futures_set)
{
    std::vector<promise<void>> proms(10);
    std::vector<future<void>> futures(proms.begin(), proms.end());
    future<void> several_acc = future_multi_and(futures);
    proms[0].set_exception(std::runtime_error(error));
    for (std::size_t i = 1; i < proms.size(); i++)
        proms[i].set();
    try
    {
        several_acc.get();
    }
    catch (const std::exception& ex)
    {
        ASSERT_EQ(ex.what(), error);
    }
}

TEST(future_multi_and, several_futures_set_then_first_with_exception)
{
    std::vector<promise<void>> proms(10);
    std::vector<future<void>> futures(proms.begin(), proms.end());
    future<void> several_acc = future_multi_and(futures);
    for (std::size_t i = 1; i < proms.size(); i++)
        proms[i].set();
    proms[0].set_exception(std::runtime_error(error));
    try
    {
        several_acc.get();
    }
    catch (const std::exception& ex)
    {
        ASSERT_EQ(ex.what(), error);
    }
}

TEST(future_multi_and, several_futures_not_set_then_first_with_exception)
{
    std::vector<promise<void>> proms(10);
    std::vector<future<void>> futures(proms.begin(), proms.end());
    future<void> several_acc = future_multi_and(futures);
    proms[0].set_exception(std::runtime_error(error));
    try
    {
        several_acc.get();
    }
    catch (const std::exception& ex)
    {
        ASSERT_EQ(ex.what(), error);
    }
}

TEST(future_multi_and, second_set_exception_then_first_set_exception)
{
    std::vector<promise<void>> proms(10);
    std::vector<future<void>> futures(proms.begin(), proms.end());
    future<void> several_acc = future_multi_and(futures);
    proms[1].set_exception(std::runtime_error(error));
    proms[0].set_exception(std::runtime_error("other error"));
    try
    {
        several_acc.get();
    }
    catch (const std::exception& ex)
    {
        ASSERT_EQ(ex.what(), error);
    }
}

TEST(future_multi_and, getter_ok_several)
{
    struct fp
    {
        future<void> f;
        promise<void> p;
    };
    std::vector<fp> tasks(10);
    for (auto& task : tasks)
        task.f = task.p;

    future<void> several_acc =
        future_multi_and(tasks, [](const fp& x) -> future<void> { return x.f; });
    for (auto& task : tasks)
        task.p.set();
    several_acc.get();
}

TEST(future_multi_and, getter_exception)
{
    struct fp
    {
        future<void> f;
        promise<void> p;
        fp()
        {
            f = p;
        }
    };
    std::vector<fp> tasks(10);
    future<void> several_acc =
        future_multi_and(tasks, [](const fp& x) -> future<void> { return x.f; });
    tasks[0].p.set_exception(std::runtime_error(error));
    try
    {
        several_acc.get();
    }
    catch (const std::exception& ex)
    {
        ASSERT_EQ(ex.what(), error);
    }
}
}
