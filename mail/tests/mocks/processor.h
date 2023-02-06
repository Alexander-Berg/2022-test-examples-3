#pragma once

#include <mocks/mock.h>
#include <processor/processor.h>

namespace yrpopper::mock {

using namespace yrpopper::processor;

class processor
    : public yrpopper::processor::processor
    , public yrpopper::mock::mock
{
public:
    int process_called_count = 0;
    promise_task_status_ptr process_result;
    popid_t process_requested_id = 0;
    rpop_context_ptr requested_context;

    processor() : yrpopper::processor::processor()
    {
        this->init_pool();
    }

    void init_mock() override
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<processor>("rpop_processor", shared_from_this());

        process_result.set(boost::make_shared<task_status>());
    }

    void reset() override
    {
        process_called_count = 0;
        process_requested_id = 0;
        process_result.reset();
    }

    future_task_status_ptr process(rpop_context_ptr ctx, const yplatform::active::ptime&) override
    {
        ++process_called_count;
        requested_context = ctx;
        process_requested_id = ctx->task->popid;
        return process_result;
    }
};

}
