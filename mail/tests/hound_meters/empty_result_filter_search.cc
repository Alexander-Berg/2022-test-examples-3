#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/hound/unistat/cpp/hound_meters.h>

using namespace ::testing;
using namespace ::unistat;

TEST(EmptyResultFilterSearch, shouldHaveZeroValueJustAfterInit) {
    EmptyResultFilterSearch meter("prefix_");
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"prefix_filter_search_empty_result_summ", 0}));
}

TEST(EmptyResultFilterSearch, shouldNotIncrementValueWhenWhereNameIsNotFilterSearch) {
    EmptyResultFilterSearch meter("prefix_");

    const std::map<std::string, std::string> record = {
            {"where_name", "counters"},
            {"error_code.value", "5022"}
    };

    meter.update(record);
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"prefix_filter_search_empty_result_summ", 0}));
}

TEST(EmptyResultFilterSearch, shouldNotIncrementValueWhenErrorCodeIsNot5022) {
    EmptyResultFilterSearch meter("prefix_");

    const std::map<std::string, std::string> record = {
            {"where_name", "filter_search"},
            {"error_code.value", "100500"}
    };

    meter.update(record);
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"prefix_filter_search_empty_result_summ", 0}));
}

TEST(EmptyResultFilterSearch, shouldIncrementValueWhenWhereNameIsFilterSearchAndErrorCodeIs5022) {
    EmptyResultFilterSearch meter("prefix_");

    const std::map<std::string, std::string> record = {
            {"where_name", "filter_search"},
            {"error_code.value", "5022"}
    };

    meter.update(record);
    EXPECT_EQ(meter.get(), (NamedValue<std::size_t>{"prefix_filter_search_empty_result_summ", 1}));
}
