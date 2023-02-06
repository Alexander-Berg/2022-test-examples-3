package ru.yandex.market.ff.tms;

import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.RequestDetailsService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestXDocSupplyService;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundDetailsXDoc;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link LoadXDocRequestDetailsExecutor}.
 */
class LoadXDocRequestDetailsExecutorTest extends IntegrationTest {

    private static final Partner PARTNER = new Partner(122L);

    @Autowired
    private RequestDetailsService requestDetailsService;

    @Autowired
    private LgwRequestXDocSupplyService lgwRequestXDocSupplyService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    private LoadXDocRequestDetailsExecutor executor;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        executor = new LoadXDocRequestDetailsExecutor(requestDetailsService,
                lgwRequestXDocSupplyService,
                transactionTemplate,
                Executors.newSingleThreadExecutor(),
                shopRequestFetchingService,
                historyAgent);

        ResourceId resourceIdInbound1 = ResourceId.builder()
                .setYandexId("1")
                .setPartnerId("11")
                .build();

        InboundDetailsXDoc inboundDetailsXDoc = new InboundDetailsXDoc(
                resourceIdInbound1, 5, 10);

        when(fulfillmentClient.getInboundDetailsXDoc(resourceIdInbound1, PARTNER))
                .thenReturn(inboundDetailsXDoc);

        ResourceId resourceIdInbound2 = ResourceId.builder()
                .setYandexId("2")
                .setPartnerId("22")
                .build();

        InboundDetailsXDoc inboundDetailsXDoc2 = new InboundDetailsXDoc(
                resourceIdInbound1, 35, 45);

        when(fulfillmentClient.getInboundDetailsXDoc(resourceIdInbound2, PARTNER))
                .thenReturn(inboundDetailsXDoc2);

        ResourceId resourceIdInbound3 = ResourceId.builder()
                .setYandexId("4")
                .setPartnerId("44")
                .build();

        InboundDetailsXDoc inboundDetailsXDoc3 = new InboundDetailsXDoc(
                resourceIdInbound3, 35, 5);

        when(fulfillmentClient.getInboundDetailsXDoc(resourceIdInbound3, PARTNER))
                .thenReturn(inboundDetailsXDoc3);
    }

    @Test
    @DatabaseSetup(value = "classpath:tms/load-xdoc-request-details/before-update-details.xml")
    @ExpectedDatabase(value = "classpath:tms/load-xdoc-request-details/after-update-details.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyExtraItemsTurnedOff() {
        executor.doJob(null);
    }


}
