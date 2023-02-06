#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <library/cpp/testing/gtest_boost_extensions/extensions.h>
#include <yamail/data/serialization/json_writer.h>
#include <internal/cache/role.h>
#include <internal/reflection/shard.h>

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::cache;

using Database = Shard::Database;
using Address = Database::Address;
using Role = Database::Role;
using OptRole = RoleCache::OptRole;
using Hosts = RoleCache::Hosts;
using HostsWithOptRole = RoleCache::HostsWithOptRole;
using HostsSet = RoleCache::HostsSet;

TEST(RoleCacheTest, get_from_empty_should_return_empty) {
    RoleCache cache;
    const auto result = cache.get(1);
    EXPECT_EQ(result, Hosts());
}

TEST(RoleCacheTest, update_then_get_should_return_equal) {
    RoleCache cache;
    const Address pg1 {"pg1.yandex.ru", 5432, "maildb", "sas"};
    const Address pg2 {"pg2.yandex.ru", 5432, "maildb", "sas"};
    const Address pg3 {"pg3.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {{pg1, OptRole(Role::Master)},
                     {pg2, OptRole(Role::Replica)}, {pg3, OptRole(Role::Replica)}});
    const Hosts expected = {{pg1, Role::Master},
                            {pg2, Role::Replica}, {pg3, Role::Replica}};
    const auto result = cache.get(1);
    EXPECT_EQ(result, expected);
}

TEST(RoleCacheTest, update_and_erase_then_get_should_return_empty) {
    RoleCache cache;
    cache.update(1, {{{"pg1.yandex.ru", 5432, "maildb", "sas"}, OptRole(Role::Master)},
                     {{"pg2.yandex.ru", 5432, "maildb", "sas"}, OptRole(Role::Replica)},
                     {{"pg3.yandex.ru.ru", 5432, "maildb", "sas"}, OptRole(Role::Replica)}});
    cache.erase(1);
    const auto result = cache.get(1);
    EXPECT_EQ(result, Hosts());
}

TEST(RoleCacheTest, update_change_role_then_get_should_return_last) {
    RoleCache cache;
    const Address localhost {"localhost", 5432, "maildb", "sas"};
    cache.update(1, {{localhost, OptRole(Role::Master)}});
    cache.update(1, {{localhost, OptRole(Role::Replica)}});
    const Hosts expected = {{localhost, Role::Replica}};
    const auto result = cache.get(1);
    EXPECT_EQ(result, expected);
}

TEST(RoleCacheTest, update_initialized_by_uninitialized_then_get_role_should_return_initialized) {
    RoleCache cache;
    const Address address {"pg1.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {{address, OptRole(Role::Master)}});
    cache.update(1, {{address, OptRole()}});
    const auto role = cache.getRole(1, address);
    EXPECT_EQ(role, OptRole(Role::Master));
}

TEST(RoleCacheTest, update_uninitialized_then_get_role_should_return_replica) {
    RoleCache cache;
    const Address address {"pg1.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {{address, OptRole()}});
    const auto role = cache.getRole(1, address);
    EXPECT_EQ(role, OptRole(Role::Replica));
}

TEST(RoleCacheTest, update_change_master_then_get_role_should_return_first_master_as_replica) {
    RoleCache cache;
    const Address pg1 {"pg1.yandex.ru", 5432, "maildb", "sas"};
    const Address pg2 {"pg2.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {{pg1, OptRole(Role::Master)}, {pg2, OptRole(Role::Replica)}});
    cache.update(1, {{pg1, OptRole()}, {pg2, OptRole(Role::Master)}});
    const auto pg1role = cache.getRole(1, pg1);
    EXPECT_EQ(pg1role, OptRole(Role::Replica));
    const auto pg2role = cache.getRole(1, pg2);
    EXPECT_EQ(pg2role, OptRole(Role::Master));
}

TEST(RoleCacheTest, update_with_more_than_one_master_then_get_should_return_equal) {
    RoleCache cache;
    const Address pg1 {"pg1.yandex.ru", 5432, "maildb", "sas"};
    const Address pg2 {"pg2.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {{pg1, OptRole(Role::Master)}, {pg2, OptRole(Role::Master)}});
    const Hosts expected = {{pg1, Role::Master}, {pg2, Role::Master}};
    const auto result = cache.get(1);
    EXPECT_EQ(result, expected);
}

TEST(RoleCacheTest, get_by_role_from_empty_should_return_empty) {
    RoleCache cache;
    const auto result = cache.get(1, Role::Master);
    EXPECT_EQ(HostsSet(), result);
}

TEST(RoleCacheTest, update_then_get_by_role_should_return_equal) {
    RoleCache cache;
    const Address pg1 {"pg1.yandex.ru", 5432, "maildb", "sas"};
    const Address pg2 {"pg2.yandex.ru", 5432, "maildb", "sas"};
    const Address pg3 {"pg3.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {
        {pg1, OptRole(Role::Master)},
        {pg2, OptRole(Role::Replica)},
        {pg3, OptRole(Role::Replica)}
    });
    const HostsSet expected = {pg2, pg3};
    const auto result = cache.get(1, Role::Replica);
    EXPECT_EQ(expected, result);
}

TEST(RoleCacheTest, update_with_no_master_then_get_by_role_master_should_return_empty) {
    RoleCache cache;
    const Address pg2 {"pg2.yandex.ru", 5432, "maildb", "sas"};
    const Address pg3 {"pg3.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {
        {pg2, OptRole(Role::Replica)},
        {pg3, OptRole(Role::Replica)}
    });
    const auto result = cache.get(1, Role::Master);
    EXPECT_EQ(HostsSet(), result);
}

TEST(RoleCacheTest, update_with_two_masters_then_change_one_to_replica_then_get_should_return_master_and_replica) {
    RoleCache cache;
    const Address pg1 {"pg1.yandex.ru", 5432, "maildb", "sas"};
    const Address pg2 {"pg2.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {{pg1, OptRole(Role::Master)}, {pg2, OptRole(Role::Master)}});
    cache.update(1, {{pg1, OptRole(Role::Master)}, {pg2, OptRole(Role::Replica)}});
    const Hosts expected = {{pg1, Role::Master}, {pg2, Role::Replica}};
    const auto result = cache.get(1);
    EXPECT_EQ(result, expected);
}

TEST(RoleCacheTest, update_with_two_masters_then_update_one_then_get_should_return_master_and_replica) {
    RoleCache cache;
    const Address pg1 {"pg1.yandex.ru", 5432, "maildb", "sas"};
    const Address pg2 {"pg2.yandex.ru", 5432, "maildb", "sas"};
    cache.update(1, {{pg1, OptRole(Role::Master)}, {pg2, OptRole(Role::Master)}});
    cache.update(1, {{pg1, OptRole(Role::Master)}, {pg2, OptRole()}});
    const Hosts expected = {{pg1, Role::Master}, {pg2, Role::Replica}};
    const auto result = cache.get(1);
    EXPECT_EQ(result, expected);
}

TEST(RoleCacheTest, get_role_when_no_shard_should_return_empty) {
    const RoleCache cache;
    EXPECT_EQ(cache.getRole(1, Address {"localhost", 5432, "maildb", "sas"}), boost::none);
}

TEST(RoleCacheTest, get_role_when_no_host_should_return_empty) {
    RoleCache cache;
    cache.update(1, {{Address {"localhost", 5432, "maildb", "sas"}, OptRole(Role::Master)}});
    EXPECT_EQ(cache.getRole(1, Address {"remote", 5432, "maildb", "sas"}), boost::none);
}

} // namespace
