#pragma once

#include <libpq-fe.h>

#include <apq/detail/connect_op.hpp>

#include <gmock/gmock.h>

#include <boost/asio.hpp>
#include <boost/system/error_code.hpp>

#include <tuple>
#include <memory>

namespace apq::test {

using namespace testing;

struct connection_impl_mock
{
    connection_impl_mock(std::shared_ptr<boost::asio::io_context> io_)
        : io(io_), strand_(std::make_shared<boost::asio::io_context::strand>(*io_))
    {
    }

    MOCK_METHOD(void, start_connect, (const apq::detail::connection_info&), ());
    MOCK_METHOD(ConnStatusType, get_status, (), ());
    MOCK_METHOD(const char*, get_error_message, (), ());
    MOCK_METHOD(PostgresPollingStatusType, connection_poll, (), ());
    MOCK_METHOD(bool, is_connected, (), ());
    MOCK_METHOD((std::tuple<boost::system::error_code, std::string>), refresh_socket, (), ());
    MOCK_METHOD((std::tuple<boost::system::error_code, std::string>), assign_socket, (), ());
    MOCK_METHOD(void, init_multihost, (), ());
    MOCK_METHOD(boost::system::error_code, write_poll, (), ());
    MOCK_METHOD(boost::system::error_code, read_poll, (), ());
    MOCK_METHOD(void, shutdown, (), ());

    std::shared_ptr<boost::asio::io_context> io;
    std::shared_ptr<boost::asio::io_context::strand> strand_;
    bool is_multihost_ = false;
};

template <typename Handler>
inline void write_poll(const boost::shared_ptr<StrictMock<connection_impl_mock>>& impl, Handler&& h)
{
    ;
    boost::asio::dispatch(std::bind(std::forward<Handler>(h), impl->write_poll()));
}

template <typename Handler>
inline void read_poll(const boost::shared_ptr<StrictMock<connection_impl_mock>>& impl, Handler&& h)
{
    ;
    boost::asio::dispatch(std::bind(std::forward<Handler>(h), impl->read_poll()));
}

inline boost::asio::io_service& get_io_service(
    const boost::shared_ptr<StrictMock<connection_impl_mock>>& impl)
{
    return *impl->io;
}

template <typename Impl>
void shutdown(Impl&& impl)
{
    impl->shutdown();
}

template <typename Handler>
auto get_connect_op(boost::shared_ptr<StrictMock<connection_impl_mock>>& impl, Handler&& handler)
{
    return ::apq::detail::connect_op<std::decay_t<Handler>, StrictMock<connection_impl_mock>>(
        impl, std::forward<Handler>(handler));
}

template <typename Handler>
apq::result call_async_connect_perform(
    boost::shared_ptr<StrictMock<connection_impl_mock>>& impl,
    const apq::detail::connection_info& conninfo,
    Handler handler)
{
    boost::asio::async_completion<Handler, void(apq::result)> init(handler);
    get_connect_op(impl, init.completion_handler).perform(conninfo);
    return init.result.get();
}

template <typename Handler>
apq::result call_async_connection_poll(
    boost::shared_ptr<StrictMock<connection_impl_mock>>& impl,
    Handler handler)
{
    boost::asio::async_completion<Handler, void(apq::result)> init(handler);
    get_connect_op(impl, init.completion_handler)(boost::system::error_code{});
    return init.result.get();
}

}
