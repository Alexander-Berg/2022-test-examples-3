#include "fakes/http_session.h"
#include "session_pool.h"
#include <catch.hpp>
#include <future>

using namespace ymod_httpclient;

template <bool Congestion>
struct fake_controller
{
    bool try_run()
    {
        return !Congestion;
    }

    void finish_with_congestion()
    {
    }

    void finish_without_congestion()
    {
    }
};

using fake_controller_with_congestion = fake_controller<true>;
using fake_controller_without_congestion = fake_controller<false>;

template <typename CongestionController>
struct pool_wrapper : session_pool<fakes::http_session, CongestionController>
{
    using base_type = session_pool<fakes::http_session, CongestionController>;

    pool_wrapper(yplatform::reactor_ptr reactor)
        : base_type(reactor, prefered_pool_size, reactor_overload_delay), io(*reactor->io())
    {
        store_tracer(io, make_shared<yplatform::reactor_tracer>(io));
    }

    template <typename CreateMethod>
    auto create(CreateMethod create_method)
    {
        std::promise<std::tuple<http_error::code, std::string, fakes::http_session_ptr>> promise;
        base_type::create(
            ctx,
            create_method,
            connect_timeout,
            [&promise](
                http_error::code errc,
                const std::string& reason,
                fakes::http_session_ptr session) mutable {
                promise.set_value(std::make_tuple(errc, reason, session));
            });
        return promise.get_future().get();
    }

    auto create_reusable_session()
    {
        return create(::fakes::create_reusable_session);
    }

    auto create_not_reusable_session()
    {
        return create(::fakes::create_not_reusable_session);
    }

    auto create_broken_session()
    {
        return create(::fakes::create_broken_session);
    }

    static inline const std::size_t prefered_pool_size = 2;
    static inline const time_traits::duration connect_timeout = time_traits::milliseconds(1);
    static inline const time_traits::duration reactor_overload_delay = time_traits::milliseconds(1);
    yplatform::task_context_ptr ctx = boost::make_shared<yplatform::task_context>();
    boost::asio::io_service& io;
};

struct t_session_pool
{
    t_session_pool()
    {
        reactor = boost::make_shared<yplatform::reactor>();
        reactor->init(1, 1);
        pool = std::make_shared<pool_wrapper<fake_controller_without_congestion>>(reactor);
    }

    yplatform::reactor_ptr reactor;
    shared_ptr<pool_wrapper<fake_controller_without_congestion>> pool;
};

struct t_session_pool_with_congestion
{
    t_session_pool_with_congestion()
    {
        reactor = boost::make_shared<yplatform::reactor>();
        reactor->init(1, 1);
        pool = std::make_shared<pool_wrapper<fake_controller_with_congestion>>(reactor);
    }

    yplatform::reactor_ptr reactor;
    shared_ptr<pool_wrapper<fake_controller_with_congestion>> pool;
};

TEST_CASE_METHOD(t_session_pool, "session_pool/return_nullptr")
{
    REQUIRE_NOTHROW(pool->put(nullptr));
}

TEST_CASE_METHOD(t_session_pool, "session_pool/close_empty_pool")
{
    REQUIRE_NOTHROW(pool->close());
}

TEST_CASE_METHOD(t_session_pool, "session_pool/empty_pool_size")
{
    REQUIRE(pool->size() == 0);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/empty_pool_acquired_size")
{
    REQUIRE(pool->acquired_size() == 0);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/get_session_from_empty_pool")
{
    REQUIRE(!pool->get());
}

TEST_CASE_METHOD(t_session_pool, "session_pool/return_session_created_not_through_pool")
{
    boost::asio::io_service io;
    auto session = fakes::create_reusable_session(io);
    REQUIRE_THROWS(pool->put(session));
}

TEST_CASE_METHOD(t_session_pool, "session_pool/connect_error")
{
    auto [err, reason, session] = pool->create_broken_session();
    REQUIRE(err != http_error::success);
}

TEST_CASE_METHOD(t_session_pool_with_congestion, "session_pool/create_with_congestion")
{
    auto [err, reason, session] = pool->create_reusable_session();
    REQUIRE(err == http_error::task_throttled);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/create")
{
    auto [err, reason, session] = pool->create_reusable_session();
    REQUIRE(err == http_error::success);
    REQUIRE(reason == "");
    REQUIRE(session);
    REQUIRE(session->connected);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/return_reusable")
{
    auto [err, reason, session] = pool->create_reusable_session();
    pool->put(session);
    REQUIRE(pool->size() == 1);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/return_not_reusable")
{
    auto [err, reason, session] = pool->create_not_reusable_session();
    pool->put(session);
    REQUIRE(pool->size() == 0);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/acquired_size_after_create")
{
    pool->create_reusable_session();
    REQUIRE(pool->acquired_size() == 1);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/acquired_size_after_return")
{
    auto [err, reason, session] = pool->create_reusable_session();
    pool->put(session);
    REQUIRE(pool->acquired_size() == 0);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/put_session_into_idle")
{
    auto [err, reason, session] = pool->create_reusable_session();
    pool->put(session);
    REQUIRE(session->idling);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/get_session")
{
    auto [err, reason, session] = pool->create_reusable_session();
    pool->put(session);
    session.reset();
    REQUIRE(pool->get());
}

TEST_CASE_METHOD(t_session_pool, "session_pool/stop_idle")
{
    auto [err, reason, session] = pool->create_reusable_session();
    pool->put(session);
    pool->get();
    REQUIRE(!session->idling);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/double_get_session")
{
    auto [err, reason, session] = pool->create_reusable_session();
    pool->put(session);
    pool->get();
    REQUIRE(!pool->get());
}

TEST_CASE_METHOD(t_session_pool, "session_pool/close")
{
    auto [err, reason, session] = pool->create_reusable_session();
    pool->put(session);
    pool->close();
    REQUIRE(session->closed);
}

TEST_CASE_METHOD(t_session_pool, "session_pool/dont_return_closed_session")
{
    auto [err, reason, session] = pool->create_reusable_session();
    pool->put(session);
    session->close_from_remote();
    REQUIRE(!pool->get());
}
