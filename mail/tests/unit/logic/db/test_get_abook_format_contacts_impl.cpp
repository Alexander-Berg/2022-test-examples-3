#include "connection_provider_mock.hpp"

#include <src/logic/db/get_abook_format_contacts_impl.hpp>
#include <src/logic/interface/types/org_id.hpp>
#include <src/services/abook/types/reflection/search_contacts_result.hpp>

#include <tests/unit/services/abook/types/search_contacts_result.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/types/email_with_tags.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/shared_contacts_mock.hpp>

namespace {

using collie::error_code;
using collie::logic::ContactId;
using collie::logic::Error;
using collie::logic::ListId;
using collie::logic::OrgId;
using collie::logic::Revision;
using collie::logic::TagId;
using collie::services::abook::ContactEmail;
using collie::services::abook::SearchContactsResult;
using collie::services::abook::SearchContactsUngroupedResult;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::query::GetContacts;
using collie::services::db::contacts::query::GetSharedContacts;
using collie::services::db::PassportUserId;
using collie::make_unexpected;

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
using GetAbookFormatContactsImpl = collie::logic::db::GetAbookFormatContactsImpl<MakeConnectionProvider>;

struct TestLogicDbGetAbookFormatContacts : public TestWithTaskContext {
    SearchContactsResult makeSearchContactsUngroupedResult() const {
        const auto count{3};
        SearchContactsUngroupedResult result{std::vector<ContactEmail>{count}, count};

        result.contact[0].cid = contactId;
        result.contact[0].id = emailId;
        result.contact[0].tags = {tagId};
        result.contact[0].name = {"First0", "Middle0", "Last0"};
        result.contact[0].email = "local0@domain0.ru";
        result.contact[0].photo_partial_url = "photo_partial_url0";
        result.contact[0].company = "Company0";
        result.contact[0].department = "Department0";
        result.contact[0].title = "Title0";
        result.contact[0].ya_directory = {orgId, {"Name0"}, {"Type0"}, yaDirectoryId};

        result.contact[1] = result.contact[0];
        result.contact[1].id = emailId + 1;
        result.contact[1].tags = {tagId + 1};
        result.contact[1].email = "local1@domain1.ru";

        result.contact[2].cid = contactId + 1;
        result.contact[2].id = emailId + 2;
        result.contact[2].tags = {tagId + 2};
        result.contact[2].name = {"First1", "Middle1", "Last1"};
        result.contact[2].email = "local2@domain2.ru";
        result.contact[2].photo_partial_url = "photo_partial_url1";
        result.contact[2].company = "Company1";
        result.contact[2].department = "Department1";
        result.contact[2].title = "Title1";
        result.contact[2].ya_directory = {orgId + 1, {"Name1"}, {"Type1"}, yaDirectoryId + 1};

        return result;
    }

    SearchContactsResult makeSearchContactsUngroupedResultWithSharedContacts() const {
        const auto count = 4;
        SearchContactsUngroupedResult result{std::vector<ContactEmail>{count}, count};

        result.contact[0].cid = contactId;
        result.contact[0].id = emailId;
        result.contact[0].tags = {tagId};
        result.contact[0].name = {"First0", "Middle0", "Last0"};
        result.contact[0].email = "local0@domain0.ru";
        result.contact[0].photo_partial_url = "photo_partial_url0";
        result.contact[0].company = "Company0";
        result.contact[0].department = "Department0";
        result.contact[0].title = "Title0";
        result.contact[0].ya_directory = {orgId, {"Name0"}, {"Type0"}, yaDirectoryId};

        result.contact[1] = result.contact[0];
        result.contact[1].id = emailId + 1;
        result.contact[1].tags = {tagId + 1};
        result.contact[1].email = "local1@domain1.ru";

        result.contact[2].cid = contactId + 1;
        result.contact[2].id = emailId + 2;
        result.contact[2].tags = {tagId + 2};
        result.contact[2].name = {"First1", "Middle1", "Last1"};
        result.contact[2].email = "local2@domain2.ru";
        result.contact[2].photo_partial_url = "photo_partial_url1";
        result.contact[2].company = "Company1";
        result.contact[2].department = "Department1";
        result.contact[2].title = "Title1";
        result.contact[2].ya_directory = {orgId + 1, {"Name1"}, {"Type1"}, yaDirectoryId + 1};

        result.contact[3].cid = 2;
        result.contact[3].id = 1;
        result.contact[3].tags = {6};
        result.contact[3].name = {"Hello", {}, "Kitty"};
        result.contact[3].email = "hello@kitty.cat";

        return result;
    }

    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const GetAbookFormatContactsImpl getAbookFormatContacts{makeProvider};
    const std::int64_t uid{42};
    const ConstUserType userType{"passport_user"};
    const ContactId contactId{1};
    const ListId listId{2};
    const Revision revision{3};
    const OrgId orgId{1};
    const std::int64_t emailId{4};
    const TagId tagId{5};
    const std::int64_t yaDirectoryId{1};
    const std::string uri{"URI"};
    const ContactRows rows {{
            contactId,
            listId,
            revision,
            R"({"names":[{"first":"First0","middle":"Middle0","last":"Last0"}],
                "emails":[{"email":"local0@domain0.ru"},{"email":"local1@domain1.ru"}],
                "directory_entries":[{"org_id":1,"org_name":"Name0","entry_id":1,
                "type":["Type0"]}],
                "photos":[{"uri":"photo_partial_url0"}],
                "organizations":[{"company":"Company0","title":"Title0","department":"Department0"}]})",
            {tagId, tagId + 1},
            uri,
            {{emailId, "local0@domain0.ru", {{tagId}}}, {emailId + 1, "local1@domain1.ru", {{tagId + 1}}}}
        }, {
            contactId + 1,
            listId,
            revision,
            R"({"names":[{"first":"First1","middle":"Middle1","last":"Last1"}],
                "emails":[{"email":"local2@domain2.ru"}],
                "directory_entries":[{"org_id":2,"org_name":"Name1","entry_id":2,
                "type":["Type1"]}],
                "photos":[{"uri":"photo_partial_url1"}],
                "organizations":[{"company":"Company1","title":"Title1","department":"Department1"}]})",
            {tagId + 2},
            uri,
            {{emailId + 2, "local2@domain2.ru", {{tagId + 2}}}}
        }
    };

    const ContactRows sharedContacts{{2, 1, 2,
        R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})",
        {6},
        "kitty.vcf",
        {{1, "hello@kitty.cat", {{6}}}}
    }};
};

TEST_F(TestLogicDbGetAbookFormatContacts, for_nonexistent_user_must_return_error_user_not_found) {
    withSpawn([&] (const auto& context) {
        const std::string nonexistentUid{"uid"};
        const auto result{getAbookFormatContacts(context, nonexistentUid, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbGetAbookFormatContacts, for_existent_user_must_return_nonempty_contactemail_list) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContacts{uid, userType, {}, {}, {}, {}})).WillOnce(Return(rows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        const auto result{getAbookFormatContacts(context, std::to_string(uid), {}, {})};
        ASSERT_TRUE(result);
        EXPECT_EQ(makeSearchContactsUngroupedResult(), result.value());
    });
}

TEST_F(TestLogicDbGetAbookFormatContacts,
        for_get_shared_contacts_ended_with_error_should_return_badRequest) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContacts{uid, userType, {}, {}, {}, {}})).WillOnce(Return(rows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result{getAbookFormatContacts(context, std::to_string(uid), "mixin", {})};

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbGetAbookFormatContacts, for_get_contacts_with_mixin_should_return_search_result_with_shared_contacts) {
    withSpawn([&] (const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContacts{uid, userType, {}, {}, {}, {}})).WillOnce(Return(rows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts{uid, userType, uid, userType, {1, 2, 3}})
        ).WillOnce(Return(sharedContacts));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result{getAbookFormatContacts(context, std::to_string(uid), "mixin", {})};

        ASSERT_TRUE(result);

        EXPECT_EQ(makeSearchContactsUngroupedResultWithSharedContacts(), result.value());
    });
}

} // namespace
