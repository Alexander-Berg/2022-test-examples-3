#include <market/idx/feeds/qparser/tests/rss_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/common/rss/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

namespace {

const TString INPUT_XML = TString(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<rss xmlns:g="http://base.google.com/ns/1.0" version="2.0">
<channel>
    <item>
        <g:id>1</g:id>
        <g:energy_efficiency_class></g:energy_efficiency_class>
        <g:min_energy_efficiency_class>1</g:min_energy_efficiency_class>
        <g:max_energy_efficiency_class>unsupported</g:max_energy_efficiency_class>
    </item>
    <item>
        <g:id>2</g:id>
        <g:energy_efficiency_class>A+++</g:energy_efficiency_class>
        <g:min_energy_efficiency_class>A++</g:min_energy_efficiency_class>
        <g:max_energy_efficiency_class>A+</g:max_energy_efficiency_class>
    </item>
    <item>
        <g:id>3</g:id>
        <g:energy_efficiency_class>A</g:energy_efficiency_class>
        <g:min_energy_efficiency_class>B</g:min_energy_efficiency_class>
        <g:max_energy_efficiency_class>C</g:max_energy_efficiency_class>
    </item>
    <item>
        <g:id>4</g:id>
        <g:energy_efficiency_class>D</g:energy_efficiency_class>
        <g:min_energy_efficiency_class>E</g:min_energy_efficiency_class>
        <g:max_energy_efficiency_class>F</g:max_energy_efficiency_class>
    </item>
    <item>
        <g:id>5</g:id>
        <g:energy_efficiency_class>G</g:energy_efficiency_class>
        <g:min_energy_efficiency_class>H</g:min_energy_efficiency_class>
        <g:max_energy_efficiency_class>J</g:max_energy_efficiency_class>
    </item>
</channel>
</rss>
)wrap");

const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "1"
    },
    {
        "EnergyEfficiencyClass": 1,
        "MaxEnergyEfficiencyClass": 3,
        "MinEnergyEfficiencyClass": 2,
        "OfferId": "2"
    },
    {
        "EnergyEfficiencyClass": 4,
        "MaxEnergyEfficiencyClass": 6,
        "MinEnergyEfficiencyClass": 5,
        "OfferId": "3"
    },
    {
        "EnergyEfficiencyClass": 7,
        "MaxEnergyEfficiencyClass": 9,
        "MinEnergyEfficiencyClass": 8,
        "OfferId": "4"
    },
    {
        "EnergyEfficiencyClass": 10,
        "OfferId": "5"
    }
]
)wrap");

}

TEST(RssFeedParser, EnergyClass) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_energy_efficiency_class()) {
                result["EnergyEfficiencyClass"] = merchant.energy_efficiency_class();
            }
            if (merchant.has_min_energy_efficiency_class()) {
                result["MinEnergyEfficiencyClass"] = merchant.min_energy_efficiency_class();
            }
            if (merchant.has_max_energy_efficiency_class()) {
                result["MaxEnergyEfficiencyClass"] = merchant.max_energy_efficiency_class();
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
