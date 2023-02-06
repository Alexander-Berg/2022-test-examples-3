#include "yplatform/application.h"
#include <yplatform/module.h>
#include <yplatform/module_registration.h>

#include <catch.hpp>
#include <iostream>
#include <cstring>
#include <signal.h>

class test_module2 : public yplatform::module
{
};

class test_module_bad : public yplatform::module
{
public:
    void init(const yplatform::ptree&)
    {
        throw std::logic_error("test-exception");
    }
};

REGISTER_MODULES_BEGIN()
REGISTER_MODULES_ADD(test_module2)
REGISTER_MODULES_ADD(test_module_bad)
REGISTER_MODULES_END()

TEST_CASE("app_init")
{
    yplatform::configuration conf;
    conf.load_from_file("app_init.yml");

    std::string exception_message;
    try
    {
        yplatform::application app(conf);
        app.run();
        app.stop(SIGUSR1); // expect no errors
    }
    catch (std::runtime_error const& e)
    {
        exception_message = e.what();
    }
    REQUIRE(
        exception_message ==
        "application::load status=error, reason: "
        "module \"test_bad\" load exception \"test-exception\"");
}
