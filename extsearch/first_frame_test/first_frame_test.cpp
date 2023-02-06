#include <extsearch/video/vh/playlist_service/library/ut/lib/ugc_test.h>

namespace NVH::NPlaylistService {

Y_UNIT_TEST_SUITE(FirstFrameTest) {
    Y_UNIT_TEST(FrameFromStream) {
        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        const TString uuid = "vjt_8YAa2Hxg";
        const TString title = "Виды овчарок. Видео подборка";
        const TString thumbnail = "https://avatars.mds.yandex.net/get-vh/4339862/2a000001784b8fc6063e6c6864941b99c4ee/orig";
        const TString firstFrameUrl = "https://avatars.mds.yandex.net/get-vh/5633778/2a0000017d49543b2b1c7ce45e655f07a943/orig";
        const TString firstFrameStream = "https://s3.mds.yandex.net/zen-vod/vod-content/8055790277755898715/d4077605-afb72a1e-a15380ad-11587727/frames/frame_0_1920_1080_4100000.jpg";
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
        UNIT_ASSERT_VALUES_EQUAL(oneResponse.GetFirstFrameUrlPrefix(), firstFrameStream);
    }
}
}
