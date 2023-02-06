#include <catch.hpp>

#include <boost/asio.hpp>
#include "../src/pool_resolver.h"
#include "stub_resolver.h"
#include "defines.h"

using namespace ymod_messenger;

struct T_POOL_RESOLVER
{
    enum class resolve_order_t
    {
        ipv6_ipv4,
        ipv4_ipv6,
        ipv6,
        ipv4
    };
    using pool_resolver_t = pool_resolver<stub_resolver, resolve_order_t>;

    shared_ptr<pool_resolver_t> resolver;

    boost::asio::io_service io;
    bool received_result{ false };
    bool received_error{ false };
    host_info result;

    void create_resolver(
        resolve_order_t order = resolve_order_t::ipv6_ipv4,
        time_traits::duration timeout = time_traits::seconds(5),
        bool delay = false)
    {
        resolver.reset(new pool_resolver_t(
            io,
            stub_resolver(io, 2 * timeout, delay),
            stub_resolver(io, 2 * timeout, delay),
            order,
            timeout));
    }

    void resolve_incoming(const host_info& address)
    {
        namespace p = std::placeholders;
        resolver->resolve_incoming(
            address,
            std::bind(&T_POOL_RESOLVER::on_resolve, this, p::_1),
            std::bind(&T_POOL_RESOLVER::on_error, this));
    }

    void resolve_outgoing(const host_info& address)
    {
        namespace p = std::placeholders;
        resolver->resolve_outgoing(
            address,
            std::bind(&T_POOL_RESOLVER::on_resolve, this, p::_1),
            std::bind(&T_POOL_RESOLVER::on_error, this));
    }

    void on_resolve(const host_info& host)
    {
        received_result = true;
        result = host;
    }

    void on_error()
    {
        received_error = true;
    }
};

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_incoming", "")
{
    create_resolver();
    resolve_incoming(make_host_info(T_HOST_IP ":8080"));
    io.run();
    REQUIRE(received_result);
    REQUIRE(!received_error);
    REQUIRE(result == T_HOST_RESOLVED);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_incoming_fails", "")
{
    create_resolver();
    resolve_incoming(make_host_info("3.3.3.3:8080"));
    io.run();
    REQUIRE(!received_result);
    REQUIRE(received_error);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv6_ipv4", "")
{
    create_resolver();
    resolve_outgoing(T_HOST);
    io.run();
    REQUIRE(received_result);
    REQUIRE(!received_error);
    REQUIRE(result == T_HOST_RESOLVED);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv6_ipv4_fails", "")
{
    create_resolver();
    resolve_outgoing(T_BAD_HOST);
    io.run();
    REQUIRE(!received_result);
    REQUIRE(received_error);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv6_ipv4_6_fails", "")
{
    create_resolver();
    resolve_outgoing(T_HOST_4_ONLY);
    io.run();
    REQUIRE(received_result);
    REQUIRE(!received_error);
    REQUIRE(result == T_HOST_RESOLVED);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv4_ipv6", "")
{
    create_resolver(resolve_order_t::ipv4_ipv6);
    resolve_outgoing(T_HOST);
    io.run();
    REQUIRE(received_result);
    REQUIRE(!received_error);
    REQUIRE(result == T_HOST_RESOLVED);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv4_ipv6_fails", "")
{
    create_resolver(resolve_order_t::ipv4_ipv6);
    resolve_outgoing(T_BAD_HOST);
    io.run();
    REQUIRE(!received_result);
    REQUIRE(received_error);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv4_ipv6_4_fails", "")
{
    create_resolver(resolve_order_t::ipv4_ipv6);
    resolve_outgoing(T_HOST_6_ONLY);
    io.run();
    REQUIRE(received_result);
    REQUIRE(!received_error);
    REQUIRE(result == T_HOST_RESOLVED);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv6", "")
{
    create_resolver(resolve_order_t::ipv6);
    resolve_outgoing(T_HOST_6_ONLY);
    io.run();
    REQUIRE(received_result);
    REQUIRE(!received_error);
    REQUIRE(result == T_HOST_RESOLVED);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv6_fails", "")
{
    create_resolver(resolve_order_t::ipv6);
    resolve_outgoing(T_HOST_4_ONLY);
    io.run();
    REQUIRE(!received_result);
    REQUIRE(received_error);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv4", "")
{
    create_resolver(resolve_order_t::ipv4);
    resolve_outgoing(T_HOST_4_ONLY);
    io.run();
    REQUIRE(received_result);
    REQUIRE(!received_error);
    REQUIRE(result == T_HOST_RESOLVED);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/resolves_outgoing_ipv4_fails", "")
{
    create_resolver(resolve_order_t::ipv4);
    resolve_outgoing(T_HOST_6_ONLY);
    io.run();
    REQUIRE(!received_result);
    REQUIRE(received_error);
}

TEST_CASE_METHOD(T_POOL_RESOLVER, "pool_resolver/handles_resolve_timeout", "")
{
    create_resolver(resolve_order_t::ipv6_ipv4, time_traits::seconds(1), true);
    resolve_outgoing(T_HOST);
    io.run();
    REQUIRE(!received_result);
    REQUIRE(received_error);
}
