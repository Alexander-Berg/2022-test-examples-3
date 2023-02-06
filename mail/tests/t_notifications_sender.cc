#include <log/equalizer.h>
#include <yxiva/core/types.h>
#include <ymod_httpclient/cluster_client.h>
#include <catch.hpp>
#include <thread>
#include <deque>

using namespace yxiva::equalizer;
using yxiva::json_value;

namespace ymod_httpclient {
std::ostream& operator<<(std::ostream& os, const request& r)
{
    static const std::vector<string> methods{ "GET", "HEAD", "POST" };
    os << methods[(std::size_t)r.method] << " " << r.url << "\r\n" << r.headers;
    return os;
}
}

struct mock_cluster_client
{
    using task_context_ptr = ymod_httpclient::task_context_ptr;
    using request = ymod_httpclient::request;
    using handler_type = ymod_httpclient::cluster_client::handler_type;
    using error_response = std::tuple<boost::system::error_code, yhttp::response>;

    boost::asio::io_service& io;
    std::vector<error_response> responses_to_send;
    std::optional<error_response> default_response;
    std::vector<yhttp::request> requests_received;

    mock_cluster_client(boost::asio::io_service& io) : io(io)
    {
    }

    void async_run(task_context_ptr /*ctx*/, request req, handler_type handler)
    {
        if (requests_received.size() < responses_to_send.size())
        {
            const auto& [ec, resp] = responses_to_send[requests_received.size()];
            io.post(std::bind(handler, ec, resp));
        }
        else if (default_response)
        {
            const auto& [ec, resp] = *default_response;
            io.post(std::bind(handler, ec, resp));
        }
        requests_received.push_back(req);
    }
};

struct mock_equalizer_log : public equalizer_log
{
    std::vector<operation_ptr> sent;
    std::vector<operation_ptr> ignored;
    std::vector<operation_ptr> dropped;

    static auto reset_instance()
    {
        return instance = std::make_shared<mock_equalizer_log>();
    }
    static auto get_instance()
    {
        return instance;
    }
    void operation_sent(const std::string& /*db*/, const operation_ptr& op) override
    {
        sent.push_back(op);
    }
    void operation_ignored(
        const std::string& /*db*/,
        const operation_ptr& op,
        const std::string& /*reason*/) override
    {
        ignored.push_back(op);
    }
    void operation_dropped(
        const std::string& /*db*/,
        const operation_ptr& op,
        const std::string& /*reason*/) override
    {
        dropped.push_back(op);
    }

private:
    static std::shared_ptr<mock_equalizer_log> instance;
};

std::shared_ptr<mock_equalizer_log> mock_equalizer_log::instance;

namespace yplatform {
template <>
std::shared_ptr<equalizer_log> find<equalizer_log, std::shared_ptr>(const string&)
{
    return mock_equalizer_log::reset_instance();
}
}

#include <equalizer/notifications_sender.h>

struct notifications_sender_fixture
{
    using notifications_sender = notifications_sender_template<mock_cluster_client>;
    boost::asio::io_service io;
    boost::asio::io_service::work work{ io };

    notifications_sender::settings settings{ "test/url",
                                             "Auth: mama_eto_ya",
                                             yxiva::milliseconds(1),
                                             100,
                                             3 };
    std::string db_name = "test_db";

    std::shared_ptr<mock_cluster_client> pusher = std::make_shared<mock_cluster_client>(io);
    std::shared_ptr<notifications_sender> test_subject =
        std::make_shared<notifications_sender>(io, settings, db_name, pusher);

    std::vector<operation_ptr> called_back;
    bool throw_in_callback{ false };

    void send_no_poll(const operation_ptr& op)
    {
        test_subject->send_notification(
            op, std::bind(&notifications_sender_fixture::on_sent, this, op));
    }

    void send(const operation_ptr& op)
    {
        send_no_poll(op);
        io.poll();
    }

    void on_sent(const operation_ptr& op)
    {
        called_back.push_back(op);
        if (throw_in_callback)
        {
            throw std::runtime_error("callback exception");
        }
    }

    bool callback_received(const operation_ptr& op)
    {
        auto it = std::find(called_back.begin(), called_back.end(), op);
        return it != called_back.end();
    }

    auto make_sender()
    {
        return test_subject = std::make_shared<notifications_sender>(io, settings, db_name, pusher);
    }

    auto make_operation(action_t a, const std::string& uid)
    {
        static std::size_t req_id = 0;

        auto ret = std::make_shared<operation>();
        ret->action_type = a;
        static_cast<std::string&>(ret->ui.uid) = uid;
        ret->x_request_id = std::to_string(req_id++);
        return ret;
    }

    auto make_operation_with_meta(action_t a, const std::string& uid, std::size_t meta_count)
    {
        auto ret = make_operation(a, uid);
        for (std::size_t i = 0; i < meta_count; ++i)
        {
            yxiva::json_value part;
            part["mid"] = std::to_string(i);
            ret->parts.push_back(part);
        }
        return ret;
    }

    auto get_logger()
    {
        return mock_equalizer_log::get_instance();
    }

#define REQUIRE_OP_IN(op, v)                                                                       \
    {                                                                                              \
        REQUIRE(v.size() > 0);                                                                     \
        auto it = std::find(v.begin(), v.end(), op);                                               \
        REQUIRE(it != v.end());                                                                    \
    }

#define REQUIRE_OP_NOT_IN(op, v)                                                                   \
    {                                                                                              \
        auto it = std::find(v.begin(), v.end(), op);                                               \
        REQUIRE(it == v.end());                                                                    \
    }

    void require_dropped(const operation_ptr& op)
    {
        REQUIRE_OP_IN(op, get_logger()->dropped);
        REQUIRE_OP_NOT_IN(op, get_logger()->sent);
        REQUIRE_OP_NOT_IN(op, get_logger()->ignored);
        REQUIRE(callback_received(op));
    }

    void require_sent(const operation_ptr& op)
    {
        REQUIRE_OP_NOT_IN(op, get_logger()->dropped);
        REQUIRE_OP_IN(op, get_logger()->sent);
        REQUIRE_OP_NOT_IN(op, get_logger()->ignored);
        REQUIRE(callback_received(op));
    }

    void require_ignored(const operation_ptr& op)
    {
        REQUIRE_OP_NOT_IN(op, get_logger()->dropped);
        REQUIRE_OP_NOT_IN(op, get_logger()->sent);
        REQUIRE_OP_IN(op, get_logger()->ignored);
        REQUIRE(callback_received(op));
    }

    void require_queued(const operation_ptr& op)
    {
        REQUIRE_OP_NOT_IN(op, get_logger()->dropped);
        REQUIRE_OP_NOT_IN(op, get_logger()->sent);
        REQUIRE_OP_NOT_IN(op, get_logger()->ignored);
        REQUIRE(!callback_received(op));
    }

    auto network_error_response()
    {
        return std::make_tuple(
            boost::system::error_code{ boost::asio::error::broken_pipe }, yhttp::response{});
    }

    auto http_error_response(int code)
    {
        return std::make_tuple(
            boost::system::error_code{}, yhttp::response{ code, {}, {}, "oops" });
    }

    auto http_ok_response(std::string body)
    {
        return std::make_tuple(boost::system::error_code{}, yhttp::response{ 200, {}, body, {} });
    }

    void wait_for_retry(int num_retries)
    {
        for (auto i = 0; i < num_retries * 10; ++i)
        {
            std::this_thread::sleep_for(settings.retry_interval / 10);
            io.poll();
        }
    }

    void require_single_retry(operation_ptr op = nullptr)
    {
        auto ok_response_body = R"literally(
            {
                "code": 200,
                "result": {
                    "processed" : 1
                }
            }
        )literally";
        if (op == nullptr)
        {
            op = make_operation(action_t::NEW_MAIL, "uid1");
        }
        // Assuming error response already pushed.
        pusher->default_response = http_ok_response(ok_response_body);
        settings.max_event_send_attempts = 10;
        make_sender();
        send(op);
        REQUIRE(pusher->requests_received.size() == 1);
        require_queued(op);
        wait_for_retry(1);
        REQUIRE(pusher->requests_received.size() == 2);
        require_sent(op);
    }
};

struct mock_pusher_task
{
    struct event
    {
        std::string x_request_id;
        json_value status;
        std::vector<json_value> items;
    };

    std::string uid;
    std::vector<event> events;
};

mock_pusher_task::event parse_event(const json_value& e)
{
    mock_pusher_task::event ret;
    ret.x_request_id = e["request_id"].to_string();
    ret.status = e["status"];
    for (auto&& i : e["items"].array_items())
    {
        ret.items.push_back(i);
    }
    return ret;
}

mock_pusher_task parse_pusher_req_body(const std::string& body)
{
    mock_pusher_task task;
    json_value task_json = yxiva::json_parse(body);
    task.uid = task_json["uid"].to_string();
    auto&& events_json = task_json["events"];
    for (auto&& e : events_json.array_items())
    {
        task.events.emplace_back(parse_event(e));
    }
    return task;
}

TEST_CASE_METHOD(notifications_sender_fixture, "notifications for unknown actions are not sent", "")
{
    auto op = std::make_shared<operation>();
    REQUIRE(op->action_type == action_t::UNKNOWN);
    send(op);
    REQUIRE(pusher->requests_received.size() == 0);
    require_dropped(op);
}

TEST_CASE_METHOD(notifications_sender_fixture, "correct pusher request is sent", "")
{
    auto op = make_operation(action_t::NEW_MAIL, "uid1");
    send(op);
    REQUIRE(pusher->requests_received.size() == 1);
    auto& req = pusher->requests_received[0];
    auto& headers = std::get<std::string>(req.headers);
    REQUIRE(req.method == yhttp::request::method_t::POST);
    REQUIRE(req.url == settings.url + "?uid=uid1&events_in_batch=1");
    REQUIRE(headers == "Content-Type: application/json\r\n" + settings.auth_header);
    auto task = yxiva::json_parse(*req.body);
    REQUIRE(task["uid"] == "uid1");
    REQUIRE(task["events"].size() == 1);
    auto&& event = task["events"][0UL];
    REQUIRE(event["action"] == "store");
    REQUIRE(event["change_id"].to_uint64() == op->operation_id);
    REQUIRE(event["args"] == op->args);
    REQUIRE(event["ts"] == op->ts);
    REQUIRE(event["lcn"] == op->lcn);
    REQUIRE(event["request_id"] == op->x_request_id);
    REQUIRE(event.has_member("status"));
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "notifications are dropped if queue limit from settings is exceeded",
    "")
{
    settings.user_queue_limit = 1;
    make_sender();
    // Not setting any expected responses in mock http
    // to emulate an unresponsive pusher and force ops
    // to be queued.
    auto op1 = make_operation(action_t::NEW_MAIL, "uid1");
    auto op2 = make_operation(action_t::NEW_MAIL, "uid1");
    send(op1);
    send(op2);
    REQUIRE(pusher->requests_received.size() == 1);
    require_queued(op1);
    require_dropped(op2);
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "notifications are dropped if max attempts limit from settings is exceeded",
    "")
{
    pusher->default_response = network_error_response();
    settings.max_event_send_attempts = 1;
    make_sender();
    auto op = make_operation(action_t::NEW_MAIL, "uid1");
    send(op);
    REQUIRE(pusher->requests_received.size() == 1);
    wait_for_retry(1);
    REQUIRE(pusher->requests_received.size() == 1);
    require_dropped(op);
}
TEST_CASE_METHOD(
    notifications_sender_fixture,
    "notifications are queued if there is a request to mailpusher in flight",
    "")
{
    // Not setting any responses in mock http
    // to emulate an unresponsive client.
    auto op1 = make_operation(action_t::NEW_MAIL, "uid1");
    auto op2 = make_operation(action_t::NEW_MAIL, "uid1");
    send(op1);
    REQUIRE(pusher->requests_received.size() == 1);
    send(op2);
    REQUIRE(pusher->requests_received.size() == 1);
    REQUIRE(!callback_received(op1));
    REQUIRE(!callback_received(op2));
}

TEST_CASE_METHOD(notifications_sender_fixture, "network failures are retried", "")
{
    pusher->responses_to_send.push_back(network_error_response());
    require_single_retry();
}

TEST_CASE_METHOD(notifications_sender_fixture, "500 codes are retried", "")
{
    pusher->responses_to_send.push_back(http_error_response(500));
    require_single_retry();
}

TEST_CASE_METHOD(notifications_sender_fixture, "invalid json in body is retried", "")
{
    pusher->responses_to_send.push_back(http_ok_response("{missing_closing_bracket"));
    require_single_retry();
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "responses with text instead of object are retried",
    "")
{
    pusher->responses_to_send.push_back(http_ok_response("123"));
    require_single_retry();
}

TEST_CASE_METHOD(notifications_sender_fixture, "responses without code in json are retried", "")
{
    pusher->responses_to_send.push_back(http_ok_response("{}"));
    require_single_retry();
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "responses with invalid code in json are retried",
    "")
{
    pusher->responses_to_send.push_back(http_ok_response(R"literally(
        { "code": "not_a_code" }
    )literally"));
    require_single_retry();
}

TEST_CASE_METHOD(notifications_sender_fixture, "on 205 from pusher notification is ignored", "")
{
    auto response_body = R"literally(
        { "code": 205 }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(response_body));
    make_sender();
    auto op = make_operation(action_t::NEW_MAIL, "uid1");
    send(op);
    REQUIRE(pusher->requests_received.size() == 1);
    require_ignored(op);
}

TEST_CASE_METHOD(notifications_sender_fixture, "500 from processor are retried", "")
{
    pusher->responses_to_send.push_back(http_ok_response(R"literally(
        { "code": 500 }
    )literally"));
    require_single_retry();
}

TEST_CASE_METHOD(notifications_sender_fixture, "missing result is retried", "")
{
    pusher->responses_to_send.push_back(http_ok_response(R"literally(
        { "code": 200 }
    )literally"));
    require_single_retry();
}

TEST_CASE_METHOD(notifications_sender_fixture, "missing processed count is retried", "")
{
    pusher->responses_to_send.push_back(http_ok_response(R"literally(
        {
            "code": 200,
            "result": {}
        }
    )literally"));
    require_single_retry();
}

TEST_CASE_METHOD(notifications_sender_fixture, "invalid processed count is retried", "")
{
    pusher->responses_to_send.push_back(http_ok_response(R"literally(
        {
            "code": 200,
            "result": {
                "processed": 100500
            }
        }
    )literally"));
    require_single_retry();
}

TEST_CASE_METHOD(notifications_sender_fixture, "incomplete task is retried immediately", "")
{
    auto incomplete_task_response_body = R"literally(
        {
            "code": 200,
            "result": {
                "processed" : 0
            }
        }
    )literally";
    auto ok_response_body = R"literally(
        {
            "code": 200,
            "result": {
                "processed" : 1
            }
        }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(incomplete_task_response_body));
    pusher->default_response = http_ok_response(ok_response_body);
    make_sender();
    auto op = make_operation(action_t::NEW_MAIL, "uid1");
    send(op);
    REQUIRE(pusher->requests_received.size() == 2);
    require_sent(op);
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "metadata for unsent events is sent with next request",
    "")
{
    auto xiva_fail_response_body = R"literally(
        {
            "code": 500,
            "result": {
                "processed": 0,
                "events": [
                    {
                        "status": {
                            "metadata_count": 2,
                            "avatar_fetched": 1,
                            "foo": "bar"
                        },
                        "items": [
                            {"meta": "data"},
                            {"other": "stuff"}
                        ]
                    }
                ]
            }
        }
    )literally";
    auto ok_response_body = R"literally(
        {
            "code": 200,
            "result": {
                "processed" : 1
            }
        }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(xiva_fail_response_body));
    pusher->default_response = http_ok_response(ok_response_body);
    make_sender();
    auto op = make_operation(action_t::NEW_MAIL, "uid1");
    yxiva::json_value part1;
    part1["meta"] = "123";
    part1["meta2"] = "456";
    op->parts.push_back(part1);
    yxiva::json_value part2;
    op->parts.push_back(part2);
    send(op);
    REQUIRE(pusher->requests_received.size() == 1);
    wait_for_retry(1);
    REQUIRE(pusher->requests_received.size() == 2);
    auto& req = pusher->requests_received[1];
    auto task = parse_pusher_req_body(*req.body);
    REQUIRE(task.events.size() == 1);
    auto& event = task.events[0];
    REQUIRE(event.status["avatar_fetched"].is_uint64());
    REQUIRE(event.status["metadata_count"].is_uint64());
    REQUIRE(event.status["avatar_fetched"].to_uint64() == 1);
    REQUIRE(event.status["metadata_count"].to_uint64() == 2);
    REQUIRE(event.status.has_member("foo"));
    REQUIRE(event.status["foo"].to_string() == "bar");
    REQUIRE(event.items.size() == 2);
    REQUIRE(event.items[0]["meta"].to_string() == "data");
    REQUIRE(event.items[0]["meta2"].to_string() == "456");
    REQUIRE(event.items[1]["other"].to_string() == "stuff");
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "processed > events.size() does not crash equalizer",
    "")
{
    auto xiva_fail_response_body = R"literally(
        {
            "code": 500,
            "result": {
                "processed": 100,
                "events": [
                    {
                        "status": {
                            "metadata_count": 2,
                            "avatar_fetched": 1
                        },
                        "items": [
                            {"meta": "data"}
                        ]
                    }
                ]
            }
        }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(xiva_fail_response_body));
    require_single_retry();
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "not-a-number in processed does not crash equalizer",
    "")
{
    auto xiva_fail_response_body = R"literally(
        {
            "code": 500,
            "result": {
                "processed": {"foo": "bar"},
                "events": [
                    {
                        "status": {
                            "metadata_count": 2,
                            "avatar_fetched": 1
                        },
                        "items": [
                            {"meta": "data"}
                        ]
                    }
                ]
            }
        }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(xiva_fail_response_body));
    require_single_retry();
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "events.size() > processed does not crash equalizer",
    "")
{
    auto xiva_fail_response_body = R"literally(
        {
            "code": 500,
            "result": {
                "processed": 1,
                "events": [
                    {
                        "status": {
                            "metadata_count": 1,
                            "avatar_fetched": 1
                        },
                        "items": [
                            {"meta": "data"}
                        ]
                    },
                    {
                        "status": {
                            "metadata_count": 1,
                            "avatar_fetched": 1
                        },
                        "items": [
                            {"abc": "def"}
                        ]
                    }
                ]
            }
        }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(xiva_fail_response_body));
    auto ok_response_body = R"literally(
        {
            "code": 200,
            "result": {
                "processed" : 1
            }
        }
    )literally";
    pusher->default_response = http_ok_response(ok_response_body);
    make_sender();
    auto op = make_operation(action_t::NEW_MAIL, "uid1");
    send(op);
    REQUIRE(pusher->requests_received.size() == 1);
    require_sent(op);
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "metadata_count < items.size() results in all parts being merged",
    "")
{
    auto xiva_fail_response_body = R"literally(
        {
            "code": 500,
            "result": {
                "processed": 0,
                "events": [
                    {
                        "status": {
                            "metadata_count": 0,
                            "avatar_fetched": 1
                        },
                        "items": [
                            {"meta": "data"}
                        ]
                    }
                ]
            }
        }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(xiva_fail_response_body));
    require_single_retry(make_operation_with_meta(action_t::NEW_MAIL, "uid1", 1));
    auto& req = pusher->requests_received[1];
    auto task = parse_pusher_req_body(*req.body);
    REQUIRE(task.events.size() == 1);
    auto& event = task.events[0];
    REQUIRE(event.status["avatar_fetched"].is_uint64());
    REQUIRE(event.status["metadata_count"].is_uint64());
    REQUIRE(event.status["avatar_fetched"].to_uint64() == 1);
    REQUIRE(event.status["metadata_count"].to_uint64() == 0);
    REQUIRE(event.items.size() == 1);
    REQUIRE(event.items[0]["meta"] == "data");
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "metadata_count > items.size() does not crash equalizer",
    "")
{
    auto xiva_fail_response_body = R"literally(
        {
            "code": 500,
            "result": {
                "processed": 0,
                "events": [
                    {
                        "status": {
                            "metadata_count": 100,
                            "avatar_fetched": 1
                        },
                        "items": [
                            {"meta": "data"}
                        ]
                    }
                ]
            }
        }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(xiva_fail_response_body));
    require_single_retry(make_operation_with_meta(action_t::NEW_MAIL, "uid1", 1));
    auto& req = pusher->requests_received[1];
    auto task = parse_pusher_req_body(*req.body);
    REQUIRE(task.events.size() == 1);
    auto& event = task.events[0];
    REQUIRE(event.status.empty());
    REQUIRE(event.items.size() == 1);
    REQUIRE(!event.items[0].has_member("meta"));
}

TEST_CASE_METHOD(
    notifications_sender_fixture,
    "not-a-number meta-count does not crash equalizer",
    "")
{
    auto xiva_fail_response_body = R"literally(
        {
            "code": 500,
            "result": {
                "processed": 0,
                "events": [
                    {
                        "status": {
                            "metadata_count": {"foo": "bar"},
                            "avatar_fetched": 1
                        },
                        "items": [
                            {"meta": "data"}
                        ]
                    }
                ]
            }
        }
    )literally";
    pusher->responses_to_send.push_back(http_ok_response(xiva_fail_response_body));
    require_single_retry(make_operation_with_meta(action_t::NEW_MAIL, "uid1", 1));
    auto& req = pusher->requests_received[1];
    auto task = parse_pusher_req_body(*req.body);
    REQUIRE(task.events.size() == 1);
    auto& event = task.events[0];
    REQUIRE(event.status.empty());
    REQUIRE(event.items.size() == 1);
    REQUIRE(!event.items[0].has_member("meta"));
}

TEST_CASE_METHOD(notifications_sender_fixture, "exception in callback is ignored", "")
{
    auto ok_response_body = R"literally(
        {
            "code": 200,
            "result": {
                "processed" : 1
            }
        }
    )literally";
    pusher->default_response = http_ok_response(ok_response_body);
    throw_in_callback = true;
    make_sender();
    auto op = make_operation(action_t::NEW_MAIL, "uid1");
    send(op);
    REQUIRE(pusher->requests_received.size() == 1);
    require_sent(op);
}
