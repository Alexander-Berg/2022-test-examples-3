#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/york/unistat/cpp/york_meters.h>

using namespace ::testing;
using namespace ::unistat;

inline CountErrorCategories makeMeter() {
    return CountErrorCategories({
        {"first::category", "first_signal"},
        {"second::category", "second_signal"}
    }, "prefix");
}

TEST(CountErrorCategories, shouldHaveZeroCountersWithUnexpectedJustAfterInit) {
    CountErrorCategories meter = makeMeter();
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"prefix_first_signal_summ", 0},
        {"prefix_second_signal_summ", 0},
        {"prefix_unexpected_error_summ", 0}
    }));
}

TEST(CountErrorCategories, shouldNotIncrementCountersWhenHaveNoLevelField) {
    CountErrorCategories meter = makeMeter();
    const std::map<std::string, std::string> record = {
        {"error_code.category", "first::category"}
    };
    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"prefix_first_signal_summ", 0},
        {"prefix_second_signal_summ", 0},
        {"prefix_unexpected_error_summ", 0}
    }));
}

TEST(CountErrorCategories, shouldNotIncrementCountersWhenLevelIsNotError) {
    CountErrorCategories meter = makeMeter();
    const std::map<std::string, std::string> record = {
        {"level", "warning"},
        {"error_code.category", "first::category"}
    };
    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"prefix_first_signal_summ", 0},
        {"prefix_second_signal_summ", 0},
        {"prefix_unexpected_error_summ", 0}
    }));
}

TEST(CountErrorCategories, shouldNotIncrementCountersWhenHaveNoErrorCategoryField) {
    CountErrorCategories meter = makeMeter();
    const std::map<std::string, std::string> record = {
        {"level", "error"}
    };
    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"prefix_first_signal_summ", 0},
        {"prefix_second_signal_summ", 0},
        {"prefix_unexpected_error_summ", 0}
    }));
}

TEST(CountErrorCategories, shouldIncrementCountersForCategoryWhenCategoryIsExpected) {
    CountErrorCategories meter = makeMeter();
    const std::map<std::string, std::string> record = {
        {"level", "error"},
        {"error_code.category", "first::category"}
    };
    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"prefix_first_signal_summ", 1},
        {"prefix_second_signal_summ", 0},
        {"prefix_unexpected_error_summ", 0}
    }));
}

TEST(CountErrorCategories, shouldIncrementCountersForUnexpectedWhenWmiCategoryIsNotExpected) {
    CountErrorCategories meter = makeMeter();
    const std::map<std::string, std::string> record = {
        {"level", "error"},
        {"error_code.category", "unknown::category"}
    };
    meter.update(record);
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"prefix_first_signal_summ", 0},
        {"prefix_second_signal_summ", 0},
        {"prefix_unexpected_error_summ", 1}
    }));
}
