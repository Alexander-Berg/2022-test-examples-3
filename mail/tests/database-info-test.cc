#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/database/database_info.h>

namespace macs {
bool operator==(const DatabaseState& lhs, const DatabaseState& rhs) {
    return lhs.name == rhs.name
           && lhs.state == rhs.state;
}
}

namespace {

using namespace ::testing;
using namespace ::macs::pg;

using Endpoint = pgg::ShardInfo::Endpoint;
using State = macs::DatabaseState::State;

struct ResolverMock {
    using Id = std::string;
    using Params = std::string;

    MOCK_METHOD(void, asyncGetShardStatus, (const Params& params, pgg::OnShardInfo hook), (const));
};

struct DatabaseInfoTest : TestWithParam<std::pair<pgg::ShardInfo, macs::DatabaseState>> {
    using Resolver = StrictMock<ResolverMock>;
    std::shared_ptr<Resolver> resolver = std::make_shared<Resolver>();

    inline auto getDatabaseInfo() {
        return createDatabaseInfo(resolver, "uid");
    }
};

INSTANTIATE_TEST_SUITE_P(shouldReturnCorrectStateBasedOnAliveHosts, DatabaseInfoTest, ::testing::Values(
    std::make_pair(
        pgg::ShardInfo{"name", {}},
        macs::DatabaseState{"name", State::dbDead}
    ),
    std::make_pair(
        pgg::ShardInfo{"name", {Endpoint::master}},
        macs::DatabaseState{"name", State::masterOnly}
    ),
    std::make_pair(
        pgg::ShardInfo{"name", {Endpoint::replica}},
        macs::DatabaseState{"name", State::singleReplicaOnly}
    ),
    std::make_pair(
        pgg::ShardInfo{"name", {Endpoint::master, Endpoint::replica}},
        macs::DatabaseState{"name", State::singleReplica}
    ),
    std::make_pair(
        pgg::ShardInfo{"name", {Endpoint::replica, Endpoint::replica}},
        macs::DatabaseState{"name", State::readOnly}
    ),
    std::make_pair(
        pgg::ShardInfo{"name", {Endpoint::master, Endpoint::replica, Endpoint::replica}},
        macs::DatabaseState{"name", State::readWrite}
    )
));

TEST_P(DatabaseInfoTest, shouldReturnCorrectStateBasedOnAliveHosts) {
    const auto [shard, expected] = GetParam();
    EXPECT_CALL(*resolver, asyncGetShardStatus("uid", _))
            .WillOnce(InvokeArgument<1>(pgg::error_code(), shard));
    const auto state = getDatabaseInfo()->getState();
    EXPECT_EQ(state, expected);
}

}
