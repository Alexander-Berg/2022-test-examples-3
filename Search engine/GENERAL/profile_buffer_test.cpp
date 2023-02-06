#include "profile_buffer.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/array_ref.h>


namespace NPlutonium::NProcessors {

size_t CopySliceToBuffer(TProfileBuffer<ui32>* buffer, TArrayRef<const ui32> inputData, size_t pos, size_t batchSize) {
    if (pos >= inputData.size()) {
        return pos;
    }
    const size_t nextPos = std::min(inputData.size(), pos + batchSize);
    buffer->Write(MakeAtomicShared<TVector<ui32>>(inputData.begin() + pos, inputData.begin() + nextPos));
    return nextPos;
}

void TestWriteAndRead(size_t totalDataSize, size_t inputBatchSize, size_t firstBatchSize, size_t outputBatchSize) {
    TVector<ui32> inputData(Reserve(totalDataSize));
    TVector<ui32> outputData(Reserve(totalDataSize));
    for (ui32 i = 0; i < totalDataSize; ++i) {
        inputData.push_back(i);
    }
    TProfileBuffer<ui32> buffer;
    for (size_t pos = 0; pos < totalDataSize;) {
        pos = CopySliceToBuffer(&buffer, inputData, pos, pos == 0 && firstBatchSize > 0 ? firstBatchSize : inputBatchSize);
        auto [batch, _] = buffer.Read(outputBatchSize);
        if (batch) {
            outputData.insert(outputData.end(), batch->begin(), batch->end());
        }
    }
    for (;;) {
        auto [batch, unreadSize] = buffer.Read(outputBatchSize, true/*incompleteBatches*/);
        if (!batch) {
            ASSERT_EQ(unreadSize, 0u);
            break;
        }
        outputData.insert(outputData.end(), batch->begin(), batch->end());
    }

    ASSERT_EQ(inputData, outputData);
}

TEST(ProfileBuffer, SameBufferSize) {
    TestWriteAndRead(1000, 100, 0, 100);
    TestWriteAndRead(1020, 100, 0, 100);
    TestWriteAndRead(1000, 100, 10, 100);
    TestWriteAndRead(1020, 100, 10, 100);
}

TEST(ProfileBuffer, SmallerBuffer) {
    TestWriteAndRead(10000, 1000, 0, 300);
    TestWriteAndRead(10200, 1000, 0, 300);
    TestWriteAndRead(10000, 1000, 10, 300);
    TestWriteAndRead(10200, 1000, 10, 300);
}

TEST(ProfileBuffer, BiggerBuffer) {
    TestWriteAndRead(10000, 300, 0, 1000);
    TestWriteAndRead(10200, 300, 0, 1000);
    TestWriteAndRead(10000, 300, 10, 1000);
    TestWriteAndRead(10200, 300, 10, 1000);
}

}
