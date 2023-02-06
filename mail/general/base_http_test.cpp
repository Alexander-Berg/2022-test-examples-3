#include "base_http_test.h"

#include "init_log.h"
#include "init_config.h"

namespace NTesting {

template <typename THttpClient>
void TBaseHttpTest<THttpClient>::SetUp() {
    NTesting::InitGlobalConfig();
    NTesting::InitGlobalLog();
    Reset();
}

template <typename THttpClient>
void TBaseHttpTest<THttpClient>::InitHttpMock(const THttpResponse& httpResponse) {
    HttpMock = std::make_shared<THttpClient>(
        [httpResponse](auto, auto, auto callback) {
            callback(httpResponse.Errc, httpResponse.Resp);
        });
}

template <typename THttpClient>
void TBaseHttpTest<THttpClient>::InitHttpMock(TCheckRequest checkRequest, const THttpResponse& httpResponse) {
    HttpMock = std::make_shared<THttpClient>(
        [checkRequest = std::move(checkRequest), httpResponse](auto request, auto, auto callback) {
            checkRequest(request);
            callback(httpResponse.Errc, httpResponse.Resp);
        });
}

template <typename THttpClient>
void TBaseHttpTest<THttpClient>::InitHttpMock(const THttpResponses& httpResponses) {
    HttpResponses = httpResponses;
    HttpMock = std::make_shared<THttpClient>(
        [this](auto, auto, auto callback) {
            if (HttpResponses.empty()) {
                throw std::runtime_error("No requests left");
            }
            auto [errc, resp] = HttpResponses.front();
            HttpResponses.pop_front();
            callback(errc, resp);
        });
}

template <typename THttpClient>
void TBaseHttpTest<THttpClient>::Reset() {
    HttpMock = nullptr;
    Ctx = boost::make_shared<yplatform::task_context>();
}

template class TBaseHttpTest<NTesting::THttpClientMock>;
template class TBaseHttpTest<NTesting::THttpClusterClientMock>;

}  // namespace NTesting
