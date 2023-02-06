#pragma once

#include "http/client.h"
#include <gtest/gtest.h>
#include <gmock/gmock.h>

struct THttpClientMock: public NNotSoLiteSrv::NHttp::IClient {
    MOCK_METHOD(void, Get, (const std::string&, TCallback), (override));
    MOCK_METHOD(void, Get, (const std::string&, const NNotSoLiteSrv::NHttp::THeaders&, TCallback), (override));

    MOCK_METHOD(void, Head, (const std::string&, TCallback), (override));
    MOCK_METHOD(void, Head, (const std::string&, const NNotSoLiteSrv::NHttp::THeaders&, TCallback), (override));

    MOCK_METHOD(void, Post, (const std::string&, const std::string&, TCallback), (override));
    MOCK_METHOD(void, Post, (const std::string&, const std::string&, const NNotSoLiteSrv::NHttp::THeaders&, TCallback), (override));

    MOCK_METHOD(void, Post, (const std::string&, const yplatform::zerocopy::segment&, TCallback), (override));
    MOCK_METHOD(void, Post, (const std::string&, const yplatform::zerocopy::segment&, const NNotSoLiteSrv::NHttp::THeaders&, TCallback), (override));
};
