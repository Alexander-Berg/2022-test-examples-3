#include "connection_provider_mock.hpp"
#include "transaction_mock.hpp"

#include <src/logic/db/shared_contacts_impl.hpp>
#include <src/logic/interface/types/reflection/contacts_counters_result.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/logic/interface/types/existing_contacts.hpp>
#include <tests/unit/generic_operators.hpp>

namespace collie::logic {

static bool operator == (const ContactCounter& lhs, const ContactCounter& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator == (const ContactsCountersResult& lhs, const ContactsCountersResult& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

}

namespace {

using collie::make_unexpected;
using collie::error_code;
using collie::make_unexpected;
using collie::logic::ContactsCountersResult;
using collie::logic::Error;
using collie::logic::ExistingContact;
using collie::logic::ExistingContacts;
using collie::logic::Revision;
using collie::logic::ListId;
using collie::logic::Vcard;
using collie::services::db::ConstUserType;
using collie::services::db::OrgUserId;
using collie::services::db::contacts::CreatedListRow;
using collie::services::db::contacts::ListType;
using collie::services::db::contacts::SubscribeList;

using collie::services::db::contacts::query::GetSharedContacts;
using collie::services::db::contacts::query::GetSubscribedLists;
using collie::services::db::contacts::query::GetSharedContactsCount;
using collie::services::db::contacts::query::GetSharedContactsCountWithEmails;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
    GetSharedContacts,
    GetSubscribedLists,
    GetSharedContactsCount,
    GetSharedContactsCountWithEmails
>;
using ConnectionProvider = collie::tests::ConnectionProvider<
    GetSharedContacts,
    GetSubscribedLists,
    GetSharedContactsCount,
    GetSharedContactsCountWithEmails
>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
    GetSharedContacts,
    GetSubscribedLists,
    GetSharedContactsCount,
    GetSharedContactsCountWithEmails
>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
    GetSharedContacts,
    GetSubscribedLists,
    GetSharedContactsCount,
    GetSharedContactsCountWithEmails
>;

using ContactRows = std::vector<GetSharedContacts::result_type>;
using SharedContactsImpl = collie::logic::db::SharedContactsImpl<MakeConnectionProvider>;

struct TestSharedContacts : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const SharedContactsImpl sharedContactsImpl {makeProvider};
    const std::int64_t uid{42};
    const std::string userContactsListName{"shared_contacts"};
    const std::string requestId{"request_id"};
    const std::int64_t revision{1};
    const std::int64_t listId{12};
    const CreatedListRow createdListRow{revision, listId};
};


TEST_F(TestSharedContacts,
        for_GetSubscribedLists_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedLists {42, userType})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = sharedContactsImpl.getSharedContactsFromOrganisation(provider, context, "42");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestSharedContacts,
        for_GetSharedContacts_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const ConstUserType orgType {"connect_organization"};

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedLists {42, userType})
        ).WillOnce(Return(std::vector<SubscribeList> {
            {1, 2, "connect_organization", 1},
            {1, 2, "connect_organization", 2},
            {1, 2, "passport_user", 1},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(makeProviderMock, call(context, OrgUserId {2})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(2));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(orgType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts {2, orgType, 42, userType, {1, 2}})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = sharedContactsImpl.getSharedContactsFromOrganisation(provider, context, "42");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}


TEST_F(TestSharedContacts, for_result_from_database_should_return_shared_contacts) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const ConstUserType orgType {"connect_organization"};

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedLists {42, userType})
        ).WillOnce(Return(std::vector<SubscribeList> {
            {1, 2, "connect_organization", 1},
            {1, 2, "connect_organization", 2},
            {1, 2, "passport_user", 1},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(makeProviderMock, call(context, OrgUserId {2})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(2));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(orgType));
        EXPECT_CALL(providerMock, request(
            GetSharedContacts {2, orgType, 42, userType, {1, 2}})
        ).WillOnce(Return(ContactRows {
            {2, 1, 2,
            R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})",
            {6},
            "kitty.vcf",
            {{1, "hello@kitty.cat", {{1}}}
        }}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result = sharedContactsImpl.getSharedContactsFromOrganisation(provider, context, "42");

        ASSERT_TRUE(result);

        Vcard vcard;
        vcard.names = {{{{"Hello"}, {}, {"Kitty"}, {}, {}}}};
        ExistingContact sharedContact = {2, 1, 2, vcard, {6}, "kitty.vcf", {{1, "hello@kitty.cat", {{1}}}}};
        EXPECT_EQ(result.value(), ExistingContacts{{std::move(sharedContact)}});
    });
}

TEST_F(TestSharedContacts, for_count_result_from_database_should_return_shared_contacts) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const ConstUserType orgType {"connect_organization"};

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedLists {42, userType})
        ).WillOnce(Return(std::vector<SubscribeList> {
            {1, 2, "connect_organization", 1},
            {1, 2, "connect_organization", 2},
            {1, 2, "passport_user", 1},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(makeProviderMock, call(context, OrgUserId {2})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(2));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(orgType));
        EXPECT_CALL(providerMock, request(
            GetSharedContactsCount {2, orgType, 42, userType, {1, 2}})
        ).WillOnce(Return(std::vector<std::int64_t> {15}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result = sharedContactsImpl.getSharedContactsCountFromOrganisation(provider, context, "42", {});

        ASSERT_TRUE(result);

        ContactsCountersResult сontactsCountersResult;
        сontactsCountersResult.total = 15;
        сontactsCountersResult.book = {{"2", 15}};

        EXPECT_EQ(result.value(), сontactsCountersResult);
    });
}

TEST_F(TestSharedContacts, for_GetSharedContactsCount_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const ConstUserType orgType {"connect_organization"};

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetSubscribedLists {42, userType})
        ).WillOnce(Return(std::vector<SubscribeList> {
            {1, 2, "connect_organization", 1},
            {1, 2, "connect_organization", 2},
            {1, 2, "passport_user", 1},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(makeProviderMock, call(context, OrgUserId {2})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(2));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(orgType));
        EXPECT_CALL(providerMock, request(
            GetSharedContactsCount {2, orgType, 42, userType, {1, 2}})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = sharedContactsImpl.getSharedContactsCountFromOrganisation(provider, context, "42", {});

        ASSERT_FALSE(result);

        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

}
