#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <market/proto/common/common.pb.h>

#include <google/protobuf/text_format.h>
#include <google/protobuf/util/time_util.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

const TString DEFAULT_META = R"(
    meta {
      rgb: WHITE
    }
)";

const TString DEFAULT_DELIVERY_PART = R"(
    delivery {
      delivery_info {
        pickup: true
        has_delivery: true
      }
      partner {
        actual {
          pickup {
            meta {
              timestamp {
                seconds: 1589833910
              }
              applier: MINER
            }
            flag: true
          }
          delivery {
            meta {
              timestamp {
                seconds: 1589833910
              }
              applier: MINER
            }
            flag: true
          }
          delivery_options {
            meta {
              timestamp {
                seconds: 1589833910
              }
              applier: MINER
            }
          }
          pickup_options {
            meta {
              timestamp {
                seconds: 1589833910
              }
              applier: MINER
            }
          }
        }
      }
    }
)";

Y_UNIT_TEST_SUITE(ContentConverterTestSuite) {
    NMiner::TOfferContentConverterConfig config("");
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    NMarket::NCapsLock::TCapsLockFixer capsFixer;

    Y_UNIT_TEST(TestBaseCase) {
        const TString originalContentPart = R"(
            original {
              name {
                value: "Title text"
              }
              description {
                value: "Description Text"
              }
              vendor {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                }
                value: "Vendor Name"
              }
              vendor_code {
                value: "Vendor Code"
              }
              barcode {
                value: "BAD-BARCODE"
                value: "55123457"
              }
              offer_params {
                param {
                  name: "Param1 name"
                  value: "Param1 value"
                }
                param {
                  name: "Param2 name"
                  unit: "Param2 unit"
                  value: "Param2 value"
                }
              }
              adult {
                meta {
                  source: PUSH_PARTNER_OFFICE
                }
                flag: true
              }
              age {
                unit: YEAR
                value: 18
              }
              url {
                value: "https://datacamp.com/url"
              }
              manufacturer_warranty {
                flag: false
              }
              expiry {
                validity_period {
                  months: 1
                }
                datetime {
                  seconds: 1589834263
                }
              }
              weight {
                grams: 1000
              }
              dimensions {
                length_mkm: 10000
                width_mkm: 10000
                height_mkm: 10000
              }
              downloadable {
                flag: false
              }
              type_prefix {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                }
                value: "Type Prefix"
              }
              type {
                value: 1
              }
              isbn {
                value: "970-5-699-23647-3"
               }
              category {
                id: 100500
                parent_id: 90401
                name: "category 100500"
              }
              cargo_types {
                value: 38
              }
            }
        )";

        const TString originalTermsContentPart = R"(
            original_terms {
                sales_notes {
                    value: "sales notes"
                }
                seller_warranty {
                    has_warranty: true
                }
                quantity {
                    min: 10
                    step: 20
                }
            }
        )";

        const TString actualContentPart = R"(
            actual {
              title {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                value: "Title text"
              }
              description {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                value: "Description Text"
              }
              offer_params {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                param {
                  name: "Param1 name"
                  value: "Param1 value"
                }
                param {
                  name: "Param2 name"
                  unit: "Param2 unit"
                  value: "Param2 value"
                }
                param {
                  name: "vendor"
                  value: "Vendor Name"
                }
                param {
                  name: "delivery_weight"
                  unit: "кг"
                  value: "1"
                }
                param {
                  name: "delivery_length"
                  unit: "см"
                  value: "1"
                }
                param {
                  name: "delivery_width"
                  unit: "см"
                  value: "1"
                }
                param {
                  name: "delivery_height"
                  unit: "см"
                  value: "1"
                }
              }
              adult {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                flag: true
              }
              age {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                unit: YEAR
                value: 18
              }
              barcode {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                value: "55123457"
              }
              expiry {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                validity_period {
                  months: 1
                }
                datetime {
                  seconds: 1589834263
                }
              }
              manufacturer_warranty {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                flag: false
              }
              url {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                value: "https://datacamp.com/url"
              }
              weight {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                grams: 1000
              }
              dimensions {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                length_mkm: 10000
                width_mkm: 10000
                height_mkm: 10000
              }
              downloadable {
                  meta {
                      timestamp {
                          seconds: 1589833910
                      }
                      applier: MINER
                  }
                  flag: false
              }
              type_prefix {
                  meta {
                      timestamp {
                          seconds: 1589833910
                      }
                      applier: MINER
                  }
                  value: "Type Prefix"
              }
              type {
                  meta {
                      timestamp {
                          seconds: 1589833910
                      }
                      applier: MINER
                  }
                  value: 1
              }
              sales_notes {
                  meta {
                      timestamp {
                          seconds: 1589833910
                      }
                      applier: MINER
                  }
                  value: "sales notes"
              }
              seller_warranty {
                  meta {
                      timestamp {
                          seconds: 1589833910
                      }
                      applier: MINER
                  }
                  has_warranty: true
              }
              quantity {
                  meta {
                      timestamp {
                          seconds: 1589833910
                      }
                      applier: MINER
                  }
                  min: 10
                  step: 20
              }
              vendor {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                value: "Vendor Name"
              }
              vendor_code {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                value: "Vendor Code"
              }
              isbn {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                value: "970-5-699-23647-3"
              }
              category {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                id: 100500
                parent_id: 90401
                name: "category 100500"
              }
              cargo_types {
                meta {
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                value: 38
              }
            }
        )";

        config.UseNewQuantityValidation = false;

        const TString& offerHead = "content { partner { ";
        const TString& offerTail = "} }";

        NMiner::TOffer baseOfferProto;
        NMiner::TDatacampOffer baseOfferAdapter{&baseOfferProto};
        auto baseOfferString = offerHead + originalContentPart + originalTermsContentPart + offerTail + DEFAULT_META;
        google::protobuf::TextFormat::ParseFromString(baseOfferString, &baseOfferProto);

        NMiner::TOffer expectedOfferProto;
        NMiner::TDatacampOffer expectedOfferAdapter{&expectedOfferProto};
        auto expectedOfferString = offerHead + originalContentPart + originalTermsContentPart
            + actualContentPart + offerTail + DEFAULT_DELIVERY_PART + DEFAULT_META;
        google::protobuf::TextFormat::ParseFromString(expectedOfferString, &expectedOfferProto);

        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        converter->Process(baseOfferAdapter, processingContext, config, fixedTimestamp);

        // Раскоменчивате при падениях теста, поможет сохранить хорошее настроение
        //Cerr << baseOfferAdapter.DebugString() << Endl;
        //Cerr << expectedOfferAdapter.DebugString() << Endl;
        UNIT_ASSERT_STRINGS_EQUAL(baseOfferAdapter.DebugString(), expectedOfferAdapter.DebugString());
    }

    // кусочек, который будет использован в следующих тестах
    const TString actualContent = R"(
        content {
          partner {
            actual {
              title {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                value: "Title text"
              }
              description {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                value: "Description Text"
              }
              offer_params {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                param {
                  name: "Param1 name"
                  value: "Param1 value"
                }
              }
              adult {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                flag: true
              }
              age {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                unit: YEAR
                value: 18
              }
              barcode {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                value: "55123457"
              }
              expiry {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                validity_period {
                  months: 1
                }
                datetime {
                  seconds: 1589834263
                }
              }
              manufacturer_warranty {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                flag: false
              }
              url {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                value: "https://datacamp.com/url"
              }
              weight {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                grams: 1000
              }
              dimensions {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                  applier: MINER
                }
                length_mkm: 10000
                width_mkm: 10000
                height_mkm: 10000
              }
              downloadable {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                }
                flag: false
              }
              type_prefix {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                }
                value: "Type Prefix"
              }
              type {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                }
                value: 1
              }
              sales_notes {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                }
                value: "sales notes"
              }
              seller_warranty {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                }
                has_warranty: true
              }
              quantity {
                meta {
                  timestamp {
                    seconds: 12345
                  }
                }
                min: 10
                step: 20
              }
            }
          }
        }
      )" + DEFAULT_META;

    Y_UNIT_TEST(TestEmptyOriginalOffer) {
        // config.ForgiveMistakes = false
        // В случае, если пришел оффер без content.original все поля в actual зачистятся, их меты обновятся
        const TString& expectedOfferString = R"(
            status {
              disabled {
                meta {
                  source: MARKET_IDX
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                flag: true
              }
            }
            content {
              partner {
                actual {
                  title {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  description {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  offer_params {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  adult {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  age {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  barcode {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  expiry {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  manufacturer_warranty {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  url {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  weight {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  dimensions {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  downloadable {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  type_prefix {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  type {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  sales_notes {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  seller_warranty {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  quantity {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                }
              }
            }
        )" + DEFAULT_DELIVERY_PART + DEFAULT_META;

        NMiner::TOffer baseOfferProto;
        NMiner::TDatacampOffer baseOfferAdapter{&baseOfferProto};
        google::protobuf::TextFormat::ParseFromString(actualContent, &baseOfferProto);

        NMiner::TOffer expectedOfferProto;
        NMiner::TDatacampOffer expectedOfferAdapter{&expectedOfferProto};
        google::protobuf::TextFormat::ParseFromString(expectedOfferString, &expectedOfferProto);

        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);

        config.ForgiveMistakes = false;
        converter->Process(baseOfferAdapter, processingContext, config, fixedTimestamp);

        // Раскоменчивате при падениях теста, поможет сохранить хорошее настроение
        //Cerr << baseOfferProto.DebugString() << Endl;
        //Cerr << expectedOfferProto.DebugString() << Endl;
        UNIT_ASSERT_STRINGS_EQUAL(baseOfferProto.DebugString(), expectedOfferAdapter.DebugString());
    }

    Y_UNIT_TEST(TestEmptyOriginalOfferForgiveMistakes) {
        // config.ForgiveMistakes = true
        // В случае, если пришел оффер без content.original все поля в actual зачистятся, их меты обновятся,
        // Кроме:
        // * actual title: сохраняем предыдущее валидное сообщение, и ставим disabled=true (как раньше)
        const TString& expectedOfferString = R"(
            status {
              disabled {
                meta {
                  source: MARKET_IDX
                  timestamp {
                    seconds: 1589833910
                  }
                  applier: MINER
                }
                flag: true
              }
            }
            content {
              partner {
                actual {
                  title {
                    meta {
                      timestamp {
                        seconds: 12345
                      }
                      applier: MINER
                    }
                    value: "Title text"
                  }
                  description {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  offer_params {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  adult {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  age {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  barcode {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  expiry {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  manufacturer_warranty {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  url {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  weight {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  dimensions {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  downloadable {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  type_prefix {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  type {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  sales_notes {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  seller_warranty {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                  quantity {
                    meta {
                      timestamp {
                        seconds: 1589833910
                      }
                      applier: MINER
                    }
                  }
                }
              }
            }
        )" + DEFAULT_DELIVERY_PART + DEFAULT_META;

        NMiner::TOffer baseOfferProto;
        NMiner::TDatacampOffer baseOfferAdapter{&baseOfferProto};
        google::protobuf::TextFormat::ParseFromString(actualContent, &baseOfferProto);

        NMiner::TOffer expectedOfferProto;
        NMiner::TDatacampOffer expectedOfferAdapter{&expectedOfferProto};
        google::protobuf::TextFormat::ParseFromString(expectedOfferString, &expectedOfferProto);

        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);

        config.ForgiveMistakes = true;
        converter->Process(baseOfferAdapter, processingContext, config, fixedTimestamp);

        // Раскоменчивате при падениях теста, поможет сохранить хорошее настроение
        //Cerr << baseOfferProto.DebugString() << Endl;
        //Cerr << expectedOfferProto.DebugString() << Endl;
        UNIT_ASSERT_STRINGS_EQUAL(baseOfferProto.DebugString(), expectedOfferAdapter.DebugString());
    }
}
