#pragma once

#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>

#include <market/idx/datacamp/proto/offer/OfferMeta.pb.h>

#include <functional>

namespace NMarket {
    using namespace NMarket;

    void TestBlueSimpleTextCsvTag(const TString& tagName,
                                  std::function<TMaybe<TString>(const NMarket::TOfferCarrier&)> extractValue,
                                  TString value = "text-value",
                                  TString expectedValue = "text-value");
    void TestBlueSimpleTextCsvTag(const TString& tagName,
                                  std::function<Market::DataCamp::StringValue(const NMarket::TOfferCarrier&)> extractValue);
    void TestSimpleListTextCsvTag(const TString& tagName,
                                  std::function<Market::DataCamp::StringListValue(const NMarket::TOfferCarrier&)> extractValue,
                                  TString value,
                                  TString expectedValue);

    template <typename IntType>
    void TestBlueSimpleIntCsvTag(const TString& tagName,
                                 std::function<TMaybe<IntType>(const NMarket::TOfferCarrier&)> extractValue);

    void TestBlueSimpleTextYmlTag(const TString& tagName,
                                  std::function<TMaybe<TString>(const NMarket::TOfferCarrier&)> extractValue);
    void TestBlueSimpleTextYmlTag(const TString& tagName,
                                  std::function<Market::DataCamp::StringValue(const NMarket::TOfferCarrier&)> extractValue);

    template <typename IntType>
    void TestBlueSimpleIntYmlTag(const TString& tagName,
                                 std::function<TMaybe<IntType>(const NMarket::TOfferCarrier&)> extractValue);

    void TestBlueFlagYmlTag(TString tagName, std::function<Market::DataCamp::Flag(const NMarket::TOfferCarrier&)> extractValue);
    void TestBlueFlagCsvTag(TString tagName, std::function<Market::DataCamp::Flag(const NMarket::TOfferCarrier&)> extractValue);
}
