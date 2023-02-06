#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs/settings_description.h>
#include <macs/settings_queries_types.h>

#include <library/cpp/testing/unittest/tests_data.h>

#include <yamail/data/deserialization/json_reader.h>

#include <boost/fusion/adapted.hpp>

#include <fstream>

BOOST_FUSION_ADAPT_STRUCT(macs::settings::SettingsRaw,
    single_settings
)

namespace {

using namespace testing;
using namespace macs;
using namespace macs::settings;

struct TestSettingsDescriptionGetProfile: TestWithParam<std::tuple<SettingsMap, SettingsMap>> {
    Description description;
};

TEST_P(TestSettingsDescriptionGetProfile, test_get_profile) {
    EXPECT_THAT(
        description.getProfile(std::get<0>(GetParam())),
        ContainerEq(std::get<1>(GetParam()))
    );
}

INSTANTIATE_TEST_SUITE_P(
    test_get_profile,
    TestSettingsDescriptionGetProfile,
    Values(
        std::make_tuple(
            SettingsMap {{"messages_per_page", "30"}, {"ml_to_inbox", "on"}, {"localize_imap", ""}, {"u2709", "on"}},
            SettingsMap {{"messages_per_page", "30"}, {"ml_to_inbox", "on"}}
        ),
        std::make_tuple(
            SettingsMap {{"localize_imap", ""}, {"u2709", "on"}},
            SettingsMap {}
        ),
        std::make_tuple(
            SettingsMap {{"Hello", "Kitty"}, {"translate", "on"}},
            SettingsMap {{"translate", "on"}}
        )
    )
);

struct TestSettingsDescriptionGetParameters: TestWithParam<std::tuple<SettingsMap, SettingsMap>> {
    Description description;
};

TEST_P(TestSettingsDescriptionGetParameters, test_get_parameters) {
    EXPECT_THAT(
        description.getParameters(std::get<0>(GetParam())),
        ContainerEq(std::get<1>(GetParam()))
    );
}

INSTANTIATE_TEST_SUITE_P(
    test_get_parameters,
    TestSettingsDescriptionGetParameters,
    Values(
        std::make_tuple(
            SettingsMap {{"messages_per_page", "30"}, {"ml_to_inbox", "on"}, {"localize_imap", ""}, {"u2709", "on"}},
            SettingsMap {{"localize_imap", ""}, {"u2709", "on"}}
        ),
        std::make_tuple(
            SettingsMap {{"messages_per_page", "30"}, {"ml_to_inbox", "on"}},
            SettingsMap {}
        ),
        std::make_tuple(
            SettingsMap {{"Hello", "Kitty"}, {"translate", "on"}},
            SettingsMap {{"Hello", "Kitty"}}
        )
    )
);

struct TestSettingsDescriptionIsParameters: Test{
    Description description;
};

struct TestSettingsDescriptionIsProtectedParameters: Test{
    Description description;
};

TEST_F(TestSettingsDescriptionIsParameters, for_settings_that_have_not_only_parameters_should_return_false) {
    EXPECT_FALSE(
        description.isParameters(
            SettingsMap {{"messages_per_page", "30"}, {"ml_to_inbox", "on"}, {"localize_imap", ""}, {"u2709", "on"}})
    );
}

TEST_F(TestSettingsDescriptionIsParameters, for_settings_that_have_only_parameters_should_return_true) {
    EXPECT_TRUE(
        description.isParameters(
            SettingsMap {{"localize_imap", ""}, {"hello", "kitty"}}
        )
    );
}

TEST_F(TestSettingsDescriptionIsProtectedParameters, for_settings_what_have_unprotected_parameters_and_profile_should_return_false) {
    EXPECT_FALSE(
        description.isProtectedParameters(
            SettingsMap {{"messages_per_page", "30"}, {"ml_to_inbox", "on"}, {"localize_imap", ""}, {"u2709", "on"}})
    );
}

TEST_F(TestSettingsDescriptionIsProtectedParameters, for_settings_what_have_not_ptotected_parameters_should_return_false) {
    EXPECT_FALSE(
        description.isProtectedParameters(
            SettingsMap {{"localize_imap", ""}, {"hello", "kitty"}}
        )
    );
}

TEST_F(TestSettingsDescriptionIsProtectedParameters, for_settings_what_have_protected_parameters_should_return_true) {
    EXPECT_TRUE(
        description.isProtectedParameters(
            SettingsMap {{"localize_imap", ""}, {"hello", "kitty"}, {"priority_mail", ""}}
        )
    );
}

struct TestSettingsDescriptionValidate: Test {
    Description description;
};

TEST_F(TestSettingsDescriptionValidate, for_input_settings_should_return_valid_description) {
    EXPECT_THAT(
        description.validateProfile(
            SettingsMap {
                {"messages_per_page", "40"},
                {"messages_avatars", "on"},
                {"no_firstline", "yes"},
                {"u2709", "on"}
            }
        ),
        ContainerEq(SettingsMap {{"messages_per_page", "40"}})
    );
}

TEST_F(TestSettingsDescriptionValidate, for_setFlag_with_on_should_set_flag_on) {
    EXPECT_EQ(description.setFlag("on"), std::string("on"));
}

TEST_F(TestSettingsDescriptionValidate, for_setFlag_with_not_on_should_return_empty_string) {
    EXPECT_EQ(description.setFlag("off"), std::string());
}

using ReturnType = SettingsMap::value_type;
using InputType = std::pair<Description::Restrictions::const_iterator, ReturnType>;

struct TestValidateSetting: Test {
    Description description;

    auto find(const std::string& setting) {
        return description.getRestrictions().profile.find(setting);
    }
};

TEST_F(TestValidateSetting,
        for_setting_with_no_restrictions_should_return_setting_whitout_changes) {
    const auto setting = ReturnType {"mobile_sign", "Hello Kitty"};
    const auto ret = ReturnType {"mobile_sign", "Hello Kitty"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"mobile_sign", "Hello Kitty"})
    );
}

TEST_F(TestValidateSetting,
        for_flag_setting_with_not_valid_value_should_return_setting_with_empty_string_value) {
    const auto setting = ReturnType {"ml_to_inbox", "off"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"ml_to_inbox", ""})
    );
}

TEST_F(TestValidateSetting,
        for_flag_setting_with_valid_value_should_return_setting_whitout_changes) {
    const auto setting = ReturnType {"ml_to_inbox", "on"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"ml_to_inbox", "on"})
    );
}

TEST_F(TestValidateSetting,
        for_char_setting_with_value_less_than_minimum_should_return_setting_with_init_value) {
    const auto setting = ReturnType {"quotation_char", " "};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"quotation_char", ">"})
    );
}

TEST_F(TestValidateSetting,
        for_char_setting_with_value_greater_than_maximum_should_return_setting_with_init_value) {
    const auto setting = ReturnType {"quotation_char", "й"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"quotation_char", ">"})
    );
}

TEST_F(TestValidateSetting,
        for_char_setting_with_valid_value_should_return_setting_with_it_value) {
    const auto setting = ReturnType {"quotation_char", "!"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"quotation_char", "!"})
    );
}

TEST_F(TestValidateSetting,
        for_char_setting_with_not_char_value_should_return_setting_with_init_value) {
    const auto setting = ReturnType {"quotation_char", "tutu"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"quotation_char", ">"})
    );
}

TEST_F(TestValidateSetting,
        for_numerical_setting_with_value_less_than_minimum_should_return_setting_with_init_value) {
    const auto setting = ReturnType {"messages_per_page", "0"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"messages_per_page", "30"})
    );
}

TEST_F(TestValidateSetting,
        for_numerical_setting_with_value_greater_than_maximum_should_return_setting_with_init_value) {
    const auto setting = ReturnType {"messages_per_page", "300"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"messages_per_page", "30"})
    );
}

TEST_F(TestValidateSetting,
        for_numerical_setting_with_invalid_value_should_return_setting_with_init_value) {
    const auto setting = ReturnType {"messages_per_page", "k"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"messages_per_page", "30"})
    );
}

TEST_F(TestValidateSetting,
        for_numerical_setting_with_valid_value_should_return_setting_with_it_value) {
    const auto setting = ReturnType {"messages_per_page", "40"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"messages_per_page", "40"})
    );
}

TEST_F(TestValidateSetting,
        for_list_setting_with_value_from_values_list_should_return_setting_with_it_value) {
    const auto setting = ReturnType {"skin_name", "neo2"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"skin_name", "neo2"})
    );
}

TEST_F(TestValidateSetting,
        for_list_setting_with_value_not_from_values_list_should_return_setting_with_init_value) {
    const auto setting = ReturnType {"skin_name", "hello_kitty"};
    EXPECT_EQ(
        description.validateSetting(InputType {find(setting.first), setting}),
        (ReturnType {"skin_name", "neo2"})
    );
}


TEST(TestInitParameters, for_initParameters_should_return_parameters_with_init_value) {
    Description description;
    EXPECT_EQ(description.initParameters().at("flight_notify"), "true");
}


TEST(TestInitProfile, for_production_initProfile_should_return_profile_with_init_value) {
    Description description;
    EXPECT_EQ(description.initProfile().at("default_mailbox"), "yandex.ru");
}

TEST(TestInitProfile, for_production_initSettings_should_return_settings_with_init_value) {
    Description description;
    EXPECT_EQ(description.initSettings().at("flight_notify"), "true");
    EXPECT_EQ(description.initSettings().at("default_mailbox"), "yandex.ru");
}

TEST(TestInitParameters, for_production_initParameters_should_return_parameters_with_init_value) {
    Description description;
    description.setMode(Mode::corp);
    EXPECT_EQ(description.initParameters().at("is_node_user"), "on");
}


TEST(TestInitProfile, for_corp_initProfile_should_return_profile_with_init_value) {
    Description description;
    description.setMode(Mode::corp);
    EXPECT_EQ(description.initProfile().at("default_mailbox"), "yandex-team.ru");
}

TEST(TestInitProfile, for_corp_initSettings_should_return_settings_with_init_value) {
    Description description;
    description.setMode(Mode::corp);
    EXPECT_EQ(description.initSettings().at("is_node_user"), "on");
    EXPECT_EQ(description.initSettings().at("default_mailbox"), "yandex-team.ru");
}

TEST(TestInitProfile, for_production_profileRestrictions_should_return_profile_restrictions) {
    Description description;
    EXPECT_EQ(description.getRestrictions().profile.at("default_mailbox").onInit, "yandex.ru");
}

TEST(TestInitProfile, for_corp_profileRestrictions_should_return_profile_restrictions) {
    Description description;
    description.setMode(Mode::corp);
    EXPECT_EQ(description.getRestrictions().profile.at("default_mailbox").onInit, "yandex-team.ru");
}

SettingsRaw getStandardInitSettings(const char* name) {
    const TString settingsFile = GetWorkPath() + '/' + name;
    std::ifstream initialDescriptionFile(settingsFile.data());
    using yamail::data::deserialization::fromJson;
    return fromJson<SettingsRaw>(
        std::string {
            std::istreambuf_iterator<char> {initialDescriptionFile},
            std::istreambuf_iterator<char> {}
        }
    );
}

TEST(TestStandardInitSettings, production_initial_settings_should_be_equal_standard_initial_settings) {
    Description description;
    description.setMode(Mode::production);
    const SettingsRaw standardInitialSettings
        = getStandardInitSettings("init_production_settings.json");
    ASSERT_TRUE(standardInitialSettings.single_settings);
    EXPECT_THAT(
        description.initSettings(),
        ContainerEq(standardInitialSettings.single_settings.value())
    );
}

TEST(TestStandardInitSettings, corp_initial_settings_should_be_equal_standard_initial_settings) {
    Description description;
    description.setMode(Mode::corp);
    const SettingsRaw standardInitialSettings
        = getStandardInitSettings("init_corp_settings.json");
    ASSERT_TRUE(standardInitialSettings.single_settings);
    EXPECT_THAT(
        description.initSettings(),
        ContainerEq(standardInitialSettings.single_settings.value())
    );
}

}
