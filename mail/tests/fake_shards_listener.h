#pragma once

#include <ymod_mdb_sharder/types.h>
#include <yplatform/log/contains_logger.h>

namespace ymod_mdb_sharder {

class fake_shards_listener : public yplatform::log::contains_logger
{
public:
    fake_shards_listener()
    {
        logger().append_log_prefix("fake_shards_listener");
    }

    void subscribe(const shard_ids_cb& on_new_shards, const shard_ids_cb& on_remove_shards)
    {
        on_new_shards_ = on_new_shards;
        on_remove_shards_ = on_remove_shards;
    }

    void add_shards(const shard_ids& shards)
    {
        for (auto& shard : shards)
        {
            YLOG_L(info) << "add shard " << shard;
        }
        shards_.insert(shards.begin(), shards.end());
        on_new_shards_(shards);
    }

    void del_shards(const shard_ids& shards)
    {
        for (auto& shard : shards)
        {
            YLOG_L(info) << "del shard " << shard;
            shards_.erase(shard);
        }
        on_remove_shards_(shards);
    }

    void get_shards(const shard_ids_cb& cb)
    {
        cb({ shards_.begin(), shards_.end() });
    }

private:
    shard_ids_cb on_new_shards_;
    shard_ids_cb on_remove_shards_;
    std::set<shard_id> shards_;
};

}