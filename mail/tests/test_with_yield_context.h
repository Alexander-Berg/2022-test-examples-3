#pragma once

#include <gtest/gtest.h>
#include <internal/yield_context.h>

namespace {

using namespace testing;

struct TestWithYieldContext : Test {
    boost::asio::io_context io;

    template <class T>
    void withSpawn(T&& impl) {
        boost::asio::spawn(io, [&] (msg_body::YieldCtx yc) {
            impl(yc);
        }, boost::coroutines::attributes(1048576));
        io.run();
    }
};

} // namespace
