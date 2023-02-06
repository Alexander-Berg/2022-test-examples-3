#pragma once

#include <mail/notsolitesrv/src/mthr/imthr.h>

#include <gmock/gmock.h>

class TMthrMock : public NNotSoLiteSrv::NMthr::IMthr {
public:
    MOCK_METHOD(NNotSoLiteSrv::NMthr::TMthrResult, GetThreadInfo,
        (NNotSoLiteSrv::TContextPtr, NNotSoLiteSrv::NMthr::TMthrRequest), (const, override));
};
