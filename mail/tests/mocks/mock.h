#pragma once

#include <string>

namespace yrpopper::mock {

struct mock
{
    mock() = default;
    virtual ~mock() noexcept = default;
    virtual void reset(){};
    virtual void init_mock(){};
};

}
