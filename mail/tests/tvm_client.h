#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace msg_body {

using namespace testing;

struct TvmClientMock {
    MOCK_METHOD(std::string, invoke, (const std::string&), ());
};

} // namespace msg_body
