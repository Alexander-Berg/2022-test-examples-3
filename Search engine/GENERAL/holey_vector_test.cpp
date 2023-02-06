#include "holey_vector.h"

#include <iterator>
#include <library/cpp/testing/gtest/gtest.h>

namespace NPlutonium::NChunkler {
namespace {

size_t CountValues(const THoleyVector<int>& v) {
    size_t n = 0;
    v.ForEach([&n](size_t, int) {
        ++n;
    });
    EXPECT_EQ(v.CountKeys(), n);
    return n;
}

template <typename T>
void CheckSize(const THoleyVector<T>& v, size_t expectedSize, size_t expectedCount) {
    EXPECT_EQ(v.Size(), expectedSize);
    EXPECT_EQ(CountValues(v), expectedCount);
}

void CheckIterator(const THoleyVector<int>& v, const TVector<std::pair<size_t, int>>& expected) {
    auto iter = v.Iterator();
    for (auto& [expectedKey, expectedValue] : expected) {
        ASSERT_FALSE(iter.AtEnd());
        EXPECT_EQ(expectedKey, iter.Key());
        EXPECT_EQ(expectedValue, iter.Value());
        iter.Next();
    }
    EXPECT_TRUE(iter.AtEnd());
}

} // anon. namespace

TEST(THoleyVector, BasicOps) {
    THoleyVector<int> v;
    CheckSize(v, 0, 0);
    EXPECT_TRUE(v.Iterator().AtEnd());

    v.Resize(10);
    CheckSize(v, 10, 0);

    EXPECT_FALSE(v.Contains(5));
    v.Set(5, 42);
    EXPECT_TRUE(v.Contains(5));
    EXPECT_EQ(v[5], 42);
    CheckSize(v, 10, 1);

    v.Set(5, 43);
    EXPECT_EQ(v[5], 43);
    CheckSize(v, 10, 1);
    v[5] = 53;
    EXPECT_EQ(v[5], 53);
    CheckSize(v, 10, 1);
    CheckIterator(v, {
        {5, 53}
    });
    {
        auto iter = v.Iterator();
        ASSERT_FALSE(iter.AtEnd());
        EXPECT_EQ(iter.Key(), 5u);
        iter.Value() = 60;
    }
    CheckIterator(v, {
        {5, 60}
    });

    v.Set(7, 46);
    CheckSize(v, 10, 2);
    CheckIterator(v, {
        {5, 60},
        {7, 46}
    });

    v.Erase(5);
    CheckSize(v, 10, 1);
    CheckIterator(v, {
        {7, 46}
    });

    v.Erase(2);
    CheckSize(v, 10, 1);
    CheckIterator(v, {
        {7, 46}
    });

    v.Erase(7);
    CheckSize(v, 10, 0);
    CheckIterator(v, {});

    v.Erase(9);
    CheckSize(v, 10, 0);
}

TEST(THoleyVector, NoHoles) {
    THoleyVector<int> v;
    v.Resize(3);
    v.Set(0, 10);
    v.Set(1, 20);
    v.Set(2, 30);
    CheckIterator(v, {
        {0, 10},
        {1, 20},
        {2, 30}
    });
}

TEST(THoleyVector, CountKeys) {
    THoleyVector<int> v;

    EXPECT_EQ(v.CountKeys(), 0u);
    v.Resize(3);
    EXPECT_EQ(v.CountKeys(), 0u);

    v.Set(0, 10);
    EXPECT_EQ(v.CountKeys(), 1u);
    v.Set(2, 30);
    EXPECT_EQ(v.CountKeys(), 2u);
    v.Set(2, 35);
    EXPECT_EQ(v.CountKeys(), 2u);

    v.Erase(2);
    EXPECT_EQ(v.CountKeys(), 1u);
    v.Erase(2);
    EXPECT_EQ(v.CountKeys(), 1u);

    v.Set(2, 40);
    EXPECT_EQ(v.CountKeys(), 2u);

    v.Resize(1);
    EXPECT_EQ(v.CountKeys(), 1u);

    v.Resize(0);
    EXPECT_EQ(v.CountKeys(), 0u);
}

TEST(THoleyVector, CollectHoles) {
    THoleyVector<int> v;
    v.Resize(3);

    TVector<int> holes;
    TVector<int> expectedHoles{0, 1, 2};
    v.CollectHoles(std::back_inserter(holes));
    EXPECT_EQ(holes, expectedHoles);

    v.Set(1, 10);
    expectedHoles = {0, 2};
    holes.clear();
    v.CollectHoles(std::back_inserter(holes));
    EXPECT_EQ(holes, expectedHoles);

    v.Set(0, 20);
    v.Set(2, 30);
    expectedHoles = {};
    holes.clear();
    v.CollectHoles(std::back_inserter(holes));
    EXPECT_EQ(holes, expectedHoles);
}

}
