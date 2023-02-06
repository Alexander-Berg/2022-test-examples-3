#include "redis_playlist_test.h"

#include <extsearch/video/vh/lb2redis/lib/options.h>
#include <extsearch/video/vh/lb2redis/lib/service.h>
#include <extsearch/video/vh/redis_service/library/handlers/yabs_playlist.h>
#include <extsearch/video/vh/redis_service/library/redis_adapter/redis_adapter.h>
#include <extsearch/video/vh/redis_service/library/options/options.h>

#include <extsearch/video/vh/playlist_service/library/common/resource_manager.h>

#include <extsearch/video/vh/apphost_lib/utils/utils.h>

#include <apphost/lib/service_testing/service_testing.h>

#include <library/cpp/json/json_writer.h>
#include <library/cpp/json/writer/json_value.h>
#include <library/cpp/resource/resource.h>

#include <util/generic/ptr.h>
#include <util/generic/string.h>
#include <util/stream/file.h>
#include <util/system/compiler.h>
#include <util/system/env.h>
#include <util/system/yassert.h>

namespace NVH::NPlaylistService::NRedis {

class TRedisPusherTest: public TRedisPusher {
public:
    TRedisPusherTest(const TRedisPusherOpts& opts)
        : TRedisPusher(opts)
    {
    }

    void Push(const TVector<TString>& messages) {
        ParseAndPush(messages);
    }
};

TPlaylistRedisTest::TPlaylistRedisTest(const TVector<TString>& lbMessage)
    : RedisSettings_(InitDatabase())
{
    TRedisPusherOpts pusherOpts;
    pusherOpts.RedisHost = RedisSettings_.Hosts.front();
    pusherOpts.RedisPassword = RedisSettings_.Password;
    TRedisPusherTest pusher(pusherOpts);
    pusher.Push(lbMessage);
}

NVH::NRedisDb::TRedisSettings TPlaylistRedisTest::InitDatabase() {
    const auto& host = GetEnv("REDIS_HOSTNAME", "localhost");
    const auto& port = (GetEnv("REDIS_PORT", "6379"));
    const auto& pass = GetEnv("REDIS_PASS");

    NVH::NRedisDb::TRedisSettings settings;

    settings.Hosts = {TString::Join(host, ":", port)};
    settings.Password = pass;

    return settings;
}

NJson::TJsonValue TPlaylistRedisTest::GetHttpRequest(const NVH::NPlaylistService::TStreamsByUuidRequest& request) const {
    NJson::TJsonValue result;
    TString protoString;
    if (!request.SerializeToString(&protoString)) {
        return {};
    }
    result["content"] = protoString;
    return result;
}

NVH::NPlaylistService::TStreamsByUuidResponse TPlaylistRedisTest::GetResponse(const NVH::NPlaylistService::TStreamsByUuidRequest& request) {
    SetEnv("STRM_SIGN_TOKEN", "token");
    SetEnv("SIGN_EXPIRATION_TIME", "864000");
    auto redis = MakeAtomicShared<NVH::NRedisDb::TRedisDb>(RedisSettings_);
    auto stats = MakeIntrusive<NStats::TTaggedClientStats<TString>>();
    THashMap<TString, NStats::TIntervals> signalsIntervals;
    THashMap<TString, TString> tags;
    auto statManager = MakeAtomicShared<NVhApphostStatistics::TVhStatManager>(stats, signalsIntervals);
    auto statHelper = MakeAtomicShared<NVH::TStatisticsHelper>(statManager, tags, "");
    NRedisService::TOptions options;
    options.DataFilePath = "flat.const";
    options.DataUpdateInterval = TDuration::Seconds(10);
    options.ReqansLog = "reqans.log";
    NRedisService::TYabsPlaylistHandler handler(options, redis, statHelper);
    NAppHost::NService::TTestContext ctx;
    ctx.AddItem(GetHttpRequest(request), "http_request", NAppHost::EContextItemKind::Input);
    handler.Handle(ctx);
    auto protoHttp = NVhApphost::GetProtoMessage<NAppHostHttp::THttpResponse>(ctx, "proto_http_response");
    TStreamsByUuidResponse result;
    Y_ENSURE(result.ParseFromString(protoHttp.GetContent()), "bad proto from proto_http_response");
    return result;
}

TVector<TString> TPlaylistRedisTest::FromResourceLbMessage(const TString& file) {
    const auto& fileMessages = NResource::Find(file);
    NJson::TJsonValue jsonLbMesssages;
    NJson::ReadJsonTree(fileMessages, &jsonLbMesssages, true);
    TVector<TString> result;
    for (const auto& message : jsonLbMesssages.GetArraySafe()) {
        result.emplace_back(NJson::WriteJson(message));
    }
    return result;
}

}
