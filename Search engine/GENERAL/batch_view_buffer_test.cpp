#include "batch_view_buffer.h"
#include "ut/common.h"

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <util/generic/xrange.h>

#include <yt/yt/core/logging/log.h>

namespace NPlutonium::NProcessors::NUT {

namespace {

static NYT::NLogging::TLogger Logger{"profile_buffer_test"};
}

TEST(BatchViewBuffer, Simple) {
    TBatchViewBuffer<TA> pb;
    const ui32 start = 0;
    const ui32 viewSize = 2;
    const ui32 totalSize = 8;
    for(ui32 i = start; i < totalSize; i += viewSize) {
        pb.Write(TABatchView(i, i + viewSize));
    }

    const ui32 batchSize = 3;
    ui32 batchNumber = 0;
    while (auto maybeBatch = pb.TryRead(batchSize)) {
        ASSERT_THAT(
            CollectToVector<TA>(std::move(*maybeBatch)), 
            testing::ElementsAreArray(xrange(start + batchNumber * batchSize, start + (batchNumber + 1) * batchSize))
        );
        batchNumber++;
    }

    if (auto maybeBatch = pb.TryReadIncomplete(batchSize)) {
        ASSERT_THAT(
            CollectToVector<TA>(std::move(*maybeBatch)), 
            testing::ElementsAreArray(xrange(start + batchNumber * batchSize, totalSize))
        );
    }

}

TEST(BatchViewBuffer, Reset) {
    TBatchViewBuffer<TA> pb;
    size_t size = pb.Write(TABatchView(0,1));
    ASSERT_THAT(size, testing::Eq(1ull));

    pb.Reset();
    auto res = pb.TryRead(size);
    ASSERT_THAT(res.Defined(), testing::IsFalse());
}

}
