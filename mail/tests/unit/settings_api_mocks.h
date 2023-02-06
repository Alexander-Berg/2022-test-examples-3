#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/common/context.h>
#include <internal/common/types_reflection.h>
#include <internal/macs/impl.h>
#include <internal/api/impl.h>
#include <internal/blackbox/impl.h>
#include <internal/recognizer/interface.h>

#include <boost/fusion/include/equal_to.hpp>


namespace macs::settings {

static bool operator ==(const TextTraits& lhs, const TextTraits& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const Signature& lhs, const Signature& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const Profile& lhs, const Profile& rhs) {
    return lhs.single_settings == rhs.single_settings
        && lhs.signs == rhs.signs;
}

static bool operator ==(const Parameters& lhs, const Parameters& rhs) {
    return lhs.single_settings == rhs.single_settings;
}

}

namespace settings {
namespace test {

using MacsPtr = api::MacsPtr;
using BlackBoxPtr = api::BlackBoxPtr;
using UserJournalPtr = api::UserJournalPtr;
using SettingsList = settings::SettingsList;
using Settings = settings::Settings;
using Profile = settings::Profile;
using Parameters = settings::Parameters;
using ProfilePtr = settings::ProfilePtr;
using ParametersPtr = settings::ParametersPtr;

struct MockMacs: macs::Interface {
    MOCK_METHOD(expected<Settings>, getSettings, (ContextPtr, SettingsList), (override));
    MOCK_METHOD(expected<Profile>, getProfile, (ContextPtr, SettingsList), (override));
    MOCK_METHOD(expected<Parameters>, getParameters, (ContextPtr, SettingsList), (override));
    MOCK_METHOD(expected<bool>, updateParameters, (ContextPtr, ParametersPtr), (override));
    MOCK_METHOD(expected<bool>, updateProtectedParameters, (ContextPtr, ParametersPtr), (override));
    MOCK_METHOD(expected<bool>, updateProfile, (ContextPtr, ProfilePtr), (override));
    MOCK_METHOD(expected<bool>, initSettings, (ContextPtr), (override));
    MOCK_METHOD(expected<bool>, deleteSettings, (ContextPtr), (override));
    MOCK_METHOD(expected<bool>, eraseParameters, (ContextPtr, SettingsList), (override));
};

struct MockBlackBoxImpl: blackbox::Interface {
    MOCK_METHOD(expected<void>, isUserExists, (ContextPtr, const std::string&,
        const std::string&), (const, override));
    MOCK_METHOD(expected<blackbox::AccountInfoPtr>, getAccountInfo, (ContextPtr, const std::string&,
        const std::string&), (const, override));
};

struct MockUserJournalImpl: user_journal::Interface {
    MOCK_METHOD(void, asyncLogSettings, (std::shared_ptr<MapOptions>), ());
    void asyncLogSettingsUpdate(ContextPtr, MapOptions options) {
        asyncLogSettings(std::make_shared<MapOptions>(std::move(options)));
    };
    void asyncLogSettingsUpdate(ContextPtr, SettingsMap) {};
    void asyncLogSettingsUpdate(ContextPtr, SignaturesList) {};
};

struct MockUTFizerImpl: utfizer::Interface {
    MOCK_METHOD(std::string, utfize, (const std::string&), (const, override));
    MOCK_METHOD((std::pair<std::int32_t, std::int32_t>), recognize, (const std::string&), (const, override));
};

} //test

static bool operator ==(const Signature& lhs, const Signature& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const Email& lhs, const Email& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const MapOptions& lhs, const MapOptions& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const DoubleMapOptions& lhs, const DoubleMapOptions& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

} // namespace settings
