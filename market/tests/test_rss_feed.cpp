#include "rss_test_runner.h"

#include <market/idx/feeds/qparser/src/feed_parsers/common/rss/feed_parser.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/stream/file.h>

using namespace NMarket;

namespace {

const auto fname = JoinFsPaths(
    ArcadiaSourceRoot(),
    "market/idx/feeds/qparser/tests/data/rss-google-merchant-feed.xml"
);
auto fStream = TUnbufferedFileInput(fname);
const auto input_xml = fStream.ReadAll();

const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "adult": 0,
        "available": 1,
        "barcode": "12345647890000",
        "condition": 1,
        "currency": "RUR",
        "description": "Super duper product",
        "expiry": {
            "seconds": 1480500000
        },
        "google_merchant": {
            "age_group": "GM_AGE_ADULT",
            "availability": "GM_AVAILABILTY_IN_STOCK",
            "availability_date": {
                "seconds": 1480068000
            },
            "category_path": "Google product category",
            "color": "black",
            "condition": "GM_CONDITION_REFURBISHED",
            "cost_of_goods_sold": {
                "currency": "RUR",
                "price": 1234500000
            },
            "custom_labels": [
                {
                    "key": 1,
                    "value": "custom label 1"
                }
            ],
            "deeplink": "example:products/1",
            "energy_efficiency_class": "GM_ENERGY_CLASS_AP",
            "gender": "GM_GENDER_UNISEX",
            "identifier_exists": true,
            "installment": {
                "amount": {
                    "currency": "RUR",
                    "price": 199900000
                },
                "months": 5
            },
            "is_bundle": false,
            "loyalty_points": {
                "points_value": 50,
                "program": "Super discount program",
                "ratio": 0.1
            },
            "max_energy_efficiency_class": "GM_ENERGY_CLASS_APP",
            "min_energy_efficiency_class": "GM_ENERGY_CLASS_A",
            "mobile_link": "http://m.example.com/products/1",
            "mpn": "BRAND0111",
            "multipack": 1,
            "product_detail": [
                {
                    "attribute_name": "attr 1",
                    "attribute_value": "val 1",
                    "section_name": "section 1"
                },
                {
                    "attribute_name": "attr 2",
                    "attribute_value": "val 2",
                    "section_name": "section 2"
                }
            ],
            "product_dimensions": {
                "height_mkm": 999744,
                "length_mkm": 1000000,
                "width_mkm": 999744
            },
            "product_highlight": [
                "best",
                "product",
                "ever"
            ],
            "product_weight_mg": 997903,
            "sale_price_effective_date": {
                "begin": {
                    "seconds": 1479636000
                },
                "end": {
                    "seconds": 1480500000
                }
            },
            "shipping": [
                {
                    "country": "US",
                    "delivery_zone": {
                        "region": "MA"
                    },
                    "handling_time": {
                        "max": 3,
                        "min": 1
                    },
                    "price": {
                        "currency": "USD",
                        "price": 64900000
                    },
                    "service": "Ground delivery",
                    "transit_time": {
                        "max": 5,
                        "min": 2
                    }
                },
                {
                    "country": "US",
                    "delivery_zone": {
                        "region": "MA"
                    },
                    "handling_time": {
                        "max": 3,
                        "min": 1
                    },
                    "price": {
                        "currency": "USD",
                        "price": 159900000
                    },
                    "service": "Express delivery",
                    "transit_time": {
                        "max": 5,
                        "min": 2
                    }
                }
            ],
            "shipping_detail": {
                "handling_time": {
                    "max": 3,
                    "min": 1
                },
                "shipping_dimensions": {
                    "height_mkm": 254000,
                    "length_mkm": 508000,
                    "width_mkm": 1016000
                },
                "shipping_label": "oversized",
                "shipping_weight_mg": 3000000,
                "ships_from_country": "RU",
                "transit_time_label": "Dog food"
            },
            "size": {
                "size": "XL",
                "size_system": "GM_SIZE_SYSTEM_EU",
                "size_type": "GM_SIZE_TYPE_REGULAR"
            },
            "subscription_cost": {
                "amount": {
                    "currency": "RUR",
                    "price": 99900000
                },
                "period": "GM_PERIOD_MONTH",
                "period_length": 6
            },
            "unit_pricing_base_measure": {
                "amount": 10000,
                "unit": "GM_VOLUME_ML"
            },
            "unit_pricing_measure": {
                "amount": 10,
                "unit": "GM_VOLUME_L"
            }
        },
        "height": 254000,
        "length": 508000,
        "name": "Product 1",
        "offer_id": "111",
        "raw_old_price": 123.45,
        "raw_original_price": "99.95  RUB",
        "raw_price": 99.95,
        "type_prefix": "Product category",
        "url": "http://example.com/products/1",
        "vendor": "Product brand",
        "weight": 3000000,
        "width": 1016000
    }
]
)wrap");

}

TEST(RssFeedParser, RunParser) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        input_xml,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["offer_id"] = item->DataCampOffer.identifiers().offer_id();

            const auto& original = item->DataCampOffer.content().partner().original();
            result["name"] = original.name().value();
            result["description"] = original.description().value();
            result["url"] = original.url().value();
            result["expiry"] = NSc::TValue::From(original.expiry().datetime());

            const auto& delivery = item->DataCampOffer.delivery().partner().original();
            result["available"] = delivery.available().flag();

            result["currency"] = item->Currency;
            result["raw_original_price"] = *item->RawOriginalPrice;
            result["raw_price"] = *item->RawPrice;
            result["raw_old_price"] = *item->RawOldPrice;

            result["type_prefix"] = original.type_prefix().value();
            result["vendor"] = original.vendor().value();
            result["adult"] = original.adult().flag();
            result["condition"] = original.condition().type();
            result["weight"] = original.weight().value_mg();
            result["length"] = original.dimensions().length_mkm();
            result["width"] = original.dimensions().width_mkm();
            result["height"] = original.dimensions().height_mkm();

            const auto& barcodes = original.barcode().value();
            if (!barcodes.empty()) {
                result["barcode"] = JoinSeq(",", barcodes);
            }

            auto merchant_copy = item->DataCampOffer.content().type_specific_content().google_merchant();
            result["google_merchant"] = NSc::TValue::From(merchant_copy);

            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
