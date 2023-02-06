package ru.yandex.market.crm.core.test.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderHistory;
import ru.yandex.market.crm.platform.models.OrderItem;
import ru.yandex.market.crm.yt.client.YtClient;

/**
 * @author apershukov
 */
@Component
public class OrderFactsTestHelper {
    public static YTreeMapNode order(String email, long puid) {
        return order(
                Uids.create(UidType.EMAIL, email),
                Uids.create(UidType.PUID, puid)
        );
    }

    public static YTreeMapNode order(Uid... uids) {
        return order(12345, "", 100500, uids);
    }

    public static YTreeMapNode order(long orderId, String multiOrderId, long totalPrice, Uid... uids) {
        OrderItem orderItem = orderItem(91491, 111, totalPrice, 1);
        return order(orderId, multiOrderId, List.of(orderItem), uids);
    }

    public static YTreeMapNode order(long orderId, String multiOrderId, List<OrderItem> items, Uid... uids) {
        long creationTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
                .minusDays(5)
                .toInstant()
                .toEpochMilli();
        long totalPrice = items.stream().mapToLong(OrderItem::getPrice).sum();

        Order.Builder orderBuilder = Order.newBuilder()
                .setId(orderId)
                .setStatus("DELIVERY")
                .setCreationDate(creationTime)
                .setRgb(RGBType.BLUE)
                .addAllUid(Arrays.asList(uids))
                .setMultiOrderId(multiOrderId)
                .setTotal(totalPrice)
                .addHistory(
                        OrderHistory.newBuilder()
                                .setType("NEW_ORDER")
                                .setTimestamp(creationTime)
                );
        items.forEach(orderBuilder::addItems);

        return YTree.mapBuilder()
                .key("id").value("3")
                .key("id_type").value("puid")
                .key("timestamp").value(creationTime)
                .key("fact_id").value(String.valueOf(orderId))
                .key("fact").value(new YTreeStringNodeImpl(
                        orderBuilder.build().toByteArray(),
                        null
                ))
                .buildMap();
    }

    public static OrderItem orderItem(int hid, long modelId, long price, int count) {
        return orderItem(hid, modelId, String.valueOf(modelId), price, count);
    }

    public static OrderItem orderItem(int hid, long modelId, String sku, long price, int count) {
        return OrderItem.newBuilder()
                .setHid(hid)
                .setModelId(modelId)
                .setSku(sku)
                .setPrice(price)
                .setCount(count)
                .build();
    }

    private final YtSchemaTestHelper schemaTestHelper;
    private final YtClient ytClient;

    private final String orderFacts;

    public OrderFactsTestHelper(YtSchemaTestHelper schemaTestHelper,
                                YtClient ytClient,
                                @Value("${var.platform_order}") String orderFacts) {
        this.schemaTestHelper = schemaTestHelper;
        this.ytClient = ytClient;
        this.orderFacts = orderFacts;
    }

    public void prepareOrders(YTreeMapNode... rows) {
        schemaTestHelper.prepareOrderFactsTable(orderFacts);

        ytClient.write(
                YPath.simple(orderFacts),
                List.of(rows)
        );
    }
}
