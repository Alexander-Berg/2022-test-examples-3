#include "connection_provider_mock.hpp"

#include <src/logic/db/get_contacts_impl.hpp>
#include <src/logic/interface/types/org_id.hpp>

#include <tests/unit/logic/interface/types/existing_contacts.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/shared_contacts_mock.hpp>

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
using collie::services::db::PassportUserId;
using collie::services::db::ConstUserType;
using collie::make_unexpected;

using collie::services::db::contacts::query::GetContacts;
using collie::services::db::contacts::query::GetSharedContacts;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
    GetContacts,
    GetSharedContacts
>;
using ConnectionProvider = collie::tests::ConnectionProvider<
    GetContacts,
    GetSharedContacts
>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
    GetContacts,
    GetSharedContacts
>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
    GetContacts,
    GetSharedContacts
>;

using ContactRows = std::vector<GetContacts::result_type>;
using GetContactsImpl = collie::logic::db::GetContactsImpl<MakeConnectionProvider>;

struct TestLogicDbGetContacts : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const GetContactsImpl getContacts{makeProvider};
    const std::int64_t uid{42};
    const ConstUserType userType{"passport_user"};
    const ContactId contactId{1};
    const ListId listId{2};
    const Revision revision{3};
    const OrgId orgId{1};
    const std::int64_t emailId{4};
    const TagId tagId{5};
    const std::string uri{"URI"};
    const ContactRows rows{{contactId, listId, revision,
        R"({"names":[{"last":"Last","middle":"Middle","first":"First"}],
            "emails":[{"type":["t1","t2"],"label":"label0","email":"local0@domain0.ru"},
                    {"type":["t1","t3"],"label":"label1","email":"local1@domain1.ru"}],
            "telephone_numbers":[{"telephone_number":"+79031234567"},{"telephone_number":"+79657654321"}],
            "organizations":[{"company":"company0","department":"department0","title":"title0","summary":"summary0","type":["type0"]}],
            "directory_entries":[{"org_id":1,"org_name":"Name0","entry_id":1,"type":["type0"]}],
            "extra_field": "value"})",
        {tagId},
        uri,
        {{emailId, "local0@domain0.ru", {{tagId}}}, {emailId + 1, "local1@domain1.ru", {{tagId}}}}
    }};

    const ContactRows sharedContacts{{2, 1, 2,
        R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})",
        {6},
        "kitty.vcf",
        {{1, "hello@kitty.cat", {{1}}}}
    }};

    ContactRows makeContactRowResultWithEmptyEmails(ListId id) const {
        ContactRows result;
        result.resize(3);

        result[0].contact_id = contactId;
        result[0].list_id = id;
        result[0].revision = revision;
        result[0].vcard = "{}";
        result[0].tag_ids = {1,2,3};
        result[0].uri = "uri";
        result[0].emails = {{1, "hello@kitty.cat", {{1}}}};

        result[1].contact_id = contactId + 1;
        result[1].list_id = id;
        result[1].revision = revision;
        result[1].vcard = "{}";
        result[1].tag_ids = {1,2,3};
        result[1].uri = "uri";
        result[1].emails = {{1, "", {{1}}}};

        result[2].contact_id = contactId + 2;
        result[2].list_id = id;
        result[2].revision = revision;
        result[2].vcard = "{}";
        result[2].tag_ids = {1,2,3};
        result[2].uri = "uri";
        result[2].emails = {
            {1, "hello@kitty.cat", {{1}}},
            {2, "", {{2}}},
        };

        return result;
    }
};

TEST_F(TestLogicDbGetContacts, for_nonexistent_user_must_return_error_user_not_found) {
    withSpawn([&] (const auto& context) {
        const std::string nonexistentUid{"uid"};
        const auto result{getContacts(context, nonexistentUid, {}, {}, {}, {}, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbGetContacts, for_invalid_offset_must_return_error_bad_request) {
    withSpawn([&] (const auto& context) {
        const auto result{getContacts(context, std::to_string(uid), {}, {}, {"Offset"}, {}, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetContacts, for_invalid_limit_must_return_error_bad_request) {
    withSpawn([&] (const auto& context) {
        const auto result{getContacts(context, std::to_string(uid), {}, {}, {}, {"Limit"}, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetContacts, for_existent_user_must_return_nonempty_contact_list) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContacts{uid, userType, {contactId}, {}, {}, {}})).WillOnce(
                Return(rows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getContacts(context, std::to_string(uid), {contactId}, {}, {}, {}, {}, {})};
        ASSERT_TRUE(result);

        ExistingContact expectedContact;
        expectedContact.contact_id = contactId;
        expectedContact.list_id = listId;
        expectedContact.revision = revision;
        expectedContact.vcard.names = {{{{"First"}, {"Middle"}, {"Last"}, {}, {}}}};
        expectedContact.vcard.telephone_numbers = {{{"+79031234567"}, {}, {}, {}}, {{"+79657654321"}, {}, {},
                {}}};
        expectedContact.vcard.emails = {{{"local0@domain0.ru"}, {{{"t1"}, {"t2"}}}, {"label0"}},
                {{"local1@domain1.ru"}, {{{"t1"}, {"t3"}}}, {"label1"}}};
        expectedContact.vcard.organizations = {{"company0", "department0", "title0", "summary0", {{"type0"}}}};
        expectedContact.vcard.directory_entries = {{orgId, "Name0", 1, {{"type0"}}}};
        expectedContact.tag_ids = {tagId};
        expectedContact.uri = uri;
        expectedContact.emails = {{emailId, "local0@domain0.ru", {{tagId}}}, {emailId + 1,
                "local1@domain1.ru", {{tagId}}}};
        EXPECT_EQ(ExistingContacts{{std::move(expectedContact)}}, result.value());
    });
}

TEST_F(TestLogicDbGetContacts,
        for_get_shared_contacts_ended_with_error_should_return_badRequest) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContacts{uid, userType, {contactId}, {}, {}, {}})).WillOnce(
                Return(rows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result{getContacts(context, std::to_string(uid), {contactId}, "mixin", {}, {}, {}, {})};

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetContacts,
        for_get_contacts_with_mixin_should_return_contacts_with_shared_contacts) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContacts{uid, userType, {contactId}, {}, {}, {}})).WillOnce(
                Return(rows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(sharedContacts));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getContacts(context, std::to_string(uid), {contactId}, "mixin", {}, {}, {}, {})};

        ASSERT_TRUE(result);

        ExistingContact expectedContact;
        expectedContact.contact_id = contactId;
        expectedContact.list_id = listId;
        expectedContact.revision = revision;
        expectedContact.vcard.names = {{{{"First"}, {"Middle"}, {"Last"}, {}, {}}}};
        expectedContact.vcard.telephone_numbers = {{{"+79031234567"}, {}, {}, {}}, {{"+79657654321"}, {}, {},
                {}}};
        expectedContact.vcard.emails = {{{"local0@domain0.ru"}, {{{"t1"}, {"t2"}}}, {"label0"}},
                {{"local1@domain1.ru"}, {{{"t1"}, {"t3"}}}, {"label1"}}};
        expectedContact.vcard.organizations = {{"company0",  "title0", "summary0", "department0",  {{"type0"}}}};
        expectedContact.vcard.directory_entries = {{orgId, "Name0", 1, {{"type0"}}}};
        expectedContact.tag_ids = {tagId};
        expectedContact.uri = uri;
        expectedContact.emails = {{emailId, "local0@domain0.ru", {{tagId}}}, {emailId + 1,
                "local1@domain1.ru", {{tagId}}}};

        ExistingContact sharedContact;
        sharedContact.contact_id = 2;
        sharedContact.list_id = 1;
        sharedContact.revision = 2;
        sharedContact.vcard.names = {{{{"Hello"}, {}, {"Kitty"}, {}, {}}}};
        sharedContact.tag_ids = {6};
        sharedContact.uri = "kitty.vcf";
        sharedContact.emails = {{1, "hello@kitty.cat", {{1}}}};

        EXPECT_THAT(
            result.value().contacts,
            ElementsAre(expectedContact, sharedContact)
        );
    });
}

TEST_F(TestLogicDbGetContacts,
        for_get_contacts_with_onlyShared_flag_should_return_only_shared_contacts) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(sharedContacts));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getContacts(context, std::to_string(uid), {contactId}, {}, {}, {}, true, {})};

        ASSERT_TRUE(result);

        ExistingContact sharedContact;
        sharedContact.contact_id = 2;
        sharedContact.list_id = 1;
        sharedContact.revision = 2;
        sharedContact.vcard.names = {{{{"Hello"}, {}, {"Kitty"}, {}, {}}}};
        sharedContact.tag_ids = {6};
        sharedContact.uri = "kitty.vcf";
        sharedContact.emails = {{1, "hello@kitty.cat", {{1}}}};

        EXPECT_EQ(ExistingContacts{{std::move(sharedContact)}}, result.value());

    });
}

TEST_F(TestLogicDbGetContacts,
        for_get_contacts_with_correct_offset_and_limit_should_return_shared_contacts) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(sharedContacts));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getContacts(context, std::to_string(uid), {contactId}, {}, "0", "1", true, {})};

        ASSERT_TRUE(result);

        ExistingContact sharedContact;
        sharedContact.contact_id = 2;
        sharedContact.list_id = 1;
        sharedContact.revision = 2;
        sharedContact.vcard.names = {{{{"Hello"}, {}, {"Kitty"}, {}, {}}}};
        sharedContact.tag_ids = {6};
        sharedContact.uri = "kitty.vcf";
        sharedContact.emails = {{1, "hello@kitty.cat", {{1}}}};

        EXPECT_EQ(ExistingContacts{{std::move(sharedContact)}}, result.value());

    });
}

TEST_F(TestLogicDbGetContacts,
        for_get_contacts_with_limit_exceeding_the_number_of_shared_contacts_should_return_all_shared_contacts) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(sharedContacts));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getContacts(context, std::to_string(uid), {contactId}, {}, "0", "2", true, {})};

        ASSERT_TRUE(result);

        EXPECT_EQ(result.value().contacts.size(), 1ul);
    });
}

TEST_F(TestLogicDbGetContacts,
        for_get_contacts_with_zero_limit_should_empty_shared_contacts) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(sharedContacts));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getContacts(context, std::to_string(uid), {contactId}, {}, "1", "0", true, {})};

        ASSERT_TRUE(result);

        EXPECT_EQ(result.value().contacts.size(), 0ul);
    });
}

TEST_F(TestLogicDbGetContacts,
        for_get_contacts_with_negative_limit_should_return_error_bad_request) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        const auto result{getContacts(context, std::to_string(uid), {contactId}, {}, "0", "-2", true, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetContacts,
        for_get_contacts_with_SharedWithEmails_flag_should_return_shared_contacts_with_emails) {
    withSpawn([&] (const auto& context) {
        const ListId userListId {0};
        const ListId sharedListId {1};
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContacts{uid, userType, {contactId}, {}, {}, {}})).WillOnce(
                Return(makeContactRowResultWithEmptyEmails(userListId)));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(makeContactRowResultWithEmptyEmails(sharedListId)));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getContacts(context, std::to_string(uid), {contactId}, "mixin", {}, {}, {}, true)};

        ASSERT_TRUE(result);

        const auto& contacts = result.value().contacts;
        EXPECT_THAT(contacts.size(), 5);
        for (const auto& contact : contacts) {
            if (contact.list_id == sharedListId) {
                ASSERT_TRUE(collie::hasAtLeastOneEmail(contact.emails));
            }
        }
    });
}

} // namespace
