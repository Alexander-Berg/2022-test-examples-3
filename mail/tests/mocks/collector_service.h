#pragma once

#include <mocks/mock.h>
#include <mocks/collector.h>
#include <collector_ng/collector_service.h>
#include <collector_ng/imap/collector_session_imap.h>

namespace yrpopper::mock {

using namespace yrpopper::collector;

class collector_service
    : public yrpopper::collector::CollectorService
    , public yrpopper::mock::mock
{
public:
    int get_collector_called_count = 0;
    std::shared_ptr<yrpopper::mock::collector> collector =
        std::make_shared<yrpopper::mock::collector>();

    collector_service() : CollectorService()
    {
    }

    void init_mock() override
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<collector_service>("collector_service", shared_from_this());

        collector->init_mock();
    }

    void reset() override
    {
        get_collector_called_count = 0;
        collector->reset();
    }

    std::shared_ptr<Collector> getCollector(rpop_context_ptr ctx) override
    {
        ++get_collector_called_count;
        collector->context = ctx;
        return collector;
    }
};

}
