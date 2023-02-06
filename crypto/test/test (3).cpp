#include <crypta/lib/native/grpc/stats/stats_interceptor_factory.h>
#include <crypta/lib/native/grpc/test_helpers/test_server_fixture.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NGrpc;

namespace {
    class StatsTestServerFixture : public TestServerFixture {
    protected:
        void Build(grpc::ServerBuilder& builder) override {
            std::vector<std::unique_ptr<grpc::experimental::ServerInterceptorFactoryInterface>> creators;

            creators.push_back(std::make_unique<TStatsInterceptorFactory>(StatsSettings));

            builder.experimental().SetInterceptorCreators(std::move(creators));
        }

        TRequest Request;
        TResponse Response;

        TStats::TSettings StatsSettings = {
            .HistMin = 0,
            .HistMax = 100,
            .HistBinCount = 100,
            .PercentMax = 10 * 1000 * 1000,
            .PercentPrecision = 3,
            .Percentiles = {100, 50}
        };

        StatsTestServerFixture() {
            Request.SetMessage("ping");
        }
    };
}

constexpr double EPS = 1e-7;

TEST_F(StatsTestServerFixture, CollectStats) {
    const auto requests = 10;

    for (int i = 0; i < requests; ++i) {
        grpc::ClientContext context;
        Client->Echo(&context, Request, &Response);

        grpc::ClientContext context2;
        Client->NotFound(&context2, Request, &Response);

        grpc::ClientContext context3;
        Client->Fail(&context3, Request, &Response);
    }

    grpc::ClientContext context;
    context.set_deadline(std::chrono::system_clock::now() + std::chrono::seconds(1));
    Client->Timeout(&context, Request, &Response);

    Server->Shutdown();
    Server->Wait();

    const auto& snapshot = Singleton<TStatsRegistry>()->GetSnapshot();

    EXPECT_EQ(25u, snapshot.size());

    EXPECT_NEAR(requests, snapshot.at("grpc.Echo.request.total.received"), EPS);
    EXPECT_NEAR(requests, snapshot.at("grpc.Echo.request.total.replied"), EPS);
    EXPECT_NEAR(requests, snapshot.at("grpc.Echo.request.status_code.OK"), EPS);
    EXPECT_NE(snapshot.end(), snapshot.find("grpc.Echo.latency.process.p50"));
    EXPECT_NE(snapshot.end(), snapshot.find("grpc.Echo.latency.wall_time.p50"));

    EXPECT_NEAR(requests, snapshot.at("grpc.NotFound.request.total.received"), EPS);
    EXPECT_EQ(snapshot.end(), snapshot.find("grpc.NotFound.request.total.replied"));
    EXPECT_NEAR(requests, snapshot.at("grpc.NotFound.request.status_code.NOT_FOUND"), EPS);
    EXPECT_NE(snapshot.end(), snapshot.find("grpc.NotFound.latency.process.p50"));
    EXPECT_NE(snapshot.end(), snapshot.find("grpc.NotFound.latency.wall_time.p50"));

    EXPECT_NEAR(requests, snapshot.at("grpc.Fail.request.total.received"), EPS);
    EXPECT_EQ(snapshot.end(), snapshot.find("grpc.Fail.request.total.replied"));
    EXPECT_NEAR(requests, snapshot.at("grpc.Fail.request.status_code.UNKNOWN"), EPS);
    EXPECT_NE(snapshot.end(), snapshot.find("grpc.Fail.latency.process.p50"));
    EXPECT_NE(snapshot.end(), snapshot.find("grpc.Fail.latency.wall_time.p50"));

    EXPECT_NEAR(1, snapshot.at("grpc.Timeout.request.total.received"), EPS);
    EXPECT_EQ(snapshot.end(), snapshot.find("grpc.Timeout.request.total.replied"));
    EXPECT_NEAR(1, snapshot.at("grpc.Timeout.request.status_code.OK"), EPS);
    EXPECT_NE(snapshot.end(), snapshot.find("grpc.Timeout.latency.process.p50"));
    EXPECT_NE(snapshot.end(), snapshot.find("grpc.Timeout.latency.wall_time.p50"));
}