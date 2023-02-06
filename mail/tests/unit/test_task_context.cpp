#include <gtest/gtest.h>

#include <src/task_context.hpp>

namespace boost::asio {

static inline bool operator ==(const boost::asio::yield_context& lhs, const boost::asio::yield_context& rhs) {
    return lhs.coro_.lock() == rhs.coro_.lock();
}

} // namespace boost::asio

namespace {

using namespace testing;

using collie::TaskContext;

struct TestTaskContext : Test {
    boost::asio::io_context io;

    template <class T>
    void test(T&& test) {
        boost::asio::spawn(io, std::forward<T>(test));
        io.run();
    }
};

TEST_F(TestTaskContext, uniq_id_should_return_value_equal_to_passed_into_ctor) {
    test([] (boost::asio::yield_context yield) {
        EXPECT_EQ(TaskContext("uniq_id", "", "", "", yield).uniq_id(), "uniq_id");
    });
}

TEST_F(TestTaskContext, request_id_should_return_value_equal_to_passed_into_ctor) {
    test([] (boost::asio::yield_context yield) {
        EXPECT_EQ(TaskContext("", "request_id", "", "", yield).requestId(), "request_id");
    });
}

TEST_F(TestTaskContext, user_ip_should_return_value_equal_to_passed_into_ctor) {
    test([] (boost::asio::yield_context yield) {
        EXPECT_EQ(TaskContext("", "", "user_ip", "", yield).userIp(), "user_ip");
    });
}

TEST_F(TestTaskContext, client_type_should_return_value_equal_to_passed_into_ctor) {
    test([] (boost::asio::yield_context yield) {
        EXPECT_EQ(TaskContext("", "", "", "client_type", yield).clientType(), "client_type");
    });
}

TEST_F(TestTaskContext, yield_should_return_value_equal_to_passed_into_ctor) {
    test([] (boost::asio::yield_context yield) {
        EXPECT_EQ(TaskContext("", "", "", "", yield).yield(), yield);
    });
}

} // namespace
