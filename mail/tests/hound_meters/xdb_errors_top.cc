#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/hound/unistat/cpp/hound_meters.h>

using namespace ::testing;
using namespace ::unistat;


TEST(XdbErrorsTop, shouldHaveZeroValueJustAfterInit) {
    XdbErrorsTop meter(10, "xdb_err");
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{}));
}

struct XdbErrorsTopTest : public Test {
    std::map<std::string, std::string> buildTskvRecord(std::string conninfo = "") {
        const std::map<std::string, std::string> values = {
                {"tskv_format", "mail-hound-tskv-log"}
                , {"level", "notice"}
                , {"uid", "4002859821"}
                , {"connection_info", conninfo}
                , {"rule_name", "pgg::database::fallback::rules::Replica"}
                , {"strategy_name", "ReplicaThenMaster"}
        };

        return values;
    }

    std::string buildLogLine(std::string conninfo = "") {
        return "tskv\ttskv_format=mail-hound-tskv-log\tthread=139640711407360\tunixtime=1555922951\t\t"
               "imestamp=2019-04-22T11:49:11.226685+0300\tlevel=notice\tuid=4002859821\t"
               "request_id=b6f8fcf292ec16c36950499edb8edab0\tmacs_message=\tmacs_method=AllLabelsList\t"
               "query_text=/* sql: AllLabelsList, uid: 4002859821 */\\n    SELECT\\n        lid, name,"
               " type, color,\\n        ROUND(EXTRACT(EPOCH FROM created))::bigint AS created,\\n"
               "        message_count, revision\\n    FROM\\n        mail.labels\\n    WHERE\\n    "
               "    uid=$1\\n\tquery_values=[\"4002859821\"]\t"
               "connection_info=" + conninfo
               + " port=6432 dbname=maildb user=web\t"
               "rule_name=pgg::database::fallback::rules::Replica\terror_code.category=apq.pq\t"
               "error_code.value=2\terror_code.message=Request queue timed out: in query AllLabelsList"
               " Request queue timed out";
    }
};

TEST_F(XdbErrorsTopTest, shouldIncrementValueForAppropriateHostAfterUpdate) {
    XdbErrorsTop meter(10, "xdb_err");
    meter.update(buildLogLine("host=pgload21e.mail.yandex.net"));
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{{"xdb_err_pgload21e.mail.yandex.net_summ", 1}}));
}

TEST_F(XdbErrorsTopTest, shouldIncrementValueForAppropriateHostAfterTskvUpdate) {
    XdbErrorsTop meter(10, "xdb_err");
    meter.update(buildTskvRecord("host=pgload21e.mail.yandex.net port=6432 dbname=maildb user=web"));
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{{"xdb_err_pgload21e.mail.yandex.net_summ", 1}}));
}

TEST_F(XdbErrorsTopTest, shouldNotIncrementForUnmatchedLine) {
    XdbErrorsTop meter(10, "xdb_err");
    meter.update(buildLogLine());
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{}));
}

TEST_F(XdbErrorsTopTest, shouldNotIncrementForUnmatchedTskvField) {
    XdbErrorsTop meter(10, "xdb_err");
    meter.update(buildTskvRecord());
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{}));
}

TEST_F(XdbErrorsTopTest, shouldShowOnlyTop) {
    XdbErrorsTop meter(2, "xdb_err");
    meter.update(buildLogLine("host=pgload21e.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload21e.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload22h.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload22h.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload666k.mail.yandex.net"));
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"xdb_err_pgload22h.mail.yandex.net_summ", 2},
        {"xdb_err_pgload21e.mail.yandex.net_summ", 2}
    }));
}

TEST_F(XdbErrorsTopTest, shouldShowOnlyTopTskv) {
    XdbErrorsTop meter(2, "xdb_err");
    meter.update(buildTskvRecord("host=pgload21e.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload21e.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload22h.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload22h.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload666k.mail.yandex.net"));
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
            {"xdb_err_pgload22h.mail.yandex.net_summ", 2},
            {"xdb_err_pgload21e.mail.yandex.net_summ", 2}
    }));
}

TEST_F(XdbErrorsTopTest, shouldSortTopByErrorNumbers) {
    XdbErrorsTop meter(3, "xdb_err");
    meter.update(buildLogLine("host=pgload21e.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload21e.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload666k.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload21e.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload22h.mail.yandex.net"));
    meter.update(buildLogLine("host=pgload22h.mail.yandex.net"));
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
        {"xdb_err_pgload21e.mail.yandex.net_summ", 3},
        {"xdb_err_pgload22h.mail.yandex.net_summ", 2},
        {"xdb_err_pgload666k.mail.yandex.net_summ", 1}
    }));
}

TEST_F(XdbErrorsTopTest, shouldSortTopByErrorNumbersTskv) {
    XdbErrorsTop meter(3, "xdb_err");
    meter.update(buildTskvRecord("host=pgload21e.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload21e.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload666k.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload21e.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload22h.mail.yandex.net"));
    meter.update(buildTskvRecord("host=pgload22h.mail.yandex.net"));
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{
            {"xdb_err_pgload21e.mail.yandex.net_summ", 3},
            {"xdb_err_pgload22h.mail.yandex.net_summ", 2},
            {"xdb_err_pgload666k.mail.yandex.net_summ", 1}
    }));
}
