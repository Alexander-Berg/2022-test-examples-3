#include "doc_info.h"
#include "helpers.h"

#include <search/plutonium/impl/chunkler/test_helpers/read_write_buffer.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/vector.h>
#include <util/stream/buffer.h>
#include <util/string/hex.h>


namespace NPlutonium::NChunkler {

TDocInfosHolder BuildDocInfos(const TVector<TDocInfo>& docInfos) {
    TDocInfosBuilder builder;
    for (ui64 i = 0; i < docInfos.size(); ++i) {
        builder.Add(i, docInfos[i]);
    }
    TReadWriteBuffer buffer;
    {
        IOutputStream* ostream = buffer.StartWriting();
        builder.Write(ostream);
        ostream->Finish();
    }
    return TDocInfosHolder{buffer.Read()};
}

void DoTest(const TVector<TDocInfo>& docInfos) {
    TDocInfosHolder fbDocs = BuildDocInfos(docInfos);
    ASSERT_EQ(fbDocs.Size(), docInfos.size());

    for (ui64 i = 0; i < docInfos.size(); ++i) {
        const TDocInfo fbDoc = fbDocs[i];
        ASSERT_EQ(fbDoc.Timestamp, docInfos[i].Timestamp);
    }
}

TEST(TDocInfo, ReadWriteSomething) {
    DoTest(TVector<TDocInfo>{
        TDocInfo{.Timestamp = TInstant::Seconds(234)},
        TDocInfo{.Timestamp = TInstant::Seconds(42)},
        TDocInfo{.Timestamp = TInstant::Seconds(100)}
    });
}

TEST(TDocInfo, ReadWriteEmpty) {
    DoTest(TVector<TDocInfo>{});
}

TEST(TDocInfo, EmptyDocInfoHolder) {
    TDocInfosHolder fbDocs(TBlob{});
    ASSERT_EQ(fbDocs.Size(), 0u);
}

TEST(TDocInfo, Get) {
    const TDocInfo defaultInfo{.Timestamp = TInstant::Seconds(400)};
    const TDocInfosHolder docInfos = BuildDocInfos(TVector<TDocInfo>{
        TDocInfo{.Timestamp = TInstant::Seconds(234)}
    });
    const TDocInfo info0 = docInfos.Get(0, defaultInfo);
    const TDocInfo info1 = docInfos.Get(1, defaultInfo);

    ASSERT_EQ(info0.Timestamp, TInstant::Seconds(234));
    ASSERT_EQ(info1.Timestamp, TInstant::Seconds(400));
}

TEST(TDocInfo, LowerBoundByTimestampDesc) {
    const TDocInfosHolder noDocs = BuildDocInfos({});
    const TDocInfosHolder singleDoc = BuildDocInfos(TVector<TDocInfo>{
        TDocInfo{.Timestamp = TInstant::Seconds(234)}
    });
    const TDocInfosHolder docs3 = BuildDocInfos(TVector<TDocInfo>{
        TDocInfo{.Timestamp = TInstant::Seconds(234)},
        TDocInfo{.Timestamp = TInstant::Seconds(200)},
        TDocInfo{.Timestamp = TInstant::Seconds(180)}
    });

    ASSERT_EQ(noDocs.LowerBoundByTimestampDesc(TInstant::Seconds(42)), 0u);
    ASSERT_EQ(singleDoc.LowerBoundByTimestampDesc(TInstant::Seconds(42)), 1u);
    ASSERT_EQ(docs3.LowerBoundByTimestampDesc(TInstant::Seconds(42)), 3u);

    ASSERT_EQ(singleDoc.LowerBoundByTimestampDesc(TInstant::Seconds(300)), 0u);
    ASSERT_EQ(docs3.LowerBoundByTimestampDesc(TInstant::Seconds(300)), 0u);
    ASSERT_EQ(singleDoc.LowerBoundByTimestampDesc(TInstant::Seconds(234)), 0u);
    ASSERT_EQ(docs3.LowerBoundByTimestampDesc(TInstant::Seconds(234)), 0u);

    ASSERT_EQ(singleDoc.LowerBoundByTimestampDesc(TInstant::Seconds(190)), 1u);
    ASSERT_EQ(docs3.LowerBoundByTimestampDesc(TInstant::Seconds(190)), 2u);

    ASSERT_EQ(docs3.LowerBoundByTimestampDesc(TInstant::Seconds(180)), 2u);
    ASSERT_EQ(docs3.LowerBoundByTimestampDesc(TInstant::Seconds(170)), 3u);
}

constexpr TStringBuf TEST_DOCINFOS_V1 =
    "10000000000000000000060010000800060000000000000008000000000000000300000000000000"
    "00E1F5050000000000C2EB0B0000000000A3E11100000000";

TEST(TDocInfo, TestSerializedDocInfos) {
    TDocInfosHolder docInfos{TBlob::FromString(HexDecode(TEST_DOCINFOS_V1))};
    ASSERT_EQ(docInfos.Size(), 3u);
    ASSERT_EQ(docInfos[0].Timestamp, TInstant::Seconds(100));
    ASSERT_EQ(docInfos[1].Timestamp, TInstant::Seconds(200));
    ASSERT_EQ(docInfos[2].Timestamp, TInstant::Seconds(300));
}

}
