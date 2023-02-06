#pragma once

#include <market/report/src/place/recom/details/fixed_models.h>
#include <market/report/src/place/recom/details/test_place.h>
#include <market/report/src/output/info/category.h>
#include <market/report/src/place/recom/details/generic.h>


namespace NMarketReport {

class TReportCgiParams;
class TReportConfig;

namespace NRecom {

namespace NDetails {

enum class ETestPlaceCase {
    NONE,
    SMALL_RESPONSE,
    BIG_RESPONSE,
    FIXED_PMODELS
};

ETestPlaceCase DetermineTestCase(const TReportCgiParams& cgi);
TMaybe<TVector<TModelId>> GetFixedModelIds(const TReportCgiParams& cgi);

bool IsTestPlaceRequested(const TReportCgiParams& cgi);

// test place type resolving templates

template<typename TOutputData>
struct TTestPlaceSelector {
    using TType = TTestPlace;
};

template<>
struct TTestPlaceSelector<NOutput::TCategory::TListType> {
    using TType = TTestCategoryPlace;
};

template<typename TPlace>
THolder<IPlace> CreateTestPlace(const TSearchPlace::TPlaceInitParams& placeContext) {
    using TOutputData = typename TPlace::TOutputData;
    using TTestPlaceType = typename TTestPlaceSelector<TOutputData>::TType;
    const TString bigTestNumDoc = "6";
    const TString smallTestNumDoc = "1";
    const auto testCase = DetermineTestCase(placeContext.Cgi);
    if (testCase == ETestPlaceCase::FIXED_PMODELS) {
        return THolder(new TFixedModels(placeContext,
                                *GetFixedModelIds(placeContext.Cgi)));
    }
    const TString numdoc = (testCase == ETestPlaceCase::BIG_RESPONSE)
                               ? bigTestNumDoc
                               : smallTestNumDoc;
    return WithPatchedCgi<TTestPlaceType>(placeContext, THashMap<TString, TString>({{"page", "1"}, {"numdoc", numdoc}}));
}

} // namespace NDetails

} // namespace NRecom

} // namespace NMarketReport
