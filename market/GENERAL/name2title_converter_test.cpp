#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <market/proto/common/common.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(Name2TitleConverterTestSuite) {
        void Convert(NMiner::TDatacampOffer& offer, const NMiner::TOfferContentConverterConfig& config, const google::protobuf::Timestamp& fixedTimestamp) {
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMarket::NCapsLock::TCapsLockFixer capsFixer;
            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(offer, processingContext, config, fixedTimestamp);
        }

        NMiner::TOfferContentConverterConfig config("");
        const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
        const auto& oldTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(327605478); // 1980

        Y_UNIT_TEST(TestValidTitleWithActualInTheBegining) {
            // с прошлого майнинга в actual есть тайтл, в новом майнинге в original валидный name
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // smb
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("OLD");
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("NEW");

            Convert(dcOffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), "NEW");
        }

        Y_UNIT_TEST(TestInvalidLongNameWithActualInTheBegining) {
            // с прошлого майнинга в actual есть тайтл, в новом майнинге в original слишком длинный name
            auto longTitleText = "Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text Title_text";
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // smb
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("OLD");
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value(longTitleText);
            // исключительно для проверки, что мета полностью сохранится:
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->mutable_meta()->set_applier(NMarketIndexer::Common::EComponent::STROLLER);

            config.ForgiveMistakes = true;
            Convert(dcOffer, config, fixedTimestamp);

            // ForgiveMistakes=true: тк в original невалидный title, мы сохраним старый хороший actual title целиком
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), oldTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::STROLLER);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), "OLD");
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, true);

            config.ForgiveMistakes = false;
            Convert(dcOffer, config, fixedTimestamp);
            // ForgiveMistakes=false: обнуляем actual, обновляя мету
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT(!offer.content().partner().actual().title().has_value());
        }

        Y_UNIT_TEST(TestInvalidEmptyNameWithActualInTheBegining) {
            // с прошлого майнинга в actual есть тайтл, в новом майнинге в original пустой name
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // smb
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("OLD");
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::SIMPLE);
            // исключительно для проверки, что мета полностью сохранится:
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->mutable_meta()->set_applier(NMarketIndexer::Common::EComponent::STROLLER);

            config.ForgiveMistakes = true;
            Convert(dcOffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), oldTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::STROLLER);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), "OLD");
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, true);

            config.ForgiveMistakes = false;
            Convert(dcOffer, config, fixedTimestamp);
            // ForgiveMistakes=false: обнуляем actual, обновляя мету
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT(!offer.content().partner().actual().title().has_value());
        }

        Y_UNIT_TEST(TestInvalidEmptyNameWithoutActualInTheBegining) {
            // с прошлого майнинга в actual нет тайтла, в новом майнинге в original пустой name
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // smb
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_description()->set_value("DESCR"); // нужен хоть какой-то content.partner, иначе конвертер не будет ничего делать

            Convert(dcOffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT(!offer.content().partner().actual().title().has_value());
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, true);
        }

        Y_UNIT_TEST(TestValidNameWithoutActualInTheBegining) {
            // с прошлого майнинга в actual нет тайтла, в новом майнинге в original валидный name
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // smb
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("NEW");
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::SIMPLE);

            Convert(dcOffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), "NEW");
        }

        Y_UNIT_TEST(TestValidNameWithEqualActualTimestampUpdated) {
            // Проверяем, что обновляется timestamp даже если title остаётся прежним
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            dcOffer.mutable_partner_info()->set_direct_product_mapping(true); // smb
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("SomeName");
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("SomeName");
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::SIMPLE);

            Convert(dcOffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), "SomeName");
        }

        Y_UNIT_TEST(TestValidNameBooks) {
            // с прошлого майнинга в actual нет тайтла, в новом майнинге в original валидный name
            NMiner::TDatacampOffer dcoffer = MakeDefault();
            auto& offer = dcoffer.GetBasicByColor();
            dcoffer.mutable_partner_info()->set_direct_product_mapping(true); // smb
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::BOOKS);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("Author A. \"Book name\"");

            Convert(dcoffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), "Author A. \"Book name\"");
        }

        Y_UNIT_TEST(TestValidNameAudiobooks) {
            // с прошлого майнинга в actual нет тайтла, в новом майнинге в original валидный name
            NMiner::TDatacampOffer dcoffer = MakeDefault();
            auto& offer = dcoffer.GetBasicByColor();
            dcoffer.mutable_partner_info()->set_direct_product_mapping(true); // smb
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::AUDIOBOOKS);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("Author A. \"Audiobook name\"");

            Convert(dcoffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), "Author A. \"Audiobook name\"");
        }

        Y_UNIT_TEST(TestTitleNoVendor) {
            NMiner::TDatacampOffer dcOffer0 = MakeDefault();
            auto& offer0 = dcOffer0.GetBasicByColor();
            offer0.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::SIMPLE);
            offer0.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("Apple smartphone");
            offer0.mutable_content()->mutable_partner()->mutable_original()->mutable_vendor()->set_value("Apple");
            offer0.mutable_content()->mutable_partner()->mutable_actual()->mutable_type()->set_value(NMarket::EProductType::SIMPLE);
            offer0.mutable_content()->mutable_partner()->mutable_actual()->mutable_name()->set_value("Apple smartphone");
            offer0.mutable_content()->mutable_partner()->mutable_actual()->mutable_vendor()->set_value("Apple");

            Convert(dcOffer0, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(offer0.content().partner().actual().title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(offer0.content().partner().actual().title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(offer0.content().partner().actual().title().value(), "Apple smartphone");
            UNIT_ASSERT_EQUAL(offer0.content().partner().actual().title_no_vendor().value(), "smartphone");

        }

        Y_UNIT_TEST(TestOriginalNameNoVendor) {
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto* originalSpecification = dcOffer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original();
            auto* actualSpecification = dcOffer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_actual();

            originalSpecification->mutable_type()->set_value(NMarket::EProductType::SIMPLE);
            originalSpecification->mutable_name()->set_value("Smartphone Apple Iphone 12 Pro MAX");
            originalSpecification->mutable_name_no_vendor()->set_value("Smartphone Iphone 12 Pro MAX");

            actualSpecification->mutable_type()->set_value(NMarket::EProductType::SIMPLE);
            actualSpecification->mutable_name()->set_value("Smartphone Apple Iphone 12 Pro MAX");
            actualSpecification->mutable_title_no_vendor()->set_value("Smartphone Iphone 12 Pro MAX");

            Convert(dcOffer, config, fixedTimestamp);

            const auto& actualContent = dcOffer.GetBasicByColor().content().partner().actual();
            UNIT_ASSERT_EQUAL(actualContent.title().value(), "Smartphone Apple Iphone 12 Pro MAX");
            UNIT_ASSERT_EQUAL(actualContent.title_no_vendor().value(), "Smartphone Iphone 12 Pro MAX");
        }

        Y_UNIT_TEST(TestIgnoreVendorModel) {
            // Тип VENDOR_MODEL (лесаги!) для майнера эквивалентен типу SIMPLE
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            auto& originalContent = *offer.mutable_content()->mutable_partner()->mutable_original();
            auto& actualContent = *offer.mutable_content()->mutable_partner()->mutable_actual();
            originalContent.mutable_type()->set_value(NMarket::EProductType::VENDOR_MODEL);
            actualContent.mutable_type()->set_value(NMarket::EProductType::VENDOR_MODEL);

            originalContent.mutable_type_prefix()->set_value("Smartphone");
            originalContent.mutable_vendor()->set_value("Apple");
            originalContent.mutable_model()->set_value("12 pro");
            originalContent.mutable_name()->set_value("Apple original name");
            actualContent.mutable_type_prefix()->set_value("Smartphone");
            actualContent.mutable_vendor()->set_value("Apple");
            actualContent.mutable_model()->set_value("12 pro");
            actualContent.mutable_name()->set_value("Apple original name");

            Convert(dcOffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(actualContent.type().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(actualContent.type().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(actualContent.type().value(), NMarket::EProductType::VENDOR_MODEL);
            UNIT_ASSERT_EQUAL(originalContent.type().value(), NMarket::EProductType::VENDOR_MODEL);

            UNIT_ASSERT_EQUAL(actualContent.title().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(actualContent.title().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(actualContent.title().value(), "Apple original name");

            UNIT_ASSERT_EQUAL(actualContent.title_no_vendor().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(actualContent.title_no_vendor().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
            UNIT_ASSERT_EQUAL(actualContent.title_no_vendor().value(), "original name");
        }

        Y_UNIT_TEST(TestCapsLockTitle) {
            /* Правила:
            - смотрим все слова КАПСОМ (4 и более символов), по списку тегов, обозначенных выше
            - не исправляем слова, написанные латиницей. правим только кириллицу.
            - база вендоров МБО - источник знаний о том, что слово - это название Вендора.
            - если вендор+какое-то слово - то вот эту добавку мы исправляем. (в этом кейсе надо учесть, что это слово может быть написано через .  -  ?  &  )
             */
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByUnitedCatalogStatus();
            auto& actualContent = *offer.mutable_content()->mutable_partner()->mutable_actual();
            auto& originalContent = *offer.mutable_content()->mutable_partner()->mutable_original();

            originalContent.mutable_name()->set_value("СУМКА КРОСС-БОДИ МЕТРО 26х18х45 САФЬЯНО");

            Convert(dcOffer, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(actualContent.title().value(), "Сумка кросс-боди метро 26х18х45 сафьяно");
        }

        Y_UNIT_TEST(TestUrlInTitle) {
            config.ShouldCheckUrlInTitle = true;
            NMiner::TDatacampOffer dcoffer = MakeDefault();
            auto& offer = dcoffer.GetBasicByColor();
            for (TString name: {"Нормальный Title", "Тоже.Пойдёт"}) {
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value(name);
                Convert(dcoffer, config, fixedTimestamp);
                ASSERT_EQ(offer.status().disabled().size(), 0);
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), name);
            }
            for (TString name: {"плохое название www.name.com, ай-яй-яй", "Another BadTitle.ru", "https://datacamp.com/url"}) {
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value(name);
                Convert(dcoffer, config, fixedTimestamp);
                ASSERT_EQ(offer.status().disabled().size(), 1);
                ASSERT_TRUE(offer.status().disabled()[0].flag());
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().title().value(), "");
            }
        }
}
