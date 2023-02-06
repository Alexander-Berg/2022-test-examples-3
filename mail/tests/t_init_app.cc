#include <yplatform/application.h>
#include <yplatform/module_registration.h>

#include <catch.hpp>

struct module_ctor_with_reactor_set : yplatform::module
{
    module_ctor_with_reactor_set(yplatform::reactor_set&)
    {
        constructed = "from reactor_set";
    }
    module_ctor_with_reactor_set(yplatform::reactor&)
    {
        constructed = "from reactor";
    }
    module_ctor_with_reactor_set(boost::asio::io_service&)
    {
        constructed = "from io_service";
    }
    module_ctor_with_reactor_set()
    {
        constructed = "from nothing";
    }

    std::string constructed;
};

struct module_ctor_with_reactor : yplatform::module
{
    module_ctor_with_reactor(yplatform::reactor&)
    {
        constructed = "from reactor";
    }
    module_ctor_with_reactor(boost::asio::io_service&)
    {
        constructed = "from io_service";
    }
    module_ctor_with_reactor()
    {
        constructed = "from nothing";
    }

    std::string constructed;
};

struct module_ctor_with_io_service : yplatform::module
{
    module_ctor_with_io_service(boost::asio::io_service&)
    {
        constructed = "from io_service";
    }
    module_ctor_with_io_service()
    {
        constructed = "from nothing";
    }

    std::string constructed;
};

struct module_ctor_with_ptree : yplatform::module
{
    module_ctor_with_ptree(const yplatform::ptree& conf)
    {
        param = conf.get<std::string>("param");
    }

    std::string param;
};

struct module_init_with_ptree : yplatform::module
{
    void init(const yplatform::ptree& conf)
    {
        param = conf.get<std::string>("param");
    }
    std::string param;
};

struct module_init_without_args : yplatform::module
{
    void init()
    {
        inited = true;
    }
    bool inited = false;
};

const std::string conf_base = "config:\n"
                              "    system:\n"
                              "        dir: .\n"
                              "    log:\n"
                              "    modules:\n"
                              "        module:\n";

TEST_CASE("init_app/pass_reactor_set_to_ctor")
{
    std::string conf_str = conf_base + "        -   system:\n" +
        "                name: module_ctor_with_reactor_set\n" +
        "                class: module_ctor_with_reactor_set\n" + "            configuration:\n";
    yplatform::configuration conf;
    conf.load_from_str(conf_str);
    yplatform::application app(conf);
    app.run();
    auto module = yplatform::find<module_ctor_with_reactor_set>("module_ctor_with_reactor_set");
    REQUIRE(module->constructed == "from reactor_set");
    app.stop();
}

TEST_CASE("init_app/pass_reactor_to_ctor")
{
    std::string conf_str = conf_base + "        -   system:\n" +
        "                name: module_ctor_with_reactor\n" +
        "                class: module_ctor_with_reactor\n" + "            configuration:\n";
    yplatform::configuration conf;
    conf.load_from_str(conf_str);
    yplatform::application app(conf);
    app.run();
    auto module = yplatform::find<module_ctor_with_reactor>("module_ctor_with_reactor");
    REQUIRE(module->constructed == "from reactor");
    app.stop();
}

TEST_CASE("init_app/pass_io_service_to_ctor")
{
    std::string conf_str = conf_base + "        -   system:\n" +
        "                name: module_ctor_with_io_service\n" +
        "                class: module_ctor_with_io_service\n" + "            configuration:\n";
    yplatform::configuration conf;
    conf.load_from_str(conf_str);
    yplatform::application app(conf);
    app.run();
    auto module = yplatform::find<module_ctor_with_io_service>("module_ctor_with_io_service");
    REQUIRE(module->constructed == "from io_service");
    app.stop();
}

TEST_CASE("init_app/pass_config_to_ctor")
{
    std::string conf_str = conf_base + "        -   system:\n" +
        "                name: module_ctor_with_ptree\n" +
        "                class: module_ctor_with_ptree\n" + "            configuration:\n" +
        "                param: val";
    yplatform::configuration conf;
    conf.load_from_str(conf_str);
    yplatform::application app(conf);
    app.run();
    auto module = yplatform::find<module_ctor_with_ptree>("module_ctor_with_ptree");
    REQUIRE(module->param == "val");
    app.stop();
}

TEST_CASE("init_app/pass_config_to_init")
{
    std::string conf_str = conf_base + "        -   system:\n" +
        "                name: module_init_with_ptree\n" +
        "                class: module_init_with_ptree\n" + "            configuration:\n" +
        "                param: val";
    yplatform::configuration conf;
    conf.load_from_str(conf_str);
    yplatform::application app(conf);
    app.run();
    auto module = yplatform::find<module_init_with_ptree>("module_init_with_ptree");
    REQUIRE(module->param == "val");
    app.stop();
}

TEST_CASE("init_app/call_init")
{
    std::string conf_str = conf_base + "        -   system:\n" +
        "                name: module_init_without_args\n" +
        "                class: module_init_without_args\n" + "            configuration:\n";
    yplatform::configuration conf;
    conf.load_from_str(conf_str);
    yplatform::application app(conf);
    app.run();
    auto module = yplatform::find<module_init_without_args>("module_init_without_args");
    REQUIRE(module->inited);
    app.stop();
}

REGISTER_MODULE(module_ctor_with_reactor_set)
REGISTER_MODULE(module_ctor_with_reactor)
REGISTER_MODULE(module_ctor_with_io_service)
REGISTER_MODULE(module_ctor_with_ptree)
REGISTER_MODULE(module_init_with_ptree)
REGISTER_MODULE(module_init_without_args)
