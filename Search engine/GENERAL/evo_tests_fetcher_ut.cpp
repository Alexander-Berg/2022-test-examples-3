#include "library/cpp/yson/node/node.h"
#include "mapreduce/yt/interface/io.h"
#include <search/scraper_over_yt/mapper/lib/evo_tests_fetcher.h>
#include <search/scraper_over_yt/mapper/lib/proto/voice_output.pb.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/hook/hook.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/yson/node/node_io.h>

#include <yt/yt/core/misc/shutdown.h>

namespace {
    using namespace NJson;
    using namespace NScraperOverYT;

    std::unique_ptr<TEvoTestsFetcher> CreateEvoTestsFetcher() {
        TUniproxyFetcherInputParams params;
        params.SetUniproxyUrl("wss://beta.uniproxy.alice.yandex.net/alice-uniproxy-hamster/uni.ws");
        params.SetAuthToken("6e9e2484-5f4a-45f1-b857-47ea867bfe8a");
        params.SetVinsUrl("http://vins.hamster.alice.yandex.net/speechkit/app/pa/");
        params.SetRetriesPerJob(2);
        params.SetRetriesPerRequest(3);
        params.SetAsrChunkSize(-1);
        params.SetAsrChunkDelayMs(0);
        params.SetTunnellerValidationRetriesPerRequest(2);
        params.SetEvoTestsStartRetries(80);
        params.SetEvoTestsStartDelayMs(5000);

        params.SetFlags("[\"alice_evo_tests\"]");
        TString path = BinaryPath("alice/tests/integration_tests/alice-tests-integration_tests");
        params.SetBinaryPath(path);

        TString toEvoPath = "to_evo";
        TString fromEvoPath = "from_evo";
        TString evoDir = "evo_tests_dir";
        TJsonValue binParams;
        binParams["to_evo_path"] = toEvoPath;
        binParams["from_evo_path"] = fromEvoPath;
        binParams["evo_dir"] = evoDir;
        params.SetBinaryParams(binParams.GetStringRobust());
        return std::make_unique<TEvoTestsFetcher>(params);
    }

    TRowHandleResult MakeTestRequestsSession(TString sessionResourceName) {
        auto rawYson = NResource::Find(sessionResourceName);
        auto row = NYT::NodeFromYsonString(TStringBuf(rawYson), ::NYson::EYsonType::Node);

        auto fetcher = CreateEvoTestsFetcher();
        fetcher->BuildEvoTestsBinaryTesting(row, /*oneRow*/ true);
        return fetcher->ProcessRequests(row);
    }

    Y_UNIT_TEST_SUITE(TEvoTestsFetcher) {
        Y_UNIT_TEST(TestPogoda) {
            auto result = MakeTestRequestsSession("/data/evo_input_table_pogoda.yson");
            UNIT_ASSERT_EQUAL(result.OutputRows.size(), 1);
            for (const auto& it : result.OutputRows) {
                UNIT_ASSERT_EQUAL(it.second, 0);
            }
        }

        Y_UNIT_TEST(BigTable) {
            auto rawYson = NResource::Find("/data/evo_input_table_big.yson");
            auto rows = NYT::NodeFromYsonString(TStringBuf(rawYson), ::NYson::EYsonType::Node);

            auto fetcher = CreateEvoTestsFetcher();
            fetcher->BuildEvoTestsBinaryTesting(rows, /*oneRow*/ false);
            for (auto& row : rows.AsList()) {
                for (i64 test_index = 0; test_index < row["session_requests"][0]["tests_count"].AsInt64(); ++test_index) {
                    auto result = fetcher->ProcessRequests(NYT::TNode(""));
                    UNIT_ASSERT_EQUAL(result.OutputRows.size(), 1);
                    for (auto it : result.OutputRows) {
                        UNIT_ASSERT_EQUAL(it.second, 0);
                    }
                }
            }
        }
    }

    Y_TEST_HOOK_AFTER_RUN(YT_TEARDOWN) {
        NYT::Shutdown();
    }
}
