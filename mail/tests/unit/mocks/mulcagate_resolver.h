#pragma once

#include <mail/ymod_mds/src/resolver.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

struct TMulcagateResolverMock: public NMds::IHostResolver {
    MOCK_METHOD(void, Start, (), (override));
    MOCK_METHOD(void, Stop, (), (override));

    MOCK_METHOD(std::string, GetRealHost, (), (const, override));
};
