#include <crypta/lib/native/neh_sync_multi_client/neh_sync_multi_client.h>

#include <library/cpp/neh/neh.h>
#include <library/cpp/neh/rpc.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <yt/yt/core/actions/invoker.h>
#include <yt/yt/core/concurrency/count_down_latch.h>
#include <yt/yt/core/concurrency/thread_pool.h>
#include <yt/yt/core/misc/shutdown.h>

using namespace NCrypta;

namespace {
    class TServer {
    public:
        TServer() = default;

        void ServeRequest(const NNeh::IRequestRef& req) {
            const auto& response = TString("echo: ") + req->Data();
            NNeh::TData data(response.data(), response.data() + response.size());
            req->SendReply(data);
        }
    };

    constexpr const char * const ADDRESS = "http://localhost:8080/test";
    constexpr const char * const REQUEST_DATA = "ping";
    constexpr const char * const REF_RESPONSE_DATA = "echo: ping";

    NNeh::IServicesRef CreateService(TServer& server) {
        auto service = NNeh::CreateLoop();
        service->Add(ADDRESS, server);
        service->ForkLoop(1);

        return service;
    }

    void TestRequest(TNehSyncMultiClient& client) {
        NNeh::TMessage msg(ADDRESS, REQUEST_DATA);

        auto response = client.Request(msg, TDuration::Seconds(5)).ValueOrThrow();

        EXPECT_FALSE(response->IsError());
        EXPECT_EQ(REF_RESPONSE_DATA, response->Data);
    }
}

TEST(TNehSyncMultiClient, RequestFromMainThread) {
    TServer server;
    auto service = CreateService(server);

    TNehSyncMultiClient client("ClientDispatcher");
    TestRequest(client);
}

TEST(TNehSyncMultiClient, RequestFromThreadPool) {
    TServer server;
    auto service = CreateService(server);

    TNehSyncMultiClient client("ClientDispatcher");

    const int requests = 10;
    auto threadPool = NYT::New<NYT::NConcurrency::TThreadPool>(3, "Requester");
    NYT::NConcurrency::TCountDownLatch latch(requests);

    for (int i = 0; i < requests; ++i) {
        threadPool->GetInvoker()->Invoke(BIND([&client, &latch](){
            TestRequest(client);
            latch.CountDown();
        }));
    }

    latch.Wait();
    threadPool.Reset();
    NYT::Shutdown();
}
