#include "queue_buffer.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <util/random/fast.h>


namespace NPlutonium {

using TQueueRow = NBigRT::TYtQueue::TReadRow;
using TQueueRowBatch = NBigRT::TYtQueue::TReadResult;

namespace {

struct TTestBatch {
    TQueueRowBatch Batch;
    i64 BytesSize = 0;
    ui64 Shard = 0;
};

struct TTestDataGenerator {
    TTestDataGenerator(ui64 nShards) {
        ShardOffsets_.reserve(nShards);
        for (ui64 i = 0; i < nShards; ++i) {
            ShardOffsets_.push_back(static_cast<i64>(Rng_.Uniform(1000)));
        }
    }

    TTestBatch NextBatch() {
        const ui64 shard = Rng_.Uniform(ShardOffsets_.size());
        const i64 nElements = static_cast<i64>(Rng_.Uniform(10, 20));

        TTestBatch batch;
        batch.Shard = shard;
        batch.Batch.OffsetFrom = ShardOffsets_[shard] + 1;
        batch.Batch.OffsetTo = batch.Batch.OffsetFrom + nElements - 1;
        batch.Batch.Rows.reserve(nElements);
        for (i64 offset = batch.Batch.OffsetFrom; offset <= batch.Batch.OffsetTo; ++offset) {
            TQueueRow row = GenQueueRow(offset);
            batch.BytesSize += row.GetRawData().size() + row.GetCodec().size();
            batch.Batch.Rows.push_back(std::move(row));
        }

        ShardOffsets_[shard] += nElements;

        return batch;
    }

private:
    TQueueRow GenQueueRow(i64 offset) {
        constexpr ui64 DATA_LIMIT = 1000;
        return TQueueRow{offset, GenRandomString(DATA_LIMIT), GenRandomString(DATA_LIMIT), ++CommitTimestamp_};
    }

    TString GenRandomString(ui64 maxLimit) {
        const ui64 limit = Rng_.Uniform(maxLimit) + 1;
        TVector<char> buffer(Reserve(limit));
        for (ui64 i = 0; i < limit; ++i) {
            buffer.push_back(Rng_.Uniform('~' - ' ' + 1) + ' ');
        }
        return TString{buffer.data(), buffer.size()};
    }

    TFastRng64 Rng_{0x1ce1f2e507541a05, 0x07d45659, 0x7b8771030dd9917e, 0x2d6636ce};
    TVector<i64> ShardOffsets_;
    NYT::NTransactionClient::TTimestamp CommitTimestamp_ = 0;
};

void ExpectEmptyBatch(i64 offset, const TQueueRowBatch& emptyBatch) {
    ASSERT_EQ(emptyBatch.OffsetFrom, offset);
    ASSERT_EQ(emptyBatch.OffsetTo, offset);
    ASSERT_TRUE(emptyBatch.Rows.empty());
}

}  // anonymous namespace

TEST(TQueueBuffer, SingleShard) {
    TTestDataGenerator gen(1);
    TTestBatch batch1 = gen.NextBatch();
    const i64 firstOffset = batch1.Batch.OffsetFrom;

    const TQueueBufferOptions options{
        .Shards = {0},
        .BytesCapacity = 1'000'000'000
    };
    TQueueBuffer buffer(options);

    const i64 freeCap1 = buffer.AddRecords(batch1.Shard, std::move(batch1.Batch));
    ASSERT_EQ(freeCap1, options.BytesCapacity - batch1.BytesSize);

    TTestBatch batch2 = gen.NextBatch();
    const i64 freeCap2 = buffer.AddRecords(batch2.Shard, std::move(batch2.Batch));
    ASSERT_EQ(freeCap2, options.BytesCapacity - batch1.BytesSize - batch2.BytesSize);

    i64 lastOffset = batch2.Batch.OffsetTo;

    const TQueueRowBatch readAll = buffer.Read(0, 0, lastOffset - firstOffset + 1, false);
    ASSERT_EQ(readAll.OffsetFrom, firstOffset);
    ASSERT_EQ(readAll.OffsetTo, lastOffset);
    ASSERT_EQ(readAll.Rows.size(), static_cast<ui64>(lastOffset - firstOffset + 1));

    ExpectEmptyBatch(firstOffset, buffer.Read(0, firstOffset, 0, true));

    const i64 midOffset = (lastOffset + firstOffset) / 2;

    const i64 freeCap3 = buffer.MoveOffsets({0}, {firstOffset});
    const i64 freeCap4 = buffer.MoveOffsets({0}, {midOffset});

    ExpectEmptyBatch(0, buffer.Read(0, 0, midOffset, true));

    const i64 freeCap5 = buffer.MoveOffsets({0}, {lastOffset});
    const i64 freeCap6 = buffer.MoveOffsets({0}, {lastOffset + 1});

    ASSERT_EQ(freeCap2, freeCap3);
    ASSERT_LT(freeCap3, freeCap4);
    ASSERT_LT(freeCap4, freeCap5);
    ASSERT_LT(freeCap5, freeCap6);
    ASSERT_EQ(freeCap6, options.BytesCapacity);

    ExpectEmptyBatch(0, buffer.Read(0, 0, Max<i32>(), false));
}

}
