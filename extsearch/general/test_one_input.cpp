#include <search/fuzzing/lib/common/common.h>
#include <search/fuzzing/lib/common/fuzz_env.h>
#include <search/pseudo_server/env.h>
#include <search/pseudo_server/arg_builder.h>

#include <util/system/env.h>
#include <util/string/builder.h>

#include <extsearch/video/base/videosearch/search_main/search_main.h>

class TVideoBaseSearchEnvironment : public NSearch::NPseudoServer::TSearchEnvironment {
    class TVideoBaseSearchRunner : public NSearch::NPseudoServer::ISearchRunner {
    public:
        TString SearchName() const override {
            return "video_basesearch_fuzzer";
        }
        ui16 SearchPort() const override {
            return 17171;
        }
        int SearchMain() override {
            NSearch::NPseudoServer::TArgBuilder args;
            args.Add(SearchName(), "-d", GetEnv("VIDEO_BASESEARCH_FUZZER_CONFIG", "./video_basesearch.cfg"), "-p", SearchPort());
            return NVideoSearch::NBase::SearchMain(args.Argc(), args.Argv());
        }
    };

public:
    bool InitEnv(bool muteSearchLog = false) {
        return NSearch::NPseudoServer::TSearchEnvironment::InitEnv(new TVideoBaseSearchRunner(), muteSearchLog);
    }
};

using TVideoBaseSearchFuzzingEnv = NSearch::NFuzzing::NCommon::TFuzzEnv<TVideoBaseSearchEnvironment>;

extern "C" {
    int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
        static bool initialized = TVideoBaseSearchFuzzingEnv::DoInit(true);
        if (initialized) {
            return TVideoBaseSearchFuzzingEnv::TestOneInput(TStringBuilder() << "/yandsearch?" << TStringBuf((const char*)data, size));
        }
        return 0;
    }
}
