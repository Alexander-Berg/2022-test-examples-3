package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.EnrichReturnRegistryQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.EnrichReturnRegistryPayload;
import ru.yandex.market.ff.util.YqlTablesInPgUtils;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class EnrichReturnRegistryQueueConsumerForRussianPostTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private EnrichReturnRegistryQueueConsumer consumer;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    @Qualifier("yqlAutoClusterNamedJdbcTemplate")
    private NamedParameterJdbcTemplate yqlJdbcTemplate;
    @Autowired
    @Qualifier("internalObjectMapper")
    private ObjectMapper mapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        YqlTablesInPgUtils.recreateTables(yqlJdbcTemplate);
    }

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-return-registry-for-russian-post.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-return-registry-for-russian-post.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistry() {
        YqlTablesInPgUtils.insertIntoTrack(yqlJdbcTemplate, "30", "box3");
        YqlTablesInPgUtils.insertIntoReturn(yqlJdbcTemplate, 30, 3, 2);
        YqlTablesInPgUtils.insertIntoReturnDelivery(yqlJdbcTemplate, 30, 1005486);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 10001, 3, 30, 333, 1, "damaged", 3);

        YqlTablesInPgUtils.insertIntoReturn(yqlJdbcTemplate, 30, 3, 30);
        YqlTablesInPgUtils.insertIntoReturnDelivery(yqlJdbcTemplate, 3, 1005486);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 10002, 3, 30, 3333, 1, "", 1);

        YqlTablesInPgUtils.insertIntoTrack(yqlJdbcTemplate, "50", "box5");
        YqlTablesInPgUtils.insertIntoReturn(yqlJdbcTemplate, 50, 5, 50);
        YqlTablesInPgUtils.insertIntoReturnDelivery(yqlJdbcTemplate, 50, 1005486);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 10003, 5, 50, 555, 1, "very bad", 0);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 10004, 5, 50, 555, 1, "very bad", 0);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 10005, 5, 50, 555, 1, "", 1);

        mockCheckouterAnswer();

        executeTask(2L);
    }

    @Test
    @DatabaseSetup("/service/returns/before-enrich-return-registry-for-russian-post-with-invalid-units.xml")
    @ExpectedDatabase(
            value = "/service/returns/after-enrich-return-registry-for-russian-post-with-invalid-units.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void shouldAddOnlyNewInvalidUnits() {
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = 'DefaultReturnInvalidRegistryUnitsServiceImpl' where id = 1007");
        YqlTablesInPgUtils.insertIntoTrack(yqlJdbcTemplate, "30", "box3");
        YqlTablesInPgUtils.insertIntoReturn(yqlJdbcTemplate, 30, 3, 2);
        YqlTablesInPgUtils.insertIntoReturnDelivery(yqlJdbcTemplate, 30, 1005486);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 10001, 3, 30, 333, 1, "damaged", 3);

        YqlTablesInPgUtils.insertIntoReturn(yqlJdbcTemplate, 30, 3, 30);
        YqlTablesInPgUtils.insertIntoReturnDelivery(yqlJdbcTemplate, 3, 1005486);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 10002, 3, 30, 3333, 1, "", 1);

        mockCheckouterAnswer();

        executeTask(2L);
    }

    private void executeTask(long requestId) {
        var payload = new EnrichReturnRegistryPayload(requestId);
        var task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        transactionTemplate.execute(status -> consumer.execute(task));
    }

    private static Order createOrder(long orderId) {
        Order order = new Order();
        order.setId(orderId);
        return order;
    }

    private static List<Order> generateOrders(int size) {
        return IntStream.iterate(0, i -> i < size, i -> i + 1)
                .mapToObj(i -> createOrder(i * 10 + 10))
                .collect(Collectors.toList());
    }

    private void mockCheckouterAnswer() {
        Pager pager = new Pager(2, 1, 3, 20, 1, 1);
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        List<Order> allOrders = generateOrders(2);
        PagedOrders pagedOrders = new PagedOrders(allOrders, pager);
        Mockito.when(checkouterAPI.getOrders(any(RequestClientInfo.class), captor.capture()))
                .thenReturn(pagedOrders);
        when(checkouterAPI.getOrders(any(), any())).thenAnswer(invocation -> {
                OrderItem orderItem = new OrderItem();
                orderItem.setId(333L);
                orderItem.setOfferId("offerid1");
                orderItem.setShopSku("sku3");
                orderItem.setSupplierId(3L);
                ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
                arrayNode.add(mapper.readTree("{\"cis\": \"cis3\", \"sn\": \"sn3\"}"));
                arrayNode.add(mapper.readTree("{\"imei\": \"imei3\", \"UIT\": \"uit3\"}"));
                orderItem.setInstances(arrayNode);

                OrderItem orderItem2 = new OrderItem();
                orderItem2.setId(3333L);
                orderItem2.setOfferId("offerid2");
                orderItem2.setShopSku("sku31");
                orderItem2.setSupplierId(31L);
                ArrayNode arrayNode2 = new ArrayNode(JsonNodeFactory.instance);
                arrayNode2.add(mapper.readTree("{\"UIT\": \"uit33\"}"));
                orderItem2.setInstances(arrayNode2);

                Order order = new Order();
                order.setId(3L);
                order.setItems(List.of(orderItem, orderItem2));

                OrderItem orderItem3 = new OrderItem();
                orderItem3.setId(555L);
                orderItem3.setOfferId("offerid3");
                orderItem3.setShopSku("sku5");
                orderItem3.setSupplierId(5L);
                ArrayNode arrayNode3 = new ArrayNode(JsonNodeFactory.instance);
                arrayNode3.add(mapper.readTree("{\"cis\": \"cis51\", \"UIT\": \"uit51\"}"));
                arrayNode3.add(mapper.readTree("{\"cis\": \"cis52\", \"UIT\": \"uit52\"}"));
                orderItem3.setInstances(arrayNode3);

                OrderItem orderItem4 = new OrderItem();
                orderItem4.setId(5555L);
                orderItem4.setOfferId("orderid4");
                orderItem4.setShopSku("sku55");
                orderItem4.setSupplierId(55L);
                ArrayNode arrayNode4 = new ArrayNode(JsonNodeFactory.instance);
                arrayNode4.add(mapper.readTree("{\"cis\": \"cis551\", \"UIT\": \"uit551\"}"));
                arrayNode4.add(mapper.readTree("{\"cis\": \"cis552\", \"UIT\": \"uit552\"}"));
                orderItem4.setInstances(arrayNode4);

                Order order2 = new Order();
                order2.setId(5L);
                order2.setItems(List.of(orderItem3, orderItem4));

                return new PagedOrders(List.of(order, order2), pager);
        });
    }
}
