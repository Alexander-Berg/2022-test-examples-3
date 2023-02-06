#include "connection_provider_mock.hpp"

#include <src/services/db/execute.hpp>

#include <tests/unit/error_category.hpp>
#include <tests/unit/test_with_task_context.hpp>

namespace {

using collie::error_code;
using collie::services::db::retry;
using collie::tests::ConnectionProvider;
using collie::tests::ConnectionProviderMock;
using collie::tests::Error;
using collie::tests::FakeQuery;
using collie::tests::QueryRepository;
using collie::tests::QueryRepositoryMock;

struct TestServicesDbExecute : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<const QueryRepositoryMock<FakeQuery>> queryRepositoryMock;
    QueryRepository<FakeQuery> queryRepository{&queryRepositoryMock};
    const ozo::time_traits::duration timeout{1};
};

TEST_F(TestServicesDbExecute, must_make_query_from_query_repository) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(providerMock, queryRepository()).WillOnce(Return(queryRepository));
        EXPECT_CALL(queryRepositoryMock, make_query(FakeQuery{})).WillOnce(Return(FakeQuery{}));
        EXPECT_CALL(providerMock, maxRetriesNumber()).WillOnce(Return(1));
        EXPECT_CALL(providerMock, requestTimeout()).WillOnce(Return(timeout));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, execute(FakeQuery{}, timeout)).WillOnce(Return(ozo::error_code{}));
        const auto result{collie::services::db::execute(retry(provider), FakeQuery{})};
        EXPECT_TRUE(result);
    });
}

TEST_F(TestServicesDbExecute, must_return_error_on_execute_error) {
    withSpawn([&](const auto& context) {
        const auto test{[&](auto expectedError, auto innerError) {
            const InSequence sequence;
            EXPECT_CALL(providerMock, queryRepository()).WillOnce(Return(queryRepository));
            EXPECT_CALL(queryRepositoryMock, make_query(FakeQuery{})).WillOnce(Return(FakeQuery{}));
            EXPECT_CALL(providerMock, maxRetriesNumber()).WillOnce(Return(0));
            EXPECT_CALL(providerMock, requestTimeout()).WillOnce(Return(timeout));
            EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
            EXPECT_CALL(providerMock, execute(_, timeout)).WillOnce(Return(ozo::error_code{innerError}));
            const auto result{collie::services::db::execute(retry(provider), FakeQuery{})};
            EXPECT_FALSE(result);
            EXPECT_EQ(error_code{expectedError}, result.error());
        }};

        test(collie::services::db::Error::databaseError, Error::fail);
        test(::sharpei::client::Errors::UidNotFound, ::sharpei::client::Errors::UidNotFound);
        test(collie::logic::Error::userNotFound, collie::logic::Error::userNotFound);
    });
}

}
