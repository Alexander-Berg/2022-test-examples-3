#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/logic/db/carddav_utils.hpp>

namespace {

using namespace collie::logic;
using namespace collie::logic::db;
using namespace std::string_literals;
using namespace testing;

using collie::services::db::contacts::CarddavContactRow;
using collie::logic::CreatedContacts;

TEST(TestLogicDbCarddavUtils, for_not_initialized_names_vector_should_return_empty_display_name) {
    Vcard vcard;
    EXPECT_EQ(getDisplayName(vcard), ""s);
}

TEST(TestLogicDbCarddavUtils, for_empty_names_vector_should_return_empty_display_name) {
    Vcard vcard;
    vcard.names = {};
    EXPECT_EQ(getDisplayName(vcard), ""s);
}

TEST(TestLogicDbCarddavUtils, for_name_vector_not_contained_name_with_first_or_last_should_return_empty_display_name) {
    Vcard vcard;
    vcard.names = std::vector {Name {}};
    EXPECT_EQ(getDisplayName(vcard), ""s);
}

TEST(TestLogicDbCarddavUtils, for_name_vector_contained_name_with_first_or_last_should_return_display_name) {
    Vcard vcard;
    vcard.names = {
        Name {"Diablo", std::nullopt, std::nullopt, std::nullopt, std::nullopt}
    };
    EXPECT_EQ(getDisplayName(vcard), "Diablo"s);
}

TEST(TestLogicDbCarddavUtils,
        for_name_vector_contained_name_with_first_or_last_name_and_name_without_them_should_return_display_name) {
    Vcard vcard;
    vcard.names = {
        Name {std::nullopt, "Mephisto"s, std::nullopt, std::nullopt, std::nullopt},
        Name {"Diablo"s, std::nullopt, std::nullopt, std::nullopt, "Mr"s}
    };
    EXPECT_EQ(getDisplayName(vcard), "Mr Diablo"s);
}

TEST(TestLogicDbCarddavUtils,
        for_name_vector_contained_full_filled_name_should_return_display_name) {
    Vcard vcard;
    vcard.names = {
        Name {std::nullopt, "Mephisto"s, std::nullopt, std::nullopt, std::nullopt},
        Name {"Diablo"s, "Junior"s, "Maks"s, "Collie"s, "Mr"s}
    };
    EXPECT_EQ(getDisplayName(vcard), "Mr Diablo Junior Maks Collie"s);
}

TEST(TestLogicDbCarddavUtils, for_not_empty_uri_should_return_him) {
    CarddavContactRow carddavContactRow;
    carddavContactRow.uri = "test.vcf";
    carddavContactRow.contact_id = 1;
    EXPECT_EQ(getUri(carddavContactRow), "test.vcf"s);
}

TEST(TestLogicDbCarddavUtils,
        for_empty_uri_should_return_uri_composed_of_identifier_and_key) {
    CarddavContactRow carddavContactRow;
    carddavContactRow.contact_id = 1;
    EXPECT_EQ(getUri(carddavContactRow), "YA-1"s);
}

TEST(TestLogicDbCarddavUtils, for_not_empty_contact_id_and_revision_should_return_etag) {
    CarddavContactRow carddavContactRow;
    carddavContactRow.contact_id = 1;
    carddavContactRow.revision = 2;
    EXPECT_EQ(getEtag(carddavContactRow), R"("1-2")");
}

TEST(TestLogicDbCarddavUtils, for_id_and_revision_should_return_etag) {
    EXPECT_EQ(getEtag(1, 2), R"("1-2")");
}

} // namespace collie::services::db
