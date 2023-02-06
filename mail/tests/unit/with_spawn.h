#pragma once

#include <gtest/gtest.h>

#include <common/spawn.h>

namespace {

using namespace testing;

struct TestWithYieldCtx : Test {
    boost::asio::io_context io;

    template <class T>
    void withSpawn(T&& impl) {
        sheltie::spawn(io, impl);
        io.run();
    }
};

} // namespace
