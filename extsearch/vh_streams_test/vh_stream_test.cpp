#include <extsearch/video/vh/playlist_service/library/ut/lib/vh_test.h>

namespace NVH::NPlaylistService {

TContentVersionByUuidFullData CreateFullData() {
    TContentVersionByUuidFullData fullData;
    auto& allStreams = fullData.OutputStreamData.StreamById;
    NVHProtos::TContentVersion cv1;
    const TString streamId1 = "9349909854436316173";
    cv1.SetOutputStream("https://strm.yandex.ru/kal/weather_saint_petersburg2/weather_saint_petersburg20.m3u8");
    cv1.SetOutputStreamId(9349909854436316173u);
    auto& meta1 = *cv1.MutableMeta();
    meta1.SetTitle("main");
    meta1.SetTechnicalName("weather_saint_petersburg2");
    meta1.SetPlaylistGeneration("from-technical-name");
    meta1.SetIsDefaultStream(true);
    allStreams[streamId1] = cv1;

    fullData.OutputStreamData.StreamIdByUuid["48172d6679c147c6904172fac906f643"].push_back(streamId1);
    return fullData;

}


Y_UNIT_TEST_SUITE(VhStreamsTests) {
    Y_UNIT_TEST(VhHlsDashTest) {

        const TString uuid = "48172d6679c147c6904172fac906f643";

        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        streamParam.SetUuid(uuid);

        auto result = VhTest(request, CreateFullData());

        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 1);

        auto& oneResponse = result.GetSingleStream().at(0);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetUuid(), uuid);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetStreamInfo().size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetOutputStreamId(), 9349909854436316173u);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetTitle(), "main");
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetPlaylistGeneration(), "from-technical-name");

        auto& firstStream = oneResponse.GetStreamInfo().at(0);
        UNIT_ASSERT(firstStream.GetStreamType() == EStreamType::ST_DASH);

        auto& secondStream = oneResponse.GetStreamInfo().at(1);
        UNIT_ASSERT(secondStream.GetStreamType() == EStreamType::ST_HLS);
    }
    Y_UNIT_TEST(VhHlsTest) {
        const TString uuid = "48172d6679c147c6904172fac906f643";

        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        streamParam.SetUuid(uuid);
        streamParam.SetStreamType("hls");

        auto result = VhTest(request, CreateFullData());

        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 1);

        auto& oneResponse = result.GetSingleStream().at(0);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetStreamInfo().size(), 1);

        auto& secondStream = oneResponse.GetStreamInfo().at(0);
        UNIT_ASSERT(secondStream.GetStreamType() == EStreamType::ST_HLS);
    }
}
}