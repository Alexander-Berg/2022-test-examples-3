#include "parallel_processor.h"
#include "ut/common.h"

#include <yt/yt/core/logging/log.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>




namespace NPlutonium::NProcessors::NUT {
namespace {

static NYT::NLogging::TLogger Logger{"parallel_processor_test"};


} // anonymous namespace

TEST(ParallelProfileProcessor, Simple) {

    const TAsyncTaskPoolConfig config {
        .Name = "test_pool",
        .ParallelTasks_ = 2
    };

    auto summator = MakeIntrusive<TSummator>();
    auto parallelSummator = MakeParallel(summator, config);
    
    auto f1 = parallelSummator->AddInput(TABatchView(0, 3));
    auto f2 = parallelSummator->AddInput(TABatchView(3, 6));
    auto f3 = parallelSummator->AddInput(TABatchView(6, 9));

    NYT::NConcurrency::WaitFor(parallelSummator->Flush()).ThrowOnError();

    ASSERT_THAT(f1.GetUnique().ValueOrThrow().Next(), testing::Eq(TA{3}));
    ASSERT_THAT(f2.GetUnique().ValueOrThrow().Next(), testing::Eq(TA{12}));
    ASSERT_THAT(f3.GetUnique().ValueOrThrow().Next(), testing::Eq(TA{21}));
    
}

}
