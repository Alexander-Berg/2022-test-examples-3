#pragma once

#include <ymod_httpclient/call.h>

#include <functional>

namespace NTesting {

using yhttp::request;

class THttpClusterClientMock : public ymod_httpclient::cluster_call {
public:
    using TAsyncRunImpl = std::function<void(request, options, callback_type)>;

    explicit THttpClusterClientMock(TAsyncRunImpl asyncRunImpl)
        : AsyncRunImpl(std::move(asyncRunImpl))
    {}

    void async_run(task_context_ptr ctx, request req, callback_type callback) override;
    void async_run(task_context_ptr ctx, request req, const options& options, callback_type callback) override;

public:
    TAsyncRunImpl AsyncRunImpl;
    size_t CallCount = 0;
};

}  // namespace NTesting

