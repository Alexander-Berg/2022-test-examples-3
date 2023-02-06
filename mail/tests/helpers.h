#pragma once

#include <yxiva/core/message.h>
#include <yxiva/core/subscriptions.h>

using namespace yxiva;
using namespace yxiva::hub;

shared_ptr<message> make_message(
    const string& uid,
    const string& service,
    const local_id_t local_id,
    const string& operation)
{
    auto result = make_shared<message>();
    result->uid = uid;
    result->service = service;
    result->local_id = local_id;
    result->operation = operation;
    result->raw_data = "testdata";
    result->event_ts = 0;
    return result;
}

struct message_builder
{
    struct message& message;

    message_builder& tag(const string& tag)
    {
        message.tags.push_back(tag);
        return *this;
    }

    message_builder& ts(int ts)
    {
        message.event_ts = ts;
        return *this;
    }
};

struct subscription_builder
{
    struct sub_t& sub;

    subscription_builder& filter(const string& filter)
    {
        sub.filter = filter;
        return *this;
    }

    subscription_builder& init_time(std::time_t init_time)
    {
        sub.init_time = init_time;
        if (sub.ack_time == 0) sub.ack_time = init_time;
        return *this;
    }
};