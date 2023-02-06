#include "catch.hpp"

#include <ymod_httpclient/detail/request_stat.h>

namespace ymod_httpclient {

using namespace ymod_httpclient::detail;

using yplatform::per_second_accumulator;
using wrs_settings = balancing_settings::wrs_settings;

struct test_stat
{
    double value = 0.5;

    test_stat() = default;

    test_stat(double value) : value(value)
    {
    }

    double weight(double factor)
    {
        return factor * value;
    }

    test_stat& operator+=(const test_stat& other)
    {
        value = other.value;
        return *this;
    }
};

wrs_settings get_wrs_settings(double max_weight_growth, double momentary_fall_threshold)
{
    wrs_settings settings;
    settings.max_weight_growth = max_weight_growth;
    settings.momentary_fall_threshold = momentary_fall_threshold;
    return settings;
}

TEST_CASE("ymod_httpclient/wrs/dynamic_weights", "")
{
    std::vector<per_second_accumulator<test_stat>> stats(3, 5);
    std::vector<per_second_accumulator<weight_stat>> weights_average(3, 5);

    time_t current_time = std::time(0);
    weights_average[0].update(current_time - 1, { 0.5 });
    weights_average[1].update(current_time - 1, { 0.5 });
    weights_average[2].update(current_time - 1, { 0.5 });
    stats[0].add({ 0.1 });
    stats[1].add({ 0.5 });
    stats[2].add({ 0.99 });

    auto settings = get_wrs_settings(1.6, 1.1);
    std::vector<double> weights(3, 0);
    calc_weights(stats, weights, weights_average, 1.0, settings);
    REQUIRE(weights[2] > weights[1] * 1.5);
    REQUIRE(weights[1] > weights[0] * 1.5);

    stats[1].add({ 0.90 });
    calc_weights(stats, weights, weights_average, 1.0, settings);
    REQUIRE(weights[2] < weights[1] * 1.5);
    REQUIRE(weights[1] > weights[0] * 1.5);
}

TEST_CASE("ymod_httpclient/wrs/max_weight_growth", "")
{
    std::vector<per_second_accumulator<test_stat>> stats(1, per_second_accumulator<test_stat>(3));
    double prev_weight = 0.3;
    std::vector<per_second_accumulator<weight_stat>> weights_average(1, 3);
    std::vector<double> weights(1, 0);

    weights_average[0].update(std::time(0) - 1, { prev_weight });
    stats[0].add({ 0.9 });
    auto settings = get_wrs_settings(1.6, 1.1);
    calc_weights(stats, weights, weights_average, 1.0, settings);
    REQUIRE(weights[0] <= prev_weight * settings.max_weight_growth);
}

}
