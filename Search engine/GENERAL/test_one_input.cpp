#include <search/fuzzing/lib/common/common.h>
#include <search/fuzzing/lib/common/fuzz_env.h>
#include <search/pseudo_server/env.h>
#include <search/pseudo_server/arg_builder.h>

#include <util/system/env.h>
#include <util/string/builder.h>

#include <search/daemons/basesearch/search_main/search_main.h>

class TBaseSearchEnvironment : public NSearch::NPseudoServer::TSearchEnvironment {
    class TBaseSearchRunner : public NSearch::NPseudoServer::ISearchRunner {
    public:
        TString SearchName() const override {
            return "basesearch_fuzzer";
        }
        ui16 SearchPort() const override {
            return 17171;
        }
        int SearchMain() override {
            NSearch::NPseudoServer::TArgBuilder args;
            args.Add(SearchName(), "-d", GetEnv("BASESEARCH_FUZZER_CONFIG", "./basesearch.cfg"), "-p", SearchPort());
            return NSearch::NBase::SearchMain(args.Argc(), args.Argv());
        }
    };

public:
    bool InitEnv(bool muteSearchLog = false) {
        return NSearch::NPseudoServer::TSearchEnvironment::InitEnv(new TBaseSearchRunner(), muteSearchLog);
    }
};

using TBaseSearchFuzzingEnv = NSearch::NFuzzing::NCommon::TFuzzEnv<TBaseSearchEnvironment>;

extern "C" {
    int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
        static bool initialized = TBaseSearchFuzzingEnv::DoInit(true);
        if (initialized) {
            return TBaseSearchFuzzingEnv::TestOneInput(TStringBuilder() << "/yandsearch?" << TStringBuf((const char*)data, size));
        }
        return 0;
    }
}
