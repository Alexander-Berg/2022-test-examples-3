#include "connection_provider_mock.hpp"

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

#include <src/logic/db/restore_impl.hpp>

namespace {

using namespace testing;

using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::logic::Error;
using collie::logic::Revision;
using collie::services::db::contacts::query::RestoreContacts;
using collie::TaskContextPtr;
using collie::services::db::PassportUserId;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<RestoreContacts>;
using ConnectionProvider = collie::tests::ConnectionProvider<RestoreContacts>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<RestoreContacts>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<RestoreContacts>;

using RestoreContactsRows = std::vector<RestoreContacts::result_type>;
using RestoreImpl = collie::logic::db::RestoreImpl<MakeConnectionProvider>;

struct TestLogicDbRestore : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const RestoreImpl restore {makeProvider};
};

TEST_F(TestLogicDbRestore, for_text_uid_should_return_user_not_found_error) {
    withSpawn([&] (const auto& context) {
        EXPECT_EQ(restore(context, "TEXT", 333), make_expected_from_error<void>(error_code{Error::userNotFound}));
    });
}

TEST_F(TestLogicDbRestore, for_existent_user_should_restore_contacts) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, request(RestoreContacts {42, 333, "request_id"}))
            .WillOnce(Return(RestoreContactsRows({{Revision {2}}})));

        EXPECT_EQ(restore(context, "42", 333), expected<void>());
    });
}

}
