#include "connection_mock.hpp"
#include "handler_mock.hpp"
#include "query_repository_mock.hpp"

#include <tests/unit/error_category.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/db/contacts/check_is_user_exists_operation.hpp>

namespace {

using namespace testing;
using namespace collie::tests;

using collie::services::db::contacts::query::IsUserExists;
using collie::services::db::ConstUserType;

using QueryRepositoryMock = collie::tests::QueryRepositoryMock<IsUserExists>;
using QueryRepository = collie::tests::QueryRepository<IsUserExists>;
using ConnectionMock = collie::tests::ConnectionMock<IsUserExists>;
using HandlerMock = collie::tests::HandlerMock<ConnectionMock*>;
using Handler = collie::tests::Handler<ConnectionMock*>;

using CheckIsUserExistsOperation = collie::services::db::CheckIsUserExistsOperation<Handler, QueryRepository>;

struct TestCheckIsUserExistsOperation : Test {
    StrictMock<const QueryRepositoryMock> repository;
    StrictMock<ConnectionMock> connection;
    StrictMock<HandlerMock> handler;
    const std::int64_t uid {42};
    const ConstUserType userType {"passport_user"};
    const ozo::time_traits::duration timeout {13};
    CheckIsUserExistsOperation checkIsUsersExists {
        QueryRepository {&repository},
        uid,
        userType,
        timeout,
        Handler {&handler}
    };
};

TEST_F(TestCheckIsUserExistsOperation, should_callback_with_error_if_called_with_error) {
    EXPECT_CALL(handler, call(ozo::error_code {Error::fail}, _)).WillOnce(Return());;

    checkIsUsersExists(ozo::error_code {Error::fail}, &connection);
}

TEST_F(TestCheckIsUserExistsOperation, should_callback_with_error_if_request_query_failed) {
    const InSequence s;

    EXPECT_CALL(repository, make_query(IsUserExists {uid, userType}))
        .WillOnce(Return(IsUserExists {uid, userType}));
    EXPECT_CALL(connection, request(IsUserExists {uid, userType}, _)).WillOnce(Return(ozo::error_code {Error::fail}));
    EXPECT_CALL(handler, call(ozo::error_code{Error::fail}, _)).WillOnce(Return());

    checkIsUsersExists(ozo::error_code {}, &connection);
}

TEST_F(TestCheckIsUserExistsOperation, should_callback_with_userNotFound_error_if_query_returns_empty_result) {
    const InSequence s;

    EXPECT_CALL(repository, make_query(IsUserExists {uid, userType}))
        .WillOnce(Return(IsUserExists {uid, userType}));
    EXPECT_CALL(connection, request(IsUserExists {uid, userType}, _)).WillOnce(Return(ozo::error_code{}));
    EXPECT_CALL(handler, call(ozo::error_code {collie::logic::Error::userNotFound}, _)).WillOnce(Return());

    checkIsUsersExists(ozo::error_code {}, &connection);
}

TEST_F(TestCheckIsUserExistsOperation, should_callback_with_userNotFound_error_if_query_returns_false) {
    const InSequence s;

    EXPECT_CALL(repository, make_query(IsUserExists {uid, userType}))
        .WillOnce(Return(IsUserExists {uid, userType}));
    EXPECT_CALL(connection, request(IsUserExists {uid, userType}, _)).WillOnce(DoAll(
            SetArgReferee<1>(std::vector<IsUserExists::result_type>{false}),
            Return(ozo::error_code{})
        ));
    EXPECT_CALL(handler, call(ozo::error_code {collie::logic::Error::userNotFound}, _)).WillOnce(Return());

    checkIsUsersExists(ozo::error_code {}, &connection);
}

TEST_F(TestCheckIsUserExistsOperation, should_callback_with_connection_if_query_returns_true) {
    const InSequence s;

    EXPECT_CALL(repository, make_query(IsUserExists {uid, userType}))
        .WillOnce(Return(IsUserExists {uid, userType}));
    EXPECT_CALL(connection, request(IsUserExists {uid, userType}, _)).WillOnce(DoAll(
            SetArgReferee<1>(std::vector<IsUserExists::result_type> {true}),
            Return(ozo::error_code {})
        ));
    EXPECT_CALL(handler, call(ozo::error_code{}, &connection)).WillOnce(Return());

    checkIsUsersExists(ozo::error_code {}, &connection);
}

} // namespace
