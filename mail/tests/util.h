#pragma once

#include <internal/poller/meta_shards_provider.h>
#include <internal/poller/poller.h>
#include <internal/shard.h>

#include <yplatform/task_context.h>

#include <boost/range/adaptors.hpp>

using namespace sharpei;
using namespace cache;

using Address = Shard::Database::Address;
using Database = Shard::Database;
using Role = Shard::Database::Role;
using State = Shard::Database::State;
using Status = Shard::Database::Status;

namespace sharpei {

inline std::string genRandomString() {
    return yplatform::task_context().uniq_id();
}

inline Database makeDatabase(
    Role role, Status status, State state = {.lag = 0}, std::string dc = "sas", std::string host = genRandomString()) {
    static unsigned nextPort = 0;
    const Database::Address addr(host, ++nextPort, genRandomString(), dc);
    return Database(addr, role, status, state);
}

inline CachePtr makeCache(std::initializer_list<Database> dbs) {
    const auto shardId = poller::MetaShardsProvider::shardId;
    const auto shardName = poller::MetaShardsProvider::shardName;
    auto cache = std::make_shared<Cache>(5, 3);
    cache->shardName.update(shardId, shardName);
    const auto roles = dbs | boost::adaptors::transformed([](auto& db) {
                           return std::pair<Address, boost::optional<Role>>(db.address(), db.role());
                       });
    cache->role.update(shardId, {roles.begin(), roles.end()});
    const auto states = dbs | boost::adaptors::transformed([](auto& db) {
                            return std::pair<Address, boost::optional<State>>(db.address(), db.state());
                        });
    cache->state.update(shardId, {states.begin(), states.end()});
    for (const auto& db : dbs) {
        if (db.status() == Database::Status::Alive) {
            cache->status.alive(shardId, db.address());
        } else {
            cache->status.dead(shardId, db.address());
        }
    }
    return cache;
}

}  // namespace sharpei
