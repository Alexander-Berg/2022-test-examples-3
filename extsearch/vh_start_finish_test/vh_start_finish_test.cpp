#include <extsearch/video/vh/playlist_service/library/ut/lib/vh_test.h>


namespace {
    TString GetSignParams(const TString& url, const TString& param) {
        TVector<TStringBuf> result;
        TStringBuf urlView = url;
        TStringBuf left, right;
        urlView.Split(param + "=", left, right);
        right.Split(',', left, right);
        return TString{left};
    }
}


namespace NVH::NPlaylistService {

TContentVersionByUuidFullData CreateFullData() {
    TContentVersionByUuidFullData fullData;
    auto& allStreams = fullData.OutputStreamData.StreamById;
    NVHProtos::TContentVersion cv1;
    const TString streamId1 = "10963919078351684589";
    cv1.SetOutputStream("https://strm.yandex.ru/kal/start/start0.m3u8");
    cv1.SetOutputStreamId(10963919078351684589u);
    auto& meta1 = *cv1.MutableMeta();
    meta1.SetTitle("_standard");
    meta1.SetTechnicalName("start");
    meta1.SetPlaylistGeneration("from-technical-name");
    meta1.SetIsDefaultStream(true);
    allStreams[streamId1] = cv1;

    fullData.OutputStreamData.StreamIdByUuid["4f94e3aa2906e464a8e45a1030c4279d"].push_back(streamId1);
    THashMap<TString, TString> resources1 = {
        {"content_type_name", "episode"},
        {"finish_time", "1653254100"},
        {"start_time", "1653245700"}
    };
    fullData.ResourceData["4f94e3aa2906e464a8e45a1030c4279d"] = resources1;

    return fullData;

}


Y_UNIT_TEST_SUITE(VhStreamsTests) {
    Y_UNIT_TEST(VhHlsDashTest) {

        const TString uuid = "4f94e3aa2906e464a8e45a1030c4279d";

        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        streamParam.SetUuid(uuid);

        auto result = VhTest(request, CreateFullData());

        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 1);

        auto& oneResponse = result.GetSingleStream().at(0);

        auto& firstStream = oneResponse.GetStreamInfo().at(0);

        const auto& firstStreamUrl = firstStream.GetOutputStream();
        const auto& firstStart = GetSignParams(firstStreamUrl, "start");
        const auto& firstEnd = GetSignParams(firstStreamUrl, "end");

        UNIT_ASSERT_VALUES_EQUAL(firstStart, "1653245700");
        UNIT_ASSERT_VALUES_EQUAL(firstEnd, "1653254100");

        auto& secondStream = oneResponse.GetStreamInfo().at(1);

        const auto& secondStreamUrl = secondStream.GetOutputStream();
        const auto& secondStart = GetSignParams(secondStreamUrl, "start");
        const auto& secondEnd = GetSignParams(secondStreamUrl, "end");

        UNIT_ASSERT_VALUES_EQUAL(secondStart, "1653245700");
        UNIT_ASSERT_VALUES_EQUAL(secondEnd, "1653254100");
    }
}
}
