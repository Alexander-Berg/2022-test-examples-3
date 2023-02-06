#include <yplatform/coroutine.h>
#include <yplatform/yield.h>
#include <catch.hpp>

using namespace yplatform;

struct simple_coroutine
{
    using yield_ctx = yield_context<simple_coroutine>;

    bool finished = false;

    void operator()(yield_ctx /*ctx*/)
    {
        finished = true;
    }
};

struct coroutine_with_bool_arg
{
    using yield_ctx = yield_context<coroutine_with_bool_arg>;

    bool finished = false;

    void operator()(yield_ctx /*ctx*/, bool arg)
    {
        finished = arg;
    }
};

struct coroutine_with_captures
{
    using yield_ctx = yield_context<coroutine_with_captures>;

    boost::asio::io_service* io;
    bool finished = false;

    void operator()(yield_ctx ctx)
    {
        reenter(ctx)
        {
            yield async_op(ctx.capture(finished));
        };
    }

    template <typename Handler>
    void async_op(Handler handler)
    {
        io->post(std::bind(handler, true));
    }
};

struct coroutine_with_exception_handling
{
    using yield_ctx = yield_context<coroutine_with_exception_handling>;

    std::string error = {};

    void operator()(yield_ctx ctx)
    {
        reenter(ctx)
        {
            yield throw std::runtime_error("failed to start async operation");
        };
    }

    void operator()(yield_ctx::exception_type e)
    {
        try
        {
            std::rethrow_exception(e);
        }
        catch (const std::exception& e)
        {
            error = e.what();
        }
    }
};

TEST_CASE("coroutine/shared_simple")
{
    auto coro = std::make_shared<simple_coroutine>();
    spawn(coro);
    REQUIRE(coro->finished);
}

TEST_CASE("coroutine/shared_with_arg")
{
    auto coro = std::make_shared<coroutine_with_bool_arg>();
    bool arg = true;
    spawn(coro, arg);
    REQUIRE(coro->finished);
}

TEST_CASE("coroutine/compilation_only/braced_init_simple")
{
    spawn(simple_coroutine{});
    REQUIRE(true);
}

TEST_CASE("coroutine/compilation_only/braced_init_with_arg")
{
    bool arg = true;

    spawn(coroutine_with_bool_arg{}, arg);
    REQUIRE(true);
}

TEST_CASE("coroutine/compilation_only/braced_init_simple_on_io_service")
{
    boost::asio::io_service io;
    spawn(io, simple_coroutine{});
    REQUIRE(true);
}

TEST_CASE("coroutine/compilation_only/braced_init_simple_on_executor")
{
    boost::asio::io_service io;
    spawn(io.get_executor(), simple_coroutine{});
    REQUIRE(true);
}

TEST_CASE("coroutine/capture_variables")
{
    boost::asio::io_service io;
    auto coro = std::make_shared<coroutine_with_captures>(coroutine_with_captures{ &io });
    spawn(io, coro);
    io.run();
    REQUIRE(coro->finished);
}

TEST_CASE("coroutine/exception_handling")
{
    boost::asio::io_service io;
    auto coro = std::make_shared<coroutine_with_exception_handling>();
    spawn(io, coro);
    io.run();
    REQUIRE(coro->error == "failed to start async operation");
}
