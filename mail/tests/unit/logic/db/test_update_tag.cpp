#include "connection_provider_mock.hpp"
#include "transaction_mock.hpp"

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

#include <src/logic/db/update_tag_impl.hpp>

namespace {

using namespace testing;

using collie::error_code;
using collie::services::db::contacts::ExistingTagRow;
using collie::services::db::contacts::TagType;
using collie::services::db::contacts::query::UpdateTag;
using collie::services::db::contacts::query::GetContactsEmailIdsByContactsIds;
using collie::services::db::contacts::query::GetContactsIdsByContactsEmailIds;
using collie::services::db::contacts::query::GetContactsEmailIdsByContactIdAndTagIds;
using collie::services::db::contacts::query::GetTagById;
using collie::services::db::contacts::query::UntagContactsEmails;
using collie::services::db::contacts::query::UntagContacts;
using collie::services::db::contacts::query::TagContacts;
using collie::services::db::contacts::query::TagContactsEmails;
using collie::logic::Error;
using collie::logic::Revision;
using collie::logic::TagId;
using collie::logic::Uid;
using collie::logic::ContactId;
using collie::TaskContextPtr;
using collie::services::db::PassportUserId;
using collie::services::db::ConstUserType;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
    UpdateTag,
    GetContactsEmailIdsByContactsIds,
    GetContactsIdsByContactsEmailIds,
    GetContactsEmailIdsByContactIdAndTagIds,
    TagContacts,
    TagContactsEmails,
    UntagContacts,
    UntagContactsEmails,
    GetTagById
>;
using ConnectionProvider = collie::tests::ConnectionProvider<
    UpdateTag,
    GetContactsEmailIdsByContactsIds,
    GetContactsIdsByContactsEmailIds,
    GetContactsEmailIdsByContactIdAndTagIds,
    TagContacts,
    TagContactsEmails,
    UntagContacts,
    UntagContactsEmails,
    GetTagById
>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
    UpdateTag,
    GetContactsEmailIdsByContactsIds,
    GetContactsIdsByContactsEmailIds,
    GetContactsEmailIdsByContactIdAndTagIds,
    TagContacts,
    TagContactsEmails,
    UntagContacts,
    UntagContactsEmails,
    GetTagById
>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
    UpdateTag,
    GetContactsEmailIdsByContactsIds,
    GetContactsIdsByContactsEmailIds,
    GetContactsEmailIdsByContactIdAndTagIds,
    TagContacts,
    TagContactsEmails,
    UntagContacts,
    UntagContactsEmails,
    GetTagById
>;

using UpdateTagRows = std::vector<UpdateTag::result_type>;
using ContactIdEmailIdEmailRows = std::vector<GetContactsEmailIdsByContactsIds::result_type>;
using ContactIdEmailIdRows = std::vector<GetContactsIdsByContactsEmailIds::result_type>;
using UntagContactsEmailsRows = std::vector<UntagContactsEmails::result_type>;
using UntagContactsRows = std::vector<UntagContacts::result_type>;
using TagContactsRows = std::vector<TagContacts::result_type>;
using TagContactsEmailsRows = std::vector<TagContactsEmails::result_type>;
using ContactsRows = std::vector<GetContactsEmailIdsByContactIdAndTagIds::result_type>;
using ExistingTagRows = std::vector<GetTagById::result_type>;

using UpdateTagImpl = collie::logic::db::UpdateTagImpl<MakeConnectionProvider>;

struct TestLogicDbUpdateTag : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const UpdateTagImpl updateTag {makeProvider};
    const std::int64_t uid{42};
    const ConstUserType userType {"passport_user"};
    const TagId tagId{333};
    const std::string tagName {"tag_name"};
    const std::string requestId {"request_id"};
    const Revision currentRevision{7};
};

TEST_F(TestLogicDbUpdateTag, for_text_uid_should_return_userNotFound) {
    withSpawn([&] (const auto& context) {
        const auto result = updateTag(context, "TEXT", tagId, {tagName, 1, {}, {}, {}, {}});

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), error_code{Error::userNotFound});
    });
}

TEST_F(TestLogicDbUpdateTag, for_existent_user_should_update_tag) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, request(UpdateTag{uid, tagId, tagName, "request_id", 1}))
            .WillOnce(Return(UpdateTagRows{2}));
        EXPECT_CALL(providerMock, commit(_));

        const auto result = updateTag(context, std::to_string(uid), tagId, {tagName, 1, {}, {}, {}, {}});

        ASSERT_TRUE(result);
        EXPECT_EQ(2, result.value());
    });
}

TEST_F(TestLogicDbUpdateTag, for_existent_user_and_empty_tag_name_must_return_current_revision) {
    withSpawn([this](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        ExistingTagRow row{{}, TagType{"user"}, {}, {}, currentRevision};
        EXPECT_CALL(providerMock, request(GetTagById{uid, userType, tagId})).WillOnce(Return(ExistingTagRows{
                std::move(row)}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        const auto revision{1};
        const auto result{updateTag(context, std::to_string(uid), tagId, {{}, revision, {}, {}, {}, {}})};
        ASSERT_TRUE(result);
        EXPECT_EQ(currentRevision, result.value());
    });
}

TEST_F(TestLogicDbUpdateTag, for_existent_user_should_update_tag_and_tag_contacts_and_emails) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, request(UpdateTag{uid, tagId, tagName, requestId, 1}))
            .WillOnce(Return(UpdateTagRows{2}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContactsIdsByContactsEmailIds{uid, userType, {1,2,3}}))
            .WillOnce(Return(ContactIdEmailIdRows{{4,4}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {1,2,3}}))
            .WillOnce(Return(ContactIdEmailIdEmailRows{{5,5,""}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(TagContacts{uid, userType, tagId, {1,2,3,4}, requestId}))
            .WillOnce(Return(TagContactsRows{2}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(TagContactsEmails{uid, userType, tagId, {1,2,3,5}, requestId}))
            .WillOnce(Return(TagContactsEmailsRows{2}));

        EXPECT_CALL(providerMock, commit(_));

        const auto result = updateTag(
            context,
            std::to_string(uid),
            tagId,
            {tagName, 1, {{1,2,3}}, {}, {{{0, 1},{0, 2}, {0, 3}}}, {}});

        ASSERT_TRUE(result);
        EXPECT_EQ(2, result.value());
    });
}

TEST_F(TestLogicDbUpdateTag, for_existent_user_should_update_tag_and_untag_contacts_and_emails) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, request(UpdateTag{uid, tagId, tagName, requestId, 1}))
            .WillOnce(Return(UpdateTagRows{2}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContactsIdsByContactsEmailIds{uid, userType, {1,2,3}}))
            .WillOnce(Return(ContactIdEmailIdRows{{4,4}, {5,5}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {1,2,3}}))
            .WillOnce(Return(ContactIdEmailIdEmailRows{{6,6,""}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(UntagContactsEmails{uid, userType, tagId, {1,2,3,6}, requestId}))
            .WillOnce(Return(UntagContactsEmailsRows{2}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactIdAndTagIds{uid, userType, 4, {tagId}}))
            .WillOnce(Return(ContactsRows{}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactIdAndTagIds{uid, userType, 5, {tagId}}))
            .WillOnce(Return(ContactsRows{6,7,8}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(UntagContacts{uid, userType, tagId, {1,2,3,4}, requestId}))
            .WillOnce(Return(UntagContactsRows{2}));

        EXPECT_CALL(providerMock, commit(_));

        const auto result = updateTag(
            context,
            std::to_string(uid),
            tagId,
            {tagName, 1, {}, {{1,2,3}}, {}, {{{0, 1},{0, 2}, {0, 3}}}});

        ASSERT_TRUE(result);
        EXPECT_EQ(2, result.value());
    });
}

}
