#include <macs/tests/mocking-settings.h>
#include <macs/settings_repository.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace testing;
using namespace macs;
using namespace macs::settings;
using namespace macs::error;

struct TestSettingsRepository : Test {
    std::shared_ptr<StrictMock<MockSettingsRepository>> repository =
        std::make_shared<StrictMock<MockSettingsRepository>>();
    Description description;
};

TEST_F(TestSettingsRepository, for_failed_get_settings_should_throw_exception) {
    const InSequence s;
    SettingsRaw databaseSettings;
    EXPECT_CALL(*repository, asyncGetSettings(_))
        .WillOnce(InvokeArgument<0>(
            error_code(Errors::logic),
            std::make_shared<SettingsRaw>(databaseSettings))
        );
    EXPECT_THROW(repository->getSettings(SettingsList {}), sys::system_error);
}

TEST_F(TestSettingsRepository, for_get_empty_settings_should_return_init_filtred_settings) {
    const InSequence s;
    SettingsRaw databaseSettings;
    Settings result;
    result.parameters.single_settings["messages_avatars"] = "on";
    result.profile.single_settings["ml_to_inbox"] = "on";
    EXPECT_CALL(*repository, asyncGetSettings(_)).WillOnce(
        InvokeArgument<0>(error_code(),
        std::make_shared<SettingsRaw>(databaseSettings))
    );
    EXPECT_EQ(*repository->getSettings(SettingsList {"ml_to_inbox", "messages_avatars"}), result);
}

TEST_F(TestSettingsRepository, for_successful_get_settings_should_return_filtred_settings) {
    const InSequence s;
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {
        {"enable_social_notification", "on"},
        {"ml_to_inbox", ""},
        {"localize_imap", ""},
        {"nik_name", "kitty cat"}
    };
    databaseSettings.signs = std::vector<Signature> {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    Settings result;
    result.parameters.single_settings["localize_imap"] = "";
    result.profile.single_settings["ml_to_inbox"] = "";
    result.profile.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    EXPECT_CALL(*repository, asyncGetSettings(_))
        .WillOnce(InvokeArgument<0>(
            error_code(),
            std::make_shared<SettingsRaw>(databaseSettings))
        );
    EXPECT_EQ(*repository->getSettings(
        SettingsList {"ml_to_inbox", "localize_imap", "signs"}), result);
}

TEST_F(TestSettingsRepository, for_failed_get_profile_should_throw_exception) {
    const InSequence s;
    SettingsRaw databaseSettings;
    EXPECT_CALL(*repository, asyncGetSettings(_))
        .WillOnce(InvokeArgument<0>(
            error_code(Errors::logic),
            std::make_shared<SettingsRaw>(databaseSettings))
        );
    EXPECT_THROW(repository->getProfile(SettingsList {}), sys::system_error);
}

TEST_F(TestSettingsRepository, for_not_initialized_profile_should_throw_exception) {
    const InSequence s;
    EXPECT_CALL(*repository, asyncGetSettings(_))
        .WillOnce(InvokeArgument<0>(error_code(), std::shared_ptr<SettingsRaw> {}));
    EXPECT_THROW(repository->getProfile(SettingsList {}), sys::system_error);
}

TEST_F(TestSettingsRepository, for_get_empty_profile_should_return_init_filtred_settings) {
    const InSequence s;
    SettingsRaw databaseSettings;
    Profile result;
    result.single_settings["ml_to_inbox"] = "on";
    EXPECT_CALL(*repository, asyncGetSettings(_)).WillOnce(
        InvokeArgument<0>(error_code(),
        std::make_shared<SettingsRaw>(databaseSettings))
    );
    EXPECT_EQ(*repository->getProfile(SettingsList {"ml_to_inbox", "nik_name"}), result);
}

TEST_F(TestSettingsRepository, for_successful_get_profile_should_return_filtred_settings) {
    const InSequence s;
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {
        {"enable_social_notification", "on"},
        {"ml_to_inbox", ""},
        {"localize_imap", ""},
        {"nik_name", "kitty cat"}
    };
    databaseSettings.signs = std::vector<Signature> {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    Profile result;
    result.single_settings["ml_to_inbox"] = "";
    result.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    EXPECT_CALL(*repository, asyncGetSettings(_))
        .WillOnce(InvokeArgument<0>(
            error_code(),
            std::make_shared<SettingsRaw>(databaseSettings))
        );
    EXPECT_EQ(*repository->getProfile(SettingsList {"ml_to_inbox", "nik_name", "signs"}), result);
}

TEST_F(TestSettingsRepository, for_failed_get_parameters_should_throw_exception) {
    const InSequence s;
    SettingsRaw databaseSettings;
    EXPECT_CALL(*repository, asyncGetSettings(_))
        .WillOnce(InvokeArgument<0>(
            error_code(Errors::logic),
            std::make_shared<SettingsRaw>(databaseSettings))
        );
    EXPECT_THROW(repository->getParameters(SettingsList {}), sys::system_error);
}

TEST_F(TestSettingsRepository, for_not_initialized_parameters_should_throw_exception) {
    const InSequence s;
    EXPECT_CALL(*repository, asyncGetSettings(_))
        .WillOnce(InvokeArgument<0>(error_code(), std::shared_ptr<SettingsRaw> {}));
    EXPECT_THROW(repository->getParameters(SettingsList {}), sys::system_error);
}

TEST_F(TestSettingsRepository, for_get_empty_parameters_should_return_init_parameters_with_filtration) {
    const InSequence s;
    SettingsRaw databaseSettings;
    Parameters result;
    result.single_settings["messages_avatars"] = "on";
    EXPECT_CALL(*repository, asyncGetSettings(_)).WillOnce(
        InvokeArgument<0>(error_code(),
        std::make_shared<SettingsRaw>(databaseSettings))
    );
    EXPECT_EQ(*repository->getParameters(
        SettingsList {"ml_to_inbox", "messages_avatars"}), result);
}

TEST_F(TestSettingsRepository, for_successful_get_parameters_should_return_filtred_settings) {
    const InSequence s;
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {
        {"enable_social_notification", "on"},
        {"ml_to_inbox", ""},
        {"localize_imap", ""},
        {"nik_name", "kitty cat"}
    };
    Parameters result;
    result.single_settings["nik_name"] = "kitty cat";
    EXPECT_CALL(*repository, asyncGetSettings(_)).WillOnce(
        InvokeArgument<0>(error_code(),
        std::make_shared<SettingsRaw>(databaseSettings))
    );
    EXPECT_EQ(*repository->getParameters(
        SettingsList {"ml_to_inbox", "nik_name"}), result);
}

TEST_F(TestSettingsRepository, for_failed_init_settings_should_throw_exception) {
    const InSequence s;
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = description.initSettings();
    EXPECT_CALL(*repository, asyncInitSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(Errors::logic), 1));
    EXPECT_THROW(repository->initSettings(), sys::system_error);
}

TEST_F(TestSettingsRepository, for_successful_init_settings_should_return_true) {
    const InSequence s;
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = description.initSettings();
    EXPECT_CALL(*repository, asyncInitSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(), 1));
    EXPECT_TRUE(repository->initSettings());
}

TEST_F(TestSettingsRepository, for_unsuccessful_init_settings_should_return_false) {
    const InSequence s;
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = description.initSettings();
    EXPECT_CALL(*repository, asyncInitSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(), 0));
    EXPECT_FALSE(repository->initSettings());
}

TEST_F(TestSettingsRepository, for_update_parameters_with_profile_settings_should_throw_exception) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["ml_to_inbox"] = "";
    EXPECT_THROW(
        repository->updateParameters(std::make_shared<Parameters>(std::move(parameters))),
        sys::system_error
    );
}

TEST_F(TestSettingsRepository, for_update_parameters_with_protected_parameters_settings_should_throw_exception) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["priority_mail"] = "";
    EXPECT_THROW(
        repository->updateParameters(std::make_shared<Parameters>(std::move(parameters))),
        sys::system_error
    );
}

TEST_F(TestSettingsRepository, for_failed_update_parameters_should_throw_exception) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["localize_imap"] = "on";
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"localize_imap", "on"}};
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(Errors::logic), 1));
    EXPECT_THROW(
        repository->updateParameters(std::make_shared<Parameters>(std::move(parameters))),
        sys::system_error
    );
}

TEST_F(TestSettingsRepository, for_unsuccessful_update_parameters_should_return_false) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["localize_imap"] = "on";
    parameters.single_settings["nik_name"] = "kitty cat";
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"localize_imap", "on"}, {"nik_name", "kitty cat"}};
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(), 0));
    EXPECT_FALSE(
        repository->updateParameters(std::make_shared<Parameters>(std::move(parameters)))
    );
}

TEST_F(TestSettingsRepository, for_successful_update_parameters_should_return_true) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["localize_imap"] = "on";
    parameters.single_settings["nik_name"] = "kitty cat";
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"localize_imap", "on"}, {"nik_name", "kitty cat"}};
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(), 1));
    EXPECT_TRUE(
        repository->updateParameters(std::make_shared<Parameters>(std::move(parameters)))
    );
}

TEST_F(TestSettingsRepository, for_update_protected_parameters_with_profile_settings_should_throw_exception) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["ml_to_inbox"] = "";
    EXPECT_THROW(
        repository->updateProtectedParameters(std::make_shared<Parameters>(std::move(parameters))),
        sys::system_error
    );
}

TEST_F(TestSettingsRepository, for_failed_update_protected_parameters_should_throw_exception) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["localize_imap"] = "on";
    parameters.single_settings["priority_mail"] = "";
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"localize_imap", "on"}, {"priority_mail", ""}};
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(Errors::logic), 1));
    EXPECT_THROW(
        repository->updateProtectedParameters(std::make_shared<Parameters>(std::move(parameters))),
        sys::system_error
    );
}

TEST_F(TestSettingsRepository, for_unsuccessful_update_protected_parameters_should_return_false) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["localize_imap"] = "on";
    parameters.single_settings["nik_name"] = "kitty cat";
    parameters.single_settings["priority_mail"] = "";
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"localize_imap", "on"}, {"nik_name", "kitty cat"}, {"priority_mail", ""}};
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(), 0));
    EXPECT_FALSE(
        repository->updateProtectedParameters(std::make_shared<Parameters>(std::move(parameters)))
    );
}

TEST_F(TestSettingsRepository, for_successful_update_protected_parameters_should_return_true) {
    const InSequence s;
    Parameters parameters;
    parameters.single_settings["localize_imap"] = "on";
    parameters.single_settings["nik_name"] = "kitty cat";
    parameters.single_settings["priority_mail"] = "";
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"localize_imap", "on"}, {"nik_name", "kitty cat"}, {"priority_mail", ""}};
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(), 1));
    EXPECT_TRUE(
        repository->updateProtectedParameters(std::make_shared<Parameters>(std::move(parameters)))
    );
}

TEST_F(TestSettingsRepository, for_failed_update_profile_should_throw_exception) {
    const InSequence s;
    Profile profile;
    profile.single_settings["enable_social_notification"] = "on";
    profile.single_settings["ml_to_inbox"] = "off";
    profile.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"enable_social_notification", "on"}, {"ml_to_inbox", ""}};
    databaseSettings.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(Errors::logic), 1));
    EXPECT_THROW(
        repository->updateProfile(std::make_shared<Profile>(std::move(profile))),
        sys::system_error
    );
}

TEST_F(TestSettingsRepository, for_unsuccessful_update_profile_should_return_false) {
    const InSequence s;
    Profile profile;
    profile.single_settings["enable_social_notification"] = "on";
    profile.single_settings["ml_to_inbox"] = "off";
    profile.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"enable_social_notification", "on"}, {"ml_to_inbox", ""}};
    databaseSettings.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(), 0));
    EXPECT_FALSE(
        repository->updateProfile(std::make_shared<Profile>(std::move(profile)))
    );
}

TEST_F(TestSettingsRepository, for_successful_update_profile_should_return_false) {
    const InSequence s;
    Profile profile;
    profile.single_settings["enable_social_notification"] = "on";
    profile.single_settings["ml_to_inbox"] = "off";
    profile.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    const auto profilePtr = std::make_shared<Profile>(std::move(profile));
    SettingsRaw databaseSettings;
    databaseSettings.single_settings = SettingsMap {{"enable_social_notification", "on"}, {"ml_to_inbox", ""}};
    databaseSettings.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    EXPECT_CALL(*repository, asyncUpdateSettings(databaseSettings, _)
    ).WillOnce(InvokeArgument<1>(error_code(), 1));
    EXPECT_TRUE(repository->updateProfile(profilePtr));
    EXPECT_THAT(
        profilePtr->single_settings,
        ElementsAre(
            Pair("enable_social_notification", "on"),
            Pair("ml_to_inbox", "")
        )
    );
}

TEST_F(TestSettingsRepository, for_erase_profile_should_throw_exception) {
    const InSequence s;
    const auto settingsList = SettingsList {"abook_page_size"};
    EXPECT_THROW(repository->eraseParameters(SettingsList {"abook_page_size"}), sys::system_error);
}

TEST_F(TestSettingsRepository, for_failed_erase_parameters_should_throw_exception) {
    const InSequence s;
    const auto settingsList = SettingsList {"messages_avatars"};
    EXPECT_CALL(*repository, asyncEraseSettings(settingsList, _))
        .WillOnce(InvokeArgument<1>(error_code(Errors::logic), 1));
    EXPECT_THROW(repository->eraseParameters(SettingsList {"messages_avatars"}), sys::system_error);
}

TEST_F(TestSettingsRepository, for_successful_erase_parameters_should_throw_exception) {
    const InSequence s;
    const auto settingsList = SettingsList {"messages_avatars"};
    EXPECT_CALL(*repository, asyncEraseSettings(settingsList, _))
        .WillOnce(InvokeArgument<1>(error_code(), 1));
    EXPECT_EQ(repository->eraseParameters(SettingsList {"messages_avatars"}), true);
}

TEST_F(TestSettingsRepository, for_unsuccessful_erase_parameters_should_throw_exception) {
    const InSequence s;
    const auto settingsList = SettingsList {"messages_avatars"};
    EXPECT_CALL(*repository, asyncEraseSettings(settingsList, _))
        .WillOnce(InvokeArgument<1>(error_code(), 0));
    EXPECT_EQ(repository->eraseParameters(SettingsList {"messages_avatars"}), false);
}

}
