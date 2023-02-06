#include "buffering_processor.h"
#include "ut/common.h"

#include <yt/yt/core/logging/log.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>


namespace NPlutonium::NProcessors::NUT {
namespace {

static NYT::NLogging::TLogger Logger{"buffered_processor_test"};

}


TEST(BufferedProcessor, Simple) {
    YT_LOG_INFO("Start");
    
    auto summator = MakeIntrusive<TSummator>();
    auto bufferingProcessor = MakeBufferingProcessor<TA>(3);

    TVector<TA> result;
    auto propagationCallback = [=, &result] (CBatchView<TA> auto batchView) {
        auto sumView = summator->AddInput(std::move(batchView));
        CollectToVector<TA>(sumView, result);
        return NYT::TError();
    };

    const ui32 totalSize = 4;
    const ui32 viewSize = 2;
    for(ui32 i = 1; i <= totalSize; i += viewSize) {
        TABatchView aView(i, i + viewSize);
        auto addInputError = bufferingProcessor->AddInput(std::move(aView), propagationCallback);
        ASSERT_THAT(addInputError.IsOK(), testing::IsTrue());
    }
    
    auto flushError = bufferingProcessor->Flush(propagationCallback);
    ASSERT_THAT(flushError.IsOK(), testing::IsTrue());


    ASSERT_THAT(result, testing::ElementsAre(TA{6}, TA{4}));
    YT_LOG_INFO("Done");
}

}
