#pragma once

#include <mocks/mock.h>
#include <db/query_dispatcher/query_dispatcher.h>
#include <yplatform/repository.h>

namespace yrpopper::mock {

class query_dispatcher
    : public yrpopper::db::query_dispatcher
    , public yrpopper::mock::mock
{
public:
    query_dispatcher() : yrpopper::db::query_dispatcher(yrpopper::db::settings{})
    {
    }

    void init_mock() override
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<yrpopper::db::query_dispatcher>(
            "query_dispatcher", yplatform::shared_from(this));
    }
};

}
