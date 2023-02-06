#include "fake_sharpei_client.h"
#include "shards_listener.h"
#include <catch.hpp>

namespace ymod_mdb_sharder {

struct shards_listener_test : yplatform::log::contains_logger
{
    using shards_listener_type = shards_listener<fake_sharpei_client*>;

    shards_listener_test()
    {
        logger().append_log_prefix("shards_listener_test");

        shards_listener =
            std::make_shared<shards_listener_type>(&io, ctx, polling_interval, &sharpei);
        shards_listener->subscribe(
            std::bind(&shards_listener_test::on_add_shards, this, ph::_1),
            std::bind(&shards_listener_test::on_del_shards, this, ph::_1));
        yplatform::spawn(shards_listener);
    }

    void on_add_shards(const shard_ids& shards)
    {
        for (auto& shard : shards)
        {
            YLOG_L(info) << "add shard " << shard;
        }
        added_shards.insert(shards.begin(), shards.end());
    }

    void on_del_shards(const shard_ids& shards)
    {
        for (auto& shard : shards)
        {
            YLOG_L(info) << "del shard " << shard;
        }
        deleted_shards.insert(shards.begin(), shards.end());
    }

    void run()
    {
        io.run_for(polling_interval + time_traits::milliseconds(1));
        io.reset();
    }

    void check_added_and_deleted(const shard_ids& added_ids, const shard_ids& deleted_ids)
    {
        REQUIRE(added_shards.size() == added_ids.size());
        REQUIRE(deleted_shards.size() == deleted_ids.size());
        for (auto& id : added_ids)
        {
            REQUIRE(added_shards.count(id));
        }
        for (auto& id : deleted_ids)
        {
            REQUIRE(deleted_shards.count(id));
        }
        added_shards.clear();
        deleted_shards.clear();
    }

    fake_sharpei_client sharpei;
    boost::asio::io_service io;
    task_context_ptr ctx = boost::make_shared<yplatform::task_context>();
    time_traits::duration polling_interval = time_traits::milliseconds(2);
    std::shared_ptr<shards_listener_type> shards_listener;
    std::set<shard_id> added_shards;
    std::set<shard_id> deleted_shards;
};

TEST_CASE_METHOD(shards_listener_test, "should notify about added shards")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    sharpei.add_shards({ shard1, shard2 });
    run();
    check_added_and_deleted({ shard1, shard2 }, {});
}

TEST_CASE_METHOD(shards_listener_test, "should notify about deleted shards")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    sharpei.add_shards({ shard1, shard2 });
    run();
    check_added_and_deleted({ shard1, shard2 }, {});

    sharpei.del_shards({ shard1 });
    run();
    check_added_and_deleted({}, { shard1 });
}

TEST_CASE_METHOD(shards_listener_test, "should return shards")
{
    shard_id shard1 = "xdb1", shard2 = "xdb2";
    sharpei.add_shards({ shard1, shard2 });
    run();

    shard_ids all_shards;
    shards_listener->get_shards([&all_shards](auto&& shards) { all_shards = shards; });
    run();
    REQUIRE(all_shards.size() == 2);
}

}