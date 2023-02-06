#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "timer.h" // must be included before all - substitute for src/access_impl/timer.h
#include <src/meta/types.h>
#include "wrap_yield.h"
#include <src/access_impl/retry.h>

using namespace ::testing;

namespace doberman {
namespace testing {

struct DummyMock {
    MOCK_METHOD(void, foo, (Yield), (const));
    MOCK_METHOD(void, log, (), (const));
};

struct RetryRuleMock {
    MOCK_METHOD(bool, retriable, (error_code), (const));
};

struct RetryTest : public Test {
    StrictMock<RetryRuleMock> rule;
    StrictMock<DummyMock> dummy;

    auto retry(std::size_t retries) const {
        return access_impl::makeRetry(retries, Seconds(0), [&](auto& ec){ return rule.retriable(ec); },
                [&](auto){ dummy.log(); });
    }

    auto ThrowSystemError() const {
        return Invoke([](Yield){ throw mail_errors::system_error(error_code{{1, boost::system::system_category()}, {}}); });
    }

    auto callFoo() const {
        return [&](Yield a) { dummy.foo(a); };
    }
};

TEST_F(RetryTest, shouldNotRetry_ifNoError) {
    EXPECT_CALL(dummy, foo(_))
            .Times(1);

    retry(5)(callFoo(), Yield());
}

TEST_F(RetryTest, shouldNotRetry_andThrowException_ifNotRetriable_andWasError) {
    EXPECT_CALL(dummy, foo(_))
            .WillOnce(ThrowSystemError());
    EXPECT_CALL(rule, retriable(_))
            .WillOnce(Return(false));

    EXPECT_THROW(retry(5)(callFoo(), Yield()), boost::system::system_error);
}

TEST_F(RetryTest, shouldNotRetry_andThrowException_ifZeroRetries) {
    EXPECT_CALL(dummy, foo(_))
            .WillOnce(ThrowSystemError());

    EXPECT_THROW(retry(0)(callFoo(), Yield()), boost::system::system_error);
}

TEST_F(RetryTest, shouldRetryAndStop_whenNoError) {
    const std::size_t retries = 5;

    EXPECT_CALL(rule, retriable(_))
            .Times(static_cast<int>(retries - 1))
            .WillRepeatedly(Return(true));
    EXPECT_CALL(dummy, log())
            .Times(static_cast<int>(retries - 1));
    InSequence seq;
    EXPECT_CALL(dummy, foo(_))
            .Times(static_cast<int>(retries - 1))
            .WillRepeatedly(ThrowSystemError());
    EXPECT_CALL(dummy, foo(_))
            .Times(1);

    retry(retries)(callFoo(), Yield());
}

TEST_F(RetryTest, shouldRetryAndStop_andThrowException_whenNotRetriableError) {
    const std::size_t retries = 5;

    EXPECT_CALL(dummy, foo(_))
            .Times(static_cast<int>(retries))
            .WillRepeatedly(ThrowSystemError());
    EXPECT_CALL(dummy, log())
            .Times(static_cast<int>(retries - 1));
    InSequence seq;
    EXPECT_CALL(rule, retriable(_))
            .Times(static_cast<int>(retries - 1))
            .WillRepeatedly(Return(true));
    EXPECT_CALL(rule, retriable(_))
            .WillOnce(Return(false));

    EXPECT_THROW(retry(retries)(callFoo(), Yield()), boost::system::system_error);
}

TEST_F(RetryTest, shouldRetryAndStop_andThrowException_whenTriesExpired) {
    const std::size_t retries = 5;

    EXPECT_CALL(dummy, foo(_))
            .Times(static_cast<int>(retries + 1))
            .WillRepeatedly(ThrowSystemError());
    EXPECT_CALL(rule, retriable(_))
            .WillRepeatedly(Return(true));
    EXPECT_CALL(dummy, log())
            .Times(static_cast<int>(retries));

    EXPECT_THROW(retry(retries)(callFoo(), Yield()), boost::system::system_error);
}



TEST(ErrorCodeRuleTest, isCommunicationError_returnTrue_onExpectedErrorCodes) {
    namespace baerr = boost::asio::error;
    std::vector<boost::system::error_code> errors = {
        baerr::make_error_code(baerr::broken_pipe),
        baerr::make_error_code(baerr::connection_aborted),
        baerr::make_error_code(baerr::connection_refused),
        baerr::make_error_code(baerr::connection_reset),
        baerr::make_error_code(baerr::fault),
        baerr::make_error_code(baerr::host_unreachable),
        baerr::make_error_code(baerr::interrupted),
        baerr::make_error_code(baerr::network_reset),
        baerr::make_error_code(baerr::not_connected),
        baerr::make_error_code(baerr::operation_aborted),
        baerr::make_error_code(baerr::shut_down),
        baerr::make_error_code(baerr::timed_out),
        baerr::make_error_code(baerr::try_again),
        { 0, baerr::get_netdb_category() },
        { 0, macs::pg::error::getConnectionCategory() },
        apq::error::make_error_code(apq::error::request_queue_timed_out),
        apq::error::make_error_code(apq::error::network),
        boost::system::errc::make_error_code(boost::system::errc::io_error)
    };

    for (const auto& ec: errors) {
        EXPECT_TRUE(ec == access_impl::errc::communication_error);
    }
}

TEST(ErrorCodeRuleTest, isCommunicationError_returnFalse_onUnexpectedErrorCodes) {
    std::vector<boost::system::error_code> errors = {
        boost::asio::error::make_error_code(boost::asio::error::access_denied),
        { 0, boost::asio::error::get_addrinfo_category() },
        { 0, macs::pg::error::getSqlCategory() },
        apq::error::make_error_code(apq::error::unknown),
        boost::system::errc::make_error_code(boost::system::errc::broken_pipe)
    };

    for (const auto& ec: errors) {
        EXPECT_FALSE(ec == access_impl::errc::communication_error);
    }
}

TEST(ErrorCodeRuleTest, isReadOnlyError_returnTrue_onExpectedErrorCodes) {
    boost::system::error_code ec { 0, macs::pg::error::getReadonlyCategory() };
    EXPECT_TRUE(ec == access_impl::errc::readonly_error);
}

TEST(ErrorCodeRuleTest, isReadOnlyError_returnFalse_onUnexpectedErrorCodes) {
    std::vector<boost::system::error_code> errors = {
        boost::asio::error::make_error_code(boost::asio::error::access_denied),
        { 0, boost::asio::error::get_addrinfo_category() },
        { 0, macs::pg::error::getSqlCategory() },
        apq::error::make_error_code(apq::error::unknown),
        boost::system::errc::make_error_code(boost::system::errc::broken_pipe)
    };

    for (const auto& ec: errors) {
        EXPECT_FALSE(ec == access_impl::errc::readonly_error);
    }
}

TEST(ErrorCodeRuleTest, isEndpointError_returnTrue_onExpectedErrorCodes) {
    boost::system::error_code ec = macs::pg::error::make_error_code(
                macs::pg::error::noEndpointForRole);
    EXPECT_TRUE(ec == access_impl::errc::endpoint_error);
}

TEST(ErrorCodeRuleTest, isEndpointError_returnFalse_onUnxpectedErrorCodes) {
    std::vector<boost::system::error_code> errors = {
        boost::asio::error::make_error_code(boost::asio::error::access_denied),
        { 0, boost::asio::error::get_addrinfo_category() },
        { 0, macs::pg::error::getSqlCategory() },
        apq::error::make_error_code(apq::error::unknown),
        boost::system::errc::make_error_code(boost::system::errc::broken_pipe)
    };

    for (const auto& ec: errors) {
        EXPECT_FALSE(ec == access_impl::errc::endpoint_error);
    }
}

} // namespace testing
} // namespace doberman

