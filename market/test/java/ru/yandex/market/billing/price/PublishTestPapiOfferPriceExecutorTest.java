package ru.yandex.market.billing.price;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.logbroker.MarketQuickLogbrokerSendingService;
import ru.yandex.market.core.offer.OfferId;
import ru.yandex.market.core.offer.PapiMarketSkuOfferService;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тест для проверки апдейта цены тестового офера {@link PublishTestPapiOfferPriceExecutor}.
 */
class PublishTestPapiOfferPriceExecutorTest extends FunctionalTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PapiMarketSkuOfferService marketSkuOfferService;

    @Autowired
    private MarketQuickLogbrokerSendingService logbrokerSendingService;

    private PublishTestPapiOfferPriceExecutor productionPublishTestPapiOfferPriceExecutor;

    @BeforeEach
    void setUp() {
        logbrokerSendingService = Mockito.mock(MarketQuickLogbrokerSendingService.class);
        productionPublishTestPapiOfferPriceExecutor = new PublishTestPapiOfferPriceExecutor(
                transactionTemplate,
                marketSkuOfferService,
                OfferId.of(6071L, "3"),
                11,
                774,
                logbrokerSendingService);
    }

    @Test
    @DbUnitDataSet(before = "PublishTestPapiOfferPriceExecutorTest.before.csv")
    void checkPublishTestOffer() {
        productionPublishTestPapiOfferPriceExecutor.doJob(null);
        Mockito.verify(logbrokerSendingService).sendUpdateOfferPricesEvent(any(),
                Mockito.eq(PartnerId.datasourceId(774L)));

        Assertions.assertEquals(1, marketSkuOfferService.countAll());
    }
}
