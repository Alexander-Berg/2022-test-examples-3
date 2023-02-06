#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/property_tree/json_parser.hpp>
#include <boost/property_tree/ptree.hpp>

#include <mail/sharpei/include/internal/server/handlers/stat.h>
#include <mail/sharpei/include/internal/server/handlers/sharddb_stat.h>

#include "mocks.h"
#include <mail/sharpei/tests/mocks.h>

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::server::handlers;
using namespace boost::property_tree;
using cache::Cache;
using cache::RoleCache;
using cache::StateCache;
using Role = Shard::Database::Role;
using State = Shard::Database::State;

ptree to_ptree(const std::string& s) {
    std::stringstream ss;
    ss << s;
    ptree t;
    read_json(ss, t);
    assert(t.size() > 0);
    return t;
}

struct StatTest : public Test {
    boost::shared_ptr<MockStreamable> streamable;
    ymod_webserver::request_ptr request;
    boost::shared_ptr<MockStream> stream;
    cache::CachePtr cache;

    void SetUp() override {
        streamable = boost::make_shared<MockStreamable>();

        request = boost::make_shared<ymod_webserver::request>();
        request->method = ymod_webserver::methods::mth_get;
        request->context = boost::make_shared<ymod_webserver::context>();

        stream = boost::make_shared<MockStream>();

        cache = std::make_shared<Cache>(5, 3);

        for (auto i = 1; i <= 2; ++i) {
            addShardToCache(i);
        }
    }

    void addShardToCache(int shardId, const std::string& hostnameSuffix = "") {
        const Shard::Database::Address master{"master" + std::to_string(shardId) + hostnameSuffix, 5432, "dbname", "dc"};
        const Shard::Database::Address replica{"replica" + std::to_string(shardId) + hostnameSuffix, 5432, "dbname", "dc"};

        cache->shardName.update(shardId, "shard" + std::to_string(shardId));
        cache->role.update(shardId, {{master, RoleCache::OptRole(Role::Master)},{replica, RoleCache::OptRole(Role::Replica)}});
        cache->status.alive(shardId, master);
        cache->status.alive(shardId, replica);
        cache->state.update(shardId, {{master, StateCache::OptState(State {0})},{replica, StateCache::OptState(State {0})}});
    }
};

TEST_F(StatTest, stat_process_should_response_ok_with_content_type_application_json_and_result_body) {
    const InSequence s;

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_chunked()).WillOnce(Return(streamable));
    EXPECT_CALL(*streamable, client_stream())
            .WillOnce(Invoke([] { return yplatform::net::streamer_wrapper(new Streamer); }));

    Stat(cache).process(stream);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(streamable.get()));
}

TEST_F(StatTest, stat_new_process_should_response_ok_with_content_type_application_json_and_result_body) {
    const InSequence s;

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_chunked()).WillOnce(Return(streamable));
    EXPECT_CALL(*streamable, client_stream())
            .WillOnce(Invoke([] { return yplatform::net::streamer_wrapper(new Streamer); }));

    StatNew<sharpei::reflection::ShardWithStringIdAndPort>(cache).process(stream);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(streamable.get()));
}

TEST_F(StatTest, stat_new_process_should_respond_ok_with_all_shards_if_shard_id_not_passed) {
    const InSequence s;

    std::string response;

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_chunked()).WillOnce(Return(streamable));
    EXPECT_CALL(*streamable, client_stream())
            .WillOnce(Invoke([&] { return yplatform::net::streamer_wrapper(new Streamer{&response}); }));

    StatNew<sharpei::reflection::ShardWithStringIdAndPort>(cache).process(stream);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(streamable.get()));

    static constexpr char expected[] = R"({"1":{"id":"1","name":"shard1","databases":[{"address":{"host":"master1","port":"5432","dbname":"dbname","dataCenter":"dc"},"role":"master","status":"alive","state":{"lag":0}},{"address":{"host":"replica1","port":"5432","dbname":"dbname","dataCenter":"dc"},"role":"replica","status":"alive","state":{"lag":0}}]},"2":{"id":"2","name":"shard2","databases":[{"address":{"host":"master2","port":"5432","dbname":"dbname","dataCenter":"dc"},"role":"master","status":"alive","state":{"lag":0}},{"address":{"host":"replica2","port":"5432","dbname":"dbname","dataCenter":"dc"},"role":"replica","status":"alive","state":{"lag":0}}]}})";

    ASSERT_FALSE(response.empty());
    ASSERT_EQ(to_ptree(response), to_ptree(expected));
}

TEST_F(StatTest, stat_new_process_should_respond_ok_with_only_one_shard_info_if_shard_id_passed) {
    const InSequence s;

    std::string response;
    request->url.params.insert({"shard_id", sharpei::to_string(2)});

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_chunked()).WillOnce(Return(streamable));
    EXPECT_CALL(*streamable, client_stream())
            .WillOnce(Invoke([&] { return yplatform::net::streamer_wrapper(new Streamer{&response}); }));

    StatNew<sharpei::reflection::ShardWithStringIdAndPort>(cache).process(stream);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(streamable.get()));

    static constexpr char expected[] = R"({"2":{"id":"2","name":"shard2","databases":[{"address":{"host":"master2","port":"5432","dbname":"dbname","dataCenter":"dc"},"role":"master","status":"alive","state":{"lag":0}},{"address":{"host":"replica2","port":"5432","dbname":"dbname","dataCenter":"dc"},"role":"replica","status":"alive","state":{"lag":0}}]}})";

    ASSERT_FALSE(response.empty());
    ASSERT_EQ(to_ptree(response), to_ptree(expected));
}

TEST_F(StatTest, stat_new_process_should_respond_not_found_if_there_is_no_shard_with_given_id) {
    const InSequence s;

    std::string response;
    request->url.params.insert({"shard_id", sharpei::to_string(222)});

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::not_found, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(R"("shard not found: shard with shard_id=222 doesn't exist")"))
        .WillOnce(Return());

    StatNew<sharpei::reflection::ShardWithStringIdAndPort>(cache).process(stream);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(streamable.get()));
}

TEST_F(StatTest, stat_new_process_should_respond_bad_request_if_shard_id_is_invalid) {
    const InSequence s;

    std::string response;
    request->url.params.insert({"shard_id", "shto_ya_takoe"});

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("text", "plain")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(_))
        .WillOnce(Return());

    StatNew<sharpei::reflection::ShardWithStringIdAndPort>(cache).process(stream);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(streamable.get()));
}

TEST_F(StatTest, stat_new_strict_should_respond_internal_error_if_cache_is_empty) {
    const InSequence s;

    using sharpei::server::handlers::tags::Strict;

    cache = std::make_shared<cache::Cache>(13, 8);

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(_))
        .WillOnce(Return());

    StatNew<sharpei::reflection::Shard, Strict>(cache).process(stream);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(streamable.get()));
}

TEST_F(StatTest, sharddb_stat_process_should_response_ok_with_content_type_application_json_and_result_body) {
    const InSequence s;

    cache = std::make_shared<cache::Cache>(5, 3);
    std::string response;

    addShardToCache(0);

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_connection(false)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_chunked()).WillOnce(Return(streamable));
    EXPECT_CALL(*streamable, client_stream())
        .WillOnce(Invoke([&] { return yplatform::net::streamer_wrapper(new Streamer{&response}); }));

    SharddbStat(cache, makeTestConfig()).process(stream);

    static constexpr char expected[] = R"([{"address":{"host":"master0","port":5432,"dbname":"dbname","dataCenter":"dc"},"role":"master","status":"alive","state":{"lag":0}},{"address":{"host":"replica0","port":5432,"dbname":"dbname","dataCenter":"dc"},"role":"replica","status":"alive","state":{"lag":0}}])";

    EXPECT_EQ(expected, response);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(streamable.get()));
}

TEST_F(StatTest, sharddb_stat_should_respond_internal_error_if_cache_is_empty) {
    const InSequence s;

    cache = std::make_shared<cache::Cache>(5, 3);

    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(_)).WillOnce(Return());

    SharddbStat(cache, makeTestConfig()).process(stream);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(stream.get()));
}

} // namespace
