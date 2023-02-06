#include <backend/meta_pg/blocked_folder.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace yimap;
using namespace yimap::backend;
using namespace testing;

seq_range makeSeqRange(std::initializer_list<range_t> ranges)
{
    seq_range res(0, numeric_limits<uint32_t>::max(), true);
    for (auto&& range : ranges)
    {
        res.insert(range);
    }
    return res;
}

seq_range makeSeqRange(range_t range)
{
    return makeSeqRange({ range });
}

static const uint32_t firstUid = 10;
static const uint32_t lastUid = 101;
static const uint32_t blockSize = 25;

struct UidMapBlockTest : TestWithParam<std::tuple<seq_range, size_t>>
{
    UidMapBlock block;

    UidMapBlockTest()
    {
        setupBlock(firstUid, lastUid, blockSize);
    }

    auto range()
    {
        return std::get<0>(GetParam());
    }

    auto limit()
    {
        return std::get<1>(GetParam());
    }

    MessageVector filterBlock(const seq_range& ranges, size_t limit)
    {
        UidMap res;
        block.filterByRange(ranges, limit, res);
        return res.toMessageVector();
    }

    MessageVector filterRemainingMessages(const MessageVector& messages)
    {
        auto range = makeSeqRange({ 0, numeric_limits<uint32_t>::max() });
        if (messages.size())
        {
            range = makeSeqRange({ { 0, messages.front().uid - 1 },
                                   { messages.back().uid + 1, numeric_limits<uint32_t>::max() } });
        }
        return filterBlock(range, block.size());
    }

    void setupBlock(uint32_t firstUid, uint32_t lastUid, uint32_t blockSize)
    {
        block = UidMapBlock(UidMapEntry{ firstUid, blockSize, lastUid + 1 });
        block.upperUid = lastUid;

        auto step = (lastUid - firstUid) / blockSize;
        if (!step) throw std::logic_error("blockSize greater than uid's distance");

        for (int i = 0; i < blockSize; ++i)
        {
            auto currentUid = firstUid + i * step;
            block.insert(MessageData(currentUid, currentUid));
        }
    }
};

TEST_P(UidMapBlockTest, filterByRangeWithLimit)
{
    auto filtered = filterBlock(range(), limit());
    ASSERT_LE(filtered.size(), limit());
    ASSERT_THAT(
        filtered, Each(ResultOf([=](auto msg) { return range().contains(msg); }, Eq(true))));

    auto remaining = filterRemainingMessages(filtered);
    ASSERT_EQ(filtered.size() + remaining.size(), block.size());
}

INSTANTIATE_TEST_SUITE_P(
    InstantiationName,
    UidMapBlockTest,
    Combine(
        Values(
            makeSeqRange({ 0, firstUid - 1 }),
            makeSeqRange({ 0, firstUid }),
            makeSeqRange({ 0, firstUid + 1 }),
            makeSeqRange({ 0, lastUid }),
            makeSeqRange({ 0, lastUid + 1000 }),

            makeSeqRange({ firstUid, firstUid }),
            makeSeqRange({ firstUid, firstUid + 1 }),
            makeSeqRange({ firstUid, lastUid }),
            makeSeqRange({ firstUid, lastUid + 1000 }),

            makeSeqRange({ firstUid + 1, firstUid + 1 }),
            makeSeqRange({ firstUid + 1, lastUid }),
            makeSeqRange({ firstUid + 1, lastUid + 1000 }),

            makeSeqRange({ lastUid, lastUid }),
            makeSeqRange({ lastUid, lastUid + 1000 }),

            makeSeqRange({ lastUid + 1000, lastUid + 10000 })),
        Values(1, 10, 12, 100)),
    [](auto info) {
        std::stringstream s;
        auto rangeStart = std::get<0>(info.param).first();
        auto rangeEnd = std::get<0>(info.param).last();
        s << "from_" << rangeStart << "_to_" << rangeEnd << "_with_limit_"
          << std::get<1>(info.param);
        return s.str();
    });
