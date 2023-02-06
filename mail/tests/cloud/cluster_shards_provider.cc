#include "../services/test_with_context.h"

#include <gtest/gtest.h>

#include <internal/cloud/cluster_shards_provider.h>
#include <internal/reflection/shard.h>

#include <yamail/data/serialization/json_writer.h>

#include <boost/filesystem.hpp>

namespace sharpei {

static bool operator ==(const ShardWithoutRoles& lhs, const ShardWithoutRoles& rhs) {
    return std::tie(lhs.id, lhs.name, lhs.addrs) == std::tie(rhs.id, rhs.name, rhs.addrs);
}

static std::ostream& operator <<(std::ostream& stream, const ShardWithoutRoles& value) {
    using yamail::data::serialization::toJson;
    return stream << toJson(reflection::ShardWithoutRoles {value.id, value.name, {value.addrs.begin(), value.addrs.end()}});
}

} // namespace sharpei

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::cloud;
using namespace sharpei::services;
using namespace sharpei::services::yc;

using sharpei::tests::TestWithContext;

ClusterShardsProviderConfig makeClusterShardsProviderConfig() {
    ClusterShardsProviderConfig config;
    config.port = 6432;
    config.dbname = "dbname";
    return config;
}

struct TestCloudClusterShardsProvider : TestWithContext {
    ClusterShardsProviderConfig config = makeClusterShardsProviderConfig();
    const YcHostsCachePtr hostsCache = std::make_shared<YcHostsCache>();
    const std::vector<Host> hosts {{Host {"foo", "sas"}, Host {"bar", "vla"}}};
    const std::vector<ShardWithoutRoles> shards {{
        ShardWithoutRoles {
            Shard::Id(1),
            "yc",
            {
                Shard::Database::Address {"foo", 6432, "dbname", "sas"},
                Shard::Database::Address {"bar", 6432, "dbname", "vla"},
            },
        }
    }};
};

TEST_F(TestCloudClusterShardsProvider, get_all_shards_should_return_shards) {
    const ClusterShardsProvider provider(config, hostsCache);

    withContext([&] (const auto& context) {
        hostsCache->emplace(hosts);

        const auto result = provider.getAllShards(context);

        ASSERT_TRUE(result) << result.error().full_message();
        EXPECT_EQ(result.value(), shards);
    });
}

TEST_F(TestCloudClusterShardsProvider, get_all_shards_with_empty_hosts_cache_should_return_error) {
    const ClusterShardsProvider provider(config, hostsCache);

    withContext([&] (const auto& context) {
        const auto result = provider.getAllShards(context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::emptyYcHostsCache) << result.error().full_message();
    });
}

} // namespace
