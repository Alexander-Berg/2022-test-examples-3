#pragma once

#include <gtest/gtest.h>

#include <internal/common/context.h>

namespace {

using namespace testing;

struct TestWithTaskContext : Test {
    boost::asio::io_context io;

    template <class T>
    void withSpawn(T&& impl) {
        boost::asio::spawn(io, [&] (boost::asio::yield_context yield) {
            impl(boost::make_shared<settings::context>("uniq_id", "request_id", "user_ip", "client_type",
                "client_version", "test_buckets", "enabled_test_buckets", yield));
        }, boost::coroutines::attributes(1048576));
        io.run();
    }
};

} // namespace
