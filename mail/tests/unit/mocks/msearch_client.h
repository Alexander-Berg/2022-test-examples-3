#pragma once

#include <mail/notsolitesrv/src/msearch/client.h>

#include <gmock/gmock.h>

namespace NNotSoLiteSrv::NMSearch {

struct TMSearchClientMock : public IMSearchClient {
    MOCK_METHOD(void, SubscriptionStatus, (boost::asio::io_context& ioContext, const TSubscriptionStatusRequest& request, TSubscriptionStatusCallback callback), (const, override));
};

} // NNotSoLiteSrv::NMSearch
