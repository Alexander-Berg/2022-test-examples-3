#include <market/report/src/place/recom/details/test_place.h>

#include <market/report/library/global/category/category_stats.h>
#include <market/report/library/global/model_stats/model_stats.h>
#include <market/report/src/place/category_renderer/category_renderer.h>
#include <market/report/src/place/recom/details/common.h>
#include <market/report/src/place/recom/details/category_model_base_search.h>
#include <market/report/src/place/recom/details/category_output.h>
#include <market/report/src/place/recom/details/default_offers.h>
#include <market/report/src/place/recom/details/model_data_loader.h>
#include <market/report/src/place/utils/ctr.h>
#include <market/report/src/place/utils/model_factory.h>
#include <market/report/src/place/utils/model_output_data.h>
#include <market/report/src/place/utils/model_renderer.h>


#include <market/report/library/category_regional_stats/category_regional_stats.h>


namespace NMarketReport {

namespace NRecom {

TVector<NOutput::TModel> Convert(const TVector<NOutput::TSearchItem>& items);
bool FilterNonProductCategory(const Market::ICategoryNode& category, bool filterNonProductCategory);

namespace NDetails {

namespace {

const size_t NUM_CATEGORIES_TO_REQUEST = 30;
const size_t DEFAULT_NUM_ITEMS_ON_PAGE = 6;

size_t GetNumDoc(const TReportCgiParams& cgi) {
    return cgi.DocumentsOnPageHardLimit().GetValueOr(DEFAULT_NUM_ITEMS_ON_PAGE);
}

bool IsProductCategory(const Market::ICategoryNode& category) {
    return FilterNonProductCategory(category, true);
}

size_t NumOffers(const Market::ICategoryNode& category,
                 const TRegionIdList& regions,
                 const ::Market::CategoriesRegionalStats& regionalStats) {

    const THyperCategoryId hid = category.GetId();
    const auto* regionalItem = regionalStats.getCategoryItem(hid, regions, Market::SHOW_ALL_OFFERS);
    return (regionalItem == nullptr || !IsProductCategory(category))
            ? 0
            : regionalItem->nOffers;
}

const ::Market::CategoriesRegionalStats& GetCategoriesRegionalStats(const TReportCgiParams&) {
    return NGlobal::CategoryRegionalStats(false);
}

TVector<THyperCategoryId> FindCategories(const ::Market::CategoriesRegionalStats& regionalStats,
                                         const TRequestLocale& locale,
                                         size_t n)
{
    const auto& regions = locale.GetRegionPath();
    const auto& tree = NGlobal::CategoryTree();
    using THidSet = TSet<std::pair<size_t, THyperCategoryId>>;
    THidSet sortedHids;
    for (const auto& node : tree.GetRoot().IterateTopDown()) {
        const size_t numOffers = NumOffers(node, regions, regionalStats);
        sortedHids.insert({numOffers, node.GetId()});
    }
    TVector<THyperCategoryId> res;
    for (auto it = sortedHids.rbegin(); it != sortedHids.rend(); ++it) {
        res.push_back(it->second);
        if (res.size() >= n) {
            break;
        }
    }
    return res;
}

} // anonymous namespace

void FillSearchInfo(const TReportCgiParams& cgi, size_t total, NOutput::TSearchInfo& result);

bool TTestPlace::IsSearchNeeded() const {
    return false;
}

void TTestPlace::SelectCollections(TCollectionSelector&) const {
    // trivial
}

TVector<Document> TTestPlace::Explore(const TVector<THyperCategoryId>& hids, Context& ctx) const {
    // find models
    auto exploringCgi = Cgi.MakeModifiedCopy([](UserData& rawData) {
        rawData.setProperty("docs-per-cat", "100");
        rawData.setProperty("prun-count", "100");
    });
    const auto categoryModelsResult = ExtRunPlace<TCategoryModelBaseSearch>(*this, *exploringCgi, ctx, ReqLogCtx, GetHyperlocalContext(), hids, false);
    return categoryModelsResult.Models;
}

THolder<NOutput::IRenderer> TTestPlace::Output(Context& ctx) const {
    auto result = MakeHolder<TModelOutputData>();

    // find categories with many offers
    const auto hids = FindCategories(GetCategoriesRegionalStats(Cgi), *Locale, NUM_CATEGORIES_TO_REQUEST);
    TRACE_ME("Test hids: " << JoinSeq(", ", hids));

    // find models
    const auto allModelDocs = Explore(hids, ctx);
    TVector<TModelId> allModelIds;
    std::transform(allModelDocs.cbegin(), allModelDocs.cend(), std::back_inserter(allModelIds), [] (const auto& doc) {
        return TryModelId(doc).GetOrElse(InvalidId);
    });
    TRACE_ME("Found models: " << JoinSeq(", ", allModelIds));

    const auto mrs = NGlobal::RegionalModels();

    // models with offer first
    TVector<TModelId> modelIds;
    TVector<TModelId> deadModelIds;
    const size_t numDocs = GetNumDoc(Cgi);
    for (const auto modelId : allModelIds) {
        (AnyOffers(mrs, *Locale, modelId) ? modelIds : deadModelIds).push_back(modelId);
        if (modelIds.size() >= numDocs) {
            break;
        }
    }
    if (modelIds.size() < numDocs && !deadModelIds.empty()) {
        const size_t n = Min(deadModelIds.size(), numDocs - modelIds.size());
        std::copy(deadModelIds.cbegin(), deadModelIds.cbegin() + n, std::back_inserter(modelIds));
    }
    if (modelIds.empty()) {
        return MakeHolder<TModelRenderer>(std::move(result), CT_JSON, GetAlsoViewedDefaultOfferFormat(Cgi));
    }
    TRACE_ME("Filtered models: " << JoinSeq(", ", modelIds));

    // find default offers
    const auto doParams = CleanParamsForDefaultOffers(Cgi);
    const auto defaultOffers = GetDefaultOffers(modelIds, *doParams, ReqLogCtx, GetHyperlocalContext(), *ctx.clone(), *Debug.Get(),
                                                GetStatsMutable(), *Obs);

    const auto reasonsToBuy = NRecom::GetReasonsToBuy(modelIds, Cgi, true);

    // attributes
    auto modelDataResult = ExtRunPlace<TModelDataLoader>(*this, Cgi, Ctx, ReqLogCtx, GetHyperlocalContext(), modelIds);

    // ouput model factory
    const THolder<NCtr::IQueryCtrCalculator> ctrCalculator(CreateCtrCalcForNonExistingQuery(Cgi, false));
    const NMoney::TRequestContext requestContext(*LMS, GetDeliveryPriceCalculator(), Loyalty, GetRegionalDeliveryReader(), GetRegionalDeliveryHolder(), NMoney::TRequestContextService(Cgi, *Req, Experiments, *Locale));
    TModelFactory factory(requestContext, Cgi, *Locale, *Req, mrs, *ctrCalculator, Experiments, GetNavigationConverter(), NOutput::EModelOutputMode::Default);
    factory.SetDefaultOffers(&defaultOffers);
    factory.SetReasonsToBuy(&reasonsToBuy);

    // create output data
    size_t position = 0;
    for (const auto modelId : modelIds) {
        const auto it = modelDataResult.find(modelId);
        if (it == modelDataResult.end()) {
            TRACE_ME("Unexpectedly not found data for model: " << modelId);
            continue;
        }
        NBase::TModel baseModel(it->second);
        auto outputModel = factory.CreateOutputModel(baseModel, position++);
        result->Models.emplace_back(std::move(outputModel));
    }
    FillSearchInfo(Cgi, result->Models.size(), result->Info);

    // render output
    return MakeHolder<TModelRenderer>(std::move(result), CT_JSON, GetAlsoViewedDefaultOfferFormat(Cgi));
}

bool TTestCategoryPlace::IsSearchNeeded() const {
    return false;
}

void TTestCategoryPlace::SelectCollections(TCollectionSelector&) const {
    // trivial
}

THolder<NOutput::IRenderer> TTestCategoryPlace::Output(Context&) const {
    // find categories with many offers
    const auto hids = FindCategories(GetCategoriesRegionalStats(Cgi), *Locale, GetNumDoc(Cgi));
    // Render output data
    NOutput::TCategory::TListType result = NDetails::CreateOutputCategories(hids, *Locale, GetNavigationConverter(), Cgi.Experiments().MarketFiltersExp);
    GetStatsMutable().TotalRenderable = result.size();
    return MakeHolder<TCategoryRenderer>(std::move(result), TCategorySearchInfo {
        false,
        0,
        0,
        Nothing(),
        Nothing(),
        Cgi.NewPictureFormat().Get()
    });

}

} // namespace NDetails

} // namespace NRecom

} // namespace NMarketReport
