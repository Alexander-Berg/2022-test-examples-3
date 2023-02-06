#include "connection_provider_mock.hpp"

#include <src/logic/db/get_shared_contacts_from_list_impl.hpp>
#include <src/logic/interface/types/org_id.hpp>

#include <tests/unit/generic_operators.hpp>
#include <tests/unit/logic/interface/types/existing_contacts.hpp>
#include <tests/unit/test_with_task_context.hpp>

namespace {

using collie::error_code;
using collie::logic::ContactId;
using collie::logic::Error;
using collie::logic::ExistingContact;
using collie::logic::ExistingContacts;
using collie::logic::ListId;
using collie::logic::OrgId;
using collie::logic::Revision;
using collie::logic::TagId;
using collie::make_unexpected;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::query::GetContacts;
using collie::services::db::contacts::query::GetContactsWithEmails;
using collie::services::db::contacts::query::GetSubscribedList;
using collie::services::db::contacts::SubscribeList;
using collie::services::db::OrgUserId;
using collie::services::db::PassportUserId;
using collie::TaskContextPtr;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
        GetContacts,
        GetContactsWithEmails,
        GetSubscribedList>;
using ConnectionProvider = collie::tests::ConnectionProvider<
        GetContacts,
        GetContactsWithEmails,
        GetSubscribedList>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
        GetContacts,
        GetContactsWithEmails,
        GetSubscribedList>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
        GetContacts,
        GetContactsWithEmails,
        GetSubscribedList>;

using ContactRows = std::vector<GetContacts::result_type>;
using GetSharedContactsFromListImpl = collie::logic::db::GetSharedContactsFromListImpl<
        MakeConnectionProvider>;

struct TestLogicDbGetSharedContactsFromList : TestWithTaskContext {
    SubscribeList makeSubscribeList() const {
        const std::string ownerUserType{"connect_organization"};
        return {listId, uid + 1, ownerUserType, listId - 1};
    }

    void prepareBaseExpectations(const TaskContextPtr& context) const {
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(passportUserType));
    }

    SubscribeList prepareExpectations(const TaskContextPtr& context) const {
        prepareBaseExpectations(context);
        const auto subscribeList{makeSubscribeList()};
        EXPECT_CALL(providerMock, request(GetSubscribedList{uid, passportUserType, listId})).WillOnce(
                Return(std::vector<SubscribeList>{subscribeList}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(makeProviderMock, call(context, OrgUserId{subscribeList.owner_user_id})).WillOnce(
                Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(subscribeList.owner_user_id));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(ConstUserType{subscribeList.owner_user_type}));
        return subscribeList;
    }

    template<typename Query> void testForQueryErrorMustReturnError(bool sharedWithEmails = false) {
        withSpawn([&](const auto& context) {
            const InSequence sequence;
            const auto subscribeList{prepareExpectations(context)};
            EXPECT_CALL(providerMock, request(Query{subscribeList.owner_user_id, ConstUserType{subscribeList.
                    owner_user_type}, {}, {subscribeList.owner_list_id}, {}, {}})).WillOnce(Return(
                            make_unexpected(error_code{Error::badRequest})));
            const auto result{getSharedContactsFromList(context, std::to_string(uid), {}, listId, {}, {},
                    sharedWithEmails)};
            ASSERT_FALSE(result);
            EXPECT_EQ(error_code{Error::badRequest}, result.error());
        });
    }

    ExistingContacts makeExistingContacts() const {
        ExistingContact expectedContact;
        expectedContact.contact_id = contactId;
        expectedContact.list_id = listId - 1;
        expectedContact.revision = revision;
        expectedContact.vcard.names = {{{{"First"}, {"Middle"}, {"Last"}, {}, {}}}};
        expectedContact.vcard.telephone_numbers = {{{"+79031234567"}, {}, {}, {}}, {{"+79657654321"}, {}, {},
                {}}};
        expectedContact.vcard.emails = {{{"local0@domain0.ru"}, {{{"t1"}, {"t2"}}}, {"label0"}},
                {{"local1@domain1.ru"}, {{{"t1"}, {"t3"}}}, {"label1"}}};
        expectedContact.vcard.organizations = {{"company0", "department0", "title0", "summary0",
                {{"type0"}}}};
        expectedContact.vcard.directory_entries = {{orgId, "Name0", 1, {{"type0"}}}};
        expectedContact.tag_ids = {tagId};
        expectedContact.uri = uri;
        expectedContact.emails = {{emailId, "local0@domain0.ru", {{tagId}}}, {emailId + 1,
                "local1@domain1.ru", {{tagId}}}};
        return {{std::move(expectedContact)}};
    }

    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const GetSharedContactsFromListImpl getSharedContactsFromList{makeProvider};
    const std::int64_t uid{42};
    const ConstUserType passportUserType{"passport_user"};
    const ContactId contactId{1};
    const ListId listId{2};
    const Revision revision{3};
    const OrgId orgId{1};
    const std::int64_t emailId{4};
    const TagId tagId{5};
    const std::string uri{"URI"};
    const ContactRows rows{{contactId, listId - 1, revision,
            R"({"names":[{"last":"Last","middle":"Middle","first":"First"}],
               "emails":[{"type":["t1","t2"],"label":"label0","email":"local0@domain0.ru"},
                       {"type":["t1","t3"],"label":"label1","email":"local1@domain1.ru"}],
               "telephone_numbers":[{"telephone_number":"+79031234567"},{"telephone_number":"+79657654321"}],
               "organizations":[{"company":"company0","department":"department0","title":"title0",
                       "summary":"summary0","type":["type0"]}],
               "directory_entries":[{"org_id":1,"org_name":"Name0","entry_id":1,"type":["type0"]}],
               "extra_field": "value"})",
            {tagId},
            uri,
            {{emailId, "local0@domain0.ru", {{tagId}}}, {emailId + 1, "local1@domain1.ru", {{tagId}}}}}};
};

TEST_F(TestLogicDbGetSharedContactsFromList, for_nonexistent_user_must_return_error_usernotfound) {
    withSpawn([&](const auto& context) {
        const std::string nonexistentUid{"uid"};
        const auto result{getSharedContactsFromList(context, nonexistentUid, {}, {}, {}, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedContactsFromList, for_invalid_offset_must_return_error_badrequest) {
    withSpawn([&](const auto& context) {
        const std::string_view invalidOffset{"-1"};
        const auto result{getSharedContactsFromList(context, std::to_string(uid), {}, {}, {invalidOffset}, {},
                {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedContactsFromList, for_invalid_limit_must_return_error_badrequest) {
    withSpawn([&](const auto& context) {
        const std::string_view invalidLimit{"-1"};
        const auto result{getSharedContactsFromList(context, std::to_string(uid), {}, {}, {}, {invalidLimit},
                {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedContactsFromList, for_GetSubscribedList_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseExpectations(context);
        EXPECT_CALL(providerMock, request(GetSubscribedList{uid, passportUserType, listId})).WillOnce(Return(
                make_unexpected(error_code{Error::badRequest})));
        const auto result{getSharedContactsFromList(context, std::to_string(uid), {}, listId, {}, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedContactsFromList, for_GetSubscribedList_empty_result_must_return_badrequest) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseExpectations(context);
        EXPECT_CALL(providerMock, request(GetSubscribedList{uid, passportUserType, listId})).WillOnce(Return(
                std::vector<SubscribeList>{}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        const auto result{getSharedContactsFromList(context, std::to_string(uid), {}, listId, {}, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetSharedContactsFromList, for_GetContacts_which_ended_with_error_must_return_error) {
    testForQueryErrorMustReturnError<GetContacts>();
}

TEST_F(TestLogicDbGetSharedContactsFromList,
        for_GetContactsWithEmails_which_ended_with_error_must_return_error) {
    const auto sharedWithEmails{true};
    testForQueryErrorMustReturnError<GetContactsWithEmails>(sharedWithEmails);
}

TEST_F(TestLogicDbGetSharedContactsFromList, for_succeeded_query_must_return_contact_list) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        const auto subscribeList{prepareExpectations(context)};
        const auto offset{2};
        const auto limit{1};
        EXPECT_CALL(providerMock, request(GetContacts{subscribeList.owner_user_id, ConstUserType{
                subscribeList.owner_user_type}, {contactId}, {subscribeList.owner_list_id}, {offset},
                        {limit}})).WillOnce(Return(rows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        const auto result{getSharedContactsFromList(context, std::to_string(uid), {contactId}, listId,
                {std::to_string(offset)}, {std::to_string(limit)}, {})};
        ASSERT_TRUE(result);
        EXPECT_EQ(makeExistingContacts(), result.value());
    });
}

}
