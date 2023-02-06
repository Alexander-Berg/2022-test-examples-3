#include <mocks/query.h>
#include <mocks/random_uint_generator.h>
#include <db/query_dispatcher/query_dispatcher.h>
#include <gtest/gtest.h>
#include <memory>
#include <utility>

namespace yrpopper::db {

auto mock_query = std::make_shared<mock::query>();
auto MOCK_QUERY_RETURNING_SET = std::make_shared<mock::query_returning_set>();
const shard_list SHARDS = { { "first_shard_conninfo", 1, 10 },
                            { "second_shard_conninfo", 11, 20 },
                            { "third_shard_conninfo", 21, 30 } };
const request_target DEFAULT_TARGET = request_target::master;

settings create_settings()
{
    settings settings;
    settings.shards = SHARDS;
    return settings;
}

class query_dispatcher_test : public query_dispatcher
{
public:
    query_dispatcher_test() : query_dispatcher(create_settings())
    {
    }
};

class covered_key_range_test
    : public query_dispatcher_test
    , public ::testing::TestWithParam<std::pair<sharding_key_t, std::string>>
{
};

INSTANTIATE_TEST_SUITE_P(
    query_dispatcher,
    covered_key_range_test,
    ::testing::Values(
        std::make_pair(SHARDS[0].start_key, SHARDS[0].master_conninfo),
        std::make_pair(SHARDS[0].end_key, SHARDS[0].master_conninfo),
        std::make_pair(SHARDS[1].start_key, SHARDS[1].master_conninfo),
        std::make_pair(SHARDS[1].end_key, SHARDS[1].master_conninfo),
        std::make_pair((SHARDS[0].start_key + SHARDS[0].end_key) / 2, SHARDS[0].master_conninfo),
        std::make_pair((SHARDS[1].start_key + SHARDS[1].end_key) / 2, SHARDS[1].master_conninfo)));

TEST_P(covered_key_range_test, should_return_corresponding_conninfo)
{
    auto [sharding_key, expected_conninfo] = GetParam();

    EXPECT_EQ(run(mock_query, sharding_key, DEFAULT_TARGET), expected_conninfo);
}

class uncovered_key_range_test
    : public query_dispatcher_test
    , public ::testing::TestWithParam<sharding_key_t>
{
};

INSTANTIATE_TEST_SUITE_P(
    query_dispatcher,
    uncovered_key_range_test,
    ::testing::Values(SHARDS.front().start_key - 1, SHARDS.back().end_key + 1));

TEST_P(uncovered_key_range_test, should_throw_exception)
{
    auto sharding_key = GetParam();

    EXPECT_THROW(run(mock_query, sharding_key, DEFAULT_TARGET), std::runtime_error);
}

class random_shard_usage_test
    : public query_dispatcher_impl<mock::random_uint_generator>
    , public ::testing::Test
{
public:
    random_shard_usage_test() : query_dispatcher_impl(create_settings())
    {
    }
};

TEST_F(random_shard_usage_test, should_use_shard_given_by_random_generator)
{
    auto result_conninfo = run_on_any(mock_query, DEFAULT_TARGET);
    auto expected_conninfo =
        SHARDS[mock::random_uint_generator::get_last_generated_uint()].master_conninfo;

    EXPECT_EQ(result_conninfo, expected_conninfo);
}

class running_query_on_all_shards_test
    : public query_dispatcher_test
    , public ::testing::Test
{
};

TEST_F(running_query_on_all_shards_test, should_return_set_with_conninfos_from_all_shards)
{
    std::set<std::string> expected_conninfos{ SHARDS[0].master_conninfo,
                                              SHARDS[1].master_conninfo,
                                              SHARDS[2].master_conninfo };

    EXPECT_EQ(*run_on_all(MOCK_QUERY_RETURNING_SET, DEFAULT_TARGET).get(), expected_conninfos);
}

} // namespace yrpopper::db
