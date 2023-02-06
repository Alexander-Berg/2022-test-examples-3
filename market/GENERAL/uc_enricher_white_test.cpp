#include <market/idx/datacamp/lib/currency_exchange/currency_rates_reader.h>
#include <market/idx/datacamp/lib/utils/dependencies.h>
#include <market/idx/datacamp/miner/lib/classifier_id_calculator.h>
#include <market/idx/datacamp/miner/lib/test_utils.h>
#include <market/idx/datacamp/miner/processors/uc_enricher/uc_enricher_white.cpp>
#include <market/library/libyt/YtHelpers.h>

#include <library/cpp/testing/mock_server/server.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/string/printf.h>
#include <util/system/env.h>


namespace {
    using google::protobuf::util::TimeUtil;
    using NMarket::NDataCamp::TCurrencyRatesFileReader;
    static const TDuration UC_REQUESTS_TTL = TDuration::Hours(8);

    class DummyUselessHandler: public NMiner::IUCEnricherHandler {
    public:
        void OnEnrichEvent(const NMiner::IUCEnricherHandler::TEnrichEvent&) const override {}
    };
    const DummyUselessHandler DummyUselessHandlerInstance;

    class TParrotReplier: public TRequestReplier {
    public:
        TParrotReplier(const TString& name, const TString& answer)
            : Answer_(answer)
            , Name_(name)
        {
        }

        bool DoReply(const TReplyParams& params) override {
            TParsedHttpFull request(params.Input.FirstLine());
            const auto& headers = params.Input.Headers();
            const TString& body = params.Input.ReadAll();

            Cout << Name_ << Endl;
            // Cout << request << Endl;
            Cout << headers << Endl;
            Cout << body << Endl;
            Cout << Answer_ << Endl;

            THttpResponse resp(HttpCodes::HTTP_OK);
            resp.SetContent(Answer_);
            resp.OutTo(params.Output);
            return true;
        }

    private:
        TString Answer_;
        TString Name_;
    };

    template<typename TResponse>
    NMock::TMockServer CreateMockServer (
            const ui16 port,
            const TResponse& response,
            const TString& name,
            i64& requestsCounter
    ) {
        TString serialized = response.SerializeAsString();
        return NMock::TMockServer(port, [name, serialized, &requestsCounter]() {
            ++requestsCounter;
            return new TParrotReplier(name, serialized);
        });
    }

    NMock::TMockServer CreateSimpleUcMockServer(
            const ui16 port,
            const Market::UltraControllerServiceData::DataResponse& response,
            i64& requestsCounter
    ) {
        return CreateMockServer<Market::UltraControllerServiceData::DataResponse>(
            port, response, "UcMockServer", requestsCounter
        );
    }

    struct TOfferData {
        ui64 BusinessId = 101;
        TString OfferId;
        ui64 ShopId = 101;
        ui64 FeedId = 0;
        ui64 CategoryId = 0;
        ui64 ModelId = 11;
        ui64 MskuId = 22;
        ui64 Price = 10000000;
        TString GoodId = "1c5ed202011c90cc7167d110abab15a5";
        ui64 LastRequestMskuId = 0;
        TString LastRequestGoodId;
        TInstant LastRequestTs;
        Market::DataCamp::MarketColor Color = Market::DataCamp::MarketColor::WHITE;
    };

    class TUcEnrichWhiteTest: public ::testing::Test {
    protected:
        void SetUp() override {
            auto currencyRatesFileReader = MakeHolder<TCurrencyRatesFileReader>(
                TDuration::Seconds(1),
                TFsPath(ArcadiaSourceRoot()) / "market/idx/yatf/resources/stubs/getter/mbi/currency_rates_no_bank_for_EUR.xml");
            currencyRatesFileReader->LoadCurrencyRates();
            NMarket::NDataCamp::TDependencies::Set("ClassifierIdCalculator", NMiner::NUnited::MakeClassifierIdCalculator());
        }

        NMiner::TUCEnricherConfig CreateConfig() const {
            TPortManager pm;

            NMiner::TUCEnricherConfig config("test");
            config.UcHost = "localhost";
            config.UcPort = pm.GetPort();
            config.EnableUcRequestsDeduplicator = true;
            config.UcRequestsTtl = UC_REQUESTS_TTL;
            config.UcFormalizedParamsSizeLimit = 100;

            return config;
        }

        void AddOfferToBatch(
                Market::DataCamp::Offer& basic,
                Market::DataCamp::Offer& service,
                NMiner::TDatacampOfferBatch& batch,
                const TOfferData& offerData
        ) const noexcept {
            // set up basic offer
            basic.mutable_identifiers()->set_business_id(offerData.BusinessId);
            basic.mutable_identifiers()->set_offer_id(offerData.OfferId);
            basic.mutable_identifiers()->mutable_extra()->set_classifier_good_id(offerData.GoodId);
            basic.mutable_content()->mutable_binding()->mutable_smb_partner()->set_market_category_id(offerData.CategoryId);
            basic.mutable_content()->mutable_binding()->mutable_mapping_for_uc()->set_market_category_id(offerData.CategoryId);
            basic.mutable_content()->mutable_binding()->mutable_mapping_for_uc()->set_market_sku_id(offerData.MskuId);
            basic.mutable_content()->mutable_binding()->mutable_mapping_for_uc()->set_market_model_id(offerData.ModelId);

            // set up service offer
            service.mutable_identifiers()->set_business_id(offerData.BusinessId);
            service.mutable_identifiers()->set_offer_id(offerData.OfferId);
            service.mutable_identifiers()->set_feed_id(offerData.FeedId);
            service.mutable_identifiers()->set_shop_id(offerData.ShopId);
            service.mutable_status()->mutable_united_catalog()->set_flag(true);
            service.mutable_meta()->set_rgb(offerData.Color);
            service.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(offerData.Price);

            if (offerData.LastRequestTs) {
                auto* irRequest = basic.mutable_content()->mutable_market()->mutable_ir_request();
                irRequest->mutable_request_ts()->set_seconds(offerData.LastRequestTs.Seconds());
                irRequest->set_classifier_good_id(offerData.LastRequestGoodId ? offerData.LastRequestGoodId : offerData.GoodId);
                irRequest->set_market_sku_id(offerData.LastRequestMskuId ? offerData.LastRequestMskuId : offerData.MskuId);
                irRequest->set_market_model_id(offerData.ModelId);
                irRequest->set_market_category_id(offerData.CategoryId);
            }

            batch.EmplaceBack(&basic, &service);
        }

        Market::UltraControllerServiceData::EnrichedOffer CreateEnriched(
            const i64 category,
            bool hugeFormalizedParams = false
        ) const {
            Market::UltraControllerServiceData::EnrichedOffer enrichedOffer;
            enrichedOffer.set_category_id(category);
            enrichedOffer.set_classifier_category_id(category);

            int paramsCount = hugeFormalizedParams ? 100 : 2;
            for (int i = 0; i < paramsCount; ++i) {
                enrichedOffer.add_confident_params_for_psku()->set_param_name("param name");
            }

            return enrichedOffer;
        }

        Market::UltraControllerServiceData::DataResponse CreateUcResponse(const i64 category) const {
            Market::UltraControllerServiceData::DataResponse response;
            *response.add_offers() = CreateEnriched(category);  // "offer_1"
            *response.add_offers() = CreateEnriched(category);  // "offer_2"
            *response.add_offers() = CreateEnriched(category);  // "offer_4"
            *response.add_offers() = CreateEnriched(category);  // "offer_5"
            *response.add_offers() = CreateEnriched(category);  // "offer_6"
            *response.add_offers() = CreateEnriched(category, true /* hugeFormalizedParams */);  // "offer_7"
            return response;
        }
    };
}


TEST_F(TUcEnrichWhiteTest, Enrich) {
    static constexpr i64 FEED_ID = 10001;
    static constexpr i64 CATEGORY_ID = 303;
    static constexpr i64 UC_CATEGORY_ID = 305;

    auto config = CreateConfig();
    i64 requestsCount = 0;
    auto MockUC = CreateSimpleUcMockServer(
        config.UcPort,
        CreateUcResponse(UC_CATEGORY_ID),
        requestsCount
    );

    THolder<IMultiWriter> loggerMock;
    auto enricher = NMiner::TUCEnricherImpl(config, DummyUselessHandlerInstance, std::move(loggerMock));
    auto batch = MakeDefault<Market::DataCamp::OffersBatch, NMiner::TDatacampOfferBatch>();
    auto basic1 = Market::DataCamp::Offer();
    auto service1 = Market::DataCamp::Offer();
    AddOfferToBatch(basic1, service1, batch, TOfferData {
        .OfferId = "offer_1",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID
    });
    // Оффер без цены, по нему будет запрос в УК, но не будет сохранения в контекст майнинга
    auto basic2 = Market::DataCamp::Offer();
    auto service2 = Market::DataCamp::Offer();
    AddOfferToBatch(basic2, service2, batch, TOfferData {
        .OfferId = "offer_2",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID,
        .Price = 0
    });
    // Этот оффер в УК не будем запрашивать (и в контекст не положим), т.к. по нему есть ответ в рамках TTL
    auto basic3 = Market::DataCamp::Offer();
    auto service3 = Market::DataCamp::Offer();
    AddOfferToBatch(basic3, service3, batch, TOfferData {
        .OfferId = "offer_3",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID,
        .LastRequestTs = TInstant::Now() - UC_REQUESTS_TTL + TDuration::Hours(1)
    });
    // Этот оффер в УК будем запрашивать, т.к. по нему есть ответ, но не в рамках TTL
    auto basic4 = Market::DataCamp::Offer();
    auto service4 = Market::DataCamp::Offer();
    AddOfferToBatch(basic4, service4, batch, TOfferData {
        .OfferId = "offer_4",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID,
        .LastRequestTs = TInstant::Now() - UC_REQUESTS_TTL - TDuration::Hours(1)
    });
    // Этот оффер в УК будем запрашивать: по нему есть ответ в рамках TTL, но изменился good_id
    auto basic5 = Market::DataCamp::Offer();
    auto service5 = Market::DataCamp::Offer();
    AddOfferToBatch(basic5, service5, batch, TOfferData {
        .OfferId = "offer_5",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID,
        .LastRequestGoodId = "123",
        .LastRequestTs = TInstant::Now() - UC_REQUESTS_TTL + TDuration::Hours(1)
    });
    // Этот оффер в УК будем запрашивать: по нему есть ответ в рамках TTL, но изменился маппинг
    auto basic6 = Market::DataCamp::Offer();
    auto service6 = Market::DataCamp::Offer();
    AddOfferToBatch(basic6, service6, batch, TOfferData {
        .OfferId = "offer_6",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID,
        .LastRequestMskuId = 222,
        .LastRequestTs = TInstant::Now() - UC_REQUESTS_TTL + TDuration::Hours(1)
    });
    // Оффер для которого вернутся формализованные параметры огромного размера
    auto basic7 = Market::DataCamp::Offer();
    auto service7 = Market::DataCamp::Offer();
    AddOfferToBatch(basic7, service7, batch, TOfferData {
        .OfferId = "offer_7",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID
    });

    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOfferBatchMiningContext miningContext;

    NMiner::IUCEnricher::TResult enrichResult = enricher.EnrichOffers(batch, miningContext, context);
    EXPECT_STREQ(enrichResult, "");

    // Выполнен один запрос на батч
    EXPECT_EQ(requestsCount, 1);
    // В контекст майнига не попал
    //   - "offer_2", т.к. у него нет цены
    //   - "offer_3", т.к. он в пределах ttl
    EXPECT_EQ(miningContext.UcDeduplicator.size(), 5);
    EXPECT_TRUE(miningContext.UcDeduplicator.contains(MakeBasicOfferId(basic1.identifiers())));
    EXPECT_TRUE(miningContext.UcDeduplicator.contains(MakeBasicOfferId(basic4.identifiers())));
    EXPECT_TRUE(miningContext.UcDeduplicator.contains(MakeBasicOfferId(basic5.identifiers())));
    EXPECT_TRUE(miningContext.UcDeduplicator.contains(MakeBasicOfferId(basic6.identifiers())));
    EXPECT_TRUE(miningContext.UcDeduplicator.contains(MakeBasicOfferId(basic7.identifiers())));

    for (auto[index, offerId]: TVector<std::tuple<int, TString>>{
        std::make_tuple(0, "offer_1"),
        std::make_tuple(1, "offer_2"),
        std::make_tuple(3, "offer_4"),
        std::make_tuple(4, "offer_5"),  // Обновляем в кеше good_id, с которым ходили в УК
        std::make_tuple(5, "offer_6"),  // Обновляем в кеше msku_id, с которым ходили в УК
    }) {
        auto* basic = &batch.offer(index).GetBasic();
        EXPECT_EQ(basic->identifiers().offer_id(), offerId);

        // Ответы УК проставились в базовый оффер
        EXPECT_TRUE(basic->content().market().has_meta());
        EXPECT_EQ(basic->content().binding().smb_partner().market_category_id(), CATEGORY_ID);
        EXPECT_EQ(basic->content().binding().uc_mapping().market_category_id(), UC_CATEGORY_ID);
        EXPECT_EQ(basic->content().market().category_id(), UC_CATEGORY_ID);
        EXPECT_EQ(basic->content().market().ir_data().classifier_category_id(), UC_CATEGORY_ID);

        // В оффер сохраняется значение для контроля кеширования
        EXPECT_TRUE(basic->content().market().ir_request().has_request_ts());
        EXPECT_EQ(basic->content().market().ir_request().classifier_good_id(), "1c5ed202011c90cc7167d110abab15a5");
        EXPECT_EQ(basic->content().market().ir_request().market_sku_id(), 22);
        EXPECT_EQ(basic->content().market().ir_request().market_model_id(), 11);
        EXPECT_EQ(basic->content().market().ir_request().market_category_id(), CATEGORY_ID);
        EXPECT_EQ(basic->content().market().ir_request().price(), offerId == "offer_2" ? 0 : 1);

        // В оффер проставились формализованные параметры
        UNIT_ASSERT_EQUAL(basic->content().market().ir_data().confident_params_for_psku().size(), 2);
        UNIT_ASSERT_EQUAL(basic->content().market().ir_data().confident_params_for_psku(0).param_name(), "param name");
        UNIT_ASSERT(not basic->content().market().ir_data().params_rejected_by_size());
    }

    // Проверяем, что параметры огромного размера не проставляются оффер, взводится фллаг реджекта
    const auto& offerWitHugeParams = batch.offer(6).GetBasic();
    UNIT_ASSERT(offerWitHugeParams.content().market().ir_data().confident_params_for_psku().empty());
    UNIT_ASSERT(offerWitHugeParams.content().market().ir_data().params_rejected_by_size());

    // Проверяем, что данные для региональных клонов берутся из контекста и у них будут проставлены данные для контроля
    // кеширования
    batch = MakeDefault<Market::DataCamp::OffersBatch, NMiner::TDatacampOfferBatch>();
    basic1 = Market::DataCamp::Offer();
    service1 = Market::DataCamp::Offer();
    AddOfferToBatch(basic1, service1, batch, TOfferData {
        .OfferId = "offer_1",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID
    });
    basic4 = Market::DataCamp::Offer();
    service4 = Market::DataCamp::Offer();
    AddOfferToBatch(basic4, service4, batch, TOfferData {
        .OfferId = "offer_4",
        .FeedId = FEED_ID,
        .CategoryId = CATEGORY_ID,
        .LastRequestTs = TInstant::Now() - UC_REQUESTS_TTL - TDuration::Hours(1)
    });

    enrichResult = enricher.EnrichOffers(batch, miningContext, context);
    EXPECT_STREQ(enrichResult, "");

    // Все еще один запрос в УК, т.к. значение берется из контекста майнинга
    EXPECT_EQ(requestsCount, 1);

    for (auto[index, offerId]: TVector<std::tuple<int, TString>>{
        std::make_tuple(0, "offer_1"),
        std::make_tuple(1, "offer_4"),
    }) {
        auto* basic = &batch.offer(index).GetBasic();
        EXPECT_EQ(basic->identifiers().offer_id(), offerId);

        // Ответы УК проставились в базовый оффер из контекста майнинга
        EXPECT_TRUE(basic->content().market().has_meta());
        EXPECT_EQ(basic->content().binding().smb_partner().market_category_id(), CATEGORY_ID);
        EXPECT_EQ(basic->content().binding().uc_mapping().market_category_id(), UC_CATEGORY_ID);
        EXPECT_EQ(basic->content().market().category_id(), UC_CATEGORY_ID);
        EXPECT_EQ(basic->content().market().ir_data().classifier_category_id(), UC_CATEGORY_ID);

        // В оффер сохраняется значение для контроля кеширования (в данном случае, берется из контекста майнинга)
        EXPECT_TRUE(basic->content().market().ir_request().has_request_ts());
        EXPECT_EQ(basic->content().market().ir_request().classifier_good_id(), "1c5ed202011c90cc7167d110abab15a5");
        EXPECT_EQ(basic->content().market().ir_request().market_sku_id(), 22);
        EXPECT_EQ(basic->content().market().ir_request().market_model_id(), 11);
        EXPECT_EQ(basic->content().market().ir_request().market_category_id(), CATEGORY_ID);
    }
}
