#pragma once

#include <mail/notsolitesrv/src/firstline/ifirstline.h>
#include <mail/notsolitesrv/src/firstline/ifirstline_impl.h>

#include <gmock/gmock.h>

namespace NNotSoLiteSrv::NFirstline {

struct TFirstlineMock : IFirstline {
    MOCK_METHOD(void, Firstline, (TContextPtr, TFirstlineRequest, TFirstlineCallback, boost::asio::io_context&), (const, override));
};

namespace NImpl {

struct TFirstlineImplMock : IFirstline {
    MOCK_METHOD(std::string, GenerateFirstline, (const TFirstlineRequest&), (const, override));
};

} // namespace NImpl

} // namespace NNotSoLiteSrv::NFirstline
