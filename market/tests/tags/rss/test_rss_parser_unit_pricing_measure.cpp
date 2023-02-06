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
        <g:unit_pricing_measure></g:unit_pricing_measure>
        <g:unit_pricing_base_measure></g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>2</g:id>
        <g:unit_pricing_measure>unsupported</g:unit_pricing_measure>
        <g:unit_pricing_base_measure>unsupported</g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>3</g:id>
        <g:unit_pricing_measure>1</g:unit_pricing_measure>
        <g:unit_pricing_base_measure>1</g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>4</g:id>
        <g:unit_pricing_measure>-1 l</g:unit_pricing_measure>
        <g:unit_pricing_base_measure>-1 kg</g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>5</g:id>
        <g:unit_pricing_measure>0.1 oz</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 OZ </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>6</g:id>
        <g:unit_pricing_measure>0.1 lb</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 LB </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>7</g:id>
        <g:unit_pricing_measure>0.1 mg</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 MG </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>8</g:id>
        <g:unit_pricing_measure>0.1 g</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 G </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>9</g:id>
        <g:unit_pricing_measure>0.1 kg</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 kG </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>10</g:id>
        <g:unit_pricing_measure>0.1 floz</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 FLOZ </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>11</g:id>
        <g:unit_pricing_measure>0.1 pt</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 PT </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>12</g:id>
        <g:unit_pricing_measure>0.1 qt</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 QT </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>13</g:id>
        <g:unit_pricing_measure>0.1 gal</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 GAL </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>14</g:id>
        <g:unit_pricing_measure>0.1 ml</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 ML </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>15</g:id>
        <g:unit_pricing_measure>0.1 cl</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 CL </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>16</g:id>
        <g:unit_pricing_measure>0.1 l</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 L </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>17</g:id>
        <g:unit_pricing_measure>0.1 cbm</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 CBM </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>18</g:id>
        <g:unit_pricing_measure>0.1 in</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 IN </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>19</g:id>
        <g:unit_pricing_measure>0.1 ft</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 FT </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>20</g:id>
        <g:unit_pricing_measure>0.1 yd</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 YD </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>21</g:id>
        <g:unit_pricing_measure>0.1 cm</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 CM </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>22</g:id>
        <g:unit_pricing_measure>0.1 m</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 M </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>23</g:id>
        <g:unit_pricing_measure>0.1 sqft</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 SQFT </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>24</g:id>
        <g:unit_pricing_measure>0.1 sqm</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 1 SQM </g:unit_pricing_base_measure>
    </item>
    <item>
        <g:id>25</g:id>
        <g:unit_pricing_measure>1 ct</g:unit_pricing_measure>
        <g:unit_pricing_base_measure> 10 CT </g:unit_pricing_base_measure>
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
        "OfferId": "2"
    },
    {
        "OfferId": "3"
    },
    {
        "OfferId": "4"
    },
    {
        "OfferId": "5",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_WEIGHT_OZ"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_WEIGHT_OZ"
        }
    },
    {
        "OfferId": "6",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_WEIGHT_LB"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_WEIGHT_LB"
        }
    },
    {
        "OfferId": "7",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_WEIGHT_MG"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_WEIGHT_MG"
        }
    },
    {
        "OfferId": "8",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_WEIGHT_G"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_WEIGHT_G"
        }
    },
    {
        "OfferId": "9",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_WEIGHT_KG"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_WEIGHT_KG"
        }
    },
    {
        "OfferId": "10",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_VOLUME_FLOZ"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_VOLUME_FLOZ"
        }
    },
    {
        "OfferId": "11",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_VOLUME_PT"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_VOLUME_PT"
        }
    },
    {
        "OfferId": "12",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_VOLUME_QT"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_VOLUME_QT"
        }
    },
    {
        "OfferId": "13",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_VOLUME_GAL"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_VOLUME_GAL"
        }
    },
    {
        "OfferId": "14",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_VOLUME_ML"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_VOLUME_ML"
        }
    },
    {
        "OfferId": "15",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_VOLUME_CL"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_VOLUME_CL"
        }
    },
    {
        "OfferId": "16",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_VOLUME_L"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_VOLUME_L"
        }
    },
    {
        "OfferId": "17",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_VOLUME_CBM"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_VOLUME_CBM"
        }
    },
    {
        "OfferId": "18",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_LENGTH_IN"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_LENGTH_IN"
        }
    },
    {
        "OfferId": "19",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_LENGTH_FT"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_LENGTH_FT"
        }
    },
    {
        "OfferId": "20",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_LENGTH_YD"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_LENGTH_YD"
        }
    },
    {
        "OfferId": "21",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_LENGTH_CM"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_LENGTH_CM"
        }
    },
    {
        "OfferId": "22",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_LENGTH_M"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_LENGTH_M"
        }
    },
    {
        "OfferId": "23",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_SQUARE_FT"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_SQUARE_FT"
        }
    },
    {
        "OfferId": "24",
        "UnitPricingBaseMeasure": {
            "amount": 100,
            "unit": "GM_SQUARE_M"
        },
        "UnitPricingMeasure": {
            "amount": 10,
            "unit": "GM_SQUARE_M"
        }
    },
    {
        "OfferId": "25",
        "UnitPricingBaseMeasure": {
            "amount": 10,
            "unit": "GM_CT"
        },
        "UnitPricingMeasure": {
            "amount": 1,
            "unit": "GM_CT"
        }
    }
]
)wrap");

}

TEST(RssFeedParser, UnitPricingMeasure) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_unit_pricing_measure()) {
                result["UnitPricingMeasure"] = NSc::TValue::From(merchant.unit_pricing_measure());
            }
            if (merchant.has_unit_pricing_base_measure()) {
                result["UnitPricingBaseMeasure"] = NSc::TValue::From(merchant.unit_pricing_base_measure());
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
