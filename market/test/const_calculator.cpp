#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/stream/file.h>

#include <market/report/library/blender/calculators/const_position.h>

using namespace NMarketReport::NMarketBlender;

TEST(ConstCalculator, LoadBundleFromJson) {
    TFileInput fileBundle(TFsPath(SRC_("./data/blender_bundles/const_calculator_bundle.json")));
    THolder<ICalculatorMaker> calcMaker = LoadCalculatorMaker(fileBundle.ReadAll());
    THashMap<int, TSet<int>> supportedIncuts;
    for (int placeId = 1; placeId <= 2; ++placeId) {
        for (int viewTypeId = 1; viewTypeId <= 5; ++viewTypeId) {
            supportedIncuts[placeId].insert(viewTypeId);
        }
    }
    THolder<IBlenderCalculator> calculator = calcMaker->Create(supportedIncuts);

    TVector<TIncutFactorsById> fullFactors = {
        {{}, "Gallery_Top_const"},
        {{}, "Gallery_Search"}
    };

    TVector<TIncutFactorsById> partFactors = {
        {{}, "Gallery_Top_const"}
    };

    TVector<TIncutFactorsById> emptyFactors = {};

    TCalculationDependencies emptyDeps {
        .YandexUid = Nothing(),
        .UniqueUid = Nothing(),
    };

    TVector<TResultIncutScore> fullResult = calculator->Calculate(fullFactors, emptyDeps);
    TVector<TResultIncutScore> partResult = calculator->Calculate(partFactors, emptyDeps);
    TVector<TResultIncutScore> emptyResult = calculator->Calculate(emptyFactors, emptyDeps);

    EXPECT_EQ(fullResult.size(), 2ULL);
    EXPECT_EQ(partResult.size(), 1ULL);
    EXPECT_EQ(emptyResult.size(), 0ULL);

    EXPECT_EQ(fullResult[0].IncutId, "Gallery_Top_const");
    EXPECT_EQ(fullResult[0].IncutPlace, EIncutPlaces::Top);
    EXPECT_EQ(fullResult[0].IncutViewType, EIncutViewTypes::Gallery);
    EXPECT_EQ(fullResult[0].RowPosition, 1);
    EXPECT_DOUBLE_EQ(fullResult[0].Score, 1.0);

    EXPECT_EQ(fullResult[1].IncutId, "Gallery_Search");
    EXPECT_EQ(fullResult[1].IncutPlace, EIncutPlaces::Search);
    EXPECT_EQ(fullResult[1].IncutViewType, EIncutViewTypes::Gallery);
    EXPECT_EQ(fullResult[1].RowPosition, 5);
    EXPECT_DOUBLE_EQ(fullResult[1].Score, 0.75);

    EXPECT_EQ(partResult[0].IncutId, "Gallery_Top_const");
    EXPECT_EQ(partResult[0].IncutPlace, EIncutPlaces::Top);
    EXPECT_EQ(partResult[0].IncutViewType, EIncutViewTypes::Gallery);
    EXPECT_EQ(partResult[0].RowPosition, 1);
    EXPECT_DOUBLE_EQ(partResult[0].Score, 1.0);
}
