#include <market/report/src/place/recom/details/test_place_factory.h>

#include <market/report/library/cgi/params.h>

namespace NMarketReport {
namespace NRecom {
namespace NDetails {

TMaybe<TVector<TModelId>> GetFixedModelIds(const TReportCgiParams& cgi) {
    static const THashMap<TString, TVector<TModelId>> fixedPmodels = {
            {"907159475",  {168553000,  175941400,  178503626,  180423140, 417037143,  183694968,  169117444}},
            {"4032805824", {1759299345, 1711235661, 1759297656, 10484351,  1725097354, 1759344530, 9281948, 160284}},
            {"993730378",  {1759299345, 1711235661, 1759297656, 10484351,  1725097354, 1759344530, 9281948, 160284}},
            {"1016530154", {1711072058, 12259971, 1712313026, 71296620, 14112311, 1759344092, 1711138469}},
            {"4032806878", {1711072058, 12259971, 1712313026, 71296620, 14112311, 1759344092, 1711138469}}
    };
    const TString puid = cgi.PassportUid().GetValueOr({});
    auto modelsMapping = fixedPmodels.find(puid);
    if (modelsMapping == fixedPmodels.end()) {
        return Nothing();
    }
    return modelsMapping->second;
}

ETestPlaceCase DetermineTestCase(const TReportCgiParams& cgi) {
    static const THashMap<TString, ETestPlaceCase> puidMap = {{"690303013", ETestPlaceCase::SMALL_RESPONSE},
                                                              {"67282295", ETestPlaceCase::BIG_RESPONSE}};
    if (!NCgiExtensions::IsTestingFeaturesEnabled(cgi)) {
        return ETestPlaceCase::NONE;
    }
    const TString puid = cgi.PassportUid().GetValueOr({});
    const auto it = puidMap.find(puid);
    if (it != puidMap.cend()) {
        return it->second;
    }

    return GetFixedModelIds(cgi) ? ETestPlaceCase::FIXED_PMODELS : ETestPlaceCase::NONE;
}

bool IsTestPlaceRequested(const TReportCgiParams& cgi) {
    return DetermineTestCase(cgi) != ETestPlaceCase::NONE;
}


} // namespace NDetails
} // namespace NRecom
} // namespace NMarketReport
