#include <crypta/lib/native/id_obfuscator/id_hash/id_hash.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/string.h>

using namespace NCrypta::NStyx;

namespace {
    const NIdHashPrivate::TPuid PUID = 4ull * 1024 * 1024 * 1024;
    const TString REF_DEFAULT_HASH = "58cd73d46f40859b64417964454423e9";
    const TString REF_HASH_30 = "364206ed5dbce8dee8fa77ae5e22b0a2";
    const TString REF_HASH_31 = "e474d974c5795c958a8d21c73b7f7e29";
}

TEST(NIdHashPrivate, ComputeDefaultHash) {
    EXPECT_EQ(REF_DEFAULT_HASH, NIdHashPrivate::ComputeDefaultHash(PUID));
}

TEST(NIdHashPrivate, ComputeHash) {
    EXPECT_EQ(REF_HASH_30, NIdHashPrivate::ComputeHash(PUID, 30));
    EXPECT_EQ(REF_HASH_31, NIdHashPrivate::ComputeHash(PUID, 31));
}
