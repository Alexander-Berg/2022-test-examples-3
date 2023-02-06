#pragma once

#include <mail/notsolitesrv/src/tupita/iclient.h>

#include <gmock/gmock.h>

class TTupitaClientMock : public NNotSoLiteSrv::NTupita::ITupitaClient {
public:
    MOCK_METHOD(
        void,
        Check,
        (boost::asio::io_context&,
         NNotSoLiteSrv::TUid,
         std::string,
         NNotSoLiteSrv::NTupita::TTupitaCheckRequest,
         NNotSoLiteSrv::NTupita::TCheckCallback),
        (const, override));
};
