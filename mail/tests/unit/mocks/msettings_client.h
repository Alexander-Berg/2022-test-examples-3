#pragma once

#include <mail/notsolitesrv/src/msettings/client.h>

#include <gmock/gmock.h>

namespace NNotSoLiteSrv::NMSettings {

struct TMSettingsClientMock : public IMSettingsClient {
    MOCK_METHOD(void, GetParams, (TContextPtr ctx, const TParamsRequest& request, TParamsCallback callback), (const, override));
    MOCK_METHOD(void, GetProfile, (TContextPtr ctx, const TParamsRequest& request, TParamsCallback callback), (const, override));
};

} // NNotSoLiteSrv::NMSettings
