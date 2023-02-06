#include <market/report/library/stat_block_id/stat_block_id.h>
#include <market/report/test/global_mock.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport;

bool HasOnlyDigits(const TString& s) {
    for (size_t i = 0; i < s.length(); ++i)
        if (!isdigit(s[i]))
            return false;

    return true;
}

TEST(StatBlockIdTest, GeneratesBsStatBlockId) {
    TString blockId = GenerateBsStatBlockId();
    EXPECT_EQ(25, blockId.length());
    EXPECT_TRUE(HasOnlyDigits(blockId));
    EXPECT_EQ('2', blockId[0]);
}

TEST(StatBlockIdTest, PatchsBsStatBlockId) {
    TString origBlockId = "2050995595120140516030533";
    TString patchedBlockId = origBlockId;
    bool patched = PatchBsStatBlockId(patchedBlockId);
    EXPECT_TRUE(patched);
    EXPECT_NE(origBlockId, patchedBlockId);
}

TEST(StatBlockIdTest, PatchsStatBlockId) {
    TString origBlockId = "2050995595120140516030533";
    TString newBlockId = PatchStatBlockId(origBlockId);

    EXPECT_NE(origBlockId, newBlockId);
    EXPECT_EQ('2', newBlockId[0]);
    EXPECT_TRUE(HasOnlyDigits(newBlockId));
}
