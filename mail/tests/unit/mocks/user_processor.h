#pragma once

#include <user/processor.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>


using namespace NNotSoLiteSrv;

struct TUserProcessorMock {
    MOCK_METHOD(void, ProcessUser, (const std::string&, bool, bool, NUser::TUser&, NUser::TCallback), (const));
};
