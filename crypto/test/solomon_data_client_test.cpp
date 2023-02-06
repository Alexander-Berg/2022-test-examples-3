#include <crypta/lib/native/mock_http_server/mock_http_server.h>
#include <crypta/lib/native/solomon/solomon_data_client.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/json/json_reader.h>

#include <util/generic/hash.h>
#include <util/string/cast.h>

using namespace NCrypta;

namespace {
    void GetMockHttpServer(THolder<TMockHttpServer>& result, TMockHttpServer::TCallback callback) {
        result = MakeHolder<TMockHttpServer>();
        result->SetCallback(callback);
        result->Start();
    }

    TMockHttpServer& GetMockSolomonServerWithScalar() {
        static THolder<TMockHttpServer> server;
        if (!server) {
            GetMockHttpServer(server, [](TClientRequest* request){
                EXPECT_EQ("POST /api/v2/projects/yt/sensors/data? HTTP/1.1", request->Input().FirstLine());

                NJson::TJsonValue body;
                NJson::ReadJsonTree(request->Input().ReadAll(), &body, true);
                EXPECT_TRUE(body.Has("program"));
                EXPECT_TRUE(body.Has("from"));
                EXPECT_TRUE(body.Has("to"));

                request->Output() << "HTTP/1.1 200 OK\r\n"
                                  << "Connection: close\r\n"
                                  << "\r\n"
                                  << R"({"scalar":630.90})" << "\r\n"
                                  << "\r\n";
            });
        }

        return *server;
    }
}

TEST(TSolomonDataClient, GetScalarSensor) {
    const auto& server = GetMockSolomonServerWithScalar();
    const TString hostPort = server.GetHost() + ":" + ToString(server.GetPort());
    
    TSolomonDataClient query(
        "http",
        server.GetHost(),
        server.GetPort(),
        "fake-token",
        TDuration::Seconds(1),
        "yt");

    auto value = query.GetScalarSensor(
        THashMap<TString, TString>({
            {"account", "crypta-cm"},
            {"cluster", "seneca-sas"},
            {"service", "accounts"},
            {"sensor", "tablet_static_memory_in_gb"},
        }),
        "avg",
        TDuration::Hours(1)
    );

    EXPECT_EQ(630.9, value);
}
