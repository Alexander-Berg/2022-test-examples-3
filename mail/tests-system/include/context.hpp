#pragma once

#include <yplatform/task_context.h>

#include <boost/asio/spawn.hpp>

namespace apq_tester {

struct context
    : yplatform::task_context
    , boost::noncopyable
{

    explicit context(
        const std::string& uniqId,
        const std::string& conninfo,
        const boost::asio::yield_context& yield)
        : yplatform::task_context(uniqId), conninfo_(conninfo), yield_(yield)
    {
    }

    std::string conninfo_;
    boost::asio::yield_context yield_;
};

using ContextPtr = boost::shared_ptr<context>;

}
