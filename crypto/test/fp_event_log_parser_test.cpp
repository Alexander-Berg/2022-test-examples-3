#include <crypta/ext_fp/common/log_parsers/fp_event_log_parser.h>
#include <crypta/lib/native/ext_fp/constants.h>

#include <library/cpp/json/common/defs.h>
#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NFpEventLogParser) {
    using namespace NCrypta::NExtFp;

    Y_UNIT_TEST(ParseLine) {
        const TString logLine = R"(
            {
                "duid": 17000000001000,
                "hit_log_id": 100500,
                "ip": "1.2.3.4",
                "log_id": 0,
                "log_type": "bs-watch-log",
                "port": 1025,
                "unixtime": 1700000000,
                "user_agent": "Mozilla/5.0 (Linux...",
                "watch_id": 200000000000000001,
                "yuid": 10001700000000,
                "domain": "domain.ru",
                "current_timestamp": 1700000001,
                "source_id": "ertelecom"
            }
        )";

        const auto& parsedLine = TFpEventLogParser::ParseLine(logLine);

        UNIT_ASSERT_EQUAL(17000000001000, parsedLine.GetDuid());
        UNIT_ASSERT_EQUAL(100500, parsedLine.GetHitLogId());
        UNIT_ASSERT_EQUAL("1.2.3.4", parsedLine.GetIp());
        UNIT_ASSERT_EQUAL(0, parsedLine.GetLogId());
        UNIT_ASSERT_EQUAL("bs-watch-log", parsedLine.GetLogType());
        UNIT_ASSERT_EQUAL(1025, parsedLine.GetPort());
        UNIT_ASSERT_EQUAL("Mozilla/5.0 (Linux...", parsedLine.GetUserAgent());
        UNIT_ASSERT_EQUAL(200000000000000001, parsedLine.GetWatchId());
        UNIT_ASSERT_EQUAL(1700000000, parsedLine.GetUnixtime());
        UNIT_ASSERT_EQUAL(10001700000000, parsedLine.GetYuid());
        UNIT_ASSERT_EQUAL("domain.ru", parsedLine.GetDomain());
        UNIT_ASSERT_EQUAL(1700000001, parsedLine.GetCurrentTimestamp());
        UNIT_ASSERT_EQUAL(NCrypta::NExtFp::ER_TELECOM_SOURCE_ID, parsedLine.GetSourceId());
    }

    Y_UNIT_TEST(ParseBrokenJson) {
        const TString logLine = R"({"duid": 17000000001000,)";
        UNIT_ASSERT_EXCEPTION_CONTAINS(
            TFpEventLogParser::ParseLine(logLine),
            NJson::TJsonException,
            "Error: Missing a name for object member.");
    }
}
