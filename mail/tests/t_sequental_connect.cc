#include "resolver_mock.h"
#include "socket_mock.h"
#include <yplatform/net/sequental_connect.h>
#include <catch.hpp>

using namespace yplatform::net;

struct t_sequental_connect
{
    t_sequental_connect() : socket(io)
    {
        yplatform::log::init_console(io);
        yplatform::log::init_global_log_console();
    }

    void set_resolve_order(
        client_settings::resolve_order_t resolve_order = client_settings::ipv6_ipv4)
    {
        connect_op_ptr = std::make_shared<sequental_connect_op>(socket, resolve_order);
        connect_op_ptr->logger(YGLOBAL_LOGGER);
    }

    void perform_connect(
        const string& hostname,
        const duration& attempt_tm,
        const duration& total_tm)
    {
        if (!connect_op_ptr)
        {
            set_resolve_order();
        }

        auto started_at = clock::now();

        connect_op_ptr->perform(
            hostname, 12345, attempt_tm, total_tm, [this](const error_code& err) { error = err; });
        io.run();

        op_duration = clock::now() - started_at;
    }

    void set_resolve_step_duration(const duration& duration)
    {
        resolver_mock::resolve_step_duration = duration;
    }

    void set_connection_duration(const duration& duration)
    {
        socket.raw_socket().connect_at = clock::now() + duration;
    }

    typedef universal_socket<socket_mock> universal_socket_mock;
    typedef async_sequental_connect_op<universal_socket_mock, resolver_mock> sequental_connect_op;
    typedef std::shared_ptr<sequental_connect_op> sequental_connect_op_ptr;

    boost::asio::io_service io;
    universal_socket_mock socket;
    sequental_connect_op_ptr connect_op_ptr;
    error_code error;
    duration op_duration;
};

TEST_CASE_METHOD(t_sequental_connect, "without_resolving/does_not_cancel_connection")
{
    set_resolve_step_duration(milliseconds(0));
    set_connection_duration(milliseconds(1));
    perform_connect("0.0.0.0", milliseconds(5), milliseconds(7));
    REQUIRE(!error);
    REQUIRE(op_duration <= milliseconds(10));
}

TEST_CASE_METHOD(t_sequental_connect, "without_resolving/does_cancel_connection")
{
    set_resolve_step_duration(milliseconds(0));
    set_connection_duration(milliseconds(1000));
    perform_connect("0.0.0.0", milliseconds(5), milliseconds(7));
    REQUIRE(error == operation_aborted);
    REQUIRE(op_duration <= milliseconds(10));
}

TEST_CASE_METHOD(t_sequental_connect, "with_resolving/does_not_cancel_connection")
{
    set_resolve_step_duration(milliseconds(0));
    set_connection_duration(milliseconds(1));
    perform_connect("connect_ipv4.ru", milliseconds(5), milliseconds(7));
    REQUIRE(!error);
    REQUIRE(op_duration <= milliseconds(10));
}

TEST_CASE_METHOD(t_sequental_connect, "with_resolving/does_cancel_connection")
{
    set_resolve_step_duration(milliseconds(0));
    set_connection_duration(milliseconds(1000));
    perform_connect("cancel_ipv4_ipv6.ru", milliseconds(5), milliseconds(7));
    REQUIRE(error == operation_aborted);
    REQUIRE(op_duration <= milliseconds(10));
}
