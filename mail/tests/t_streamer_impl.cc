#include "catch.hpp"
#include "common.h"

#include <streamer/planner.h>
#include <streamer/streamers_loader.h>
#include <streamer/streamer_impl.h>

#include <mail_errors/error_code.h>
#include <yplatform/reactor.h>

#include <atomic>

using namespace collectors;
using namespace collectors::streamer;

struct fake_planner
{
    using remove_handler_type = std::function<void(const global_collector_id&)>;

    void set_remove_handler(remove_handler_type h)
    {
        remove_handler = h;
    }

    void add(global_collector_id id, task_function f)
    {
        planned[id] = f;
    }

    void remove(global_collector_id id)
    {
        planned.erase(id);
        removed.insert(id);
    }

    void stop(global_collector_id id)
    {
        stopped.insert(id);
    }

    remove_handler_type remove_handler;
    std::map<global_collector_id, task_function> planned;
    std::set<global_collector_id> removed;
    std::set<global_collector_id> stopped;
};

struct fake_loader
{
    struct processed_data
    {
        shard_id shard;
        uids uids;
    };

    struct cancelled_data
    {
        shard_id shard;
        uid uid;
    };

    template <typename Handler>
    void set_handler(Handler&& h)
    {
        handler = h;
    }

    void process(const shard_id& shard, const uids& uids)
    {
        processed.push_back({ shard, uids });
    }

    void process_prior(const shard_id& shard, const uids& uids)
    {
        processed_prior.push_back({ shard, uids });
    }

    void cancel(const shard_id& shard, const uid& uid)
    {
        cancelled.push_back({ shard, uid });
    }

    void cancel(const shard_id& shard)
    {
        cancelled_shards.push_back(shard);
    }

    std::function<void(shard_id, loaded_users)> handler;
    std::vector<processed_data> processed;
    std::vector<processed_data> processed_prior;
    std::vector<cancelled_data> cancelled;
    std::vector<shard_id> cancelled_shards;
};

struct fake_macs
{
    template <typename Ctx, typename Handler>
    void get_user_shard_id(Ctx /*ctx*/, uid /*uid*/, Handler&& h)
    {
        h(mail_errors::error_code(), result_shard);
    }

    shard_id result_shard;
};

using streamer_type = detail::streamer_impl<fake_planner*, fake_loader*, fake_macs*>;
using streamer_ptr = std::shared_ptr<streamer_type>;

class streamer_impl_test
{
public:
    streamer_impl_test()
    {
        reactor.init(1, 1);
        auto settings = std::make_shared<streamer_settings>();
        streamer = std::make_shared<streamer_type>(reactor, &planner, &loader, &macs, settings);
        streamer->init();

        int collector_id = 1;
        for (auto& uid : test_uids)
        {
            for (auto i = 0; i < collectors_per_user; ++i)
            {
                src_data[uid].emplace_back(collector_id++, uid, uid + std::to_string(i));
            }
        }
        macs.result_shard = test_shard;
    }

    template <typename Op>
    void run_on_io(Op&& op)
    {
        reactor.io()->post(op);
        reactor.io()->reset();
        reactor.io()->run();
    }

    void run_io()
    {
        reactor.io()->reset();
        reactor.io()->run();
    }

    void load_test_users()
    {
        run_on_io([&]() { streamer->on_acquire_accounts(test_shard, sharder_uids()); });
        run_on_io([&]() { loader.handler(test_shard, src_data); });
    }

    global_collector_id load_test_collector()
    {
        auto uid = test_uids.front();
        streamer->on_acquire_accounts(test_shard, { std::stoull(uid) });
        run_io();
        loader.handler(test_shard, src_data);

        return global_collector_id(uid, src_data[uid].front().id);
    }

    std::vector<uint64_t> sharder_uids()
    {
        std::vector<uint64_t> uids;
        for (auto& uid : test_uids)
        {
            uids.push_back(std::stoull(uid));
        }
        return uids;
    }

    shard_id test_shard = "123";
    shard_id second_shard = "456";
    uids test_uids = { "1", "2", "3", "4" };
    int collectors_per_user = 2;
    loaded_users src_data;

    yplatform::reactor reactor;
    fake_planner planner;
    fake_loader loader;
    fake_macs macs;
    streamer_ptr streamer;
    context_ptr ctx = boost::make_shared<yplatform::task_context>();
};

TEST_CASE_METHOD(streamer_impl_test, "acquired_users")
{
    run_on_io([&]() {
        std::vector<uint64_t> acquired_uids;
        for (auto& uid : test_uids)
        {
            acquired_uids.push_back(std::stoull(uid));
        }
        streamer->on_acquire_accounts(test_shard, acquired_uids);
    });

    run_on_io([&]() {
        REQUIRE(loader.processed.size() == 1);
        auto data = loader.processed.front();

        REQUIRE(data.shard == test_shard);
        REQUIRE(data.uids == test_uids);
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::initial);
                REQUIRE(user->shard == test_shard);
            });
        }
        REQUIRE(planner.planned.size() == 0);
    });
}

TEST_CASE_METHOD(streamer_impl_test, "loaded_users")
{
    run_on_io([&]() {
        std::vector<uint64_t> acquired_uids;
        for (auto& uid : test_uids)
        {
            acquired_uids.push_back(std::stoull(uid));
        }
        streamer->on_acquire_accounts(test_shard, acquired_uids);
    });

    loader.handler(test_shard, src_data);

    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::full_ready);
                REQUIRE(user->shard == test_shard);
                REQUIRE(user->streamers.size() == src_data[uid].size());
                REQUIRE(user->streamers.size() != 0);
                auto streamer = user->streamers.begin()->second;
                REQUIRE(planner.planned.count(streamer->global_id()));
            });
        }
    });
}

TEST_CASE_METHOD(streamer_impl_test, "not_acquired_users")
{
    loader.handler(test_shard, src_data);

    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [](error ec, user_ptr user) {
                REQUIRE(ec == code::user_not_found);
                REQUIRE(!user);
            });
        }
        REQUIRE(planner.planned.size() == 0);
    });
}

TEST_CASE_METHOD(streamer_impl_test, "implicit_load_users")
{
    std::atomic_uint cb_calls{ 0 };
    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->load_user(nullptr, uid, [&](error ec, user_ptr user) {
                cb_calls++;
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::api_ready);
                REQUIRE(user->shard == test_shard);
            });
        }
    });

    loader.handler(test_shard, src_data);

    run_on_io([&]() {
        REQUIRE(loader.processed.size() == 0);
        REQUIRE(loader.processed_prior.size() == src_data.size());
        for (auto& data : loader.processed_prior)
        {
            REQUIRE(data.shard == test_shard);
            REQUIRE(src_data.count(data.uids.front()));
        }

        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::api_ready);
                REQUIRE(user->shard == test_shard);
            });
        }
        REQUIRE(planner.planned.size() == 0);
    });
    REQUIRE(cb_calls == src_data.size());
}

TEST_CASE_METHOD(streamer_impl_test, "acquire_implicitly_loaded")
{
    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->load_user(nullptr, uid, [&](error /*ec*/, user_ptr /*user*/) {});
        }
    });

    loader.processed.clear();
    loader.processed_prior.clear();
    loader.handler(test_shard, src_data);
    std::vector<uint64_t> acquired_uids;
    for (auto& uid : test_uids)
    {
        acquired_uids.push_back(std::stoull(uid));
    }
    streamer->on_acquire_accounts(test_shard, acquired_uids);

    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::full_ready);
                REQUIRE(user->shard == test_shard);
                REQUIRE(user->streamers.size() == src_data[uid].size());
                REQUIRE(user->streamers.size() != 0);
                auto streamer = user->streamers.begin()->second;
                REQUIRE(planner.planned.count(streamer->global_id()));
            });
        }
        REQUIRE(loader.processed.size() == 0);
        REQUIRE(loader.processed_prior.size() == 0);
    });
}

TEST_CASE_METHOD(streamer_impl_test, "multiple_loads")
{
    int iterations = 3;

    std::atomic_uint cb_calls{ 0 };
    for (int i = 0; i < iterations; ++i)
    {
        run_on_io([&]() {
            for (auto& [uid, data] : src_data)
            {
                streamer->load_user(nullptr, uid, [&](error ec, user_ptr user) {
                    cb_calls++;
                    REQUIRE(!ec);
                    REQUIRE(user);
                    REQUIRE(user->state == user_state::api_ready);
                    REQUIRE(user->shard == test_shard);
                });
            }
        });

        loader.handler(test_shard, src_data);
    }

    run_on_io([&]() {
        REQUIRE(loader.processed.size() == 0);
        REQUIRE(loader.processed_prior.size() == src_data.size());
        REQUIRE(planner.planned.size() == 0);
    });

    REQUIRE(cb_calls == src_data.size() * iterations);
};

TEST_CASE_METHOD(streamer_impl_test, "add_new_streamers_new_user")
{
    auto it = src_data.begin();
    run_on_io([&]() { streamer->add_new_streamers(it->first, it->second); });

    REQUIRE(loader.processed.size() == 0);
    REQUIRE(loader.processed_prior.size() == 0);
    REQUIRE(planner.planned.size() == 0);

    run_on_io([&]() {
        streamer->get_user(it->first, [](error ec, user_ptr user) {
            REQUIRE(ec == code::user_not_found);
            REQUIRE(!user);
        });
    });
}

TEST_CASE_METHOD(streamer_impl_test, "add_new_streamers_existing_user")
{
    auto uid = test_uids.front();
    collector_info first_info(1, uid, uid + "00");
    collector_info second_info(2, uid, uid + "01");

    run_on_io([&]() { streamer->on_acquire_accounts(test_shard, { std::stoull(uid) }); });

    loaded_users res;
    res.insert({ uid, { first_info } });
    loader.handler(test_shard, res);

    run_on_io([&]() { streamer->add_new_streamers(uid, { second_info }); });

    run_on_io([&]() {
        streamer->get_user(uid, [&](error ec, user_ptr user) {
            REQUIRE(!ec);
            REQUIRE(user);

            REQUIRE(user->streamers.size() == 2);
            for (auto& [id, streamer] : user->streamers)
            {
                REQUIRE(planner.planned.count(streamer->global_id()));
            }
        });
    });
}

TEST_CASE_METHOD(streamer_impl_test, "load_existing_user")
{
    run_on_io([&]() {
        std::vector<uint64_t> acquired_uids;
        for (auto& uid : test_uids)
        {
            acquired_uids.push_back(std::stoull(uid));
        }
        streamer->on_acquire_accounts(test_shard, acquired_uids);
    });
    loader.handler(test_shard, src_data);

    loader.processed.clear();
    loader.processed_prior.clear();

    run_on_io([&]() {
        streamer->load_user(nullptr, test_uids.front(), [](error ec, user_ptr user) {
            REQUIRE(!ec);
            REQUIRE(user);
        });
    });

    REQUIRE(loader.processed.size() == 0);
    REQUIRE(loader.processed_prior.size() == 0);
}

TEST_CASE_METHOD(streamer_impl_test, "load_new_user")
{
    bool cb_called = false;
    run_on_io([&]() {
        streamer->load_user(nullptr, test_uids.front(), [&](error ec, user_ptr user) {
            cb_called = true;
            REQUIRE(!ec);
            REQUIRE(user);
        });
    });

    loaded_users res;
    res.insert({ test_uids.front(), src_data[test_uids.front()] });
    loader.handler(test_shard, res);

    run_on_io([&]() {
        streamer->get_user(test_uids.front(), [&](error ec, user_ptr user) {
            REQUIRE(!ec);
            REQUIRE(user);
        });
    });
    REQUIRE(cb_called);
}

TEST_CASE_METHOD(streamer_impl_test, "double_load_new_user")
{
    std::atomic_int cb_called_times{ 0 };
    auto iterations = 3;
    run_on_io([&]() {
        for (auto i = 0; i < iterations; ++i)
        {
            streamer->load_user(nullptr, test_uids.front(), [&](error ec, user_ptr user) {
                cb_called_times++;
                REQUIRE(!ec);
                REQUIRE(user);
            });
        }
    });

    run_on_io([&]() {
        loaded_users res;
        res.insert({ test_uids.front(), src_data[test_uids.front()] });
        loader.handler(test_shard, res);
    });

    REQUIRE(cb_called_times == iterations);
}

TEST_CASE_METHOD(streamer_impl_test, "release_not_loaded")
{
    std::vector<uint64_t> uids;
    for (auto& uid : test_uids)
    {
        uids.push_back(std::stoull(uid));
    }

    run_on_io([&]() {
        streamer->on_acquire_accounts(test_shard, uids);
        streamer->on_release_accounts(test_shard, uids);
    });

    REQUIRE(loader.processed.size() == 1);
    REQUIRE(loader.cancelled.size() == test_uids.size());
    for (auto& data : loader.cancelled)
    {
        REQUIRE(data.shard == test_shard);
        REQUIRE(src_data.count(data.uid));
    }

    loader.handler(test_shard, src_data);

    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(ec);
                REQUIRE(!user);
            });
        }
    });
}

TEST_CASE_METHOD(streamer_impl_test, "normal_release")
{
    std::vector<uint64_t> uids;
    for (auto& uid : test_uids)
    {
        uids.push_back(std::stoull(uid));
    }

    run_on_io([&]() { streamer->on_acquire_accounts(test_shard, uids); });

    run_on_io([&]() { loader.handler(test_shard, src_data); });
    REQUIRE(planner.planned.size() == uids.size() * collectors_per_user);

    run_on_io([&]() {
        streamer->on_release_accounts(test_shard, uids);
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user->state == user_state::released);
            });
        }
    });

    REQUIRE(planner.removed.size() == uids.size() * collectors_per_user);
    run_on_io([&]() {
        for (auto& id : planner.removed)
        {
            planner.remove_handler(id);
        }
    });

    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [](error ec, user_ptr user) {
                REQUIRE(ec == code::user_not_found);
                REQUIRE(!user);
            });
        }
    });
}

TEST_CASE_METHOD(streamer_impl_test, "release_not_acquired")
{
    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->load_user(nullptr, uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
            });
        }
    });

    loader.handler(test_shard, src_data);

    run_on_io([&]() {
        streamer->on_release_shards({ test_shard });
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [](error ec, user_ptr user) {
                REQUIRE(ec == code::user_not_found);
                REQUIRE(!user);
            });
        }
    });

    REQUIRE(planner.planned.size() == 0);
}

TEST_CASE_METHOD(streamer_impl_test, "reacquire_released")
{
    std::vector<uint64_t> uids;
    for (auto& uid : test_uids)
    {
        uids.push_back(std::stoull(uid));
    }

    run_on_io([&]() { streamer->on_acquire_accounts(test_shard, uids); });

    run_on_io([&]() { loader.handler(test_shard, src_data); });
    REQUIRE(planner.planned.size() == uids.size() * collectors_per_user);
    loader.processed.clear();

    run_on_io([&]() {
        streamer->on_release_accounts(test_shard, uids);
        streamer->on_acquire_accounts(test_shard, uids);
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::initial);
                REQUIRE(user->shard == test_shard);
            });
        }
    });

    REQUIRE(planner.planned.size() == 0);
    run_on_io([&]() {
        for (auto& id : planner.removed)
        {
            planner.remove_handler(id);
        }
    });
    REQUIRE(loader.processed.size() == uids.size());
}

TEST_CASE_METHOD(streamer_impl_test, "acquire_released_then_release_again")
{
    load_test_users();
    run_on_io([&]() {
        auto uids = sharder_uids();
        streamer->on_release_accounts(test_shard, uids);
        streamer->on_acquire_accounts(test_shard, uids);
        streamer->on_release_accounts(test_shard, uids);
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::released);
            });
        }
    });
}

TEST_CASE_METHOD(streamer_impl_test, "acquire_on_different_shard")
{
    load_test_users();
    run_on_io([&]() { streamer->on_acquire_accounts(second_shard, sharder_uids()); });
    run_on_io([&]() {
        for (auto& id : planner.removed)
        {
            planner.remove_handler(id);
        }
    });

    REQUIRE(loader.processed.size() == test_uids.size() + 1);
    run_on_io([&]() { loader.handler(second_shard, src_data); });

    run_on_io([&]() {
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::full_ready);
                REQUIRE(user->shard == second_shard);
            });
        }
    });
}

TEST_CASE_METHOD(streamer_impl_test, "release_on_different_shard")
{
    load_test_users();
    run_on_io([&]() {
        streamer->on_release_accounts(second_shard, sharder_uids());
        for (auto& uid : test_uids)
        {
            streamer->get_user(uid, [&](error ec, user_ptr user) {
                REQUIRE(!ec);
                REQUIRE(user);
                REQUIRE(user->state == user_state::full_ready);
                REQUIRE(user->shard == test_shard);
            });
        }
    });
}

TEST_CASE_METHOD(streamer_impl_test, "streamer_post_operation")
{
    auto collector_id = load_test_collector();
    callback<context_ptr, streamer_data_ptr, no_data_cb> operation;
    streamer->post_operation(ctx, collector_id, operation::edit, operation);
    run_io();
    REQUIRE(operation.called());
}

TEST_CASE_METHOD(streamer_impl_test, "streamer_post_multiple_operations")
{
    auto collector_id = load_test_collector();

    callback<context_ptr, streamer_data_ptr, no_data_cb> first_operation, second_operation;
    streamer->post_operation(ctx, collector_id, operation::edit, first_operation);
    streamer->post_operation(ctx, collector_id, operation::edit, second_operation);
    run_io();
    REQUIRE(!second_operation.called());

    auto [ctx, streamer, cb] = first_operation.args();
    cb({});
    run_io();
    REQUIRE(second_operation.called());
}

TEST_CASE_METHOD(streamer_impl_test, "streamer_release_with_active_operation")
{
    auto collector_id = load_test_collector();
    auto check_user = [&](user_state state) {
        callback<error, user_ptr> user_cb;
        streamer->get_user(collector_id.uid, user_cb);
        run_io();
        auto [ec, user] = user_cb.args();
        REQUIRE(!ec);
        REQUIRE(user->state == state);
    };

    // Post operation and release user
    callback<context_ptr, streamer_data_ptr, no_data_cb> operation;
    streamer->post_operation(ctx, collector_id, operation::edit, operation);
    streamer->on_release_accounts(test_shard, { std::stoull(collector_id.uid) });
    run_io();
    // User should be there, because of active op
    check_user(user_state::released);

    // Release streamer from planner
    REQUIRE(planner.removed.size());
    for (auto& id : planner.removed)
    {
        planner.remove_handler(id);
    }
    // User should be there, because of active op
    check_user(user_state::released);

    // Complete running op
    auto [ctx, streamer_data, op_cb] = operation.args();
    op_cb({});

    // User shouldn't exist
    callback<error, user_ptr> user_cb;
    streamer->get_user(collector_id.uid, user_cb);
    run_io();
    auto [ec, user] = user_cb.args();
    REQUIRE(ec == code::user_not_found);
    REQUIRE(!user);
}
