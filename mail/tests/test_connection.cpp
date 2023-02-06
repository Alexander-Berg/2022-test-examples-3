#include "test_with_spawn.hpp"
#include "connection_mocks.hpp"

#include <apq/error.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/shared_ptr.hpp>

namespace {

using namespace testing;
using namespace apq::test;
using namespace apq::error;

struct TestConnection : public TestWithSpawn
{
    boost::shared_ptr<StrictMock<connection_impl_mock>> mockConnection =
        boost::make_shared<StrictMock<connection_impl_mock>>(io);
    apq::detail::connection_info conninfo = { { "host", "host" },
                                              { "port", "42" },
                                              { "dbname", "dbname" } };
};

TEST_F(TestConnection, for_failed_allocation_empty_connect_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, start_connect(conninfo));
        EXPECT_CALL(*mockConnection, is_connected()).WillOnce(Return(false));
        const auto result = call_async_connect_perform(mockConnection, conninfo, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_bad_connect_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, start_connect(conninfo));
        EXPECT_CALL(*mockConnection, is_connected()).WillOnce(Return(true));
        EXPECT_CALL(*mockConnection, get_status()).WillOnce(Return(CONNECTION_BAD));
        EXPECT_CALL(*mockConnection, get_error_message()).WillOnce(Return("error"));
        const auto result = call_async_connect_perform(mockConnection, conninfo, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_failed_assign_socket_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, start_connect(conninfo));
        EXPECT_CALL(*mockConnection, is_connected()).WillOnce(Return(true));
        EXPECT_CALL(*mockConnection, get_status()).WillOnce(Return(CONNECTION_OK));
        EXPECT_CALL(*mockConnection, init_multihost());
        EXPECT_CALL(*mockConnection, assign_socket())
            .WillOnce(Return(std::make_tuple(pq_errors::network, std::string())));
        const auto result = call_async_connect_perform(mockConnection, conninfo, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_throw_exception_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, start_connect(conninfo)).WillOnce(Throw(std::exception()));
        const auto result = call_async_connect_perform(mockConnection, conninfo, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_failed_write_poll_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, start_connect(conninfo));
        EXPECT_CALL(*mockConnection, is_connected()).WillOnce(Return(true));
        EXPECT_CALL(*mockConnection, get_status()).WillOnce(Return(CONNECTION_OK));
        EXPECT_CALL(*mockConnection, init_multihost());
        EXPECT_CALL(*mockConnection, assign_socket())
            .WillOnce(Return(std::make_tuple(boost::system::error_code{}, std::string())));
        EXPECT_CALL(*mockConnection, write_poll()).WillOnce(Return(pq_errors::network));
        const auto result = call_async_connect_perform(mockConnection, conninfo, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_failed_connection_poll_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, connection_poll()).WillOnce(Return(PGRES_POLLING_FAILED));
        EXPECT_CALL(*mockConnection, get_error_message()).WillOnce(Return("error"));
        const auto result = call_async_connection_poll(mockConnection, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_connection_poll_throw_exception_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, connection_poll()).WillOnce(Throw(std::exception()));
        EXPECT_CALL(*mockConnection, get_error_message()).WillOnce(Return("error"));
        const auto result = call_async_connection_poll(mockConnection, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(
    TestConnection,
    for_failed_refresh_socket_after_successful_connection_poll_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, connection_poll()).WillOnce(Return(PGRES_POLLING_OK));
        EXPECT_CALL(*mockConnection, refresh_socket())
            .WillOnce(Return(std::make_tuple(pq_errors::network, std::string())));
        const auto result = call_async_connection_poll(mockConnection, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_successful_connection_poll_should_no_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, connection_poll()).WillOnce(Return(PGRES_POLLING_OK));
        EXPECT_CALL(*mockConnection, refresh_socket())
            .WillOnce(Return(std::make_tuple(boost::system::error_code{}, std::string())));
        const auto result = call_async_connection_poll(mockConnection, yield);
        ASSERT_FALSE(result);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_failed_read_poll_after_successful_connection_poll_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, connection_poll()).WillOnce(Return(PGRES_POLLING_READING));
        EXPECT_CALL(*mockConnection, refresh_socket())
            .WillOnce(Return(std::make_tuple(boost::system::error_code{}, std::string())));
        EXPECT_CALL(*mockConnection, read_poll()).WillOnce(Return(pq_errors::network));
        const auto result = call_async_connection_poll(mockConnection, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_failed_write_poll_after_successful_connection_poll_should_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        EXPECT_CALL(*mockConnection, connection_poll()).WillOnce(Return(PGRES_POLLING_WRITING));
        EXPECT_CALL(*mockConnection, refresh_socket())
            .WillOnce(Return(std::make_tuple(boost::system::error_code{}, std::string())));
        EXPECT_CALL(*mockConnection, write_poll()).WillOnce(Return(pq_errors::network));
        const auto result = call_async_connection_poll(mockConnection, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

TEST_F(TestConnection, for_failed_multihost_connection_should_call_shutdown_and_return_error)
{
    withSpawn([this](boost::asio::yield_context yield) {
        const InSequence s;
        mockConnection->is_multihost_ = true;
        EXPECT_CALL(*mockConnection, connection_poll()).WillOnce(Return(PGRES_POLLING_WRITING));
        EXPECT_CALL(*mockConnection, refresh_socket())
            .WillOnce(Return(std::make_tuple(boost::system::error_code{}, std::string())));
        EXPECT_CALL(*mockConnection, write_poll())
            .WillOnce(Return(boost::asio::error::operation_aborted));
        EXPECT_CALL(*mockConnection, shutdown());
        EXPECT_CALL(*mockConnection, connection_poll()).WillOnce(Return(PGRES_POLLING_FAILED));
        EXPECT_CALL(*mockConnection, get_error_message()).WillOnce(Return("error"));
        const auto result = call_async_connection_poll(mockConnection, yield);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.code(), pq_errors::network);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(mockConnection.get()));
}

}
