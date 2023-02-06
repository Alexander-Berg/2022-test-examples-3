#include "mocks.h"
#include "hubrpc/impl.h"
#include "processor/subscriber.h"
#include "web/xivaws_subscription_control.h"
#include <catch.hpp>
#include <yplatform/module.h>

using namespace yxiva;

#define TEST_UID "111"
#define TEST_SERVICE "test"
#define TEST_UI user_info(user_id(TEST_UID))

struct test_dep_traits
{
    using xivaws_subscriptions_storage = mock_xivaws_subscriptions_storage;
    using timer = mock_timer;
};

using test_xivaws_subscription_control = xivaws_subscription_control<test_dep_traits>;

struct t_xivaws_subscription_control
{
    t_xivaws_subscription_control()
    {
        yplatform::register_module(
            io, "xivaws_subscriptions_storage", xivaws_subscriptions_storage);
        yplatform::repository::instance().add_service<processor::processor>("processor", processor);
        ws_log->init(yplatform::ptree{});
        yplatform::repository::instance().add_service<xivaws_log>("xiva_log", ws_log);
        channel_key key;
        key.service = TEST_SERVICE;
        channel_options opt;
        web_subscription sub = { TEST_UID, key, "id", "http://callback", 8 };
        channel = std::make_shared<test_xivaws_subscription_control>(io, sub, opt, subscriber);
    }

    ~t_xivaws_subscription_control()
    {
        channel->stop();
    }

    boost::asio::io_service io;
    task_context_ptr ctx{ new task_context };
    boost::shared_ptr<mock_subscriber> subscriber{ new mock_subscriber };
    std::shared_ptr<mock_xivaws_subscriptions_storage> xivaws_subscriptions_storage{
        new mock_xivaws_subscriptions_storage
    };
    std::shared_ptr<mock_processor> processor{ new mock_processor };
    std::shared_ptr<xivaws_log> ws_log{ new xivaws_log };
    std::shared_ptr<test_xivaws_subscription_control> channel;
};

TEST_CASE_METHOD(t_xivaws_subscription_control, "subscription_control/nothing_happens_on_idle")
{
    io.run();
    REQUIRE(xivaws_subscriptions_storage->subscribes == 0);
    REQUIRE(xivaws_subscriptions_storage->unsubscribes == 0);
    REQUIRE(subscriber->notify_connected_calls == 0);
    REQUIRE(subscriber->notify_disconnected_calls == 0);
}

TEST_CASE_METHOD(t_xivaws_subscription_control, "subscription_control/subscribe_on_run")
{
    channel->start();
    io.run();
    REQUIRE(xivaws_subscriptions_storage->subscribes == 1);
    REQUIRE(xivaws_subscriptions_storage->unsubscribes == 0);
    REQUIRE(subscriber->notify_connected_calls == 0);
    REQUIRE(subscriber->notify_disconnected_calls == 0);
}

TEST_CASE_METHOD(
    t_xivaws_subscription_control,
    "subscription_control/dont_notify_before_subscribe_finished")
{
    channel->start();
    io.run();
    REQUIRE(subscriber->notify_connected_calls == 0);
    REQUIRE(subscriber->notify_disconnected_calls == 0);
}

TEST_CASE_METHOD(
    t_xivaws_subscription_control,
    "subscription_control/notify_connected_on_successfully_subscribed")
{
    channel->start();
    xivaws_subscriptions_storage->handler({}, yhttp::response{ 200, {}, "", "" });
    io.run();
    REQUIRE(subscriber->notify_connected_calls == 1);
    REQUIRE(subscriber->notify_disconnected_calls == 0);
}

TEST_CASE_METHOD(t_xivaws_subscription_control, "subscription_control/resubscribe")
{
    channel->start();
    xivaws_subscriptions_storage->handler({}, yhttp::response{ 200, {}, "", "" });
    io.run();
    REQUIRE(find_mock_timers(io).size() == 1);
    find_mock_timers(io)[0]->handler({});
    io.run();
    REQUIRE(xivaws_subscriptions_storage->subscribes == 2);
    REQUIRE(xivaws_subscriptions_storage->unsubscribes == 0);
}

TEST_CASE_METHOD(t_xivaws_subscription_control, "subscription_control/notify_connected_only_once")
{
    channel->start();
    xivaws_subscriptions_storage->handler({}, yhttp::response{ 200, {}, "", "" });
    io.run();
    REQUIRE(find_mock_timers(io).size() == 1);
    find_mock_timers(io)[0]->handler({});
    io.run();
    xivaws_subscriptions_storage->handler({}, yhttp::response{ 200, {}, "", "" });
    io.run();
    REQUIRE(subscriber->notify_connected_calls == 1);
}

TEST_CASE_METHOD(
    t_xivaws_subscription_control,
    "subscription_control/notify_disconnected_on_forbidden")
{
    channel->start();
    xivaws_subscriptions_storage->handler({}, yhttp::response{ 403, {}, "", "" });
    io.run();
    REQUIRE(subscriber->notify_connected_calls == 0);
    REQUIRE(subscriber->notify_disconnected_calls == 1);
}

TEST_CASE_METHOD(
    t_xivaws_subscription_control,
    "subscription_control/dont_notify_on_subscribe_fail")
{
    channel->start();
    xivaws_subscriptions_storage->handler({}, yhttp::response{ 500, {}, "", "" });
    io.run();
    REQUIRE(subscriber->notify_connected_calls == 0);
    REQUIRE(subscriber->notify_disconnected_calls == 0);
}

TEST_CASE_METHOD(t_xivaws_subscription_control, "subscription_control/unsubscribe_on_stop")
{
    channel->start();
    channel->stop();
    io.run();
    REQUIRE(xivaws_subscriptions_storage->subscribes == 1);
    REQUIRE(xivaws_subscriptions_storage->unsubscribes == 1);
    REQUIRE(subscriber->notify_connected_calls == 0);
    REQUIRE(subscriber->notify_disconnected_calls == 1);
}

TEST_CASE_METHOD(t_xivaws_subscription_control, "subscription_control/add_subscriber_on_start")
{
    channel->start();
    auto catalogue = find_processor()->catalogue();
    REQUIRE(catalogue != nullptr);
    CHECK(catalogue->stat()->get_subscribers() == 1);
}

TEST_CASE_METHOD(t_xivaws_subscription_control, "subscription_control/add_subscriber_on_stop")
{
    channel->start();
    channel->stop();
    auto catalogue = find_processor()->catalogue();
    REQUIRE(catalogue != nullptr);
    CHECK(catalogue->stat()->get_subscribers() == 0);
}
