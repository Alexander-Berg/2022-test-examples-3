#include <common/seq_range.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace yimap;

seq_range makeSeqRange(const std::initializer_list<range_t>& ranges)
{
    seq_range res;
    for (auto&& range : ranges)
    {
        res.insert(range);
    }
    return res;
}

auto testRange = makeSeqRange({ { 10, 100 }, { 105, 200 }, { 210, 300 } });

TEST(SEQ_RANGE, trimLeftBeforeStart)
{
    ASSERT_EQ(
        trim_left_copy(testRange, 5), makeSeqRange({ { 10, 100 }, { 105, 200 }, { 210, 300 } }));
}

TEST(SEQ_RANGE, trimLeftStartValue)
{
    ASSERT_EQ(
        trim_left_copy(testRange, 10), makeSeqRange({ { 11, 100 }, { 105, 200 }, { 210, 300 } }));
}

TEST(SEQ_RANGE, trimLeftFirstRange)
{
    ASSERT_EQ(
        trim_left_copy(testRange, 50), makeSeqRange({ { 51, 100 }, { 105, 200 }, { 210, 300 } }));
}

TEST(SEQ_RANGE, trimLeftOnRangeEnd)
{
    ASSERT_EQ(trim_left_copy(testRange, 100), makeSeqRange({ { 105, 200 }, { 210, 300 } }));
}

TEST(SEQ_RANGE, trimLeftBetweenRanges)
{
    ASSERT_EQ(trim_left_copy(testRange, 102), makeSeqRange({ { 105, 200 }, { 210, 300 } }));
}

TEST(SEQ_RANGE, trimLeftSecondRangeStart)
{
    ASSERT_EQ(trim_left_copy(testRange, 105), makeSeqRange({ { 106, 200 }, { 210, 300 } }));
}

TEST(SEQ_RANGE, trimLeftSecondRangeEnd)
{
    ASSERT_EQ(trim_left_copy(testRange, 200), makeSeqRange({ { 210, 300 } }));
}

TEST(SEQ_RANGE, trimLeftEndValue)
{
    ASSERT_EQ(trim_left_copy(testRange, 300), makeSeqRange({}));
}

TEST(SEQ_RANGE, trimLeftAfterEnd)
{
    ASSERT_EQ(trim_left_copy(testRange, 350), makeSeqRange({}));
}

TEST(SEQ_RANGE, trimLeftRangeWithSignleElement)
{
    auto seqRangeWithSingleElement = makeSeqRange({ { 5, 5 }, { 10, 100 } });
    ASSERT_EQ(trim_left_copy(seqRangeWithSingleElement, 5), makeSeqRange({ { 10, 100 } }));
}

TEST(SEQ_RANGE, trimRightBeforeStart)
{
    ASSERT_EQ(trim_right_copy(testRange, 5), makeSeqRange({}));
}

TEST(SEQ_RANGE, trimRightStartValue)
{
    ASSERT_EQ(trim_right_copy(testRange, 10), makeSeqRange({}));
}

TEST(SEQ_RANGE, trimRightFirstRange)
{
    ASSERT_EQ(trim_right_copy(testRange, 50), makeSeqRange({ { 10, 49 } }));
}

TEST(SEQ_RANGE, trimRightOnRangeEnd)
{
    ASSERT_EQ(trim_right_copy(testRange, 100), makeSeqRange({ { 10, 99 } }));
}

TEST(SEQ_RANGE, trimRightBetweenRanges)
{
    ASSERT_EQ(trim_right_copy(testRange, 102), makeSeqRange({ { 10, 100 } }));
}

TEST(SEQ_RANGE, trimRightSecondRangeStart)
{
    ASSERT_EQ(trim_right_copy(testRange, 105), makeSeqRange({ { 10, 100 } }));
}

TEST(SEQ_RANGE, trimRightSecondRangeEnd)
{
    ASSERT_EQ(trim_right_copy(testRange, 200), makeSeqRange({ { 10, 100 }, { 105, 199 } }));
}

TEST(SEQ_RANGE, trimRightEndValue)
{
    ASSERT_EQ(
        trim_right_copy(testRange, 300), makeSeqRange({ { 10, 100 }, { 105, 200 }, { 210, 299 } }));
}

TEST(SEQ_RANGE, trimRightAfterEnd)
{
    ASSERT_EQ(
        trim_right_copy(testRange, 350), makeSeqRange({ { 10, 100 }, { 105, 200 }, { 210, 300 } }));
}

TEST(SEQ_RANGE, trimRightRangeWithSignleElement)
{
    auto seqRangeWithSingleElement = makeSeqRange({ { 5, 5 }, { 10, 100 } });
    ASSERT_EQ(trim_right_copy(seqRangeWithSingleElement, 5), makeSeqRange({}));

    seqRangeWithSingleElement = makeSeqRange({ { 10, 100 }, { 150, 150 } });
    ASSERT_EQ(trim_right_copy(seqRangeWithSingleElement, 150), makeSeqRange({ { 10, 100 } }));
}
