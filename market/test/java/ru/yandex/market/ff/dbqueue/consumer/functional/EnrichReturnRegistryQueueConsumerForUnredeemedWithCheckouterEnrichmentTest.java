package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.EnrichReturnRegistryQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.EnrichReturnRegistryPayload;
import ru.yandex.market.ff.util.CreateCheckouterOrdersForReturnUtils;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EnrichReturnRegistryQueueConsumerForUnredeemedWithCheckouterEnrichmentTest
        extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private EnrichReturnRegistryQueueConsumer consumer;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-registry-no-return-info.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-registry-no-return-info.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistryNoReturnInfo() {

        when(checkouterAPI.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(CreateCheckouterOrdersForReturnUtils.createPagedOrders(1));
        when(lomClient.searchOrders(any(), any()))
                .thenReturn(PageResult.of(List.of(CreateCheckouterOrdersForReturnUtils.createLomOrderDto()), 1, 0, 1));

        executeTask(1L);

        verifyZeroInteractions(lomClient);
    }

    @Test
    @DatabaseSetup("/service/returns/before-enrich-registry-with-return-info-no-order-id.xml")
    @ExpectedDatabase(value = "/service/returns/after-enrich-registry-with-return-info-no-order-id.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistryWithReturnInfoNoOrderId() {

        when(checkouterAPI.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(CreateCheckouterOrdersForReturnUtils.createPagedOrders(1));
        when(lomClient.searchOrders(any(), any()))
                .thenReturn(PageResult.of(List.of(CreateCheckouterOrdersForReturnUtils.createLomOrderDto()), 1, 0, 1));

        executeTask(1L);

        verifyZeroInteractions(lomClient);
    }

    @Test
    @DatabaseSetup("/service/returns/before-enrich-registry-with-return-info-with-order-id.xml")
    @ExpectedDatabase(value = "/service/returns/after-enrich-registry-with-return-info-with-order-id.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistryWithReturnInfoWithOrderId() {

        when(checkouterAPI.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(CreateCheckouterOrdersForReturnUtils.createPagedOrders(1));
        when(lomClient.searchOrders(any(), any()))
                .thenReturn(PageResult.of(List.of(CreateCheckouterOrdersForReturnUtils.createLomOrderDto()), 1, 0, 1));

        executeTask(1L);

        verifyZeroInteractions(lomClient);
    }

    @Test
    @DatabaseSetup("/service/returns/before-enrich-registry-with-return-no-items.xml")
    @ExpectedDatabase(value = "/service/returns/after-enrich-registry-with-return-no-items.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistryWithReturnNoItems() {
        when(checkouterOrderHistoryEventsApi.getOrdersHistoryEvents(any()))
            .thenReturn(new OrderHistoryEvents(List.of()));
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
            .thenReturn(CreateCheckouterOrdersForReturnUtils.createPagedOrders(1));

        executeTask(1L);
        verifyZeroInteractions(lomClient);
    }

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-registry-from-lom.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-registry-from-lom.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyModifyRegistryFromLom() {
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = 'DefaultReturnInvalidRegistryUnitsServiceImpl' where id = 1008");
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(CreateCheckouterOrdersForReturnUtils.createPagedOrders(1));
        when(lomClient.searchOrders(any(), any()))
                .thenReturn(PageResult.of(List.of(CreateCheckouterOrdersForReturnUtils.createLomOrderDto()), 1, 0, 1));
        executeTask(1L);
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = null where id = 1008");
    }

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-unredeemed-with-incorrect-order-id.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-unredeemed-with-incorrect-order-id.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyModifyRegistry() {
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = 'DefaultReturnInvalidRegistryUnitsServiceImpl' where id = 1008");
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(CreateCheckouterOrdersForReturnUtils.createPagedOrders(1));
        when(lomClient.searchOrders(any(), any()))
                .thenReturn(PageResult.of(List.of(CreateCheckouterOrdersForReturnUtils.createLomOrderDto()), 1, 0, 1));
        executeTask(1L);
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = null where id = 1008");
    }

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-unredeemed-with-all-incorrect-order-ids.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-unredeemed-with-all-incorrect-order-ids.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldMarkRequestInvalidInCaseOfFullModification() {
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = 'DefaultReturnInvalidRegistryUnitsServiceImpl' where id = 1008");
        executeTask(1L);
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = null where id = 1008");
        verify(checkouterAPI, never()).getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class));
    }

    @Test
    @DatabaseSetup("/service/returns/before-enrich-unredeemed-from-lrm.xml")
    @ExpectedDatabase(value = "/service/returns/after-enrich-unredeemed-from-lrm.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichUnredeemedFromLrm() {
        executeTask(1L);
    }

    @Test
    @DatabaseSetup("/service/returns/before-enrich-unredeemed-from-lrm-when-order-and-box-are-same.xml")
    @ExpectedDatabase(
            value = "/service/returns/after-enrich-unredeemed-from-lrm-when-order-and-box-are-same.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichUnredeemedFromLrmWhenOrderAndBoxAreSame() {
        executeTask(1L);
    }

    private TaskExecutionResult executeTask(long requestId) {
        var payload = new EnrichReturnRegistryPayload(requestId);
        var task = new Task<>(new QueueShardId("shard"), payload, 0,
            ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        return transactionTemplate.execute(status -> consumer.execute(task));
    }

}
