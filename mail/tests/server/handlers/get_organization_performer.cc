#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/server/handlers/detail/get_organization_performer.h>

#include "../../test_with_spawn.h"
#include "../../mocks.h"
#include "mocks.h"

namespace sharpei {
namespace db {

static bool operator ==(const CreateOrganizationParams& lhs, const CreateOrganizationParams& rhs) {
    return lhs.orgId == rhs.orgId
        && lhs.domainId == rhs.domainId
        && lhs.shardId == rhs.shardId;
}

} // namespace db
} // namespace sharpei

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::cache;
using namespace sharpei::db;
using namespace sharpei::server::handlers;

using sharpei::tests::TestWithSpawn;

auto makeRequest(OrgId orgId) {
    const auto request = boost::make_shared<ymod_webserver::request>();
    request->method = ymod_webserver::methods::mth_post;
    request->context = boost::make_shared<ymod_webserver::context>();
    request->url.params.emplace("org_id", std::to_string(orgId));
    return request;
}

struct GetOrganizationPerformerTest : TestWithSpawn {
    using Performer = GetOrganizationPerformer<MetaAdaptor, PeersAdaptor>;

    const OrgId orgId {146};
    const std::string domainAddress {"ru.ya"};
    const ymod_webserver::request_ptr request = makeRequest(orgId);
    const boost::shared_ptr<MockStream> stream {boost::make_shared<MockStream>()};
    const RequestContext context {request, stream, ""};
    const ConfigPtr config = makeTestConfig();
    MetaAdaptorMock metaAdaptorMock;
    PeersAdaptorMock peersAdaptorMock;
    const MetaAdaptor metaAdaptor {&metaAdaptorMock};
    const PeersAdaptor peersAdaptor {&peersAdaptorMock};
    const cache::CachePtr cache = std::make_shared<Cache>(2, 1);
    const Shard::Id shardId {13};
    const Shard::Id realShardId {14};
    const Shard::Database::Address shardMaster {"host", 5432, "dbname", "dataCenter"};
    RegData regData;
    std::string masterHost = "masterHost";
    CreateOrganizationParams params;

    GetOrganizationPerformerTest() {
        using Role = Shard::Database::Role;
        using State = Shard::Database::State;

        for (const auto v : {shardId, realShardId}) {
            cache->shardName.update(v, "shard");
            cache->role.update(v, {{shardMaster, RoleCache::OptRole(Role::Master)}});
            cache->status.alive(v, shardMaster);
            cache->state.update(v, {{shardMaster, StateCache::OptState(State {0})}});
        }

        regData.weightedShardIds.emplace_back(shardId, 1);

        params.orgId = orgId;
        params.shardId = shardId;
    }
};

TEST_F(GetOrganizationPerformerTest, on_empty_request_params_should_response_bad_request) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        request->url.params.clear();

        const InSequence s;

        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::bad_request, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"invalid request","description":"org_id parameter not found"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, on_invalid_org_id_should_response_bad_request) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        request->url.params.erase("org_id");
        request->url.params.emplace("org_id", "foo");

        const InSequence s;

        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::bad_request, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"invalid request","description":"invalid org_id parameter value"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, on_get_organization_shard_id_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::internalError), Shard::Id {}));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"internal error","description":"internal error"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, should_response_shard_info) {
    const std::string resultBody =
        R"({"id":13,"name":"shard",)"
            R"("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)"
            R"("role":"master","status":"alive","state":{"lag":0}}]})";

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(), shardId));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(resultBody))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, on_no_shard_in_cache_should_response_internal_server_error) {
    cache->shardName.erase(shardId);

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(), shardId));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"cached shard name not found","description":"for shard 13"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, on_organization_not_found_should_create_organization) {
    const std::string resultBody =
        R"({"id":13,"name":"shard",)"
            R"("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)"
            R"("role":"master","status":"alive","state":{"lag":0}}]})";

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::organizationNotFound), Shard::Id {}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(), masterHost));
        EXPECT_CALL(metaAdaptorMock, getRegData(_)).WillOnce(InvokeArgument<0>(ExplainedError(), regData));
        EXPECT_CALL(peersAdaptorMock, createOrganization(masterHost, params, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(), shardId));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(resultBody))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, on_get_reg_data_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::organizationNotFound), Shard::Id {}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(), masterHost));
        EXPECT_CALL(metaAdaptorMock, getRegData(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(Error::internalError), regData));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"internal error","description":"internal error"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, on_get_master_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&](const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::organizationNotFound), Shard::Id{}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(
                InvokeArgument<0>(ExplainedError(Error::metaMasterProviderError), std::string{}));
        EXPECT_CALL(metaAdaptorMock, getRegData(_)).Times(0);
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json")).WillOnce(Return());
        EXPECT_CALL(
            *stream,
            result_body(
                R"({"result":"meta master provider error","description":"meta master provider error"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, on_create_organization_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::organizationNotFound), Shard::Id {}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(), masterHost));
        EXPECT_CALL(metaAdaptorMock, getRegData(_)).WillOnce(InvokeArgument<0>(ExplainedError(), regData));
        EXPECT_CALL(peersAdaptorMock, createOrganization(masterHost, params, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(Error::internalError), Shard::Id{}));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"internal error","description":"internal error"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, for_dead_master_when_create_domain_should_response_internal_server_error) {
    cache->status.dead(shardId, shardMaster);

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::organizationNotFound), Shard::Id {}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(), masterHost));
        EXPECT_CALL(metaAdaptorMock, getRegData(_)).WillOnce(InvokeArgument<0>(ExplainedError(), regData));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"shard with alive master not found","description":"shard with alive master not found"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetOrganizationPerformerTest, should_response_shard_info_for_real_shard_id) {
    const std::string resultBody =
        R"({"id":14,"name":"shard",)"
            R"("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)"
            R"("role":"master","status":"alive","state":{"lag":0}}]})";

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getOrganizationShardId(orgId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::organizationNotFound), Shard::Id {}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(), masterHost));
        EXPECT_CALL(metaAdaptorMock, getRegData(_)).WillOnce(InvokeArgument<0>(ExplainedError(), regData));
        EXPECT_CALL(peersAdaptorMock, createOrganization(masterHost, params, _))
            .WillOnce(InvokeArgument<2>(ExplainedError(), realShardId));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(resultBody))
            .WillOnce(Return());

        performer(yield);
    });
}

} // namespace
