#pragma once

#include <common/types.h>

namespace fan {

struct fake_timer
{
    without_data_cb cb;

    fake_timer(boost::asio::io_service&)
    {
    }

    void expires_from_now(duration /*d*/)
    {
    }

    void async_wait(without_data_cb cb)
    {
        this->cb = cb;
    }

    void fire(error_code err = {})
    {
        if (!cb) throw runtime_error("cb not assigned");
        cb(err);
    }
};

}
