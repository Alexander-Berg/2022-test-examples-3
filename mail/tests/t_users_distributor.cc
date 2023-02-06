#include "fake_shards_distributor.h"
#include "fake_sharpei_client.h"
#include "fake_users_storage.h"
#include "users_distributor_impl.h"
#include <ymod_mdb_sharder/errors.h>
#include <catch.hpp>

namespace ymod_mdb_sharder {

struct users_distributor_test : yplatform::log::contains_logger
{
    using users_distributor_type =
        users_distributor_impl<fake_shards_distributor*, fake_sharpei_client*>;

    users_distributor_test()
    {
        logger().append_log_prefix("users_distributor_test");

        my_node = node_info{ shards_distributor.my_node_id(), "localhost" };
        other_node = node_info{ "other_node_id", "other_node.net" };

        users_polling_settings.get_changed_users_interval = time_traits::milliseconds(2);
        users_polling_settings.get_all_users_interval = time_traits::milliseconds(10);

        users_distributor = std::make_shared<users_distributor_type>(
            io, ctx, &shards_distributor, &sharpei, users_polling_settings);
        users_distributor->init();
        users_distributor->set_polling_methods(
            std::bind(&fake_users_storage::get_all_users, &users_storage, ph::_1, ph::_2),
            std::bind(
                &fake_users_storage::get_changed_users,
                &users_storage,
                ph::_1,
                ph::_2,
                ph::_3,
                ph::_4));
        users_distributor->subscribe(
            std::bind(&users_distributor_test::on_acquire, this, ph::_1, ph::_2),
            std::bind(&users_distributor_test::on_release, this, ph::_1, ph::_2));
    }

    ~users_distributor_test()
    {
        users_distributor->fini();
    }

    void on_acquire(const shard_id& shard, const std::vector<uid_t>& uids)
    {
        for (auto& uid : uids)
        {
            YLOG_L(info) << "acquire uid=" << uid << " shard=" << shard;
            acquired_users.emplace(shard, uid);
        }
    }

    void on_release(const shard_id& shard, const std::vector<uid_t>& uids)
    {
        for (auto& uid : uids)
        {
            YLOG_L(info) << "release uid=" << uid << " shard=" << shard;
            released_users.emplace(uid);
        }
    }

    void run()
    {
        io.run_for(
            users_polling_settings.get_changed_users_interval + time_traits::milliseconds(1));
        io.reset();
    }

    void check_acquired_and_released(
        const std::vector<std::pair<shard_id, uid_t>>& acquired,
        const std::vector<uid_t>& released)
    {
        REQUIRE(acquired_users.size() == acquired.size());
        REQUIRE(released_users.size() == released.size());
        for (auto& pair : acquired)
        {
            REQUIRE(acquired_users.count(pair));
        }
        for (auto& uid : released)
        {
            REQUIRE(released_users.count(uid));
        }
        acquired_users.clear();
        released_users.clear();
    }

    void add_user(uid_t uid, const shard_id& shard)
    {
        sharpei.assign_user_to_shard(uid, shard);
        users_storage.add_user(uid, shard);
    }

    void delete_user(uid_t uid)
    {
        users_storage.delete_user(uid);
    }

    void move_user(uid_t uid, const shard_id& shard_to)
    {
        sharpei.assign_user_to_shard(uid, shard_to);
        users_storage.move_user(uid, shard_to);
    }

    fake_sharpei_client sharpei;
    fake_shards_distributor shards_distributor;
    fake_users_storage users_storage;
    std::shared_ptr<users_distributor_type> users_distributor;
    boost::asio::io_service io;
    users_listener::settings users_polling_settings;
    task_context_ptr ctx = boost::make_shared<yplatform::task_context>();
    std::set<std::pair<shard_id, uid_t>> acquired_users;
    std::set<uid_t> released_users;
    node_info my_node;
    node_info other_node;
};

TEST_CASE_METHOD(users_distributor_test, "should acquire users")
{
    uid_t user1 = 100, user2 = 200, user3 = 400;
    shard_id shard1 = "xdb1", shard2 = "xdb2";

    shards_distributor.set_owner(shard1, my_node);
    shards_distributor.set_owner(shard2, my_node);
    add_user(user1, shard1);
    add_user(user2, shard1);
    add_user(user3, shard2);
    run();
    check_acquired_and_released({ { shard1, user1 }, { shard1, user2 }, { shard2, user3 } }, {});
}

TEST_CASE_METHOD(users_distributor_test, "should release users when shard released")
{
    uid_t user1 = 100;
    shard_id shard1 = "xdb1";

    shards_distributor.set_owner(shard1, my_node);
    add_user(user1, shard1);
    run();
    check_acquired_and_released({ { shard1, user1 } }, {});

    shards_distributor.reset_owner(shard1);
    run();
    check_acquired_and_released({}, { user1 });
}

TEST_CASE_METHOD(users_distributor_test, "should call release when user deleted")
{
    uid_t user1 = 100;
    shard_id shard1 = "xdb1";

    shards_distributor.set_owner(shard1, my_node);
    add_user(user1, shard1);
    run();
    check_acquired_and_released({ { shard1, user1 } }, {});

    delete_user(user1);
    run();
    check_acquired_and_released({}, { user1 });
}

TEST_CASE_METHOD(
    users_distributor_test,
    "should call release and acquire with new shard when user moved")
{
    uid_t user1 = 100;
    shard_id shard1 = "xdb1", shard2 = "xdb2";

    shards_distributor.set_owner(shard1, my_node);
    shards_distributor.set_owner(shard2, my_node);
    add_user(user1, shard1);
    run();
    check_acquired_and_released({ { shard1, user1 } }, {});

    move_user(user1, shard2);
    run();
    check_acquired_and_released({ { shard2, user1 } }, { user1 });
}

TEST_CASE_METHOD(users_distributor_test, "should get owner for acquired users")
{
    uid_t user1 = 100;
    shard_id shard1 = "xdb1";

    shards_distributor.set_owner(shard1, my_node);
    add_user(user1, shard1);
    run();
    check_acquired_and_released({ { shard1, user1 } }, {});

    bool called = false;
    users_distributor->get_owner(ctx, user1, [this, &called](error_code err, node_info owner) {
        called = true;
        REQUIRE(!err);
        REQUIRE(owner.id == my_node.id);
        REQUIRE(owner.host == my_node.host);
    });
    run();
    REQUIRE(called);
}

TEST_CASE_METHOD(users_distributor_test, "should get owner for users acquired by other node")
{
    uid_t user1 = 100;
    shard_id shard1 = "xdb1";

    shards_distributor.set_owner(shard1, other_node);
    add_user(user1, shard1);
    bool called = false;
    users_distributor->get_owner(ctx, user1, [this, &called](error_code err, node_info owner) {
        called = true;
        REQUIRE(!err);
        REQUIRE(owner.id == other_node.id);
        REQUIRE(owner.host == other_node.host);
    });
    run();
    REQUIRE(called);
}

TEST_CASE_METHOD(users_distributor_test, "should get owner for not owned users")
{
    uid_t user1 = 100;
    shard_id shard1 = "xdb1";

    add_user(user1, shard1);
    bool called = false;
    users_distributor->get_owner(ctx, user1, [&called](error_code err, node_info) {
        called = true;
        REQUIRE(err == error::not_owned);
    });
    run();
    REQUIRE(called);
}

}