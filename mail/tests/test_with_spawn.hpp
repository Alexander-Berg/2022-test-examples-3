#pragma once

#include <gtest/gtest.h>

#include <boost/asio/spawn.hpp>

#include <memory>

namespace {

using namespace testing;

struct TestWithSpawn : Test
{
    std::shared_ptr<boost::asio::io_context> io = std::make_shared<boost::asio::io_context>();
    template <class T>
    void withSpawn(T&& impl)
    {
        boost::asio::spawn(*io, [&](boost::asio::yield_context yield) { impl(yield); });
        io->run();
    }
};

}
