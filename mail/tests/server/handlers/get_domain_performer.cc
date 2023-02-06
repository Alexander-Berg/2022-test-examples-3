#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/server/handlers/detail/get_domain_performer.h>

#include "../../test_with_spawn.h"
#include "../../mocks.h"
#include "mocks.h"

namespace sharpei {
namespace db {

static bool operator ==(const CreateDomainParams& lhs, const CreateDomainParams& rhs) {
    return lhs.domainId == rhs.domainId && lhs.shardId == rhs.shardId;
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

auto makeRequest(DomainId domainId) {
    const auto request = boost::make_shared<ymod_webserver::request>();
    request->method = ymod_webserver::methods::mth_post;
    request->context = boost::make_shared<ymod_webserver::context>();
    request->url.params.emplace("domain_id", std::to_string(domainId));
    return request;
}

struct GetDomainPerformerTest : TestWithSpawn {
    using Performer = GetDomainPerformer<MetaAdaptor, PeersAdaptor>;

    const DomainId domainId {42};
    const ymod_webserver::request_ptr request = makeRequest(domainId);
    const boost::shared_ptr<MockStream> stream {boost::make_shared<MockStream>()};
    const RequestContext context {request, stream, ""};
    const ConfigPtr config = makeTestConfig();
    MetaAdaptorMock metaAdaptorMock;
    PeersAdaptorMock peersAdaptorMock;
    const MetaAdaptor metaAdaptor {&metaAdaptorMock};
    const PeersAdaptor peersAdaptor {&peersAdaptorMock};
    const cache::CachePtr cache = std::make_shared<Cache>(2, 1);
    const std::shared_ptr<const BlackboxMock> blackbox = std::make_shared<StrictMock<const BlackboxMock>>();
    const Shard::Id shardId {13};
    const Shard::Id realShardId {14};
    const Shard::Database::Address shardMaster {"host", 5432, "dbname", "dataCenter"};
    RegData regData;
    std::string masterHost = "masterHost";
    CreateDomainParams params;

    GetDomainPerformerTest() {
        using Role = Shard::Database::Role;
        using State = Shard::Database::State;

        for (const auto v : {shardId, realShardId}) {
            cache->shardName.update(v, "shard");
            cache->role.update(v, {{shardMaster, RoleCache::OptRole(Role::Master)}});
            cache->status.alive(v, shardMaster);
            cache->state.update(v, {{shardMaster, StateCache::OptState(State {0})}});
        }

        regData.weightedShardIds.emplace_back(shardId, 1);

        params.domainId = domainId;
        params.shardId = shardId;
    }
};

TEST_F(GetDomainPerformerTest, on_empty_request_params_should_response_bad_request) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        request->url.params.clear();

        const InSequence s;

        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::bad_request, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"invalid request","description":"domain_id parameter not found"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetDomainPerformerTest, on_invalid_domain_id_should_response_bad_request) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        request->url.params.erase("domain_id");
        request->url.params.emplace("domain_id", "foo");

        const InSequence s;

        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::bad_request, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"invalid request","description":"invalid domain_id parameter value"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetDomainPerformerTest, on_get_domain_shard_id_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
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

TEST_F(GetDomainPerformerTest, should_response_shard_info) {
    const std::string resultBody =
        R"({"id":13,"name":"shard",)"
            R"("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)"
            R"("role":"master","status":"alive","state":{"lag":0}}]})";

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
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

TEST_F(GetDomainPerformerTest, on_no_shard_in_cache_should_response_internal_server_error) {
    cache->shardName.erase(shardId);

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
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

TEST_F(GetDomainPerformerTest, on_domain_not_found_should_check_blackbox_and_create_domain) {
    const std::string resultBody =
        R"({"id":13,"name":"shard",)"
            R"("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)"
            R"("role":"master","status":"alive","state":{"lag":0}}]})";

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::domainNotFound), Shard::Id {}));
        EXPECT_CALL(*blackbox, getHostedDomains(domainId, _))
            .WillOnce(Return(std::vector<HostedDomain> {{domainId}}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(), masterHost));
        EXPECT_CALL(metaAdaptorMock, getRegData(_)).WillOnce(InvokeArgument<0>(ExplainedError(), regData));
        EXPECT_CALL(peersAdaptorMock, createDomain(masterHost, params, _))
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

TEST_F(GetDomainPerformerTest, on_domain_not_found_in_blackbox_should_response_not_found) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::domainNotFound), Shard::Id {}));
        EXPECT_CALL(*blackbox, getHostedDomains(domainId, _))
            .WillOnce(Return(std::vector<HostedDomain>()));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::not_found, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"domain not found","description":"domain not found"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetDomainPerformerTest, on_blackbox_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::domainNotFound), Shard::Id {}));
        EXPECT_CALL(*blackbox, getHostedDomains(domainId, _))
            .WillOnce(Return(make_unexpected(ExplainedError(Error::blackBoxHttpError))));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, _))
            .WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application", "json"))
            .WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"result":"blackbox http error","description":"blackbox http error"})"))
            .WillOnce(Return());

        performer(yield);
    });
}

TEST_F(GetDomainPerformerTest, on_get_master_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&](const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::domainNotFound), Shard::Id{}));
        EXPECT_CALL(*blackbox, getHostedDomains(domainId, _))
            .WillOnce(Return(std::vector<HostedDomain>{{domainId}}));
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

TEST_F(GetDomainPerformerTest, on_get_reg_data_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::domainNotFound), Shard::Id {}));
        EXPECT_CALL(*blackbox, getHostedDomains(domainId, _))
            .WillOnce(Return(std::vector<HostedDomain> {{domainId}}));
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

TEST_F(GetDomainPerformerTest, on_create_domain_error_should_response_internal_server_error) {
    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::domainNotFound), Shard::Id {}));
        EXPECT_CALL(*blackbox, getHostedDomains(domainId, _))
            .WillOnce(Return(std::vector<HostedDomain> {{domainId}}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(), masterHost));
        EXPECT_CALL(metaAdaptorMock, getRegData(_)).WillOnce(InvokeArgument<0>(ExplainedError(), regData));
        EXPECT_CALL(peersAdaptorMock, createDomain(masterHost, params, _))
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

TEST_F(GetDomainPerformerTest, for_dead_master_when_create_domain_should_response_internal_server_error) {
    cache->status.dead(shardId, shardMaster);

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::domainNotFound), Shard::Id {}));
        EXPECT_CALL(*blackbox, getHostedDomains(domainId, _))
            .WillOnce(Return(std::vector<HostedDomain> {{domainId}}));
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

TEST_F(GetDomainPerformerTest, should_response_shard_info_for_real_shard_id) {
    const std::string resultBody =
        R"({"id":14,"name":"shard",)"
            R"("databases":[{"address":{"host":"host","port":5432,"dbname":"dbname","dataCenter":"dataCenter"},)"
            R"("role":"master","status":"alive","state":{"lag":0}}]})";

    const Performer performer(context, config, metaAdaptor, peersAdaptor, cache, blackbox);

    withSpawn([&] (const auto yield) {
        const InSequence s;

        EXPECT_CALL(metaAdaptorMock, getDomainShardId(domainId, _))
            .WillOnce(InvokeArgument<1>(ExplainedError(Error::domainNotFound), Shard::Id {}));
        EXPECT_CALL(*blackbox, getHostedDomains(domainId, _))
            .WillOnce(Return(std::vector<HostedDomain> {{domainId}}));
        EXPECT_CALL(metaAdaptorMock, getMaster(_))
            .WillOnce(InvokeArgument<0>(ExplainedError(), masterHost));
        EXPECT_CALL(metaAdaptorMock, getRegData(_)).WillOnce(InvokeArgument<0>(ExplainedError(), regData));
        EXPECT_CALL(peersAdaptorMock, createDomain(masterHost, params, _))
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
