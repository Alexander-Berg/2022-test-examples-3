#pragma once
#include <extsearch/video/vh/redis_service/library/redis_adapter/redis_adapter.h>

#include <extsearch/video/vh/playlist_service/library/data_structures/protos/handle_by_uuid_structs.pb.h>

#include <extsearch/video/vh/library/redis_db/redis_db.h>

#include <library/cpp/json/writer/json_value.h>

namespace NVH::NPlaylistService::NRedis {

class TPlaylistRedisTest {
public:
    TPlaylistRedisTest(const TVector<TString>& lbMessage);
    NVH::NPlaylistService::TStreamsByUuidResponse GetResponse(const NVH::NPlaylistService::TStreamsByUuidRequest& request);
    static TVector<TString> FromResourceLbMessage(const TString& file);

private:
    NVH::NRedisDb::TRedisSettings InitDatabase();
    NJson::TJsonValue GetHttpRequest(const NVH::NPlaylistService::TStreamsByUuidRequest& request) const;
    TStreamsByUuidResponse GetHttpResponse(const NJson::TJsonValue& httpRequest) const;

private:
    NVH::NRedisDb::TRedisSettings RedisSettings_;
};
}
