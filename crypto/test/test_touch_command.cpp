#include "helpers.h"

#include <crypta/cm/services/common/changes/change_commands.h>
#include <crypta/cm/services/common/changes/touch_command.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TTouchCommand) {
    using namespace NCrypta::NCm;

    const TString SHARDING_KEY = "key";
    const TId EXT_ID("type", "value");
    const TId EXT_ID_2("type-2", "value-2");
    const auto TOUCH_TS = TInstant::Seconds(1500000000);
    const auto TIMESTAMP = TInstant::Seconds(1500000001);
    const TString REF_SERIALIZED_COMMAND = R"({"cmd":"touch","ext_id":"type:value","touch_ts":1500000000,"unixtime":1500000001,"sharding_key":"key"})";

    Y_UNIT_TEST(Constructor) {
        const TTouchCommand cmd(SHARDING_KEY, EXT_ID, TOUCH_TS, TIMESTAMP);

        UNIT_ASSERT_STRINGS_EQUAL(SHARDING_KEY, cmd.ShardingKey);
        UNIT_ASSERT_EQUAL(EXT_ID, cmd.ExtId);
        UNIT_ASSERT_EQUAL(TOUCH_TS, cmd.TouchTs);
        UNIT_ASSERT_EQUAL(TIMESTAMP, cmd.Timestamp);
    }

    Y_UNIT_TEST(Equality) {
        {
            TTouchCommand cmd1;
            TTouchCommand cmd2;

            AssertEqual(cmd1, cmd1);
            AssertEqual(cmd1, cmd2);
        }

        {
            const TTouchCommand cmd1(SHARDING_KEY, EXT_ID, TOUCH_TS, TIMESTAMP);
            const TTouchCommand cmd2(SHARDING_KEY, EXT_ID, TOUCH_TS, TIMESTAMP);
            const TTouchCommand copy = cmd1;

            AssertEqual(cmd1, cmd1);
            AssertEqual(cmd1, cmd2);
            AssertEqual(cmd1, copy);
        }
    }

    Y_UNIT_TEST(Inequality) {
        {
            TTouchCommand cmd1;
            TTouchCommand cmd2;

            cmd1.ExtId = EXT_ID;
            cmd2.ExtId = EXT_ID_2;

            AssertUnequal(cmd1, cmd2);
        }

        {
            TTouchCommand cmd1;
            TTouchCommand cmd2;

            cmd1.TouchTs = TInstant::Seconds(1);
            cmd2.TouchTs = TInstant::Seconds(2);

            AssertUnequal(cmd1, cmd2);
        }

        {
            TTouchCommand cmd1;
            TTouchCommand cmd2;

            cmd1.Timestamp = TInstant::Seconds(1);
            cmd2.Timestamp = TInstant::Seconds(2);

            AssertUnequal(cmd1, cmd2);
        }
    }

    Y_UNIT_TEST(ToString) {
        UNIT_ASSERT_STRINGS_EQUAL(REF_SERIALIZED_COMMAND, TTouchCommand::ToString(TTouchCommand(SHARDING_KEY, EXT_ID, TOUCH_TS, TIMESTAMP)));
    }

    Y_UNIT_TEST(FromJsonValue) {
        const auto& deserializedCommand = TTouchCommand::FromJsonValue(NChangeCommands::ParseToJsonValue(REF_SERIALIZED_COMMAND));
        UNIT_ASSERT_STRINGS_EQUAL(SHARDING_KEY, deserializedCommand.ShardingKey);
        UNIT_ASSERT_EQUAL(EXT_ID, deserializedCommand.ExtId);
        UNIT_ASSERT_EQUAL(TOUCH_TS, deserializedCommand.TouchTs);
        UNIT_ASSERT_EQUAL(TIMESTAMP, deserializedCommand.Timestamp);
    }

    Y_UNIT_TEST(ToStringFromJsonValue) {
        const auto& refCommand = TTouchCommand(SHARDING_KEY, EXT_ID, TOUCH_TS, TIMESTAMP);
        const auto& resultCommand = TTouchCommand::FromJsonValue(NChangeCommands::ParseToJsonValue(TTouchCommand::ToString(refCommand)));

        UNIT_ASSERT_EQUAL(refCommand, resultCommand);
    }

    Y_UNIT_TEST(ErrorsInDeserialization) {
        TVector<std::pair<TString, TString>> testCases = {
                {R"([])", "Command json must be a map"},

                {R"({})", "Field cmd must be a string"},
                {R"({"cmd": "touch"})", "Field ext_id must be a string"},
                {R"({"cmd": "touch", "ext_id": "type:value"})", "Field touch_ts must be an integer"},
                {R"({"cmd": "touch", "ext_id": "type:value", "touch_ts": 1500000000})", "Field unixtime must be an integer"},
                {R"({"cmd": "touch", "ext_id": "type:value", "touch_ts": 1500000000, "unixtime": 10})", "Field sharding_key must be a string"},

                {R"({"cmd": "not-touch", "ext_id": "type:value", "touch_ts": 1500000000, "unixtime": 10, "sharding_key": "key"})", "Field 'cmd' must be equal to 'touch'"},

                {R"({"cmd": "touch", "ext_id": "", "touch_ts": 1500000000, "unixtime": 10, "sharding_key": "key"})", "Could not split id '' by delimiter ':'"},
        };

        for (const auto& [json, errorMsg] : testCases) {
            const auto& jsonValue = NChangeCommands::ParseToJsonValue(json);
            UNIT_ASSERT_EXCEPTION_CONTAINS_C(TTouchCommand::FromJsonValue(jsonValue), yexception, errorMsg, json);
        }
    }
}
