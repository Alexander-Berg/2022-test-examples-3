#include "web/api2/send_parsers.h"
#include <yxiva/core/packing.hpp>
#include <yplatform/zerocopy/streambuf.h>
#include <catch.hpp>
#include <sstream>

namespace yxiva {

struct T_message_preparser
{
    struct message message;
    json_value attributes;
    ymod_webserver::request_ptr req;

    void prepare_json_request(const string& json_str)
    {
        req = boost::make_shared<ymod_webserver::request>();
        req->context = boost::make_shared<ymod_webserver::context>();
        req->content.type = "application";
        req->content.subtype = "json";
        req->raw_body = to_zerocopy_segment(json_str);
    }

    void prepare_multipart_request(const string& json_str)
    {
        req = boost::make_shared<ymod_webserver::request>();
        req->context = boost::make_shared<ymod_webserver::context>();
        req->childs.push_back({});
        req->childs.push_back({});
        req->content.type = "multipart";
        req->content.subtype = "related";
        req->childs[0].content.type = "application";
        req->childs[0].content.subtype = "json";
        req->childs[1].content.type = "application";
        req->childs[1].content.subtype = "octet-stream";

        json_value json;
        json.parse(json_str);
        auto payload = json["payload"];
        req->childs[1].body =
            to_zerocopy_segment(payload.is_string() ? payload.to_string() : payload.stringify());
        json.remove_member("payload");
        req->childs[0].body = to_zerocopy_segment(json.stringify());
    }

    yplatform::zerocopy::segment to_zerocopy_segment(const string& str)
    {
        yplatform::zerocopy::streambuf streambuf;
        auto buffers = streambuf.prepare(str.size());
        size_t offset = 0;
        for (auto& buffer : buffers)
        {
            auto it_begin = str.begin() + std::min(offset, str.size());
            auto it_end = str.begin() + std::min(offset + buffer.size(), str.size());
            std::copy(it_begin, it_end, boost::asio::buffers_begin(buffer));
            offset += buffer.size();
        }
        streambuf.commit(str.size());
        return streambuf.detach(streambuf.end());
    }

    auto msg_tie(const struct message& message) const
    {
        return std::tie(
            message.data,
            message.raw_data,
            message.tags,
            message.subscription_matchers,
            message.experiments);
    }

    void check_parse_ok(const string& json_str)
    {
        prepare_json_request(json_str);
        operation::error error;
        std::tie(error, message, attributes) = web::api2::preparse_message(req);
        CAPTURE(string(error));
        REQUIRE(!error);

        prepare_multipart_request(json_str);
        auto [error2, message2, attributes2] = web::api2::preparse_message(req);
        CAPTURE(string(error2));
        REQUIRE(!error2);
        REQUIRE(msg_tie(message) == msg_tie(message2));
        REQUIRE(message2.repacking_rules.size() == 0);
    }

    void check_parse_fail(const string& json_str, const string& err_msg = "")
    {
        prepare_json_request(json_str);
        operation::error error;
        std::tie(error, message, attributes) = web::api2::preparse_message(req);
        REQUIRE(error);
        REQUIRE(string(error).find(err_msg) != string::npos);
    }
};

TEST_CASE_METHOD(T_message_preparser, "message_preparser/with_text_payload_ok", "")
{
    check_parse_ok(R"({
  "payload" : "a"
})");
    REQUIRE(message.raw_data == "a");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/with_num_payload_fail", "")
{
    check_parse_fail(
        R"({
  "payload" : 5
})",
        "unsupported payload format");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/empty_json_fails", "")
{
    check_parse_fail("{}", "missing payload");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/empty_string_fails", "")
{
    check_parse_fail("", "no data");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/tags_as_string_fails", "")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "tags" : "tagsstring"
})",
        "invalid tags argument format");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/tags_as_num_fails", "")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "tags" : 5
})",
        "invalid tags argument format");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/tags_as_object_fails", "")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "tags" : { "key" : "value" }
})",
        "invalid tags argument format");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/tags_as_string_array_ok", "")
{
    check_parse_ok(R"({
  "payload" : "a",
  "tags" : ["tag1", "tag2", "tag3"]
})");
    REQUIRE(message.tags.size() == 3);
    REQUIRE(message.tags[0] == "tag1");
    REQUIRE(message.tags[1] == "tag2");
    REQUIRE(message.tags[2] == "tag3");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/tags_as_mixed_array_fails", "")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "tags" : ["tag1", 5]
})",
        "invalid tag");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/tags_empty_array_ok", "")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "tags" : ["tag1", 5]
})",
        "invalid tag");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/keys_as_array_fails", "")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "keys" : ["b", "c"]
})",
        "invalid keys argument format");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/keys_as_string_fails", "")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "keys" : "str"
})",
        "invalid keys argument format");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/keys_as_object_ok", "")
{
    check_parse_ok(R"({
  "payload" : "a",
  "keys" : { "key" : "value" }
})");
    REQUIRE(message.data.size() == 1);
    REQUIRE(message.data.begin()->first == "key");
    REQUIRE(message.data.begin()->second == "value");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/key_with_num_value_fails", "")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "keys" : { "key" : 5 }
})",
        "invalid value in keys argument");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/key_as_array_ok_and_sorted", "")
{
    check_parse_ok(R"({
  "payload" : "a",
  "keys" : { "key" : ["value1", "value0"] }
})");
    REQUIRE(message.data.size() == 1);
    REQUIRE(message.data.begin()->first == "key");
    string packed = yxiva::pack(std::vector<string>({ "value0", "value1" }));
    REQUIRE(message.data.begin()->second == packed);
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/empty_repacker",
    "Valid 'repack': empty root repack node")
{
    check_parse_ok(R"({
  "payload" : "a",
  "repack": {}
}
)");
    REQUIRE(message.repacking_rules.size() == 0);
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/empty_repacker_skipped",
    "Valid 'repack': empty repackers are parsed but skipped in result")
{
    check_parse_ok(R"({
  "payload" : {},
  "repack": { "apns": { } }
})");
    REQUIRE(message.repacking_rules.size() == 0);
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/payload_empty_when_only_xiva_values_in_repack",
    "Valid 'repack': when all values filled from xiva payload allowed to be not specified")
{
    check_parse_ok(R"({
  "payload" : "",
  "repack": { "apns": { "repack_payload": [ {"b": "::xiva::push_token"} ] } }
})");
    REQUIRE(message.repacking_rules.size() == 1);
    {
        json_value unpacked_repacker;
        unpacked_repacker.parse(message.repacking_rules["apns"]);
        REQUIRE(unpacked_repacker.size() == 1);
        REQUIRE(unpacked_repacker.has_member("repack_payload"));
        REQUIRE(unpacked_repacker["repack_payload"].size() == 1);
    }

    message.raw_data.clear();
    message.repacking_rules.clear();
    check_parse_ok(R"({
  "payload" : {},
  "repack": { "apns": { "repack_payload": [ {"b": "::xiva::push_token"} ] } }
})");
    REQUIRE(message.repacking_rules.size() == 1);
    {
        json_value unpacked_repacker;
        unpacked_repacker.parse(message.repacking_rules["apns"]);
        REQUIRE(unpacked_repacker.size() == 1);
        REQUIRE(unpacked_repacker.has_member("repack_payload"));
        REQUIRE(unpacked_repacker["repack_payload"].size() == 1);
    }
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/both_repacking_payload_and_service_specific_fields",
    "Valid 'repack': custom_repacker with fields from payload and push-service specific values")
{
    check_parse_ok(R"({
  "payload" : { "a": 1 },
  "repack": { "apns": { "p": 1, "repack_payload": [ "a" ] } }
})");
    REQUIRE(message.repacking_rules.size() == 1);
    {
        json_value unpacked_repacker;
        unpacked_repacker.parse(message.repacking_rules["apns"]);
        REQUIRE(unpacked_repacker.size() == 2);
        REQUIRE(unpacked_repacker.has_member("repack_payload"));
        REQUIRE(unpacked_repacker.has_member("p"));
        REQUIRE(unpacked_repacker["repack_payload"].size() == 1);
    }
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/all_valid_push_service_names",
    "Valid 'repack': check all allowed push service names")
{
    check_parse_ok(R"({
  "payload" : {},
  "repack": { "apns": { "p": 1 }, "fcm": { "p": 1 }, "other": { "p": 1 } }
})");
    REQUIRE(message.repacking_rules.size() == 3);
    {
        std::vector<string> keys = { "apns", "fcm", "other" };
        for (auto& key : keys)
        {
            json_value unpacked_repacker;
            unpacked_repacker.parse(message.repacking_rules[key]);
            REQUIRE(unpacked_repacker.size() == 1);
            REQUIRE(unpacked_repacker.has_member("p"));
        }
    }
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/invalid_repack_node_value_type",
    "Invalid 'repack': must be json-object")
{
    check_parse_fail(
        R"({
  "payload" : "a",
  "repack": 10
})",
        "invalid 'repack'");
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/unknown_push_service",
    "Invalid 'repack': unknown push_service")
{
    check_parse_fail(R"({
  "payload" : {},
  "repack": { "abg": { "repack_payload": [] } }
})");
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/duplicate_push_service",
    "Invalid 'repack': duplicate push_service")
{
    check_parse_fail(
        R"({
  "payload" : {},
  "repack": {
    "ios": { "repack_payload": [] },
    "apns": { "repack_payload": [] }
  }
})",
        "duplicate push services in 'repack'");
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/duplicate_push_service_gcm",
    "Invalid 'repack': duplicate push_service") // gcm_compatibility
{
    check_parse_fail(
        R"({
  "payload" : {},
  "repack": {
    "fcm": { "repack_payload": [] },
    "gcm": { "repack_payload": [] }
  }
})",
        "duplicate push services in 'repack'");
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/too_large_repacker",
    "Invalid 'repack': one of repackers is larger then 4kb")
{
    string long_key_name(5000UL, 'a');
    check_parse_fail(
        R"({
  "payload" : {},
  "repack": { "apns": {")" +
            long_key_name + R"(": "some value"} }
})",
        "too large");
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/raw_payload_with_repack_payload",
    "Invalid 'repack': cant repack payload fields when payload is raw_string")
{
    check_parse_fail(R"({
  "payload" : "a",
  "repack": { "apns": { "repack_payload": [ "b" ] } }
})");
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/repack/key_not_present_in_payload",
    "Invalid 'repack': references unknown field[s] in combination with known or without")
{
    check_parse_fail(
        R"({
  "payload" : { "a": 1 },
  "repack": { "apns": { "repack_payload": [ "b" ] } }
})",
        "unknown");
    check_parse_fail(
        R"({
  "payload" : { "a": 1 },
  "repack": { "apns": { "repack_payload": [ "b", "a", "c" ] } }
})",
        "unknown");
    check_parse_fail(
        R"({
  "payload" : { "a": 1 },
  "repack": { "apns": { "repack_payload": [ "b", "c" ] } }
})",
        "unknown");
    check_parse_fail(
        R"({
  "payload" : { "a": 1 },
  "repack": { "apns": { "repack_payload": [ "a" ] }, "gcm": { "repack_payload": [ "b" ] } }})", // gcm_compatibility
        "unknown");
    check_parse_fail(
        R"({
  "payload" : { "a": 1 },
  "repack": { "apns": { "repack_payload": [ "a" ] }, "fcm": { "repack_payload": [ "b" ] } }
})",
        "unknown");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/subscription_matchers/none", "")
{
    check_parse_ok(R"({
    "payload" : "a"
  })");
    REQUIRE(message.subscription_matchers.empty());
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/subscription_matchers/empty", "")
{
    check_parse_ok(R"({
    "payload" : "a",
    "subscriptions": []
  })");
    REQUIRE(message.subscription_matchers.empty());
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/subscription_matchers/invalid", "")
{
    check_parse_fail(R"({
    "payload" : "a",
    "subscriptions": {}
  })");
    check_parse_fail(R"({
    "payload" : "a",
    "subscriptions": ""
  })");
    check_parse_fail(R"({
    "payload" : "a",
    "subscriptions": 0
  })");
    check_parse_fail(R"({
    "payload" : "a",
    "subscriptions": [{"$has_tags": ["wns", "fcm"]}]
  })");
}

TEST_CASE_METHOD(
    T_message_preparser,
    "message_preparser/subscription_matchers/one_rule_gcm",
    "") // gcm_compatibility
{
    using cond_t = filter::subscription_condition_type;

    check_parse_ok(R"({
    "payload" : "a",
    "subscriptions": [{"platform": ["wns", "gcm"]}]})"); // gcm_compatibility
    filter::subscription_condition cond{ cond_t::platform,
                                         "",
                                         { "gcm", "wns" } }; // gcm_compatibility

    REQUIRE(message.subscription_matchers.size() == 1);
    REQUIRE(message.subscription_matchers.front() == cond);
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/subscription_matchers/one_rule", "")
{
    using cond_t = filter::subscription_condition_type;

    check_parse_ok(R"({
    "payload" : "a",
    "subscriptions": [{"platform": ["wns", "fcm"]}]
  })");
    filter::subscription_condition cond{ cond_t::platform, "", { "fcm", "wns" } };

    REQUIRE(message.subscription_matchers.size() == 1);
    REQUIRE(message.subscription_matchers.front() == cond);
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/subscription_matchers/multiple", "")
{
    using cond_t = filter::subscription_condition_type;

    check_parse_ok(R"({
    "payload" : "a",
    "subscriptions": [
      {"transport": ["a", "b"]},
      {"subscription_id": ["c", "d"]},
      {"session": ["e", "f"]},
      {"uuid": ["g", "h"]},
      {"device": ["i"]},
      {"app": ["123", "abc", "ya.navigator.ru"]}
    ]
  })");
    filter::subscription_condition conds[]{
        { cond_t::transport, "", { "a", "b" } },
        { cond_t::subscription_id, "", { "c", "d" } },
        { cond_t::session, "", { "e", "f" } },
        { cond_t::uuid, "", { "g", "h" } },
        { cond_t::device, "", { "i" } },
        { cond_t::app, "", { "123", "abc", "ya.navigator.ru" } }
    };

    REQUIRE(message.subscription_matchers.size() == 6);
    size_t i = 0;
    for (auto& c : message.subscription_matchers)
    {
        REQUIRE(c == conds[i++]);
    }
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/experiments/empty", "")
{
    check_parse_ok(R"({
    "payload" : "a",
    "experiments": []
  })");
    REQUIRE(message.experiments.size() == 0);
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/experiments/invalid", "")
{
    check_parse_fail(
        R"({
    "payload" : "a",
    "experiments": {}
  })",
        "invalid json type of \"experiments\"");

    check_parse_fail(
        R"({
    "payload" : "a",
    "experiments": [{}]
  })",
        "invalid json type of \"experiments\" item");

    check_parse_fail(
        R"({
    "payload" : "a",
    "experiments": [""]
  })",
        "invalid experiment id");

    check_parse_fail(
        R"({
    "payload" : "a",
    "experiments": ["1"]
  })",
        "invalid experiment id");

    check_parse_fail(
        R"({
    "payload" : "a",
    "experiments": ["A-B"]
  })",
        "invalid experiment id");
}

TEST_CASE_METHOD(T_message_preparser, "message_preparser/experiments/success", "")
{
    check_parse_ok(R"({
    "payload" : "a",
    "experiments": ["A-1", "A-2", "A-1", "A-3"]
  })");
    REQUIRE(message.experiments.size() == 3);
}

}
