#define CATCH_CONFIG_MAIN // This tell CATCH to provide a main() - only do this in one cpp file
#include <catch.hpp>
#include <yplatform/log.h>

int init_logs = [] {
    yplatform::log::init_global_log_console();

    return 0;
}();
