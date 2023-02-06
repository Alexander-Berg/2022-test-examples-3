#pragma once

#include <collector_ng/collector.h>

namespace yrpopper::mock {

using namespace yrpopper::collector;

class collector
    : public yrpopper::collector::Collector
    , public yrpopper::mock::mock
{
public:
    mutable int step_called_count = 0;
    PromiseStepResultPtr step_result;
    popid_t requested_popid = 0;

    boost::shared_ptr<rpop_context> context =
        boost::make_shared<rpop_context>(nullptr, "id", false, false, nullptr);

    collector()
    {
    }

    void set_step_result(StepResultPtr result)
    {
        step_result.reset();
        step_result.set(result);
    }

    void set_step_exception(const std::exception& exception)
    {
        step_result.reset();
        step_result.set_exception(exception);
    }

    void init_mock() override
    {
        step_result.set(std::make_shared<yrpopper::collector::StepResult>());
    }

    void reset() override
    {
        step_called_count = 0;
        requested_popid = 0;
        step_result.reset();
    }

    FutureStepResultPtr step() override
    {
        ++step_called_count;
        if (context->task != nullptr) requested_popid = context->task->popid;
        return step_result;
    }

    void stop() override
    {
    }

    rpop_context_ptr getContext() override
    {
        return context;
    }

    void updateContext(rpop_context_ptr context) override
    {
        this->context = context;
    }
};

}
