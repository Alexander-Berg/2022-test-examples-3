#include "multi_holey_vector_iterator.h"

#include <library/cpp/testing/gtest/gtest.h>

namespace NPlutonium::NChunkler {
namespace {

THoleyVector<int> CreateVector(size_t size, const TVector<std::pair<size_t, int>>& content) {
    THoleyVector<int> vec;
    [&]() {
        vec.Resize(size);
        for (auto& [k, v] : content) {
            ASSERT_LT(k, size);
            vec.Set(k, v);
        }
    }();
    return vec;
}

template <typename TIterator>
void CheckIterator(TIterator&& iter, const TVector<std::tuple<size_t, size_t, int>>& expected) {
    for (auto& [expectedVector, expectedKey, expectedValue] : expected) {
        ASSERT_FALSE(iter.AtEnd());
        EXPECT_EQ(expectedVector, iter.Key().first);
        EXPECT_EQ(expectedKey, iter.Key().second);
        EXPECT_EQ(expectedValue, iter.Value());
        iter.Next();
    }
    EXPECT_TRUE(iter.AtEnd());
}

template <typename TIterator>
void CheckIteratorAndCount(TIterator&& iter, std::pair<size_t, size_t> expectedCount, const TVector<std::tuple<size_t, size_t, int>>& expected) {
    auto [size, keysCount] = iter.CountSizeAndKeys();
    EXPECT_EQ(size, expectedCount.first);
    EXPECT_EQ(keysCount, expectedCount.second);
    CheckIterator(std::forward<TIterator>(iter), expected);
}

} // anon. namespace

TEST(TMultiHolerVectorIterator, Empty) {
    TMultiHoleyVectorIterator<int> iter({}, {});
    ASSERT_TRUE(iter.AtEnd());

    TVector<THoleyVector<int>> vectors(3);
    iter = TMultiHoleyVectorIterator<int>(vectors, {0, 1});
    ASSERT_TRUE(iter.AtEnd());

    vectors[0].Resize(4);
    vectors[1].Resize(4);
    vectors[2].Resize(4);
    iter = TMultiHoleyVectorIterator<int>(vectors, {0, 1, 2});
    ASSERT_TRUE(iter.AtEnd());
}

TEST(TMultiHolerVectorIterator, SingleVector) {
    THoleyVector<int> vec = CreateVector(3, {
        {0, 10},
        {1, 20},
        {2, 30}
    });
    CheckIteratorAndCount(TMultiHoleyVectorIterator<int>({&vec, 1}, {0}), {3, 3}, {
        {0, 0, 10},
        {0, 1, 20},
        {0, 2, 30}
    });
}

TEST(TMultiHolerVectorIterator, SingleVector2) {
    THoleyVector<int> vec = CreateVector(3, {
        {1, 10},
        {2, 30}
    });
    CheckIteratorAndCount(TMultiHoleyVectorIterator<int>({&vec, 1}, {0}), {3, 2}, {
        {0, 1, 10},
        {0, 2, 30}
    });
}

TEST(TMultiHolerVectorIterator, ThreeVectors) {
    TVector<THoleyVector<int>> vectors = {
        CreateVector(3, {
            {0, 10},
            {1, 20},
            {2, 30}
        }),
        CreateVector(0, {}),
        CreateVector(4, {
            {1, 10},
            {2, 30}
        }),
    };
    CheckIteratorAndCount(TMultiHoleyVectorIterator<int>(vectors, {0, 1, 2}), {7, 5}, {
        {0, 0, 10},
        {0, 1, 20},
        {0, 2, 30},
        {2, 1, 10},
        {2, 2, 30}
    });
    CheckIteratorAndCount(TMultiHoleyVectorIterator<int>(vectors, {1}), {0, 0}, {});
    CheckIteratorAndCount(TMultiHoleyVectorIterator<int>(vectors, {1, 2}), {4, 2}, {
        {2, 1, 10},
        {2, 2, 30}
    });
    CheckIteratorAndCount(TMultiHoleyVectorIterator<int>(vectors, {0, 1}), {3, 3}, {
        {0, 0, 10},
        {0, 1, 20},
        {0, 2, 30}
    });
}

TEST(TLimitedIterator, BasicTest) {
    TVector<THoleyVector<int>> vectors = {
        CreateVector(3, {
            {0, 10},
            {1, 20},
            {2, 30}
        }),
        CreateVector(0, {}),
        CreateVector(4, {
            {1, 10},
            {2, 30}
        }),
    };
    TMultiHoleyVectorIterator<int> emptyIter(vectors, {});
    CheckIterator(CreateLimitedIterator(&emptyIter, 100), {});

    TMultiHoleyVectorIterator<int> emptyIter2(vectors, {1});
    CheckIterator(CreateLimitedIterator(&emptyIter2, 100), {});

    TMultiHoleyVectorIterator<int> fullIter(vectors, {0, 1, 2});
    CheckIterator(CreateLimitedIterator(&fullIter, 100), {
        {0, 0, 10},
        {0, 1, 20},
        {0, 2, 30},
        {2, 1, 10},
        {2, 2, 30}
    });

    TMultiHoleyVectorIterator<int> fullIter2(vectors, {0, 1, 2});
    CheckIterator(CreateLimitedIterator(&fullIter2, 0), {});
    CheckIterator(CreateLimitedIterator(&fullIter2, 1), {
        {0, 0, 10}
    });
    auto limitedIter = CreateLimitedIterator(&fullIter2, 1);
    CheckIterator(limitedIter, {
        {0, 1, 20}
    });
    limitedIter.ResetLimit(2);
    CheckIterator(limitedIter, {
        {0, 2, 30},
        {2, 1, 10}
    });
    limitedIter.ResetLimit(3);
    CheckIterator(limitedIter, {
        {2, 2, 30}
    });
}

}
