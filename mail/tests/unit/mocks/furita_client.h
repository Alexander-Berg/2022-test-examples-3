#pragma once

#include <mail/notsolitesrv/src/furita/iclient.h>

#include <gmock/gmock.h>

class TFuritaClientMock : public NNotSoLiteSrv::NFurita::IFuritaClient {
public:
    MOCK_METHOD(void, List, (boost::asio::io_context&, NNotSoLiteSrv::TUid,
        NNotSoLiteSrv::NFurita::TListCallback), (const, override));
    MOCK_METHOD(void, Get, (boost::asio::io_context&, NNotSoLiteSrv::TOrgId,
        NNotSoLiteSrv::NFurita::TGetCallback), (const, override));
};
