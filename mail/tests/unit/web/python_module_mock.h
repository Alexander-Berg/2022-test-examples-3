#pragma once

#include <common/types.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace sheltie::tests {

using namespace testing;

struct PythonModuleMock {
    MOCK_METHOD(std::string, exportContacts, (std::string, std::string, sheltie::YieldCtx), ());
    MOCK_METHOD(std::string, importContacts, (std::string, std::string, sheltie::YieldCtx), ());
    MOCK_METHOD(std::string, transformFromVcard, (std::string, std::string, sheltie::YieldCtx), ());
    MOCK_METHOD(std::string, transformToVcard, (std::string, std::string, sheltie::YieldCtx), ());
};

}
