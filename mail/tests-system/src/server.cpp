#include <yplatform/find.h>
#include <yplatform/reactor.h>

#include <include/logger.hpp>
#include <include/server.hpp>
#include <include/ping.hpp>
#include <include/pingdb.hpp>

#include <boost/asio/spawn.hpp>

#include <memory>
#include <string>

namespace apq_tester::server {

using namespace boost::hana::literals;

namespace {

ContextPtr makeContext(request_ptr request, boost::asio::yield_context yc)
{
    auto conninfo = request->url.params.find("conninfo");
    return boost::make_shared<context>(
        request->context->uniq_id(),
        conninfo == request->url.params.end() ? "" : conninfo->second,
        yc);
};

}

void Server::init()
{
    bind<handlers::Ping, decltype("/ping"_s)>();
    bind<handlers::PingDb, decltype("/pingdb"_s)>();
}

template <class Handler, class Uri>
void Server::bind()
{
    using namespace std::string_literals;
    const auto httpServer = yplatform::find<server, std::shared_ptr>("http_server");
    std::cout << Uri::c_str() << std::endl;
    httpServer->bind("", { Uri::c_str() }, [=](response_ptr response) {
        boost::asio::spawn(
            *yplatform::global_net_reactor->io(),
            [response = std::move(response)](boost::asio::yield_context yc) {
                const auto handler = Handler{};
                try
                {
                    auto result = handler(
                        response->request(), response, makeContext(response->request(), yc));
                    if (!result)
                    {
                        APQ_TESTER_LOG_ERROR(
                            logdog::message = "error ", logdog::error_code = result.error());
                        response->set_code(internal_server_error);
                        response->set_content_type("text/plain");
                        response->result_body("Internal error "s + result.error().message());
                    }
                    response->set_code(ok);
                    response->set_content_type("application/json");
                    auto text = std::move(result).value();
                    if (!text.empty())
                    {
                        response->result_body(text);
                    }
                }
                catch (const boost::coroutines::detail::forced_unwind&)
                {
                    throw;
                }
                catch (const std::exception& error)
                {
                    response->set_code(internal_server_error);
                    response->set_content_type("text/plain");
                    response->result_body("Internal error "s + error.what());
                }
                catch (...)
                {
                    response->set_code(internal_server_error);
                    response->set_content_type("text/plain");
                    response->result_body("Internal error ");
                }
            },
            boost::coroutines::attributes(1048576));
    });
}

using impl = Server;

}

#include <yplatform/module_registration.h>

DEFINE_SERVICE_OBJECT(apq_tester::server::impl)
