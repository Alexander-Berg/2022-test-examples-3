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
        const auto& singleStream = response.GetSingleStream().at(0);
        UNIT_ASSERT_VALUES_EQUAL(singleStream.GetStreamInfo().size(), 2);
        const auto& streamInfo = singleStream.GetStreamInfo();
        UNIT_ASSERT(streamInfo[0].streamtype() == EStreamType::ST_DASH);
        UNIT_ASSERT(streamInfo[1].streamtype() == EStreamType::ST_HLS);
    }
}

}
