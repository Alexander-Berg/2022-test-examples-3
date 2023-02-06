#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <library/cpp/testing/gtest_boost_extensions/extensions.h>
#include <yamail/data/serialization/json_writer.h>
#include <internal/cache/status.h>

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::cache;

using Database = Shard::Database;
using Address = Database::Address;
using Status = Database::Status;
using OptStatus = StatusCache::OptStatus;

TEST(LimitedCounterTest, construct_with_value_greater_than_max_should_throw_exception) {
    EXPECT_THROW(LimitedCounter(0, 1), std::logic_error);
}

TEST(LimitedCounterTest, decrement_not_zero_should_change_value) {
    LimitedCounter counter(1, 1);
    counter -= 1;
    EXPECT_EQ(counter.value(), 0u);
}

TEST(LimitedCounterTest, increment_not_max_value_should_change_value) {
    LimitedCounter counter(1, 0);
    counter += 1;
    EXPECT_EQ(counter.value(), 1u);
}

TEST(LimitedCounterTest, decrement_zero_should_left_zero_value) {
    LimitedCounter counter(1, 0);
    counter -= 1;
    EXPECT_EQ(counter.value(), 0u);
}

TEST(LimitedCounterTest, increment_max_value_should_left_max_value) {
    const std::size_t max = 1;
    LimitedCounter counter(max, 1);
    counter += 1;
    EXPECT_EQ(counter.value(), max);
}

TEST(LimitedCounterTest, add_more_than_max_should_set_max_value) {
    const std::size_t max = 1;
    LimitedCounter counter(max, 0);
    counter += max + 1;
    EXPECT_EQ(counter.value(), max);
}

TEST(LimitedCounterTest, sub_more_than_value_should_set_zero) {
    const std::size_t value = 1;
    LimitedCounter counter(1, value);
    counter -= value + 1;
    EXPECT_EQ(counter.value(), 0u);
}

TEST(AvailabilityTest, update_by_alive_when_errors_limit_is_zero_then_check_by_smooth_should_return_dead) {
    Availability availability(1, 0);
    availability.alive();
    EXPECT_EQ(availability.smooth(), Status::Dead);
}

TEST(AvailabilityTest, update_by_dead_when_errors_limit_is_zero_then_check_by_smooth_should_return_dead) {
    Availability availability(1, 0);
    availability.dead();
    EXPECT_EQ(availability.smooth(), Status::Dead);
}

TEST(AvailabilityTest, update_by_alive_when_errors_limit_is_one_then_check_by_smooth_should_return_alive) {
    Availability availability(1, 1);
    availability.alive();
    EXPECT_EQ(availability.smooth(), Status::Alive);
}

TEST(AvailabilityTest, update_by_dead_when_errors_limit_is_one_then_check_by_smooth_should_return_dead) {
    Availability availability(1, 1);
    availability.dead();
    EXPECT_EQ(availability.smooth(), Status::Dead);
}

TEST(AvailabilityTest, check_empty_by_any_method_should_throw_exception) {
    Availability availability(1, 1);
    const auto methods = {&Availability::smooth, &Availability::recent};
    for (const auto method : methods) {
        EXPECT_THROW(availability.get(method), std::logic_error);
    }
}

TEST(AvailabilityTest, update_by_alive_more_than_history_capacity_then_check_by_smooth_should_return_alive) {
    const std::size_t historyCapacity = 3;
    Availability availability(historyCapacity, 2);
    for (std::size_t i = 0; i < historyCapacity + 1; ++i) {
        availability.update(&Availability::alive);
        EXPECT_EQ(availability.smooth(), Status::Alive);
    }
}

TEST(AvailabilityTest, update_by_dead_more_than_history_capacity_then_check_by_smooth_should_return_dead) {
    const std::size_t historyCapacity = 3;
    Availability availability(historyCapacity, 2);
    for (std::size_t i = 0; i < historyCapacity + 1; ++i) {
        availability.dead();
        EXPECT_EQ(availability.smooth(), Status::Dead);
    }
}

TEST(AvailabilityTest, update_to_ressurect_from_dead_then_check_by_smooth_at_end_should_return_alive) {
    Availability availability(3, 2);
    for (std::size_t i = 0; i < 3; ++i) {
        availability.dead();
        EXPECT_EQ(availability.smooth(), Status::Dead);
    }
    availability.alive();
    EXPECT_EQ(availability.smooth(), Status::Dead);
    availability.alive();
    EXPECT_EQ(availability.smooth(), Status::Alive);
}

TEST(AvailabilityTest, update_many_times_then_check_should_return_status_according_to_proportion) {
    Availability availability(9, 6);

    for (int i = 0; i < 2; ++i) {
        availability.alive();
    }
    const auto at0to2 = availability.smooth();
    EXPECT_EQ(at0to2, Status::Alive);

    for (int i = 0; i < 4; ++i) {
        availability.dead();
    }
    const auto at4to6 = availability.smooth();
    EXPECT_EQ(at4to6, Status::Dead);

    availability.alive();
    const auto at4to7 = availability.smooth();
    EXPECT_EQ(at4to7, Status::Alive);

    for (int i = 0; i < 3; ++i) {
        availability.dead();
    }
    const auto at6to9 = availability.smooth();
    EXPECT_EQ(at6to9, Status::Dead);
}

TEST(AvailabilityTest, update_many_times_then_check_by_recent_always_should_return_last) {
    Availability availability(3, 2);
    availability.alive();
    EXPECT_EQ(availability.recent(), Status::Alive);
    availability.dead();
    EXPECT_EQ(availability.recent(), Status::Dead);
    availability.alive();
    EXPECT_EQ(availability.recent(), Status::Alive);
    availability.dead();
    EXPECT_EQ(availability.recent(), Status::Dead);
}

TEST(AvailabilityTest, update_many_times_then_get_history_should_return_sequence_corresponding_to_call_sequence) {
    Availability availability(5, 3);
    availability.alive();
    availability.dead();
    availability.alive();
    availability.alive();
    availability.dead();
    const auto result = availability.getHistory();
    const Availability::History expected = {
        Status::Alive,
        Status::Dead,
        Status::Alive,
        Status::Alive,
        Status::Dead,
    };
    EXPECT_EQ(result, expected);
}

TEST(AvailabilityTest, get_history_from_empty_should_return_empty_sequence) {
    Availability availability(3, 2);
    const auto result = availability.getHistory();
    EXPECT_EQ(result, Availability::History());
}

TEST(AvailabilityTest, update_many_times_then_get_history_should_return_sequence_with_size_less_than_history_size) {
    Availability availability(3, 2);
    availability.alive();
    availability.alive();
    const auto result = availability.getHistory();
    const Availability::History expected = {
        Status::Alive,
        Status::Alive,
    };
    EXPECT_EQ(result, expected);
}

TEST(AvailabilityTest, update_many_times_then_get_history_should_return_sequence_with_size_not_greater_than_history_size) {
    Availability availability(3, 2);
    availability.alive();
    availability.alive();
    availability.alive();
    availability.alive();
    const auto result = availability.getHistory();
    const Availability::History expected = {
        Status::Alive,
        Status::Alive,
        Status::Alive,
    };
    EXPECT_EQ(result, expected);
}

TEST(StatusCacheTest, get_from_empty_should_return_uninitialized_status) {
    StatusCache cache(1, 1);
    const auto status = cache.get(1, {"localhost", 5432, "maildb", "sas"}, &Availability::recent);
    EXPECT_EQ(status, boost::none);
}

TEST(StatusCacheTest, update_alive_then_get_should_return_alive_status) {
    StatusCache cache(1, 1);
    const auto address = Address {"localhost", 5432, "maildb", "sas"};
    cache.update(1, address, &Availability::alive);
    const auto status = cache.get(1, address, &Availability::recent);
    EXPECT_EQ(status, OptStatus(Status::Alive));
}

TEST(StatusCacheTest, update_twice_then_get_recent_should_return_last_status) {
    StatusCache cache(2, 2);
    const auto address = Address {"localhost", 5432, "maildb", "sas"};
    cache.update(1, address, &Availability::alive);
    cache.update(1, address, &Availability::dead);
    const auto status = cache.get(1, address, &Availability::recent);
    EXPECT_EQ(status, OptStatus(Status::Dead));
}

TEST(StatusCacheTest, update_and_get_unexisted_from_same_shard_should_return_uninitialized_status) {
    StatusCache cache(1, 1);
    cache.update(1, {"pg1.yandex.net", 5432, "maildb", "sas"}, &Availability::alive);
    const auto status = cache.get(1, {"pg2.yandex.net", 5432, "maildb", "sas"}, &Availability::recent);
    EXPECT_EQ(status, boost::none);
}

TEST(StatusCacheTest, update_and_erase_then_get_should_return_uninitialized_status) {
    StatusCache cache(1, 1);
    const auto address = Address {"localhost", 5432, "maildb", "sas"};
    cache.update(1, address, &Availability::dead);
    cache.erase(1);
    const auto status = cache.get(1, address, &Availability::smooth);
    EXPECT_EQ(status, boost::none);
}

TEST(StatusCacheTest, update_many_times_then_get_history_should_return_sequence_corresponding_to_call_sequence) {
    StatusCache cache(5, 3);
    const auto address = Address {"localhost", 5432, "maildb", "sas"};
    cache.update(1, address, &Availability::dead);
    cache.update(1, address, &Availability::alive);
    cache.update(1, address, &Availability::dead);
    cache.update(1, address, &Availability::dead);
    cache.update(1, address, &Availability::alive);
    const auto result = cache.getHistory(1, address);
    const Availability::History expected = {
        Status::Dead,
        Status::Alive,
        Status::Dead,
        Status::Dead,
        Status::Alive,
    };
    EXPECT_EQ(result, expected);
}

TEST(StatusCacheTest, get_history_when_no_shard_should_return_empty) {
    const StatusCache cache(1, 1);
    EXPECT_EQ(cache.getHistory(1, Address {"localhost", 5432, "maildb", "sas"}), Availability::History());
}

TEST(StatusCacheTest, get_history_when_no_host_should_return_empty) {
    StatusCache cache(1, 1);
    cache.alive(1, Address {"localhost", 5432, "maildb", "sas"});
    EXPECT_EQ(cache.getHistory(1, Address {"remote", 5432, "maildb", "sas"}), Availability::History());
}

TEST(StatusCacheTest, history_capacity_after_construct_should_return_same) {
    const StatusCache cache(42, 13);
    EXPECT_EQ(cache.historyCapacity(), 42u);
}

TEST(StatusCacheTest, errors_limit_after_construct_should_return_same) {
    const StatusCache cache(42, 13);
    EXPECT_EQ(cache.errorsLimit(), 13u);
}

} // namespace
