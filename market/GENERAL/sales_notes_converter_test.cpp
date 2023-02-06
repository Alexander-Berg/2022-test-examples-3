#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(SalesNotesConverterTestSuite) {
        NMiner::TOfferContentConverterConfig config("");
        const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
        NMarket::NCapsLock::TCapsLockFixer capsFixer;

        Y_UNIT_TEST(SalesNotesAreGoodSize) {
            // проверяем, что хорошее sales_notes копируется как есть, скрытий нету
            const TString goodSalesNotes = "ЭтоХорошееОписание";
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original_terms()->mutable_sales_notes()->set_value(goodSalesNotes);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().sales_notes().value(), goodSalesNotes);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().sales_notes().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        Y_UNIT_TEST(SalesNotesAreWrongSize) {
            // проверяем, что очень длинное sales_notes обрезается, скрытий нету
            const TString longSalesNotes = "ЭтоОченьДлинноеОписание!)ЭтоОченьДлинноеОписание!)ЭтоОченьДлинноеОписание!)";
            const TString expectedSalesNotes = "ЭтоОченьДлинноеОписание!)ЭтоОченьДлинноеОписание!)";
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original_terms()->mutable_sales_notes()->set_value(longSalesNotes);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().sales_notes().value(), expectedSalesNotes);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().sales_notes().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        Y_UNIT_TEST(SalesNotesEmpty) {
            // проверяем, что для пустых sales_notes ничего не создается
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(!offer.content().partner().actual().has_sales_notes());
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        NMiner::TDatacampOffer CreateOffer(Market::DataCamp::Offer& basic,
                                           Market::DataCamp::Offer& service,
                                           Market::DataCamp::MarketColor color,
                                           const TString& salesNotes) {
            service.mutable_meta()->set_rgb(color);
            (*service.mutable_meta()->mutable_platforms())[color] = true;
            NMiner::TDatacampOffer dcOffer(&basic, &service);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            dcOffer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            dcOffer.GetService().mutable_content()->mutable_partner()->mutable_original_terms()->mutable_sales_notes()->set_value(salesNotes);
            return dcOffer;
        }

        Y_UNIT_TEST(CheckServiceFieldWithFallback) {
            const TString salesNotes = "Описание";
            auto converter = NWhite::MakeContentConverter(capsFixer);
            NMarket::TDatacampOfferBatchProcessingContext processingContext;

            for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, salesNotes);
                converter->Process(dcOffer, processingContext, config, fixedTimestamp);

                UNIT_ASSERT_EQUAL(dcOffer.GetService().content().partner().actual().sales_notes().value(), salesNotes);
                if (color == Market::DataCamp::MarketColor::WHITE) {
                    UNIT_ASSERT(!dcOffer.GetBasic().content().partner().actual().has_sales_notes());
                } else {
                    // для синих конентная базовая часть не заполняетя вообще
                    UNIT_ASSERT(!dcOffer.GetBasic().has_content());
                }
            }
        }

}
