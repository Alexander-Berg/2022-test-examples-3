#pragma once

#include <ymod_mdb_sharder/types.h>
#include <yplatform/log/contains_logger.h>

namespace ymod_mdb_sharder {

class fake_users_storage : public yplatform::log::contains_logger
{
public:
    fake_users_storage()
    {
        logger().append_log_prefix("fake_users_storage");
    }

    void add_user(const uid_t& uid, const shard_id& shard)
    {
        YLOG_L(info) << "add user " << uid << " to shard " << shard;
        ++current_ts;
        shard_user user;
        user.uid = uid;
        user.is_here = true;
        user.is_deleted = false;
        user.here_since_ts = current_ts;
        user.purge_ts = 0;
        users_[shard].emplace_back(user);
    }

    void delete_user(uid_t uid)
    {
        YLOG_L(info) << "delete user " << uid;
        ++current_ts;
        auto& user = find_user(uid);
        user.is_deleted = true;
        user.purge_ts = current_ts;
    }

    void move_user(uid_t uid, const shard_id& shard_to)
    {
        YLOG_L(info) << "move user " << uid << " to shard " << shard_to;
        ++current_ts;
        auto& user_in_old_shard = find_user(uid);

        shard_user user_in_new_shard = user_in_old_shard;
        user_in_new_shard.is_here = current_ts;
        users_[shard_to].emplace_back(user_in_new_shard);

        user_in_old_shard.is_here = false;
        user_in_old_shard.purge_ts = current_ts;
    }

    void get_all_users(const shard_id& shard, const shard_users_cb& cb)
    {
        YLOG_L(info) << "get all users from shard " << shard;
        cb({}, users_[shard]);
    }

    void get_changed_users(
        const shard_id& shard,
        std::time_t last_moved_ts,
        std::time_t last_deleted_ts,
        const shard_users_cb& cb)
    {
        YLOG_L(info) << "get changed users from shard " << shard;
        shard_users res;
        for (auto& user : users_[shard])
        {
            // Corresponds to logic in mdb.
            bool new_user = user.here_since_ts > last_moved_ts && user.is_here;
            bool moved_to_another_shard = user.purge_ts > last_moved_ts && !user.is_here;
            bool deleted_user = user.purge_ts > last_deleted_ts && user.is_deleted;
            if (new_user || moved_to_another_shard || deleted_user)
            {
                res.emplace_back(user);
            }
        }
        cb({}, res);
    }

private:
    shard_user& find_user(uid_t uid)
    {
        for (auto& [shard, users] : users_)
        {
            for (auto& user : users)
            {
                if (user.uid == uid && user.is_here)
                {
                    return user;
                }
            }
        }
        throw std::runtime_error("user not found");
    }

    std::map<shard_id, shard_users> users_;
    std::time_t current_ts = 0;
};

}