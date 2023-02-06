#pragma once

#include <ymod_mdb_sharder/types.h>
#include <yplatform/log/contains_logger.h>
#include <sharpei_client/sharpei_client.h>

namespace ymod_mdb_sharder {

class fake_sharpei_client : public yplatform::log::contains_logger
{
public:
    fake_sharpei_client()
    {
        logger().append_log_prefix("fake_sharpei_client");
    }

    void asyncStat(sharpei::client::SharpeiClient::AsyncMapHandler handler) const
    {
        sharpei::client::MapShard res;
        for (auto& shard : shards_)
        {
            res[shard].id = shard;
        }
        handler({}, res);
    }

    void asyncGetConnInfo(
        const sharpei::client::ResolveParams& params,
        sharpei::client::SharpeiClient::AsyncHandler handler) const
    {
        auto it = users_.find(params.uid);
        if (it == users_.end())
        {
            return handler(
                sharpei::client::make_error_code(sharpei::client::Errors::UidNotFound), {});
        }
        sharpei::client::Shard res;
        res.id = it->second;
        handler({}, res);
    }

    void assign_user_to_shard(uid_t uid, const shard_id& shard)
    {
        YLOG_L(info) << "assign user " << uid << " to shard " << shard;
        users_[std::to_string(uid)] = shard;
    }

    void add_shards(const shard_ids& shards)
    {
        for (auto& shard : shards)
        {
            YLOG_L(info) << "add shard " << shard;
            shards_.insert(shard);
        }
    }

    void del_shards(const shard_ids& shards)
    {
        for (auto& shard : shards)
        {
            YLOG_L(info) << "del shard " << shard;
            shards_.erase(shard);
        }
    }

private:
    std::map<std::string, shard_id> users_;
    std::set<shard_id> shards_;
};

}