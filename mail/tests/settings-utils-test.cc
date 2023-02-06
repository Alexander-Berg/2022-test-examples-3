#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs/settings.h>
#include <macs/settings_utils.h>

namespace macs::settings {

static bool operator ==(const TextTraits& lhs, const TextTraits& rhs) {
    return lhs.code == rhs.code
        && lhs.lang == rhs.lang;
}

static bool operator ==(const Signature& lhs, const Signature& rhs) {
    return lhs.text == rhs.text
        && lhs.lang == rhs.lang
        && lhs.associated_emails == rhs.associated_emails
        && lhs.is_default == rhs.is_default
        && lhs.text_traits == rhs.text_traits;
}

}

namespace {

using namespace testing;
using namespace macs::settings;
using namespace macs::settings::utils;

TEST(TestpostgresqlUtils, for_empty_settings_list_settings_should_not_filtre) {
    SettingsMap settings;
    settings["optional"] = "off";
    filterSettings(settings, SettingsList {});
    EXPECT_THAT(
        settings,
        ContainerEq(SettingsMap {{"optional", "off"}})
    );
}

TEST(TestpostgresqlUtils, for_not_empty_settings_list_settings_should_filtre) {
    SettingsMap settings;
    settings["optional"] = "off";
    settings["version"] = "1";
    filterSettings(settings, SettingsList {"version"});
    EXPECT_THAT(
        settings,
        ContainerEq(SettingsMap {{"version", "1"}})
    );
}

TEST(TestpostgresqlUtils,
        for_settings_list_not_containing_signs_should_not_return_signs) {
    Signature signature;
    signature.text = "Mrs. Kitty";
    auto signatures = SignaturesListOpt {{signature}};
    auto profile = Profile {};
    addSignatures(profile, signatures, SettingsList {"tutu"});
    EXPECT_FALSE(profile.signs.has_value());
}

TEST(TestpostgresqlUtils,
        for_settings_list_containing_sings_should_return_signs) {
    Signature signature;
    signature.text = "Mrs. Kitty";
    auto signatures = SignaturesListOpt {{signature}};
    auto result = SignaturesListOpt {{signature}};
    auto profile = Profile {};
    addSignatures(profile, signatures, SettingsList {"signs"});
    EXPECT_EQ(profile.signs, result);
}

TEST(TestpostgresqlUtils, for_empty_settings_list_should_return_signs) {
    Signature signature;
    signature.text = "Mrs. Kitty";
    auto signatures = SignaturesListOpt {{signature}};
    auto result = SignaturesListOpt {{signature}};
    auto profile = Profile {};
    addSignatures(profile, signatures, SettingsList {});
    EXPECT_EQ(profile.signs, result);
}

}
