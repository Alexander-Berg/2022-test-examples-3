#pragma once

#include <ymod_pq/call.h>
#include "settings.h"

using namespace ymod_pq;

struct mock_call : public call
{
    using response = std::function<void(const response_handler_ptr&)>;

    struct request_params
    {
        yplatform::task_context_ptr ctx;
        std::string db;
        std::string request_text;
        bind_array_ptr bind_vars;
        response_handler_ptr handler;
        bool log_timings;
        yplatform::time_traits::duration deadline;
    };

    struct update_params
    {
        yplatform::task_context_ptr ctx;
        std::string db;
        std::string request_text;
        bind_array_ptr bind_vars;
        bool log_timings;
        yplatform::time_traits::duration deadline;
    };

    struct execute_params
    {
        yplatform::task_context_ptr ctx;
        std::string db;
        std::string request_text;
        bind_array_ptr bind_vars;
        bool log_timings;
        yplatform::time_traits::duration deadline;
    };

    settings settings;
    std::vector<request_params> requests;
    response default_response = [](const response_handler_ptr&) {};
    std::vector<update_params> updates;
    std::vector<execute_params> executes;
    bool fail{ false };
    double error_rate{ 0.0 };

    mock_call() = default;

    mock_call(boost::asio::io_service&, const struct settings& settings) : settings(settings)
    {
    }

    virtual future_result request(
        yplatform::task_context_ptr ctx,
        const std::string& db,
        const std::string& request,
        bind_array_ptr bind_vars,
        response_handler_ptr handler,
        bool log_timings,
        const yplatform::time_traits::duration& deadline) override
    {
        promise_result prom;
        requests.push_back(
            request_params{ ctx, db, request, bind_vars, handler, log_timings, deadline });
        default_response(handler);
        if (fail)
        {
            prom.set_exception(std::runtime_error("mock_call exception"));
        }
        else
        {
            prom.set(true);
        }
        return prom;
    }

    virtual future_up_result update(
        yplatform::task_context_ptr ctx,
        const std::string& db,
        const std::string& request,
        bind_array_ptr bind_arr,
        bool log_timings,
        const yplatform::time_traits::duration& deadline) override
    {
        updates.push_back(update_params{ ctx, db, request, bind_arr, log_timings, deadline });
        return future_up_result{};
    }

    virtual future_result execute(
        yplatform::task_context_ptr ctx,
        const std::string& db,
        const std::string& request,
        bind_array_ptr bind_arr,
        bool log_timings,
        const yplatform::time_traits::duration& deadline) override
    {
        executes.push_back(execute_params{ ctx, db, request, bind_arr, log_timings, deadline });
        return future_result{};
    }

    yplatform::ptree get_stats() const
    {
        yplatform::ptree ret;
        ret.put("conninfo", settings.conninfo);
        ret.put("foo", "bar");
        return ret;
    }

    double get_error_rate() const
    {
        return error_rate;
    }
};

struct mock_response_handler : public response_handler
{
public:
    virtual void handle_cell(
        unsigned /*row*/,
        unsigned /*col*/,
        const std::string& /*value*/,
        bool /*is_null*/) override{};
    virtual unsigned column_count() const override
    {
        return 0;
    }
};
