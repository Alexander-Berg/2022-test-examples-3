#include <crypta/lib/native/grpc/logging/logging_interceptor_factory.h>
#include <crypta/lib/native/grpc/test_helpers/test_server_fixture.h>
#include <crypta/lib/native/log/loggers/std_logger.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <util/stream/file.h>
#include <util/string/cast.h>

#include <contrib/libs/grpc/src/proto/grpc/reflection/v1alpha/reflection.grpc.pb.h>

using namespace NCrypta::NGrpc;

namespace {
    NCrypta::NLog::TLogPtr GetLog() {
        static auto log = NCrypta::NLog::NStdLogger::RegisterLog("main", "stderr", "info");
        return log;
    }

    class LoggingTestServerFixture : public TestServerFixture {
    protected:
        LoggingTestServerFixture() {
            Request.SetMessage("ping");
        }

        void Build(grpc::ServerBuilder& builder) override {
            std::vector<std::unique_ptr<grpc::experimental::ServerInterceptorFactoryInterface>> creators;

            creators.push_back(std::make_unique<TLoggingInterceptorFactory>(
                [&](TString&& line){ LogLine = std::move(line); },
                GetLog()
            ));

            builder.experimental().SetInterceptorCreators(std::move(creators));
        }

        void CheckLog(const TStringBuf& expected) {
            Server->Shutdown();
            Server->Wait();

            NJson::TJsonValue actual = NJson::ReadJsonFastTree(LogLine);

            actual.EraseValue("peer");
            actual.EraseValue("request_metadata");
            const auto& actualFormatted = NJson::WriteJson(actual, true, true);
            const auto& expectedFormatted = NJson::WriteJson(NJson::ReadJsonFastTree(expected), true, true);
            EXPECT_EQ(expectedFormatted, actualFormatted);
        }

        TRequest Request;
        TResponse Response;
        grpc::ClientContext Context;
        TString LogLine;
    };
}

TEST_F(LoggingTestServerFixture, Echo) {
    Client->Echo(&Context, Request, &Response);

    CheckLog(R"JSON({
        "service":"NCrypta.NGrpc.TTestService",
        "handle":"Echo",
        "request":{"message":"ping"},
        "response_metadata": [],
        "response":{"message":"Got ping"},
        "status": {
            "code":"OK",
            "message":""
        },
        "send_status":true
    })JSON");
}
TEST_F(LoggingTestServerFixture, NotFound) {
    Client->NotFound(&Context, Request, &Response);

    CheckLog(R"JSON({
        "service":"NCrypta.NGrpc.TTestService",
        "handle":"NotFound",
        "request":{"message":"ping"},
        "response_metadata": [],
        "status": {
            "code":"NOT_FOUND",
            "message":"not found"
        }
    })JSON");
}

TEST_F(LoggingTestServerFixture, Fail) {
    Client->Fail(&Context, Request, &Response);

    CheckLog(R"JSON({
        "service":"NCrypta.NGrpc.TTestService",
        "handle":"Fail",
        "request":{"message":"ping"},
        "response_metadata": [],
        "status": {
            "code":"UNKNOWN",
            "message":"Unexpected error in RPC handling"
        }
    })JSON");
}

TEST_F(LoggingTestServerFixture, Timeout) {
    Context.set_deadline(std::chrono::system_clock::now() + std::chrono::seconds(1));
    Client->Timeout(&Context, Request, &Response);

    CheckLog(R"JSON({
        "service":"NCrypta.NGrpc.TTestService",
        "handle":"Timeout",
        "request":{"message":"ping"},
        "response_metadata": [],
        "response":{"message":"Got ping"},
        "status": {
            "code":"OK",
            "message":""
        },
        "send_status":false
    })JSON");
}

TEST_F(LoggingTestServerFixture, Reflection) {
    using namespace grpc::reflection::v1alpha;

    auto client = ServerReflection::NewStub(Channel);

    auto stream = client->ServerReflectionInfo(&Context);

    ServerReflectionRequest request;
    request.Setfile_containing_symbol("NCrypta.NGrpc.TTestService");
    stream->Write(request);
    stream->WritesDone();

    ServerReflectionResponse response;
    while (stream->Read(&response)) {}
    stream->Finish();

    CheckLog("");
}
