#pragma once

#include <gmock/gmock.h>
#include <macs/settings_repository.h>

struct MockSettingsRepository: public macs::SettingsRepository {
    MOCK_METHOD(void, asyncGetSettings, (macs::OnQuerySettings), (const, override));
    MOCK_METHOD(void, asyncInitSettings, (macs::settings::SettingsRaw, macs::Hook<std::int32_t>), (const, override));
    MOCK_METHOD(void, asyncUpdateSettings, (macs::settings::SettingsRaw, macs::Hook<std::int32_t>), (const, override));
    MOCK_METHOD(void, asyncDeleteSettings, (macs::Hook<std::int32_t>), (const, override));
    MOCK_METHOD(void, asyncEraseSettings, (macs::settings::SettingsList, macs::Hook<std::int32_t>), (const, override));
};

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

static bool operator ==(const Profile& lhs, const Profile& rhs) {
    return lhs.single_settings == rhs.single_settings
        && lhs.signs == rhs.signs;
}

static bool operator ==(const Parameters& lhs, const Parameters& rhs) {
    return lhs.single_settings == rhs.single_settings;
}

static bool operator ==(const Settings& lhs, const Settings& rhs) {
    return lhs.profile == rhs.profile
        && lhs.parameters == rhs.parameters;
}

static bool operator ==(const SettingsRaw& lhs, const SettingsRaw& rhs) {
    return lhs.signs == rhs.signs
        && lhs.single_settings == rhs.single_settings;
}

}
