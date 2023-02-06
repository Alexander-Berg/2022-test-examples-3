#pragma once

#include <mocks/mock.h>
#include <api/api.h>
#include <yplatform/module.h>
#include <yplatform/find.h>
#include <memory>

namespace yrpopper::mock {

using namespace yrpopper::api;

class api
    : public yrpopper::api::api
    , public yrpopper::mock::mock
{

public:
    int enable_called_count = 0;
    int check_task_validity_called_count = 0;
    popid_t enable_requested_id = 0;
    promise_task_validity_t check_task_validity_result;

    api()
    {
    }

    void set_check_task_validity_result(const task_validity& validity)
    {
        check_task_validity_result.reset();
        check_task_validity_result.set(validity);
    }

    void set_check_task_validity_exception(const std::exception& exception)
    {
        check_task_validity_result.reset();
        check_task_validity_result.set_exception(exception);
    }

    void init_mock() override
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<api>("rpop_api", shared_from_this());

        check_task_validity_result.set(task_validity::task_pq_ok);
    }

    void reset() override
    {
        check_task_validity_result.reset();
        enable_called_count = 0;
        check_task_validity_called_count = 0;
        enable_requested_id = 0;
    }

    future_popid_t create(
        const yplatform::task_context_ptr&,
        const string&,
        const string&,
        const string&,
        const task_info&) override
    {
        return promise_popid_t{};
    }

    future_void_t remove(
        const yplatform::task_context_ptr&,
        const string&,
        const string&,
        const popid_t&) override
    {
        return promise_void_t{};
    }

    future_popid_t edit(
        const yplatform::task_context_ptr&,
        const string&,
        const string&,
        const string&,
        const task_info&,
        bool) override
    {
        return promise_popid_t{};
    }

    future_void_t enable(
        const yplatform::task_context_ptr&,
        const string&,
        const string&,
        const popid_t& id,
        bool) override
    {
        ++enable_called_count;
        enable_requested_id = id;
        return promise_void_t{};
    }

    future_void_t enable_abook(
        const yplatform::task_context_ptr&,
        const string&,
        const string&,
        const popid_t&) override
    {
        return promise_void_t{};
    }

    future_task_info_list list(
        const yplatform::task_context_ptr&,
        const string&,
        const string&,
        const popid_t&,
        bool) override
    {
        return promise_task_info_list{};
    }

    future_server_status check_server(
        const yplatform::task_context_ptr&,
        const string&,
        const string&,
        const string&,
        const task_info&) override
    {
        return promise_server_status{};
    }

    future_task_validity_t check_task_validity(
        const yplatform::task_context_ptr&,
        const string&,
        const string&,
        const base_task_info&) override
    {
        ++check_task_validity_called_count;
        return check_task_validity_result;
    }
};

}
