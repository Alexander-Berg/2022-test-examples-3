#include <yxiva/core/shards/static_storage.h>
#include <catch.hpp>

using namespace yxiva::shard_config;

TEST_CASE("shards/static_storage", "")
{
    auto test_subject = std::make_shared<static_storage>();

    yplatform::ptree shard_node;
    shard_node.put("id", 0);
    shard_node.put("max_gid", 65535);

    SECTION("single master conninfo")
    {
        shard_node.put("master", "master conninfo");

        yplatform::ptree config;
        config.put_child("shards", shard_node);

        test_subject->init(config);

        auto shards = *test_subject->get();
        REQUIRE(shards.size() == 1);
        REQUIRE(shards[0].master.conninfo == "master conninfo");
    }

    SECTION("master and two replicas")
    {
        shard_node.put("master", "master conninfo");
        shard_node.push_back(std::make_pair("replicas", yplatform::ptree("replica1")));
        shard_node.push_back(std::make_pair("replicas", yplatform::ptree("replica2")));

        yplatform::ptree config;
        config.put_child("shards", shard_node);

        test_subject->init(config);

        auto shards = *test_subject->get();
        REQUIRE(shards.size() == 1);
        auto& shard = shards[0];
        REQUIRE(shard.master.conninfo == "master conninfo");
        auto& replicas = shard.replicas;
        REQUIRE(replicas.size() == 2);
        REQUIRE(replicas[0].conninfo == "replica1");
        REQUIRE(replicas[1].conninfo == "replica2");
    }

    SECTION("dynamic roles")
    {
        yplatform::ptree conninfo_node;
        conninfo_node.push_back(std::make_pair("hosts", yplatform::ptree("host1")));
        conninfo_node.push_back(std::make_pair("hosts", yplatform::ptree("host2")));
        conninfo_node.push_back(std::make_pair("hosts", yplatform::ptree("host3")));
        conninfo_node.put("params", "general params");
        conninfo_node.put("master_params", "master params");
        conninfo_node.put("replica_params", "replica params");

        shard_node.put_child("multirole_conninfo", conninfo_node);

        yplatform::ptree config;
        config.put_child("shards", shard_node);

        test_subject->init(config);

        auto shards = *test_subject->get();
        REQUIRE(shards.size() == 1);
        auto& shard = shards[0];
        REQUIRE(shard.master.conninfo == "host=host1,host2,host3 general params master params");
        auto& replicas = shard.replicas;
        REQUIRE(replicas.size() == 1);
        REQUIRE(replicas[0].conninfo == "host=host1,host2,host3 general params replica params");
    }
}
