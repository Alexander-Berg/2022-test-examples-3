#include <extsearch/video/vh/playlist_service/library/ut/lib/ugc_test.h>

namespace {
    TAtomicSharedPtr<TUgcContentInfo> CreateDoc(const TString& uuid) {
        auto ugcItem = MakeAtomicShared<TUgcContentInfo>();
        ugcItem->ID = uuid;
        ugcItem->Title = "title";
        ugcItem->Thumbnail = "https://avatars.mds.yandex.net/get-vh/4339862/2a000001784b8fc6063e6c6864941b99c4ee/orig";
        ugcItem->FirstFrameUrl = "https://avatars.mds.yandex.net/get-vh/5633778/2a0000017d49543b2b1c7ce45e655f07a943/orig";
        ugcItem->Streams = NVH::NPlaylistService::ReadFromFile("est_mobile_stream.json");
        return ugcItem;
    }

}

namespace NVH::NPlaylistService {
Y_UNIT_TEST_SUITE(TagTest) {
    Y_UNIT_TEST(NoTag) {
        UNIT_ASSERT_VALUES_EQUAL(IsValidByTag(0, {}), true);
        UNIT_ASSERT_VALUES_EQUAL(IsValidByTag(0, {2, 3, 4, 5}), false);
    }
    Y_UNIT_TEST(HasTag) {
        UNIT_ASSERT_VALUES_EQUAL(IsValidByTag(1, {}), false);
        UNIT_ASSERT_VALUES_EQUAL(IsValidByTag(1, {2, 3, 4, 5}), false);
        UNIT_ASSERT_VALUES_EQUAL(IsValidByTag(1, {1, 2, 3}), true);
    }
}

Y_UNIT_TEST_SUITE(TagVerticalTest) {
    Y_UNIT_TEST(HasVerticalTag) {
        const TString uuid = "vmr6Mr9XUYUM";

        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        streamParam.SetUuid(uuid);
        streamParam.SetTag(NVideo::EStreamTag::EST_MOBILE_VERTICAL);

        TVector<TAtomicSharedPtr<TUgcContentInfo>> ugcContentInfo;
        ugcContentInfo.emplace_back(CreateDoc(uuid));

        auto result = UgcTest(request, std::move(ugcContentInfo));
        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 1);
    }
    Y_UNIT_TEST(NoTag) {
        const TString uuid = "vmr6Mr9XUYUM";

        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        streamParam.SetUuid(uuid);

        TVector<TAtomicSharedPtr<TUgcContentInfo>> ugcContentInfo;
        ugcContentInfo.emplace_back(CreateDoc(uuid));

        auto result = UgcTest(request, std::move(ugcContentInfo));
        UNIT_ASSERT_VALUES_EQUAL(result.GetSingleStream().size(), 17);
    }
}
}
