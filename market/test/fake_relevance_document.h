#pragma once

#include <market/report/library/relevance/relevance/relevance_document.h>
#include <util/system/yassert.h>
namespace NMarketReport {
    class FakeRelevanceDocument: public IRelevanceDocument {
    public:
         ui32 GetDocId() const override {
             Y_FAIL("not implemented");
         }

         bool IsModel() const override {
             Y_FAIL("not implemented");
         }

         bool IsVCluster() const override {
             Y_FAIL("not implemented");
         }

         bool IsOffer() const override {
             Y_FAIL("not implemented");
         }

         bool GetHasPicture() const override {
             Y_FAIL("not implemented");
         }

         const NewRel::TDeliveryInfo& GetDelivery() const override {
             Y_FAIL("not implemented");
         }

         const NewRel::TDeliveryInfo& GetShownDelivery() const override {
             Y_FAIL("not implemented");
         }

         const TMaybe<NMarketReport::TDeliveryOption>& GetDefaultDeliveryOption() const override {
             Y_FAIL("not implemented");
         }

         ui16 GetBid() const override {
             Y_FAIL("not implemented");
         }

         ui16 GetVBid() const override {
             Y_FAIL("not implemented");
         }

         bool GetIsCutPrice() const override {
             Y_FAIL("not implemented");
         }

         bool GetPreviouslyUsed() const override {
             Y_FAIL("not implemented");
         }

         bool IsAlcohol() const override {
            Y_FAIL("not implemented");
         }

         unsigned GetShopRating() const override {
             Y_FAIL("not implemented");
         }

         unsigned GetShopRatingFiltering() const override {
             Y_FAIL("not implemented");
         }

         NMarketReport::EOfferTypePriority GetOfferTypePriority() const override {
             Y_FAIL("not implemented");
         }

        bool GetHasShopIcon() const override {
            Y_FAIL("not implemented");
        }

        bool GetHasShopLogo() const override {
            Y_FAIL("not implemented");
        }

         const TShopInfo& GetShopInfo() const override {
             Y_FAIL("not implemented");
         }

         const TShopInfo& GetSupplierShopInfo() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<NMarketReport::NLMS::TWarehouseId> GetWarehouseId() const override {
             Y_FAIL("not implemented");
         }

        TMaybe<NMarketReport::NLMS::TWarehouseId> GetFullFilmentWarehouseId() const override {
            Y_FAIL("not implemented");
        }

         const TFixedPointNumber& GetMinPrice() const override {
             Y_FAIL("not implemented");
         }

         const TFixedPointNumber& GetOfferPrice() const override {
            Y_FAIL("not implemented");
         }

        const TFixedPointNumber& GetOfferMerchPrice() const override {
            Y_FAIL("not implemented");
        }

         const TFixedPointNumber& GetOfferOldPrice() const override {
             Y_FAIL("not implemented");
         }

         bool IsOnFastStock() const override {
             Y_FAIL("not implemented");
         }

         int GetCategoryId() const override {
             Y_FAIL("not implemented");
         }

         int GetModelId(int /*defaultValue*/, const bool /*getContexExpModelId*/) const override {
             Y_FAIL("not implemented");
         }

         TMaybe<unsigned> GetVendorId() const override {
             Y_FAIL("not implemented");
         }

        GuruLightSC3::TGuruOfferId GetGuruOfferId() const override {
             Y_FAIL("not implemented");
         }

         GuruLightSC3::TGuruOfferId GetGuruContexOfferId() const override {
             Y_FAIL("not implemented");
         }

         const TModelRelevanceStats& GetModelStats(bool) const override {
             Y_FAIL("not implemented");
         }

         int GetCPA() const override {
             Y_FAIL("not implemented");
         }

         const MarketSearch::TFactors& GetFactorCalculationPrerequisites() const override {
             Y_FAIL("not implemented");
         }

         const MarketSearch::TPersonalCounters& GetPersonalCounters() const {
             Y_FAIL("not implemented");
         }

         ui32 GetIpRegion() const override {
             Y_FAIL("not implemented");
         }

         ui32 GetRegion() const override {
             Y_FAIL("not implemented");
         }

         ui32 GetTovarId() const override {
             Y_FAIL("not implemented");
         }

         bool IsDownloadable() const override {
             Y_FAIL("not implemented");
         }

         int GetAttribute(NMarketRelevance::EGroupId) const override {
             Y_FAIL("not implemented");
         }

         int GetAttribute(NMarketRelevance::EGroupId, int) const override {
             Y_FAIL("not implemented");
         }

         bool IsForceRegion() const override {
             Y_FAIL("not implemented");
         }

         bool IsFromAccessoriesCategory() const override {
             Y_FAIL("not implemented");
         }

         bool IsFromBookCategory() const override {
             Y_FAIL("not implemented");
         }

         NMarketReport::THyperCategoryId GetHid() const override {
             Y_FAIL("not implemented");
         }

         bool IsFromAlcoholCategory() const override {
             Y_FAIL("not implemented");
         }

         bool IsRecommendedByVendor() const override {
             Y_FAIL("not implemented");
         }

         float GetSmoothedDocumentQueryCtr() const override {
             Y_FAIL("not implemented");
         }

         const NCtr::IQueryCtrCalculator& GetCtrCalculator() const override {
             Y_FAIL("not implemented");
         }

         const NCtr::IQueryCtrCalculator::IDocument& GetDocumentCtrData() const override {
             Y_FAIL("not implemented");
         }

         const NCtr::IQueryCtrCalculator* GetCtrCalculatorForMNFactors(const NMarketReport::ECtrFactorsGroup) const override {
             Y_FAIL("not implemented");
         }

         float GetDssmMarketClick() const override {
             Y_FAIL("not implemented");
         }

        float GetHard2Dssm() const override {
            Y_FAIL("not implemented");
        }

        float GetReformulationDssm() const override {
            Y_FAIL("not implemented");
        }

        float GetBertDssm() const override {
            Y_FAIL("not implemented");
        }

        float GetDssmDistilAssessmentBinary() const override {
            Y_FAIL("not implemented");
        }

        float GetSuperEmbedDssm() const override {
            Y_FAIL("not implemented");
        }

        float GetPicturesDssm() const override {
            Y_FAIL("not implemented");
        }

        const NMarketReport::TEmbeddingsVector& GetHard2DssmEmbedding() const override {
            Y_FAIL("not implemented");
        }

        const NMarketReport::TEmbeddingsVector& GetReformulationDssmEmbedding() const override {
            Y_FAIL("not implemented");
        }

        const NMarketReport::TEmbeddingsVector& GetBertDssmEmbedding() const override {
            Y_FAIL("not implemented");
        }

        const NMarketReport::TEmbeddingsVector& GetAssessmentBinaryEmbedding() const override {
            Y_FAIL("not implemented");
        }

        const NMarketReport::TEmbeddingsVector& GetSuperEmbedDssmEmbedding() const override {
            Y_FAIL("not implemented");
        }

         const NMarketReport::TEmbeddingsVector& GetMarketClickDssmEmbedding() const override {
             Y_FAIL("not implemented");
         }

         float GetCategoryUniqnameBlueDssm() const override {
            Y_FAIL("not implemented");
         }

         float GetCategoryUniqnameDssm() const override {
            Y_FAIL("not implemented");
         }

         float GetCategoryQueriesDssm() const override {
             Y_FAIL("not implemented");
         }

         float GetCategoryTitlesDssm() const override {
             Y_FAIL("not implemented");
         }

         float GetCategoryHardDssm() const override {
             Y_FAIL("not implemented");
         }

         const MarketSearch::SortingSpec::TRequestCategoryClassificationFactors* GetCategoryClassificationFactors() const override {
             Y_FAIL("not implemented");
         }

         std::variant<SBlueErfMarket, SErfMarket> GetErf() const override {
             Y_FAIL("not implemented");
         }
         const SSkuErfMarket& GetSkuErf() const override {
             Y_FAIL("not implemented");
         }
         const SHerfMarket& GetHerf() const override {
             Y_FAIL("not implemented");
         }

         bool IsCpcEnabled() const override {
             Y_FAIL("not implemented");
         }

         bool IsPromoCodeEnabled() const override {
             Y_FAIL("not implemented");
         }

         NMarket::NPromo::TPromoType GetPromoType() const override {
             Y_FAIL("not implemented");
         }

         bool IsBlueOffer() const override {
             Y_FAIL("not implemented");
         }

         bool IsWhiteOffer() const override {
             Y_FAIL("not implemented");
         }

         bool IsAvailableOffer() const override {
             Y_FAIL("not implemented");
         }

         const TMaybe<TFixedPointNumber>& GetMinimumPickupPrice() const override {
            Y_FAIL("not implemented");
         }

         const TOutletWithOptionsContainer& GetOutlets() const override {
             Y_FAIL("not implemented");
         }

         bool IsMarketSku() const override {
             Y_FAIL("not implemented");
         }

         TString GetMarketSku() const override {
             Y_FAIL("not implemented");
         }

         TString GetMarketSkuAll() const override {
             Y_FAIL("not implemented");
         }

         TString GetWareMD5() const override {
             Y_FAIL("not implemented");
         }

         MarketRelevance::TColorData GetColorData() const override {
             Y_FAIL("not implemented");
         }

         const TMaybe<NMarketReport::TQuickData>& TryQuickData() const {
             Y_FAIL("not implemented");
         }

         const NMarketReport::IOfferDelivery::TPtr& TryOfferDelivery() const {
             Y_FAIL("not implemented");
         }

         const TPriceParams& GetPriceParams() const {
             Y_FAIL("not implemented");
         }

        virtual const TFixedPointNumber& GetRawOfferPriceInRub() const {
             Y_FAIL("not implemented");
         }

         const TGroupAttrs& GetAttributes() const {
             Y_FAIL("not implemented");
         }

         NQBid::TFee GetFee() const {
             Y_FAIL("not implemented");
         }

         int GetShopId() const {
             Y_FAIL("not implemented");
         }

         int GetFeedId() const {
             Y_FAIL("not implemented");
         }

         float GetModelConversion(bool) const {
             Y_FAIL("not implemented");
         }

         float GetCategoryConversion(bool) const {
             Y_FAIL("not implemented");
         }

         const NMarketReport::TModelOpinionData* GetModelOpinions() const override {
             Y_FAIL("not implemented");
         }

         ui32 GetModelLifetime() const {
             Y_FAIL("not implemented");
         }

         bool IsDisabled() const {
             Y_FAIL("not implemented");
         }

         NMarketReport::IOfferFilter::TFilterSpec GetOfferSpec() const {
             Y_FAIL("not implemented");
         }

         TMaybe<unsigned> GetDiscountPercent() const override {
             Y_FAIL("not implemented");
         }


         TMaybe<unsigned> GetDiscount() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<TFixedPointNumber> GetHistoryPrice() const override {
             Y_FAIL("not implemented");
         }

         const NewRel::TAcceptedDoc& GetAcceptedDoc() const override {
             Y_FAIL("not implemented");
         }

         bool ShouldDisableRelevanceThreshold(const NewRel::TCheatedDoc&) const override {
             Y_FAIL("not implemented");
         }

         TMaybe<TOfferPromoInfo> GetActivePromo(const EPromoType promoType) const override {
             Y_UNUSED(promoType);
             Y_FAIL("not_implemented");
         }

         const TOfferPromos& GetPromo() const override {
             Y_FAIL("not_implemented");
         }

         NMarketRelevance::TPromoQualityRank GetPromoQualityRank() const override {
             Y_FAIL("not implemented");
         }

         bool GetFromWebmaster() const override {
             Y_FAIL("not implemented");
         }

         bool HasClickNCollect() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<TFixedPointNumber> GetPriceAdjustedByBuyBox() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<TFixedPointNumber> GetPriceBeforeDynamicStrategy() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<TFixedPointNumber> GetPriceAfterDynamicStrategy() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<NMarket::NBlueMarket::TDynamicPriceStrategy> GetDynamicPricingData() const {
             Y_FAIL("not implemented");
         }

         bool HasCredit() const override {
            Y_FAIL("not implemented");
         }

         bool HasBnpl() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<TCreditTemplateId> GetCreditTemplateId() const override {
            Y_FAIL("not implemented");
         }

         double GetOfferConversion() const override {
            Y_FAIL("not implemented");
         }

         TString GetVendorCode() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<NMarketReport::TTimestamp> GetLastTimeViewed() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<TFixedPointNumber> GetRefMinPrice() const override {
            Y_FAIL("not implemented");
         }

         bool CalculateAdult(const bool) const override {
            Y_FAIL("not implemented");
         }

         bool IsGoldenMatrix() const override {
            Y_FAIL("not implemented");
         }

         bool IsHigherThanRefMinPrice() const override {
            Y_FAIL("not implemented");
         }

         bool IsLowerThanRefMinPrice() const override {
            Y_FAIL("not implemented");
         }

         TMaybe<uint32_t> GetPercentFromRefMinPrice() const override {
             Y_FAIL("not implemented");
         }

        bool IsFulfillment() const override {
             Y_FAIL("not implemented");
         }

         OutletTypeStats GetOutletTypeStats() const override {
             Y_FAIL("not implemented");
         }

         bool IsNew() const override {
             Y_FAIL("not implemented");
         }

         TVector<NMarket::NBlueMarket::TElasticity> GetElasticity() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<ui8> GetForbiddenMarketMask() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<NMarketReport::TTimestamp> GetLastTimeOrdered() const override {
             Y_FAIL("not implemented");
         }

         TMaybe<float> GetPredictedElasticity() const override {
             Y_FAIL("not implemented");
         }
    };
}
