#include <search/fuzzing/lib/common/common.h>
#include <search/fuzzing/lib/common/fuzz_env.h>
#include <search/fuzzing/lib/common/app_host.h>
#include <search/pseudo_server/env.h>
#include <search/pseudo_server/arg_builder.h>
#include <search/daemons/ranking_middlesearch/search_main/search_main.h>

#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/json/json_writer.h>

#include <util/system/env.h>
#include <util/string/builder.h>
#include <util/string/strip.h>
#include <util/system/env.h>

class TMetaSearchEnvironment : public NSearch::NPseudoServer::TSearchEnvironment {
    class TMetaSearchRunner : public NSearch::NPseudoServer::ISearchRunner {
    public:
        TString SearchName() const override {
            return "metasearch_fuzzer";
        }
        ui16 SearchPort() const override {
            return 8038;
        }
        int SearchMain() override {
            NSearch::NPseudoServer::TArgBuilder args;
            args.Add(SearchName(),
                "-d", GetEnv("METASEARCH_FUZZER_CONFIG", "./metasearch.cfg"),
                "-p", AdminPort());
            return NSearch::NRankingMiddle::SearchMain(args.Argc(), args.Argv());
        }
        ui16 AdminPort() const override {
            return SearchPort();
        }
        NSearch::NPseudoServer::ISearchRunner::TRequest PingRequest() const override {
            return {AdminPort(), "yandsearch?action=getversion"};
        }
        NSearch::NPseudoServer::ISearchRunner::TRequest ShutdownRequest() const override {
            return {AdminPort(), "admin?action=shutdown"};
        }
    };

public:
    bool InitEnv(bool muteSearchLog = false) {
        return NSearch::NPseudoServer::TSearchEnvironment::InitEnv(new TMetaSearchRunner(), muteSearchLog);
    }
};

using TMetaSearchFuzzingEnv = NSearch::NFuzzing::NCommon::TFuzzEnv<TMetaSearchEnvironment>;

extern "C" {
    int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
        static bool initialized = TMetaSearchFuzzingEnv::DoInit(true);
        if (initialized && size > 0) {
            TString encoded;
            TString cgi;

            try {
                encoded = StripString(TString{TStringBuf((const char*)data, size)});
                cgi = NFuzzing::NAppHostTools::ExtractCgiFromAppHost(encoded);
            } catch (...) {
                Cerr << "LLVMFuzzerTestOneInput: unable decode request :(, " << CurrentExceptionMessage() << Endl;
                Cerr << "Request: " << encoded << Endl;
                Cerr << "Cgi: " << cgi << Endl;
            }

            return TMetaSearchFuzzingEnv::TestOneInput(TStringBuilder() << "/yandsearch?" << cgi);
        }
        return 0;
    }
}
