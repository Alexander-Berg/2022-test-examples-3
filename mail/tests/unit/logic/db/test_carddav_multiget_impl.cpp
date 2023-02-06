#include "connection_provider_mock.hpp"

#include <tests/unit/generic_operators.hpp>

#include <src/logic/db/carddav_multiget_impl.hpp>
#include <src/logic/interface/types/reflection/vcard.hpp>

#include <tests/unit/sheltie_client_mock.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

namespace collie::logic {

using collie::tests::operator ==;

}

namespace {

using collie::error_code;
using collie::logic::CarddavContact;
using collie::logic::Error;
using collie::logic::MapUriVcardJson;
using collie::logic::MapUriVcardRfc;
using collie::logic::Vcard;
using collie::make_unexpected;
using collie::tests::SheltieClientMock;
using collie::services::db::PassportUserId;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::CarddavContactRow;
using collie::services::db::contacts::TagType;
using collie::services::db::contacts::query::GetContactsByTagNameAndTagTypeAndUris;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<GetContactsByTagNameAndTagTypeAndUris>;
using ConnectionProvider = collie::tests::ConnectionProvider<GetContactsByTagNameAndTagTypeAndUris>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<GetContactsByTagNameAndTagTypeAndUris>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<GetContactsByTagNameAndTagTypeAndUris>;
using CarddavMultigetImpl = collie::logic::db::CarddavMultigetImpl<MakeConnectionProvider>;

struct TestLogicDbCarddavMultiget : public TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const std::shared_ptr<StrictMock <const SheltieClientMock>> sheltieClient{
        std::make_shared<StrictMock <const SheltieClientMock>>()};
    const CarddavMultigetImpl carddavMultiget{makeProvider, sheltieClient};
};

TEST_F(TestLogicDbCarddavMultiget, for_invalid_uid_should_return_userNotFound) {
    withSpawn([this](const auto& context) {
        const std::string nonexistentUid {"uid"};
        const auto result = carddavMultiget(context, nonexistentUid, {"uris"});

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbCarddavMultiget, for_request_whith_empty_uris_should_return_empty_result) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const auto result = carddavMultiget(context, "42", {});

        ASSERT_TRUE(result);
        EXPECT_TRUE(result.value().contact.empty());
    });
}

TEST_F(TestLogicDbCarddavMultiget, for_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"YA-1", "dude.vcf", "kitty.vcf"}})
        ).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavMultiget(context, "42", {"YA-1", "dude.vcf", "kitty.vcf"});

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavMultiget, for_empty_result_from_database_should_return_empty_carddav_multiget_result) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"YA-1", "dude.vcf", "kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        const auto result = carddavMultiget(context, "42", {"YA-1", "dude.vcf", "kitty.vcf"});

        ASSERT_TRUE(result);
        EXPECT_THAT(
            result.value().contact,
            ElementsAre(
                CarddavContact {"YA-1", 404, std::nullopt, std::nullopt},
                CarddavContact {"dude.vcf", 404, std::nullopt, std::nullopt},
                CarddavContact {"kitty.vcf", 404, std::nullopt, std::nullopt}
            )
        );
    });
}

TEST_F(TestLogicDbCarddavMultiget, for_invalid_result_from_database_should_return_exception) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"YA-1", "dude.vcf", "kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "", {}},
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_THROW(
            carddavMultiget(context, "42", {"YA-1", "dude.vcf", "kitty.vcf"}),
            std::runtime_error
        );
    });
}

TEST_F(TestLogicDbCarddavMultiget, for_result_from_database_should_return_carddav_multiget_result) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const ConstUserType userType {"passport_user"};
        const TagType tagType {"system"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"YA-1", "dude.vcf", "kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {
            {1, 2, "{}", {}},
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        Vcard vcard;
        vcard.names = {{"Hello", std::nullopt, "Kitty", std::nullopt, std::nullopt}};

        EXPECT_CALL(*sheltieClient, toVcard(context, "42",
            MapUriVcardJson {
                {"YA-1", {}},
                {"kitty.vcf", vcard}
            }
        )).WillOnce(Return(
            MapUriVcardRfc {
                {"YA-1", "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n"},
                {"kitty.vcf", "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Hello Kitty\r\nN:Kitty;Hello;;;\r\nEND:VCARD\r\n"}
            }
        ));

        const auto result = carddavMultiget(context, "42", {"YA-1", "dude.vcf", "kitty.vcf"});

        ASSERT_TRUE(result);
        EXPECT_THAT(
            result.value().contact,
            ElementsAre(
                CarddavContact {"YA-1", 200, R"("1-2")", "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:\r\nN:;;;;\r\nEND:VCARD\r\n"},
                CarddavContact {"dude.vcf", 404, std::nullopt, std::nullopt},
                CarddavContact {"kitty.vcf", 200, R"("3-4")",
                    "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Hello Kitty\r\nN:Kitty;Hello;;;\r\nEND:VCARD\r\n"
                }
            )
        );
    });
}

}
