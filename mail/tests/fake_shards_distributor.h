#pragma once

#include <ymod_mdb_sharder/types.h>
#include <ymod_mdb_sharder/errors.h>
#include <yplatform/log/contains_logger.h>

namespace ymod_mdb_sharder {

class fake_shards_distributor : public yplatform::log::contains_logger
{
public:
    fake_shards_distributor()
    {
        logger().append_log_prefix("fake_shards_distributor");
    }

    void subscribe(const shard_ids_cb& on_acquire_shards, const shard_ids_cb& on_release_shards)
    {
        on_acquire_shards_ = on_acquire_shards;
        on_release_shards_ = on_release_shards;
    }

    void get_owner(task_context_ptr, const shard_id& shard, const node_info_cb& cb)
    {
        if (shard_owner_.count(shard))
        {
            cb({}, shard_owner_[shard]);
        }
        else
        {
            cb(error::not_owned, {});
        }
    }

    void set_owner(const shard_id& shard, const node_info& owner)
    {
        bool was_owner = shard_owner_.count(shard) && shard_owner_[shard].id == my_node_id_;
        bool is_owner = owner.id == my_node_id_;
        shard_owner_[shard] = owner;
        if (!was_owner && is_owner)
        {
            YLOG_L(info) << "acquire shard " << shard;
            on_acquire_shards_({ shard });
        }
        else if (was_owner && !is_owner)
        {
            YLOG_L(info) << "release shard " << shard;
            on_release_shards_({ shard });
        }
    }

    void reset_owner(const shard_id& shard)
    {
        bool was_owner = shard_owner_.count(shard) && shard_owner_[shard].id == my_node_id_;
        shard_owner_.erase(shard);
        if (was_owner)
        {
            YLOG_L(info) << "release shard " << shard;
            on_release_shards_({ shard });
        }
    }

    std::string my_node_id() const
    {
        return my_node_id_;
    }

private:
    shard_ids_cb on_acquire_shards_;
    shard_ids_cb on_release_shards_;
    std::map<shard_id, node_info> shard_owner_;
    std::string my_node_id_ = "my_node_id";
};

}