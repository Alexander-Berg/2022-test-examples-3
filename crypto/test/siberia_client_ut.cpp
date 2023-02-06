#include <crypta/lib/native/cgiparam/cgiparam.h>
#include <crypta/siberia/bin/common/data/types.h>
#include <crypta/siberia/bin/common/describing/mode/cpp/describing_mode.h>
#include <crypta/siberia/bin/common/proto/describe_ids_response.pb.h>
#include <crypta/siberia/bin/common/siberia_client/cpp/siberia_client.h>

#include <library/cpp/neh/neh.h>
#include <library/cpp/neh/rpc.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/tvmauth/client/facade.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/maybe.h>
#include <util/stream/file.h>

using namespace NCrypta;
using namespace NCrypta::NSiberia;

namespace {
    static const TString HOST = "localhost";
    static const ui64 PORT = 8080;
    static const TString BASE_ADDRESS = TStringBuilder() << "http://" << HOST << ":" << PORT;

    NNeh::IHttpRequest* CastReq2HttpReq(const NNeh::IRequestRef& req) {
        auto* httpReq = dynamic_cast<::NNeh::IHttpRequest*>(req.Get());
        Y_ENSURE(httpReq != nullptr);
        return httpReq;
    }

    class TDescribeIdsServer {
    public:
        TDescribeIdsServer() = default;

        void ServeRequest(const NNeh::IRequestRef& req) {
            auto* httpReq = CastReq2HttpReq(req);
            LastCgiParameters = TCgiParameters(httpReq->Cgi());
            const auto& ids = NProtobufJson::Json2Proto<TIds>(httpReq->Data());

            TDescribeIdsResponse responseProto;
            responseProto.SetUserSetId(ToString(ids.IdsSize()));
            const auto& response = NProtobufJson::Proto2Json(responseProto);

            NNeh::TData data(response.data(), response.data() + response.size());
            httpReq->SendReply(data);
        }

        TMaybe<TCgiParameters> GetLastCgiParameters() const {
            return LastCgiParameters;
        }

    private:
        TMaybe<TCgiParameters> LastCgiParameters;
    };

    class TGetUserSetStatsServer {
    public:
        TGetUserSetStatsServer() = default;

        void ServeRequest(const NNeh::IRequestRef& req) {
            auto* httpReq = CastReq2HttpReq(req);
            const auto& cgiParameters = TCgiParameters(httpReq->Cgi());

            TStats stats;
            stats.MutableInfo()->SetReady(true);
            stats.MutableInfo()->SetProcessedUsersCount(NCgiParam::Get<TUserSetId>(cgiParameters, "user_set_id"));
            stats.MutableUserDataStats();

            const auto& response = NProtobufJson::Proto2Json(stats);
            NNeh::TData data(response.data(), response.data() + response.size());
            httpReq->SendReply(data);
        }
    };

    NTvmAuth::TTvmClient CreateTvmClient() {
        const auto& port = FromString<ui64>(TUnbufferedFileInput("tvmapi.port").ReadAll());

        NTvmAuth::NTvmApi::TClientSettings settings;
        settings.SetTvmHostPort("localhost", port);
        settings.SetSelfTvmId(1000501);
        settings.EnableServiceTicketChecking();
        settings.EnableServiceTicketsFetchOptions("bAicxJVa5uVY7MjDlapthw", {{TSiberiaClient::SIBERIA_TVM_ALIAS, 1000502}});
        return NTvmAuth::TTvmClient(settings, NTvmAuth::TDevNullLogger::IAmBrave());
    }

    TSiberiaClient CreateSiberiaClient(const NTvmAuth::TTvmClient& tvmClient) {
        return TSiberiaClient(HOST, PORT, TDuration::Seconds(5), tvmClient, TRetryOptions());
    }

    NNeh::IServicesRef CreateDescribeIdsService(TDescribeIdsServer& describeIdsServer) {
        auto describeIdsService = NNeh::CreateLoop();
        describeIdsService->Add(BASE_ADDRESS + "/user_sets/describe_ids", describeIdsServer);

        describeIdsService->ForkLoop(1);
        return describeIdsService;
    }

    NNeh::IServicesRef CreateGetUserSetStatsServer(TGetUserSetStatsServer& getUserSetStatsServer) {
        auto getUserSetStatsService = NNeh::CreateLoop();
        getUserSetStatsService->Add(BASE_ADDRESS + "/user_sets/get_stats", getUserSetStatsServer);
        getUserSetStatsService->ForkLoop(1);
        return getUserSetStatsService;
    }
}

Y_UNIT_TEST_SUITE(TSiberiaClient) {
    Y_UNIT_TEST(TestDescribeIds) {
        TDescribeIdsServer server;
        auto service = CreateDescribeIdsService(server);
        const auto& tvmClient = CreateTvmClient();
        auto client = CreateSiberiaClient(tvmClient);

        TIds ids;
        auto* id = ids.AddIds();
        id->SetType("yandexuid");
        id->SetValue("11111111");

        auto* id2 = ids.AddIds();
        id2->SetType("yandexuid");
        id2->SetValue("22222222");

        UNIT_ASSERT_EQUAL(2, client.DescribeIds(ids));
        UNIT_ASSERT_EQUAL(Nothing(), NCgiParam::GetOptional<TString>(*server.GetLastCgiParameters(), TSiberiaClient::DESCRIBING_MODE_CGI_PARAM));

        UNIT_ASSERT_EQUAL(2, client.DescribeIds(ids, DESCRIBING_MODE.GetFast()));
        UNIT_ASSERT(IsFastDescribingMode(NCgiParam::Get<TString>(*server.GetLastCgiParameters(), TSiberiaClient::DESCRIBING_MODE_CGI_PARAM)));

        UNIT_ASSERT_EQUAL(2, client.DescribeIds(ids, DESCRIBING_MODE.GetSlow()));
        UNIT_ASSERT(IsSlowDescribingMode(NCgiParam::Get<TString>(*server.GetLastCgiParameters(), TSiberiaClient::DESCRIBING_MODE_CGI_PARAM)));
    }

    Y_UNIT_TEST(TestGetUserSetStats) {
        TGetUserSetStatsServer server;
        auto service = CreateGetUserSetStatsServer(server);
        const auto& tvmClient = CreateTvmClient();
        auto client = CreateSiberiaClient(tvmClient);

        const TUserSetId userSetId = 10;
        const auto& stats = client.GetUserSetStats(userSetId);
        UNIT_ASSERT_EQUAL(userSetId, stats.GetInfo().GetProcessedUsersCount());
    }
}
