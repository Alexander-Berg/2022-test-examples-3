#include "fakes/http_client.h"
#include <tasks/module.h>

namespace fan::tasks {

struct t_tasks
{
    using module = module_impl<fake_http_client>;

    task_context_ptr ctx;
    shared_ptr<fake_http_client> client = make_shared<fake_http_client>();
    shared_ptr<module> tasks = make_shared<module>(client);

    void prepare_response(int status, std::string body)
    {
        client->response = { .status = status, .body = body };
    }
};

}
