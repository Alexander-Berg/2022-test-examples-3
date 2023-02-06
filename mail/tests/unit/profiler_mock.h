#ifndef DOBERMAN_TESTS_PROFILER_MOCK_H_
#define DOBERMAN_TESTS_PROFILER_MOCK_H_

#include <src/profiling/profiler.h>
#include <gmock/gmock.h>

namespace doberman {
namespace testing {

using namespace ::testing;

struct ProfilerMock {

    MOCK_METHOD(void, write, (const std::string&, const std::string&, profiling::Duration), (const));
    MOCK_METHOD(void, write, (const std::string&, const std::string&, const std::string&, profiling::Duration), (const));
};

} // namespace test
} // namespace doberman



#endif /* DOBERMAN_TESTS_PROFILER_MOCK_H_ */
