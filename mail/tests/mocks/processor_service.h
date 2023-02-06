#pragma once

#include <mocks/mock.h>
#include <processor_ng/processor_service.h>
#include <yplatform/repository.h>

using namespace yrpopper::processor;

namespace yrpopper::mock {

class ProcessorService
    : public yplatform::module
    , public yrpopper::mock::mock
{
public:
    void init_mock() override
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<ProcessorService>("message_processor", shared_from_this());
    }

    Future<ProcessorResult> processMessage(rpop_context_ptr /*context*/, MessagePtr /*message*/)
    {
        if (exception)
        {
            std::rethrow_exception(exception);
        }
        Promise<ProcessorResult> prom;
        prom.set(result);
        return prom;
    }

    void setProcessMessageResult(const ProcessorResult& result)
    {
        if (exception) exception = nullptr;
        this->result = result;
    }

    void setProcessMessageResult(std::exception_ptr exception)
    {
        this->exception = exception;
    }

    std::exception_ptr exception;
    ProcessorResult result;
};

}
