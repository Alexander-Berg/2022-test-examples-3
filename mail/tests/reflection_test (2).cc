#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yamail/data/serialization/json_writer.h>
#include <internal/reflection/shard.h>

namespace {

using namespace testing;
using Database = sharpei::Shard::Database;
using Role = Database::Role;
using Status = Database::Status;
using Mode = sharpei::Mode;

template <class T>
std::string serialize(const T& value) {
    return yamail::data::serialization::toJson(value);
}

TEST(ReflectionTest, makeShardWithoutRolesOrderedByState_returnsShardWithAddressesInCorrectOrder) {
    const auto shardOriginal = sharpei::Shard(1, "shard1", {
            {{"pg2.yandex.ru", 2000, "maildb", "sas"}, Role::Replica, Status::Alive, {50}},
            {{"pg3.yandex.ru", 3000, "maildb", "sas"}, Role::Replica, Status::Alive, {1}}
    });

    const auto shardOrdered = sharpei::reflection::makeShardWithoutRolesOrderedByState(shardOriginal);

    const std::string expected =
        R"json({"id":1,"name":"shard1","addrs":[)json"
            R"json({"host":"pg3.yandex.ru","port":3000,"dbname":"maildb","dataCenter":"sas"},)json"
            R"json({"host":"pg2.yandex.ru","port":2000,"dbname":"maildb","dataCenter":"sas"})json"
        "]}";

    ASSERT_EQ(serialize(shardOrdered), expected);
}

TEST(ReflectionTest, makeShardOrderedByState_returnsShardWithDatabasesInCorrectOrder) {
    const auto shardOriginal = sharpei::Shard(1, "shard1", {
            {{"pg2.yandex.ru", 2000, "maildb", "sas"}, Role::Replica, Status::Alive, {50}},
            {{"pg3.yandex.ru", 3000, "maildb", "sas"}, Role::Replica, Status::Alive, {1}}
    });

    const auto shardOrdered = sharpei::reflection::makeShardOrderedByState(shardOriginal);

    const std::string expected =
        R"json({"id":1,"name":"shard1","databases":[)json"
            R"json({"address":{"host":"pg3.yandex.ru","port":3000,"dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":1}},)json"
            R"json({"address":{"host":"pg2.yandex.ru","port":2000,"dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":50}})json"
        "]}";

    ASSERT_EQ(serialize(shardOrdered), expected);
}

TEST(ReflectionTest, makeShardOrderedByModeAndState_forModeReadWrite_returnsShardWithDatabasesInCorrectOrder) {
    const auto shardOriginal = sharpei::Shard(1, "shard1", {
            {{"pg2.yandex.ru", 2000, "maildb", "sas"}, Role::Replica, Status::Alive, {50}},
            {{"pg1.yandex.ru", 1000, "maildb", "sas"}, Role::Master, Status::Alive, {0}},
            {{"pg3.yandex.ru", 3000, "maildb", "sas"}, Role::Replica, Status::Alive, {1}}
    });

    const auto shardOrdered = sharpei::reflection::makeShardOrderedByModeAndState(shardOriginal, Mode::ReadWrite);

    const std::string expected =
        R"json({"id":1,"name":"shard1","databases":[)json"
            R"json({"address":{"host":"pg3.yandex.ru","port":3000,"dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":1}},)json"
            R"json({"address":{"host":"pg2.yandex.ru","port":2000,"dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":50}},)json"
            R"json({"address":{"host":"pg1.yandex.ru","port":1000,"dbname":"maildb","dataCenter":"sas"},"role":"master","status":"alive","state":{"lag":0}})json"
        "]}";

    ASSERT_EQ(serialize(shardOrdered), expected);
}

TEST(ReflectionTest, makeShardOrderedByModeAndState_forModeWriteRead_returnsShardWithDatabasesInCorrectOrder) {
    const auto shardOriginal = sharpei::Shard(1, "shard1", {
            {{"pg2.yandex.ru", 2000, "maildb", "sas"}, Role::Replica, Status::Alive, {50}},
            {{"pg1.yandex.ru", 1000, "maildb", "sas"}, Role::Master, Status::Alive, {0}},
            {{"pg3.yandex.ru", 3000, "maildb", "sas"}, Role::Replica, Status::Alive, {1}}
    });

    const auto shardOrdered = sharpei::reflection::makeShardOrderedByModeAndState(shardOriginal, Mode::WriteRead);

    const std::string expected =
        R"json({"id":1,"name":"shard1","databases":[)json"
            R"json({"address":{"host":"pg1.yandex.ru","port":1000,"dbname":"maildb","dataCenter":"sas"},"role":"master","status":"alive","state":{"lag":0}},)json"
            R"json({"address":{"host":"pg3.yandex.ru","port":3000,"dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":1}},)json"
            R"json({"address":{"host":"pg2.yandex.ru","port":2000,"dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":50}})json"
        "]}";

    ASSERT_EQ(serialize(shardOrdered), expected);
}

TEST(ReflectionTest, makeShardsInfoNewFormat_resultShouldBeOrdered) {
    const sharpei::ShardsInfoNewFormat shardsInfo = {
        {1, sharpei::Shard(1, "shard1", {
            {{"pg2.yandex.ru", 2000, "maildb", "sas"}, Role::Replica, Status::Alive, {50}},
            {{"pg1.yandex.ru", 1000, "maildb", "sas"}, Role::Master, Status::Alive, {0}},
            {{"pg3.yandex.ru", 3000, "maildb", "sas"}, Role::Replica, Status::Alive, {1}}
        })},
        {2, sharpei::Shard(2, "shard2", {
            {{"pg6.yandex.ru", 6000, "maildb", "sas"}, Role::Replica, Status::Alive, {20}},
            {{"pg4.yandex.ru", 4000, "maildb", "sas"}, Role::Master, Status::Alive, {0}},
            {{"pg5.yandex.ru", 5000, "maildb", "sas"}, Role::Replica, Status::Alive, {10}}
        })}
    };

    const auto shardsInfoOrdered = sharpei::reflection::makeShardsInfoNewFormat<sharpei::reflection::ShardWithStringIdAndPort>(shardsInfo);

    const std::string expected =
        "{"
            "\"1\":{"
                "\"id\":\"1\",\"name\":\"shard1\",\"databases\":["
                    R"json({"address":{"host":"pg1.yandex.ru","port":"1000","dbname":"maildb","dataCenter":"sas"},"role":"master","status":"alive","state":{"lag":0}},)json"
                    R"json({"address":{"host":"pg3.yandex.ru","port":"3000","dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":1}},)json"
                    R"json({"address":{"host":"pg2.yandex.ru","port":"2000","dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":50}})json"
                "]"
            "},"
            "\"2\":{"
                "\"id\":\"2\",\"name\":\"shard2\",\"databases\":["
                    R"json({"address":{"host":"pg4.yandex.ru","port":"4000","dbname":"maildb","dataCenter":"sas"},"role":"master","status":"alive","state":{"lag":0}},)json"
                    R"json({"address":{"host":"pg5.yandex.ru","port":"5000","dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":10}},)json"
                    R"json({"address":{"host":"pg6.yandex.ru","port":"6000","dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":20}})json"
                "]"
            "}"
        "}";

    ASSERT_EQ(serialize(shardsInfoOrdered), expected);
}

TEST(ReflectionTest, makeShardsInfoOldFormat_resultShouldBeOrdered) {
    const sharpei::ShardsInfoOldFormat shardsInfo = {
        {1, {
            {{"pg2.yandex.ru", 2000, "maildb", "sas"}, Role::Replica, Status::Alive, {50}},
            {{"pg1.yandex.ru", 1000, "maildb", "sas"}, Role::Master, Status::Alive, {0}},
            {{"pg3.yandex.ru", 3000, "maildb", "sas"}, Role::Replica, Status::Alive, {1}}
        }},
        {2, {
            {{"pg6.yandex.ru", 6000, "maildb", "sas"}, Role::Replica, Status::Alive, {20}},
            {{"pg4.yandex.ru", 4000, "maildb", "sas"}, Role::Master, Status::Alive, {0}},
            {{"pg5.yandex.ru", 5000, "maildb", "sas"}, Role::Replica, Status::Alive, {10}}
        }}
    };

    const auto shardsInfoOrdered = sharpei::reflection::makeShardsInfoOldFormat(shardsInfo);

    const std::string expected =
        "{"
            "\"1\":["
                R"json({"address":{"host":"pg1.yandex.ru","port":"1000","dbname":"maildb","dataCenter":"sas"},"role":"master","status":"alive","state":{"lag":0}},)json"
                R"json({"address":{"host":"pg3.yandex.ru","port":"3000","dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":1}},)json"
                R"json({"address":{"host":"pg2.yandex.ru","port":"2000","dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":50}})json"
            "],"
            "\"2\":["
                R"json({"address":{"host":"pg4.yandex.ru","port":"4000","dbname":"maildb","dataCenter":"sas"},"role":"master","status":"alive","state":{"lag":0}},)json"
                R"json({"address":{"host":"pg5.yandex.ru","port":"5000","dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":10}},)json"
                R"json({"address":{"host":"pg6.yandex.ru","port":"6000","dbname":"maildb","dataCenter":"sas"},"role":"replica","status":"alive","state":{"lag":20}})json"
            "]"
        "}";

    ASSERT_EQ(serialize(shardsInfoOrdered), expected);
}

} // namespace
