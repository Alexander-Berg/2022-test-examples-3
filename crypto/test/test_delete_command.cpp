#include "helpers.h"

#include <crypta/cm/services/common/changes/change_commands.h>
#include <crypta/cm/services/common/changes/delete_command.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TDeleteCommand) {
    using namespace NCrypta::NCm;

    const TString SHARDING_KEY = "key";
    const TId ID("type", "value");
    const TId ID_2("type-2", "value-2");
    const auto TIMESTAMP = TInstant::Seconds(1500000001);
    const auto TIMESTAMP_2 = TInstant::Seconds(1500000002);
    const TString REF_SERIALIZED_COMMAND = R"({"cmd":"delete","ext_id":"type:value","unixtime":1500000001,"sharding_key":"key"})";

    Y_UNIT_TEST(Constructor) {
        const TDeleteCommand cmd(SHARDING_KEY, ID, TIMESTAMP);

        UNIT_ASSERT_STRINGS_EQUAL(SHARDING_KEY, cmd.ShardingKey);
        UNIT_ASSERT_EQUAL(ID, cmd.ExtId);
        UNIT_ASSERT_EQUAL(TIMESTAMP, cmd.Timestamp);
    }

    Y_UNIT_TEST(Equality) {
        {
            TDeleteCommand cmd1;
            TDeleteCommand cmd2;

            AssertEqual(cmd1, cmd1);
            AssertEqual(cmd1, cmd2);
        }

        {
            const TDeleteCommand cmd1(SHARDING_KEY, ID, TIMESTAMP);
            const TDeleteCommand cmd2(SHARDING_KEY, ID, TIMESTAMP);
            const TDeleteCommand copy = cmd1;

            AssertEqual(cmd1, cmd1);
            AssertEqual(cmd1, cmd2);
            AssertEqual(cmd1, copy);
        }
    }

    Y_UNIT_TEST(Inequality) {
        {
            TDeleteCommand cmd1;
            TDeleteCommand cmd2;

            cmd1.ExtId = ID;
            cmd2.ExtId = ID_2;

            AssertUnequal(cmd1, cmd2);
        }

        {
            TDeleteCommand cmd1;
            TDeleteCommand cmd2;

            cmd1.Timestamp = TIMESTAMP;
            cmd2.Timestamp = TIMESTAMP_2;

            AssertUnequal(cmd1, cmd2);
        }
    }

    Y_UNIT_TEST(ToString) {
        UNIT_ASSERT_STRINGS_EQUAL(REF_SERIALIZED_COMMAND, TDeleteCommand(SHARDING_KEY, ID, TIMESTAMP).ToString());
    }

    Y_UNIT_TEST(FromJsonValue) {
        const auto& deserializedCommand = TDeleteCommand::FromJsonValue(NChangeCommands::ParseToJsonValue(REF_SERIALIZED_COMMAND));
        UNIT_ASSERT_STRINGS_EQUAL(SHARDING_KEY, deserializedCommand.ShardingKey);
        UNIT_ASSERT_EQUAL(ID, deserializedCommand.ExtId);
        UNIT_ASSERT_EQUAL(TIMESTAMP, deserializedCommand.Timestamp);
    }

    Y_UNIT_TEST(ToStringFromJsonValue) {
        const auto& refCommand = TDeleteCommand(SHARDING_KEY, ID, TIMESTAMP);
        const auto& resultCommand = TDeleteCommand::FromJsonValue(NChangeCommands::ParseToJsonValue(refCommand.ToString()));

        UNIT_ASSERT_EQUAL(refCommand, resultCommand);
    }

    Y_UNIT_TEST(ErrorsInDeserialization) {
        TVector<std::pair<TString, TString>> testCases = {
                {R"([])", "Command json must be a map"},

                {R"({})", "Field cmd must be a string"},
                {R"({"cmd": "delete"})", "Field ext_id must be a string"},
                {R"({"cmd": "delete", "ext_id": "type:value"})", "Field unixtime must be an integer"},
                {R"({"cmd": "delete", "ext_id": "type:value", "expire_ts": 1500000000, "ttl": 86400, "unixtime": 10})", "Field sharding_key must be a string"},

                {R"({"cmd": "not-delete", "ext_id": "type:value", "expire_ts": 1500000000, "ttl": 86400, "unixtime": 10, "sharding_key": "key"})", "Field 'cmd' must be equal to 'delete'"},

                {R"({"cmd": "delete", "ext_id": "", "unixtime": 1500000000, "sharding_key": "key"})", "Could not split id '' by delimiter ':'"},
        };

        for (const auto& [json, errorMsg] : testCases) {
            const auto& jsonValue = NChangeCommands::ParseToJsonValue(json);
            UNIT_ASSERT_EXCEPTION_CONTAINS_C(TDeleteCommand::FromJsonValue(jsonValue), yexception, errorMsg, json);
        }
    }
}
