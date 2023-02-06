#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(SaveActualDataTestSuite) {
        NMiner::TOfferContentConverterConfig config("");
        const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
        NMarket::NCapsLock::TCapsLockFixer capsFixer;

        Y_UNIT_TEST(NotSaveActualDataForOriginalPartWithFullLogic) {
            // проверяем, что поля по которым уже целиком перенесли бизнес-логику будут изменяться майнером
            // original-part
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            const auto &feedparserTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589423910);

            auto original = offer.mutable_content()->mutable_partner()->mutable_original();
            auto actual = offer.mutable_content()->mutable_partner()->mutable_actual();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            original->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            original->mutable_url()->set_value("http://www.test.ru");
            original->mutable_url()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_url()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_age()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_age()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_adult()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_adult()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_manufacturer_warranty()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_manufacturer_warranty()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_weight()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_weight()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_dimensions()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_dimensions()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_expiry()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_expiry()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_type()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_type()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_price_from()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_price_from()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_isbn()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_isbn()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_description()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_description()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            original->mutable_offer_params()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_offer_params()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().original().url().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().barcode().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().age().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().adult().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().adult().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().manufacturer_warranty().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().manufacturer_warranty().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().weight().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().weight().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().dimensions().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().dimensions().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().expiry().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().expiry().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().type().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().type().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().price_from().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().price_from().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().isbn().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().isbn().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().description().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().description().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original().offer_params().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().offer_params().meta().timestamp(), fixedTimestamp);
    }

        Y_UNIT_TEST(NotSaveActualDataForOriginalTermsPartWithFullLogic) {
            // проверяем, что поля по которым уже целиком перенесли бизнес-логику будут изменяться майнером
            // original_terms-part
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            const auto &feedparserTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589423910);

            auto originalTerms = offer.mutable_content()->mutable_partner()->mutable_original_terms();
            auto actual = offer.mutable_content()->mutable_partner()->mutable_actual();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            originalTerms->mutable_seller_warranty()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_seller_warranty()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            originalTerms->mutable_sales_notes()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);
            actual->mutable_sales_notes()->mutable_meta()->mutable_timestamp()->CopyFrom(feedparserTimestamp);

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().original_terms().seller_warranty().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().seller_warranty().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().original_terms().sales_notes().meta().timestamp(), feedparserTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().sales_notes().meta().timestamp(), fixedTimestamp);
    }
}
