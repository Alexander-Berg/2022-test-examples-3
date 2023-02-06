#include "connection_provider_mock.hpp"

#include <src/logic/db/get_contacts_with_tag_impl.hpp>

#include <tests/unit/logic/interface/types/existing_contacts.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

namespace {

using collie::error_code;
using collie::logic::ContactId;
using collie::logic::Error;
using collie::logic::ExistingContact;
using collie::logic::ExistingContacts;
using collie::logic::ListId;
using collie::logic::Revision;
using collie::logic::TagId;
using collie::services::db::contacts::query::GetContactsByTagId;
using collie::services::db::PassportUserId;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<GetContactsByTagId>;
using ConnectionProvider = collie::tests::ConnectionProvider<GetContactsByTagId>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<GetContactsByTagId>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<GetContactsByTagId>;

using ContactRows = std::vector<GetContactsByTagId::result_type>;
using GetContactsWithTagImpl = collie::logic::db::GetContactsWithTagImpl<MakeConnectionProvider>;

struct TestLogicDbGetContactsWithTag : public TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const GetContactsWithTagImpl getContactsWithTag{makeProvider};
    const std::int64_t uid{42};
    const ContactId contactId{1};
    const ListId listId{2};
    const Revision revision{3};
    const std::int64_t emailId{4};
    const TagId tagId{5};
    const std::string uri{"URI"};
    const ContactRows rows{{contactId, listId, revision,
        R"({"names":[{"last":"Last","middle":"Middle","first":"First"}],
            "emails":[{"type":["t1","t2"],"label":"label0","email":"local0@domain0.ru"},
                    {"type":["t1","t3"],"label":"label1","email":"local1@domain1.ru"}],
            "telephone_numbers":[{"telephone_number":"+79031234567"},{"telephone_number":"+79657654321"}],
            "extra_field": "value"})",
        {tagId},
        uri,
        {{emailId, "local0@domain0.ru", {{tagId}}}, {emailId + 1, "local1@domain1.ru", {{tagId}}}}
    }};
};

TEST_F(TestLogicDbGetContactsWithTag, for_nonexistent_user_must_return_error_user_not_found) {
    withSpawn([&] (const auto& context) {
        const std::string nonexistentUid{"uid"};
        const auto result{getContactsWithTag(context, nonexistentUid, {}, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbGetContactsWithTag, for_invalid_offset_must_return_error_bad_request) {
    withSpawn([&] (const auto& context) {
        const auto result{getContactsWithTag(context, std::to_string(uid), {}, {"Offset"}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetContactsWithTag, for_invalid_limit_must_return_error_bad_request) {
    withSpawn([&] (const auto& context) {
        const auto result{getContactsWithTag(context, std::to_string(uid), {}, {}, {"Limit"})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetContactsWithTag, for_existent_user_must_return_nonempty_contact_list) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, request(GetContactsByTagId{uid, tagId, {}, {}})).WillOnce(Return(rows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getContactsWithTag(context, std::to_string(uid), tagId, {}, {})};
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
        expectedContact.tag_ids = {tagId};
        expectedContact.uri = uri;
        expectedContact.emails = {{emailId, "local0@domain0.ru", {{tagId}}}, {emailId + 1,
                "local1@domain1.ru", {{tagId}}}};
        EXPECT_EQ(ExistingContacts{{std::move(expectedContact)}}, result.value());
    });
}

} // namespace
