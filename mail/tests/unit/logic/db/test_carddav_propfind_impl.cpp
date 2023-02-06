#include "connection_provider_mock.hpp"

#include <src/logic/db/carddav_propfind_impl.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

namespace collie::logic {

static bool operator == (const CarddavContactInfo& lhs, const CarddavContactInfo& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

}

namespace {

using collie::error_code;
using collie::make_unexpected;
using collie::logic::Error;
using collie::logic::CarddavContactInfo;
using collie::services::db::contacts::query::GetContactsByTagNameAndTagTypeAndUris;
using collie::services::db::contacts::CarddavContactRow;
using collie::services::db::PassportUserId;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::TagType;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<GetContactsByTagNameAndTagTypeAndUris>;
using ConnectionProvider = collie::tests::ConnectionProvider<GetContactsByTagNameAndTagTypeAndUris>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<GetContactsByTagNameAndTagTypeAndUris>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<GetContactsByTagNameAndTagTypeAndUris>;
using CarddavPropfindImpl = collie::logic::db::CarddavPropfindImpl<MakeConnectionProvider>;

struct TestLogicDbCarddavPropfind : public TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const CarddavPropfindImpl carddavPropfind {makeProvider};
};

TEST_F(TestLogicDbCarddavPropfind, for_invalid_uid_should_return_userNotFound) {
    withSpawn([this](const auto& context) {
        const std::string nonexistentUid {"uid"};
        const auto result = carddavPropfind(context, nonexistentUid);

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbCarddavPropfind, for_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {}})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavPropfind(context, "42");

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavPropfind, for_empty_result_from_database_should_return_empty_carddav_propfind_result) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result = carddavPropfind(context, "42");

        ASSERT_TRUE(result);
        EXPECT_TRUE(result.value().contact.empty());
    });
}

TEST_F(TestLogicDbCarddavPropfind, for_invalid_result_from_database_should_return_exception) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "", {}}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_THROW(carddavPropfind(context, "42"), std::runtime_error);
    });
}

TEST_F(TestLogicDbCarddavPropfind, for_result_from_database_should_return_carddav_propfind_result) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result = carddavPropfind(context, "42");

        ASSERT_TRUE(result);
        EXPECT_THAT(
            result.value().contact,
            ElementsAre(
                CarddavContactInfo {"YA-1", "", R"("1-2")"},
                CarddavContactInfo {"kitty.vcf", "Hello Kitty", R"("3-4")"}
            )
        );
    });
}

}
