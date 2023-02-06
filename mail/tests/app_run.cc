#include "yplatform/application.h"
#include <yplatform/module.h>
#include <yplatform/module_registration.h>
#include <yplatform/ptree.h>

#include <catch.hpp>
#include <iostream>
#include <cstring>

class test_module : public yplatform::module
{
public:
    void init(const yplatform::ptree&)
    {
        started = false;
        stopped = false;
    }
    void start()
    {
        started = true;
    }
    void stop()
    {
        stopped = true;
    }

    bool started;
    bool stopped;
};

REGISTER_MODULE(test_module)

TEST_CASE("app_run")
{
    {
        yplatform::configuration conf;
        conf.load_from_file("app_run.yml");
        yplatform::application app(conf);

        REQUIRE(yplatform::exists<test_module>("test"));
        REQUIRE(!yplatform::find<test_module>("test")->started);
        REQUIRE(yplatform::global_net_reactor);
        REQUIRE(yplatform::global_reactor_set);

        app.run();
        auto module = yplatform::find<test_module>("test");
        REQUIRE(module);
        REQUIRE(module->started);
        REQUIRE(!module->stopped);
        REQUIRE(!yplatform::exists<yplatform::reactor>("any"));
        REQUIRE(yplatform::exists<yplatform::log::source>("any"));

        app.stop(10);

        REQUIRE(!yplatform::exists<test_module>("test"));
        REQUIRE(module->stopped);
    }

    REQUIRE(yplatform::global_net_reactor);
    REQUIRE(yplatform::global_reactor_set);
}
