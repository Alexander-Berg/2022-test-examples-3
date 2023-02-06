#include "mod_wns/chunks.h"
#include <yxiva/core/operation_result.h>
#include <catch.hpp>
#include <utility>

using namespace yxiva;
using namespace yxiva::mobile;

using params_t = std::map<string, string>;

struct test_url
{
    string param_value(const string& key, const string& default_value) const
    {
        auto it = params.find(key);
        return it == params.end() ? default_value : it->second;
    }

    test_url() = default;

    test_url(params_t params) : params(std::move(params))
    {
    }

    params_t params;
};

struct test_data
{
    std::vector<chunk> chunks;
    std::vector<error::code> chunk_results;

    bool operator==(const test_data& other) const
    {
        return chunks == other.chunks && chunk_results == other.chunk_results;
    }
};

operation::result test_convert_wns(
    const string& input,
    wns_notification_type::type /*notification_type*/,
    string& output)
{
    if (input == "good")
    {
        output = "good";
        return operation::success;
    }
    return "fail";
}

std::shared_ptr<test_data> make_chunks(params_t params, string payload)
{
    return make_chunked_data<test_data, test_url>(test_url(params), payload, test_convert_wns);
}

TEST_CASE("wns/chunks/invalid_payload_if_no_params", "")
{
    auto data = make_chunks({}, "");

    REQUIRE(data->chunks.empty());
    REQUIRE(data->chunk_results.size() == 1);
    REQUIRE(data->chunk_results[0] == error::code::invalid_payload);
}

TEST_CASE("wns/chunks/invalid_payload_if_bad_param", "")
{
    auto data = make_chunks({ { "x-toast", "bad" } }, "");

    REQUIRE(data->chunks.empty());
    REQUIRE(data->chunk_results.size() == 1);
    REQUIRE(data->chunk_results[0] == error::code::invalid_payload);
}

TEST_CASE("wns/chunks/one_chunk_and_one_error_if_one_good_one_bad", "")
{
    auto data = make_chunks({ { "x-toast", "bad" }, { "x-tile", "good" } }, "");

    test_data result = { { { "good", wns_notification_type::tile } },
                         { error::code::invalid_payload } };

    REQUIRE(*data == result);
}

TEST_CASE("wns/chunks/no_errors_if_only_good_chunks", "")
{
    auto data =
        make_chunks({ { "x-toast", "good" }, { "x-tile", "good" }, { "x-badge", "good" } }, "good");

    test_data result = { {
                             { "good", wns_notification_type::toast },
                             { "good", wns_notification_type::raw },
                             { "good", wns_notification_type::tile },
                             { "good", wns_notification_type::badge },
                         },
                         {} };

    REQUIRE(*data == result);
}

TEST_CASE("wns/chunks/mixed_status/empty")
{
    REQUIRE(calc_mixed_result({}) == error::code::data_compose_error);
}

TEST_CASE("wns/chunks/mixed_status/single/success")
{
    REQUIRE(calc_mixed_result({ error::code::success }) == error::code::success);
}

TEST_CASE("wns/chunks/mixed_status/single/bad_subscription")
{
    REQUIRE(calc_mixed_result({ error::code::invalid_token }) == error::code::invalid_token);
    REQUIRE(
        calc_mixed_result({ error::code::invalid_token_length }) ==
        error::code::invalid_token_length);
    REQUIRE(
        calc_mixed_result({ error::code::subscription_expired }) ==
        error::code::subscription_expired);
    REQUIRE(
        calc_mixed_result({ error::code::invalid_subscription }) ==
        error::code::invalid_subscription);
}

TEST_CASE("wns/chunks/mixed_status/single/bad_request")
{
    REQUIRE(calc_mixed_result({ error::code::invalid_payload }) == error::code::data_compose_error);
    REQUIRE(
        calc_mixed_result({ error::code::invalid_payload_length }) ==
        error::code::data_compose_error);
    REQUIRE(
        calc_mixed_result({ error::code::data_compose_error }) == error::code::data_compose_error);
}

TEST_CASE("wns/chunks/mixed_status/single/internal_error")
{
    REQUIRE(calc_mixed_result({ error::code::task_cancelled }) == error::code::task_cancelled);
    REQUIRE(calc_mixed_result({ error::code::cloud_error }) == error::code::cloud_error);
    REQUIRE(calc_mixed_result({ error::code::internal_error }) == error::code::internal_error);
}

TEST_CASE("wns/chunks/mixed_status/many/bad_subscription")
{
    REQUIRE(
        calc_mixed_result({ error::code::invalid_token, error::code::success }) ==
        error::code::invalid_token);
    REQUIRE(
        calc_mixed_result({ error::code::success, error::code::invalid_token }) ==
        error::code::invalid_token);
    REQUIRE(
        calc_mixed_result({ error::code::invalid_payload, error::code::invalid_token }) ==
        error::code::invalid_token);
}

TEST_CASE("wns/chunks/mixed_status/many/partial_success")
{
    REQUIRE(
        calc_mixed_result({ error::code::invalid_payload, error::code::success }) ==
        error::code::partial_success);
    REQUIRE(
        calc_mixed_result({ error::code::success, error::code::invalid_payload }) ==
        error::code::partial_success);
    REQUIRE(
        calc_mixed_result(
            { error::code::invalid_payload, error::code::success, error::code::cloud_error }) ==
        error::code::partial_success);
}

TEST_CASE("wns/chunks/mixed_status/many/internal_error")
{
    REQUIRE(
        calc_mixed_result({ error::code::invalid_payload, error::code::cloud_error }) ==
        error::code::cloud_error);
    REQUIRE(
        calc_mixed_result({ error::code::cloud_error, error::code::invalid_payload }) ==
        error::code::cloud_error);
    REQUIRE(
        calc_mixed_result({ error::code::invalid_payload,
                            error::code::internal_error,
                            error::code::cloud_error }) == error::code::internal_error);
}

TEST_CASE("wns/chunks/mixed_status/many/compose_error")
{
    REQUIRE(
        calc_mixed_result({ error::code::invalid_payload, error::code::invalid_payload_length }) ==
        error::code::data_compose_error);
    REQUIRE(
        calc_mixed_result({ error::code::invalid_payload_length, error::code::invalid_payload }) ==
        error::code::data_compose_error);
    REQUIRE(
        calc_mixed_result({ error::code::invalid_payload_length,
                            error::code::data_compose_error,
                            error::code::invalid_payload_length }) ==
        error::code::data_compose_error);
}
