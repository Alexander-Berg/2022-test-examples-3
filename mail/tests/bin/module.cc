#include <ymod_webserver/server.h>

#include <yplatform/find.h>
#include <yplatform/module_registration.h>
#include <yplatform/loader.h>

#include "handlers.h"

namespace queuedb_api {

using WrappedFunction = std::function<std::string(ymod_webserver::response_ptr, boost::asio::yield_context)>;

void bindHandler(std::shared_ptr<ymod_webserver::server> server, const std::string& path,
          std::shared_ptr<yplatform::reactor> reactor, WrappedFunction f) {
    server->bind("", { path }, [=] (ymod_webserver::response_ptr s) {
        boost::asio::spawn(*reactor->io(), [=](boost::asio::yield_context yield) {
            try {
                auto res = f(s, yield);
                s->set_code(ymod_webserver::codes::ok);
                s->set_content_type("application", "json");
                s->result_body(res);
            } catch (const std::exception& ex) {
                s->result(ymod_webserver::codes::internal_server_error, ex.what());
            } catch(...) {
                s->result(ymod_webserver::codes::internal_server_error, "strange error");
            }
        }, boost::coroutines::attributes(1048576));
    });
}

struct Server: public yplatform::module {
    void init(const yplatform::ptree&) {
        auto serverModule = yplatform::find<ymod_webserver::server, std::shared_ptr>("webserver");
        auto reactor = yplatform::find_reactor<std::shared_ptr>("global");
        auto queuedb = yplatform::find<ymod_queuedb::Queue, std::shared_ptr>("queuedb");

        bindHandler(serverModule, "/ping", reactor,
            [=](ymod_webserver::response_ptr, boost::asio::yield_context yield) {
                return ping(PingParams(), yield);
        });

        bindHandler(serverModule, "/acquire_tasks", reactor,
            [=](ymod_webserver::response_ptr resp, boost::asio::yield_context yield) {
                return acquireTasks(getAcquireTasksParams(queuedb, resp), yield);
        });

        bindHandler(serverModule, "/add_task", reactor,
            [=](ymod_webserver::response_ptr resp, boost::asio::yield_context yield) {
                return addTask(getAddTaskParams(queuedb, resp), yield);
        });

        bindHandler(serverModule, "/complete_task", reactor,
            [=](ymod_webserver::response_ptr resp, boost::asio::yield_context yield) {
                return completeTask(getCompleteTaskParams(queuedb, resp), yield);
        });

        bindHandler(serverModule, "/fail_task", reactor,
            [=](ymod_webserver::response_ptr resp, boost::asio::yield_context yield) {
                return failTask(getFailTaskParams(queuedb, resp), yield);
        });

        bindHandler(serverModule, "/delay_task", reactor,
            [=](ymod_webserver::response_ptr resp, boost::asio::yield_context yield) {
                return delayTask(getDelayTaskParams(queuedb, resp), yield);
        });

        bindHandler(serverModule, "/refresh_task", reactor,
            [=](ymod_webserver::response_ptr resp, boost::asio::yield_context yield) {
                return refreshTask(getRefreshTaskParams(queuedb, resp), yield);
        });
    }
};

}

DEFINE_SERVICE_OBJECT(queuedb_api::Server)


int main(int argc, char* argv[]) {
    if (argc != 2) {
        std::cout << "usage " << argv[0] << " <config>\n";
        return 1;
    }

    return yplatform_start(argv[1]);
}
