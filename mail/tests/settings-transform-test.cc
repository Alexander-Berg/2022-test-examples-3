#include <internal/settings/transform.h>

#include <boost/fusion/include/equal_to.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace macs::settings {

static bool operator ==(const TextTraits& lhs, const TextTraits& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const Signature& lhs, const Signature& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

static bool operator ==(const SettingsRaw& lhs, const SettingsRaw& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

}

namespace {

using namespace testing;
using namespace macs::pg;
using namespace macs::settings;

TEST(TestSettingsTransform, for_get_bad_data_settings_from_database_should_throw_exception) {
    const auto settings = reflection::Settings {std::string{"{"}};
    EXPECT_THROW(getSettings(settings), std::exception);
}

TEST(TestSettingsTransform, for_get_data_settings_from_database_should_return_settings_with_settings_and_signs) {
    const auto settings = reflection::Settings {
        std::string{R"({"signs":[{"associated_emails":[], "is_default":false,)"
        R"("text":"<div>-- </div><div>meow</div>"}],)"
        R"("single_settings":{"enable_social_notification":"on", "localize_imap":""}})"}
    };
    SettingsRaw result;
    result.signs = {
        Signature {"<div>-- </div><div>meow</div>", std::nullopt, std::vector<std::string> {}, false}
    };
    result.single_settings = SettingsMap {{"localize_imap", ""}, {"enable_social_notification", "on"}};
    EXPECT_EQ(*getSettings(settings), result);
}

}
