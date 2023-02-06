#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <library/cpp/testing/gtest_boost_extensions/extensions.h>
#include <yamail/data/serialization/json_writer.h>
#include <internal/cache/cache.h>
#include <internal/reflection/shard.h>

namespace {

template <class T>
std::string serialize(const T& value) {
    return yamail::data::serialization::JsonWriter<T>(value).result();
}

} // namespace

namespace sharpei {

static inline bool operator ==(const Shard& lhs, const Shard& rhs) {
    return lhs.id == rhs.id && lhs.name == rhs.name && lhs.databases == rhs.databases;
}

static inline std::ostream& operator <<(std::ostream& stream, Error value) {
    return stream << boost::system::error_code(value).message();
}

} // namespace sharpei

namespace mail_errors {

static inline std::ostream& operator <<(std::ostream& stream, const error_code& value) {
    return stream << "mail_errors::error_code(" << static_cast<sharpei::Error>(value.value())
        << ", \"" << value.what() << "\")";
}

} // namespace mail_errors

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::cache;

using Database = Shard::Database;
using Role = Database::Role;
using OptRole = RoleCache::OptRole;
using OptState = StateCache::OptState;
using State = StateCache::State;
using OptShardMaster = Cache::OptShardMaster;

TEST(CacheTest, emptyCache_serialize_returnEmptyJson) {
    Cache cache(1, 1);
    ASSERT_EQ("{}", serialize(makeShardsInfoOldFormat(cache, &Availability::smooth)));
}

TEST(CacheTest, fullCache_serialize_returnCorrectJson) {
    Cache cache(1, 1);
    cache.role.update(1, {
        {{"pg1.yandex.ru", 1000, "maildb", "sas"}, OptRole(Role::Master)},
        {{"pg2.yandex.ru", 2000, "maildb", "sas"}, OptRole(Role::Replica)},
        {{"pg3.yandex.ru", 3000, "maildb", "sas"}, OptRole(Role::Replica)},
    });
    cache.state.update(1, {
        {{"pg1.yandex.ru", 1000, "maildb", "sas"}, OptState(State{0})},
        {{"pg2.yandex.ru", 2000, "maildb", "sas"}, OptState(State{5})},
        {{"pg3.yandex.ru", 3000, "maildb", "sas"}, OptState(State{100})},
    });
    cache.status.alive(1, {"pg1.yandex.ru", 1000, "maildb", "sas"});
    cache.status.alive(1, {"pg2.yandex.ru", 2000, "maildb", "sas"});
    cache.status.alive(1, {"pg3.yandex.ru", 3000, "maildb", "sas"});
    cache.role.update(2, {
        {{"pg4.yandex.ru", 4000, "maildb", "sas"}, OptRole(Role::Master)},
        {{"pg5.yandex.ru", 5000, "maildb1", "sas"}, OptRole(Role::Replica)},
        {{"pg6.yandex.ru", 6000, "maildb2", "sas"}, OptRole(Role::Replica)},
    });
    cache.state.update(2, {
        {{"pg4.yandex.ru", 4000, "maildb", "sas"}, OptState(State{0})},
        {{"pg5.yandex.ru", 5000, "maildb1", "sas"}, OptState(State{50})},
        {{"pg6.yandex.ru", 6000, "maildb2", "sas"}, OptState(State{1})},
    });
    cache.status.alive(2, {"pg4.yandex.ru", 4000, "maildb", "sas"});
    cache.status.alive(2, {"pg5.yandex.ru", 5000, "maildb1", "sas"});
    cache.status.alive(2, {"pg6.yandex.ru", 6000, "maildb2", "sas"});
    const std::string json = "{"
        "\"2\":["
        "{\"address\":{\"host\":\"pg4.yandex.ru\",\"port\":4000,\"dbname\":\"maildb\",\"dataCenter\":\"sas\"},\"role\":\"master\",\"status\":\"alive\",\"state\":{\"lag\":0}},"
        "{\"address\":{\"host\":\"pg5.yandex.ru\",\"port\":5000,\"dbname\":\"maildb1\",\"dataCenter\":\"sas\"},\"role\":\"replica\",\"status\":\"alive\",\"state\":{\"lag\":50}},"
        "{\"address\":{\"host\":\"pg6.yandex.ru\",\"port\":6000,\"dbname\":\"maildb2\",\"dataCenter\":\"sas\"},\"role\":\"replica\",\"status\":\"alive\",\"state\":{\"lag\":1}}"
        "],"
        "\"1\":["
        "{\"address\":{\"host\":\"pg1.yandex.ru\",\"port\":1000,\"dbname\":\"maildb\",\"dataCenter\":\"sas\"},\"role\":\"master\",\"status\":\"alive\",\"state\":{\"lag\":0}},"
        "{\"address\":{\"host\":\"pg2.yandex.ru\",\"port\":2000,\"dbname\":\"maildb\",\"dataCenter\":\"sas\"},\"role\":\"replica\",\"status\":\"alive\",\"state\":{\"lag\":5}},"
        "{\"address\":{\"host\":\"pg3.yandex.ru\",\"port\":3000,\"dbname\":\"maildb\",\"dataCenter\":\"sas\"},\"role\":\"replica\",\"status\":\"alive\",\"state\":{\"lag\":100}}"
        "]"
    "}";
    ASSERT_EQ(json, serialize(makeShardsInfoOldFormat(cache, &Availability::smooth)));
}

TEST(CacheTest, get_alive_shard_master_when_cache_is_empty_should_return_none) {
    Cache cache(1, 1);
    const Shard::Id shardId(1);
    EXPECT_EQ(cache.getAliveShardMaster(shardId), boost::none);
}

TEST(CacheTest, get_alive_shard_master_when_no_master_in_shard_should_return_none) {
    Cache cache(1, 1);
    const Shard::Id shardId(1);
    cache.role.update(shardId, {{{"pg1.yandex.ru", 1000, "maildb", "sas"}, OptRole(Role::Replica)}});
    EXPECT_EQ(cache.getAliveShardMaster(shardId), boost::none);
}

TEST(CacheTest, get_alive_shard_master_when_no_master_host_status_should_return_none) {
    Cache cache(1, 1);
    const Shard::Id shardId(1);
    const Database::Address address {"pg1.yandex.ru", 1000, "maildb", "sas"};
    cache.role.update(shardId, {{address, OptRole(Role::Master)}});
    EXPECT_EQ(cache.getAliveShardMaster(shardId), boost::none);
}

TEST(CacheTest, get_alive_shard_master_when_no_alive_master_should_return_none) {
    Cache cache(1, 1);
    const Shard::Id shardId(1);
    const Database::Address address {"pg1.yandex.ru", 1000, "maildb", "sas"};
    cache.role.update(shardId, {{address, OptRole(Role::Master)}});
    cache.status.dead(shardId, address);
    EXPECT_EQ(cache.getAliveShardMaster(shardId), boost::none);
}

TEST(CacheTest, get_alive_shard_master_when_has_alive_master_should_return_address) {
    Cache cache(1, 1);
    const Shard::Id shardId(1);
    const Database::Address address {"pg1.yandex.ru", 1000, "maildb", "sas"};
    cache.role.update(shardId, {{address, OptRole(Role::Master)}});
    cache.status.alive(shardId, address);
    EXPECT_EQ(cache.getAliveShardMaster(shardId), address);
}

TEST(CacheTest, get_shard_without_cached_shard_name_should_return_error) {
    Cache cache(1, 1);
    ExplainedError error;
    boost::optional<Shard> shard;
    std::tie(error, shard) = cache.getShard(0);
    EXPECT_EQ(error, Error::noCachedShardName);
    EXPECT_FALSE(shard.is_initialized());
}

TEST(CacheTest, get_shard_without_cached_databases_roles_should_return_error) {
    Cache cache(1, 1);
    const Shard::Id shardId = 1;
    cache.shardName.update(shardId, "shard");
    ExplainedError error;
    boost::optional<Shard> shard;
    std::tie(error, shard) = cache.getShard(shardId);
    EXPECT_EQ(error, Error::noCachedShardDatabasesRoles);
    EXPECT_FALSE(shard.is_initialized());
}

TEST(CacheTest, get_shard_without_cached_databases_statuses_should_return_error) {
    Cache cache(1, 1);
    const Shard::Id shardId = 1;
    cache.shardName.update(shardId, "shard");
    cache.role.update(shardId, {{{"pg1.yandex.ru", 1000, "maildb", "sas"}, OptRole(Role::Master)}});
    ExplainedError error;
    boost::optional<Shard> shard;
    std::tie(error, shard) = cache.getShard(shardId);
    EXPECT_EQ(error, Error::noCachedShardDatabases);
    EXPECT_FALSE(shard.is_initialized());
}

TEST(CacheTest, get_shard_without_cached_databases_states_should_return_error) {
    Cache cache(1, 1);
    const Shard::Id shardId = 1;
    const Shard::Database::Address address {"pg1.yandex.ru", 1000, "maildb", "sas"};
    cache.shardName.update(shardId, "shard");
    cache.role.update(shardId, {{address, OptRole(Role::Master)}});
    cache.status.alive(shardId, address);
    ExplainedError error;
    boost::optional<Shard> shard;
    std::tie(error, shard) = cache.getShard(shardId);
    EXPECT_EQ(error, Error::noCachedShardDatabases);
    EXPECT_FALSE(shard.is_initialized());
}

TEST(CacheTest, get_shard_with_name_hosts_and_databases_should_return_shard) {
    using Status = Shard::Database::Status;
    using OptState = StateCache::OptState;
    Cache cache(1, 1);
    const Shard::Id shardId = 1;
    const std::string shardName = "shard";
    const Shard::Database::Address address {"pg1.yandex.ru", 1000, "maildb", "sas"};
    const auto role = Role::Master;
    const State state {0};
    cache.shardName.update(shardId, shardName);
    cache.role.update(shardId, {{address, OptRole(role)}});
    cache.status.alive(shardId, address);
    cache.state.update(shardId, {{address, OptState(state)}});
    ExplainedError error;
    boost::optional<Shard> shard;
    std::tie(error, shard) = cache.getShard(shardId);
    EXPECT_EQ(error, Error::ok);
    EXPECT_EQ(shard, boost::optional<Shard>(Shard(shardId, shardName, {Database(address, role, Status::Alive, state)})));
}

} // namespace
