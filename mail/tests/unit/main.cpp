#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yplatform/log.h>

int main(int argc, char* argv[]) {
    ::testing::FLAGS_gtest_output = "xml";
    ::testing::FLAGS_gtest_death_test_style = "threadsafe";
    ::testing::InitGoogleMock(&argc, argv);

    yplatform::log::init_global_log_file("collie.log");

    return RUN_ALL_TESTS();
}
