#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/lib/log/logger.h>
#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(BarcodeConverterTestSuite) {
        NMiner::TOfferContentConverterConfig config("");

        const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
        const auto oldTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(327605478); // 1980
        const auto badBarcode = "Not-a-barcode";
        const auto validBarcode1 = "4690389099335";
        const auto validBarcode2 = "8056539880073";

        NMarket::NCapsLock::TCapsLockFixer capsFixer;

        struct TEnableVerdictsFixture : public NUnitTest::TBaseFixture {
            void SetUp(NUnitTest::TTestContext&) override { DATACAMP_LOG.Options.EnableVerdicts_ = true; }
            void TearDown(NUnitTest::TTestContext&) override { DATACAMP_LOG.Options.EnableVerdicts_ = false; }
        };

        bool ResolutionHasCode(const ::Market::DataCamp::Resolution& resolution, const TString& code) {
            for (const auto& s : resolution.by_source()) {
                for (const auto& v : s.verdict()) {
                    for (const auto& r : v.results()) {
                        for (const auto& m : r.messages()) {
                            if (m.code() == code)
                                return true;
                        }
                    }
                }
            }
            return false;
        }

        // Если в actual были хорошие баркоды, а в original - только плохие, сохраняем баркоды из actual
        Y_UNIT_TEST(BadBarcodesInOriginalGoodInActual) {
            config.ForgiveMistakes = true;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasic();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(badBarcode);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_barcode()->add_value(validBarcode1);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value().size(), 1);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value(0), validBarcode1);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().meta().timestamp(), oldTimestamp);
        }

        // Если в original валидный баркод, он как и раньше переезжает в actual
        Y_UNIT_TEST(GoodBarcodesEverywhere) {
            config.ForgiveMistakes = true;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasic();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(validBarcode1);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_barcode()->add_value(validBarcode2);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value().size(), 1);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value(0), validBarcode1);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().meta().timestamp(), fixedTimestamp);
        }

        // Если в original хотя бы 1 валидный баркод, он как и раньше переезжает в actual
        Y_UNIT_TEST(BadAndGoodBarcodeInOriginal) {
            config.ForgiveMistakes = true;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasic();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(badBarcode);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(validBarcode1);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_barcode()->add_value(validBarcode2);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value().size(), 1);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value(0), validBarcode1);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().meta().timestamp(), fixedTimestamp);
        }

        // Если в actual хороший баркод, а в original баркода нет, считаем, что партнер удалил баркод - очищаем actual
        Y_UNIT_TEST(LetPartnerRemoveBarcode) {
            config.ForgiveMistakes = true;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasic();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");

            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_barcode()->add_value(validBarcode2);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value().size(), 0);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().meta().timestamp(), fixedTimestamp);
        }

        Y_UNIT_TEST(GoodOriginalNoActualBarcodeBlue) {
            for (bool unitedCatalogStatus : {false, true}) {
                const auto validBarcode = "4670028540053";
                config.ForgiveMistakes = true;

                NMarket::TDatacampOfferBatchProcessingContext processingContext;
                NMiner::TDatacampOffer dcOffer = MakeDefault();
                dcOffer.mutable_status()->mutable_united_catalog()->set_flag(unitedCatalogStatus);
                auto& offer = unitedCatalogStatus ? dcOffer.GetBasic() : dcOffer.GetService();
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");

                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value("4670028540053");

                auto converter = NBlue::MakeContentConverter(capsFixer);
                converter->Process(dcOffer, processingContext, config, fixedTimestamp);

                CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value().size(), 1);
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value(0), validBarcode);
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().meta().timestamp(), fixedTimestamp);
            }
        }

        //  Для валидных баркодов должны вырезаться дефисы
        Y_UNIT_TEST(ISBNInBarcodeForBooks) {
            config.ForgiveMistakes = true;

            const TString validISBNOriginal = "978-3-16-148410-0";
            const TString invalidISBNOriginal = "978-3-16-148410-1";
            const TString validISBNActual = "9783161484100";
            const TString validBarcode = "4670028540053";
            const TString invalidBarcode = "Not-a-barcode";

            TMap<TString, THolder<NMiner::IOfferContentConverter>> converters;
            converters["whiteConverter"] = NWhite::MakeContentConverter(capsFixer);
            converters["blueConverter"] = NBlue::MakeContentConverter(capsFixer);

            for (const auto& [_, converter] : converters) {
                NMarket::TDatacampOfferBatchProcessingContext processingContext;
                NMiner::TDatacampOffer dcOffer = MakeDefault();
                auto& offer = dcOffer.GetBasic();
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::BOOKS);

                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(validISBNOriginal);
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(invalidISBNOriginal);
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(validBarcode);
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(invalidBarcode);
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

                (converter)->Process(dcOffer, processingContext, config, fixedTimestamp);

                CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value().size(), 2);
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value(0), validISBNActual);
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value(1), validBarcode);
                UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().meta().timestamp(), fixedTimestamp);
            }
        }

        // В original есть разные баркоды: и хорошие, и плохие, но включена настройка "не валидировать баркоды"
        // В actual прорастут все баркоды
        Y_UNIT_TEST(DisableBarcodesValidation) {
            config.ForgiveMistakes = false;
            config.ShouldValidateBarcodes = false;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasic();
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(badBarcode);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(validBarcode1);

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value().size(), 2);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value(0), badBarcode);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().value(1), validBarcode1);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().barcode().meta().timestamp(), fixedTimestamp);
        }

        Y_UNIT_TEST_F(ISBNValidWithoutHyphen, TEnableVerdictsFixture) {
            const TString isbn = "4251620856539";

            TMap<TString, THolder<NMiner::IOfferContentConverter>> converters;
            converters["whiteConverter"] = NWhite::MakeContentConverter(capsFixer);
            converters["blueConverter"] = NBlue::MakeContentConverter(capsFixer);

            for (const auto& [_, converter] : converters) {
                NMarket::TDatacampOfferBatchProcessingContext processingContext;
                NMiner::TDatacampOffer dcOffer = MakeDefault();
                auto& offer = dcOffer.GetBasic();
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::BOOKS);

                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(isbn);
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

                (converter)->Process(dcOffer, processingContext, config, fixedTimestamp);
                UNIT_ASSERT_EQUAL(offer.has_resolution(), false);
            }
        }

        Y_UNIT_TEST_F(ISBNValidWithHyphen, TEnableVerdictsFixture) {
            const TString isbn = "978-3-16-148410-0";

            TMap<TString, THolder<NMiner::IOfferContentConverter>> converters;
            converters["whiteConverter"] = NWhite::MakeContentConverter(capsFixer);
            converters["blueConverter"] = NBlue::MakeContentConverter(capsFixer);

            for (const auto& [_, converter] : converters) {
                NMarket::TDatacampOfferBatchProcessingContext processingContext;
                NMiner::TDatacampOffer dcOffer = MakeDefault();
                auto& offer = dcOffer.GetBasic();
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::BOOKS);

                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(isbn);
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

                (converter)->Process(dcOffer, processingContext, config, fixedTimestamp);
                UNIT_ASSERT_EQUAL(offer.has_resolution(), false);
            }
        }

        Y_UNIT_TEST_F(ISBNInvalid, TEnableVerdictsFixture) {
            const TString isbn = "978-3-16-148410-1";

            TMap<TString, THolder<NMiner::IOfferContentConverter>> converters;
            converters["whiteConverter"] = NWhite::MakeContentConverter(capsFixer);
            converters["blueConverter"] = NBlue::MakeContentConverter(capsFixer);

            for (const auto& [_, converter] : converters) {
                NMarket::TDatacampOfferBatchProcessingContext processingContext;
                NMiner::TDatacampOffer dcOffer = MakeDefault();
                auto& offer = dcOffer.GetBasic();
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://url.ru/1");
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::BOOKS);

                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value(isbn);
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

                (converter)->Process(dcOffer, processingContext, config, fixedTimestamp);
                UNIT_ASSERT_EQUAL(offer.has_resolution(), true);
                UNIT_ASSERT_EQUAL(ResolutionHasCode(offer.resolution(), "358"), true);
            }
        }

        Y_UNIT_TEST(ShouldNotCreateEmptyBarcode) {
            config.ForgiveMistakes = false;
            config.ShouldValidateBarcodes = false;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasic();

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(not offer.content().partner().actual().has_barcode());
        }

}
