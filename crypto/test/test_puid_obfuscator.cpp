#include <crypta/lib/native/id_obfuscator/puid_obfuscator.h>

#include <crypta/lib/native/id_obfuscator/dict/dict_serializer.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/string/cast.h>

const NCrypta::TPuidObfuscator::TPuidType PUID_64 = 4ull * 1024 * 1024 * 1024;
const TString REF_HASH_64 = "58cd73d46f40859b64417964454423e9";  // precomputed

TEST(TPuidObfuscator, HashPuid) {
    const TString& dictFileName = "";
    
    const auto& puid64Str = ToString(PUID_64);

    const NCrypta::TPuidObfuscator puidObfuscator(dictFileName);

    EXPECT_EQ(REF_HASH_64, puidObfuscator.GetHash(PUID_64, 0).GetRef());
    EXPECT_EQ(REF_HASH_64, puidObfuscator.GetHash(puid64Str, 0).GetRef());

    const ui64 unixtime = 1;  // providing non-zero unixtime has no effect for puids absent in dictionary
    EXPECT_EQ(REF_HASH_64, puidObfuscator.GetHash(PUID_64, unixtime).GetRef());
}

TEST(TPuidObfuscator, TestZeroPadding) {
    const NCrypta::TPuidObfuscator puidObfuscator("");

    const NCrypta::TPuidObfuscator::TPuidType puid = 111826064;  // puid and refHash found by brute force search
    const TString refHash = "08cc2ea72e597d89597379e3c248dd9f";

    const auto hash = puidObfuscator.GetHash(puid, 0).GetRef();
    EXPECT_EQ(refHash, hash);
}

TEST(TPuidObfuscator, HashSize) {
    const NCrypta::TPuidObfuscator puidObfuscator("");

    for (ui64 puid = 100500; puid < 100600; ++puid) {
        EXPECT_EQ(32ull, puidObfuscator.GetHash(puid, 0)->size());
    }
}

NCrypta::NStyx::TDict GetDict() {
    NCrypta::NStyx::TDict::TStorage storage = {
            {1, {.Timestamp = TInstant::Seconds(1), .Hash = "foo"}},
    };
    return NCrypta::NStyx::TDict(std::move(storage));
}

TEST(TPuidObfuscator, TestWithDictionary) {
    using namespace NCrypta::NStyx;

    const TString& fileName = "test_with_dictionary";
    NDictSerializer::Serialize(GetDict(), fileName);

    const NCrypta::TPuidObfuscator puidObfuscator(fileName);

    EXPECT_EQ(Nothing(), puidObfuscator.GetHash(1, 0));
    EXPECT_EQ("foo", puidObfuscator.GetHash(1, 1));

    // For puid absent in dictionary, default hash is returned:
    EXPECT_EQ(REF_HASH_64, puidObfuscator.GetHash(PUID_64, 0));
}
