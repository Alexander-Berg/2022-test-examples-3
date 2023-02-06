#include <extsearch/video/vh/playlist_service/library/ut/lib/ugc_test.h>
#include <util/string/split.h>

namespace {
    TVector<TStringBuf> GetSignParams(const TString& url, const TString& param) {
        TVector<TStringBuf> result;
        TStringBuf urlView = url;
        TStringBuf left, right;
        urlView.Split(param + "=", left, right);
        right.Split(',', left, right);
        Split(left, ";", result);
        return result;
    }
}

namespace NVH::NPlaylistService {

Y_UNIT_TEST_SUITE(StreamsTests) {
    Y_UNIT_TEST(HlsAndDash) {

        const TString uuid = "vjt_8YAa2Hxg";
        const TString title = "Виды овчарок. Видео подборка";
        const TString thumbnail = "https://avatars.mds.yandex.net/get-vh/4339862/2a000001784b8fc6063e6c6864941b99c4ee/orig";
        const TString firstFrameUrl = "https://avatars.mds.yandex.net/get-vh/5633778/2a0000017d49543b2b1c7ce45e655f07a943/orig";

        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams()->Add();
        streamParam.SetUuid(uuid);
        streamParam.SetStreamType("dash");
        auto& preroll = *streamParam.MutablePrerolls()->Add();
        preroll.SetContentGroupId("4c76836a5ee6dd69a6a1c48c769cdee2");
        preroll.SetTranscodingId("1644854103");
        preroll.SetOutputStreamUrl("kaltura/dash_clear_sdr_hd_avc_aac");
        preroll.SetCpid(2);

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

        auto& firstStream = oneResponse.GetStreamInfo().at(0);
        const auto& prerolls = GetSignParams(firstStream.GetOutputStream(), "prerolls");
        UNIT_ASSERT_VALUES_EQUAL(prerolls.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(prerolls[0], "4c76836a5ee6dd69a6a1c48c769cdee2*1644854103*kaltura*dash_clear_sdr_hd_avc_aac");
        const auto& cpids = GetSignParams(firstStream.GetOutputStream(), "cpids");
        UNIT_ASSERT_VALUES_EQUAL(cpids.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(cpids[0], "2");

    }
}
}