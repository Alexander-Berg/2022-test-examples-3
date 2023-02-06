#include "multi_lump_packer.h"
#include "multi_lump_unpacker.h"

#include "helpers.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/vector.h>

namespace NPlutonium::NChunkler {

void CheckUnpackerContent(const TMultiLumpUnpacker& unpacker, const TVector<std::pair<TString, TString>>& expectedContent) {
    auto expectedIt = expectedContent.begin();
    for (auto&& [lumpId, lumpBody] : unpacker) {
        ASSERT_NE(expectedIt, expectedContent.end());
        ASSERT_EQ(expectedIt->first, lumpId);
        ASSERT_EQ(expectedIt->second, AsStrBuf(lumpBody));
        ++expectedIt;
    }
    ASSERT_EQ(expectedIt, expectedContent.end());
}

void DoTest(const TVector<std::pair<TString, TString>>& lumpList) {
    TMultiLumpPacker packer;
    for (auto& [lumpId, lumpBody] : lumpList) {
        packer.AddLump(lumpId, AsBytesRef(lumpBody));
    }
    const TArrayRef<const ui8> packedData = packer.Finish();
    const TBlob packedBlob = TBlob::Copy(packedData.data(), packedData.size());
    TMultiLumpUnpacker unpacker(packedBlob);
    CheckUnpackerContent(unpacker, lumpList);
}

TEST(TMultiLump, SimplePackUnpack) {
    DoTest({
        {"lumpt1", "lump1_content"},
        {"lump2", "lump22"},
        {"another_lump", "something"}
    });
}

TEST(TMultiLump, PackUnpackEmpty) {
    DoTest({});
}

TEST(TMultiLump, PackUnpackSingle) {
    DoTest({
        {"single_lump", "lump_content"}
    });
}

}
