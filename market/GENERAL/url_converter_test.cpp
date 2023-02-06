#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/field_converters.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

    Y_UNIT_TEST_SUITE(UrlConverterTestSuite) {
        NMiner::TOfferContentConverterConfig config("");
        const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
        const auto oldTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(327605478); // 1980
        const auto oldUrl = "http://oldUrl.ru/kistibiy";
        const auto newUrl = "http://bahetle.ru/kistibiy";
        const auto newInvalidUrl = "not-url-at-all";
        const auto urlWithUtmTemplate = "http://example.com/data?utm_content=cid|{campaign_id}|gid|{gbid}";
        NMarket::NCapsLock::TCapsLockFixer capsFixer;

        NMiner::TDatacampOffer CreateOffer(Market::DataCamp::Offer& basic,
                                           Market::DataCamp::Offer& service,
                                           Market::DataCamp::MarketColor color,
                                           bool writeToService,
                                           TMaybe<TString> originalUrl,
                                           TMaybe<TString> actualUrl,
                                           bool isDsbs) {
            service.mutable_meta()->set_rgb(color);
            (*service.mutable_meta()->mutable_platforms())[color] = true;
            NMiner::TDatacampOffer dcOffer(&basic, &service);
            dcOffer.mutable_partner_info()->mutable_meta()->mutable_timestamp()->set_seconds(1);
            if (color == Market::DataCamp::MarketColor::WHITE) {
                dcOffer.mutable_partner_info()->set_is_dsbs(isDsbs);
            }
            dcOffer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title
            auto& offer = writeToService ? dcOffer.GetService() : dcOffer.GetBasicByColor();
            if (originalUrl.Defined()) {
                offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(*originalUrl);
            }
            if (actualUrl.Defined()) {
                dcOffer.GetService().mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->set_value(*actualUrl);
                dcOffer.GetService().mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            }
            return dcOffer;
        }

        void SetADVAttributes(Market::DataCamp::Offer& service) {
            service.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
            service.mutable_partner_info()->mutable_meta()->mutable_timestamp()->set_seconds(1); //partner_info is available
            service.mutable_partner_info()->set_is_dsbs(false); //ADV marker
            service.mutable_partner_info()->set_direct_product_mapping(false); //ADV marker -> url is required
        }

        void CheckOffer(TMaybe<TString> originalUrl,
                        TMaybe<TString> actualUrl,
                        bool isDsbs,
                        bool forgiveMistakes,
                        const TString expectedUrl,
                        bool expectedFlag)
        {
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto converter = NWhite::MakeContentConverter(capsFixer);

            for (const bool writeToService: {true, false}) {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, Market::DataCamp::MarketColor::WHITE, writeToService, originalUrl, actualUrl, isDsbs);
                config.ForgiveMistakes = forgiveMistakes;
                converter->Process(dcOffer, processingContext, config, fixedTimestamp);

                if (writeToService) {
                    UNIT_ASSERT_EQUAL(dcOffer.GetService().content().partner().actual().url().value(), expectedUrl);
                    CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, expectedFlag);
                }
                else if (!actualUrl.Defined()) { // фолбека в базовую больше нет
                    UNIT_ASSERT_EQUAL(dcOffer.GetService().content().partner().actual().url().value(), "");
                }
                // Пишется только в сервисную часть
                UNIT_ASSERT_EQUAL(dcOffer.GetBasic().content().partner().actual().url().value(), "");
            }
        }

        // Url in white vs smb tests
        Y_UNIT_TEST(UrlIsRequiredButItIsAbsent) {
            // ADV - url is required
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer offer = MakeDefault();
            offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value(
                    "title"); // чтобы не получить disable из-за title

            SetADVAttributes(offer.GetService());

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(offer, processingContext, config, fixedTimestamp);

            CheckFlag(offer, Market::DataCamp::DataSource::MARKET_IDX, true);

            CheckOffer(/* originalUrl */ Nothing(),
                       /* actualUrl */ Nothing(),
                       /* isDsbs */ false, //ADV
                       /* forgiveMistakes */ false,
                       /* expectedUrl */ "",
                       /* expectedFlag */ true);
        }

        Y_UNIT_TEST(UrlIsRequiredButItIsEmpty) {
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();

            SetADVAttributes(dcOffer.GetService());

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("");
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, true);

            CheckOffer(/* originalUrl */ "",
                       /* actualUrl */ Nothing(),
                       /* isDsbs */ false,
                       /* forgiveMistakes */ false,
                       /* expectedUrl */ "",
                       /* expectedFlag */ true);
        }

        Y_UNIT_TEST(UrlIsRequiredAndOk) {
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(newUrl);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->set_value(oldUrl);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().value(), newUrl);

            CheckOffer(/* originalUrl */ newUrl,
                       /* actualUrl */ Nothing(),
                       /* isDsbs */ false,
                       /* forgiveMistakes */ false,
                       /* expectedUrl */ newUrl,
                       /* expectedFlag */ false);
        }

        Y_UNIT_TEST(UrlIsOptionalAndEmpty) {
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.clear_partner_info(); // Offer is not enriched with shops.dat info
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("");
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);

            CheckOffer(/* originalUrl */ "",
                       /* actualUrl */ Nothing(),
                       /* isDsbs */ true,
                       /* forgiveMistakes */ false,
                       /* expectedUrl */ "",
                       /* expectedFlag */ false);
        }

        Y_UNIT_TEST(UrlIsRequiredAndEmptyWithValidOldActual) {
            // если в original поле нет url, но он необходим, при наличие хорошего url в actual сохраним его (при ForgiveMistakes = true)
            config.ForgiveMistakes = true;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();

            SetADVAttributes(dcOffer.GetService());

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("");
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->set_value(oldUrl);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, true);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().value(), oldUrl);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().meta().timestamp(), oldTimestamp);

            CheckOffer(/* originalUrl */ "",
                       /* actualUrl */ oldUrl,
                       /* isDsbs */ false,
                       /* forgiveMistakes */ true,
                       /* expectedUrl */ oldUrl,
                       /* expectedFlag */ true);

            // при выключенном режиме прощения всё пройдет по-старому - actual очистится
            config.ForgiveMistakes = false;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(!offer.content().partner().actual().url().has_value());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().meta().timestamp(), fixedTimestamp);

            CheckOffer(/* originalUrl */ "",
                       /* actualUrl */ oldUrl,
                       /* isDsbs */ false,
                       /* forgiveMistakes */ false,
                       /* expectedUrl */ "",
                       /* expectedFlag */ true);
        }

        Y_UNIT_TEST(UrlIsOptionalAndInvalidWithValidOldActual) {
            // Если url необязательный и невалидный - работает также, как для обязательного (см тест выше)

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_partner_info()->set_direct_product_mapping(true);

            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(newInvalidUrl);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->set_value(oldUrl);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);

            config.ForgiveMistakes = true;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().value(), oldUrl);
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().meta().timestamp(), oldTimestamp);

            CheckOffer(/* originalUrl */ newInvalidUrl,
                       /* actualUrl */ oldUrl,
                       /* isDsbs */ true,
                       /* forgiveMistakes */ true,
                       /* expectedUrl */ oldUrl,
                       /* expectedFlag */ false);

            config.ForgiveMistakes = false;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            UNIT_ASSERT(!offer.content().partner().actual().url().has_value());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().meta().timestamp(), fixedTimestamp);

            CheckOffer(/* originalUrl */ newInvalidUrl,
                       /* actualUrl */ oldUrl,
                       /* isDsbs */ true,
                       /* forgiveMistakes */ false,
                       /* expectedUrl */ "",
                       /* expectedFlag */ false);
        }

        Y_UNIT_TEST(UrlIsOptionalAndEmptyWithValidOldActual) {
            // Если url необязательный и пустой, даем партнеру возможность удалить значение необязательного поля
            // при любом значении ForgiveMistakes

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            auto& offer = dcOffer.GetBasicByColor();
            offer.mutable_partner_info()->set_direct_product_mapping(true);

            // не задаем original url
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->set_value(oldUrl);
            offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_url()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value("title"); // чтобы не получить disable из-за title

            auto converter = NWhite::MakeContentConverter(capsFixer);

            config.ForgiveMistakes = true;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            UNIT_ASSERT(!offer.content().partner().actual().url().has_value());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().meta().timestamp(), fixedTimestamp);

            CheckOffer(/* originalUrl */ Nothing(),
                       /* actualUrl */ oldUrl,
                       /* isDsbs */ true,
                       /* forgiveMistakes */ true,
                       /* expectedUrl */ "",
                       /* expectedFlag */ false);

            config.ForgiveMistakes = false;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            UNIT_ASSERT(!offer.content().partner().actual().url().has_value());
            UNIT_ASSERT_EQUAL(offer.content().partner().actual().url().meta().timestamp(), fixedTimestamp);
            CheckOffer(/* originalUrl */ Nothing(),
                       /* actualUrl */ oldUrl,
                       /* isDsbs */ true,
                       /* forgiveMistakes */ false,
                       /* expectedUrl */ "",
                       /* expectedFlag */ false);
        }

        Y_UNIT_TEST(BlueUrlConverter) {
            auto doCheck = [&](TString url, bool unitedCatalogStatus, bool expectedFlag) {
                NMarket::TDatacampOfferBatchProcessingContext processingContext;
                NMiner::TDatacampOffer dcOffer = MakeDefault();
                dcOffer.mutable_meta()->set_rgb(Market::DataCamp::BLUE);
                (*dcOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::BLUE] = true;
                dcOffer.mutable_status()->mutable_united_catalog()->set_flag(unitedCatalogStatus);

                auto& serviceOffer = dcOffer.GetService();
                serviceOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(url);

                FillUrl(dcOffer, processingContext, config, fixedTimestamp);

                if (url == newUrl) {
                    UNIT_ASSERT(serviceOffer.content().partner().actual().url().has_value());
                    UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().url().value(), newUrl);
                    UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().url().meta().timestamp(), fixedTimestamp);
                } else {
                    UNIT_ASSERT(!serviceOffer.content().partner().actual().url().has_value());
                }

                CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, expectedFlag);
            };

            doCheck(newUrl, false, false);
            doCheck(newUrl, true, false);
            doCheck(newInvalidUrl, false, false);
            doCheck(newInvalidUrl, true, false);
        }

        Y_UNIT_TEST(SMBUrlConverter) {
            // SMB - url is optional
            auto doCheck = [&](TString url, bool expectedFlag) {
                NMarket::TDatacampOfferBatchProcessingContext processingContext;
                NMiner::TDatacampOffer dcOffer = MakeDefault();
                dcOffer.mutable_meta()->set_rgb(Market::DataCamp::WHITE);
                (*dcOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::WHITE] = true;
                dcOffer.mutable_partner_info()->mutable_meta()->mutable_timestamp()->set_seconds(1);
                dcOffer.mutable_partner_info()->set_direct_product_mapping(true);

                auto& serviceOffer = dcOffer.GetService();
                serviceOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(url);

                FillUrl(dcOffer, processingContext, config, fixedTimestamp);

                if (url == newUrl) {
                    UNIT_ASSERT(serviceOffer.content().partner().actual().url().has_value());
                    UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().url().value(), newUrl);
                    UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().url().meta().timestamp(), fixedTimestamp);
                } else {
                    UNIT_ASSERT(!serviceOffer.content().partner().actual().url().has_value());
                }

                CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, expectedFlag);
            };

            doCheck(newUrl, false);
            doCheck(newInvalidUrl, false);
        }

        Y_UNIT_TEST(DirectUrlConverter) {
            auto doCheck = [&](const TString& url, Market::DataCamp::MarketColor rgb, bool isUrlValid) {
                NMarket::TDatacampOfferBatchProcessingContext processingContext;
                NMiner::TDatacampOffer dcOffer = MakeDefault();
                dcOffer.mutable_meta()->set_rgb(rgb);
                (*dcOffer.mutable_meta()->mutable_platforms())[rgb] = true;

                auto& serviceOffer = dcOffer.GetService();
                serviceOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(url);

                FillUrl(dcOffer, processingContext, config, fixedTimestamp);

                if (isUrlValid) {
                    UNIT_ASSERT(serviceOffer.content().partner().actual().url().has_value());
                    UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().url().value(), url);
                    UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().url().meta().timestamp(), fixedTimestamp);
                } else {
                    UNIT_ASSERT(!serviceOffer.content().partner().actual().url().has_value());
                }

                CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
            };
            std::vector<Market::DataCamp::MarketColor> colors{
                Market::DataCamp::DIRECT_SEARCH_SNIPPET_GALLERY,
                Market::DataCamp::DIRECT_STANDBY
            };
            for (const auto rgb : colors) {
                doCheck(urlWithUtmTemplate, rgb, true);
                doCheck(newUrl, rgb, true);
                doCheck(newInvalidUrl, rgb, false);
            }
        }

        Y_UNIT_TEST(WhiteWithUtmTemplateUrlConverter) {
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            NMiner::TDatacampOffer dcOffer = MakeDefault();
            dcOffer.mutable_meta()->set_rgb(Market::DataCamp::WHITE);
            (*dcOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::WHITE] = true;

            auto& serviceOffer = dcOffer.GetService();
            serviceOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value(urlWithUtmTemplate);

            FillUrl(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT(!serviceOffer.content().partner().actual().url().has_value());
            CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
        }
}
