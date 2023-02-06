#include "catch.hpp"
#include "common.h"

#include <common/collector_info.h>
#include <streamer/streamers_loader.h>

uint32_t LOADER_CHUNK_SIZE = 5;

using namespace collectors;
using namespace collectors::streamer;

struct load_call_data
{
    std::string shard;
    uids uids;
    collector_info_chunk_cb cb;
};

using load_calls = std::vector<load_call_data>;
using load_calls_ptr = std::shared_ptr<load_calls>;

struct fake_load_op
{
    fake_load_op(load_calls_ptr load_calls) : load_calls(load_calls)
    {
    }

    void operator()(const std::string& shard, const uids& uids, collector_info_chunk_cb cb)
    {
        load_calls->emplace_back(load_call_data{ shard, uids, cb });
    }

    load_calls_ptr load_calls;
};

using loader_type = streamers_loader<fake_load_op>;
using loader_ptr = std::shared_ptr<loader_type>;

class streamers_loader_test
{
public:
    streamers_loader_test()
    {
        load_calls = std::make_shared<::load_calls>();
        loader = std::make_shared<loader_type>(&io_, LOADER_CHUNK_SIZE, fake_load_op(load_calls));
        loader->set_handler(std::bind(&streamers_loader_test::handle_load, this, ph::_1, ph::_2));
    }

    void run_io()
    {
        io_.reset();
        io_.run();
    }

    void handle_load(shard_id /*shard*/, loaded_users res)
    {
        load_result.swap(res);
    }

    boost::asio::io_context io_;
    loaded_users load_result;
    load_calls_ptr load_calls;
    loader_ptr loader;
};

TEST_CASE_METHOD(streamers_loader_test, "normal_load")
{
    shard_id shard = "123";
    uids uids = { "1", "2", "3", "4" };

    loader->process(shard, uids);
    run_io();

    REQUIRE(load_calls->size() == 1);
    REQUIRE(load_result.empty());

    auto call_data = load_calls->front();
    REQUIRE(call_data.shard == shard);
    REQUIRE(call_data.uids == uids);

    call_data.cb({}, {});
    run_io();

    REQUIRE(load_calls->size() == 1);
    REQUIRE(load_result.size() == uids.size());
    for (auto& [uid, data] : load_result)
    {
        REQUIRE(data.empty());
        REQUIRE(std::find(uids.begin(), uids.end(), uid) != uids.end());
    }
}

TEST_CASE_METHOD(streamers_loader_test, "big_chunk_load")
{
    shard_id shard = "123";
    uids uids = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" };

    loader->process(shard, uids);
    run_io();

    REQUIRE(load_calls->size() == 1);
    REQUIRE(load_result.empty());

    auto first_call_data = load_calls->front();
    REQUIRE(first_call_data.shard == shard);
    REQUIRE(first_call_data.uids.size() == LOADER_CHUNK_SIZE);

    first_call_data.cb({}, {});
    run_io();

    REQUIRE(load_calls->size() == 2);
    REQUIRE(load_result.size() == first_call_data.uids.size());
    for (auto& [uid, data] : load_result)
    {
        REQUIRE(data.empty());
        REQUIRE(
            std::find(first_call_data.uids.begin(), first_call_data.uids.end(), uid) !=
            first_call_data.uids.end());
    }
}

TEST_CASE_METHOD(streamers_loader_test, "queued_load")
{
    shard_id shard = "123";
    uids first_uids = { "1", "2" };
    uids second_uids = { "3", "4" };

    loader->process(shard, first_uids);
    run_io();

    REQUIRE(load_calls->size() == 1);
    REQUIRE(load_result.empty());

    auto call_data = load_calls->front();
    REQUIRE(call_data.uids == first_uids);

    loader->process(shard, second_uids);
    run_io();

    REQUIRE(load_calls->size() == 1);

    call_data.cb({}, {});
    run_io();

    REQUIRE(load_calls->size() == 2);
    REQUIRE(load_calls->back().uids == second_uids);
}

TEST_CASE_METHOD(streamers_loader_test, "parallel_load")
{
    shard_id shard1 = "123";
    shard_id shard2 = "456";
    uids first_uids = { "1", "2" };
    uids second_uids = { "3", "4" };

    loader->process(shard1, first_uids);
    run_io();
    loader->process(shard2, second_uids);
    run_io();

    REQUIRE(load_calls->size() == 2);
    REQUIRE(load_result.empty());

    REQUIRE(load_calls->front().uids == first_uids);
    REQUIRE(load_calls->back().uids == second_uids);
}

TEST_CASE_METHOD(streamers_loader_test, "priority_load")
{
    shard_id shard = "123";
    uids normal_uids = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
    uids prior_uids = { "10", "20", "30", "40", "50" };

    loader->process(shard, normal_uids);
    run_io();
    loader->process_prior(shard, prior_uids);
    run_io();

    REQUIRE(load_calls->size() == 1);

    load_calls->front().cb({}, {});
    run_io();

    REQUIRE(load_calls->size() == 2);
    REQUIRE(load_calls->back().uids == prior_uids);
}

TEST_CASE_METHOD(streamers_loader_test, "multiple_load")
{
    shard_id shard = "123";
    uids uids = { "1", "2" };

    loader->process(shard, uids);
    run_io();
    loader->process(shard, uids);
    run_io();

    REQUIRE(load_calls->size() == 1);
}

TEST_CASE_METHOD(streamers_loader_test, "cancel_uids")
{
    shard_id shard = "123";
    uids uids = { "1", "2" };

    loader->process(shard, uids);
    run_io();
    for (auto& uid : uids)
    {
        loader->cancel(shard, uid);
        run_io();
    }
    REQUIRE(load_calls->size() == 1);
    load_calls->front().cb({}, {});
    run_io();

    REQUIRE(load_result.empty());
}

TEST_CASE_METHOD(streamers_loader_test, "cancel_shard")
{
    shard_id shard = "123";
    uids uids = { "1", "2" };

    loader->process(shard, uids);
    run_io();
    loader->cancel(shard);
    run_io();

    REQUIRE(load_calls->size() == 1);
    load_calls->front().cb({}, {});
    run_io();

    REQUIRE(load_result.empty());
}

TEST_CASE_METHOD(streamers_loader_test, "process_after_cancel")
{
    shard_id shard = "123";
    uids uids = { "1", "2" };

    loader->process(shard, uids);
    run_io();
    for (auto& uid : uids)
    {
        loader->cancel(shard, uid);
        run_io();
    }
    loader->process(shard, uids);
    run_io();

    REQUIRE(load_calls->size() == 1);

    load_calls->front().cb({}, {});
    run_io();

    REQUIRE(load_result.size() == uids.size());
    for (auto& [uid, data] : load_result)
    {
        REQUIRE(data.empty());
        REQUIRE(std::find(uids.begin(), uids.end(), uid) != uids.end());
    }
}

TEST_CASE_METHOD(streamers_loader_test, "prior_process_after_cancel")
{
    shard_id shard = "123";
    uids uids = { "1", "2" };

    loader->process(shard, uids);
    run_io();
    for (auto& uid : uids)
    {
        loader->cancel(shard, uid);
        run_io();
    }
    loader->process_prior(shard, uids);
    run_io();

    REQUIRE(load_calls->size() == 1);

    load_calls->front().cb({}, {});
    run_io();

    REQUIRE(load_result.size() == uids.size());
    for (auto& [uid, data] : load_result)
    {
        REQUIRE(data.empty());
        REQUIRE(std::find(uids.begin(), uids.end(), uid) != uids.end());
    }
}

TEST_CASE_METHOD(streamers_loader_test, "process_after_cancel_prior")
{
    shard_id shard = "123";
    uids uids = { "1", "2" };

    loader->process_prior(shard, uids);
    run_io();
    for (auto& uid : uids)
    {
        loader->cancel(shard, uid);
        run_io();
    }
    loader->process(shard, uids);
    run_io();

    REQUIRE(load_calls->size() == 1);

    load_calls->front().cb({}, {});
    run_io();

    REQUIRE(load_result.size() == uids.size());
    for (auto& [uid, data] : load_result)
    {
        REQUIRE(data.empty());
        REQUIRE(std::find(uids.begin(), uids.end(), uid) != uids.end());
    }
}
