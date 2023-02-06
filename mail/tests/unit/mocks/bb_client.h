#pragma once

#include "blackbox/client.h"
#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace NNotSoLiteSrv;

struct TBBClientMock {
    MOCK_METHOD(void, GetUser, (const std::string&, bool, NUser::TUser&, NBlackbox::TCallback), (const));
};
