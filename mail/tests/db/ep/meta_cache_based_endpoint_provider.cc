#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <internal/db/ep/meta_cache_based_endpoint_provider.h>

#include <internal/dc_vanga_config.h>
#include <mail/sharpei/tests/mocks.h>
#include <mail/sharpei/tests/util.h>

#include <limits>
#include <memory>
#include <stdexcept>

using namespace std::literals;

using namespace ::testing;
using namespace sharpei;
using namespace sharpei::db;
using namespace sharpei::dc_vanga;
using namespace sharpei::poller;
using namespace cache;
using RuleConfig = sharpei::dc_vanga::RuleConfig;

namespace sharpei::db {

extern bool operator==(const ConnectionInfo& lhs, const ConnectionInfo& rhs);

}  // namespace sharpei::db

namespace {

AuthInfo makeAuthInfo() {
    return AuthInfo{}.password("pwd").sslmode("required").user("usr");
}

TEST(MetaCacheBasedEndpointProvider_FiltrationTests, deadHostFilteredOut) {
    const auto db = makeDatabase(Role::Master, Status::Dead);
    const auto res = filterDatabases({db}, AcceptingAllNodesFiltrationStrategy{});
    ASSERT_EQ(res.size(), 0u);
}

TEST(MetaCacheBasedEndpointProvider_FiltrationTests, masterDoesntFilteredOut) {
    const auto db = makeDatabase(Role::Master, Status::Alive);
    const auto res = filterDatabases({db}, AcceptingAllNodesFiltrationStrategy{});
    ASSERT_EQ(res.size(), 1u);
    ASSERT_EQ(res.back(), db);
}

TEST(MetaCacheBasedEndpointProvider_FiltrationTests, laggingHostsDoesntFilteredOut) {
    const auto maxLag = std::numeric_limits<Database::State::ReplicationLag>::max();
    const auto db = makeDatabase(Role::Replica, Status::Alive, State{.lag = maxLag});
    const auto res = filterDatabases({db}, AcceptingAllNodesFiltrationStrategy{});
    ASSERT_EQ(res.size(), 1u);
    ASSERT_EQ(res.back(), db);
}

TEST(MetaCacheBasedEndpointProvider_FiltrationTests, multipleHosts) {
    constexpr size_t N = 10;
    std::vector<Database> dbs;
    std::generate_n(std::back_inserter(dbs), N, []() { return makeDatabase(Role::Replica, Status::Alive); });
    const auto res = filterDatabases({dbs.begin(), dbs.end()}, AcceptingAllNodesFiltrationStrategy{});
    ASSERT_EQ(res.size(), N);
}

struct MetaCacheBasedEndpointProviderTests : Test {
    const AuthInfo authInfo = makeAuthInfo();
    const Rules dcVanga = std::vector<RuleConfig>{};
    const FiltrationStrategyPtr filter{new AcceptingAllNodesFiltrationStrategy};
    const EndpointSelectionStrategyPtr fallbackStrategy{new SimpleEndpointSelectionStrategy};
    const std::size_t aliveHostsThreshold = 0;
};

TEST_F(MetaCacheBasedEndpointProviderTests, simple) {
    const auto db = makeDatabase(Role::Replica, Status::Alive);
    const auto metaCache = makeCache({db});
    const auto ep = makeMetaCacheBasedEndpointProvider(metaCache, authInfo, dcVanga, aliveHostsThreshold);
    const auto ret = ep->getNext();
    ASSERT_EQ(ret, toEndpoint(db, authInfo));
}

TEST_F(MetaCacheBasedEndpointProviderTests, preferHostFromLocalDCIfVangaConfigured) {
    const auto sasHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "sas");
    const auto vlaHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "vla");
    const auto metaCache = makeCache({sasHost, vlaHost});
    const auto mainStrategy = std::make_shared<DCAwareMainStrategy>("vla", aliveHostsThreshold);
    auto ep = MetaCacheBasedEndpointProvider(metaCache, authInfo, filter, mainStrategy, fallbackStrategy);
    const auto ret = ep.getNext();
    ASSERT_EQ(ret, toEndpoint(vlaHost, authInfo));
}

TEST_F(MetaCacheBasedEndpointProviderTests, preferHostFromLocalDCIfThereAreEnoughAliveHosts) {
    const auto sasHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "sas");
    const auto vlaHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "vla");
    const auto vlaAliveHostsThreshold = 1;

    const auto metaCache = makeCache({sasHost, vlaHost});
    const auto mainStrategy = std::make_shared<DCAwareMainStrategy>("vla", vlaAliveHostsThreshold);
    auto ep = MetaCacheBasedEndpointProvider(metaCache, authInfo, filter, mainStrategy, fallbackStrategy);

    const auto ret = ep.getNext();
    ASSERT_EQ(ret, toEndpoint(vlaHost, authInfo));
}

TEST_F(MetaCacheBasedEndpointProviderTests, hostFromAnotherDCUsedIfHostFromLocalDCIsDead) {
    const auto sasHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "sas");
    const auto vlaHost = makeDatabase(Role::Replica, Status::Dead, State{.lag = 0}, "vla");
    const auto metaCache = makeCache({sasHost, vlaHost});
    const auto mainStrategy = std::make_shared<DCAwareMainStrategy>("vla", aliveHostsThreshold);
    auto ep = MetaCacheBasedEndpointProvider(metaCache, authInfo, filter, mainStrategy, fallbackStrategy);
    const auto ret = ep.getNext();
    ASSERT_EQ(ret, toEndpoint(sasHost, authInfo));
}

TEST_F(MetaCacheBasedEndpointProviderTests, exceptionIfCacheIsEmpty) {
    const auto db = makeDatabase(Role::Replica, Status::Alive);
    auto metaCache = std::make_shared<Cache>(5, 3);
    metaCache->shardName.update(MetaShardsProvider::shardId, MetaShardsProvider::shardName);
    const auto ep = makeMetaCacheBasedEndpointProvider(metaCache, authInfo, dcVanga, aliveHostsThreshold);
    try {
        const auto res = ep->getNext();
        FAIL() << "expected exception";
    } catch (const std::runtime_error& e) {
        ASSERT_EQ(e.what(), "metaCache_->getShard error"s);
    }
}

TEST_F(MetaCacheBasedEndpointProviderTests, exceptionIfThereIsNoSuitableHosts) {
    const auto db1 = makeDatabase(Role::Replica, Status::Dead);
    const auto db2 = makeDatabase(Role::Master, Status::Dead);
    const auto metaCache = makeCache({db1, db2});
    const auto ep = makeMetaCacheBasedEndpointProvider(metaCache, authInfo, dcVanga, aliveHostsThreshold);
    try {
        const auto res = ep->getNext();
        FAIL() << "expected exception";
    } catch (const std::runtime_error& e) {
        ASSERT_EQ(e.what(), "appropriate host not found"s);
    }
}

struct MockedEndpointSelectionStrategy : IEndpointSelectionStrategy {
    MOCK_METHOD(yamail::expected<Shard::Database>, selectEndpoint, (const std::vector<Shard::Database>& dbs), (override));
};

using MockedStrategy = StrictMock<MockedEndpointSelectionStrategy>;

struct MetaCacheBasedEndpointProvider_StrategiesTests : Test {
    const AuthInfo authInfo = makeAuthInfo();
    const FiltrationStrategyPtr filter{new AcceptingAllNodesFiltrationStrategy};
    const std::shared_ptr<MockedStrategy> mainStrategy{new MockedStrategy};
    const std::shared_ptr<MockedStrategy> fallbackStrategy{new MockedStrategy};
};

TEST_F(MetaCacheBasedEndpointProvider_StrategiesTests, fallbackedDoesntUsedIfMainStrategySucceed) {
    const auto db = makeDatabase(Role::Replica, Status::Alive);
    const auto metaCache = makeCache({db});
    MetaCacheBasedEndpointProvider ep(metaCache, authInfo, filter, mainStrategy, fallbackStrategy);

    EXPECT_CALL(*mainStrategy, selectEndpoint(_)).WillOnce(Return(yamail::make_expected(db)));
    EXPECT_CALL(*fallbackStrategy, selectEndpoint(_)).Times(0);

    const auto ret = ep.getNext();
    ASSERT_EQ(ret, toEndpoint(db, authInfo));
}

TEST_F(MetaCacheBasedEndpointProvider_StrategiesTests, fallbackedUsedIfMainStrategyFailed) {
    const auto db = makeDatabase(Role::Replica, Status::Alive);
    const auto metaCache = makeCache({db});
    MetaCacheBasedEndpointProvider ep(metaCache, authInfo, filter, mainStrategy, fallbackStrategy);

    EXPECT_CALL(*mainStrategy, selectEndpoint(_))
        .WillOnce(Return(yamail::make_unexpected(ExplainedError(Error::appropriateHostNotFound))));
    EXPECT_CALL(*fallbackStrategy, selectEndpoint(_)).WillOnce(Return(yamail::make_expected(db)));

    const auto ret = ep.getNext();
    ASSERT_EQ(ret, toEndpoint(db, authInfo));
}

TEST_F(MetaCacheBasedEndpointProvider_StrategiesTests, fallbackedUsedIfThereAreNotEnoughAliveHosts) {
    const auto db = makeDatabase(Role::Replica, Status::Alive);
    const auto vlaHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "vla");
    const auto metaCache = makeCache({db, vlaHost});
    const auto awareStrategy = std::make_shared<DCAwareMainStrategy>("vla", 2);
    auto ep = MetaCacheBasedEndpointProvider(metaCache, authInfo, filter, awareStrategy, fallbackStrategy);

    EXPECT_CALL(*fallbackStrategy, selectEndpoint(_)).WillOnce(Return(yamail::make_expected(db)));

    const auto ret = ep.getNext();
    ASSERT_EQ(ret, toEndpoint(db, authInfo));
}

TEST_F(MetaCacheBasedEndpointProvider_StrategiesTests, exceptionIfBothStrategiesFailed) {
    const auto db = makeDatabase(Role::Replica, Status::Alive);
    const auto metaCache = makeCache({db});
    MetaCacheBasedEndpointProvider ep(metaCache, authInfo, filter, mainStrategy, fallbackStrategy);

    EXPECT_CALL(*mainStrategy, selectEndpoint(_))
        .WillOnce(Return(yamail::make_unexpected(ExplainedError(Error::appropriateHostNotFound))));
    EXPECT_CALL(*fallbackStrategy, selectEndpoint(_))
        .WillOnce(Return(yamail::make_unexpected(ExplainedError(Error::appropriateHostNotFound))));

    try {
        const auto res = ep.getNext();
        FAIL() << "expected exception";
    } catch (const std::runtime_error& e) {
        ASSERT_EQ(e.what(), "appropriate host not found"s);
    }
}

}  // namespace
