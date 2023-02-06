#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/hound/unistat/cpp/hound_meters.h>
#include <mail/hound/include/internal/wmi/errors.h>

using namespace ::testing;
using namespace ::unistat;

TEST(CountWmiErrors, shouldHaveZeroCountersWithUnexpectedJustAfterInit) {
    CountWmiErrors meter({libwmi::error::dbUnknownError, libwmi::error::noSuchMessage}, "signal");
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"signal_unknown_database_error_summ", 0},
        {"signal_no_such_message_summ", 0},
        {"signal_unexpected_error_summ", 0}
    }));
}

TEST(CountWmiErrors, shouldNotIncrementCountersWhenHaveNoLevelField) {
    CountWmiErrors meter({libwmi::error::dbUnknownError, libwmi::error::noSuchMessage}, "signal");

    const std::map<std::string, std::string> record = {
            {"wmi_code", "1"}
    };

    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"signal_unknown_database_error_summ", 0},
        {"signal_no_such_message_summ", 0},
        {"signal_unexpected_error_summ", 0}
    }));
}

TEST(CountWmiErrors, shouldNotIncrementCountersWhenLevelIsNotError) {
    CountWmiErrors meter({libwmi::error::dbUnknownError, libwmi::error::noSuchMessage}, "signal");

    const std::map<std::string, std::string> record = {
            {"level", "warning"},
            {"wmi_code", "1"}
    };

    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"signal_unknown_database_error_summ", 0},
        {"signal_no_such_message_summ", 0},
        {"signal_unexpected_error_summ", 0}
    }));
}

TEST(CountWmiErrors, shouldNotIncrementCountersWhenHaveNoWmiCodeField) {
    CountWmiErrors meter({libwmi::error::dbUnknownError, libwmi::error::noSuchMessage}, "signal");

    const std::map<std::string, std::string> record = {
            {"level", "error"}
    };

    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"signal_unknown_database_error_summ", 0},
        {"signal_no_such_message_summ", 0},
        {"signal_unexpected_error_summ", 0}
    }));
}

TEST(CountWmiErrors, shouldIncrementCountersForCodeWhenWmiCodeIsExpected) {
    CountWmiErrors meter({libwmi::error::dbUnknownError, libwmi::error::noSuchMessage}, "signal");

    const std::map<std::string, std::string> record = {
            {"level", "error"},
            {"wmi_code", std::to_string(libwmi::error::dbUnknownError)}
    };

    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"signal_unknown_database_error_summ", 1},
        {"signal_no_such_message_summ", 0},
        {"signal_unexpected_error_summ", 0}
    }));
}

TEST(CountWmiErrors, shouldIncrementCountersForUnexpectedWhenWmiCodeIsNotExpected) {
    CountWmiErrors meter({libwmi::error::dbUnknownError, libwmi::error::noSuchMessage}, "signal");

    const std::map<std::string, std::string> record = {
            {"level", "error"},
            {"wmi_code", "13"}
    };

    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"signal_unknown_database_error_summ", 0},
        {"signal_no_such_message_summ", 0},
        {"signal_unexpected_error_summ", 1}
    }));
}
