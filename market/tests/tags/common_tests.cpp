#include "common_tests.h"

#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>

namespace {
    using ::Market::DataCamp::StringListValue;
}

namespace NMarket {
    namespace {
        const TString YmlStart = R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>)wrap";
        const TString YmlEnd = R"wrap(    </offers>
  </shop>
</yml_catalog>)wrap";

        struct TYmlTag {
            TString Name;
            TString Value;
        };

        struct TBlueYmlOffer {
            TString Id;
            TList<TYmlTag> Tags = {};
        };

        inline IOutputStream& operator<<(IOutputStream& builder, const TYmlTag& t) {
            builder << '<' << t.Name << '>' << t.Value << "</" << t.Name << '>';
            return builder;
        }

        inline IOutputStream& operator<<(IOutputStream& builder, const TBlueYmlOffer& t) {
            builder << "<offer>" << TYmlTag{"shop-sku", t.Id};
            for (const auto& item : t.Tags) {
                builder << item;
            }
            builder << "</offer>" << Endl;
            return builder;
        }

        template <typename T1>
        NSc::TValue JsonOffer(TString id, TMaybe<T1> value = Nothing()) {
            NSc::TValue result;
            result.Add("OfferId", NSc::TValue(id));
            if (value.Defined())
                result.Add("Value", NSc::TValue(*value));
            return result;
        }

        template <typename T1>
        NSc::TValue JsonOfferTS(TString id, TMaybe<T1> value = Nothing()) {
            NSc::TValue result = JsonOffer(id, value);
            result.Add("TimeStamp", 1000);
            return result;
        }
    } // namespace

    void TestBlueSimpleTextCsvTag(const TString& tagName,
                                  std::function<TMaybe<TString>(const NMarket::TOfferCarrier&)> extractValue,
                                  TString value /* = "text-value" */,
                                  TString expectedValue /* = "text-value" */) {
        TStringBuilder inputCsv;
        TString idKey = SubstGlobalCopy(tagName, '_', '-');
        inputCsv << "shop-sku;price;" << tagName << Endl
                 << "blue-csv-with-" << idKey << ";7;" << value << Endl
                 << "blue-csv-without-" << idKey << ";7;";

        auto expected = NSc::TValue().AppendAll(
            {NSc::TValue().SetDict().AddAll({{"OfferId", NSc::TValue("blue-csv-with-" + idKey)}, {"Value", NSc::TValue(expectedValue)}}),
             NSc::TValue().SetDict().AddAll({{"OfferId", NSc::TValue("blue-csv-without-" + idKey)}, {"Value", NSc::TValue("")}})});

        const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
            inputCsv,
            [&extractValue](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                auto maybe = extractValue(*item);
                result["Value"] = maybe.Defined() ? *maybe : "";
                return result;
            },
            GetDefaultBlueFeedInfo(EFeedType::CSV),
            "offers-trace.log");
        ASSERT_EQ(actual, expected);
    }

    void TestBlueSimpleTextCsvTag(const TString& tagName,
                                  std::function<Market::DataCamp::StringValue(const NMarket::TOfferCarrier&)> extractValue) {
        TestBlueSimpleTextCsvTag(tagName, [&extractValue](const NMarket::TOfferCarrier& x) {
            const auto& value = extractValue(x);
            return value.has_value() ? MakeMaybe<TString>(value.value()) : Nothing();
        });
    }

    void TestSimpleListTextCsvTag(const TString& tagName,
                                  std::function<StringListValue(const NMarket::TOfferCarrier&)> extractValue,
                                  TString value,
                                  TString expectedValue) {
        auto extractString = [&extractValue](const NMarket::TOfferCarrier& x) {
            const auto& value = extractValue(x);
            return !value.value().empty() ? MakeMaybe<TString>(ToString(value.value())) : Nothing();
        };

        TestBlueSimpleTextCsvTag(tagName, extractString, value, expectedValue);
    }

    void TestBlueSimpleTextYmlTag(const TString& tagName,
                                  std::function<TMaybe<TString>(const NMarket::TOfferCarrier&)> extractValue) {
        TStringBuilder inputXml;
        inputXml << YmlStart
                 << TBlueYmlOffer{"blue-yml-with-" + tagName, {{"price", "7"}, {tagName, "1 text value"}}}
                 << TBlueYmlOffer{"blue-yml-with-rus-" + tagName, {{"price", "7"}, {tagName, "days 25 дней - days"}}}
                 << TBlueYmlOffer{"blue-yml-without-" + tagName, {{"price", "7"}}}
                 << TBlueYmlOffer{"blue-yml-with-empty-" + tagName, {{"price", "7"}, {tagName, ""}}}
                 << YmlEnd;

        const auto expected = NSc::TValue().AppendAll(
            {JsonOffer<TString>("blue-yml-with-" + tagName, "1 text value"),
             JsonOffer<TString>("blue-yml-with-rus-" + tagName, "days 25 дней - days"),
             JsonOffer<TString>("blue-yml-without-" + tagName, ""),
             JsonOffer<TString>("blue-yml-with-empty-" + tagName, "")});

        const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            inputXml,
            [&extractValue](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                auto maybe = extractValue(*item);
                result["Value"] = maybe.Defined() ? *maybe : "";
                return result;
            });
        ASSERT_EQ(actual, expected);
    }

    void TestBlueSimpleTextYmlTag(const TString& tagName,
                                  std::function<Market::DataCamp::StringValue(const NMarket::TOfferCarrier&)> extractValue) {
        TestBlueSimpleTextYmlTag(tagName, [&extractValue](const NMarket::TOfferCarrier& x) {
            const auto& value = extractValue(x);
            return value.has_value() ? MakeMaybe<TString>(value.value()) : Nothing();
        });
    }

    template <typename IntType>
    NSc::TValue GetExpectedOffersForSimpleIntField(const TString& idKey, const TString& fileType) {
        NSc::TValue result;
        result.AppendAll({
            JsonOffer<IntType>("blue-" + fileType + "-with-" + idKey, 1234),
            JsonOffer<IntType>("blue-" + fileType + "-without-" + idKey),
            JsonOffer<IntType>("blue-" + fileType + "-wrong1-" + idKey)});

        if constexpr (std::is_signed_v<IntType>) {
            result.Push(JsonOffer<IntType>("blue-" + fileType + "-signed-" + idKey, -100500));
        } else {
            result.Push(JsonOffer<IntType>("blue-" + fileType + "-signed-" + idKey));
        }

        result.Push(JsonOffer<IntType>("blue-" + fileType + "-wrong3-" + idKey));
        return result;
    }

    template <typename IntType>
    void TestBlueSimpleIntYmlTag(const TString& tagName,
                                 std::function<TMaybe<IntType>(const NMarket::TOfferCarrier&)> extractValue) {
        TStringBuilder inputXml;
        TString idKey = SubstGlobalCopy(tagName, '_', '-');
        inputXml << YmlStart
                 << TBlueYmlOffer{"blue-yml-with-" + idKey, {{"price", "7"}, {tagName, "1234"}}}
                 << TBlueYmlOffer{"blue-yml-without-" + idKey, {{"price", "7"}}}
                 << TBlueYmlOffer{"blue-yml-wrong1-" + idKey, {{"price", "7"}, {tagName, "dgasdgasdg"}}}
                 << TBlueYmlOffer{"blue-yml-signed-" + idKey, {{"price", "7"}, {tagName, "-100500"}}}
                 << TBlueYmlOffer{"blue-yml-wrong3-" + idKey, {{"price", "7"}, {tagName, "100500100500100500100500100500"}}}
                 << YmlEnd;

        const NSc::TValue expected = GetExpectedOffersForSimpleIntField<IntType>(idKey, "yml");

        const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            inputXml,
            [&extractValue](const TQueueItem& item) {
              NSc::TValue result;
              result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
              auto maybe = extractValue(*item);
              if (maybe.Defined())
                  result["Value"] = *maybe;
              return result;
            });
        ASSERT_EQ(actual, expected);
    }

    template void TestBlueSimpleIntYmlTag<ui32>(const TString&,
                                                std::function<TMaybe<ui32>(const NMarket::TOfferCarrier&)>);
    template void TestBlueSimpleIntYmlTag<i64>(const TString&,
                                               std::function<TMaybe<i64>(const NMarket::TOfferCarrier&)>);

    template <typename IntType>
    void TestBlueSimpleIntCsvTag(const TString& tagName,
                                 std::function<TMaybe<IntType>(const NMarket::TOfferCarrier&)> extractValue) {
        TStringBuilder inputCsv;
        TString idKey = SubstGlobalCopy(tagName, '_', '-');
        inputCsv << "shop-sku;price;" << tagName << Endl
                 << "blue-csv-with-" << idKey << ";7;1234" << Endl
                 << "blue-csv-without-" << idKey << ";7;" << Endl
                 << "blue-csv-wrong1-" << idKey << ";7;olololo" << Endl
                 << "blue-csv-signed-" << idKey << ";7;-100500" << Endl
                 << "blue-csv-wrong3-" << idKey << ";7;100500100500100500100500100500" << Endl;

        const NSc::TValue expected = GetExpectedOffersForSimpleIntField<IntType>(idKey, "csv");

        const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
            inputCsv,
            [&extractValue](const TQueueItem& item) {
              NSc::TValue result;
              result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
              auto maybe = extractValue(*item);
              if (maybe.Defined())
                  result["Value"] = *maybe;
              return result;
            },
            GetDefaultBlueFeedInfo(EFeedType::CSV),
            "offers-trace.log");
        ASSERT_EQ(actual, expected);
    }

    template void TestBlueSimpleIntCsvTag<ui32>(const TString&,
                                                std::function<TMaybe<ui32>(const NMarket::TOfferCarrier&)>);
    template void TestBlueSimpleIntCsvTag<i64>(const TString&,
                                               std::function<TMaybe<i64>(const NMarket::TOfferCarrier&)>);

    void TestBlueFlagYmlTag(TString tagName, std::function<Market::DataCamp::Flag(const NMarket::TOfferCarrier&)> extractValue) {
        TStringBuilder inputXml;
        inputXml << YmlStart
                 << TBlueYmlOffer{"blue-yml-true1-" + tagName, {{"price", "7"}, {tagName, " 1 "}}}
                 << TBlueYmlOffer{"blue-yml-true2-" + tagName, {{"price", "7"}, {tagName, " дА "}}}
                 << TBlueYmlOffer{"blue-yml-false1-" + tagName, {{"price", "7"}, {tagName, " 0 "}}}
                 << TBlueYmlOffer{"blue-yml-false2-" + tagName, {{"price", "7"}, {tagName, " нЕт "}}}
                 << TBlueYmlOffer{"blue-yml-without-" + tagName, {{"price", "7"}}}
                 << TBlueYmlOffer{"blue-yml-wrong1-" + tagName, {{"price", "7"}, {tagName, " нееет "}}}
                 << TBlueYmlOffer{"blue-yml-wrong2-" + tagName, {{"price", "7"}, {tagName, "-1"}}}
                 << YmlEnd;

        const auto expected = NSc::TValue().AppendAll(
            {JsonOfferTS<ui32>("blue-yml-true1-" + tagName, 1),
             JsonOfferTS<ui32>("blue-yml-true2-" + tagName, 1),
             JsonOfferTS<ui32>("blue-yml-false1-" + tagName, 0),
             JsonOfferTS<ui32>("blue-yml-false2-" + tagName, 0),
             JsonOffer<ui32>("blue-yml-without-" + tagName),
             JsonOfferTS<ui32>("blue-yml-wrong1-" + tagName),
             JsonOfferTS<ui32>("blue-yml-wrong2-" + tagName)});

        const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            inputXml,
            [&extractValue](const TQueueItem& item) {
              NSc::TValue result;
              result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
              auto flag = extractValue(*item);
              if (flag.has_flag())
                  result["Value"] = flag.flag();
              if (flag.has_meta() && flag.meta().has_timestamp()) {
                  result["TimeStamp"] = flag.meta().timestamp().seconds();
              }
              return result;
            });
        ASSERT_EQ(actual, expected);
    }

    void TestBlueFlagCsvTag(TString tagName, std::function<Market::DataCamp::Flag(const NMarket::TOfferCarrier&)> extractValue) {
        TStringBuilder inputCsv;
        TString idKey = SubstGlobalCopy(tagName, '_', '-');
        inputCsv << "shop-sku;price;" << tagName << Endl
            << "blue-csv-true1-" << idKey << ";7;1 " << Endl
            << "blue-csv-true2-" << idKey << ";7; дА " << Endl
            << "blue-csv-false1-" << idKey << ";7; 0 " << Endl
            << "blue-csv-false2-" << idKey << ";7; нЕт " << Endl
            << "blue-csv-without-" << idKey << ";7;" << Endl
            << "blue-csv-wrong1-" << idKey << ";7; нееет " << Endl
            << "blue-csv-wrong2-" << idKey << ";7;-1" << Endl;

        const auto expected = NSc::TValue().AppendAll(
            {JsonOfferTS<ui32>("blue-csv-true1-" + tagName, 1),
             JsonOfferTS<ui32>("blue-csv-true2-" + tagName, 1),
             JsonOfferTS<ui32>("blue-csv-false1-" + tagName, 0),
             JsonOfferTS<ui32>("blue-csv-false2-" + tagName, 0),
             JsonOfferTS<ui32>("blue-csv-without-" + tagName),
             JsonOfferTS<ui32>("blue-csv-wrong1-" + tagName),
             JsonOfferTS<ui32>("blue-csv-wrong2-" + tagName)});

        const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
            inputCsv,
            [&extractValue](const TQueueItem& item) {
              NSc::TValue result;
              result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
              auto flag = extractValue(*item);
              if (flag.has_flag())
                  result["Value"] = flag.flag();
              if (flag.has_meta() && flag.meta().has_timestamp()) {
                  result["TimeStamp"] = flag.meta().timestamp().seconds();
              }
              return result;
            },
            GetDefaultBlueFeedInfo(EFeedType::CSV),
            "offers-trace.log");
        ASSERT_EQ(actual, expected);
    }
} // namespace NMarket
