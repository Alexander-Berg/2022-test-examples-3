#include <ymod_httpclient/client.h>

namespace ymod_httpclient {

struct cluster_client_mock : cluster_call
{
    cluster_client_mock(const std::string& default_resp_body) : resp{ .body = default_resp_body }
    {
    }

    void async_run(task_context_ptr /*ctx*/, request /*req*/, callback_type /*callback*/)
    {
        throw std::runtime_error("not implemented");
    }

    void async_run(
        task_context_ptr /*ctx*/,
        request req,
        const options& /*opts*/,
        callback_type callback)
    {
        cluster_client_mock::req = req;
        callback(ec, resp);
    }

    boost::system::error_code ec;
    response resp;
    request req;
};

}
