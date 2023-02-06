#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/stream/file.h>

#include <market/report/library/blender/bundles_config/bundles_config.h>

using namespace NMarketReport::NMarketBlender;
using namespace NMarketReport::NCgi;

TEST(BundlesConfig, ReadBundlesConfigAndBundleFromDir) {
    TBlenderCalculatorsManager calcManager;
    calcManager.RegisterBundlesPath(SRC_("./data"));
    calcManager.Load();

    THashMap<int, TSet<int>> supportedIncuts = {{1, {1, 2, 3, 4}}, {2, {1, 2, 3, 4}}};
    TCalculatorSelectionParams params = {
        .IsTextSearch = true,
        .Client = EClient::Frontend,
        .Platform = EPlatformType::Desktop,
        .SupportedIncuts = supportedIncuts,
    };

    const auto selectCalculator = [&](NMarketReport::EInClid inClid, const TString& bundleName = "") {
        auto calculators = bundleName.empty() ? calcManager.SelectCalculator(inClid, params) :
                                                calcManager.SelectCalculator(inClid, params, {bundleName, bundleName});
        return calculators ? calculators->front() : nullptr;
    };

    auto calculator = selectCalculator(NMarketReport::EInClid::INCLID_ORGANIC);
    EXPECT_EQ(calculator, nullptr);

    params.IsTextSearch = false;
    params.Client = EClient::Frontend;
    params.Platform = EPlatformType::Desktop;
    calculator = selectCalculator(NMarketReport::EInClid::INCLID_MATERIAL_ENTRYPOINTS);
    EXPECT_NE(calculator, nullptr);

    TVector<TIncutFactorsById> fullFactors = {
        {{}, "Gallery_Top_const"},
        {{}, "Gallery_Search"}
    };
    TCalculationDependencies emptyDeps {
        .YandexUid = Nothing(),
        .UniqueUid = Nothing(),
    };
    TVector<TResultIncutScore> fullResult = calculator->Calculate(fullFactors, emptyDeps);
    EXPECT_EQ(fullResult.size(), 2ULL);

    params.IsTextSearch = true;
    params.Client = EClient::Frontend;
    params.Platform = EPlatformType::Desktop;
    calculator = selectCalculator(NMarketReport::EInClid::INCLID_MATERIAL_ENTRYPOINTS);
    EXPECT_NE(calculator, nullptr);

    params.IsTextSearch = true;
    params.Client = EClient::Frontend;
    params.Platform = EPlatformType::Touch;
    calculator = selectCalculator(NMarketReport::EInClid::INCLID_MATERIAL_ENTRYPOINTS);
    EXPECT_EQ(calculator, nullptr);

    // если передать имя конкретного бандла, то он найдется, даже если его не было в конфиге
    params.IsTextSearch = true;
    params.Client = EClient::Frontend;
    params.Platform = EPlatformType::Touch;
    calculator = selectCalculator(NMarketReport::EInClid::INCLID_MATERIAL_ENTRYPOINTS, "const_calculator_bundle1");
    EXPECT_NE(calculator, nullptr);
}
