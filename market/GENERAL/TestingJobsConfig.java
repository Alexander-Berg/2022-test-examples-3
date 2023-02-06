package ru.yandex.market.billing.price;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.logbroker.MarketQuickLogbrokerSendingService;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.OfferId;
import ru.yandex.market.core.offer.PapiMarketSkuOfferService;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;

/**
 * Конфиг для джоб, работающих в тестинге.
 *
 * @author ogonek 27.07.2018
 */
@Configuration
@Profile("testing")
public class TestingJobsConfig {

    private static final long FEED_ID = 200303249;
    private static final String OFFER_ID = "3";

    private static final long CLIENT_ID = 9706741;

    private static final IndexerOfferKey PRICE_FEED_MARKET_SKU = IndexerOfferKey.marketSku(200345026L,
            100126182688L);
    private static final IndexerOfferKey HIDING_FEED_MARKET_SKU = IndexerOfferKey.marketSku(200345026L,
            100126182689L);

    private static final long CLIENT_ID_MARKET_SKU = 81129326;

    private static final long DATASOURCE_ID = 10203879;


    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PapiMarketSkuOfferService papiMarketSkuOfferService;

    @Autowired
    private MarketQuickLogbrokerSendingService logbrokerSendingService;

    @CronTrigger(
            description = "Обновление цены тестового офера для выгрузки PAPI-цен в MDS-S3",
            cronExpression = "0 * * * * ?"
    )
    @Bean
    public PublishTestPapiOfferPriceExecutor testingPublishTestPapiOfferPriceExecutor() {
        return new PublishTestPapiOfferPriceExecutor(
                transactionTemplate,
                papiMarketSkuOfferService,
                OfferId.of(
                        FEED_ID,
                        OFFER_ID
                ),
                CLIENT_ID,
                DATASOURCE_ID,
                logbrokerSendingService);
    }

    @CronTrigger(
            description = "Обновление цены тестового офера для выгрузки PAPI-цен в MDS-S3",
            cronExpression = "0 * * * * ?"
    )
    @Bean
    public PublishTestPapiMarketSkuOfferPriceExecutor testingPublishTestPapiMarketSkuOfferPriceExecutor() {
        return new PublishTestPapiMarketSkuOfferPriceExecutor(transactionTemplate, papiMarketSkuOfferService,
                PRICE_FEED_MARKET_SKU, HIDING_FEED_MARKET_SKU, CLIENT_ID_MARKET_SKU, DATASOURCE_ID);
    }
}
