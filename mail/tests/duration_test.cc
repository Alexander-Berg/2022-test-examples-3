#include <gtest/gtest.h>

#include <yamail/data/reflection.h>
#include <yamail/data/serialization/yajl.h>
#include <yamail/data/deserialization/yajl.h>

using namespace testing;
using namespace yamail::data::serialization;
using namespace yamail::data::deserialization;

namespace yr = yamail::data::reflection;

using namespace std::literals;

using Days = std::chrono::duration<int, std::ratio<60*60*24>>;

TEST(to_string, with_nanoseconds_should_return_duration_with_ns_units) {
    EXPECT_EQ(yr::to_string(std::chrono::nanoseconds(5)), "5ns"s);
}

TEST(to_string, with_microseconds_should_return_duration_with_us_units) {
    EXPECT_EQ(yr::to_string(std::chrono::microseconds(5)), "5us"s);
}

TEST(to_string, with_milliseconds_should_return_duration_with_ms_units) {
    EXPECT_EQ(yr::to_string(std::chrono::milliseconds(5)), "5ms"s);
}

TEST(to_string, with_seconds_should_return_duration_with_s_units) {
    EXPECT_EQ(yr::to_string(std::chrono::seconds(5)), "5s"s);
}

TEST(to_string, with_minutes_should_return_duration_with_m_units) {
    EXPECT_EQ(yr::to_string(std::chrono::minutes(5)), "5m"s);
}

TEST(to_string, with_hours_should_return_duration_with_h_units) {
    EXPECT_EQ(yr::to_string(std::chrono::hours(5)), "5h"s);
}

TEST(to_string, with_unknown_units_should_return_duration_wthout_units) {
    EXPECT_EQ(yr::to_string(Days(5)), "5"s);
}


TEST(from_string, with_ns_units_should_return_nanoseconds) {
    EXPECT_EQ(yr::from_string<std::chrono::nanoseconds>("5ns"s), std::chrono::nanoseconds(5));
}

TEST(from_string, with_not_ns_units_should_throw) {
    EXPECT_THROW(yr::from_string<std::chrono::nanoseconds>("5tz"s), std::runtime_error);
}

TEST(from_string, with_us_units_should_return_microseconds) {
    EXPECT_EQ(yr::from_string<std::chrono::microseconds>("5us"s), std::chrono::microseconds(5));
}

TEST(from_string, with_not_us_units_should_throw) {
    EXPECT_THROW(yr::from_string<std::chrono::microseconds>("5tz"s), std::runtime_error);
}

TEST(from_string, with_ms_units_should_return_milliseconds) {
    EXPECT_EQ(yr::from_string<std::chrono::milliseconds>("5ms"s), std::chrono::milliseconds(5));
}

TEST(from_string, with_not_ms_units_should_throw) {
    EXPECT_THROW(yr::from_string<std::chrono::milliseconds>("5tz"s), std::runtime_error);
}

TEST(from_string, with_s_units_should_return_seconds) {
    EXPECT_EQ(yr::from_string<std::chrono::seconds>("5s"s), std::chrono::seconds(5));
}

TEST(from_string, with_not_s_units_should_throw) {
    EXPECT_THROW(yr::from_string<std::chrono::seconds>("5tz"s), std::runtime_error);
}

TEST(from_string, with_m_units_should_return_minutes) {
    EXPECT_EQ(yr::from_string<std::chrono::minutes>("5m"s), std::chrono::minutes(5));
}

TEST(from_string, with_not_m_units_should_throw) {
    EXPECT_THROW(yr::from_string<std::chrono::minutes>("5tz"s), std::runtime_error);
}

TEST(from_string, with_h_units_should_return_hours) {
    EXPECT_EQ(yr::from_string<std::chrono::hours>("5h"s), std::chrono::hours(5));
}

TEST(from_string, with_not_h_units_should_throw) {
    EXPECT_THROW(yr::from_string<std::chrono::hours>("5tz"s), std::runtime_error);
}

TEST(from_string, with_bad_value_should_throw) {
    EXPECT_THROW(yr::from_string<std::chrono::hours>("q5h"s), std::runtime_error);
}

TEST(from_string, for_duration_without_units_should_return_duration) {
    EXPECT_EQ(yr::from_string<Days>("5"s), Days(5));
}

TEST(from_string, with_s_units_should_return_milliseconds) {
    EXPECT_EQ(yr::from_string<std::chrono::milliseconds>("5s"s), std::chrono::milliseconds(5000));
}
