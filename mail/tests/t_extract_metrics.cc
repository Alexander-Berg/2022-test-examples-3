#include "../src/extract_metrics.h"
#include <boost/property_tree/json_parser.hpp>

#include <catch.hpp>

namespace ymod_metricserver {

using std::string;
using yplatform::ptree;

const string metric_name = "metric_name";
const string hgram_metric_name = "metric_name_hgram";

struct extract_metrics_test
{
    extract_metrics_test()
    {
    }

    ptree generate_stats(const string& json)
    {
        ptree stats;
        std::stringstream stream(json);
        read_json(stream, stats);
        return stats;
    }

    ptree generate_stats(const string& metric_name, const string& value)
    {
        return generate_stats("{\"" + metric_name + "\": " + value + "}");
    }
};

TEST_CASE_METHOD(extract_metrics_test, "integer numeric value")
{
    auto value = "1";
    auto stats = generate_stats(metric_name, value);

    REQUIRE(extract_metrics(stats) == metric_name + " " + value + "\n");
}

TEST_CASE_METHOD(extract_metrics_test, "bool value")
{
    auto stats_with_value_true = generate_stats(metric_name, "true");
    auto stats_with_value_false = generate_stats(metric_name, "false");

    REQUIRE(extract_metrics(stats_with_value_true) == metric_name + " 1\n");
    REQUIRE(extract_metrics(stats_with_value_false) == metric_name + " 0\n");
}

TEST_CASE_METHOD(extract_metrics_test, "floating point numeric value")
{
    auto value = "1.1";
    auto stats = generate_stats(metric_name, value);

    REQUIRE(extract_metrics(stats) == metric_name + " " + value + "\n");
}

TEST_CASE_METHOD(extract_metrics_test, "raw hgram")
{
    auto value = "[1,2,3]";
    auto stats = generate_stats(hgram_metric_name, value);

    REQUIRE(extract_metrics(stats) == hgram_metric_name + " " + value + "\n");
}

TEST_CASE_METHOD(extract_metrics_test, "bucket hgram")
{
    auto value = "[[1,1],[2,2],[3,3]]";
    auto stats = generate_stats(hgram_metric_name, value);

    REQUIRE(extract_metrics(stats) == hgram_metric_name + " " + value + "\n");
}

TEST_CASE_METHOD(extract_metrics_test, "not numeric value")
{
    auto value = "\"not_numeric\"";
    auto stats = generate_stats(metric_name, value);

    REQUIRE(extract_metrics(stats) == "");
}

TEST_CASE_METHOD(extract_metrics_test, "not _hgram array")
{
    auto value = "[1,2,3]";
    auto stats = generate_stats(metric_name, value);

    REQUIRE(extract_metrics(stats) == "");
}

TEST_CASE_METHOD(extract_metrics_test, "hgram contains name")
{
    auto value = "[1,{\"name\":2}]";
    auto stats = generate_stats(hgram_metric_name, value);

    REQUIRE(extract_metrics(stats) != hgram_metric_name + " " + value + "\n");
}

TEST_CASE_METHOD(extract_metrics_test, "hgram contains not numeric")
{
    auto value = "[\"not_numeric\",1]";
    auto stats = generate_stats(hgram_metric_name, value);

    REQUIRE(extract_metrics(stats) != hgram_metric_name + " " + value + "\n");
}

TEST_CASE_METHOD(extract_metrics_test, "bucket in hgram contains more values")
{
    auto value = "[[1,1],[2,2,2],[3,3]]";
    auto stats = generate_stats(hgram_metric_name, value);

    REQUIRE(extract_metrics(stats) == "");
}

TEST_CASE_METHOD(extract_metrics_test, "bucket in hgram contains less values")
{
    auto value = "[[1,1],[2,2],[3]]";
    auto stats = generate_stats(hgram_metric_name, value);

    REQUIRE(extract_metrics(stats) == "");
}

TEST_CASE_METHOD(extract_metrics_test, "bucket hgram contains not bucket")
{
    auto value = "[[1,1],[2,2],3]";
    auto stats = generate_stats(hgram_metric_name, value);

    REQUIRE(extract_metrics(stats) == "");
}

TEST_CASE_METHOD(extract_metrics_test, "bucket in hgram contains bucket")
{
    auto value = "[[1,1],[2,2],[[1,1]]]";
    auto stats = generate_stats(hgram_metric_name, value);

    REQUIRE(extract_metrics(stats) == "");
}

TEST_CASE_METHOD(extract_metrics_test, "metric name convertation")
{
    auto&& [raw_name, converted_name] =
        GENERATE(table<string, string>({ { "no_format", "no_format" },
                                         { "ToLOWEr", "tolower" },
                                         { "&*^%@_some&*^%@symbols-&*^%@", "some_symbols" },
                                         { "p1nc^0D_", "p1nc_0d" },
                                         { ".-_trim_.--._test----....____", "trim_test" } }));

    CAPTURE(raw_name, converted_name);

    auto value = "1";
    auto stats = generate_stats(raw_name, value);

    REQUIRE(extract_metrics(stats) == converted_name + " " + value + "\n");
}

TEST_CASE_METHOD(extract_metrics_test, "nested statistics")
{
    string external_name = "external";
    string internal_name = "internal";
    auto value = "1";
    auto stats =
        generate_stats("{\"" + external_name + "\": {\"" + internal_name + "\": " + value + "}}");

    REQUIRE(extract_metrics(stats) == external_name + "_" + internal_name + " " + value + "\n");
}

}
