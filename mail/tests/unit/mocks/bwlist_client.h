#pragma once

#include "blackwhitelist/client.h"
#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace NNotSoLiteSrv;

struct TBWListClientMock {
    MOCK_METHOD(void, LoadLists, (const std::string&, NBlackWhiteList::TCallback), (const));
};
