#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(DescriptionActualDataTestSuite) {
    NMiner::TOfferContentConverterConfig config("");
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    NMarket::NCapsLock::TCapsLockFixer capsFixer;

    Y_UNIT_TEST(DescriptionChangedIfOriginalPartDeleted) {
        // проверяем, что оригинальное описание, по которому пока не целиком перенесли бизнес-логику, было удалено
        // то майнер затрет и актуальные
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        NMiner::TDatacampOffer dcOffer = MakeDefault();
        auto& offer = dcOffer.GetBasicByColor();
        const auto& timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589423910);

        auto original = offer.mutable_content()->mutable_partner()->mutable_original();
        auto actual = offer.mutable_content()->mutable_partner()->mutable_actual();
        dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
        original->mutable_name()->set_value("title");
        actual->mutable_description()->set_value("description");
        actual->mutable_description()->mutable_meta()->mutable_timestamp()->CopyFrom(timestamp);

        auto converter = NWhite::MakeContentConverter(capsFixer);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp);

        UNIT_ASSERT(!offer.content().partner().actual().description().has_value());
        UNIT_ASSERT_EQUAL(offer.content().partner().actual().description().meta().timestamp(), fixedTimestamp);
    }

    Y_UNIT_TEST(DescriptionChangedIfOriginalPartAdded) {
        // проверяем, что оригинальное описание, по которому пока не целиком перенесли бизнес-логику, было добавлено
        // то майнер обработает их в актуальные (как может)
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        NMiner::TDatacampOffer dcOffer = MakeDefault();
        auto& offer = dcOffer.GetBasicByColor();
        const auto& timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589423910);

        auto original = offer.mutable_content()->mutable_partner()->mutable_original();
        dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
        original->mutable_name()->set_value("title");
        original->mutable_description()->set_value("description");
        original->mutable_description()->mutable_meta()->mutable_timestamp()->CopyFrom(timestamp);

        auto converter = NWhite::MakeContentConverter(capsFixer);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp);

        UNIT_ASSERT(offer.content().partner().actual().description().has_value());
        UNIT_ASSERT_EQUAL(offer.content().partner().actual().description().value(), "description");
        UNIT_ASSERT_EQUAL(offer.content().partner().actual().description().meta().timestamp(), fixedTimestamp);
    }

    Y_UNIT_TEST(DescriptionChangedIfOriginalPartChanged) {
        // проверяем, что оригинальное описание, по которому пока не целиком перенесли бизнес-логику, было изменено отдельно
        // то майнер изменит и актуальные (как может)
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        NMiner::TDatacampOffer dcOffer = MakeDefault();
        auto& offer = dcOffer.GetBasicByColor();
        const auto& timestamp1 = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589423910);
        const auto& timestamp2 = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589420910);

        auto original = offer.mutable_content()->mutable_partner()->mutable_original();
        auto actual = offer.mutable_content()->mutable_partner()->mutable_actual();
        dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
        original->mutable_name()->set_value("title");
        original->mutable_description()->set_value("description1");
        original->mutable_description()->mutable_meta()->mutable_timestamp()->CopyFrom(timestamp1);
        actual->mutable_description()->set_value("description2");
        actual->mutable_description()->mutable_meta()->mutable_timestamp()->CopyFrom(timestamp2);

        auto converter = NWhite::MakeContentConverter(capsFixer);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp);

        UNIT_ASSERT(offer.content().partner().actual().description().has_value());
        UNIT_ASSERT_EQUAL(offer.content().partner().actual().description().value(), "description1");
        UNIT_ASSERT_EQUAL(offer.content().partner().actual().description().meta().timestamp(), fixedTimestamp);
    }

    NMiner::TDatacampOffer CreateOffer(Market::DataCamp::Offer& basic,
                                       Market::DataCamp::Offer& service) {
        service.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
        (*service.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;
        NMiner::TDatacampOffer dcOffer(&basic, &service);
        dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
        auto original = basic.mutable_content()->mutable_partner()->mutable_original();
        original->mutable_name()->set_value("title");
        original->mutable_description()->set_value("description");
        service.mutable_content()->mutable_partner()->mutable_original_terms()->mutable_sales_notes()->set_value("sales notes");
        return dcOffer;
    }
}
