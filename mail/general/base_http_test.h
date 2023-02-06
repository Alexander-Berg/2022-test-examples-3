#pragma once

#include "http_client_mock.h"
#include "http_cluster_client_test_impl.h"

#include <yplatform/task_context.h>
#include <ymod_httpclient/errors.h>

#include <gtest/gtest.h>

namespace NTesting {

template <typename THttpClient>
class TBaseHttpTest : public testing::Test {
protected:
    void SetUp() override;

    void Reset();

public:
    struct THttpResponse {
        boost::system::error_code Errc;
        NTesting::response Resp;
    };
    using THttpResponses = std::deque<THttpResponse>;

    const THttpResponse HTTP_500 = {{}, {.status = 500}};
    const THttpResponse HTTP_200 = {{}, {.status = 200}};
    const THttpResponse HTTP_404 = {{}, {.status = 404}};
    const THttpResponse HTTP_401 = {{}, {.status = 401}};

    void InitHttpMock(const THttpResponse& httpResponse);
    using TCheckRequest = std::function<void(const yhttp::request&)>;
    void InitHttpMock(TCheckRequest checkRequest, const THttpResponse& httpResponse);
    void InitHttpMock(const THttpResponses& httpResponses);

public:
    std::shared_ptr<THttpClient> HttpMock;
    THttpResponses HttpResponses;
    yplatform::task_context_ptr Ctx;
};

}  // namespace NTesting
