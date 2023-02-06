#pragma once

#include "common.hpp"

namespace {

using namespace testing;

struct TvmClientMock {
    MOCK_METHOD(std::string, invoke, (const std::string&), ());
};

} // namespace
