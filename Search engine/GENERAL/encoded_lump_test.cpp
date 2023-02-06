#include "encoded_lump.h"

#include <library/cpp/testing/gtest/gtest.h>


namespace NPlutonium::NChunkler {

void DoTest(ui64 codecId, const TString& body) {
    TBlob encoded = PackEncodedLump(TEncodedLump{
        .CodecId = codecId,
        .Body = TArrayRef<const ui8>{(const ui8*)body.data(), body.size()}
    });
    TEncodedLump unpacked = UnpackEncodedLump(encoded);
    const TString unpackedBody{(const char*)unpacked.Body.data(), unpacked.Body.size()};

    ASSERT_EQ(unpacked.CodecId, codecId);
    ASSERT_EQ(unpackedBody, body);
}

TEST(TEncodedLump, SimplePackUnpack) {
    DoTest(0, "asdf");
    DoTest(42, "somthing longggggggg...");
}

}
