#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/server/handlers/detail/create_user_performer.h>

#include "../../mocks.h"
#include "mocks.h"

namespace sharpei {
namespace db {

template <class T>
bool operator ==(const CreateUserParams<T>& lhs, const CreateUserParams<T>& rhs) {
    return lhs.uid == rhs.uid && lhs.shardId == rhs.shardId;
}

} // namespace db
} // namespace sharpei

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::db;
using namespace sharpei::server;

struct WithInt64Uid {
    using UserIdValue = std::int64_t;
    static constexpr UserIdValue uid = 13;
};

struct WithStringUid {
    using UserIdValue = std::string;
    static constexpr auto uid = "foo";
};

template <class TraitsT>
struct CreateUserPerformerTest : public Test {
    using Traits = TraitsT;
    using UserIdValue = typename Traits::UserIdValue;
    using UserId = BasicUserId<UserIdValue>;
    using CreateUserPerformer = handlers::CreateUserPerformer<MockedPeersAdaptorWrapper<UserIdValue>, UserIdValue>;

    const UserId uid {Traits::uid};
    const Shard::Id shardId = 42;
    const Shard::Id realShardId = 146;
    const Shard::Database::Address shardMaster {"host", 5432, "dbname", "dataCenter"};
    const std::string metaMaster = "metaMaster";
    const CreateUserParams<UserIdValue> createUserParams {uid, shardId};

    boost::shared_ptr<MockStream> stream;
    ConfigPtr config;
    cache::CachePtr cache;
    std::shared_ptr<const MockedMetaAdaptor<UserIdValue>> metaAdaptor;
    MockedPeersAdaptorWrapper<UserIdValue> peersAdaptor;
    ymod_webserver::request_ptr request;
    std::unique_ptr<RequestContext> context;
    std::shared_ptr<CreateUserPerformer> performer;
    RegData regData;

    void SetUp() override {
        using cache::Cache;
        using cache::RoleCache;
        using cache::StateCache;
        using Role = Shard::Database::Role;
        using State = Shard::Database::State;

        stream = boost::make_shared<MockStream>();
        config = makeTestConfig();
        cache = std::make_shared<Cache>(2, 1);
        metaAdaptor = std::make_shared<const MockedMetaAdaptor<UserIdValue>>();
        peersAdaptor = MockedPeersAdaptorWrapper<UserIdValue>();

        request = boost::make_shared<ymod_webserver::request>();
        request->method = ymod_webserver::methods::mth_post;
        request->context = boost::make_shared<ymod_webserver::context>();
        request->url.params.insert({"uid", sharpei::to_string(uid)});
        context = std::make_unique<RequestContext>(request, stream, "");

        regData.weightedShardIds.emplace_back(shardId, 1);

        for (auto shardId : {this->shardId, realShardId}) {
            cache->shardName.update(shardId, "shard");
            cache->role.update(shardId, {{shardMaster, RoleCache::OptRole(Role::Master)}});
            cache->status.alive(shardId, shardMaster);
            cache->state.update(shardId, {{shardMaster, StateCache::OptState(State {0})}});
        }

        performer = std::make_shared<CreateUserPerformer>(*context, config, cache, metaAdaptor, peersAdaptor);
    }
};

using CreateUserPerformerTestTypes = Types<WithInt64Uid, WithStringUid>;

TYPED_TEST_SUITE(CreateUserPerformerTest, CreateUserPerformerTestTypes);

TYPED_TEST(CreateUserPerformerTest, perform_without_request_parameters_should_response_bad_request) {
    const std::string resultBody = R"json({"result":"invalid request","description":"uid parameter not found"})json";

    this->request->url.params.clear();

    InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

using CreateUserPerformerTestWithInt64Uid = CreateUserPerformerTest<WithInt64Uid>;

TEST_F(CreateUserPerformerTestWithInt64Uid, perform_with_bad_uid_parameter_value_should_response_bad_request) {
    const std::string resultBody = R"json({"result":"invalid request","description":"invalid uid parameter value"})json";

    request->url.params.find("uid")->second = "bad_uid";

    InSequence s;

    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(resultBody)).WillOnce(Return());

    performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_bad_shard_id_parameter_value_should_response_bad_request) {
    const std::string resultBody = R"json({"result":"invalid request","description":"invalid shard_id parameter value"})json";

    this->request->url.params.insert({"shard_id", "bad_shard_id"});

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_error_on_request_for_reg_data_should_response_internal_server_error) {
    const std::string resultBody =
            R"json({"result":"error in request to meta database",)json"
                R"json("description":"error in request to meta database"})json";

    InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _))
        .WillOnce(InvokeArgument<2>(ExplainedError(Error::metaRequestError)));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_internal_error_on_create_user_should_response_internal_server_error) {
    const std::string resultBody = R"json({"result":"internal error","description":"internal error"})json";

    InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _)).WillOnce(InvokeArgument<1>(this->regData));
    EXPECT_CALL(*this->peersAdaptor.impl, createUser(this->metaMaster, this->createUserParams, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::internalError), Shard::Id()));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_no_errors_should_response_ok) {
    const std::string resultBody =
            R"json({"id":42,"name":"shard",)json"
                R"json("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)json"
                R"json("role":"master","status":"alive","state":{"lag":0}}]})json";

    InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _)).WillOnce(InvokeArgument<1>(this->regData));
    EXPECT_CALL(*this->peersAdaptor.impl, createUser(this->metaMaster, this->createUserParams, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::ok), this->shardId));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_already_created_user_in_different_shard_id_should_response_ok) {
    const std::string resultBody =
            R"json({"id":146,"name":"shard",)json"
                R"json("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)json"
                R"json("role":"master","status":"alive","state":{"lag":0}}]})json";

    InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _)).WillOnce(InvokeArgument<1>(this->regData));
    EXPECT_CALL(*this->peersAdaptor.impl, createUser(this->metaMaster, this->createUserParams, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::ok), this->realShardId));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_existing_shard_id_should_response_ok_without_user_creation) {
    const std::string resultBody =
            R"json({"id":42,"name":"shard",)json"
                R"json("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)json"
                R"json("role":"master","status":"alive","state":{"lag":0}}]})json";

    this->regData.userShardId = this->shardId;

    InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _)).WillOnce(InvokeArgument<1>(this->regData));
    EXPECT_CALL(*this->peersAdaptor.impl, createUser(this->metaMaster, this->createUserParams, _)).Times(0);
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(
    CreateUserPerformerTest,
    perform_without_shard_id_and_error_on_request_for_master_should_response_internal_server_error) {
    const std::string resultBody = R"json({"result":"meta master provider error",)json"
                                   R"json("description":"meta master provider error"})json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _))
        .WillOnce(InvokeArgument<1>(ExplainedError(Error::metaMasterProviderError)));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _))
        .WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_set_shard_id_and_error_on_request_for_master_should_response_internal_server_error) {
    const std::string resultBody =
            R"json({"result":"error in request to meta database",)json"
                R"json("description":"error in request to meta database"})json";

    this->request->url.params.insert({"shard_id", std::to_string(this->shardId)});

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::metaRequestError)));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_set_shard_id_should_create_user_in_that_shard_and_response_ok) {
    const std::string resultBody =
            R"json({"id":42,"name":"shard",)json"
                R"json("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)json"
                R"json("role":"master","status":"alive","state":{"lag":0}}]})json";

    this->request->url.params.insert({"shard_id", std::to_string(this->shardId)});

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->peersAdaptor.impl, createUser(this->metaMaster, this->createUserParams, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::ok), this->shardId));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_no_shard_in_cache_should_return_internal_server_error) {
    using cache::Cache;

    const std::string resultBody = R"json({"result":"cached shard name not found","description":"for shard 0"})json";
    const Shard::Id shardId = 0;
    const CreateUserParams<typename TestFixture::UserIdValue> createUserParams {this->uid, shardId};

    this->request->url.params.insert({"shard_id", std::to_string(shardId)});

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->peersAdaptor.impl, createUser(this->metaMaster, createUserParams, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::ok), shardId));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_std_exception_on_request_for_reg_data_should_response_internal_server_error) {
    const std::string resultBody = R"json({"result":"internal error","description":"some error"})json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _)).WillOnce(Invoke([](auto...) {
        throw std::runtime_error("some error");
    }));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

struct BoostException : public boost::exception {};

TYPED_TEST(CreateUserPerformerTest, perform_with_boost_exception_on_request_for_reg_data_should_response_internal_server_error) {
    const std::string resultBodyPrefix = "{\"result\":\"internal error\",\"description\":\"";
    const std::string resultBodySuffix = "\"}";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _)).WillOnce(Invoke([](auto...) {
        throw BoostException();
    }));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(AllOf(StartsWith(resultBodyPrefix), EndsWith(resultBodySuffix)))).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_exception_on_request_for_reg_data_should_response_internal_server_error) {
    const std::string resultBody = R"json({"result":"internal error","description":"unknown error"})json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _)).WillOnce(Invoke([](auto...) { throw 42; }));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(CreateUserPerformerTest, perform_with_only_dead_master_in_cache_should_response_internal_server_error) {
    const std::string resultBody = R"json({"result":"shard with alive master not found","description":"shard with alive master not found"})json";

    this->cache->status.dead(this->shardId, this->shardMaster);

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->metaMaster));
    EXPECT_CALL(*this->metaAdaptor, getUserRegData(this->uid, _, _)).WillOnce(InvokeArgument<1>(this->regData));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

} // namespace
