#include "nsls_test.h"
#include "fakes/context.h"
#include "fakes/http_stream.h"
#include <mail/notsolitesrv/src/server.h>
#include <mail/notsolitesrv/src/http/handlers/store.h>
#include <mail/notsolitesrv/src/http/types/request.h>
#include <mail/notsolitesrv/src/util/zerocopy.h>

#include <functional>
#include <memory>
#include <optional>
#include <string>

using namespace testing;
using namespace NNotSoLiteSrv;

class TFakeServer: public IServer {
public:
    using TFillUsers = std::function<void(NUser::TStoragePtr)>;
    TFakeServer() = default;
    TFakeServer(TFillUsers fillUsers): FillUsers(fillUsers) {}

    void SetFillUsers(TFillUsers fillUsers) { FillUsers = fillUsers; }

    void init(const yplatform::ptree& cfg) override { Y_UNUSED(cfg); }
    void reload(const yplatform::ptree& cfg) override { Y_UNUSED(cfg); }
    TConfigPtr GetConfig() const override { return ::GetConfig({}); }
    NTskv::TUserJournalPtr GetUserJournalWriter() const override { return NTskv::TUserJournalPtr{}; }
    void Deliver(
        TContextPtr,
        std::shared_ptr<std::string>,
        const TEnvelope&,
        NUser::TStoragePtr,
        const NTimeTraits::TSystemTimePoint&,
        NNewEmails::TProcessor::TAsyncSender,
        TDeliverer::TCallback) override
    {
    }

    void Deliver(
        TContextPtr,
        const std::string&,
        const TEnvelope&,
        NUser::TStoragePtr userStorage,
        const NTimeTraits::TSystemTimePoint&,
        TDeliverer::TMessageProcessor,
        NNewEmails::TProcessor::TAsyncSender,
        TDeliverer::TCallback cb) override
    {
        if (FillUsers) {
            (*FillUsers)(userStorage);
        }
        cb(EError::Ok, TMessagePtr());
    }

private:
    std::optional<TFillUsers> FillUsers;
};

class THttpHandlersTest: public TNslsTest {
protected:
    void SetUp() override {
        Stream = boost::make_shared<TFakeHttpStream>();
        Handler = std::make_shared<NHttp::TStore>();
        Ctx = GetContext();
    }

    void Run() {
        return Handler->Run(Ctx, Stream);
    }

    std::string MakeStoreRequestBody() const {
        return R"({
            "envelope": {
                "remote_ip": "remote_ip",
                "remote_host": "remote_host",
                "mail_from": {"email": "email"},
                "envelope_id": "envelope_id",
                "helo": "helo"
            },
            "message": {
                "stid": "stid",
                "timemark": 3,
                "hints": [{"key0" : ["value0", "value1"]}]
            },
            "recipients": [{
                "email": "a@a.ru",
                "is_local": "yes",
                "notify": {
                    "success": false,
                    "failure": false,
                    "delay": false
                }
            }]
        })";
    }

protected:
    boost::shared_ptr<TFakeHttpStream> Stream;
private:
    NHttp::THandlerPtr Handler;
    TContextPtr Ctx;
};

TEST_F(THttpHandlersTest, InvalidJsonIsBadRequest) {
    Stream->Request->raw_body = NUtil::MakeZerocopySegment("{\"NotAJson");
    Run();
    EXPECT_EQ(Stream->Code, NHttp::ECode::bad_request);
    EXPECT_FALSE(Stream->Body.empty());
}

TEST_F(THttpHandlersTest, ValidJsonButInvalidStoreRequestIsBadRequest) {
    Stream->Request->raw_body = NUtil::MakeZerocopySegment(R"({"some":"value"})");
    Run();
    EXPECT_EQ(Stream->Code, NHttp::ECode::bad_request);
    EXPECT_FALSE(Stream->Body.empty());
}

TEST_F(THttpHandlersTest, StoreImplemented) {
    yplatform::global_factory::add_factory<TFakeServer>("TFakeServer");
    yplatform::configuration::module_data cfg;
    cfg.name = "nsls";
    cfg.factory = "TFakeServer";
    cfg.options.add_child("options", yplatform::ptree{});
    auto reactorSet = boost::make_shared<yplatform::reactor_set>();
    reactorSet->add("global", {1, 1});
    auto moduleWrapper = yplatform::global_factory::create(reactorSet, cfg);
    yplatform::repository::instance().add_service<TFakeServer>("nsls", moduleWrapper->module);
    auto srv = yplatform::find<TFakeServer>("nsls");
    srv->SetFillUsers([](NUser::TStoragePtr us) {
        auto u = us->GetUserByEmail("a@a.ru");
        u->Status = NUser::ELoadStatus::Loaded;
    });

    Stream->Request->raw_body = NUtil::MakeZerocopySegment(MakeStoreRequestBody());
    Run();
    EXPECT_EQ(Stream->Code, NHttp::ECode::ok);
    EXPECT_FALSE(Stream->Body.empty());
}

TEST_F(THttpHandlersTest, DryRunOk) {
    Stream->Request->raw_body = NUtil::MakeZerocopySegment(MakeStoreRequestBody());
    Stream->Request->url.params.emplace("dry_run", "1");
    Run();
    EXPECT_EQ(Stream->Code, NHttp::ECode::ok);
    EXPECT_FALSE(Stream->Body.empty());
}
