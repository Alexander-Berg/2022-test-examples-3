#include <extsearch/video/vh/playlist_service/library/ut/lib/ugc_test.h>

namespace NVH::NPlaylistService {

Y_UNIT_TEST_SUITE(StreamsTests) {
    Y_UNIT_TEST(HlsAndDash) {
        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        const TString uuid = "vjt_8YAa2Hxg";
        const TString title = "Виды овчарок. Видео подборка";
        const TString thumbnail = "https://avatars.mds.yandex.net/get-vh/4339862/2a000001784b8fc6063e6c6864941b99c4ee/orig";
        const TString firstFrameUrl = "https://avatars.mds.yandex.net/get-vh/5633778/2a0000017d49543b2b1c7ce45e655f07a943/orig";
        TStringBuf firstFrameUrlView = TStringBuf{firstFrameUrl};
        firstFrameUrlView.ChopSuffix("/orig");

        streamParam.SetUuid(uuid);

        auto ugcItem = MakeAtomicShared<TUgcContentInfo>();
        ugcItem->ID = uuid;
        ugcItem->Streams = ReadFromFile("streams.json");
        ugcItem->Title = title;
        ugcItem->Thumbnail = thumbnail;
        ugcItem->FirstFrameUrl = firstFrameUrl;
        TVector<TAtomicSharedPtr<TUgcContentInfo>> ugcContentInfo;
        ugcContentInfo.emplace_back(std::move(ugcItem));
        auto result = UgcTest(request, std::move(ugcContentInfo));

        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 1);

        auto& oneResponse = result.GetSingleStream().at(0);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetUuid(), uuid);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetStreamInfo().size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetTitle(), title);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetThumbnail(), thumbnail);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetPlaylistGeneration(), "from-vod");
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetFirstFrameUrlPrefix(), firstFrameUrlView);

        auto& firstStream = oneResponse.GetStreamInfo().at(0);
        UNIT_ASSERT(firstStream.GetStreamType() == EStreamType::ST_DASH);

        auto& secondStream = oneResponse.GetStreamInfo().at(1);
        UNIT_ASSERT(secondStream.GetStreamType() == EStreamType::ST_HLS);
    }
    Y_UNIT_TEST(WithTagInRequest) {
        const TString uuid = "vjt_8YAa2Hxg";
        const TString title = "Виды овчарок. Видео подборка";
        const TString thumbnail = "https://avatars.mds.yandex.net/get-vh/4339862/2a000001784b8fc6063e6c6864941b99c4ee/orig";
        const TString firstFrameUrl = "https://avatars.mds.yandex.net/get-vh/5633778/2a0000017d49543b2b1c7ce45e655f07a943/orig";

        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        streamParam.SetUuid(uuid);
        streamParam.SetTag(NVideo::EStreamTag::EST_MOBILE_VERTICAL);

        auto ugcItem = MakeAtomicShared<TUgcContentInfo>();
        ugcItem->ID = uuid;
        ugcItem->Streams = ReadFromFile("streams.json");
        ugcItem->Title = title;
        ugcItem->Thumbnail = thumbnail;
        ugcItem->FirstFrameUrl = firstFrameUrl;
        TVector<TAtomicSharedPtr<TUgcContentInfo>> ugcContentInfo;
        ugcContentInfo.emplace_back(std::move(ugcItem));
        auto result = UgcTest(request, std::move(ugcContentInfo));

        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 1);
        // Has tag in request, but stream without tags.
        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().at(0).GetTags().size(), 0);
    }
    Y_UNIT_TEST(BackendId) {
        const TString uuid = "vjt_8YAa2Hxg";
        const TString title = "Виды овчарок. Видео подборка";
        const TString thumbnail = "https://avatars.mds.yandex.net/get-vh/4339862/2a000001784b8fc6063e6c6864941b99c4ee/orig";
        const TString firstFrameUrl = "https://avatars.mds.yandex.net/get-vh/5633778/2a0000017d49543b2b1c7ce45e655f07a943/orig";

        TStreamsByUuidRequest request;
        auto& backend1 = *request.MutableStreamParams()->Add();
        backend1.SetUuid(uuid);
        backend1.SetBackendId("B1");

        auto& backend2 = *request.MutableStreamParams()->Add();
        backend2.SetUuid(uuid);
        backend2.SetBackendId("B2");

        auto ugcItem = MakeAtomicShared<TUgcContentInfo>();
        ugcItem->ID = uuid;
        ugcItem->Streams = ReadFromFile("streams.json");
        ugcItem->Title = title;
        ugcItem->Thumbnail = thumbnail;
        ugcItem->FirstFrameUrl = firstFrameUrl;
        TVector<TAtomicSharedPtr<TUgcContentInfo>> ugcContentInfo;
        ugcContentInfo.emplace_back(std::move(ugcItem));

        auto result = UgcTest(request, std::move(ugcContentInfo));

        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 2);

        auto& responseB1 = result.GetSingleStream().at(0);
        UNIT_ASSERT_VALUES_EQUAL(responseB1.GetBackendId(), "B2");

        auto& responseB2 = result.GetSingleStream().at(1);
        UNIT_ASSERT_VALUES_EQUAL(responseB2.GetBackendId(), "B1");
    }
    Y_UNIT_TEST(HlsAndDashRequested) {
        const TString uuid = "vjt_8YAa2Hxg";
        const TString title = "Виды овчарок. Видео подборка";
        const TString thumbnail = "https://avatars.mds.yandex.net/get-vh/4339862/2a000001784b8fc6063e6c6864941b99c4ee/orig";
        const TString firstFrameUrl = "https://avatars.mds.yandex.net/get-vh/5633778/2a0000017d49543b2b1c7ce45e655f07a943/orig";

        TStreamsByUuidRequest request;
        auto& streamParam1 = *request.MutableStreamParams()->Add();
        streamParam1.SetUuid(uuid);
        streamParam1.SetStreamType("hls");

        auto& streamParam2 = *request.MutableStreamParams()->Add();
        streamParam2.SetUuid(uuid);
        streamParam2.SetStreamType("dash");

        auto ugcItem = MakeAtomicShared<TUgcContentInfo>();
        ugcItem->ID = uuid;
        ugcItem->Streams = ReadFromFile("streams.json");
        ugcItem->Title = title;
        ugcItem->Thumbnail = thumbnail;
        ugcItem->FirstFrameUrl = firstFrameUrl;
        TVector<TAtomicSharedPtr<TUgcContentInfo>> ugcContentInfo;
        ugcContentInfo.emplace_back(std::move(ugcItem));
        auto result = UgcTest(request, std::move(ugcContentInfo));

        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 1);

        auto& oneResponse = result.GetSingleStream().at(0);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetStreamInfo().size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetPlaylistGeneration(), "from-vod");

        auto& firstStream = oneResponse.GetStreamInfo().at(0);
        UNIT_ASSERT(firstStream.GetStreamType() == EStreamType::ST_DASH);

        auto& secondStream = oneResponse.GetStreamInfo().at(1);
        UNIT_ASSERT(secondStream.GetStreamType() == EStreamType::ST_HLS);
    }
}
}