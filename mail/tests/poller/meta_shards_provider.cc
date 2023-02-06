#include <internal/poller/meta_shards_provider.h>

#include <internal/config_reflection.h>
#include <mail/sharpei/tests/mocks.h>

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <chrono>

namespace {

using namespace sharpei;
using namespace sharpei::db;
using namespace sharpei::poller;
using namespace ::testing;

TEST(MetaShardsProviderTest, simple) {
    const std::string dbname = "dbname";
    const unsigned port = 42;
    const auto hostlist = MetaShardsProvider::Hostlist{{"host1", DC::sas}, {"host2", DC::iva}};
    const auto provider = MetaShardsProvider(dbname, port, hostlist);
    const auto result = provider.getAllShards({});
    ASSERT_TRUE(result) << result.error().full_message();
    ASSERT_EQ(result.value().size(), 1u);
    const auto& shard = result.value().back();
    EXPECT_EQ(shard.id, MetaShardsProvider::shardId);
    EXPECT_EQ(shard.name, MetaShardsProvider::shardName);
    ASSERT_EQ(shard.addrs.size(), hostlist.size());
    for (size_t i = 0; i < shard.addrs.size(); ++i) {
        EXPECT_EQ(shard.addrs[i].dataCenter, yreflection::to_string(hostlist[i].dc));
        EXPECT_EQ(shard.addrs[i].host, hostlist[i].host);
        EXPECT_EQ(shard.addrs[i].dbname, dbname);
        EXPECT_EQ(shard.addrs[i].port, port);
    }
}

}  // namespace
