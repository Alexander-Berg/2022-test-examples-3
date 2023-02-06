#include "blob_packer.h"
#include "blob_unpacker.h"

#include "helpers.h"

#include <search/plutonium/helpers/hasher/calc_hash.h>

#include <search/plutonium/impl/chunkler/idl/serialized_codec.fbs64.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/stream/buffer.h>
#include <util/string/split.h>

namespace NPlutonium::NChunkler {

constexpr TStringBuf TEST_DATA = R"(
foo,bar,foobbar
{"key":"value"};
{"key":"another value"};
{"key":"new value"};
{"foo":"bar"};
{"key":"new value", "foo": "bbar"};
{"a":"bc", "foo": "bbar"};
)";

void TestPackingUnpacking(const TBlobPacker& packer, TStringBuf testData) {
    TBlob codecBlob = packer.SerializeCodec();

    TBlob packedBlob = packer.Pack(AsBytesRef(testData));

    TBlobUnpacker unpacker(codecBlob);
    TBlob unpackedBlob = unpacker.Unpack(packedBlob);

    EXPECT_EQ(AsStrBuf(unpackedBlob), testData);
}

TString AppendHash(TStringBuf str) {
    const TString strHash = CalcHash(str);
    return TString::Join(str, '#', strHash);
}

void CheckHash(TStringBuf str) {
    TStringBuf left, right;
    bool result = str.TrySplit('#', left, right);
    EXPECT_TRUE(result);
    EXPECT_EQ(CalcHash(left), right);
}

TEST(BlobPacking, SimplePackUnpack) {
    const TString codecName = "lz4";
    TBlobPacker packer{TBlobPackerOptions{.MainCodec = codecName}};
    EXPECT_TRUE(packer.GetCurrentCodecName().StartsWith(codecName));
    EXPECT_TRUE(packer.IsTrained());

    TestPackingUnpacking(packer, TEST_DATA);
}

TEST(BlobPacking, TrainCodec) {
    const TString codecName = "zstd08d-1";
    TBlobPacker packer{TBlobPackerOptions{.MainCodec = codecName, .TrainSampleSize = 10240}};
    EXPECT_EQ(packer.GetCurrentCodecName(), codecName);
    EXPECT_FALSE(packer.IsTrained());

    ui64 trainCount = 0;
    while (!packer.IsTrained()) {
        for (auto line : StringSplitter(TEST_DATA).Split('\n').SkipEmpty()) {
            packer.AddBlobForTraining(trainCount, AsBytesRef(AppendHash(line.Token())));
            ++trainCount;
            if (packer.IsTrained()) {
                break;
            }
        }
    }
    EXPECT_EQ(packer.GetCurrentCodecName(), codecName);

    TestPackingUnpacking(packer, TEST_DATA);

    THolder<IBlobIterator> trainingBlobs = packer.PackTrainingBlobs();

    TBlobUnpacker unpacker(packer.SerializeCodec());
    ui64 count = 0;
    for (; !trainingBlobs->AtEnd(); trainingBlobs->Next()) {
        const auto& [index, packedBlob] = trainingBlobs->Current();
        EXPECT_EQ(index, count);
        CheckHash(AsStrBuf(unpacker.Unpack(packedBlob)));
        ++count;
    }
    EXPECT_EQ(trainCount, count);
}

}
