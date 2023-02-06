#pragma once

#include <mail/notsolitesrv/src/mulcagate/client.h>
#include <mail/notsolitesrv/src/context.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NMulcagate;

struct TMulcagateClientMock: public NMds::TClient {
    MOCK_METHOD(void, Put, (NMds::TContext, const std::string&, const yplatform::zerocopy::segment&, NMds::ENsType, NMds::TGetPutCallback), (override));
    MOCK_METHOD(void, Check, (NMds::TContext, const std::string&, NMds::TCheckDelCallback), (override));
    MOCK_METHOD(void, Del, (NMds::TContext, const std::string&, NMds::TCheckDelCallback), (override));
    MOCK_METHOD(void, Get, (NMds::TContext, const std::string&, NMds::TGetPutCallback), (override));
};
