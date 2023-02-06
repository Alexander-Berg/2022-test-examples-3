#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(AgeConverterTestSuite) {
        NMiner::TOfferContentConverterConfig config("");
        const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
        const auto& oldTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(327605478); // 1980
        NMarket::NCapsLock::TCapsLockFixer capsFixer;

        Y_UNIT_TEST(AgeGoodMonth) {
            // проверяем, что для правильного значения "месяцев" все появится в actual, скрытий нет
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_unit(Market::DataCamp::Age::MONTH);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_value(3);
            offer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().value(), 3);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().unit(), Market::DataCamp::Age::MONTH);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        Y_UNIT_TEST(AgeInvalidMonth) {
            // проверяем, что для неправильного значения "месяцев" ничего не появится в actual, скрытий нет
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_unit(Market::DataCamp::Age::MONTH);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_value(30);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(!offer.content().partner().actual().age().has_value());
            UNIT_ASSERT(!offer.content().partner().actual().age().has_unit());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        Y_UNIT_TEST(AgeGoodYear) {
            // проверяем, что для правильного значения "лет" все появится в actual, скрытий нет
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_unit(Market::DataCamp::Age::YEAR);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_value(18);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().value(), 18);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().unit(), Market::DataCamp::Age::YEAR);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        Y_UNIT_TEST(AgeInvalidYear) {
            // проверяем, что для неправильного значения "лет" ничего не появится в actual, скрытий нет
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_unit(Market::DataCamp::Age::YEAR);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_value(30);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(!offer.content().partner().actual().age().has_value());
            UNIT_ASSERT(!offer.content().partner().actual().age().has_unit());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        Y_UNIT_TEST(AgeEmptyUnit) {
            // проверяем, что для пустого unit по дефолту проставиться YEAR, скрытий нет
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_value(18);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().value(), 18);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().unit(), Market::DataCamp::Age::YEAR);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        Y_UNIT_TEST(OnlyUnit) {
            // проверяем, что если будет проставлен только unit, то ничего не сломается и в actual данные копировать не будем
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_unit(Market::DataCamp::Age::YEAR);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(!offer.content().partner().actual().age().has_value());
            UNIT_ASSERT(!offer.content().partner().actual().age().has_unit());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }


        Y_UNIT_TEST(EmptyValueEmptyUnit) {
            // проверяем, что если unit и количество не проставлены, то ничего не сломается и в actual данные копировать не будем
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(!offer.content().partner().actual().age().has_value());
            UNIT_ASSERT(!offer.content().partner().actual().age().has_unit());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }

        Y_UNIT_TEST(SavePreviousValidAge) {
            // раньше у оффера был валидный Age в actual, но пришел новый невалидный Age =>
            // - сохраним старый хороший, если включено ForgiveMistakes
            // - очистим старый хороший и обновим timestamp, если не включено
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_age()->set_value(1);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_age()->set_unit(Market::DataCamp::Age::YEAR);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_age()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_value(30);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->set_unit(Market::DataCamp::Age::MONTH);
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);

            config.ForgiveMistakes = true;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().value(), 1);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().unit(), Market::DataCamp::Age::YEAR);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), oldTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);

            config.ForgiveMistakes = false;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            UNIT_ASSERT(!offer.content().partner().actual().age().has_value());
            UNIT_ASSERT(!offer.content().partner().actual().age().has_unit());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
        }

        Y_UNIT_TEST(ClearAgeIfOriginalEmpty) {
            // раньше у оффера был валидный Age в actual, пришел пустой Age => очистим age и при включеном ForgiveMistakes
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            config.ForgiveMistakes = true;
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_age()->set_value(1);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_age()->set_unit(Market::DataCamp::Age::YEAR);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_age()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_age()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);

            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(!offer.content().partner().actual().age().has_value());
            UNIT_ASSERT(!offer.content().partner().actual().age().has_unit());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().age().meta().timestamp(), fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }
}
