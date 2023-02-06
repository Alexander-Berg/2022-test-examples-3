#include <gtest/gtest.h>
#include <mail/hound/include/internal/wmi/common/error_codes.h>

namespace {

using namespace ::testing;
using TestParams = std::pair<boost::system::error_code, libwmi::error::error_code>;

class ErrorConversionTest : public TestWithParam<TestParams> { };

TEST_P(ErrorConversionTest, should_convert_error_codes_to_wmi_errors) {
    const auto param = GetParam();
    EXPECT_EQ(errorCode2WmiError(param.first), param.second);
}

INSTANTIATE_TEST_SUITE_P(TestErrorConversionWithVariousCodes, ErrorConversionTest, Values(
        TestParams{ libwmi::error::internal, libwmi::error::internal },
        TestParams{ macs::error::duplicateLabelSymbol, libwmi::error::dbUniqueConstraint },
        TestParams{ macs::error::noSuchLabel, libwmi::error::noSuchLabel },
        TestParams{ macs::error::cantModifyLabel, libwmi::error::invalidArgument },
        TestParams{ macs::error::invalidArgument, libwmi::error::invalidArgument },
        TestParams{ macs::error::userNotInitialized, libwmi::error::notInitialized },
        TestParams{ macs::error::folderAlreadyExists, libwmi::error::dbUniqueConstraint },
        TestParams{ macs::error::noSuchFolder, libwmi::error::noSuchFolder },
        TestParams{ macs::error::folderCantBeParent, libwmi::error::invalidArgument },
        TestParams{ macs::error::cantModifyFolder, libwmi::error::invalidArgument },
        TestParams{ macs::error::folderIsNotEmpty, libwmi::error::folderNotEmpty },
        TestParams{ apq::error::sqlstate::read_only_sql_transaction, libwmi::error::dbReadOnly },
        TestParams{ macs::error::noSuchMessage, libwmi::error::noSuchMessage },
        TestParams{ apq::error::sqlstate::admin_shutdown, libwmi::error::dbUnknownError },
        TestParams{ sharpei::client::Errors::HttpCode, libwmi::error::invalidArgument },
        TestParams{ sharpei::client::Errors::UidNotFound, libwmi::error::uidNotFound },
        TestParams{ sharpei::client::Errors::RegistrationInProgress, libwmi::error::regInProgress },
        TestParams{ sharpei::client::Errors::Exception, libwmi::error::dbUnknownError },
        TestParams{ macs::error::noSuchTab, libwmi::error::noSuchTab }
));

}
