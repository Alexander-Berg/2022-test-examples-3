#include "connection_provider_mock.hpp"

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

#include <src/logic/db/get_changes_impl.hpp>

#include <boost/range/algorithm/equal.hpp>

namespace {

using namespace testing;

using collie::error_code;
using collie::logic::Change;
using collie::services::db::contacts::ChangeType;
using collie::services::db::PassportUserId;
using collie::services::db::contacts::query::GetChanges;
using collie::logic::Error;
using collie::logic::makeChangeType;
using collie::logic::Uid;
using collie::TaskContextPtr;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<GetChanges>;
using ConnectionProvider = collie::tests::ConnectionProvider<GetChanges>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<GetChanges>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<GetChanges>;

using ChangeRows = std::vector<GetChanges::result_type>;
using GetChangesImpl = collie::logic::db::GetChangesImpl<MakeConnectionProvider>;

struct TestLogicDbGetChanges : public TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const GetChangesImpl getChanges {makeProvider};

    const ChangeRows changeRows {
        {0,  0,  ChangeType{"abook"}},
        {1,  1,  ChangeType{"create_user"}},
        {2,  2,  ChangeType{"delete_user"}},
        {3,  3,  ChangeType{"create_list"}},
        {4,  4,  ChangeType{"delete_list"}},
        {5,  5,  ChangeType{"update_list"}},
        {6,  6,  ChangeType{"share_list"}},
        {7,  7,  ChangeType{"revoke_list"}},
        {8,  8,  ChangeType{"create_contacts"}},
        {9,  9,  ChangeType{"delete_contacts"}},
        {10, 10, ChangeType{"update_contacts"}},
        {11, 11, ChangeType{"create_tag"}},
        {12, 12, ChangeType{"delete_tag"}},
        {13, 13, ChangeType{"update_tag"}},
        {14, 14, ChangeType{"tag_contacts"}},
        {15, 15, ChangeType{"untag_contacts"}},
        {16, 16, ChangeType{"create_emails"}},
        {17, 17, ChangeType{"delete_emails"}},
        {18, 18, ChangeType{"update_emails"}},
        {19, 19, ChangeType{"tag_emails"}},
        {20, 20, ChangeType{"untag_emails"}},
        {21, 21, ChangeType{"copy_abook"}},
        {22, 22, ChangeType{"create_directory_entities"}},
        {23, 23, ChangeType{"delete_directory_entities"}},
        {24, 24, ChangeType{"subscribe_to_list"}},
        {25, 25, ChangeType{"revoke_subscribed_list"}}
    };
};

TEST_F(TestLogicDbGetChanges, for_text_uid_should_return_userNotFound) {
    withSpawn([&] (const auto& context) {
        const auto result = getChanges(context, "TEXT");

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), error_code{Error::userNotFound});
    });
}

TEST_F(TestLogicDbGetChanges, for_existent_user_should_return_changes) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, request(GetChanges{42})).WillOnce(Return(changeRows));

        const auto result = getChanges(context, "42");

        ASSERT_TRUE(result);

        EXPECT_TRUE(boost::equal(changeRows, result.value().changes,
            [] (const auto& expected, const auto& actual) {
                return std::make_tuple(expected.revision, expected.change_date, makeChangeType(expected.type.get()))
                    == std::make_tuple(actual.revision, actual.time, actual.type);
            }));
    });
}

}
