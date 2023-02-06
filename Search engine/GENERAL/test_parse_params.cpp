#include <search/scraper_over_yt/scheduler/scheduler.h>

#include <search/scraper_over_yt/mapper/lib/proto/params.pb.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/yson/node/node_io.h>

namespace NScraperOverYTTest {

    Y_UNIT_TEST_SUITE(ParseArgsByScraper) {
        Y_UNIT_TEST(TestProcessUniproxyFetcherParams) {
            NScraperOverYT::TUniproxyFetcherInputParams uniproxyParams;
            const auto tnode_spec = NYT::NodeFromJsonString(R"({
                "args": {
                    "vins_url": "http://vins.hamster.alice.yandex.net/speechkit/app/pa/",
                    "asr_chunk_delay_ms": 12,
                    "auth_token": "6e9e2484-5f4a-45f1-b857-47ea867bfe8a",
                    "uniproxy_url": "wss://beta.uniproxy.alice.yandex.net/alice-uniproxy-hamster/uni.ws",
                    "asr_chunk_size": 1234
                }
            })");
            NScraperOverYT::ProcessUniproxyFetcherParams(tnode_spec, uniproxyParams);
            UNIT_ASSERT_VALUES_EQUAL(uniproxyParams.GetAsrChunkSize(), 1234);
            UNIT_ASSERT_VALUES_EQUAL(uniproxyParams.GetAsrChunkDelayMs(), 12);
        }

        Y_UNIT_TEST(TestProcessUniproxyFetcherParamsNoAsr) {
            NScraperOverYT::TUniproxyFetcherInputParams uniproxyParams;
            const auto tnode_spec = NYT::NodeFromJsonString(R"({
                "args": {
                    "vins_url": "http://vins.hamster.alice.yandex.net/speechkit/app/pa/",
                    "auth_token": "6e9e2484-5f4a-45f1-b857-47ea867bfe8a",
                    "uniproxy_url": "wss://beta.uniproxy.alice.yandex.net/alice-uniproxy-hamster/uni.ws"
                }
            })");
            NScraperOverYT::ProcessUniproxyFetcherParams(tnode_spec, uniproxyParams);
            // defaults
            UNIT_ASSERT_VALUES_EQUAL(uniproxyParams.GetAsrChunkSize(), 820);
            UNIT_ASSERT_VALUES_EQUAL(uniproxyParams.GetAsrChunkDelayMs(), 0);
        }

        Y_UNIT_TEST(TestProcessUniproxyFetcherParamsBadAsr) {
            NScraperOverYT::TUniproxyFetcherInputParams uniproxyParams;
            const auto tnode_spec = NYT::NodeFromJsonString(R"({
                "args": {
                    "vins_url": "http://vins.hamster.alice.yandex.net/speechkit/app/pa/",
                    "auth_token": "6e9e2484-5f4a-45f1-b857-47ea867bfe8a",
                    "uniproxy_url": "wss://beta.uniproxy.alice.yandex.net/alice-uniproxy-hamster/uni.ws",
                    "asr_chunk_size": "abcde"
                }
            })");
            UNIT_ASSERT_EXCEPTION(
                NScraperOverYT::ProcessUniproxyFetcherParams(tnode_spec, uniproxyParams),
                NScraperOverYT::TSpecValidationException
            );
        }

        Y_UNIT_TEST(TestProcessUniproxyFetcherParamsAsrUnderZeroValid) {
            NScraperOverYT::TUniproxyFetcherInputParams uniproxyParams;
            const auto tnode_spec = NYT::NodeFromJsonString(R"({
                "args": {
                    "vins_url": "http://vins.hamster.alice.yandex.net/speechkit/app/pa/",
                    "auth_token": "6e9e2484-5f4a-45f1-b857-47ea867bfe8a",
                    "uniproxy_url": "wss://beta.uniproxy.alice.yandex.net/alice-uniproxy-hamster/uni.ws",
                    "asr_chunk_size": -1
                }
            })");
            NScraperOverYT::ProcessUniproxyFetcherParams(tnode_spec, uniproxyParams);
            UNIT_ASSERT_VALUES_EQUAL(uniproxyParams.GetAsrChunkSize(), -1);
            UNIT_ASSERT_VALUES_EQUAL(uniproxyParams.GetAsrChunkDelayMs(), 0);
        }

        Y_UNIT_TEST(TestProcessUniproxyFetcherParamsAsrUnderZeroInvalid) {
            NScraperOverYT::TUniproxyFetcherInputParams uniproxyParams;
            const auto tnode_spec = NYT::NodeFromJsonString(R"({
                "args": {
                    "vins_url": "http://vins.hamster.alice.yandex.net/speechkit/app/pa/",
                    "auth_token": "6e9e2484-5f4a-45f1-b857-47ea867bfe8a",
                    "uniproxy_url": "wss://beta.uniproxy.alice.yandex.net/alice-uniproxy-hamster/uni.ws",
                    "asr_chunk_delay_ms": -1
                }
            })");
            UNIT_ASSERT_EXCEPTION(
                NScraperOverYT::ProcessUniproxyFetcherParams(tnode_spec, uniproxyParams),
                NScraperOverYT::TSpecValidationException
            );
        }

        Y_UNIT_TEST(TestProcessUniproxyFetcherParamsBadAsrNull) {
            NScraperOverYT::TUniproxyFetcherInputParams uniproxyParams;
            const auto tnode_spec = NYT::NodeFromJsonString(R"({
                "args": {
                    "vins_url": "http://vins.hamster.alice.yandex.net/speechkit/app/pa/",
                    "auth_token": "6e9e2484-5f4a-45f1-b857-47ea867bfe8a",
                    "uniproxy_url": "wss://beta.uniproxy.alice.yandex.net/alice-uniproxy-hamster/uni.ws",
                    "asr_chunk_size": null
                }
            })");
            UNIT_ASSERT_EXCEPTION(
                NScraperOverYT::ProcessUniproxyFetcherParams(tnode_spec, uniproxyParams),
                NScraperOverYT::TSpecValidationException
            );
        }
    }

} // namespace NScraperOverYTTest
