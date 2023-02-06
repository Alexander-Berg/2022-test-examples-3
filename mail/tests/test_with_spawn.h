#pragma once

#include <gtest/gtest.h>

#include <boost/asio/spawn.hpp>

namespace sharpei::tests {

using namespace testing;

struct TestWithSpawn : Test {
    boost::asio::io_context io;

    template <class T>
    void withSpawn(T&& impl) {
        boost::asio::spawn(io, std::forward<T>(impl), boost::coroutines::attributes(1024 * 1024));
        io.run();
    }
};

} // namespace sharpei::tests
