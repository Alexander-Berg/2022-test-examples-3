#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/common/error_code.h>

namespace {

using namespace testing;
using namespace settings;
using SharpeiErrors = sharpei::client::Errors;
using PggCommonErrors = pgg::error::CommonErrors;

TEST(TestErrors, settings_UidNotFound_should_equivalent_sharpei_client_errors_UidNotFound) {
    EXPECT_TRUE(error_code(SharpeiErrors::UidNotFound) == Error::uidNotFound);
}

TEST(TestErrors, settings_UidNotFound_should_not_equivalent_sharpei_client_SharpeiError_errors) {
    EXPECT_FALSE(error_code(SharpeiErrors::SharpeiError) == Error::uidNotFound);
}

TEST(TestErrors, settings_noSettings_should_equivalent_pgg_errors_noDataReceived) {
    EXPECT_TRUE(error_code(PggCommonErrors::noDataReceived) == Error::noSettings);
}

TEST(TestErrors, settings_noSettings_should_not_equivalent_pgg_exceptionInHandler_errors) {
    EXPECT_FALSE(error_code(PggCommonErrors::exceptionInHandler) == Error::noSettings);
}

TEST(TestErrors, settings_blackBoxError_should_not_equivalent_pgg_exceptionInHandler_errors) {
    EXPECT_FALSE(error_code(PggCommonErrors::exceptionInHandler) == Error::blackBoxError);
}

}
