#pragma once

#include "private.h"

namespace NCloud::NFileStore {

////////////////////////////////////////////////////////////////////////////////

struct ITest {
    virtual ~ITest() = default;

    virtual int Run() = 0;

    virtual void Stop() = 0;
};

////////////////////////////////////////////////////////////////////////////////

ITestPtr CreateTest(TOptionsPtr options);

}   // namespace NCloud::NFileStore