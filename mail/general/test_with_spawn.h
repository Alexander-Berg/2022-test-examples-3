#pragma once

#include <gtest/gtest.h>

#include <boost/asio/spawn.hpp>

namespace {

using namespace testing;

struct TTestWithSpawn : Test {
    boost::asio::io_context Io;

    template <class T>
    void WithSpawn(T&& impl) {
        boost::asio::spawn(Io, [&] (boost::asio::yield_context yield) {
            impl(yield);
        });
        Io.run();
    }
};

} // namespace anonymous
