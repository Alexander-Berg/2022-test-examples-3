#pragma once

#include "../core/user_handler.h"

namespace yxiva { namespace processor {

struct mock_user_handler : public user_handler_interface
{

    task_context_ptr ctx_;

    mock_user_handler(user_info const& ui) : ctx_(new untyped_ctx), ui_(ui)
    {
    }

    task_context_ptr ctx()
    {
        return ctx_;
    }

    MOCK_METHOD(void, notify, (const message& msg), (override));
    MOCK_METHOD(
        void,
        notify_disconnected_by_storage,
        (const service_name& service, const string& storage),
        (override));
    MOCK_METHOD(void, notify_disconnected_all, (), (override));

    size_t subscribe(std::vector<request> const& req, subscriber_ptr subscriber)
    {
        count_ += req.size();
        return req.size();
    }

    size_t unsubscribe(std::vector<request> const& req, subscriber_ptr subscriber)
    {
        count_ -= req.size();
        return req.size();
    }

    void clear()
    {
        count_ = 0;
    }

    MOCK_METHOD(void, set_timer, (subscription & subscription), (override));

    MOCK_METHOD(void, cancel_timer, (subscription & info), (override));
    MOCK_METHOD(size_t, subscriptions_count, (), (const, override));
    MOCK_METHOD(size_t, subscribers_count, (), (const, override));

    size_t size() const
    {
        return count_;
    }

    const user_info& get_user_info() const
    {
        return ui_;
    }

    size_t count_;
    user_info ui_;
};

}}
