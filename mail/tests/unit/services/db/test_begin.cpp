#include "connection_provider_mock.hpp"

#include <tests/unit/error_category.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/db/begin.hpp>

namespace {

using namespace testing;
using namespace collie::tests;

using collie::tests::ConnectionProviderMock;
using collie::tests::ConnectionProvider;
using collie::error_code;
using collie::services::db::retry;

struct TestServicesDbBegin : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    const ozo::time_traits::duration timeout {1};
};

TEST_F(TestServicesDbBegin, should_not_retry_with_no_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(providerMock, maxRetriesNumber()).WillOnce(Return(1));
        EXPECT_CALL(providerMock, requestTimeout()).WillOnce(Return(timeout));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, begin(timeout)).WillOnce(Return(ozo::error_code{}));

        const auto result = collie::services::db::begin(retry(provider));
        EXPECT_TRUE(result);
    });
}

TEST_F(TestServicesDbBegin, should_retry_with_error_no_more_than_retry_count) {
    withSpawn([&] (const auto& context) {
        Sequence s;

        EXPECT_CALL(providerMock, maxRetriesNumber()).InSequence(s).WillOnce(Return(1));
        EXPECT_CALL(providerMock, requestTimeout()).InSequence(s).WillOnce(Return(timeout));
        EXPECT_CALL(providerMock, context()).InSequence(s).WillOnce(Return(context));
        EXPECT_CALL(providerMock, begin(timeout)).InSequence(s).WillOnce(Return(ozo::error_code{Error::fail}));
        EXPECT_CALL(providerMock, requestTimeout()).InSequence(s).WillOnce(Return(timeout));
        EXPECT_CALL(providerMock, context()).InSequence(s).WillOnce(Return(context));
        EXPECT_CALL(providerMock, begin(timeout)).InSequence(s).WillOnce(Return(ozo::error_code{Error::fail}));

        const auto result = collie::services::db::begin(retry(provider));

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), error_code{collie::services::db::Error::databaseError});
    });
}

TEST_F(TestServicesDbBegin, should_stop_retry_with_no_error_answer) {
    withSpawn([&] (const auto& context) {
        Sequence s;

        EXPECT_CALL(providerMock, maxRetriesNumber()).InSequence(s).WillOnce(Return(1));
        EXPECT_CALL(providerMock, requestTimeout()).InSequence(s).WillOnce(Return(timeout));
        EXPECT_CALL(providerMock, context()).InSequence(s).WillOnce(Return(context));
        EXPECT_CALL(providerMock, begin(timeout)).InSequence(s).WillOnce(Return(ozo::error_code{Error::fail}));
        EXPECT_CALL(providerMock, requestTimeout()).InSequence(s).WillOnce(Return(timeout));
        EXPECT_CALL(providerMock, context()).InSequence(s).WillOnce(Return(context));
        EXPECT_CALL(providerMock, begin(timeout)).InSequence(s).WillOnce(Return(ozo::error_code{}));

        const auto result = collie::services::db::begin(retry(provider));
        EXPECT_TRUE(result);
    });
}

TEST_F(TestServicesDbBegin, must_return_error_on_begin_error) {
    withSpawn([&](const auto& context) {
        const auto test{[&](auto expectedError, auto innerError) {
            const InSequence sequence;
            EXPECT_CALL(providerMock, requestTimeout()).WillOnce(Return(timeout));
            EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
            EXPECT_CALL(providerMock, begin(timeout)).WillOnce(Return(ozo::error_code{innerError}));
            const auto result{collie::services::db::begin(provider)};
            EXPECT_FALSE(result);
            EXPECT_EQ(error_code{expectedError}, result.error());
        }};

        test(collie::services::db::Error::databaseError, Error::fail);
        test(::sharpei::client::Errors::UidNotFound, ::sharpei::client::Errors::UidNotFound);
        test(collie::logic::Error::userNotFound, collie::logic::Error::userNotFound);
    });
}

} // namespace
