#include "helpers.h"
#include "messages.h"
#include <processor/processor.h>
#include <processor/json_helpers.h>
#include <yplatform/coroutine.h>
#include <catch.hpp>

using processor_t = processor<fake_http_client>;

static auto make_http_clients_map(const fake_http_client& http)
{
    auto clients = std::make_shared<std::map<string, fake_http_client>>();
    (*clients)["xiva_list.production"] = http;
    (*clients)["counters.production"] = http;
    (*clients)["xiva_list.corp"] = http;
    (*clients)["counters.corp"] = http;
    (*clients)["ava"] = http;
    (*clients)["meta"] = http;
    (*clients)["xiva_send"] = http;
    (*clients)["searchapp_send"] = http;
    return clients;
}

static auto make_default_settings()
{
    settings s;
    s.environment = "production";
    s.list["production"].request = "/list";
    s.counters["production"].request = "/counters";
    s.meta.request = "/meta";
    s.meta.max_mids_in_filter_search_request = 1;
    s.meta.fallback_to_db_master_max_lag = 0;
    s.ava.request = "/ava";
    s.ava.max_emails_in_profiles_request = 1;
    s.send.request = "/send";
    s.send.ttl = 777;
    s.send.rtec_3674_rollout_percent = 100;
    return s;
}

TEST_CASE("processor/subsctiptions/rate_controller_error")
{
    auto task = make_task();
    fake_http_client http;
    std::vector<rc_error> rc_errors{ rc_error::capacity_exceeded };
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    mod_rc->rc_map["xiva_list.production"] = std::make_shared<fake_rate_controller>(rc_errors);
    auto s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::rate_limit));
    REQUIRE(http.requests().empty());
}

TEST_CASE("processor/subsctiptions/http_error")
{
    auto task = make_task();
    fake_http_client http({ yhttp::response{ 500, {}, "", "" } });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    auto s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::bad_gateway));
    REQUIRE(http.requests().size() == 1);
    REQUIRE(mod_rc->rc_map["xiva_list.production"]->complete_tasks == 1);
}

TEST_CASE("processor/subsctiptions/empty")
{
    auto task = make_task();
    task->events.push_back(make_event(action::MOVE_MAILS, R"([{"mid": "13"},{"mid": "14"}])"));
    fake_http_client http({ yhttp::response{ 200, {}, "[]", "" } });
    ;
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    auto s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 1);
    REQUIRE(mod_rc->rc_map.size() == 2);
    REQUIRE(mod_rc->rc_map["xiva_list.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["searchapp_send"]->complete_tasks == 1);
}

TEST_CASE("processor/all_ok")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK);
    REQUIRE(res.error_reason == "");

    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST, "" },
        yhttp::response{ 200, {}, COUNTERS, "" },
        yhttp::response{ 200, {}, R"({"envelopes": [)" + ENVELOPE_111 + "]}", "" },
        yhttp::response{ 200, {}, R"({"envelopes": []})", "" },
        yhttp::response{ 200, {}, R"({"envelopes": []})", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, R"({"envelopes": [)" + ENVELOPE_444 + "]}", "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 11);
    // List subscriptions once for all events.
    REQUIRE(http.requests()[0].url == "/list?service=mail&user=123");
    // Fetch counters once for all events.
    REQUIRE(http.requests()[1].url == "/counters?uid=123");
    // Fetch metadata for each mid in first event (replica-replica miss-master miss).
    REQUIRE(
        http.requests()[2].url ==
        "/meta?uid=123&dbtype=replica&full_folders_and_labels=1&mids=111");
    REQUIRE(
        http.requests()[3].url ==
        "/meta?uid=123&dbtype=replica&full_folders_and_labels=1&mids=222");
    REQUIRE(
        http.requests()[4].url == "/meta?uid=123&dbtype=master&full_folders_and_labels=1&mids=222");
    // Send notification for the first event.
    REQUIRE(
        http.requests()[5].url ==
        "/send?user=123&service=mail&event=move+mails&request_id=ctx_id_move_mails_http_webpush_"
        "websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[5].body);
    REQUIRE(json_parse(*http.requests()[5].body) == json_parse(MOVE_MAILS_MESSAGE));
    // Send tho notifications for the second event (no meta needed).
    REQUIRE(
        http.requests()[6].url ==
        "/send?user=123&service=mail&event=status+change&request_id=ctx_id_status_change_http_"
        "webpush_websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[6].body);
    REQUIRE(json_parse(*http.requests()[6].body) == json_parse(STATUS_CHANGE_MESSAGE));
    REQUIRE(
        http.requests()[7].url ==
        "/send?user=123&service=mail&event=update+labels&request_id=ctx_id_update_labels_http_"
        "webpush_websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[7].body);
    REQUIRE(json_parse(*http.requests()[7].body) == json_parse(UPDATE_LABELS_MESSAGE));
    // Fetch metadata for the third event.
    REQUIRE(
        http.requests()[8].url ==
        "/meta?uid=123&dbtype=replica&full_folders_and_labels=1&mids=444");
    // Fetch avatar for the third event.
    REQUIRE(http.requests()[9].url == "/ava");
    REQUIRE(http.requests()[9].body);
    REQUIRE(*http.requests()[9].body == "json=1&email=user@domain.ru");
    // Send notification for the third event.
    REQUIRE(
        http.requests()[10].url ==
        "/send?user=123&service=mail&event=insert&request_id=ctx_id_insert_http_webpush_websocket&"
        "source="
        "mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[10].body);
    REQUIRE(json_parse(*http.requests()[10].body) == json_parse(INSERT_MESSAGE));
    REQUIRE(mod_rc->rc_map.size() == 6);
    REQUIRE(mod_rc->rc_map["xiva_list.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["counters.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["meta"]->complete_tasks == 3);
    REQUIRE(mod_rc->rc_map["ava"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["xiva_send"]->complete_tasks == 3);
    REQUIRE(mod_rc->rc_map["searchapp_send"]->complete_tasks == 1);

    REQUIRE(dump_task(*task) == json_parse(R"({"events":[],"processed":3})"));
}

TEST_CASE("processor/metadata_grouping")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK);
    REQUIRE(res.error_reason == "");
    auto METADATA = R"({"envelopes": [)" + ENVELOPE_111 + "," + ENVELOPE_444 + "]}";

    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST, "" },
        yhttp::response{ 200, {}, COUNTERS, "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    s.meta.max_mids_in_filter_search_request = 3;

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    std::vector<string> expected_urls = {
        // List subscriptions once for all events.
        "/list?service=mail&user=123",
        // Fetch counters once for all events.
        "/counters?uid=123",
        // Fetch metadata for each mid in first event (replica-replica miss-master miss).
        "/meta?uid=123&dbtype=replica&full_folders_and_labels=1&mids=111&mids=222&mids=444",
        "/meta?uid=123&dbtype=master&full_folders_and_labels=1&mids=111&mids=222&mids=444",
        // Send notification for the first event.
        "/send?user=123&service=mail&event=move+mails&request_id=ctx_id_move_mails_http_webpush_"
        "websocket&"
        "source=mailpusher&session=&ttl=777",
        // Send tho notifications for the second event (no meta needed).
        "/send?user=123&service=mail&event=status+change&request_id=ctx_id_status_change_http_"
        "webpush_websocket&"
        "source=mailpusher&session=&ttl=777",
        "/send?user=123&service=mail&event=update+labels&request_id=ctx_id_update_labels_http_"
        "webpush_websocket&"
        "source=mailpusher&session=&ttl=777",
        // Fetch avatar for the third event.
        "/ava",
        // Send notification for the third event.
        "/send?user=123&service=mail&event=insert&request_id=ctx_id_insert_http_webpush_websocket&"
        "source="
        "mailpusher&session=&ttl=777",
    };
    std::vector<string> urls(http.requests().size());
    std::transform(http.requests().begin(), http.requests().end(), urls.begin(), [](auto& req) {
        return req.url;
    });
    REQUIRE(urls == expected_urls);
    REQUIRE(http.requests().size() == 9);
    // Send notification for the first event.
    REQUIRE(http.requests()[4].body);
    REQUIRE(json_parse(*http.requests()[4].body) == json_parse(MOVE_MAILS_MESSAGE));
    // Send tho notifications for the second event (no meta needed).
    REQUIRE(http.requests()[5].body);
    REQUIRE(json_parse(*http.requests()[5].body) == json_parse(STATUS_CHANGE_MESSAGE));
    REQUIRE(http.requests()[6].body);
    REQUIRE(json_parse(*http.requests()[6].body) == json_parse(UPDATE_LABELS_MESSAGE));
    // Fetch avatar for the third event.
    REQUIRE(http.requests()[7].body);
    REQUIRE(*http.requests()[7].body == "json=1&email=user@domain.ru");
    // Send notification for the third event.
    REQUIRE(http.requests()[8].body);
    REQUIRE(json_parse(*http.requests()[8].body) == json_parse(INSERT_MESSAGE));
    REQUIRE(mod_rc->rc_map.size() == 6);
    REQUIRE(mod_rc->rc_map["xiva_list.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["counters.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["meta"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["ava"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["xiva_send"]->complete_tasks == 3);
    REQUIRE(mod_rc->rc_map["searchapp_send"]->complete_tasks == 1);

    REQUIRE(dump_task(*task) == json_parse(R"({"events":[],"processed":3})"));
}

TEST_CASE("processor/metadata_error")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK);
    REQUIRE(res.error_reason == "");

    fake_http_client http({ yhttp::response{ 200, {}, SUB_LIST, "" },
                            yhttp::response{ 200, {}, COUNTERS, "" },
                            yhttp::response{ 500, {}, "", "" } });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::bad_gateway));
    REQUIRE(http.requests().size() == 3);
    // List subscriptions once for all events.
    REQUIRE(http.requests()[0].url == "/list?service=mail&user=123");
    // Fetch counters once for all events.
    REQUIRE(http.requests()[1].url == "/counters?uid=123");
    // Metadata error is global error.
    REQUIRE(
        http.requests()[2].url ==
        "/meta?uid=123&dbtype=replica&full_folders_and_labels=1&mids=111");
    REQUIRE(mod_rc->rc_map.size() == 3);
    REQUIRE(mod_rc->rc_map["xiva_list.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["counters.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["meta"]->complete_tasks == 1);
    // No item bodies in response as there's no additional data.
    REQUIRE(dump_task(*task) == json_parse(JSON_TASK_AFTER_METADATA_ERROR));
}

TEST_CASE("processor/ignore_avatar_and_counters_errors")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK);
    REQUIRE(res.error_reason == "");
    auto METADATA = R"({"envelopes": [)" + ENVELOPE_111 + "," + ENVELOPE_444 + "]}";

    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST, "" },
        yhttp::response{ 500, {}, "", "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 500, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    s.meta.max_mids_in_filter_search_request = 3;

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 9);
    // List subscriptions once for all events.
    REQUIRE(http.requests()[0].url == "/list?service=mail&user=123");
    // Fetch counters once for all events.
    REQUIRE(http.requests()[1].url == "/counters?uid=123");
    // Fetch metadata for each mid in first event (replica-replica miss-master miss).
    REQUIRE(
        http.requests()[2].url ==
        "/meta?uid=123&dbtype=replica&full_folders_and_labels=1&mids=111&mids=222&mids=444");
    REQUIRE(
        http.requests()[3].url ==
        "/meta?uid=123&dbtype=master&full_folders_and_labels=1&mids=111&mids=222&mids=444");
    // Send notification for the first event.
    REQUIRE(
        http.requests()[4].url ==
        "/send?user=123&service=mail&event=move+mails&request_id=ctx_id_move_mails_http_webpush_"
        "websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[4].body);
    REQUIRE(json_parse(*http.requests()[4].body) == json_parse(MOVE_MAILS_MESSAGE));
    // Send tho notifications for the second event (no meta needed).
    REQUIRE(
        http.requests()[5].url ==
        "/send?user=123&service=mail&event=status+change&request_id=ctx_id_status_change_http_"
        "webpush_websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[5].body);
    REQUIRE(json_parse(*http.requests()[5].body) == json_parse(STATUS_CHANGE_MESSAGE));
    REQUIRE(
        http.requests()[6].url ==
        "/send?user=123&service=mail&event=update+labels&request_id=ctx_id_update_labels_http_"
        "webpush_websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[6].body);
    REQUIRE(json_parse(*http.requests()[6].body) == json_parse(UPDATE_LABELS_MESSAGE));
    // Fetch avatar for the third event.
    REQUIRE(http.requests()[7].url == "/ava");
    REQUIRE(http.requests()[7].body);
    REQUIRE(*http.requests()[7].body == "json=1&email=user@domain.ru");
    // Send notification for the third event.
    REQUIRE(
        http.requests()[8].url ==
        "/send?user=123&service=mail&event=insert&request_id=ctx_id_insert_http_webpush_websocket&"
        "source="
        "mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[8].body);
    REQUIRE(json_parse(*http.requests()[8].body) == json_parse(INSERT_NO_AVATAR_MESSAGE));
    REQUIRE(mod_rc->rc_map.size() == 6);
    REQUIRE(mod_rc->rc_map["xiva_list.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["counters.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["meta"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["ava"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["xiva_send"]->complete_tasks == 3);
    REQUIRE(mod_rc->rc_map["searchapp_send"]->complete_tasks == 1);

    REQUIRE(dump_task(*task) == json_parse(R"({"events":[],"processed":3})"));
}

TEST_CASE("processor/notify_failed")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK);
    REQUIRE(res.error_reason == "");
    auto METADATA = R"({"envelopes": [)" + ENVELOPE_111 + "," + ENVELOPE_444 + "]}";

    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST, "" },
        yhttp::response{ 200, {}, COUNTERS, "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 500, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    s.meta.max_mids_in_filter_search_request = 3;

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::bad_gateway));
    REQUIRE(http.requests().size() == 9);
    // List subscriptions once for all events.
    REQUIRE(http.requests()[0].url == "/list?service=mail&user=123");
    // Fetch counters once for all events.
    REQUIRE(http.requests()[1].url == "/counters?uid=123");
    // Fetch metadata for each mid in first event (replica-replica miss-master miss).
    REQUIRE(
        http.requests()[2].url ==
        "/meta?uid=123&dbtype=replica&full_folders_and_labels=1&mids=111&mids=222&mids=444");
    REQUIRE(
        http.requests()[3].url ==
        "/meta?uid=123&dbtype=master&full_folders_and_labels=1&mids=111&mids=222&mids=444");
    // Send notification for the first event.
    REQUIRE(
        http.requests()[4].url ==
        "/send?user=123&service=mail&event=move+mails&request_id=ctx_id_move_mails_http_webpush_"
        "websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[4].body);
    REQUIRE(json_parse(*http.requests()[4].body) == json_parse(MOVE_MAILS_MESSAGE));
    // Send tho notifications for the second event (no meta needed).
    REQUIRE(
        http.requests()[5].url ==
        "/send?user=123&service=mail&event=status+change&request_id=ctx_id_status_change_http_"
        "webpush_websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[5].body);
    REQUIRE(json_parse(*http.requests()[5].body) == json_parse(STATUS_CHANGE_MESSAGE));
    REQUIRE(
        http.requests()[6].url ==
        "/send?user=123&service=mail&event=update+labels&request_id=ctx_id_update_labels_http_"
        "webpush_websocket&"
        "source=mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[6].body);
    REQUIRE(json_parse(*http.requests()[6].body) == json_parse(UPDATE_LABELS_MESSAGE));
    // Fetch avatar for the third event.
    REQUIRE(http.requests()[7].url == "/ava");
    REQUIRE(http.requests()[7].body);
    REQUIRE(*http.requests()[7].body == "json=1&email=user@domain.ru");
    // Send notification for the third event.
    REQUIRE(
        http.requests()[8].url ==
        "/send?user=123&service=mail&event=insert&request_id=ctx_id_insert_http_webpush_websocket&"
        "source="
        "mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[8].body);
    REQUIRE(json_parse(*http.requests()[8].body) == json_parse(INSERT_MESSAGE));
    REQUIRE(mod_rc->rc_map.size() == 5);
    REQUIRE(mod_rc->rc_map["xiva_list.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["counters.production"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["meta"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["ava"]->complete_tasks == 1);
    REQUIRE(mod_rc->rc_map["xiva_send"]->complete_tasks == 3);
    // Response contains items with additional metadata.
    REQUIRE(dump_task(*task) == json_parse(JSON_TASK_AFTER_NOTIFY_ERROR));
}

TEST_CASE("processor/second_notify_failed")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK);
    REQUIRE(res.error_reason == "");
    auto METADATA = R"({"envelopes": [)" + ENVELOPE_111 + "," + ENVELOPE_444 + "]}";
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST, "" },
        yhttp::response{ 200, {}, COUNTERS, "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 500, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    s.meta.max_mids_in_filter_search_request = 3;
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::bad_gateway));
    REQUIRE(http.requests().size() == 7);
    REQUIRE(json_parse(*http.requests()[5].body) == json_parse(STATUS_CHANGE_MESSAGE));
    REQUIRE(json_parse(*http.requests()[6].body) == json_parse(UPDATE_LABELS_MESSAGE));
    REQUIRE(dump_task(*task) == json_parse(JSON_TASK_WITH_SENT));
}

TEST_CASE("processor/does not resend notifications delivered previously")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK_AFTER_UPDATE_LABELS_FAIL);
    REQUIRE(res.error_reason == "");
    auto METADATA = R"({"envelopes": [)" + ENVELOPE_111 + "," + ENVELOPE_444 + "]}";
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST, "" },
        yhttp::response{ 200, {}, COUNTERS, "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, METADATA, "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    s.meta.max_mids_in_filter_search_request = 3;
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 6);
    REQUIRE(json_parse(*http.requests()[2].body) == json_parse(UPDATE_LABELS_MESSAGE));
    REQUIRE(json_parse(*http.requests()[5].body) == json_parse(INSERT_MESSAGE));
    REQUIRE(dump_task(*task) == json_parse(R"({"events":[],"processed":2})"));
}

TEST_CASE("processor/sends separate direct notifications for mobile transport")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK_STATUS_CHANGE);
    REQUIRE(res.error_reason == "");

    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST_WITH_MOBILE, "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    auto headers = std::get<string>(http.requests()[3].headers);
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 4);
    // List subscriptions once for all events.
    REQUIRE(http.requests()[0].url == "/list?service=mail&user=123");
    // Send notifications for the event (no meta needed).
    REQUIRE(json_parse(*http.requests()[1].body) == json_parse(STATUS_CHANGE_MESSAGE));
    REQUIRE(json_parse(*http.requests()[2].body) == json_parse(UPDATE_LABELS_MESSAGE));
    REQUIRE(json_parse(*http.requests()[3].body) == json_parse(STATUS_CHANGE_ANDROID_MESSAGE));
    REQUIRE(headers.find("X-DeliveryMode: direct") != string::npos);

    REQUIRE(dump_task(*task) == json_parse(R"({"events":[],"processed":1})"));
}

TEST_CASE("processor/fcm_insert_payload_and_repack")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK_INSERT);
    REQUIRE(res.error_reason == "");
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST_GCM, "" },
        yhttp::response{ 200, {}, COUNTERS, "" },
        yhttp::response{ 200, {}, R"({"envelopes": [)" + ENVELOPE_444 + "]}", "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 5);
    REQUIRE(
        http.requests()[4].url ==
        "/send?user=123&service=mail&event=insert&request_id=ctx_id_insert_mobile_fcm_hms&source="
        "mailpusher&"
        "session=&ttl=777");
    REQUIRE(http.requests()[4].body);
    REQUIRE(json_parse(*http.requests()[4].body) == json_parse(INSERT_ANDROID_MESSAGE));
    REQUIRE(dump_task(*task) == json_parse(R"({"events":[],"processed":1})"));
}

TEST_CASE("processor/apns_insert_payload_and_repack")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK_INSERT);
    REQUIRE(res.error_reason == "");
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST_APNS, "" },
        yhttp::response{ 200, {}, COUNTERS, "" },
        yhttp::response{ 200, {}, R"({"envelopes": [)" + ENVELOPE_444 + "]}", "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 6);
    // Bright.
    REQUIRE(
        http.requests()[4].url ==
        "/send?user=123&service=mail&event=insert&request_id=ctx_id_insert_mobile_apns_mob_870a4d_"
        "bright&source="
        "mailpusher&session=&ttl=777");
    REQUIRE(http.requests()[4].body);
    REQUIRE(json_parse(*http.requests()[4].body) == json_parse(INSERT_APNS_BRIGHT_MESSAGE));
    // Silent.
    REQUIRE(
        http.requests()[5].url ==
        "/send?user=123&service=mail&event=insert&request_id=ctx_id_insert_mobile_apns_silent&"
        "source=mailpusher&"
        "session=&ttl=0");
    REQUIRE(http.requests()[5].body);
    REQUIRE(json_parse(*http.requests()[5].body) == json_parse(INSERT_APNS_SILENT_MESSAGE));
    REQUIRE(dump_task(*task) == json_parse(R"({"events":[],"processed":1})"));
}

TEST_CASE("processor/gradual_rtec_3674_rollout")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK_INSERT);
    REQUIRE(res.error_reason == "");
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST_APNS, "" },
        yhttp::response{ 200, {}, COUNTERS, "" },
        yhttp::response{ 200, {}, R"({"envelopes": [)" + ENVELOPE_444 + "]}", "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    s.send.rtec_3674_rollout_percent = 0;
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 5);
    REQUIRE(
        http.requests()[4].url ==
        "/send?user=123&service=mail&event=insert&request_id=ctx_id_insert_mobile&source="
        "mailpusher&session=&"
        "ttl=777");
    REQUIRE(http.requests()[4].body);
    REQUIRE(
        json_parse(*http.requests()[4].body) == json_parse(INSERT_PRE_RTEC_3674_MOBILE_MESSAGE));
}

TEST_CASE("processor/apns_update_labels_status_change")
{
    auto task = make_task();
    auto res = parse_task(*task, JSON_TASK_STATUS_CHANGE);
    REQUIRE(res.error_reason == "");
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST_APNS, "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 3);
    // Status change.
    REQUIRE(
        http.requests()[1].url ==
        "/send?user=123&service=mail&event=status+change&request_id=ctx_id_status_change_mobile_"
        "apns&source="
        "mailpusher&session=&ttl=0");
    // Update labels.
    REQUIRE(
        http.requests()[2].url ==
        "/send?user=123&service=mail&event=update+labels&request_id=ctx_id_update_labels_mobile_"
        "apns&source="
        "mailpusher&session=&ttl=0");
    REQUIRE(http.requests()[2].body);
    REQUIRE(json_parse(*http.requests()[2].body) == json_parse(UPDATE_LABELS_APNS_MESSAGE));
    REQUIRE(dump_task(*task) == json_parse(R"({"events":[],"processed":1})"));
}

TEST_CASE("processor/apns_multiaccount_badge")
{
    auto task = make_task();
    parse_task(*task, JSON_TASK_INSERT);
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST_APNS_MULTIACCOUNT_123, "" },
        yhttp::response{ 200, {}, SUB_LIST_APNS_MULTIACCOUNT_321, "" },
        yhttp::response{ 200, {}, COUNTERS_123, "" },
        yhttp::response{ 200, {}, COUNTERS_321, "" },
        yhttp::response{ 200, {}, R"({"envelopes": [)" + ENVELOPE_444 + "]}", "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 8);
    // Fetch subscriptions/counters for 321 to compute badge.
    REQUIRE(http.requests()[1].url == "/list?service=mail&user=321");
    REQUIRE(http.requests()[3].url == "/counters?uid=321");
    // Send bright with badge.
    REQUIRE(
        json_parse(*http.requests()[6].body) == json_parse(INSERT_APNS_BRIGHT_MESSAGE_WITH_BADGE));
}

TEST_CASE("processor/apns_cross_environment_badge")
{
    auto task = make_task();
    parse_task(*task, JSON_TASK_INSERT);
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_LIST_APNS_CROSS_ENVIRONMENT_PROD, "" },
        yhttp::response{ 200, {}, SUB_LIST_APNS_CROSS_ENVIRONMENT_PROD, "" },
        yhttp::response{ 200, {}, SUB_LIST_APNS_CROSS_ENVIRONMENT_CORP, "" },
        yhttp::response{ 200, {}, COUNTERS_PROD, "" },
        yhttp::response{ 200, {}, COUNTERS_PROD, "" },
        yhttp::response{ 200, {}, COUNTERS_CORP, "" },
        yhttp::response{ 200, {}, R"({"envelopes": [)" + ENVELOPE_444 + "]}", "" },
        yhttp::response{ 200, {}, R"({"user@domain.ru": "avatar_url"})", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
    });
    auto mod_rc = std::make_shared<fake_rate_controller_module>();
    settings s = make_default_settings();
    s.list["production"].request = "/list_production";
    s.counters["production"].request = "/counters_production";
    s.list["corp"].request = "/list_corp";
    s.counters["corp"].request = "/counters_corp";
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(processor_t{ task, s, make_http_clients_map(http), mod_rc, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 10);
    // Fetch subscriptions/counters from prod to compute badge.
    REQUIRE(http.requests()[1].url == "/list_production?service=mail&user=prod");
    REQUIRE(http.requests()[4].url == "/counters_production?uid=prod");
    // Fetch subscriptions/counters from corp to compute badge.
    REQUIRE(http.requests()[2].url == "/list_corp?service=mail&user=corp");
    REQUIRE(http.requests()[5].url == "/counters_corp?uid=corp");
    // Send bright with badge.
    REQUIRE(
        json_parse(*http.requests()[8].body) == json_parse(INSERT_APNS_BRIGHT_MESSAGE_WITH_BADGE));
}