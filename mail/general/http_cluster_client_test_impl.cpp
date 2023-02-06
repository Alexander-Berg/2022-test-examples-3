#include "http_cluster_client_test_impl.h"

namespace NTesting {

void THttpClusterClientMock::async_run(
    task_context_ptr task,
    request req,
    callback_type callback)
{
    async_run(task, req, options{}, callback);
}

void THttpClusterClientMock::async_run(
    task_context_ptr,
    request req,
    const options& opts,
    callback_type callback)
{
    ++CallCount;
    AsyncRunImpl(req, opts, callback);
}

}  // namespace NTesting
