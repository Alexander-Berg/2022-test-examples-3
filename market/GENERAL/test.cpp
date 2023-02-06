#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <market/library/itertools/itertools.h>

#include <util/generic/vector.h>
#include <util/string/join.h>

#include <algorithm>
#include <iterator>


using namespace Market;
using TIntVector = TVector<int>;
using TMyScalingIter = TForwardScalingIterator<TIntVector::const_iterator>;


void DoTest(float factor,
            const TIntVector& input,
            const TIntVector& expected)
{
    TIntVector result;
    TMyScalingIter first(input.begin(), input.end(), factor);
    TMyScalingIter last(input.end());
    std::copy(first, last, std::back_inserter(result));
    EXPECT_EQ(expected, result);
}

TEST(IterTools, ForwardScalingIteratorPreserve5to5)
{
    DoTest(1, {0,1,2,3,4}, {0,1,2,3,4});
}

TEST(IterTools, ForwardScalingIteratorDouble0)
{
    DoTest(2, {}, {});
}

TEST(IterTools, ForwardScalingIteratorDouble1)
{
    DoTest(2, {0}, {0,0});
}

TEST(IterTools, ForwardScalingIteratorStrech5to10)
{
    DoTest(2, {0,1,2,3,4}, {0,0,1,1,2,2,3,3,4,4});
}

TEST(IterTools, ForwardScalingIteratorStrech1point8times)
{
    DoTest(1.8, {1,2,3,4,5}, {1,1,2,2,3,3,4,4,5});
}

TEST(IterTools, ForwardScalingIteratorShrink10to5)
{
    DoTest(0.5, {0,1,2,3,4,5,6,7,8,9}, {0,2,4,6,8});
}

TEST(IterTools, ForwardScalingIteratorShrink10to3)
{
    DoTest(0.3, {0,1,2,3,4,5,6,7,8,9}, {0,4,7});
}

TEST(IterTools, ForwardScalingIteratorShrink10to2)
{
    DoTest(0.2, {0,1,2,3,4,5,6,7,8,9}, {0,5});
}
