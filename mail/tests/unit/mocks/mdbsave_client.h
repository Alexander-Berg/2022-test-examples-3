#pragma once

#include <mail/notsolitesrv/src/mdbsave/iclient.h>

#include <gmock/gmock.h>

class TMdbSaveClientMock : public NNotSoLiteSrv::NMdbSave::IMdbSaveClient {
public:
    MOCK_METHOD(
        void,
        MdbSave,
        (boost::asio::io_context&,
         std::string,
         NNotSoLiteSrv::NMdbSave::TMdbSaveRequest,
         NNotSoLiteSrv::NMdbSave::TMdbSaveCallback),
        (const, override));
};
