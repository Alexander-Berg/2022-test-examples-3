#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yplatform/zerocopy/streambuf.h>

#include <internal/server/handlers/detail/update_user_performer.h>

#include "../../mocks.h"
#include "mocks.h"

namespace sharpei {
namespace db {

template <class T>
bool operator ==(const UpdateUserParams<T>& lhs, const UpdateUserParams<T>& rhs) {
    return lhs.uid == rhs.uid && lhs.data == rhs.data;
}

} // namespace db
} // namespace sharpei

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::db;
using namespace sharpei::server;

yplatform::zerocopy::segment makeSegment(const std::string& value) {
    yplatform::zerocopy::streambuf buffer(512, 512);
    std::ostream stream(&buffer);
    stream << value << std::flush;
    return buffer.detach(buffer.end());
}

struct WithInt64Uid {
    using UserIdValue = std::int64_t;
    static constexpr UserIdValue uid = 13;
};

struct WithStringUid {
    using UserIdValue = std::string;
    static constexpr auto uid = "foo";
};

template <class TraitsT>
struct UpdateUserPerformerTest : public Test {
    using Traits = TraitsT;
    using UserIdValue = typename Traits::UserIdValue;
    using UserId = BasicUserId<UserIdValue>;
    using UpdateUserPerformer = handlers::UpdateUserPerformer<MockedPeersAdaptorWrapper<UserIdValue>, UserIdValue>;

    const UserId uid {Traits::uid};
    const boost::optional<std::string> data = std::string("{}");
    const std::string master = "master";
    const UpdateUserParams<UserIdValue> updateUserParams {uid, boost::none, boost::none, data};

    boost::shared_ptr<MockStream> stream;
    ConfigPtr config;
    std::shared_ptr<const MockedMetaAdaptor<UserIdValue>> metaAdaptor;
    MockedPeersAdaptorWrapper<UserIdValue> peersAdaptor;
    ymod_webserver::request_ptr request;
    std::unique_ptr<RequestContext> context;
    std::shared_ptr<UpdateUserPerformer> performer;

    void SetUp() override {
        stream = boost::make_shared<MockStream>();
        config = makeTestConfig();
        metaAdaptor = std::make_shared<const MockedMetaAdaptor<UserIdValue>>();
        peersAdaptor = MockedPeersAdaptorWrapper<UserIdValue>();

        request = boost::make_shared<ymod_webserver::request>();
        request->method = ymod_webserver::methods::mth_post;
        request->context = boost::make_shared<ymod_webserver::context>();
        request->content.type = "application";
        request->content.subtype = "json";
        request->url.params.insert({"uid", sharpei::to_string(uid)});
        request->raw_body = makeSegment(data.get());

        context = std::make_unique<RequestContext>(request, stream, "");

        performer = std::make_shared<UpdateUserPerformer>(*context, config, metaAdaptor, peersAdaptor);
    }
};

using UpdateUserPerformerTestTypes = Types<WithInt64Uid, WithStringUid>;

TYPED_TEST_SUITE(UpdateUserPerformerTest, UpdateUserPerformerTestTypes);

TYPED_TEST(UpdateUserPerformerTest, perform_without_request_parameters_should_response_bad_request) {
    const std::string resultBody = R"json({"result":"invalid request","description":"uid parameter not found"})json";

    this->request->url.params.clear();

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_new_shard_id_but_without_shard_id_parameter_should_response_bad_request) {
    const std::string resultBody =
            R"json({"result":"invalid request",)json"
                R"json("description":"shard_id parameter not found but new_shard_id is present"})json";

    this->request->url.params.insert({"new_shard_id", ""});

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

using UpdateUserPerformerTestWithInt64Uid = UpdateUserPerformerTest<WithInt64Uid>;

TEST_F(UpdateUserPerformerTestWithInt64Uid, perform_with_bad_uid_parameter_value_should_response_bad_request) {
    const std::string resultBody =
            R"json({"result":"invalid request",)json"
                R"json("description":"invalid uid parameter value"})json";

    this->request->url.params.find("uid")->second = "bad_uid";

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_not_empty_body_and_not_application_json_content_type_should_response_bad_request) {
    const std::string resultBody = R"json({"result":"invalid request","description":"unsupported content type"})json";

    this->request->content.type = "not application";
    this->request->content.subtype = "not json";

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_not_empty_body_and_application_but_not_json_content_type_should_response_bad_request) {
    const std::string resultBody = R"json({"result":"invalid request","description":"unsupported content type"})json";

    this->request->content.type = "application";
    this->request->content.subtype = "not json";

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_not_empty_body_and_not_application_but_json_content_type_should_response_bad_request) {
    const std::string resultBody = R"json({"result":"invalid request","description":"unsupported content type"})json";

    this->request->content.type = "not application";
    this->request->content.subtype = "json";

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_bad_shard_id_parameter_value_should_response_bad_request) {
    const std::string resultBody =
            R"json({"result":"invalid request",)json"
                R"json("description":"invalid shard_id parameter value"})json";

    this->request->url.params.insert({"shard_id", "bad_shard_id"});

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_bad_new_shard_id_parameter_value_should_response_bad_request) {
    const std::string resultBody =
            R"json({"result":"invalid request",)json"
                R"json("description":"invalid new_shard_id parameter value"})json";

    this->request->url.params.insert({"shard_id", "42"});
    this->request->url.params.insert({"new_shard_id", "bad_shard_id"});

    const InSequence s;

    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::bad_request, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_internal_error_on_request_for_master_should_response_internal_server_error) {
    const std::string resultBody =
            R"json({"result":"error in request to meta database",)json"
                R"json("description":"error in request to meta database"})json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::metaRequestError)));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_uid_not_found_error_on_update_user_should_response_not_found) {
    const std::string resultBody = R"json({"result":"uid not found","description":"uid not found"})json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->master));
    EXPECT_CALL(*this->peersAdaptor.impl, updateUser(this->master, this->updateUserParams, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::not_found, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_internal_error_on_update_user_should_response_internal_server_error) {
    const std::string resultBody = R"json({"result":"internal error","description":"internal error"})json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->master));
    EXPECT_CALL(*this->peersAdaptor.impl, updateUser(this->master, this->updateUserParams, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::internalError)));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_no_errors_should_response_ok) {
    const std::string resultBody = R"json("done")json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->master));
    EXPECT_CALL(*this->peersAdaptor.impl, updateUser(this->master, this->updateUserParams, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::ok)));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::ok, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_empty_body_should_call_update_user_with_none_data) {
    using Params = UpdateUserParams<typename TestFixture::UserIdValue>;

    this->request->content.type = "";
    this->request->content.subtype = "";
    this->request->raw_body = makeSegment("");

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->master));
    EXPECT_CALL(*this->peersAdaptor.impl, updateUser(this->master, Params {this->uid, boost::none, boost::none, boost::none}, _)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_shard_id_should_call_update_user_with_not_none_shard_id) {
    using Params = UpdateUserParams<typename TestFixture::UserIdValue>;

    const InSequence s;

    const Shard::Id shardId = 42;

    this->request->url.params.insert({"shard_id", std::to_string(shardId)});

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->master));
    EXPECT_CALL(*this->peersAdaptor.impl, updateUser(this->master, Params {this->uid, shardId, boost::none, this->data}, _)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_new_shard_id_should_call_update_user_with_not_none_new_shard_id) {
    using Params = UpdateUserParams<typename TestFixture::UserIdValue>;

    const InSequence s;

    const Shard::Id shardId = 42;
    const Shard::Id newShardId = 127;

    this->request->url.params.insert({"shard_id", std::to_string(shardId)});
    this->request->url.params.insert({"new_shard_id", std::to_string(newShardId)});

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(this->master));
    EXPECT_CALL(*this->peersAdaptor.impl, updateUser(this->master, Params {this->uid, shardId, newShardId, this->data}, _)).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_std_exception_on_request_for_master_should_response_internal_server_error) {
    const std::string resultBody = R"json({"result":"internal error","description":"some error"})json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(Invoke([] (auto ...) { throw std::runtime_error("some error"); }));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

struct BoostException : public boost::exception {};

TYPED_TEST(UpdateUserPerformerTest, perform_with_boost_exception_on_request_for_master_should_response_internal_server_error) {
    const std::string resultBodyPrefix = "{\"result\":\"internal error\",\"description\":\"";
    const std::string resultBodySuffix = "\"}";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(Invoke([] (auto ...) { throw BoostException(); }));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(AllOf(StartsWith(resultBodyPrefix), EndsWith(resultBodySuffix)))).WillOnce(Return());

    this->performer->perform();
}

TYPED_TEST(UpdateUserPerformerTest, perform_with_excetion_on_request_for_master_should_response_internal_server_error) {
    const std::string resultBody = R"json({"result":"internal error","description":"unknown error"})json";

    const InSequence s;

    EXPECT_CALL(*this->metaAdaptor, getMaster(_, _)).WillOnce(Invoke([] (auto ...) { throw 42; }));
    EXPECT_CALL(*this->stream, set_code(ymod_webserver::codes::internal_server_error, _)).WillOnce(Return());
    EXPECT_CALL(*this->stream, set_content_type("application", "json")).WillOnce(Return());
    EXPECT_CALL(*this->stream, result_body(resultBody)).WillOnce(Return());

    this->performer->perform();
}

} // namespace
