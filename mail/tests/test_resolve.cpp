#include "resolver_fake.hpp"
#include "callback.hpp"
#include <apq/detail/resolve_op.hpp>
#include <apq/error.hpp>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

struct connection_impl_fake
{
    bool ipv6_only_ = false;
};

struct test_resolve : ::testing::Test
{
    boost::shared_ptr<resolver_fake> resolver = boost::make_shared<resolver_fake>();
    boost::shared_ptr<connection_impl_fake> conn = boost::make_shared<connection_impl_fake>();
    apq::detail::connection_info conninfo = { { "host", "localhost" },
                                              { "port", "6432" },
                                              { "dbname", "dbname" } };

    std::tuple<apq::result, apq::detail::connection_info> async_resolve()
    {
        callback<apq::result, apq::detail::connection_info> cb;
        auto op = std::make_shared<apq::detail::resolve_op<connection_impl_fake, resolver_fake>>(
            conn, resolver, conninfo, cb);
        yplatform::spawn(op);
        return cb.args();
    }

    void add_ipv4_addr(const std::string& host, const std::string& ip)
    {
        resolver->known_ipv4_hosts[host].emplace_back(ip);
    }

    void add_ipv6_addr(const std::string& host, const std::string& ip)
    {
        resolver->known_ipv6_hosts[host].emplace_back(ip);
    }

    void set_error_on_resolve_ipv4(const std::string& host, boost::system::error_code err)
    {
        resolver->broken_ipv4_hosts[host] = err;
    }

    void set_error_on_resolve_ipv6(const std::string& host, boost::system::error_code err)
    {
        resolver->broken_ipv6_hosts[host] = err;
    }

    void set_ipv6_only()
    {
        conn->ipv6_only_ = true;
    }
};

TEST_F(test_resolve, empty_host)
{
    conninfo["host"] = "";
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "");
    EXPECT_EQ(resolved_conninfo.count("hostaddr"), 0u);
}

TEST_F(test_resolve, erased_host)
{
    conninfo.erase("host");
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "");
    EXPECT_EQ(resolved_conninfo.count("hostaddr"), 0u);
}

TEST_F(test_resolve, resolve_ipv4_error)
{
    set_error_on_resolve_ipv4("localhost", boost::asio::error::connection_refused);
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_TRUE(err);
}

TEST_F(test_resolve, resolve_ipv6_error)
{
    set_error_on_resolve_ipv6("localhost", boost::asio::error::connection_refused);
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_TRUE(err);
}

TEST_F(test_resolve, resolve_error_try_again)
{
    set_error_on_resolve_ipv4("localhost", boost::asio::error::host_not_found_try_again);
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_TRUE(err);
}

TEST_F(test_resolve, unknown_host)
{
    conninfo["host"] = "unknown_host";
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "unknown_host");
    EXPECT_EQ(resolved_conninfo.count("hostaddr"), 0u);
}

TEST_F(test_resolve, resolve_ipv4)
{
    add_ipv4_addr("localhost", "127.0.0.1");
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "localhost");
    EXPECT_EQ(resolved_conninfo["hostaddr"], "127.0.0.1");
}

TEST_F(test_resolve, resolve_ipv6)
{
    add_ipv6_addr("localhost", "::1");
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "localhost");
    EXPECT_EQ(resolved_conninfo["hostaddr"], "::1");
}

TEST_F(test_resolve, resolve_ipv6_ipv4)
{
    add_ipv6_addr("localhost", "::1");
    add_ipv4_addr("localhost", "127.0.0.1");
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "localhost,localhost");
    EXPECT_EQ(resolved_conninfo["hostaddr"], "::1,127.0.0.1");
}

TEST_F(test_resolve, already_resolved)
{
    add_ipv4_addr("localhost", "127.0.0.1");
    add_ipv6_addr("localhost", "::1");
    conninfo["hostaddr"] = "::1";
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "localhost");
    EXPECT_EQ(resolved_conninfo["hostaddr"], "::1");
}

TEST_F(test_resolve, resolve_ipv6_only)
{
    add_ipv6_addr("localhost", "::1");
    add_ipv4_addr("localhost", "127.0.0.1");
    set_ipv6_only();
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "localhost");
    EXPECT_EQ(resolved_conninfo["hostaddr"], "::1");
}

TEST_F(test_resolve, resolve_multihost_error)
{
    add_ipv6_addr("localhost", "::1");
    add_ipv4_addr("localhost", "127.0.0.1");
    set_error_on_resolve_ipv6("localhost2", boost::asio::error::connection_refused);
    conninfo["host"] = "localhost,localhost2";
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_TRUE(err);
}

TEST_F(test_resolve, resolve_multihost)
{
    add_ipv6_addr("localhost", "::1");
    add_ipv4_addr("localhost", "127.0.0.1");
    add_ipv4_addr("localhost2", "127.0.0.2");
    conninfo["host"] = "localhost,localhost2";
    auto [err, resolved_conninfo] = async_resolve();
    EXPECT_FALSE(err) << err.code().message() << err.message();
    EXPECT_EQ(resolved_conninfo["host"], "localhost,localhost,localhost2");
    EXPECT_EQ(resolved_conninfo["hostaddr"], "::1,127.0.0.1,127.0.0.2");
}