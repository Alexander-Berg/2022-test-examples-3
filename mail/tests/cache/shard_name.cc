#include <gtest/gtest.h>

#include <internal/cache/shard_name.h>
#include <library/cpp/testing/gtest_boost_extensions/extensions.h>

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::cache;

using Shards = ShardNameCache::Shards;
using OptShardName = ShardNameCache::OptShardName;

TEST(ShardNameCacheTest, get_from_empty_should_return_empty) {
    ShardNameCache cache;
    EXPECT_EQ(cache.get(1), OptShardName());
}

TEST(ShardNameCacheTest, all_from_empty_should_return_empty) {
    ShardNameCache cache;
    EXPECT_EQ(cache.all(), Shards());
}

TEST(ShardNameCacheTest, update_then_get_should_return_equal) {
    ShardNameCache cache;
    cache.update(1, "shard");
    EXPECT_EQ(cache.get(1), OptShardName("shard"));
}

TEST(ShardNameCacheTest, update_then_all_should_return_equal) {
    ShardNameCache cache;
    cache.update(1, "shard1");
    cache.update(2, "shard2");
    EXPECT_EQ(cache.all(), Shards({{1, "shard1"}, {2, "shard2"}}));
}

TEST(ShardNameCacheTest, update_and_erase_then_get_should_return_none) {
    ShardNameCache cache;
    cache.update(1, "shard1");
    cache.erase(1);
    EXPECT_EQ(cache.get(1), boost::none);
}

} // namespace
