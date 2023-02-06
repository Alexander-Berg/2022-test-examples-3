#include "blob_list_view.h"
#include "blob_list_writer.h"

#include "helpers.h"
#include "holey_vector.h"

#include <search/plutonium/impl/chunkler/idl/offsets.fbs64.h>
#include <search/plutonium/impl/chunkler/test_helpers/read_write_buffer.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/buffer.h>
#include <util/generic/vector.h>
#include <util/stream/buffer.h>

namespace NPlutonium::NChunkler {

testing::AssertionResult AssertOffsetEqual(
    const char* a_expr,
    const char* b_expr,
    const char* i_expr,
    const NFb::TOffsets* a,
    const TVector<ui64>& b,
    int i
) {
    if (a->Offsets()->Get(i) == b[i]) {
        return testing::AssertionSuccess();
    }
    return testing::AssertionFailure()
        << a_expr << "[" << i_expr << "]" << " != " << b_expr << "[" << i_expr << "] because "
        << a_expr << "[" << i << "]" << " = " << a->Offsets()->Get(i) << " and "
        << b_expr << "[" << i << "]" << " = " << b[i];
}

void CheckOffsets(const NFb::TOffsets* actualOffsets, const TVector<ui64>& expectedOffsets) {
    ASSERT_NE(actualOffsets, nullptr);
    ASSERT_EQ(actualOffsets->Offsets()->size(), expectedOffsets.size());
    for (size_t i = 0; i < expectedOffsets.size(); ++i) {
        EXPECT_PRED_FORMAT3(AssertOffsetEqual, actualOffsets, expectedOffsets, i);
    }
}

void WriteStringsToBlobList(IOutputStream* offsets, IOutputStream* blobs, const TVector<std::pair<ui64, TString>>& data) {
    TBlobListWriter writer(offsets, blobs);
    for (const auto& [id, item] : data) {
        writer.Write(id, AsBytesRef(item));
    }
    writer.Finish();
}

void ReadStringsFromBlobList(TBlob offsets, TBlob blobs, const TVector<std::pair<ui64, TString>>& expectedData) {
    TBlobListView view(std::move(offsets), std::move(blobs));

    if (view.empty()) {
        ASSERT_TRUE(expectedData.empty());
        return;
    }

    if (expectedData.empty()) {
        for (size_t i = 0, imax = view.size(); i < imax; ++i) {
            ASSERT_TRUE(view[i].empty());
        }
        return;
    }

    size_t viewIndex = 0;
    size_t expIndex = 0;
    for (size_t viewEnd = view.size(); viewIndex < viewEnd && expIndex < expectedData.size(); ++viewIndex) {
        const auto& [id, data] = expectedData[expIndex];
        if (viewIndex < id) {
            ASSERT_TRUE(view[viewIndex].empty());
        } else {
            ASSERT_EQ(AsStrBuf(view[viewIndex]), data);
            ++expIndex;
        }
    }
    ASSERT_EQ(expIndex, expectedData.size());
    ASSERT_EQ(viewIndex, expectedData.back().first + 1);
}

void TestStringReadWrite(const TVector<std::pair<ui64, TString>>& testData) {
    TReadWriteBuffer offsets;
    TReadWriteBuffer blobs;
    WriteStringsToBlobList(offsets.StartWriting(), blobs.StartWriting(), testData);
    ReadStringsFromBlobList(offsets.Read(), blobs.Read(), testData);
}

TVector<std::pair<ui64, TString>> CreateHoleyBlobListViewAndReadIt(const TVector<std::pair<ui64, TString>>& testData, const TVector<ui64>& holes) {
    TReadWriteBuffer offsets;
    TReadWriteBuffer blobs;
    WriteStringsToBlobList(offsets.StartWriting(), blobs.StartWriting(), testData);

    using THoleyBlobListView = THoleyVectorBaseWithErase<TBlobListView, TArrayRef<const char>>;

    THoleyBlobListView listView(WithHolesTag{}, holes, offsets.Read(), blobs.Read());

    TVector<std::pair<ui64, TString>> result;
    listView.ForEach([&result](size_t key, TArrayRef<const ui8> value) {
        result.emplace_back(key, AsStrBuf(value));
    });
    return result;
}

TEST(BlobListStorage, OffsetsReadWrite) {
    TVector<ui64> testData{10, 25, 234};

    flatbuffers64::FlatBufferBuilder builder;
    BuildFbOffsets(builder, testData);
    flatbuffers64::Verifier verifier(builder.GetBufferPointer(), builder.GetSize());
    NFb::VerifyTOffsetsBuffer(verifier);

    CheckOffsets(NFb::GetTOffsets(builder.GetBufferPointer()), testData);
}

TEST(BlobListStorage, EmptyRead) {
    TestStringReadWrite({});
}

TEST(BlobListStorage, Simple) {
    TestStringReadWrite({
        {0, "foo"},
        {1, "bar"},
        {2, "foobar"}
    });
}

TEST(BlobListStorage, WithHoles) {
    TestStringReadWrite({
        {0, "foo"},
        {2, "bar"}
    });
}

TEST(BlobListStorage, HoleyBlobListView) {
    const TVector<std::pair<ui64, TString>> inputData {
        {0, "foo"},
        {1, "bar"},
        {2, "foobar"}
    };

    const TVector<std::pair<TVector<ui64>, TVector<std::pair<ui64, TString>>>> testCases = {
        { {}, { {0, "foo"}, {1, "bar"}, {2, "foobar"} } },
        { {0, 1, 2}, {} },
        { {1, 2}, { {0, "foo"} } },
        { {0}, { {1, "bar"}, {2, "foobar"} } }
    };

    for (const auto& [holes, expectedContent] : testCases) {
        const TVector<std::pair<ui64, TString>> listContent = CreateHoleyBlobListViewAndReadIt(inputData, holes);
        EXPECT_EQ(listContent, expectedContent);
    }
}

}
