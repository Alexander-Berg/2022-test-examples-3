#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/blackbox/types_reflection.h>
#include <internal/common/types_reflection.h>

#include <internal/api/utils.h>

#include <yamail/data/deserialization/json_reader.h>

#include <boost/fusion/include/equal_to.hpp>

namespace macs::settings {

static bool operator ==(const TextTraits& lhs, const TextTraits& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const Signature& lhs, const Signature& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

}

namespace settings {

static bool operator ==(const Email& lhs, const Email& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const MapOptions& lhs, const MapOptions& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

}

namespace {

using namespace testing;
using namespace settings;
using namespace settings::api;

blackbox::AddressList getBbDefaultAddresslItem() {
    const std::string response = R"({"users":[{"address-list":)"
        R"([{"validated":true,"default":true,"rpop":true,"silent":true,"unsafe":true,"native":true,)"
        R"("born-date":"1974-01-01","address":"hello@yandex.ru"}]}]})";
    blackbox::InfoResponse infoResponse;
    yamail::data::deserialization::fromJson<blackbox::InfoResponse>(response, infoResponse);
    return infoResponse.users[0].addresses.value();
}

TEST(TestApiUtils, for_empty_settings_list_should_return_true) {
    EXPECT_TRUE(hasSetting(SettingsList {}, "some_settings"));
}

TEST(TestApiUtils, for_settings_list_containing_setting_should_return_true) {
    EXPECT_TRUE(hasSetting(SettingsList {"some_settings"}, "some_settings"));
}

TEST(TestApiUtils, for_settings_list_not_containing_setting_should_return_false) {
    EXPECT_FALSE(hasSetting(SettingsList {"some_settings"}, "tutu"));
}

TEST(TestApiUtils, for_settings_list_not_containing_from_name_settings_does_not_change) {
    auto options = MapOptions {};
    options.single_settings["from_name"] = "from_name";
    auto result = MapOptions {};
    result.single_settings["from_name"] = "from_name";
    addFromName(
        options,
        std::shared_ptr<blackbox::AccountInfo> {}, SettingsList {"tutu"},
        "production"
    );
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils, for_settings_list_and_settings_containing_from_name_settings_does_not_change) {
    auto options = MapOptions {};
    options.single_settings["from_name"] = "from_name";
    auto result = MapOptions {};
    result.single_settings["from_name"] = "from_name";
    addFromName(
        options,
        std::shared_ptr<blackbox::AccountInfo> {}, SettingsList {"from_name"},
        "production"
    );
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_from_name_and_settings_does_not_containing_it_and_empty_account_info_should_init_empty_string) {
    auto options = MapOptions {};
    auto result = MapOptions {};
    result.single_settings["from_name"] = "";
    addFromName(
        options,
        std::shared_ptr<blackbox::AccountInfo> {}, SettingsList {"from_name"},
        "production"
    );
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_from_name_and_settings_does_not_containing_from_name_should_init_account_info_from_name) {
    auto options = MapOptions {};
    auto result = MapOptions {};
    result.single_settings["from_name"] = "Hello Kitty";
    addFromName(
        options,
        std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng", getBbDefaultAddresslItem()), SettingsList {"from_name"},
        "production"
    );
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_from_name_and_settings_does_not_containing_from_name_and_corp_should_init_account_info_from_name_eng) {
    auto options = MapOptions {};
    auto result = MapOptions {};
    result.single_settings["from_name"] = "HelloEng KittyEng";
    addFromName(
        options,
        std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng", getBbDefaultAddresslItem()), SettingsList {"from_name"},
        "corp"
    );
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_from_name_and_settings_does_not_containing_from_name_and_corp_and_empty_account_info_from_name_eng_should_init_account_info_from_name) {
    auto options = MapOptions {};
    auto result = MapOptions {};
    result.single_settings["from_name"] = "Hello Kitty";
    addFromName(
        options,
        std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "", "", getBbDefaultAddresslItem()), SettingsList {"from_name"},
        "corp"
    );
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_default_email_and_settings_containing_valid_email_default_email_does_not_change) {
    auto options = MapOptions {};
    options.single_settings["default_email"] = "hello@yandex.ru";
    auto result = MapOptions {};
    result.single_settings["default_email"] = "hello@yandex.ru";
    addDefaultEmail(options, std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng",
        getBbDefaultAddresslItem()), SettingsList {"default_email"});
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_default_email_and_empty_account_info_should_erase_default_email) {
    auto options = MapOptions {};
    options.single_settings["default_email"] = "hello@yandex.ru";
    auto result = MapOptions {};
    addDefaultEmail(options,
        std::shared_ptr<blackbox::AccountInfo> {}, SettingsList {"default_email"});
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_default_email_and_settings_containing_not_valid_email_should_replaced_valid_default_email) {
    auto options = MapOptions {};
    options.single_settings["default_email"] = "kitty@yandex.ru";
    auto result = MapOptions {};
    result.single_settings["default_email"] = "hello@yandex.ru";
    addDefaultEmail(options,
        std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng",
            getBbDefaultAddresslItem()), SettingsList {"default_email"});
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_emails_and_empty_account_should_not_add_emails) {
    auto options = MapOptions {};
    auto result = MapOptions {};
    addEmails(options,
        std::shared_ptr<blackbox::AccountInfo> {}, SettingsList {"emails"});
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
        for_settings_list_containing_emails_and_not_empty_account_should_add_emails) {
    auto options = MapOptions {};
    auto result = MapOptions {};
    result.emails = std::vector<Email> {{true, true, true, true, "hello@yandex.ru", "1974-01-01"}};
    addEmails(options,
        std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng",
            getBbDefaultAddresslItem()), SettingsList {"emails"});
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils,
    for_settings_list_not_containing_default_email_default_email_does_not_change) {
    auto options = MapOptions {};
    options.single_settings["default_email"] = "hello@yandex.ru";
    auto result = MapOptions {};
    result.single_settings["default_email"] = "hello@yandex.ru";
    addEmails(options,
        std::shared_ptr<blackbox::AccountInfo> {}, SettingsList {"tutu"});
    EXPECT_EQ(result, options);
}

TEST(TestApiUtils, for_not_empty_signatures_should_return_macs_signatures) {
    Signature signature1, signature2;
    signature1.text = "text1";
    signature1.lang = "lang1";
    signature1.associated_emails = {"emails1"};
    signature1.is_default = false;
    signature1.text_traits = TextTraits {1, 1};
    signature1.is_sanitize = false;
    signature2.text = "text2";
    signature2.lang = "lang2";
    signature2.associated_emails = {"emails2"};
    signature2.is_default = true;
    signature2.text_traits = TextTraits {2, 2};
    signature2.is_sanitize = true;

    SignaturesListOpt signaturesList =  SignaturesList {signature1, signature2};

    MacsSignaturesListOpt macsSignaturesList =  MacsSignaturesList {
        MacsSignature {"text1", "lang1", {"emails1"}, false, TextTraits {1, 1}},
        MacsSignature {"text2", "lang2", {"emails2"}, true, TextTraits {2, 2}}
    };
    MacsSignaturesListOpt result;
    getMacsSignatures(signaturesList, result);
    EXPECT_EQ(result, macsSignaturesList);
}

TEST(TestApiUtils, for_empty_signatures_should_return_empty_macs_signatures) {
    SignaturesListOpt signaturesList;
    MacsSignaturesListOpt result;
    getMacsSignatures(signaturesList, result);
    EXPECT_EQ(result, std::nullopt);
}

}
