#include <library/cpp/testing/unittest/gtest.h>
#include <market/idx/stats/src/GLRangeCalculator.h>

TRangeCalculator::TRanges GetRanges(const TRangeCalculator::TFloatMap& values)
{
    TRangeCalculator calculator;
    calculator.Store(values, 1);
    return calculator.Calculate();
}

TEST(TestGLRangeCalculator, test1)
{
    TRangeCalculator::TFloatMap values;
    values[1650] = 4;
    values[3000] = 1;
    values[3400] = 5;

    const TRangeCalculator::TRanges& ranges = GetRanges(values);
    ASSERT_EQ(2U, ranges.size());
    ASSERT_EQ(1650, ranges[0].first);
    ASSERT_EQ(1650, ranges[0].second);
    ASSERT_EQ(1650, ranges[1].first);
    ASSERT_EQ(1650, ranges[1].second);
}

TEST(TestGLRangeCalculator, test2)
{
    TRangeCalculator::TFloatMap values;
    values[1650] = 4;
    values[1900] = 4;
    values[2000] = 1;
    values[3400] = 1;
    values[3900] = 1;
    values[4000] = 1;
    values[4100] = 1;

    const TRangeCalculator::TRanges& ranges = GetRanges(values);
    ASSERT_EQ(3U, ranges.size());
    ASSERT_EQ(1650, ranges[0].first);
    ASSERT_EQ(1650, ranges[0].second);
    ASSERT_EQ(1650, ranges[1].first);
    ASSERT_EQ(1900, ranges[1].second);
    ASSERT_EQ(1900, ranges[2].first);
    ASSERT_EQ(1900, ranges[2].second);
}

TEST(TestGLRangeCalculator, test3)
{
    TRangeCalculator::TFloatMap values;
    values[1650] = 4;
    values[1900] = 4;
    values[2000] = 7;
    values[3400] = 1;
    values[3900] = 1;
    values[4000] = 1;
    values[4100] = 1;

    const TRangeCalculator::TRanges& ranges = GetRanges(values);
    ASSERT_EQ(3U, ranges.size());
    ASSERT_EQ(1650, ranges[0].first);
    ASSERT_EQ(1900, ranges[0].second);
    ASSERT_EQ(1900, ranges[1].first);
    ASSERT_EQ(2000, ranges[1].second);
    ASSERT_EQ(2000, ranges[2].first);
    ASSERT_EQ(2000, ranges[2].second);
}


TEST(TestGLRangeCalculator, test4)
{
    TRangeCalculator::TFloatMap values;
    values[1650] = 4;
    values[1900] = 4;
    values[2000] = 7;
    values[3400] = 1;
    values[3900] = 12;
    values[4000] = 1;
    values[4100] = 17;

    const TRangeCalculator::TRanges& ranges = GetRanges(values);
    ASSERT_EQ(2U, ranges.size());
    ASSERT_EQ(3900, ranges[0].first);
    ASSERT_EQ(4100, ranges[0].second);
    ASSERT_EQ(4100, ranges[1].first);
    ASSERT_EQ(4100, ranges[1].second);
}
