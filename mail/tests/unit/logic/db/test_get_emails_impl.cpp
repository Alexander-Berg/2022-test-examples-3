#include "connection_provider_mock.hpp"

#include <src/logic/db/get_emails_impl.hpp>

#include <tests/unit/logic/interface/types/get_emails_result.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

namespace {

using collie::error_code;
using collie::logic::Error;
using collie::logic::ExistingEmails;
using collie::logic::GetEmailsResult;
using collie::logic::TagId;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::query::GetContactsEmailsByTagIds;
using collie::services::db::PassportUserId;
using collie::services::db::UserIdTraits;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<GetContactsEmailsByTagIds>;
using ConnectionProvider = collie::tests::ConnectionProvider<GetContactsEmailsByTagIds>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<GetContactsEmailsByTagIds>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<GetContactsEmailsByTagIds>;

using ExistingEmailRows = std::vector<GetContactsEmailsByTagIds::result_type>;
using GetEmailsImpl = collie::logic::db::GetEmailsImpl<MakeConnectionProvider>;

struct TestLogicDbGetEmails : public TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const GetEmailsImpl getEmails{makeProvider};
    const std::int64_t uid{42};
    const ConstUserType userType{UserIdTraits<PassportUserId>::typeName};
    const TagId tagId{1};
    const std::vector<TagId> tagIds{tagId - 1, tagId, tagId + 1};
    const ExistingEmailRows existingEmailRows {
        {tagId, 0, 0, 0, "local0@domain0.com", {{"type01", "type02"}}, {"label0"}},
        {tagId, 1, 1, 1, "local1@domain1.com", {{"type11", "type12"}}, {"label1"}},
        {tagId + 1, 2, 2, 2, "local2@domain2.com", {{"type21", "type22"}}, {"label2"}}
    };

    const GetEmailsResult expectedResult {{
        ExistingEmails {
            tagId,
            2,
            {{0, 0, 0, "local0@domain0.com", {{"type01", "type02"}}, "label0"},
             {1, 1, 1, "local1@domain1.com", {{"type11", "type12"}}, "label1"}}
        },
        ExistingEmails {
            tagId + 1,
            1,
            {{2, 2, 2, "local2@domain2.com", {{"type21", "type22"}}, "label2"}}
        }
    }};
};

TEST_F(TestLogicDbGetEmails, for_nonexistent_user_must_return_error_user_not_found) {
    withSpawn([&] (const auto& context) {
        const std::string nonexistentUid{"uid"};
        const auto result{getEmails(context, nonexistentUid, {}, {})};
        ASSERT_FALSE(result);
        EXPECT_EQ(error_code{Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbGetEmails, for_empty_tag_ids_must_return_empty_email_list) {
    withSpawn([&] (const auto& context) {
        const auto result{getEmails(context, std::to_string(uid), {}, {})};
        ASSERT_TRUE(result);
        EXPECT_EQ(GetEmailsResult{}, result.value());
    });
}

TEST_F(TestLogicDbGetEmails, for_existent_user_must_return_nonempty_email_list_and_corresponding_count) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContactsEmailsByTagIds{uid, userType, tagIds})).WillOnce(Return(
                existingEmailRows));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        const auto result{getEmails(context, std::to_string(uid), tagIds, {})};
        ASSERT_TRUE(result);
        EXPECT_EQ(expectedResult, result.value());
    });
}

}
