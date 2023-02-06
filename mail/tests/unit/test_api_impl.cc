#include "settings_api_mocks.h"
#include "test_with_task_context.h"

#include <yamail/data/deserialization/json_reader.h>

#include <internal/blackbox/types_reflection.h>
#include <internal/common/error_code.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace std::string_literals;

using namespace testing;
using namespace settings;
using namespace settings::test;
using namespace settings::api;
using namespace yamail::data::deserialization;
using namespace settings::utfizer;
using SharpeiErrors = sharpei::client::Errors;
using PggCommonErrors = pgg::error::CommonErrors;

struct TestApiImpl: public TestWithTaskContext {
    std::shared_ptr<StrictMock<MockMacs>> mockMacs = std::make_shared<StrictMock<MockMacs>>();
    std::shared_ptr<StrictMock<MockBlackBoxImpl>> mockBlackBox = std::make_shared<StrictMock<MockBlackBoxImpl>>();
    std::shared_ptr<StrictMock<MockUserJournalImpl>> mockUserJournal = std::make_shared<StrictMock<MockUserJournalImpl>>();
    std::shared_ptr<StrictMock<MockUTFizerImpl>> utfizer = std::make_shared<StrictMock<MockUTFizerImpl>>();

    std::shared_ptr<api_impl> mockApi = std::make_shared<api_impl>(mockMacs, mockBlackBox, mockUserJournal, utfizer);
    blackbox::AddressList getBbDefaultAddresslItem() {
        const std::string response = R"({"users":[{"address-list":)"
        R"([{"validated":true,"default":true,"rpop":true,"silent":true,"unsafe":true,      "native":true,)"
        R"("born-date":"1974-01-01","address":"hello@yandex.ru"}]}]})";
        blackbox::InfoResponse infoResponse;
        yamail::data::deserialization::fromJson<blackbox::InfoResponse>(response, infoResponse);
        return infoResponse.users[0].addresses.value();
    }
};

MATCHER_P(EqMapOptionsPtr, other, "Equality matcher for shared_ptr for MapOptions") {
    return *arg == *other;
}

TEST_F(TestApiImpl, for_get_error_on_remove_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(*mockMacs, deleteSettings(_))
            .WillOnce(Return(make_unexpected(error_code(SharpeiErrors::UidNotFound))));
        const auto result = mockApi->remove(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), SharpeiErrors::UidNotFound);
    });
}

TEST_F(TestApiImpl, for_return_false_on_remove_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(*mockMacs, deleteSettings(_)).WillOnce(Return(false));
        const auto result = mockApi->remove(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::deleteError);
    });
}

TEST_F(TestApiImpl, for_success_remove_should_no_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(*mockMacs, deleteSettings(_)).WillOnce(Return(true));
        const auto result = mockApi->remove(context);
        ASSERT_TRUE(result);
    });
}

TEST_F(TestApiImpl, for_get_error_on_get_params_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(*mockMacs, getParameters(_, SettingsList {}))
            .WillOnce(Return(ByMove(make_unexpected(error_code(SharpeiErrors::UidNotFound)))));
        const auto result = mockApi->getParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), SharpeiErrors::UidNotFound);
    });
}

TEST_F(TestApiImpl, for_get_params_should_return_params) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        auto optionsReturn = MapOptions {};
        optionsReturn.single_settings["data"] = "data";
        auto returnParameters = Parameters {};
        returnParameters.single_settings["data"] = "data";
        EXPECT_CALL(*mockMacs, getParameters(_, SettingsList {}))
            .WillOnce(Return(ByMove(std::move(returnParameters))));
        const auto result = mockApi->getParams(context);
        ASSERT_TRUE(result);
        EXPECT_EQ(optionsReturn, result.value());
    });
}

TEST_F(TestApiImpl, for_successful_update_params_should_no_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        auto optionsForUpdate = MapOptions {};
        optionsForUpdate.single_settings["data"] = "data";
        auto parametersForUpdate = Parameters {};
        parametersForUpdate.single_settings["data"] = "data";
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(parametersForUpdate))))
            .WillOnce(Return(true));
        EXPECT_CALL(
            *mockUserJournal,
            asyncLogSettings(
                EqMapOptionsPtr(std::make_shared<MapOptions>(std::move(parametersForUpdate)))
            )
        );
        const auto result = mockApi->updateParams(context);
        ASSERT_TRUE(result);
    });
}

TEST_F(TestApiImpl, for_return_error_for_isUserExists_after_unsuccessful_update_params_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto parametersForUpdate = Parameters {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdate)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::blackBoxUserError)))));
        const auto result = mockApi->updateParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestApiImpl, for_error_on_init_settings_after_unsuccessful_update_params_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto parametersForUpdate = Parameters {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdate)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(make_unexpected(error_code(SharpeiErrors::UidNotFound))));
        const auto result = mockApi->updateParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), SharpeiErrors::UidNotFound);
    });
}

TEST_F(TestApiImpl, for_unsuccessful_update_params_after_successful_init_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto parametersForUpdateFirst = Parameters {};
        auto parametersForUpdateSecond = Parameters {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdateFirst)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(true));
        EXPECT_CALL(*mockMacs, updateParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdateSecond)))))
            .WillOnce(Return(false));
        const auto result = mockApi->updateParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::updateError);
    });
}

TEST_F(TestApiImpl, for_successful_update_params_after_successful_init_should_no_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        optionsForUpdate.single_settings["data"] = "data";
        auto parametersForUpdateFirst = Parameters {};
        parametersForUpdateFirst.single_settings["data"] = "data";
        auto parametersForUpdateSecond = Parameters {};
        parametersForUpdateSecond.single_settings["data"] = "data";
        auto optionsReturn = MapOptions {};
        optionsReturn.single_settings["data"] = "data";
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdateFirst)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(true));
        EXPECT_CALL(*mockMacs, updateParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(parametersForUpdateSecond))))
            .WillOnce(Return(true));
        EXPECT_CALL(
            *mockUserJournal,
            asyncLogSettings(
                EqMapOptionsPtr(std::make_shared<MapOptions>(std::move(parametersForUpdateSecond)))
            )
        );
        const auto result = mockApi->updateParams(context);
        ASSERT_TRUE(result);
    });
}

TEST_F(TestApiImpl, for_successful_update_protected_params_should_no_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        auto optionsForUpdate = MapOptions {};
        optionsForUpdate.single_settings["data"] = "data";
        auto parametersForUpdate = Parameters {};
        parametersForUpdate.single_settings["data"] = "data";
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProtectedParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(parametersForUpdate))))
            .WillOnce(Return(true));
        EXPECT_CALL(
            *mockUserJournal,
            asyncLogSettings(
                EqMapOptionsPtr(std::make_shared<MapOptions>(std::move(parametersForUpdate)))
            )
        );
        const auto result = mockApi->updateProtectedParams(context);
        ASSERT_TRUE(result);
    });
}

TEST_F(TestApiImpl, for_return_error_for_isUserExists_after_unsuccessful_update_protected_params_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto parametersForUpdate = Parameters {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProtectedParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdate)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::blackBoxUserError)))));
        const auto result = mockApi->updateProtectedParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestApiImpl, for_error_on_init_settings_after_unsuccessful_update_protected_params_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto parametersForUpdate = Parameters {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProtectedParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdate)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(make_unexpected(error_code(SharpeiErrors::UidNotFound))));
        const auto result = mockApi->updateProtectedParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), SharpeiErrors::UidNotFound);
    });
}

TEST_F(TestApiImpl, for_unsuccessful_update_protected_params_after_successful_init_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto parametersForUpdateFirst = Parameters {};
        auto parametersForUpdateSecond = Parameters {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProtectedParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdateFirst)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(true));
        EXPECT_CALL(*mockMacs, updateProtectedParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdateSecond)))))
            .WillOnce(Return(false));
        const auto result = mockApi->updateProtectedParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::updateError);
    });
}

TEST_F(TestApiImpl, for_successful_update_protected_params_after_successful_init_should_no_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        optionsForUpdate.single_settings["data"] = "data";
        auto parametersForUpdateFirst = Parameters {};
        parametersForUpdateFirst.single_settings["data"] = "data";
        auto parametersForUpdateSecond = Parameters {};
        parametersForUpdateSecond.single_settings["data"] = "data";
        auto optionsReturn = MapOptions {};
        optionsReturn.single_settings["data"] = "data";
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProtectedParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(std::move(parametersForUpdateFirst)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(true));
        EXPECT_CALL(*mockMacs, updateProtectedParameters(_,
            EqMapOptionsPtr(std::make_shared<Parameters>(parametersForUpdateSecond))))
            .WillOnce(Return(true));
        EXPECT_CALL(
            *mockUserJournal,
            asyncLogSettings(
                EqMapOptionsPtr(std::make_shared<MapOptions>(std::move(parametersForUpdateSecond)))
            )
        );
        const auto result = mockApi->updateProtectedParams(context);
        ASSERT_TRUE(result);
    });
}

TEST_F(TestApiImpl, for_get_black_box_error_on_get_profile_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        context->askValidator(true);
        EXPECT_CALL(*mockBlackBox, getAccountInfo(_, "1", "user_ip"))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::blackBoxUserError)))));
        const auto result = mockApi->getProfile(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestApiImpl, for_get_error_on_get_all_profile_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(*mockMacs, getProfile(_, SettingsList {}))
            .WillOnce(Return(ByMove(make_unexpected(error_code(SharpeiErrors::UidNotFound)))));
        const auto result = mockApi->getProfile(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), SharpeiErrors::UidNotFound);
    });
}

TEST_F(TestApiImpl, for_get_profile_should_return_profile) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        context->askValidator(true);
        auto optionsReturn = MapOptions {};
        Signature signature1;
        signature1.text = "Mrs. Kitty";
        signature1.text_traits = {1, 1};
        optionsReturn.signs = std::vector<Signature> {signature1};
        optionsReturn.single_settings["data"] = "data";
        optionsReturn.single_settings["from_name"] = "Hello Kitty";
        optionsReturn.single_settings["default_email"] = "hello@yandex.ru";
        optionsReturn.emails = std::vector<settings::Email> {
            {true, true, true, true, "hello@yandex.ru", "1974-01-01"}
        };
        auto returnProfile = Profile {};
        MacsSignature signature2;
        signature2.text = "Mrs. Kitty";
        returnProfile.signs = std::vector<MacsSignature> {signature2};
        returnProfile.single_settings["data"] = "data";
        EXPECT_CALL(*mockBlackBox, getAccountInfo(_, "1", "user_ip"))
            .WillOnce(Return(std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng", getBbDefaultAddresslItem())));
        EXPECT_CALL(*mockMacs, getProfile(_, SettingsList {}))
            .WillOnce(Return(ByMove(std::move(returnProfile))));
        EXPECT_CALL(*utfizer, utfize("Mrs. Kitty"))
            .WillOnce(Return("Mrs. Kitty"));
        EXPECT_CALL(*utfizer, recognize("Mrs. Kitty"))
            .WillOnce(Return(std::make_pair(1, 1)));
        const auto result = mockApi->getProfile(context);
        ASSERT_TRUE(result);
        EXPECT_EQ(optionsReturn, result.value());
    });
}

TEST_F(TestApiImpl, for_get_black_box_error_on_get_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        context->askValidator(true);
        EXPECT_CALL(*mockBlackBox, getAccountInfo(_, "1", "user_ip"))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::blackBoxUserError)))));
        const auto result = mockApi->get(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestApiImpl, for_get_error_on_get_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        EXPECT_CALL(*mockMacs, getSettings(_, SettingsList {}))
            .WillOnce(Return(ByMove(make_unexpected(error_code(SharpeiErrors::UidNotFound)))));
        const auto result = mockApi->get(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), SharpeiErrors::UidNotFound);
    });
}

TEST_F(TestApiImpl, for_get_should_return_settings) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        context->askValidator(true);
        auto optionsReturn = DoubleMapOptions {};
        Signature signature1;
        signature1.text = "Mrs. Kitty";
        signature1.text_traits = {1, 1};
        optionsReturn.profile.signs = std::vector<Signature> {signature1};
        optionsReturn.parameters.single_settings["data"] = "data";
        optionsReturn.profile.single_settings["from_name"] = "Hello Kitty";
        optionsReturn.profile.single_settings["default_email"] = "hello@yandex.ru";
        optionsReturn.profile.emails = std::vector<settings::Email> {
            {true, true, true, true, "hello@yandex.ru", "1974-01-01"}
        };
        auto returnSettings = Settings {};
        MacsSignature signature2;
        signature2.text = "Mrs. Kitty";
        returnSettings.profile.signs = std::vector<MacsSignature> {signature2};
        returnSettings.parameters.single_settings["data"] = "data";
        EXPECT_CALL(*mockBlackBox, getAccountInfo(_, "1", "user_ip"))
            .WillOnce(Return(std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng", getBbDefaultAddresslItem())));
        EXPECT_CALL(*mockMacs, getSettings(_, SettingsList {}))
            .WillOnce(Return(ByMove(std::move(returnSettings))));
        EXPECT_CALL(*utfizer, utfize("Mrs. Kitty"))
            .WillOnce(Return("Mrs. Kitty"));
        EXPECT_CALL(*utfizer, recognize("Mrs. Kitty"))
            .WillOnce(Return(std::make_pair(1, 1)));
        const auto result = mockApi->get(context);
        ASSERT_TRUE(result);
        EXPECT_EQ(optionsReturn, result.value());
    });
}

TEST_F(TestApiImpl, for_return_error_for_get_account_info_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        optionsForUpdate.single_settings["default_email"] = "tutu";
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockBlackBox, getAccountInfo(_, "1", "user_ip"))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::blackBoxUserError)))));
        const auto result = mockApi->updateProfile(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestApiImpl, for_not_valid_default_email_on_update_profile_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        optionsForUpdate.single_settings["default_email"] = "tutu";
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockBlackBox, getAccountInfo(_, "1", "user_ip"))
            .WillOnce(Return(std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng", getBbDefaultAddresslItem())));
        const auto result = mockApi->updateProfile(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxDefaultEmailError);
    });
}

TEST_F(TestApiImpl, for_successful_update_profile_should_no_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        optionsForUpdate.single_settings["default_email"] = "hello@yandex.ru";
        auto profileForUpdate = Profile {};
        profileForUpdate.single_settings["default_email"] = "hello@yandex.ru";
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockBlackBox, getAccountInfo(_, "1", "user_ip"))
            .WillOnce(Return(std::make_shared<blackbox::AccountInfo>("Hello", "Kitty", "HelloEng", "KittyEng", getBbDefaultAddresslItem())));
        EXPECT_CALL(*mockMacs, updateProfile(_,
            EqMapOptionsPtr(std::make_shared<Profile>(profileForUpdate))))
            .WillOnce(Return(true));
        EXPECT_CALL(
            *mockUserJournal,
            asyncLogSettings(
                EqMapOptionsPtr(std::make_shared<MapOptions>(std::move(profileForUpdate)))
            )
        );
        const auto result = mockApi->updateProfile(context);
        ASSERT_TRUE(result);
    });
}

TEST_F(TestApiImpl, for_return_error_for_isUserExists_after_unsuccessful_update_profile_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto profileForUpdate = Profile {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProfile(_,
            EqMapOptionsPtr(std::make_shared<Profile>(std::move(profileForUpdate)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::blackBoxUserError)))));
        const auto result = mockApi->updateProfile(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestApiImpl, for_error_on_init_settings_after_unsuccessful_update_profile_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto profileForUpdate = Profile {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProfile(_,
            EqMapOptionsPtr(std::make_shared<Profile>(std::move(profileForUpdate)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(make_unexpected(error_code(SharpeiErrors::UidNotFound))));
        const auto result = mockApi->updateProfile(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), SharpeiErrors::UidNotFound);
    });
}

TEST_F(TestApiImpl, for_unsuccessful_update_profile_after_successful_init_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        auto profileForUpdateFirst = Profile {};
        auto profileForUpdateSecond = Profile {};
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProfile(_,
            EqMapOptionsPtr(std::make_shared<Profile>(std::move(profileForUpdateFirst)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(true));
        EXPECT_CALL(*mockMacs, updateProfile(_,
            EqMapOptionsPtr(std::make_shared<Profile>(std::move(profileForUpdateSecond)))))
            .WillOnce(Return(false));
        const auto result = mockApi->updateProfile(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::updateError);
    });
}

TEST_F(TestApiImpl, for_successful_update_profile_after_successful_init_should_no_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->uid("1");
        auto optionsForUpdate = MapOptions {};
        optionsForUpdate.single_settings["data"] = "data";
        auto profileForUpdateFirst = Profile {};
        profileForUpdateFirst.single_settings["data"] = "data";
        auto profileForUpdateSecond = Profile {};
        profileForUpdateSecond.single_settings["data"] = "data";
        context->settings(std::make_shared<MapOptions>(std::move(optionsForUpdate)));
        EXPECT_CALL(*mockMacs, updateProfile(_,
            EqMapOptionsPtr(std::make_shared<Profile>(std::move(profileForUpdateFirst)))))
            .WillOnce(Return(false));
        EXPECT_CALL(*mockBlackBox, isUserExists(_, "1", "user_ip"))
            .WillOnce(Return(expected<void>()));
        EXPECT_CALL(*mockMacs, initSettings(_))
            .WillOnce(Return(true));
        EXPECT_CALL(*mockMacs, updateProfile(_,
            EqMapOptionsPtr(std::make_shared<Profile>(profileForUpdateSecond))))
            .WillOnce(Return(true));
        EXPECT_CALL(
            *mockUserJournal,
            asyncLogSettings(
                EqMapOptionsPtr(std::make_shared<MapOptions>(std::move(profileForUpdateSecond)))
            )
        );
        const auto result = mockApi->updateProfile(context);
        ASSERT_TRUE(result);
    });
}

TEST_F(TestApiImpl, for_get_error_on_delete_params_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->settingsList({"messages_avatars"});
        EXPECT_CALL(*mockMacs, eraseParameters(_, SettingsList {"messages_avatars"}))
            .WillOnce(Return(make_unexpected(error_code(SharpeiErrors::UidNotFound))));
        const auto result = mockApi->deleteParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), SharpeiErrors::UidNotFound);
    });
}

TEST_F(TestApiImpl, for_return_false_on_delete_params_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->settingsList({"messages_avatars"});
        EXPECT_CALL(*mockMacs, eraseParameters(_, SettingsList {"messages_avatars"}))
            .WillOnce(Return(false));
        const auto result = mockApi->deleteParams(context);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::deleteError);
    });
}

TEST_F(TestApiImpl, for_success_delete_params_should_no_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        context->settingsList({"messages_avatars"});
        EXPECT_CALL(*mockMacs, eraseParameters(_, SettingsList {"messages_avatars"}))
            .WillOnce(Return(true));
        const auto result = mockApi->deleteParams(context);
        ASSERT_TRUE(result);
    });
}

}
