#define CATCH_CONFIG_MAIN // This tell CATCH to provide a main() - only do this in one cpp file
#include <yplatform/log.h>
#include <catch.hpp>

int init_logs = [] {
    yplatform::log::init_global_log_console();
    return 0;
}();
