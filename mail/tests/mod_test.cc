#include <string>
#include <list>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/thread.hpp>
#include <boost/bind.hpp>
#include <boost/unordered_map.hpp>

#include <yplatform/module_registration.h>
#include <yplatform/find.h>

#include <ymod_httpserver/handler.h>
#include <ymod_httpserver/server.h>

namespace mod_test {

class ok_handler : public ymod_http_server::handler
{

    bool process(ymod_http_server::request_ptr req, ymod_http_server::response_ptr res)
    {
        res->result(ymod_http_server::response_codes::CODE_OK, "pong");

        return true;
    }
};

class mod_test
    : public yplatform::module
    , public ymod_http_server::handler
{
public:
    boost::shared_ptr<test_up> get_shared_from_this()
    {
        return boost::static_pointer_cast<test_up>(shared_from_this());
    }

    void init()
    {
        yplatform::find<ymod_http_server::server>("http")->subscribe(
            "/ping", boost::make_shared<ok_handler>());
    }
};

DEFINE_SERVICE_OBJECT_BEGIN()
DEFINE_SERVICE_OBJECT_MODULE(mod_test)
DEFINE_SERVICE_OBJECT_END()
