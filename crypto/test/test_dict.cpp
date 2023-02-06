#include <crypta/lib/native/id_obfuscator/dict/dict.h>

#include <library/cpp/testing/gtest/gtest.h>

TEST(TDict, Dummy) {
    using namespace NCrypta::NStyx;

    const ui64 puid = 1'000'000'001'000'000'000ull;
    const auto timestamp = TInstant::Seconds(1'600'000'000);
    const TString hash = "foo";

    TDict::TStorage storage = {
            {puid, {.Timestamp = timestamp, .Hash = hash}}
    };
    TDict dict(std::move(storage));

    EXPECT_EQ(timestamp, dict.GetPuidInfo(puid)->Timestamp);
    EXPECT_EQ(hash, dict.GetPuidInfo(puid)->Hash);

    EXPECT_EQ(Nothing(), dict.GetPuidInfo(puid + 1));
}
