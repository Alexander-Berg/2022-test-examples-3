#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <pgg/service/resolver.h>
#include <pgg/service/uid_resolver.h>

namespace pgg {
bool operator==(const ShardInfo& lhs, const ShardInfo& rhs) {
    return lhs.name == rhs.name
           && lhs.aliveEndpoints == rhs.aliveEndpoints;
}
}

namespace {

using namespace testing;
using namespace pgg;
using namespace pgg::query;
using namespace sharpei::client;

const Credentials credentials = {"user", "password"};
const Shard::Database deadMaster{{"host", 6432, "dbname", "dataCenter"}, "master", "dead", {0}};
const Shard::Database deadReplica{{"host", 6432, "dbname", "dataCenter"}, "replica", "dead", {1}};
const Shard::Database aliveMaster{{"master_host", 6432, "dbname", "dataCenter"}, "master", "alive", {0}};
const Shard::Database aliveNoLagReplica{{"no_lag_host", 6432, "dbname", "dataCenter"}, "replica", "alive", {0}};
const Shard::Database aliveLagReplica{{"lag_host", 6432, "dbname", "dataCenter"}, "replica", "alive", {1000}};
const Shard::Database aliveReplica = aliveNoLagReplica;

struct MakeConnStringsTest : Test {
};

TEST_F(MakeConnStringsTest, should_not_return_dead_databases) {
    UidResolveParams params("1");
    Shard shard{"id", "name", std::vector<Shard::Database>{deadMaster, deadReplica}};

    EXPECT_TRUE(makeConnStrings(credentials, params, shard).empty());
}

struct MakeConnStringsTestWithParams : public MakeConnStringsTest, public WithParamInterface<std::pair<Traits::EndpointType, ConnStrings>> {
    const Shard shard{"id", "name", std::vector<Shard::Database>{aliveMaster, aliveNoLagReplica, aliveLagReplica}};
};

const std::string masterConnString = "host=master_host port=6432 dbname=dbname user=user password=password";
const std::string lagReplicaConnString = "host=lag_host port=6432 dbname=dbname user=user password=password";
const std::string noLagReplicaConnString = "host=no_lag_host port=6432 dbname=dbname user=user password=password";

INSTANTIATE_TEST_SUITE_P(endpoint_values, MakeConnStringsTestWithParams, Values(
    std::make_pair(Traits::EndpointType::master, ConnStrings{masterConnString}),
    std::make_pair(Traits::EndpointType::replica, ConnStrings{lagReplicaConnString, noLagReplicaConnString}),
    std::make_pair(Traits::EndpointType::noLagReplica, ConnStrings{noLagReplicaConnString}),
    std::make_pair(Traits::EndpointType::lagReplica, ConnStrings{lagReplicaConnString})
));

TEST_P(MakeConnStringsTestWithParams, should_return_right_connstrings_for_endpoint_type) {
    UidResolveParams params("1");
    const auto [endpointType, connStrings] = GetParam();
    params.endpointType(endpointType);

    EXPECT_THAT(makeConnStrings(credentials, params, shard), UnorderedElementsAreArray(connStrings));
}

TEST_F(MakeConnStringsTest, should_return_all_no_lag_replicas_before_master) {
    UidResolveParams params("1");
    params.endpointType(Traits::EndpointType::noLagReplica);
    Shard shard{"id", "name", std::vector<Shard::Database>{aliveMaster, aliveNoLagReplica, aliveNoLagReplica}};

    EXPECT_THAT(makeConnStrings(credentials, params, shard),
            UnorderedElementsAreArray({noLagReplicaConnString, noLagReplicaConnString}));
}

TEST_F(MakeConnStringsTest, should_return_all_lag_replicas_after_master) {
    UidResolveParams params("1");
    params.endpointType(Traits::EndpointType::lagReplica);
    Shard shard{"id", "name", std::vector<Shard::Database>{aliveMaster, aliveLagReplica, aliveLagReplica}};

    EXPECT_THAT(makeConnStrings(credentials, params, shard),
                UnorderedElementsAreArray({lagReplicaConnString, lagReplicaConnString}));
}

TEST_F(MakeConnStringsTest, should_throw_invalid_argument_for_authomatic_endpoint_type) {
    UidResolveParams params("1");
    params.endpointType(Traits::EndpointType::automatic);
    Shard shard{"id", "name", std::vector<Shard::Database>{aliveMaster, aliveNoLagReplica, aliveLagReplica}};

    EXPECT_THROW(makeConnStrings(credentials, params, shard), std::invalid_argument);
}


struct ShardInfoTest : Test {};

TEST_F(ShardInfoTest, handler_onError_shouldBeCalledWithError) {
    auto handler = getShardInfoHandler([](error_code ec, const ShardInfo&){
        EXPECT_TRUE(ec);
        EXPECT_EQ(ec, Errors::UidNotFound);
    });
    handler(make_error_code(Errors::UidNotFound),
            Shard{"id", "name", std::vector<Shard::Database>{aliveMaster}});
}

struct ShardInfoTestWithParams : ShardInfoTest, WithParamInterface<std::pair<Shard, ShardInfo>> {};

using Endpoint = ShardInfo::Endpoint;

INSTANTIATE_TEST_SUITE_P(forShard_shouldReturnOnlyAliveHosts, ShardInfoTestWithParams, ::testing::Values(
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{}},
            ShardInfo{"name", {}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{deadMaster}},
            ShardInfo{"name", {}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{deadReplica}},
            ShardInfo{"name", {}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{aliveMaster}},
            ShardInfo{"name", {Endpoint::master}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{aliveReplica}},
            ShardInfo{"name", {Endpoint::replica}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{aliveMaster, deadReplica}},
            ShardInfo{"name", {Endpoint::master}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{deadMaster, aliveReplica}},
            ShardInfo{"name", {Endpoint::replica}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{deadMaster, deadReplica}},
            ShardInfo{"name", {}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{aliveMaster, aliveReplica}},
            ShardInfo{"name", {Endpoint::master, Endpoint::replica}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{aliveMaster, deadReplica, aliveReplica}},
            ShardInfo{"name", {Endpoint::master, Endpoint::replica}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{deadMaster, aliveReplica, aliveReplica}},
            ShardInfo{"name", {Endpoint::replica, Endpoint::replica}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{aliveMaster, deadReplica, deadReplica}},
            ShardInfo{"name", {Endpoint::master}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{deadMaster, deadReplica, deadReplica}},
            ShardInfo{"name", {}}
        ),
        std::make_pair(
            Shard{"id", "name", std::vector<Shard::Database>{aliveMaster, aliveReplica, aliveReplica}},
            ShardInfo{"name", {Endpoint::master, Endpoint::replica, Endpoint::replica}}
        )
));

TEST_P(ShardInfoTestWithParams, forShard_shouldReturnOnlyAliveHosts) {
    const auto [shard, expected] = GetParam();
    auto handler = getShardInfoHandler([expected = expected](error_code ec, const ShardInfo& info){
        EXPECT_FALSE(ec);
        EXPECT_EQ(info, expected);
    });
    handler(shard);
}

}
