#include <market/report/library/relevance/relevance/doc_indexes.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <algorithm>
#include <iterator>


TVector<size_t> ToVec(const TDocIndexes& indexes) {
    TVector<size_t> result;
    std::copy(indexes.begin(), indexes.end(), std::back_inserter(result));
    return result;
}

TEST(DocIndexesTest, Slice) {
    using TVec = TVector<size_t>;

    const TVec vector {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    const TDocIndexes indexes(vector);

    EXPECT_EQ(ToVec(indexes), (TVec{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));

    // Stride 1
    EXPECT_EQ(ToVec(indexes.Slice(0, 10, 1)), (TVec{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(0, 3, 1)), (TVec{0, 1, 2}));
    EXPECT_EQ(ToVec(indexes.Slice(2, 5, 1)), (TVec{2, 3, 4, 5, 6}));
    EXPECT_EQ(ToVec(indexes.Slice(4, 6, 1)), (TVec{4, 5, 6, 7, 8, 9}));

    EXPECT_EQ(ToVec(indexes.Slice(0, 20, 1)), (TVec{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(5, 10, 1)), (TVec{5, 6, 7, 8, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(10, 10, 1)), (TVec{}));

    EXPECT_EQ(ToVec(indexes.Slice(0, 1)), (TVec{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(7, 1)), (TVec{7, 8, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(10, 1)), (TVec{}));

    // Stride 2
    EXPECT_EQ(ToVec(indexes.Slice(0, 5, 2)), (TVec{0, 2, 4, 6, 8}));
    EXPECT_EQ(ToVec(indexes.Slice(1, 5, 2)), (TVec{1, 3, 5, 7, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(0, 2, 2)), (TVec{0, 2}));
    EXPECT_EQ(ToVec(indexes.Slice(1, 2, 2)), (TVec{1, 3}));
    EXPECT_EQ(ToVec(indexes.Slice(2, 3, 2)), (TVec{2, 4, 6}));
    EXPECT_EQ(ToVec(indexes.Slice(3, 3, 2)), (TVec{3, 5, 7}));
    EXPECT_EQ(ToVec(indexes.Slice(6, 3, 2)), (TVec{6, 8}));
    EXPECT_EQ(ToVec(indexes.Slice(7, 4, 2)), (TVec{7, 9}));

    EXPECT_EQ(ToVec(indexes.Slice(0, 2)), (TVec{0, 2, 4, 6, 8}));
    EXPECT_EQ(ToVec(indexes.Slice(1, 2)), (TVec{1, 3, 5, 7, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(6, 2)), (TVec{6, 8}));
    EXPECT_EQ(ToVec(indexes.Slice(7, 2)), (TVec{7, 9}));

    // Stride 3
    EXPECT_EQ(ToVec(indexes.Slice(0, 3)), (TVec{0, 3, 6, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(1, 3)), (TVec{1, 4, 7   }));
    EXPECT_EQ(ToVec(indexes.Slice(2, 3)), (TVec{2, 5, 8   }));

    // Stride 4
    EXPECT_EQ(ToVec(indexes.Slice(0, 4)), (TVec{0, 4, 8}));
    EXPECT_EQ(ToVec(indexes.Slice(1, 4)), (TVec{1, 5, 9}));
    EXPECT_EQ(ToVec(indexes.Slice(2, 4)), (TVec{2, 6   }));
    EXPECT_EQ(ToVec(indexes.Slice(3, 4)), (TVec{3, 7   }));

    // Stride 5
    EXPECT_EQ(ToVec(indexes.Slice(0, 5)), (TVec{0, 5}));
    EXPECT_EQ(ToVec(indexes.Slice(1, 5)), (TVec{1, 6}));
    EXPECT_EQ(ToVec(indexes.Slice(2, 5)), (TVec{2, 7}));
    EXPECT_EQ(ToVec(indexes.Slice(3, 5)), (TVec{3, 8}));
    EXPECT_EQ(ToVec(indexes.Slice(4, 5)), (TVec{4, 9}));
}
