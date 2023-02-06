#include <crypta/cm/services/common/changes/change_commands.h>
#include <crypta/cm/services/common/changes/upload_command.h>
#include <crypta/cm/services/common/data/id_utils.h>
#include <crypta/cm/services/common/data/random_id_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <utility>

Y_UNIT_TEST_SUITE(TUploadCommand) {
    using namespace NCrypta::NCm;

    const TString SHARDING_KEY = "key";
    const TString EXT_NS = "ext_ns";
    const TString REF_SERIALIZED_COMMAND = R"({"cmd":"upload","match":"ChAKBmV4dF9ucxIGMTAwNTAwElgKKgoVCgdpY29va2llEgoxNjAwMDAwMDAwEICg+PoFGgkKBHN5bnQSATAgAQoqChcKCXlhbmRleHVpZBIKMTUwMDAwMDAwMBCA3qDLBRoJCgRzeW50EgExGIDiz6oGIICjBQ==","unixtime":1700000100,"sharding_key":"key"})";
    const TMatch REF_MATCH(
        TId("ext_ns", "100500"),
        TMatch::TMatchedIds{
            {YANDEXUID_TYPE, TMatchedId(TId(YANDEXUID_TYPE, "1500000000"), TInstant::Seconds(1500000000), 0, {{"synt", "1"}})},
            {ICOOKIE_TYPE, TMatchedId(TId(ICOOKIE_TYPE, "1600000000"), TInstant::Seconds(1600000000), 1, {{"synt", "0"}})}
        },
       TInstant::Seconds(1700000000),
       TDuration::Seconds(86400)
    );
    const TInstant REF_TIMESTAMP = TInstant::Seconds(1700000100);

    Y_UNIT_TEST(Equality) {
        const auto& match1 = CreateRandomMatch(EXT_NS, "0", "1");
        const auto& match2 = CreateRandomMatch(EXT_NS, "1", "0");

        const TUploadCommand uploadCommand1(SHARDING_KEY, match1);
        const TUploadCommand uploadCommand1Copy(SHARDING_KEY, match1);
        const TUploadCommand uploadCommand2(SHARDING_KEY, match2);

        UNIT_ASSERT_EQUAL(uploadCommand1, uploadCommand1);

        UNIT_ASSERT_EQUAL(uploadCommand1, uploadCommand1Copy);
        UNIT_ASSERT_EQUAL(uploadCommand1Copy, uploadCommand1);

        UNIT_ASSERT_UNEQUAL(uploadCommand1, uploadCommand2);
        UNIT_ASSERT_UNEQUAL(uploadCommand2, uploadCommand1);
    }

    Y_UNIT_TEST(ToString) {
        const auto& serialized = TUploadCommand::ToString(TUploadCommand(SHARDING_KEY, REF_MATCH, REF_TIMESTAMP));
        UNIT_ASSERT_STRINGS_EQUAL(REF_SERIALIZED_COMMAND, serialized);
    }

    Y_UNIT_TEST(FromJsonValue) {
        const auto& deserializedCommand = TUploadCommand::FromJsonValue(NChangeCommands::ParseToJsonValue(REF_SERIALIZED_COMMAND));
        UNIT_ASSERT_STRINGS_EQUAL(SHARDING_KEY, deserializedCommand.ShardingKey);
        UNIT_ASSERT_EQUAL(REF_MATCH, deserializedCommand.IncomingMatch);
        UNIT_ASSERT_EQUAL(REF_TIMESTAMP, deserializedCommand.Timestamp);
    }

    Y_UNIT_TEST(ToStringFromJsonValue) {
        const auto& refCommand = TUploadCommand(SHARDING_KEY, REF_MATCH, REF_TIMESTAMP);
        const auto& resultCommand = TUploadCommand::FromJsonValue(NChangeCommands::ParseToJsonValue(TUploadCommand::ToString(refCommand)));

        UNIT_ASSERT_EQUAL(refCommand, resultCommand);
    }

    Y_UNIT_TEST(ErrorsInDeserialization) {
        TVector<std::pair<TString, TString>> testCases = {
            {R"([])", "Command json must be a map"},

            {R"({})", "Field cmd must be a string"},
            {R"({"cmd": "upload"})", "Field match must be a string"},
            {R"({"cmd": "upload", "match": "CgASAA=="})", "Field unixtime must be an integer"},
            {R"({"cmd": "upload", "match": "CgASAA==", "unixtime": 10})", "Field sharding_key must be a string"},

            {R"({"cmd": "not-upload", "match": "CgASAA==", "unixtime": 10, "sharding_key": "key"})", "Field 'cmd' must be equal to 'upload'"},

            {R"({"cmd": "upload", "match": "", "unixtime": 10, "sharding_key": "key"})", "Serialized ext_id match is empty"},
            {R"({"cmd": "upload", "match": "CgASAA==", "unixtime": 10, "sharding_key": "key"})", "Command must contain a valid match"},
        };

        for (const auto& [json, errorMsg] : testCases) {
            const auto& jsonValue = NChangeCommands::ParseToJsonValue(json);
            UNIT_ASSERT_EXCEPTION_CONTAINS_C(TUploadCommand::FromJsonValue(jsonValue), yexception, errorMsg, json);
        }
    }
}
