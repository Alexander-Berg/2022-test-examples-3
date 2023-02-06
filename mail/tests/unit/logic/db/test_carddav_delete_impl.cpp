#include "connection_provider_mock.hpp"
#include "transaction_mock.hpp"

#include <src/logic/db/carddav_delete_impl.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

namespace {

using collie::error_code;
using collie::make_expected;
using collie::make_unexpected;
using collie::logic::Error;
using collie::logic::Revision;
using collie::services::db::PassportUserId;
using collie::services::db::contacts::CarddavContactRow;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::TagType;
using collie::services::db::contacts::ContactIdEmailIdEmailRow;

using collie::services::db::contacts::query::GetContactsByTagNameAndTagTypeAndUris;
using collie::services::db::contacts::query::GetContactsEmailIdsByContactsIds;
using collie::services::db::contacts::query::RemoveContacts;
using collie::services::db::contacts::query::UntagContactsCompletely;
using collie::services::db::contacts::query::RemoveContactsEmails;
using collie::services::db::contacts::query::UntagContactsEmailsCompletely;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
    RemoveContacts,
    GetContactsByTagNameAndTagTypeAndUris,
    GetContactsEmailIdsByContactsIds,
    UntagContactsCompletely,
    RemoveContactsEmails,
    UntagContactsEmailsCompletely
>;

using ConnectionProvider = collie::tests::ConnectionProvider<
    RemoveContacts,
    GetContactsByTagNameAndTagTypeAndUris,
    GetContactsEmailIdsByContactsIds,
    UntagContactsCompletely,
    RemoveContactsEmails,
    UntagContactsEmailsCompletely
>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
    RemoveContacts,
    GetContactsByTagNameAndTagTypeAndUris,
    GetContactsEmailIdsByContactsIds,
    UntagContactsCompletely,
    RemoveContactsEmails,
    UntagContactsEmailsCompletely
>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
    RemoveContacts,
    GetContactsByTagNameAndTagTypeAndUris,
    GetContactsEmailIdsByContactsIds,
    UntagContactsCompletely,
    RemoveContactsEmails,
    UntagContactsEmailsCompletely
>;

using CarddavDeleteImpl = collie::logic::db::CarddavDeleteImpl<MakeConnectionProvider>;
using DbError = collie::services::db::Error;

struct TestLogicDbCarddavDelete : public TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const CarddavDeleteImpl carddavDelete{makeProvider};
};

TEST_F(TestLogicDbCarddavDelete, for_invalid_uid_should_return_userNotFound) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const std::string nonExistentUid {"uid"};
        const auto result = carddavDelete(context, nonExistentUid, "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete, for_begin_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_))
            .WillOnce(SetArgReferee<0>(DbError::databaseError));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {DbError::databaseError}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete,
        for_GetContactsByTagNameAndTagTypeAndUris_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete,
        for_GetContactsByTagNameAndTagTypeAndUris_empty_result_from_database_should_return_void) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_TRUE(result);
        EXPECT_EQ(result, make_expected());
    });
}

TEST_F(TestLogicDbCarddavDelete, for_commit_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, commit(_))
            .WillOnce(SetArgReferee<0>(DbError::databaseError));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {DbError::databaseError}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete,
        for_GetContactsEmailIdsByContactsIds_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsEmailIdsByContactsIds {42, userType, {1, 3}}
        )).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete,
        for_untagContactsEmailsCompletely_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsEmailIdsByContactsIds {42, userType, {1, 3}}
        )).WillOnce(Return(std::vector<ContactIdEmailIdEmailRow> {
            {1, 5, ""},
            {2, 6, "hello@kitty.cat"},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsEmailsCompletely {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete,
        for_removeContactsEmails_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsEmailIdsByContactsIds {42, userType, {1, 3}}
        )).WillOnce(Return(std::vector<ContactIdEmailIdEmailRow> {
            {1, 5, ""},
            {2, 6, "hello@kitty.cat"},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsEmailsCompletely {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {1}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, request(
            RemoveContactsEmails {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete,
        for_untagContactsCompletely_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsEmailIdsByContactsIds {42, userType, {1, 3}}
        )).WillOnce(Return(std::vector<ContactIdEmailIdEmailRow> {
            {1, 5, ""},
            {2, 6, "hello@kitty.cat"},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsEmailsCompletely {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {1}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            RemoveContactsEmails {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {2}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsCompletely {42, userType, {1, 3}, "request_id"}
        )).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete,
        for_removeContacts_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsEmailIdsByContactsIds {42, userType, {1, 3}}
        )).WillOnce(Return(std::vector<ContactIdEmailIdEmailRow> {
            {1, 5, ""},
            {2, 6, "hello@kitty.cat"},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsEmailsCompletely {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {1}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            RemoveContactsEmails {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {2}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsCompletely {42, userType, {1, 3}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {3}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            RemoveContacts {42, userType, {1, 3}, "request_id"}
        )).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavDelete, for_successful_request_should_return_void) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsEmailIdsByContactsIds {42, userType, {1, 3}}
        )).WillOnce(Return(std::vector<ContactIdEmailIdEmailRow> {
            {1, 5, ""},
            {2, 6, "hello@kitty.cat"},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsEmailsCompletely {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {1}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            RemoveContactsEmails {42, userType, {5, 6}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {2}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsCompletely {42, userType, {1, 3}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {3}));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            RemoveContacts {42, userType, {1, 3}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {4}));

        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_TRUE(result);
        EXPECT_EQ(result, make_expected());
    });
}

TEST_F(TestLogicDbCarddavDelete,
        for_GetContactsEmailIdsByContactsIds_empty_result_request_should_return_void) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}}
        )).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsEmailIdsByContactsIds {42, userType, {1, 3}}
        )).WillOnce(Return(std::vector<ContactIdEmailIdEmailRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UntagContactsCompletely {42, userType, {1, 3}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {1}));


        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            RemoveContacts {42, userType, {1, 3}, "request_id"}
        )).WillOnce(Return(std::vector<Revision> {2}));

        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavDelete(context, "42", "kitty.vcf");

        ASSERT_TRUE(result);
        EXPECT_EQ(result, make_expected());
    });
}

}
