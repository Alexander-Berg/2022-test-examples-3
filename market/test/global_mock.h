#pragma once

#include <market/library/price_regional_stats/price_regional_stats.h>
#include <market/library/trace_log/trace_log.h>
#include <market/report/library/external_requester/external_requester.h>
#include <market/report/library/external_requester/external_service_enum.h>
#include <market/report/library/global/contex/contex.h>
#include <market/report/src/Global.h>

#include <util/system/yassert.h>

namespace NMarketReport {

    namespace NPrivate {
        class TMockExternalRequester : public IExternalRequester {
        public:
            TMockExternalRequester() = default;

            TString Request(EExternalService, const TCgiParameters &, const TMaybe<long>&, const TVector<TString> &, long *, TTraceLogRecord&) const override {
                return {};
            }

            TString RequestCustomHost(EExternalService, const TString&, const TString&, const TCgiParameters&, const TMaybe<long>&, const TVector<TString>&, long*) const override {
                return {};
            }

            TString PostRequest(EExternalService, const TCgiParameters&, const TString&, const TMaybe<long>&, const TVector<TString>&, long*, TTraceLogRecord&) const override {
                return {};
            }

            TString PostRequestCustomHost(EExternalService, const TString&, const TString&, const TCgiParameters&, const TString&, const TMaybe<long>&, const TVector<TString>&, long*) const override {
                return {};
            }

            THolder<ILazyExternalResponse> SendGetRequest(EExternalService, const TCgiParameters&, const TMaybe<long>&, const TVector<TString>&) const override {
                return {};
            }

            THolder<ILazyExternalResponse> SendPostRequest(EExternalService, const TCgiParameters&, const TMaybe<long>&, const TString&,
                                                          const TVector<TString>&) const override {
                return {};
            }
        };
    }

    class TGlobalMockBase: public IGlobal {
    protected:
        TGlobalMockBase()
            : ExternalRequester(new NPrivate::TMockExternalRequester())
        {
            global.Reset(this);
            NMarketReport::NGlobal::LoadContex("", "");
        }

        ~TGlobalMockBase() override {
            Y_UNUSED(global.Release());
        }

    public:
        virtual TString applyStopWordRules(const TString&, bool) const {
            Y_FAIL("not implemented");
        }

        void FillStopWordsExts(const TString&, TExtensions&) const override {
            Y_FAIL("not implemented");
        }

        void FillExperimentalStopWordsExts(const TString&, TExtensions&) const override {
            Y_FAIL("not implemented");
        }

        const TCanonizedQuerySet& getMainStopQueries() const override {
            Y_FAIL("not implemented");
        }

        const TCanonizedQuerySet& getParallelOffersStopQueries() const override {
            Y_FAIL("not implemented");
        }

        const TCanonizedQuerySet& getParallelBlockQueries() const override {
            Y_FAIL("not implemented");
        }

        const TGoodWordsSet& getParallelBlockWords() const override {
            Y_FAIL("not implemented");
        }

        const TCanonizedWordsSet& getParallelFinalStopWords() const override {
            Y_FAIL("not implemented");
        }

        const TGoodWordsSet& getRedirectBlackWords() const override {
            Y_FAIL("not implemented");
        }

        const TGoodWordsQuerySet& getAlcoholQueries() const override {
            Y_FAIL("not implemented");
        }

        const TGoodWordsQuerySet& getAdultQueries() const override {
            Y_FAIL("not implemented");
        }

        virtual const Market::TModelCardRegionalStats& getModelsRegionalInfo() const {
            Y_FAIL("not implemented");
        }

        const UIntSet& getEmptyIntSet() const override {
            Y_FAIL("not implemented");
        }

        void InitDynamics() override {
            Y_FAIL("not implemented");
        }

        void UpdateFilters() override {
            Y_FAIL("not implemented");
        }

        void UpdateFulfillmentFilters() override {
            Y_FAIL("not implemented");
        }

        void UpdateLMS() override {
            Y_FAIL("not implemented");
        }

        void UpdateLoyalty() override {
            Y_FAIL("not implemented");
        }

        void UpdateModelBidsCutoff() override {
            Y_FAIL("not implemented");
        }

        void UpdateQPromos() override {
            Y_FAIL("not implemented");
        }

        void RollbackQPromos() override {
            Y_FAIL("not implemented");
        }

        void UpdateQBids() override {
            Y_FAIL("not implemented");
        }

        void UpdateQBidsForCollection(const NMarketReport::TReportConfig::TIndexCollection& /*collection*/, const TMaybe<TString>& = Nothing()) override {
            Y_FAIL("not implemented");
        }

        TMaybe<TString> UpdateQIndex() override final {
            Y_FAIL("not implemented");
        }

        TString getDynamicDataInfo() const override {
            Y_FAIL("not implemented");
        }

        TString getDynamicRollbackInfo() const override {
            Y_FAIL("not implemented");
        }

        const UIntSet& getRedirectStopCategories(const TString&) const override {
            Y_FAIL("not implemented");
        }

        bool isCategoryInRedirectStopList(const TString& canonical_query, THyperCategoryId category) const override {
            const UIntSet& stopCategories = global->getRedirectStopCategories(canonical_query);
            return stopCategories.find(category) != stopCategories.end();
        }

        const UIntSet& getRedirectStopVendors(const TString&) const override {
            Y_FAIL("not implemented");
        }

        TMaybe<TString> GetUrlFromWhiteList(const TString&) const override {
            Y_FAIL("not implemented");
        }

        TMaybe<TString> GetUrlFromWhiteListForApp(const TString&) const override {
            Y_FAIL("not implemented");
        }


        bool isVendorInRedirectStopList(const TString& canonical_query, TVendorId vendor) const override {
            const UIntSet& stopVendors = global->getRedirectStopVendors(canonical_query);
            return stopVendors.find(vendor) != stopVendors.end();
        }

        const RegionSearcher& getRegionSearcher() const override {
            Y_FAIL("not implemented");
        }

        const TString& getLocalSourceList() const override {
            Y_FAIL("not implemented");
        }

        TString getCountryName(Market::TRegionId) const override {
            Y_FAIL("not implemented");
        }

        bool isUnicode() const override {
            Y_FAIL("not implemented");
        }

        bool useReqWizardResponse() const override {
            Y_FAIL("not implemented");
        }

        bool areFormalizerRequestsEnabled() const override {
            Y_FAIL("not implemented");
        }

        const TString& suggestHostAndPort() const override {
            Y_FAIL("not implemented");
        }

        bool areSuggestRequestsEnabled() const override {
            Y_FAIL("not implemented");
        }

        bool areSpellerRequestsEnabled() const override {
            Y_FAIL("not implemented");
        }

        TVector<TString> GetModelsByMark(const TString&) const override {
            Y_FAIL("not implemented");
        }

        const TVendorsInfo& GetVendorsInfo() const override final {
            Y_FAIL("not implemented");
        }

        const THashSet<THyperCategoryId>& GetPreviewCategories() const override {
            Y_FAIL("not implemented");
        }

        const TMaybe<TSet<TVendorId>>& GetPublishedVendors() const override {
            Y_FAIL("not implemented");
        }

        const Market::Pictures::TPictureThumbsConfig& GetThumbsConfig() const override {
            Y_FAIL("not implemented");
        }

        const Market::Pictures::TPictureThumbsConfig& GetTrimmedThumbsConfig() const override {
            Y_FAIL("not implemented");
        }

        const NMarket::NDemandPrediction::IReader& GetDemandPrediction() const override {
            Y_FAIL("not implemented");
        }

        TRtyBackuper* GetRtyBackuper() const override {
            Y_FAIL("not implemented");
        }

        const ITvmWrapper& GetTvmWrapper() const override {
            Y_FAIL("not implemented");
        }

    private:
        std::shared_ptr<IExternalRequester> ExternalRequester;
    };

    class TGlobalTestScope : public TGlobalMockBase {
    public:
        using TBase = TGlobalMockBase;

        TGlobalTestScope() = default;
    };
}
