#pragma once

#include <gmock/gmock.h>
#include <internal/search/regular.h>

namespace regular::test {

struct MockRegular: Regular {
    MOCK_METHOD(int, find, (const std::string & text, std::string& matched), (override));
    MOCK_METHOD(int, initRegexp, (), (override));
    MOCK_METHOD(bool, checkFileChanged, (), (override));
};

}
