#pragma once

#include "helpers.h"
#include <processor/settings.h>
#include <processor/counters_fetcher.h>
#include <yplatform/coroutine.h>

#include <boost/asio/yield.hpp>

struct fake_counters_fetcher
{
    using yield_context_t = yplatform::yield_context<fake_counters_fetcher>;
    using counters_t = yxiva::mailpusher::counters_fetcher_impl<fake_http_client>::counters_t;
    using counters_callback_t =
        yxiva::mailpusher::counters_fetcher_impl<fake_http_client>::counters_callback_t;
    using response_t = std::tuple<error_code, counters_t>;

    yplatform::task_context_ptr ctx;
    string uid;
    ymod_ratecontroller::rate_controller_ptr rate_controller;
    fake_http_client& http_client;
    const struct settings::counters& settings;
    counters_callback_t cb;

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
