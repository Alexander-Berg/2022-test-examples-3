#include "connection_provider_mock.hpp"

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/shared_contacts_mock.hpp>

#include <src/logic/interface/types/existing_shared_list.hpp>
#include <src/logic/interface/types/reflection/existing_shared_list.hpp>
#include <src/logic/db/get_shared_lists_impl.hpp>
#include <vector>

namespace collie::logic {

static bool operator==(const ExistingSharedList& left, const ExistingSharedList& right) {
    return boost::fusion::operator==(left, right);
}

} // namespace collie::logic

namespace {

using namespace testing;
using collie::error_code;
using collie::logic::ExistingSharedList;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::SubscribeList;
using collie::services::db::contacts::query::GetUserTypeLists;
using collie::services::db::contacts::query::GetSubscribedLists;
using collie::logic::Error;
using collie::services::db::PassportUserId;
using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
    GetSubscribedLists,
    GetUserTypeLists
>;
using ConnectionProvider = collie::tests::ConnectionProvider<
    GetSubscribedLists,
    GetUserTypeLists
>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
    GetSubscribedLists,
    GetUserTypeLists
>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
    GetSubscribedLists,
    GetUserTypeLists
>;
using SharedListRows = std::vector<GetUserTypeLists::result_type>;
using collie::make_unexpected;
using GetSharedListsImpl = collie::logic::db::GetSharedListsImpl<MakeConnectionProvider>;

struct TestLogicDbGetSharedLists : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const GetSharedListsImpl getSharedLists {makeProvider};
    const std::int64_t uid{42};
    const ConstUserType userType{"passport_user"};

    std::vector<SubscribeList> makeSubscribeList() {
        std::vector<SubscribeList> result;
        for (auto i : {1, 2, 3}) {
            result.emplace_back(SubscribeList{i, i, "passport_user", i});
        }
        return result;
    }
};

TEST_F(TestLogicDbGetSharedLists, for_text_uid_should_return_userNotFound) {
    withSpawn([&] (const auto& context) {
        const auto result = getSharedLists(context, "TEXT");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), error_code{Error::userNotFound});
    });
}

TEST_F(TestLogicDbGetSharedLists, for_GetSubscribedLists_request_which_ended_with_error_should_return_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedLists {uid, userType})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));
        const auto result = getSharedLists(context, std::to_string(uid));
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedLists, for_empty_subscribedLists_return_empty_ExistingSharedLists) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedLists {uid, userType})
        ).WillOnce(Return(std::vector<SubscribeList> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        const auto result = getSharedLists(context, std::to_string(uid));
        ASSERT_TRUE(result);
        ASSERT_TRUE(result.value().lists.empty());
    });
}

TEST_F(TestLogicDbGetSharedLists, for_no_empty_subscribedLists_should_return_ExistingSharedLists) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedLists {uid, userType})
        ).WillOnce(Return(makeSubscribeList()));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, request(
            GetUserTypeLists {uid})
        ).WillOnce(Return(SharedListRows{{1, "org_1"}, {2, "org_2"}, {3, "org_3"}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result = getSharedLists(context, std::to_string(uid));
        ASSERT_TRUE(result);
        EXPECT_THAT(result.value().lists, ElementsAre(
            ExistingSharedList{1, "org_1"},
            ExistingSharedList{2, "org_2"},
            ExistingSharedList{3, "org_3"}
        ));
    });
}

} // namespace
