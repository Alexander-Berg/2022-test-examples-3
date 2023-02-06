#include "connection_provider_mock.hpp"

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

#include <src/logic/interface/types/list_contacts_counter.hpp>
#include <src/logic/interface/types/reflection/list_contacts_counter.hpp>
#include <src/logic/db/get_shared_contacts_count_from_list_impl.hpp>

namespace {

using namespace testing;
using collie::error_code;
using collie::logic::ListContactsCounter;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::SubscribeList;
using collie::services::db::contacts::query::GetContactsCount;
using collie::services::db::contacts::query::GetContactsWithEmailsCount;
using collie::services::db::contacts::query::GetSubscribedList;
using collie::logic::Error;
using collie::logic::db::contacts::SubscribeListOpt;
using collie::services::db::PassportUserId;
using collie::services::db::OrgUserId;
using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
        GetContactsCount,
        GetContactsWithEmailsCount,
        GetSubscribedList>;
using ConnectionProvider = collie::tests::ConnectionProvider<
        GetContactsCount,
        GetContactsWithEmailsCount,
        GetSubscribedList>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
        GetContactsCount,
        GetContactsWithEmailsCount,
        GetSubscribedList>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
        GetContactsCount,
        GetContactsWithEmailsCount,
        GetSubscribedList>;
using CountFromListRows = std::vector<GetContactsCount::result_type>;
using collie::make_unexpected;
using GetSharedContactsCountFromListImpl = collie::logic::db::GetSharedContactsCountFromListImpl<MakeConnectionProvider>;

struct TestLogicDbGetSharedContactsCountFromList : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const GetSharedContactsCountFromListImpl getSharedContactsCountFromList {makeProvider};
    const std::int64_t uid{42};
    const ConstUserType userType{"passport_user"};
    const std::int64_t listId{1};
    const std::int64_t counter{16};

    SubscribeList makeOrgSubscribeList() {
        return {1, 1, "connect_organization", 1};
    }

    SubscribeList makePassportUserSubscribeList() {
        return {1, 1, "passport_user", 1};
    }
};

TEST_F(TestLogicDbGetSharedContactsCountFromList, for_text_uid_should_return_userNotFound) {
    withSpawn([&] (const auto& context) {
        const auto result = getSharedContactsCountFromList(context, "TEXT", listId, {});
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), error_code{Error::userNotFound});
    });
}

TEST_F(TestLogicDbGetSharedContactsCountFromList, for_GetSubscribedList_request_which_ended_with_error_should_return_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedList {uid, userType, listId})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = getSharedContactsCountFromList(context, std::to_string(uid), listId, {});
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedContactsCountFromList, for_GetSubscribedList_request_which_returned_empty_result_should_return_badRequest) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedList {uid, userType, listId})
        ).WillOnce(Return(std::vector<SubscribeList>{}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result = getSharedContactsCountFromList(context, std::to_string(uid), listId, {});
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedContactsCountFromList, for_GetContactsCountFromList_request_which_ended_with_error_should_return_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {uid})).WillOnce(Return(provider));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        auto subscribeList = makeOrgSubscribeList();
        EXPECT_CALL(providerMock, request(
            GetSubscribedList {uid, userType, listId})
        ).WillOnce(Return(std::vector<SubscribeList>{subscribeList}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(makeProviderMock, call(context, OrgUserId {subscribeList.owner_user_id})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(subscribeList.owner_user_id));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(ConstUserType{subscribeList.owner_user_type}));
        EXPECT_CALL(providerMock, request(
            GetContactsCount{
                subscribeList.owner_user_id,
                ConstUserType{subscribeList.owner_user_type},
                {subscribeList.owner_list_id}})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = getSharedContactsCountFromList(context, std::to_string(uid), listId, {});
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedContactsCountFromList, for_ConnectOrganiation_type_user_from_shared_list_should_return_counter) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {uid})).WillOnce(Return(provider));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        auto subscribeList = makeOrgSubscribeList();
        EXPECT_CALL(providerMock, request(
            GetSubscribedList {uid, userType, listId})
        ).WillOnce(Return(std::vector<SubscribeList>{subscribeList}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(makeProviderMock, call(context, OrgUserId {subscribeList.owner_user_id})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(subscribeList.owner_user_id));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(ConstUserType{subscribeList.owner_user_type}));
        EXPECT_CALL(providerMock, request(
            GetContactsCount{
                subscribeList.owner_user_id,
                ConstUserType{subscribeList.owner_user_type},
                {subscribeList.owner_list_id}})
        ).WillOnce(Return(CountFromListRows{counter}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result = getSharedContactsCountFromList(context, std::to_string(uid), listId, {});
        ASSERT_TRUE(result);
        EXPECT_EQ(result.value().count, counter);
    });
}

TEST_F(TestLogicDbGetSharedContactsCountFromList, for_PassportUser_type_user_from_shared_list_should_return_counter) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {uid})).WillOnce(Return(provider));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        auto subscribeList = makePassportUserSubscribeList();
        EXPECT_CALL(providerMock, request(
            GetSubscribedList {uid, userType, listId})
        ).WillOnce(Return(std::vector<SubscribeList>{subscribeList}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {subscribeList.owner_user_id})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(subscribeList.owner_user_id));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(ConstUserType{subscribeList.owner_user_type}));
        EXPECT_CALL(providerMock, request(
            GetContactsWithEmailsCount{
                subscribeList.owner_user_id,
                ConstUserType{subscribeList.owner_user_type},
                {subscribeList.owner_list_id}})
        ).WillOnce(Return(CountFromListRows{counter}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto sharedWithEmails{true};
        const auto result = getSharedContactsCountFromList(context, std::to_string(uid), listId,
                sharedWithEmails);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.value().count, counter);
    });
}

} // namespace
