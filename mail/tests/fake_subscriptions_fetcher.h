#pragma once

#include "helpers.h"
#include <processor/settings.h>
#include <yplatform/coroutine.h>

#include <boost/asio/yield.hpp>

struct fake_subscription_fetcher
{
    using yield_context_t = yplatform::yield_context<fake_subscription_fetcher>;
    using subscriptions_callback_t =
        std::function<void(const error_code&, const std::vector<subscription>&)>;
    using response_t = std::tuple<error_code, std::vector<subscription>>;

    yplatform::task_context_ptr ctx;
    string uid;
    ymod_ratecontroller::rate_controller_ptr rate_controller;
    fake_http_client& http_client;
    const struct settings::list& settings;
    subscriptions_callback_t cb;

    static auto& fetched_uids()
    {
        static std::vector<string> uids;
        return uids;
    }

    static auto& responses_by_uid()
    {
        static std::map<string, response_t> responses;
        return responses;
    }

    void operator()(yield_context_t yield_context)
    {
        reenter(yield_context)
        {
            fetched_uids().push_back(uid);
            yplatform::util::call_with_tuple_args(cb, responses_by_uid()[uid]);
        }
    }
};

#include <boost/asio/unyield.hpp>
