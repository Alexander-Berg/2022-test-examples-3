#include "connection_provider_mock.hpp"
#include "transaction_mock.hpp"

#include <tests/unit/generic_operators.hpp>
#include <tests/unit/sheltie_client_mock.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/types/jsonb.hpp>

#include <src/services/db/contacts/types/reflection/new_contact.hpp>
#include <src/services/db/contacts/types/vcard_format.hpp>

#include <src/logic/db/carddav_put_impl.hpp>
#include <src/logic/interface/types/reflection/carddav_put_result.hpp>
#include <src/logic/interface/types/revision.hpp>

namespace collie::logic {

using collie::tests::operator ==;

}

namespace collie::services::db::contacts {

using collie::tests::operator ==;

}

namespace collie::tests {

template<class ... Ts>
collie::expected<collie::logic::CreatedContacts> createContacts(
    std::vector<collie::logic::NewContact> newContacts,
    collie::tests::ConnectionProvider<Ts ...> connection
) {
    using services::db::unwrap;
    services::db::contacts::query::CreateContacts query;
    query.uid = unwrap(connection).uid();
    query.user_type = unwrap(connection).userType();
    using yamail::data::serialization::toJson;
    ozo::pg::jsonb vcard(toJson(newContacts[0].vcard));
    query.contacts = std::vector<collie::services::db::contacts::NewContact> {
        services::db::contacts::NewContact {
            1, services::db::contacts::VcardFormat {"vcard_v1"}, vcard, newContacts[0].uri.value()
    }};
    const auto context(unwrap(connection).context());
    query.x_request_id = context->requestId();
    return services::db::request(connection, std::as_const(query))
        .bind([&] (auto&& row) {
            auto result(std::get<0>(services::db::expectSingleRow(std::move(row))));
            return collie::logic::CreatedContacts{std::move(result.contact_ids), result.revision};
        });
}

template<class ... Ts>
collie::expected<collie::logic::Revision> updateContacts(
    collie::logic::UpdatedContacts contacts,
    collie::tests::ConnectionProvider<Ts ...> connection
) {
    using services::db::unwrap;
    services::db::contacts::query::UpdateContacts query;
    query.uid = unwrap(connection).uid();
    query.user_type = unwrap(connection).userType();
    using yamail::data::serialization::toJson;
    ozo::pg::jsonb vcard(toJson(contacts.updated_contacts[0].vcard));
    query.contacts = std::vector<services::db::contacts::UpdatedContact> {
        services::db::contacts::UpdatedContact {
            contacts.updated_contacts[0].contact_id,
            1, services::db::contacts::VcardFormat {"vcard_v1"},
            vcard, contacts.updated_contacts[0].uri.value()
    }};
    const auto context(unwrap(connection).context());
    query.x_request_id = context->requestId();
    query.revision = contacts.revision;
    return services::db::request(connection, std::as_const(query))
        .bind([&] (auto&& rows) {
            return collie::logic::Revision(services::db::expectSingleRow(std::move(rows)));
        });
}

}

namespace {

using collie::error_code;
using collie::logic::CarddavPutResult;
using collie::logic::CreatedContacts;
using collie::logic::Error;
using collie::logic::Revision;
using collie::make_unexpected;

using collie::services::db::ConstUserType;
using collie::services::db::contacts::NewContact;
using collie::services::db::PassportUserId;
using collie::services::db::contacts::TagType;
using collie::services::db::contacts::VcardFormat;
using collie::services::db::contacts::CreateContactsResult;
using collie::services::db::contacts::CarddavContactRow;

using collie::tests::SheltieClientMock;

using collie::services::db::contacts::query::GetTagIdByTagNameAndTagType;
using collie::services::db::contacts::query::GetContactsByTagNameAndTagTypeAndUris;
using collie::services::db::contacts::query::CreateContacts;
using collie:: services::db::contacts::query::UpdateContacts;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
    GetTagIdByTagNameAndTagType,
    CreateContacts,
    GetContactsByTagNameAndTagTypeAndUris,
    UpdateContacts
>;
using ConnectionProvider = collie::tests::ConnectionProvider<
    GetTagIdByTagNameAndTagType,
    CreateContacts,
    GetContactsByTagNameAndTagTypeAndUris,
    UpdateContacts
>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
    GetTagIdByTagNameAndTagType,
    CreateContacts,
    GetContactsByTagNameAndTagTypeAndUris,
    UpdateContacts
>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
    GetTagIdByTagNameAndTagType,
    CreateContacts,
    GetContactsByTagNameAndTagTypeAndUris,
    UpdateContacts
>;

using CarddavPutImpl = collie::logic::db::CarddavPutImpl<MakeConnectionProvider>;
using DbError = collie::services::db::Error;

struct TestLogicDbCarddavPut : public TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const std::shared_ptr<StrictMock <const SheltieClientMock>> sheltieClient {
        std::make_shared<StrictMock <const SheltieClientMock>>()};
    const CarddavPutImpl carddavPut {makeProvider, sheltieClient};

    const std::string vcardAsRfc = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Hello Kitty\r\nN:Kitty;Hello;;;\r\nEND:VCARD\r\n";
    const std::string vcardAdJson = R"({"names":[{"first":"Hello","last":"Kitty"}]})";


};

TEST_F(TestLogicDbCarddavPut, for_invalid_uid_should_return_userNotFound) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const std::string nonExistentUid {"uid"};
        const auto result = carddavPut(context, nonExistentUid, "kitty.vcf", R"("3-4")", vcardAsRfc);

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::userNotFound}, result.error());
    });
}

TEST_F(TestLogicDbCarddavPut, for_begin_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_))
            .WillOnce(SetArgReferee<0>(DbError::databaseError));

        const auto result = carddavPut(context, "42", "kitty.vcf", R"("3-4")", vcardAsRfc);

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {DbError::databaseError}, result.error());
    });
}

TEST_F(TestLogicDbCarddavPut, for_bad_json_vcard_in_sheltie_response_should_throw_exception) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(""));

        EXPECT_THROW(carddavPut(context, "42", "kitty.vcf", R"("3-4")", vcardAsRfc), std::runtime_error);
    });
}

TEST_F(TestLogicDbCarddavPut,
        for_GetContactsByTagNameAndTagTypeAndUris_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(make_unexpected(error_code(DbError::databaseError))));


        const auto result = carddavPut(context, "42", "kitty.vcf", "*", vcardAsRfc);

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {DbError::databaseError}, result.error());
    });
}

TEST_F(TestLogicDbCarddavPut,
        for_create_contact_and_not_empty_GetContactsByTagNameAndTagTypeAndUris_should_return_CarddavPutResult_with_409_status) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavPut(context, "42", "kitty.vcf", "*", vcardAsRfc);

        ASSERT_TRUE(result);
        const auto expectedResult = CarddavPutResult {409, std::nullopt, "kitty.vcf alredy exists"};
        EXPECT_EQ(result.value(), expectedResult);
    });
}

TEST_F(TestLogicDbCarddavPut, for_GetTagIdByTagNameAndTagType_request_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetTagIdByTagNameAndTagType {42, userType, tagType, "Phone"}
        )).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        const auto result = carddavPut(context, "42", "kitty.vcf", "*", vcardAsRfc);

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {Error::badRequest}, result.error());
    });
}

TEST_F(TestLogicDbCarddavPut,
    for_CreateContacts_request_which_ended_with_error_should_return_CarddavPutResult_with_400_status) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetTagIdByTagNameAndTagType {42, userType, tagType, "Phone"}
        )).WillOnce(Return(std::vector<long>{1}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            CreateContacts {42, userType, {{1, VcardFormat {"vcard_v1"},  vcardAdJson, "kitty.vcf"}},
            "request_id"})).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));
        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavPut(context, "42", "kitty.vcf", "*", vcardAsRfc);

        ASSERT_TRUE(result);
        const auto expectedResult = CarddavPutResult {400, std::nullopt, "failed create contact"};
        EXPECT_EQ(result.value(), expectedResult);
    });
}

TEST_F(TestLogicDbCarddavPut,
    for_commit_which_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetTagIdByTagNameAndTagType {42, userType, tagType, "Phone"}
        )).WillOnce(Return(std::vector<long>{1}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            CreateContacts {42, userType, {{1, VcardFormat {"vcard_v1"},  vcardAdJson, "kitty.vcf"}},
            "request_id"})).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));

        EXPECT_CALL(providerMock, commit(_))
            .WillOnce(SetArgReferee<0>(DbError::databaseError));

        const auto result = carddavPut(context, "42", "kitty.vcf", "*", vcardAsRfc);

        ASSERT_FALSE(result);
        EXPECT_EQ(error_code {DbError::databaseError}, result.error());

    });
}

TEST_F(TestLogicDbCarddavPut,
    for_successful_CreateContacts_request_should_return_CarddavPutResult_with_201_status) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetTagIdByTagNameAndTagType {42, userType, tagType, "Phone"}
        )).WillOnce(Return(std::vector<long>{1}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            CreateContacts {42, userType, {{1, VcardFormat {"vcard_v1"},  vcardAdJson, "kitty.vcf"}},
            "request_id"})).WillOnce(Return(std::vector<std::tuple<CreateContactsResult>> {{{5, {3}}}}));
        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavPut(context, "42", "kitty.vcf", "*", vcardAsRfc);

        ASSERT_TRUE(result);
        const auto expectedResult = CarddavPutResult {201, R"("3-5")", std::nullopt};
        EXPECT_EQ(result.value(), expectedResult);
    });
}

TEST_F(TestLogicDbCarddavPut,
    for_update_and_empty_result_GetContactsByTagNameAndTagTypeAndUris_request_should_return_CarddavPutResult_with_400_status) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavPut(context, "42", "kitty.vcf", R"("3-4")", vcardAsRfc);

        ASSERT_TRUE(result);
        const auto expectedResult = CarddavPutResult {404, std::nullopt, "uri not found"};
        EXPECT_EQ(result.value(), expectedResult);

    });
}

TEST_F(TestLogicDbCarddavPut,
    for_mismatch_etag_should_return_CarddavPutResult_with_409_status) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavPut(context, "42", "kitty.vcf", R"("3-5")", vcardAsRfc);

        ASSERT_TRUE(result);
        const auto expectedResult = CarddavPutResult {409, std::nullopt, R"(etag mismatch, contact etag ("3-4" != "3-5"))"};
        EXPECT_EQ(result.value(), expectedResult);
    });
}

TEST_F(TestLogicDbCarddavPut,
    for_UpdateContacts_request_which_ended_with_error_should_return_CarddavPutResult_with_400_status) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UpdateContacts {42, userType, {{3, 1, VcardFormat {"vcard_v1"},  vcardAdJson, "kitty.vcf"}},
            "request_id", std::nullopt})).WillOnce(Return(make_unexpected(error_code(Error::badRequest))));
        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavPut(context, "42", "kitty.vcf", R"("3-4")", vcardAsRfc);

        ASSERT_TRUE(result);
        const auto expectedResult = CarddavPutResult {400, std::nullopt, "not update contact"};
        EXPECT_EQ(result.value(), expectedResult);
    });
}

TEST_F(TestLogicDbCarddavPut,
    for_successful_UpdateContacts_request_should_return_CarddavPutResult_with_200_status) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        const TagType tagType {"system"};
        const ConstUserType userType {"passport_user"};

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));

        EXPECT_CALL(*sheltieClient, fromVcard(context, "42", vcardAsRfc)).WillOnce(Return(vcardAdJson));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, request(
            GetContactsByTagNameAndTagTypeAndUris {42, userType, tagType, "Phone", {"kitty.vcf"}})
        ).WillOnce(Return(std::vector<CarddavContactRow> {
            {3, 4, R"({"names" : [ {"first" : "Hello", "last" : "Kitty"}]})", "kitty.vcf"}
        }));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        EXPECT_CALL(providerMock, uid()).WillOnce(Return(42));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(
            UpdateContacts {42, userType, {{3, 1, VcardFormat {"vcard_v1"},  vcardAdJson, "kitty.vcf"}},
            "request_id", std::nullopt})).WillOnce(Return(std::vector<Revision> {6}));
        EXPECT_CALL(providerMock, commit(_));

        const auto result = carddavPut(context, "42", "kitty.vcf", R"("3-4")", vcardAsRfc);

        ASSERT_TRUE(result);
        const auto expectedResult = CarddavPutResult {200, R"("3-6")", std::nullopt};
        EXPECT_EQ(result.value(), expectedResult);
    });
}

}
