#include <market/idx/generation/genlog_dumper/dumpers/ShopCategoryIdsDumper.h>

#include <market/library/flat_helpers/flat_helpers.h>
#include <market/library/libshopsdat/shopsdat.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/tempdir.h>
#include <util/stream/file.h>

namespace {

enum class EType {
    BLUE,
    DSBS,
    CPC,
};

using Record = MarketIndexer::GenerationLog::Record;

Record MakeRecord(EType type, TString shopCategoryPathIds) {
    Record record;
    record.set_shop_category_path_ids(shopCategoryPathIds);
    if (type == EType::BLUE) {
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::REAL));
        record.set_is_blue_offer(true);
    } else if (type == EType::DSBS) {
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::REAL));
        record.set_is_blue_offer(false);
    } else {
        record.set_cpa(static_cast<i32>(NMarket::NBind::ECpa::NO));
        record.set_is_blue_offer(false);
    }
    return record;
}

} // end namespace

TEST(ShopCategoryIdsDumper, Dump)
{
    struct TSpec {
        EType Type;
        TString ShopCategoryPathIds;
    };
    TSpec specs[] = {
        {EType::CPC, ""},
        {EType::CPC, "1"},
        {EType::BLUE, ""},
        {EType::BLUE, "1"},
        {EType::DSBS, ""},
        {EType::DSBS, "1"},
        {EType::DSBS, "1\\2"},
        {EType::DSBS, "1\\2\\444333222111"},
    };
    const size_t numSpecs = Y_ARRAY_SIZE(specs);
    TTempDir dir;
    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeShopCategoryIdsDumper(context);
    for (size_t i = 0; i < numSpecs; i++) {
        dumper->ProcessGenlogRecord(MakeRecord(specs[i].Type, specs[i].ShopCategoryPathIds), i);
    }
    dumper->Finish();

    const auto fileData = TUnbufferedFileInput(dir.Path() / NDumpers::GetShopCategoryIdsFileName()).ReadAll();
    const auto* mappingVec = NMarket::NFlatbufferHelpers::GetTShopCategoryIdsData(fileData)->Offers();
    ASSERT_EQ(numSpecs, mappingVec->size());

    using TResult = TVector<uint64_t>;
    auto check = [&](size_t seqNum, const TResult& expected) {
        const auto* offsets = mappingVec->Get(seqNum);
        const auto* cats = offsets->Categories();
        ASSERT_EQ(expected, TResult(cats->begin(), cats->end()));
    };
    check(0, {});
    check(1, {});
    check(2, {});
    check(3, {});
    check(4, {});
    check(5, {1});
    check(6, {1, 2});
    check(7, {1, 2, 444333222111});
}

TEST(ShopCategoryIdsDumper, Hole)
{
    TTempDir dir;
    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeShopCategoryIdsDumper(context);
    dumper->ProcessGenlogRecord(MakeRecord(EType::DSBS, "1"), 1);
    dumper->ProcessGenlogRecord(MakeRecord(EType::DSBS, "5"), 5);
    dumper->ProcessGenlogRecord(MakeRecord(EType::DSBS, "6"), 6);
    dumper->ProcessGenlogRecord(MakeRecord(EType::DSBS, "9"), 9);
    dumper->Finish();

    const auto fileData = TUnbufferedFileInput(dir.Path() / NDumpers::GetShopCategoryIdsFileName()).ReadAll();
    const auto* mappingVec = NMarket::NFlatbufferHelpers::GetTShopCategoryIdsData(fileData)->Offers();
    ASSERT_EQ(10, mappingVec->size());

    using TResult = TVector<uint64_t>;
    auto check = [&](size_t seqNum, const TResult& expected) {
        const auto* offsets = mappingVec->Get(seqNum);
        const auto* cats = offsets->Categories();
        ASSERT_EQ(expected, TResult(cats->begin(), cats->end()));
    };
    check(0, {});
    check(1, {1});
    check(2, {});
    check(3, {});
    check(4, {});
    check(5, {5});
    check(6, {6});
    check(7, {});
    check(8, {});
    check(9, {9});
}
