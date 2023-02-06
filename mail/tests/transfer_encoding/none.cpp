#include "../mocks.hpp"
#include <ymod_webserver_helpers/transfer_encoding/none.hpp>
#include <gtest/gtest.h>

namespace {

using namespace testing;
using namespace ymod_webserver::helpers::tests;
using namespace ymod_webserver::helpers::transfer_encoding;

struct NoneTest : public Test {};

TEST(NoneTest, write_should_call_nothing) {
    MockedResponse response;
    None {}.write(response);
}

} // namespace
