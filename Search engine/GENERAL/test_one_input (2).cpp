#include <search/fuzzing/lib/common/common.h>
#include <search/fuzzing/lib/common/app_host.h>
#include <search/fuzzing/lib/common/fuzz_env.h>
#include <search/pseudo_server/env.h>
#include <search/pseudo_server/arg_builder.h>
#include <search/begemot/core/rulefactory.h>
#include <search/begemot/server/server.h>

#include <apphost/lib/service_testing/service_testing.h>

#include <library/cpp/string_utils/base64/base64.h>

#include <util/generic/yexception.h>
#include <util/string/builder.h>
#include <util/string/strip.h>
#include <util/system/env.h>

class TBegemotEnvironment : public NSearch::NPseudoServer::TSearchEnvironment {
    class TBegemotRunner : public NSearch::NPseudoServer::ISearchRunner {
    public:
        TString SearchName() const override {
            return "begemot_fuzzer";
        }
        ui16 SearchPort() const override {
            return 31334;
        }
        int SearchMain() override {
            NSearch::NPseudoServer::TArgBuilder args;
            args.Add(SearchName(),
                "-p", SearchPort() - 1,
                "--port2", SearchPort(),
                "-l", "./eventlog",
                "--data", "./data");
            return NBg::StartDaemon(NBg::CreateConfig(args.Argc(), args.Argv()), NBg::DefaultRuleFactory()) ? 0 : 1;
        }
        ui16 AdminPort() const override {
            return SearchPort() - 1;
        }
        NSearch::NPseudoServer::ISearchRunner::TRequest PingRequest() const override {
            return {AdminPort(), "admin?action=ping"};
        }
        NSearch::NPseudoServer::ISearchRunner::TRequest ShutdownRequest() const override {
            return {AdminPort(), "admin?action=shutdown"};
        }
    };

public:
    bool InitEnv(bool muteSearchLog = false) {
        return NSearch::NPseudoServer::TSearchEnvironment::InitEnv(new TBegemotRunner(), muteSearchLog);
    }
};

using TBegemotFuzzingEnv = NSearch::NFuzzing::NCommon::TFuzzEnv<TBegemotEnvironment>;

namespace {
    class TStatCounter {
    public:
        void AddError() noexcept {
            AtomicIncrement(Errors_);
            AddTotal();
        }
        void AddSuccess() noexcept {
            AtomicIncrement(Success_);
            AddTotal();
        }

    private:
        void AddTotal() noexcept {
            if ((AtomicIncrement(Count_) & 0x7F) == 0) {
                DumpStat();
            }
        }
        void DumpStat() noexcept {
            try {
                TAtomicBase total = AtomicGet(Count_);
                TAtomicBase errors = AtomicGet(Errors_);
                TAtomicBase success = AtomicGet(Success_);

                Cerr << "Fuzzing stat: total=" << total << " errors=" << errors << " success=" << success << Endl;
            } catch (...) {
            }
        }
    private:
        TAtomic Count_ = 0;
        TAtomic Errors_ = 0;
        TAtomic Success_ = 0;
    };

    class TStatGuard {
    public:
        ~TStatGuard() noexcept {
            if (UncaughtException()) {
                Counter().AddError();
            } else {
                Counter().AddSuccess();
            }
        }
    private:
        TStatCounter& Counter() {
            return *Singleton<TStatCounter>();
        }
    };
}

extern "C" {
    int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
        static bool initialized = TBegemotFuzzingEnv::DoInit(true);
        if (initialized && size) {
            try {
                TStatGuard statGuard;
                TString jsonStr = TString{StripString(TStringBuf((const char*)data, size))};
                TMemoryInput in(jsonStr.data(), jsonStr.size());
                NJson::TJsonValue json;
                NJson::ReadJsonTree(&in, &json);
                return TBegemotFuzzingEnv::TestOneInput(TString(""), NFuzzing::NAppHostTools::JsonToProtoBuf(json));
            } catch (...) {
            }
        }
        return 0;
    }
}
