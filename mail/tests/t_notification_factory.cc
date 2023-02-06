#include "catch.hpp"
#include "helpers.h"
#include <processor/notification_factory.h>

struct notification_factory_fixture
{
    notification_factory factory;
    shared_ptr<task> task;
    settings settings;

    notification_factory_fixture()
    {
        task = make_task();
        task->events.push_back(make_event(action::MOVE_MAILS, "[]"));
        settings.send.rtec_3674_rollout_percent = 100;
    }
    auto make_notifications()
    {
        return factory.make(task, task->events.back(), settings);
    }
};

namespace yxiva::mailpusher {
inline std::ostream& operator<<(std::ostream& os, const destination& d)
{
    os << "transport:";
    for (auto& t : d.transport)
    {
        os << " " << t;
    }
    os << " platform:";
    for (auto& t : d.platform)
    {
        os << " " << t;
    }
    return os;
}
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/separates notifications for mobile and non-mobile subscriptions")
{
    task->subscriptions = { mobile_builder().id("1").build(), http_builder().id("2").build() };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 2);
    REQUIRE(notifications[0].id == "move mails mobile fcm hms");
    REQUIRE(notifications[0].destination == DESTINATION_ANDROID);
    REQUIRE(notifications[0].delivery == notification::delivery_mode::direct);
    REQUIRE(notifications[1].id == "move mails http webpush websocket");
    REQUIRE(notifications[1].destination == DESTINATION_EXCEPT_MOBILE);
    REQUIRE(notifications[1].delivery == notification::delivery_mode::queued);
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/makes a single notification for all mobile subscriptions")
{
    task->subscriptions = { mobile_builder().id("1").build(),
                            mobile_builder().id("2").build(),
                            mobile_builder().id("3").build() };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 1);
    REQUIRE(notifications[0].destination == DESTINATION_ANDROID);
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/makes a single notification for all non-mobile subscriptions")
{
    task->subscriptions = { http_builder().id("1").build(),
                            webpush_builder().id("2").build(),
                            http_builder().id("3").url("xivaws://something").build() };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 1);
    REQUIRE(notifications[0].destination == DESTINATION_EXCEPT_MOBILE);
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/ignores unsupported operations for fcm")
{
    task->subscriptions = { http_builder().id("1").build(),
                            mobile_builder().id("2").platform("fcm").build() };
    SECTION("update labels")
    {
        task->events = { make_event(CHANGE_STATUS, "[]") };
        auto notifications = make_notifications();
        REQUIRE(notifications.size() == 3);
        REQUIRE(notifications[0].operation() == "status change");
        REQUIRE(notifications[0].destination == DESTINATION_EXCEPT_MOBILE);
        REQUIRE(notifications[1].operation() == "update labels");
        REQUIRE(notifications[1].destination == DESTINATION_EXCEPT_MOBILE);
        REQUIRE(notifications[2].operation() == "status change");
        REQUIRE(notifications[2].destination == DESTINATION_ANDROID);
    }
    SECTION("delete mails")
    {
        task->events = { make_event(DELETE_MAILS, "[]") };
        auto notifications = make_notifications();
        REQUIRE(notifications.size() == 3);
        REQUIRE(notifications[0].operation() == "move mails");
        REQUIRE(notifications[0].destination == DESTINATION_EXCEPT_MOBILE);
        REQUIRE(notifications[1].operation() == "delete mails");
        REQUIRE(notifications[1].destination == DESTINATION_EXCEPT_MOBILE);
        REQUIRE(notifications[2].operation() == "move mails");
        REQUIRE(notifications[2].destination == DESTINATION_ANDROID);
    }
    SECTION("reset fresh")
    {
        task->events = { make_event(RESET_FRESH, "[]") };
        auto notifications = make_notifications();
        REQUIRE(notifications.size() == 1);
        REQUIRE(notifications[0].operation() == "reset fresh");
        REQUIRE(notifications[0].destination == DESTINATION_EXCEPT_MOBILE);
    }
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/ignores unsupported operations for apns")
{
    task->subscriptions = {
        http_builder().id("1").build(),
        mobile_builder().id("2").platform("apns").build(),
    };
    SECTION("delete mails")
    {
        task->events = { make_event(DELETE_MAILS, "[]") };
        auto notifications = make_notifications();
        REQUIRE(notifications.size() == 3);
        REQUIRE(notifications[0].operation() == "move mails");
        REQUIRE(notifications[0].destination == DESTINATION_EXCEPT_MOBILE);
        REQUIRE(notifications[1].operation() == "delete mails");
        REQUIRE(notifications[1].destination == DESTINATION_EXCEPT_MOBILE);
        REQUIRE(notifications[2].operation() == "move mails");
        REQUIRE(notifications[2].destination == DESTINATION_APNS);
    }
    SECTION("reset fresh")
    {
        task->events = { make_event(RESET_FRESH, "[]") };
        auto notifications = make_notifications();
        REQUIRE(notifications.size() == 1);
        REQUIRE(notifications[0].operation() == "reset fresh");
        REQUIRE(notifications[0].destination == DESTINATION_EXCEPT_MOBILE);
    }
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/silent apns are marked as such")
{
    task->subscriptions = {
        mobile_builder().platform("apns").build(),
    };
    task->events = { make_event(MOVE_MAILS, R"([])") };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 1);
    REQUIRE(notifications[0].silent);
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/apns insert produces 2 notifications: bright and silent")
{
    task->subscriptions = { mobile_builder().id("id1").platform("apns").build() };
    task->events = { make_event(NEW_MAIL, "[]") };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 2);
    auto& bright = notifications[0];
    REQUIRE(bright.id == "insert mobile apns id1 bright");
    REQUIRE(bright.repack["apns"]["aps"]["alert"].is_object());
    auto& silent = notifications[1];
    REQUIRE(silent.id == "insert mobile apns silent");
    REQUIRE(silent.repack["apns"]["aps"]["content-available"] == 1);
}

TEST_CASE_METHOD(notification_factory_fixture, "notification_factory/apns bright insert static aps")
{
    task->subscriptions = { mobile_builder().id("id1").platform("apns").build() };
    task->events = { make_event(NEW_MAIL, "[]") };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 2);
    auto& bright = notifications[0];
    REQUIRE(bright.repack["apns"]["aps"]["category"] == "M");
    REQUIRE(bright.repack["apns"]["aps"]["mutable-content"] == 1);
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/bright_aps/fills sound from extra data")
{
    const auto SOUND_EXTRA_1 = R"(
        { "sound": "p.woof" }
    )";
    const auto SOUND_EXTRA_2 = "sound.proof";
    task->subscriptions = {
        mobile_builder().platform("apns").id("id1").extra(SOUND_EXTRA_1).build(),
        mobile_builder().platform("apns").id("id2").extra(SOUND_EXTRA_2).build(),
        mobile_builder().platform("apns").id("id3").extra("").build(),
    };
    task->events = { make_event(NEW_MAIL, "[]") };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 4); // 1 bright per sub, 1 silent.
    REQUIRE(notifications[0].repack["apns"]["aps"]["sound"].to_string() == "p.woof");
    REQUIRE(notifications[2].repack["apns"]["aps"]["sound"].to_string() == "proof");
    REQUIRE(notifications[3].repack["apns"]["aps"]["sound"].to_string() == "p.caf");
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/bright_aps/fills loc-args depending on ios version")
{
    static const string IOS_10_CLIENT = "ru_yandex_mail_30821_605__iPhone9_3__iOS_12_2_";
    static const string IOS_9_CLIENT = "ru_yandex_mail_355_433__iPad2_2__iPhone_OS_9_3_5_";
    static const string LOC_ARGS_ITEMS = R"([{
        "from": [
            {
                "local":"user",
                "domain":"domain.ru",
                "displayName":"Jack Daniels"
            }
        ],
        "subject": "Hi Jack",
        "firstline": "test test"
    }])";
    static const string IOS_10_ALERT = R"({
        "title": "Jack Daniels",
        "subtitle": "Hi Jack",
        "body": "test test"
    })";
    static const string IOS_9_ALERT = R"({
        "loc-key": "p",
        "loc-args": [
            "Jack Daniels",
            "Hi Jack",
            "test test"
        ]
    })";
    task->subscriptions = {
        mobile_builder().platform("apns").id("id1").client(IOS_10_CLIENT).build(),
        mobile_builder().platform("apns").id("id2").client(IOS_9_CLIENT).build(),
    };
    task->events = { make_event(NEW_MAIL, LOC_ARGS_ITEMS) };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 3);
    REQUIRE(notifications[0].repack["apns"]["aps"]["alert"] == json_parse(IOS_10_ALERT));
    REQUIRE(notifications[2].repack["apns"]["aps"]["alert"] == json_parse(IOS_9_ALERT));
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/bright_insert/not sent when filtered")
{
    static const auto IGNORED_FOLDERS_FILTER = R"({
        "rules":[{"do":"send_silent","if":"EXFID"}],
        "vars":{"EXFID":{"fid":{"$eq":["12"]}}}
    })";
    task->subscriptions = {
        mobile_builder().platform("apns").filter(IGNORED_FOLDERS_FILTER).build()
    };
    task->events = { make_event(NEW_MAIL, R"([{"fid": 12}])") };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 1);
    REQUIRE(notifications[0].id.find("silent") != string::npos);
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/bright_insert/not sent when marked read")
{
    task->subscriptions = { mobile_builder().platform("apns").build() };
    task->events = { make_event(NEW_MAIL, R"([{"seen": true}])") };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 1);
    REQUIRE(notifications[0].id.find("silent") != string::npos);
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/bright_insert/not sent when fid_type is not allowed")
{
    task->subscriptions = { mobile_builder().platform("apns").build() };
    task->events = { make_event(NEW_MAIL, R"([
        {
            "folder": {
                "symbolicName": {
                    "code": 7
                }
            }
        }
    ])") };
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 1);
    REQUIRE(notifications[0].id.find("silent") != string::npos);
}

TEST_CASE_METHOD(
    notification_factory_fixture,
    "notification_factory/bright_insert/adds collapse-id")
{
    task->subscriptions = {
        mobile_builder().platform("apns").build(),
    };
    task->events = { make_event(NEW_MAIL, "[]") };
    task->events.back().uid = "111";
    task->events.back().lcn = "222";
    auto notifications = make_notifications();
    REQUIRE(notifications.size() == 2); // 1 bright per sub, 1 silent.
    REQUIRE(notifications[0].repack["apns"]["collapse-id"].to_string() == "111_222");
    REQUIRE(notifications[1].repack["apns"]["collapse-id"].is_null());
}

TEST_CASE("rtec_3674_rollout_percent")
{
    REQUIRE(not hacks::roll_rtec_3674("0", 0));
    REQUIRE(hacks::roll_rtec_3674("0", 1));
    REQUIRE(not hacks::roll_rtec_3674("19", 19));
    REQUIRE(hacks::roll_rtec_3674("19", 20));
    REQUIRE(hacks::roll_rtec_3674("1119", 20));
    // Invalid uids should collapse into 0
    REQUIRE(not hacks::roll_rtec_3674({}, 0));
    REQUIRE(hacks::roll_rtec_3674({}, 1));
    REQUIRE(not hacks::roll_rtec_3674("", 0));
    REQUIRE(hacks::roll_rtec_3674("", 1));
    REQUIRE(not hacks::roll_rtec_3674("zzz", 0));
    REQUIRE(hacks::roll_rtec_3674("zzz", 1));
}