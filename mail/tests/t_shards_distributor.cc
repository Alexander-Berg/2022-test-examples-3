#include "fake_shards_listener.h"
#include "fake_lock_manager.h"
#include "fake_bucket_manager.h"
#include "shards_distributor_impl.h"
#include <catch.hpp>

using namespace ymod_mdb_sharder;

enum shards_distributor_mode
{
    use_shards_listener = 0,
    use_lock_manager,
    use_bucket_manager
};

struct shards_distributor_test : yplatform::log::contains_logger
{
    using shards_distributor_type =
        shards_distributor_impl<fake_shards_listener*, fake_lock_manager*, fake_bucket_manager*>;

    shards_distributor_test()
    {
        logger().append_log_prefix("shards_distributor_test");

        my_node = node_info{ "my_node_id", "localhost" };
        other_node = node_info{ "other_node_id", "other_node.net" };
    }

    std::shared_ptr<shards_distributor_type> create_shards_distributor(shards_distributor_mode mode)
    {
        auto shards_distributor = std::make_shared<shards_distributor_type>(
            io,
            ctx,
            buckets,
            open_bucket,
            my_node.id,
            &shards_listener,
            mode >= use_lock_manager ? &lock_manager : nullptr,
            mode >= use_bucket_manager ? &bucket_manager : nullptr);
        shards_distributor->init();
        shards_distributor->subscribe(
            std::bind(&shards_distributor_test::on_acquire, this, ph::_1),
            std::bind(&shards_distributor_test::on_release, this, ph::_1));
        return shards_distributor;
    }

    void on_acquire(const shard_ids& shards)
    {
        for (auto& shard : shards)
        {
            YLOG_L(info) << "acquire shard " << shard;
        }
        acquired_shards.insert(shards.begin(), shards.end());
    }

    void on_release(const shard_ids& shards)
    {
        for (auto& shard : shards)
        {
            YLOG_L(info) << "release shard " << shard;
        }
        released_shards.insert(shards.begin(), shards.end());
    }

    void run()
    {
        io.run();
        io.reset();
    }

    void check_acquired_and_released(const shard_ids& acquired_ids, const shard_ids& released_ids)
    {
        REQUIRE(acquired_shards.size() == acquired_ids.size());
        REQUIRE(released_shards.size() == released_ids.size());
        for (auto& id : acquired_ids)
        {
            REQUIRE(acquired_shards.count(id));
        }
        for (auto& id : released_ids)
        {
            REQUIRE(released_shards.count(id));
        }
        acquired_shards.clear();
        released_shards.clear();
    }

    fake_shards_listener shards_listener;
    fake_lock_manager lock_manager;
    fake_bucket_manager bucket_manager;
    boost::asio::io_service io;
    task_context_ptr ctx = boost::make_shared<yplatform::task_context>();
    std::map<shard_id, bucket_id> buckets;
    bucket_id open_bucket = "open_bucket";
    std::set<shard_id> acquired_shards;
    std::set<shard_id> released_shards;
    node_info my_node;
    node_info other_node;
};

TEST_CASE_METHOD(
    shards_distributor_test,
    "should acquire all shards if lock_manager and bucket_manager not specified")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    auto shards_distributor = create_shards_distributor(use_shards_listener);
    shards_listener.add_shards({ shard1, shard2 });
    run();
    check_acquired_and_released({ shard1, shard2 }, {});
}

TEST_CASE_METHOD(shards_distributor_test, "should release shard when it deleted")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    auto shards_distributor = create_shards_distributor(use_shards_listener);
    shards_listener.add_shards({ shard1, shard2 });
    shards_listener.del_shards({ shard2 });
    run();
    check_acquired_and_released({ shard1, shard2 }, { shard2 });
}

TEST_CASE_METHOD(shards_distributor_test, "should acquire shard when it acquired from lock_manager")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    auto shards_distributor = create_shards_distributor(use_lock_manager);
    shards_listener.add_shards({ shard1, shard2 });
    lock_manager.acquire_resource(shard1);
    run();
    check_acquired_and_released({ shard1 }, {});
}

TEST_CASE_METHOD(
    shards_distributor_test,
    "should acquire shard when it acquired from bucket_manager")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    std::string bucket1 = "b1";
    buckets[shard1] = bucket1;
    buckets[shard2] = bucket1;
    auto shards_distributor = create_shards_distributor(use_bucket_manager);
    shards_listener.add_shards({ shard1, shard2 });
    bucket_manager.acquire_bucket(bucket1);
    run();
    check_acquired_and_released({ shard1, shard2 }, {});
}

TEST_CASE_METHOD(shards_distributor_test, "should acquire shards not specified in buckets config")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    std::string bucket1 = "b1";
    buckets[shard1] = bucket1;
    auto shards_distributor = create_shards_distributor(use_bucket_manager);
    shards_listener.add_shards({ shard1, shard2 });
    bucket_manager.acquire_bucket(bucket1);
    bucket_manager.acquire_bucket(open_bucket);
    run();
    check_acquired_and_released({ shard1, shard2 }, {});
}

TEST_CASE_METHOD(shards_distributor_test, "should get owner for acquired shards")
{
    shard_id shard1 = "xdb1";
    auto shards_distributor = create_shards_distributor(use_shards_listener);
    shards_listener.add_shards({ shard1 });
    run();
    check_acquired_and_released({ shard1 }, {});

    bool called = false;
    shards_distributor->get_owner(
        ctx, shard1, [this, &called](error_code err, const node_info& owner) {
            called = true;
            REQUIRE(!err);
            REQUIRE(owner.id == my_node.id);
            REQUIRE(owner.host == boost::asio::ip::host_name());
        });
    run();
    REQUIRE(called);
}

TEST_CASE_METHOD(shards_distributor_test, "should get owner for shards acquired by other nodes")
{
    shard_id shard1 = "xdb1";
    auto shards_distributor = create_shards_distributor(use_lock_manager);
    shards_listener.add_shards({ shard1 });
    run();
    check_acquired_and_released({}, {});

    lock_manager.set_resource_value(
        shard1, "{\"host\":\"" + other_node.host + "\", \"id\":\"" + other_node.id + "\"}");
    bool called = false;
    shards_distributor->get_owner(
        ctx, shard1, [this, &called](error_code err, const node_info& owner) {
            called = true;
            REQUIRE(!err);
            REQUIRE(owner.id == other_node.id);
            REQUIRE(owner.host == other_node.host);
        });
    run();
    REQUIRE(called);
}

TEST_CASE_METHOD(shards_distributor_test, "should get owner for not owned shards")
{
    shard_id shard1 = "xdb1";
    auto shards_distributor = create_shards_distributor(use_lock_manager);
    shards_listener.add_shards({ shard1 });
    run();
    check_acquired_and_released({}, {});

    bool called = false;
    shards_distributor->get_owner(ctx, shard1, [&called](error_code err, const node_info&) {
        called = true;
        REQUIRE(err == error::not_owned);
    });
    run();
    REQUIRE(called);
}

TEST_CASE_METHOD(
    shards_distributor_test,
    "should return acquired shards if lock_manager and bucket_manager not specified")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    auto shards_distributor = create_shards_distributor(use_shards_listener);
    shards_listener.add_shards({ shard1, shard2 });
    run();

    shard_ids acquired_shards;
    shards_distributor->get_acquired_shards(
        [&acquired_shards](const shard_ids& shards) { acquired_shards = shards; });
    run();
    REQUIRE(acquired_shards.size() == 2);
}

TEST_CASE_METHOD(
    shards_distributor_test,
    "should return acquired shards when working with only lock_manager")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    auto shards_distributor = create_shards_distributor(use_lock_manager);
    shards_listener.add_shards({ shard1, shard2 });
    lock_manager.acquire_resource(shard1);
    run();

    shard_ids acquired_shards;
    shards_distributor->get_acquired_shards(
        [&acquired_shards](const shard_ids& shards) { acquired_shards = shards; });
    run();
    REQUIRE(acquired_shards.size() == 1);
    REQUIRE(acquired_shards[0] == shard1);
}

TEST_CASE_METHOD(
    shards_distributor_test,
    "should return acquired shards when working with bucket_manager")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    std::string bucket1 = "b1";
    std::string bucket2 = "b2";
    buckets[shard1] = bucket1;
    buckets[shard2] = bucket2;
    auto shards_distributor = create_shards_distributor(use_bucket_manager);
    shards_listener.add_shards({ shard1, shard2 });
    bucket_manager.acquire_bucket(bucket1);
    run();

    shard_ids acquired_shards;
    shards_distributor->get_acquired_shards(
        [&acquired_shards](const shard_ids& shards) { acquired_shards = shards; });
    run();
    REQUIRE(acquired_shards.size() == 1);
    REQUIRE(acquired_shards[0] == shard1);
}

TEST_CASE_METHOD(shards_distributor_test, "should return acquired buckets info")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    std::string bucket1 = "b1";
    std::string bucket2 = "b2";
    buckets[shard1] = bucket1;
    buckets[shard2] = bucket2;
    auto shards_distributor = create_shards_distributor(use_bucket_manager);
    shards_listener.add_shards({ shard1, shard2 });
    bucket_manager.acquire_bucket(bucket1);
    run();

    std::map<std::string /*bucket*/, shard_ids> acquired_buckets;
    shards_distributor->get_acquired_buckets_info(
        [&acquired_buckets](auto&& buckets) { acquired_buckets = buckets; });
    run();
    REQUIRE(acquired_buckets.size() == 1);
    REQUIRE(acquired_buckets.count(bucket1));
    REQUIRE(acquired_buckets[bucket1].size() == 1);
    REQUIRE(acquired_buckets[bucket1][0] == shard1);
}