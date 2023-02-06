#pragma once

#include <market/report/library/request_base/request_base.h>

#include <market/library/text_extensions/text_extensions.h>

class FakeRequest : public NMarketReport::IRequest {
public:
    //FakeRequest();

    virtual void setOriginal(const TString& original) {
        original_ = original;
    }

    virtual void setNormalizedToLower(const TString& normalized_to_lower) {
        normalized_to_lower_ = normalized_to_lower;
    }

    virtual void setNormalizedToLowerAndSorted(const TString& normalized_to_lower_and_sorted) {
        normalized_to_lower_and_sorted_ = normalized_to_lower_and_sorted;
    }

    virtual void setNormalizedByDnorm(const TString& normalized_by_dnorm) {
        normalized_by_dnorm_ = normalized_by_dnorm;
    }

    virtual void setNormalizedBySynnorm(const TString& normalized_by_synnorm) {
        normalized_by_synnorm_ = normalized_by_synnorm;
    }

    virtual void setCanonical(const TString& canonical) {
        canonical_ = canonical;
    }

    virtual void setNoStopWords(const TString& no_stop_words) {
        no_stop_words_ = no_stop_words;
    }

    virtual unsigned getWordCountNoStopWords() const {
        Y_FAIL("not implemented");
    }

    virtual bool isTooShort() const {
        Y_FAIL("not implemented");
    }

    virtual bool isTooLong() const {
        Y_FAIL("not implemented");
    }

    virtual bool isParallelStopOffers() const {
        Y_FAIL("not implemented");
    }

    virtual bool isParallelBlockQuery() const {
        Y_FAIL("not implemented");
    }

    virtual bool isMainStop() const {
        Y_FAIL("not implemented");
    }
    virtual bool isParallelStop() const {
        Y_FAIL("not implemented");
    }
    virtual bool isFamilyStop() const {
        Y_FAIL("not implemented");
    }
    virtual bool matchesParallelBlockWord() const {
        Y_FAIL("not implemented");
    }
    virtual bool matchesRedirectBlackWord() const {
        Y_FAIL("not implemented");
    }

    virtual bool containsParallelFinalStopWordsOnly() const {
        Y_FAIL("not implemented");
    }

    virtual bool empty() const {
        Y_FAIL("not implemented");
    }

    virtual bool hasLongWord() const {
        Y_FAIL("not implemented");
    }

    virtual const TString& getOriginal() const {
        return original_;
    }

    virtual const NMarketReport::TRequestCategoriesClassificationFactors& getRequestCategoriesClassificationFactors() const {
        Y_FAIL("not implemented");
    }

    virtual const TString& getNormalizedToLower() const {
        return normalized_to_lower_;
    }

    virtual const TString& getNormalizedToLowerAndSorted() const {
        return normalized_to_lower_and_sorted_;
    }

    virtual const TString& getNormalizedByDnorm() const {
        return normalized_by_dnorm_;
    }

    virtual const TString& getNormalizedBySynnorm() const {
        return normalized_by_synnorm_;
    }

    virtual const TString& getCanonical() const {
        return canonical_;
    }

    virtual const TString& getNoStopWords() const {
        return no_stop_words_;
    }

    virtual const TString& getNoBuyStopWords() const {
        Y_FAIL("not implemented");
    }

    virtual const TString& getNoRegionWords() const {
        Y_FAIL("not implemented");
    }

    virtual bool hasRegionWords() const {
        Y_FAIL("not implemented");
    }

    virtual const TString& getOrigHiddenStopWords() const {
        Y_FAIL("not implemented");
    }

    virtual const TString& getSearchText() const {
        Y_FAIL("not implemented");
    }

    virtual TRichTreePtr getSearchTree() const {
        Y_FAIL("not implemented");
    }

    virtual const NMarketReport::TReqWizardAnswer* getReqWizardAnswer() const {
        Y_FAIL("not implemented");
    }

    virtual const NMarketReport::NFactors::IWaresFactorSource* getWaresFactorSource() const {
        Y_FAIL("not implemented");
    }

    virtual const TString& getRelevFactors() const {
        Y_FAIL("not implemented");
    }

    virtual const TString& getCommonQtree() const {
        Y_FAIL("not implemented");
    }

    virtual TString getQueryMarkup() const {
        Y_FAIL("not implemented");
    }

    virtual TString getWizardRules() const {
        Y_FAIL("not implemented");
    }

    /// Returns regions specified in request
    /// @note it's possible to return multiple regions having only one in request due to region resolving ambiguity
    virtual Market::RegionSet getRegions() const {
        Y_FAIL("not implemented");
    }

    virtual TRegionSet getRelevantRegions() const {
        Y_FAIL("not implemented");
    }

    virtual void getTiresKeywordPositions(NMarketReport::TTiresKeywordPositions&) const {
        Y_FAIL("not implemented");
    }

    virtual void getTiresMarkPositions(NMarketReport::TTiresMarkPositions&) const {
        Y_FAIL("not implemented");
    }

    virtual void getTiresModelPositions(NMarketReport::TTiresModelPositions&) const {
        Y_FAIL("not implemented");
    }

    virtual void getShopPositions(NMarketReport::TShopPositions&) const {
        Y_FAIL("not implemented");
    }

    virtual void getMainCategories(TVector<size_t>&) const {
        Y_FAIL("not implemented");
    }

    virtual void getMainCategoriesPositions(NMarketReport::TCategoriesPositions&) const {
        Y_FAIL("not implemented");
    }

    virtual void getExtraCategories(TVector<size_t>&) const {
        Y_FAIL("not implemented");
    }

    virtual void getExtraCategoriesPositions(NMarketReport::TCategoriesPositions&) const {
        Y_FAIL("not implemented");
    }

    virtual bool getParametricQueryInfo(TString&, TExtensions&) const {
        Y_FAIL("not implemented");
    }

    virtual TVector<NMarketReport::TShopId> getShops() const {
        Y_FAIL("not implemented");
    }

    TString getUrlHash() const override final {
        Y_FAIL("not implemented");
    }

    virtual const NMarketReport::TEmbeddings& getEmbeddings() const override final {
        Y_FAIL("not implemented");
    }

    virtual const NMarketReport::TRequestDssmFactors& getRequestDssmFactors() const override final {
        Y_FAIL("not implemented");
    }

    virtual TString getExpansions() const override final {
        Y_FAIL("not implemented");
    }

private:
    TString original_;
    TString normalized_to_lower_;
    TString normalized_to_lower_and_sorted_;
    TString normalized_by_dnorm_;
    TString normalized_by_synnorm_;
    TString canonical_;
    TString no_stop_words_;
};
