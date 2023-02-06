#pragma once

#include "processor/interface.h"
#include "processor/subscriber.h"
#include "web/xivaws_subscription_control.h"
#include <yplatform/module.h>

using namespace yxiva;
using namespace yxiva::web;

class mock_subscriber : public subscriber
{
public:
    void notify(const user_info& /*info*/, const message& /*message*/, const string& /*context_id*/)
        override
    {
        notify_calls++;
    }

    void notify_plain_text(const string&) override
    {
        notify_plain_text_calls++;
    }

    void notify_connected(const connected_message&) override
    {
        notify_connected_calls++;
    }

    void notify_position(const position_message&) override
    {
        notify_position_calls++;
    }

    void notify_disconnected(const disconnected_message&) override
    {
        notify_disconnected_calls++;
    }

    virtual task_context_ptr ctx() const override
    {
        return ctx_;
    }

    string id() const override
    {
        return ctx_->uniq_id();
    }

    unsigned notify_calls = 0;
    unsigned notify_plain_text_calls = 0;
    unsigned notify_connected_calls = 0;
    unsigned notify_position_calls = 0;
    unsigned notify_disconnected_calls = 0;

private:
    task_context_ptr ctx_{ new task_context };
};

struct mock_xivaws_subscriptions_storage : public yplatform::module
{
    void subscribe(
        task_context_ptr,
        const web_subscription&,
        std::function<void(boost::system::error_code, yhttp::response)> h)
    {
        subscribes++;
        handler = h;
    }

    void unsubscribe(
        task_context_ptr,
        const web_subscription&,
        std::function<void(boost::system::error_code, yhttp::response)> h)
    {
        unsubscribes++;
        handler = h;
    }

    unsigned subscribes = 0;
    unsigned unsubscribes = 0;
    std::function<void(boost::system::error_code, yhttp::response)> handler;
};

class mock_timers_storage : public boost::asio::io_service::service
{
public:
    static const boost::asio::io_service::id id;

    struct mock_timer
    {
        mock_timer(boost::asio::io_service& io)
        {
            auto& service = boost::asio::use_service<mock_timers_storage>(io);
            service.timers.push_back(this);
        }

        void expires_from_now(time_duration d)
        {
            duration = d;
        }

        template <typename Handler>
        void async_wait(Handler h)
        {
            handler = h;
        }

        void cancel()
        {
            handler = decltype(handler)();
        }

        time_duration duration;
        std::function<void(boost::system::error_code)> handler;
    };

    mock_timers_storage(boost::asio::io_service& io) : boost::asio::io_service::service(io)
    {
    }

    std::vector<mock_timer*> timers;
};

inline auto& find_mock_timers(boost::asio::io_service& io)
{
    auto& service = boost::asio::use_service<mock_timers_storage>(io);
    return service.timers;
}

using mock_timer = mock_timers_storage::mock_timer;

class mock_processor
    : public yplatform::module
    , public processor::processor
{
public:
    catalogue_ptr catalogue() override
    {
        return catalogue_;
    }

    authorizer_ptr authorizer() override
    {
        throw std::logic_error("not implemented");
    }

    std::shared_ptr<services::decoder_interface> decoder(const string& /*name*/) override
    {
        throw std::logic_error("not implemented");
    }

    formatters::kit_ptr formatters() override
    {
        throw std::logic_error("not implemented");
    }

    yxiva::processor::settings_ptr settings() override
    {
        return settings_;
    }

    catalogue_ptr catalogue_{ new yxiva::catalogue };
    yxiva::processor::settings_ptr settings_{ new struct yxiva::processor::settings };
};
