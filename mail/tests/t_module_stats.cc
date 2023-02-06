#include "module_stats.h"
#include <boost/property_tree/json_parser.hpp>
#include <boost/property_tree/ini_parser.hpp>
#include <catch.hpp>
#include <algorithm>
#include <regex>
#include <sstream>
#include <numeric>

using namespace ymod_webserver;

const string PATH = "ping";
const string ANOTHER_PATH = "pong";
const double TIMING = 1;

struct t_code_stats
{
    t_code_stats() : stats(detail::make_http_code_counters())
    {
    }

    string make_metric_name(codes::code code, const string& path)
    {
        return "codes_" + (path.length() ? path + "_" : "code_") + std::to_string(code) +
            "_cumulative";
    }

    auto get_code_counter(codes::code code, const string& path = "")
    {
        auto&& metrics = stats.to_ptree();
        auto&& code_counter_opt = metrics.get_optional<string>(make_metric_name(code, path));
        return std::stoi(*code_counter_opt);
    }

    bool is_code_counter_exist(codes::code code, const string& path = "")
    {
        auto&& metrics = stats.to_ptree();
        auto&& code_counter_opt = metrics.get_optional<string>(make_metric_name(code, path));
        return code_counter_opt.is_initialized();
    }

    code_stats stats;
};

struct t_timing_stats
{
    using count_t = uint64_t;

    t_timing_stats() : stats(AXIS)
    {
    }

    const double MILLISECOND = 0.001;
    const double MIN_BOUND = 0.1;
    const double MAX_BOUND = 1000;
    const std::size_t BUCKET_NUM = 10;
    const timing_stats::histogram_axis AXIS =
        detail::make_hgram_axis(BUCKET_NUM, MIN_BOUND, MAX_BOUND);
    timing_stats stats;
};

TEST_CASE("paths are formatted")
{
    auto&& [path_to_format, formatted_path] =
        GENERATE(table<string, string>({ { "/ping*", "ping" },
                                         { "//ping_pong//", "ping_pong" },
                                         { "PING/PONG/PING", "ping_pong_ping" },
                                         { "P&i^n&C0d", "pinc0d" },
                                         { "/**/", "" } }));

    CAPTURE(path_to_format, formatted_path);
    REQUIRE(detail::format_path(path_to_format) == formatted_path);
}

TEST_CASE_METHOD(t_code_stats, "increment code counter")
{
    stats.add_path(PATH);
    auto&& increment_count = GENERATE(values<uint32_t>({ 1, 2, 3, 10 }));

    for (uint32_t i = 0; i < increment_count; ++i)
    {
        stats.increment_code_counters(PATH, codes::ok);
    }
    REQUIRE(get_code_counter(codes::ok) == increment_count);
    REQUIRE(get_code_counter(codes::ok, PATH) == increment_count);
}

TEST_CASE_METHOD(t_code_stats, "increment by different codes")
{
    stats.add_path(PATH);
    stats.increment_code_counters(PATH, codes::ok);
    stats.increment_code_counters(PATH, codes::unauthorized);
    stats.increment_code_counters(PATH, codes::ok);

    auto&& [code, expected_value] =
        GENERATE(table<codes::code, uint32_t>({ { codes::ok, 2 }, { codes::unauthorized, 1 } }));

    CAPTURE(code, expected_value);
    REQUIRE(get_code_counter(code) == expected_value);
    REQUIRE(get_code_counter(code, PATH) == expected_value);
}

TEST_CASE_METHOD(t_code_stats, "increment by unknown path")
{
    string unknown_path = "abacaba";

    stats.increment_code_counters(unknown_path, codes::ok);

    REQUIRE_FALSE(is_code_counter_exist(codes::ok, unknown_path));
    REQUIRE(get_code_counter(codes::ok) == 1);
}

TEST_CASE_METHOD(t_code_stats, "increment by unknown code")
{
    auto&& code = GENERATE(1, 450, 900);

    stats.add_path(PATH);
    stats.increment_code_counters(PATH, static_cast<codes::code>(code));

    CAPTURE(code);
    REQUIRE_FALSE(is_code_counter_exist(static_cast<codes::code>(code)));
    REQUIRE_FALSE(is_code_counter_exist(static_cast<codes::code>(code), PATH));
}

TEST_CASE_METHOD(t_code_stats, "total code counts")
{
    string first_path = "ping";
    string second_path = "pong";

    stats.add_path(first_path);
    stats.add_path(second_path);

    stats.increment_code_counters(first_path, codes::unauthorized);
    stats.increment_code_counters(first_path, codes::ok);
    stats.increment_code_counters(second_path, codes::unauthorized);

    REQUIRE(get_code_counter(codes::unauthorized) == 2);
    REQUIRE(get_code_counter(codes::ok) == 1);
}

TEST_CASE_METHOD(t_timing_stats, "init timing stats")
{
    REQUIRE(stats.total_timings().axis() == AXIS);
    REQUIRE(stats.timings_by_path().empty());
}

TEST_CASE_METHOD(t_timing_stats, "add path")
{
    stats.add_path(PATH);
    auto timing_hgrams_by_path = stats.timings_by_path();

    REQUIRE(timing_hgrams_by_path.contains(PATH));
    REQUIRE(timing_hgrams_by_path.at(PATH).axis() == AXIS);
}

TEST_CASE_METHOD(t_timing_stats, "add timing")
{
    auto timing = GENERATE_COPY(values<double>({ (MIN_BOUND + MAX_BOUND) / 2,
                                                 0.0,
                                                 MIN_BOUND - MILLISECOND,
                                                 MAX_BOUND + MILLISECOND,
                                                 -1.0 }));
    stats.add_path(PATH);
    stats.add_timing(PATH, timing);
    auto timing_hgram = stats.timings_by_path().at(PATH);
    auto total_timing_hgram = stats.total_timings();

    CAPTURE(timing);
    REQUIRE(timing_hgram.at(AXIS.index(timing)) == 1);
    REQUIRE(total_timing_hgram.at(AXIS.index(timing)) == 1);
}

TEST_CASE_METHOD(t_timing_stats, "add timing several times")
{
    stats.add_path(PATH);
    std::size_t rep_count = 5;
    for (std::size_t i = 0; i < rep_count; ++i)
    {
        stats.add_timing(PATH, TIMING);
    }
    auto timing_hgram = stats.timings_by_path().at(PATH);
    auto total_timing_hgram = stats.total_timings();

    REQUIRE(timing_hgram.at(AXIS.index(TIMING)) == rep_count);
    REQUIRE(total_timing_hgram.at(AXIS.index(TIMING)) == rep_count);
}

TEST_CASE_METHOD(t_timing_stats, "add timings by different paths")
{
    stats.add_path(PATH);
    stats.add_path(ANOTHER_PATH);
    stats.add_timing(PATH, TIMING);
    stats.add_timing(ANOTHER_PATH, TIMING);
    auto timing_hgram_by_path = stats.timings_by_path().at(PATH);
    auto timing_hgram_by_another_path = stats.timings_by_path().at(ANOTHER_PATH);
    auto total_timing_hgram = stats.total_timings();

    REQUIRE(timing_hgram_by_path.at(AXIS.index(TIMING)) == 1);
    REQUIRE(timing_hgram_by_another_path.at(AXIS.index(TIMING)) == 1);
    REQUIRE(total_timing_hgram.at(AXIS.index(TIMING)) == 2);
}

TEST_CASE_METHOD(t_timing_stats, "add timing by unknown path")
{
    string unknown_path = "abacaba";
    stats.add_timing(unknown_path, TIMING);
    auto timing_hgrams_by_path = stats.timings_by_path();
    auto total_timing_hgram = stats.total_timings();

    REQUIRE_FALSE(timing_hgrams_by_path.contains(unknown_path));
    REQUIRE(total_timing_hgram.at(AXIS.index(TIMING)) == 1);
}
