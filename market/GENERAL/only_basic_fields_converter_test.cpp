#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(OnlyBasicFieldsConverterTestSuite) {
        NMiner::TOfferContentConverterConfig config("");
        const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
        NMarket::NCapsLock::TCapsLockFixer capsFixer;

        NMiner::TDatacampOffer CreateOfferWithOnlyBasicPart(Market::DataCamp::Offer& basic) {
            // Только оффера из каталога под бизнесом могут жить без сервисной части
            basic.mutable_meta()->mutable_business_catalog()->set_flag(true);
            NMiner::TDatacampOffer dcOffer(&basic, nullptr);
            return dcOffer;
        }

        Y_UNIT_TEST(ConvertOfferWithoutServicePart) {
            // Проверяем, что конвертер корректно работает с оффером без сервисной части
            Market::DataCamp::Offer basic;
            NMiner::TDatacampOffer dcOffer = CreateOfferWithOnlyBasicPart(basic);
            dcOffer.GetBasic().mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title");

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp, /*onlyBasicFields*/true);

            UNIT_ASSERT_EQUAL(dcOffer.GetBasic().content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(dcOffer.GetBasic().content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(dcOffer.GetBasic().content().partner().actual().title().value(), "title");
        }

        Y_UNIT_TEST(DoNotModifyServiceFields) {
            // Проверяем, что после конвертации только базового оффера не появляются сервисные поля в базовой части
            auto badTitle = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
            Market::DataCamp::Offer basic;
            NMiner::TDatacampOffer dcOffer = CreateOfferWithOnlyBasicPart(basic);
            dcOffer.GetBasic().mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value(badTitle);
            dcOffer.GetBasic().mutable_content()->mutable_partner()->mutable_original()->mutable_downloadable()->set_flag(true);
            dcOffer.GetBasic().mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value("4690389099335");

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp, /*onlyBasicFields*/true);

            // Баркод на месте
            UNIT_ASSERT_EQUAL(dcOffer.GetBasic().content().partner().actual().barcode().value().size(), 1);
            UNIT_ASSERT_EQUAL(dcOffer.GetBasic().content().partner().actual().barcode().value(0), "4690389099335");
            UNIT_ASSERT_EQUAL(dcOffer.GetBasic().content().partner().actual().barcode().meta().timestamp(), fixedTimestamp);
            // Downloadable на месте
            UNIT_ASSERT_EQUAL(dcOffer.GetBasic().content().partner().actual().downloadable().flag(), true);
            // Плохой тайтл не положили
            UNIT_ASSERT(!dcOffer.GetBasic().content().partner().actual().title().has_value());
            // Скрытие из-за плохого тайтла не подсунули
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            // Флаги доставки после заполнения downloadable не появились
            UNIT_ASSERT(!dcOffer.GetBasic().delivery().partner().actual().has_delivery());
        }
}
