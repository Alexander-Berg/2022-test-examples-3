#include <gtest/gtest.h>
#include <macs/method_caller.h>

namespace {

using namespace ::testing;
using namespace macs;

using OptInt = boost::optional<int>;

TEST(InserterTest, create_and_call_handler_with_value_then_container_should_contain_value) {
    std::vector<int> container;
    auto inserter = synchronize::makeBackInserter("method", container);
    auto handler = inserter.handler();
    handler(make_error_code(error::ok), OptInt(42));
    EXPECT_EQ(std::vector<int>({42}), container);
}

TEST(InserterTest, create_and_call_handler_with_empty_value_then_get_should_return) {
    std::vector<int> container;
    auto inserter = synchronize::makeBackInserter("method", container);
    auto handler = inserter.handler();
    handler(make_error_code(error::ok), OptInt());
    EXPECT_NO_THROW(inserter.get());
}

TEST(InserterTest, create_and_call_handler_with_error_then_get_should_throw_exception) {
    std::vector<int> container;
    auto inserter = synchronize::makeBackInserter("method", container);
    auto handler = inserter.handler();
    handler(make_error_code(error::logic), OptInt());
    EXPECT_THROW(inserter.get(), system_error);
}

} // namespace
