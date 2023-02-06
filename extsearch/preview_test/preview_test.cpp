#include <extsearch/video/vh/lb2redis/ft/configurator/redis_playlist_test.h>

#include <extsearch/video/vh/playlist_service/library/data_structures/protos/handle_by_uuid_structs.pb.h>

#include <library/cpp/testing/unittest/registar.h>

namespace NVH::NPlaylistService::NRedis {

Y_UNIT_TEST_SUITE(SimpleRedisPlaylistTest) {
    Y_UNIT_TEST(CheckStreamCountTest) {
        TStreamsByUuidRequest request;
        auto& streamParam = *request.MutableStreamParams() -> Add();
        streamParam.SetUuid("vLsBcTWM49Qs");

        const auto& messages = TPlaylistRedisTest::FromResourceLbMessage("lb_messages.json");
        TPlaylistRedisTest test(messages);
        const auto& response = test.GetResponse(request);

        UNIT_ASSERT_VALUES_EQUAL(response.GetSingleStream().size(), 1);

        /*
        const auto& singleStream = response.GetSingleStream().at(0);
        const auto& previews = singleStream.GetPreviewTargetWidthToUrl();

        UNIT_ASSERT_VALUES_EQUAL(previews.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(previews.contains(640), true);
        UNIT_ASSERT_VALUES_EQUAL(previews.at(640), "https://video-preview.s3.yandex.net/ugc/866014ff56000eb7e7b7d321838df7a4_vmaf-preview-360.mp4");

        const auto& longPreviews = singleStream.GetLongPreviewTargetWidthToUrl();

        UNIT_ASSERT_VALUES_EQUAL(longPreviews.size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(longPreviews.contains(480), true);
        UNIT_ASSERT_VALUES_EQUAL(longPreviews.at(480), "https://video-preview.s3.yandex.net/ugc/866014ff56000eb7e7b7d321838df7a4_simple-preview-360.mp4");

        UNIT_ASSERT_VALUES_EQUAL(longPreviews.contains(1280), true);
        UNIT_ASSERT_VALUES_EQUAL(longPreviews.at(1280), "https://video-preview.s3.yandex.net/ugc/866014ff56000eb7e7b7d321838df7a4_vmaf-preview-480.mp4");
        */

    }
}

}
