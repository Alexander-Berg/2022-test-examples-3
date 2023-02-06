#include <yxiva/core/json.h>
#include <yxiva/core/repacker.h>
#include <yplatform/encoding/url_encode.h>
#include <catch.hpp>
#include <boost/regex.hpp>

using namespace yxiva;

message load_message(json_value json, const message& base = {})
{
    auto&& json_message = json["message"];
    message msg = base;
    msg.bright = json_get(json_message, "bright", msg.bright);
    msg.uid = json_get(json_message, "uid", msg.uid);
    msg.service = json_get(json_message, "service", msg.service);
    msg.operation = json_get(json_message, "operation", msg.operation);
    msg.transit_id = json_get(json_message, "transit_id", msg.transit_id);
    msg.local_id = json_get(json_message, "local_id", msg.local_id);
    if (json_message.has_member("raw_data"))
    {
        if (json_message["raw_data"].is_string())
            msg.raw_data = json_message["raw_data"].to_string();
        else
            msg.raw_data = json_write(json_message["raw_data"]);
    }
    if (json_message.has_member("data"))
    {
        auto&& json_data = json_message["data"];
        if (json_data.is_object())
        {
            for (auto idata = json_data.members_begin(); idata != json_data.members_end(); ++idata)
            {
                msg.data[string(idata.key())] = (*idata).to_string();
            }
        }
    }
    if (json_message.has_member("repack"))
    {
        auto&& json_data = json_message["repack"];
        if (json_data.is_object())
        {
            for (auto idata = json_data.members_begin(); idata != json_data.members_end(); ++idata)
            {
                msg.repacking_rules[string(idata.key())] =
                    (*idata).is_string() ? (*idata).to_string() : json_write(*idata);
            }
        }
    }
    return msg;
}

push_subscription_params load_subscription(
    json_value json,
    const push_subscription_params& base = {})
{
    auto&& json_sub = json["subscription"];
    push_subscription_params sub = base;
    sub.id = json_get(json_sub, "id", sub.id);
    sub.platform = platform::resolve_alias(json_get(json_sub, "platform", sub.platform)).name;
    sub.app_name = json_get(json_sub, "app_name", sub.app_name);
    sub.push_token = json_get(json_sub, "push_token", sub.push_token);
    sub.device = json_get(json_sub, "device", sub.device);
    sub.client = json_get(json_sub, "client", sub.client);
    sub.extra_data = json_get(json_sub, "extra_data", sub.extra_data);
    return sub;
}

void validate_payload(const json_value& expected, const string& result)
{
    if (expected.is_string())
    {
        CHECK(result == expected.to_string());
    }
    else
    { // json-object
        CHECK(json_parse(result) == expected);
    }
}

void validate_url_params(const json_value& expected, const string& result)
{
    if (expected.type() == json_type::tnull)
    {
        REQUIRE(result.empty());
        return;
    }

    boost::regex full_url_params_string(R"(^(&([^=\s&]+)=([^=\s&]+))*$)");
    boost::regex url_param(R"(&([^=\s&]+)=([^=\s&]+))");

    boost::smatch match;
    bool matched = boost::regex_match(result, match, full_url_params_string);
    REQUIRE(matched == true);

    boost::sregex_iterator iter(result.begin(), result.end(), url_param);
    boost::sregex_iterator end;
    json_value result_opts(json_type::tobject);
    for (; iter != end; ++iter)
    {
        auto param_key = yplatform::url_decode<string>((*iter)[1].str());
        auto param_value = yplatform::url_decode<string>((*iter)[2].str());
        // XXX: x-collapse-id and x-priority are the only parameters not packed in json
        // and not parsed as JSON in xiva/mobile,
        // but priority is a single number, therefore a valid JSON by itself.
        // Disable parsing x-collapse-id as JSON, since it is a single string
        if (param_key == "x-collapse-id")
        {
            result_opts[param_key] = param_value;
        }
        else
        {
            result_opts[param_key] = json_parse_no_type_check(param_value);
        }
    }
    CHECK(result_opts == expected);
    // XXX also check that result_opts were properly url encoded?
}

void test_repacking_with_fixtures_from_file(const string& filename)
{
    std::ifstream test_data_file(filename);
    std::stringstream test_data_raw;
    test_data_raw << test_data_file.rdbuf();
    test_data_file.close();
    json_value test_data = json_parse(test_data_raw.str());

    json_value defaults = test_data["defaults"];
    test_data.remove_member("defaults");
    message base_msg = load_message(defaults);
    push_subscription_params base_sub = load_subscription(defaults);

    for (auto&& itest_case = test_data.members_begin(); itest_case != test_data.members_end();
         ++itest_case)
    {
        SECTION(string(itest_case.key()))
        {
            auto&& test_case = *itest_case;
            message msg = load_message(test_case, base_msg);
            push_subscription_params sub = load_subscription(test_case, base_sub);
            auto&& all_expected = test_case.cref["expect"]["messages"];
            bool expect_bright = json_get<bool>(test_case["expect"], "bright", false);

            push_requests_queue messages;
            auto repacked = repack_message_if_needed(packet(nullptr, msg, sub_t{}), sub, messages);
            CHECK(messages.is_bright() == expect_bright);
            if (all_expected.size() > 0)
            {
                CHECK(repacked.error_reason == "");
                REQUIRE(messages.size() == all_expected.size());

                for (unsigned int i = 0; i < all_expected.size(); ++i)
                {
                    const auto& message = messages.pop_front();
                    auto&& expected = all_expected[i];
                    validate_payload(expected["payload"], message.payload());
                    validate_url_params(expected["urlparams"], message.http_url_params());
                }
            }
            else
            {
                if (all_expected.is_string())
                    CHECK(repacked.error_reason == all_expected.to_string());
                assert(all_expected.size() == 0);
                REQUIRE(messages.size() == 0);
            }
        }
    }
}

TEST_CASE("repacking/mobile/repacking_rules/apply", "")
{
    test_repacking_with_fixtures_from_file("repack.json");
}

TEST_CASE("repacking/mobile/repack_hardcoded/mail", "")
{
    test_repacking_with_fixtures_from_file("repack_mail.json");
}

TEST_CASE("repacking/mobile/repack_hardcoded/disk", "")
{
    test_repacking_with_fixtures_from_file("repack_disk.json");
}

TEST_CASE("repacking/mobile/repack_hardcoded/apns_queue", "")
{
    test_repacking_with_fixtures_from_file("repack_apns_queue.json");
}
