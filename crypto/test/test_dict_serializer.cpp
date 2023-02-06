#include <crypta/lib/native/id_obfuscator/dict/dict_serializer.h>
#include <crypta/lib/native/id_obfuscator/puid_obfuscator.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/string.h>
#include <util/stream/file.h>

using namespace NCrypta::NStyx;

const ui64 BASE_PUID = 1'000'000'000'000ull;
const ui64 BASE_TIMESTAMP = 1'600'000'000ull;
const TString HASH = "aabbccddeeffaabbccddeeffaabbccdd";

TFsPath GetDictDirectory() {
    return GetOutputPath() / "dicts";
}

TString GetDictionaryPath(int entryCount, const TString& name) {
    const auto& tmpDir = GetDictDirectory();
    tmpDir.MkDirs();

    const auto& dictFilePath = tmpDir / name;

    TFileOutput out(dictFilePath);
    for (int i = 0; i < entryCount; ++i) {
        out << "- puid: " << BASE_PUID + i << Endl;
        out << "  timestamp: " << BASE_TIMESTAMP + i << Endl;
        out << "  hash: \"" << HASH << "\"" << Endl;
    }
    out.Finish();

    return dictFilePath.c_str();
}

TEST(Serializer, EmptyDict) {
    const auto& emptyDict = NDictSerializer::Deserialize(GetDictionaryPath(0, "zero"));
    EXPECT_EQ(0ul, emptyDict.GetStorage().size());
    EXPECT_EQ(Nothing(), emptyDict.GetPuidInfo(BASE_PUID));
}

TEST(Serializer, Sto500) {
    const ui64 entryCount = 100500;
    const auto& sto500 = NDictSerializer::Deserialize(GetDictionaryPath(entryCount, "100500"));
    const ui64 lastIndex = entryCount - 1;
    const auto lastPuid = BASE_PUID + lastIndex;

    EXPECT_EQ(HASH, sto500.GetPuidInfo(lastPuid)->Hash);
    EXPECT_EQ(TInstant::Seconds(BASE_TIMESTAMP + lastIndex), sto500.GetPuidInfo(lastPuid)->Timestamp);

    EXPECT_EQ(Nothing(), sto500.GetPuidInfo(lastPuid + 1));
}

TEST(Serializer, SerializeDeserialize) {
    const ui64 puid1 = 1000000000100500;
    const ui64 puid2 = 1000000000100501;
    const auto timestamp1 = TInstant::Seconds(1600000000);
    const auto timestamp2 = TInstant::Seconds(1600000001);
    const TString hash1 = "aabbccddaabbccddaabbccddaabbccdd";
    const TString hash2 = "eebbccddaabbccddaabbccddaabbccee";

    TDict::TStorage storage;
    storage[puid1] = TDict::TDictEntry{.Timestamp = timestamp1, .Hash = hash1};
    storage[puid2] = TDict::TDictEntry{.Timestamp = timestamp2, .Hash = hash2};

    TDict dict(std::move(storage));

    const auto& entry1 = dict.GetPuidInfo(puid1);
    EXPECT_EQ(timestamp1, entry1->Timestamp);
    EXPECT_EQ(hash1, entry1->Hash);

    const auto& entry2 = dict.GetPuidInfo(puid2);
    EXPECT_EQ(timestamp2, entry2->Timestamp);
    EXPECT_EQ(hash2, entry2->Hash);

    const TString dictPath = (GetDictDirectory() / "serialized").c_str();
    NDictSerializer::Serialize(dict, dictPath);

    const auto& deserialized = NDictSerializer::Deserialize(dictPath);

    const auto& e1 = deserialized.GetPuidInfo(puid1);
    EXPECT_EQ(timestamp1, e1->Timestamp);
    EXPECT_EQ(hash1, e1->Hash);

    const auto& e2 = deserialized.GetPuidInfo(puid2);
    EXPECT_EQ(timestamp2, e2->Timestamp);
    EXPECT_EQ(hash2, e2->Hash);

    EXPECT_EQ(dict.GetStorage(), deserialized.GetStorage());
}
