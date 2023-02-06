#define CATCH_CONFIG_MAIN // This tell CATCH to provide a main() - only do this in one cpp file
#include <typed_log/typed_log.h>
#include <yplatform/log.h>
#include <yplatform/repository.h>
#include <catch.hpp>
#include <boost/asio.hpp>

boost::asio::io_context io;

int init_logs = [] {
    yplatform::log::init_global_log_console();

    auto typed_log = std::make_shared<xeno::typed_log>(io, yplatform::ptree());
    yplatform::repository::instance().add_service<xeno::typed_log>("typed_log", typed_log);

    return 0;
}();
