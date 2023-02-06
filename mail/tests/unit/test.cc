#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <pa/async.h>
#include "yplatform.hpp"

void mainTest(const std::string& /* configPath */)
{
}

int runTests(const std::string& configPath)
{
    if (hasYplatform()) sleep(1);

    mainTest(configPath);

    auto result = RUN_ALL_TESTS();

    stopYplatform();
    return result;
}

#define CONFIG_PATH "tests.yml"
static std::string YplatformConfig = CONFIG_PATH;

extern bool hasYplatform()
{
    return !YplatformConfig.empty();
}

int main(int argc, char* argv[])
{
    std::string pa_log_file = "./profiler.log";
    pa::async_profiler::init(1000, 1000, pa_log_file);

    ::testing::FLAGS_gtest_output = "xml";
    ::testing::FLAGS_gtest_death_test_style = "threadsafe";
    ::testing::InitGoogleMock(&argc, argv);

    yplatform::log::init_global_log_console();

    std::thread testsThread(runTests, YplatformConfig);
    startYplatform(YplatformConfig);
    testsThread.join();
    return 0;
}
