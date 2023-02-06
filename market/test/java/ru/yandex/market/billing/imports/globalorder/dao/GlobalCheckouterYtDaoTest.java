package ru.yandex.market.billing.imports.globalorder.dao;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.common.YtErrorMapping;
import ru.yandex.inside.yt.kosher.impl.common.YtServiceUnavailableException;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtTemplate;
import ru.yandex.market.yt.YtClusterStub;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultCheckouterOrder;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultCheckouterOrderItem;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.toYTreeMapNode;

@ParametersAreNonnullByDefault
class GlobalCheckouterYtDaoTest extends FunctionalTest {

    private static final String TEST_DATE = "2021-11-11";
    private static final String ORDER_TABLES_DIR = "//global/order/";
    private static final String ORDER_ITEM_TABLES_DIR = "//global/order-item/";

    private YtClusterStub hahn = new YtClusterStub("hahn");
    private YtClusterStub arnold = new YtClusterStub("arnold");

    private GlobalCheckouterYtDao ytDao;

    @BeforeEach
    void setUp() {
        ytDao = new GlobalCheckouterYtDao(
                new YtTemplate(new YtCluster[]{hahn, arnold}),
                ORDER_TABLES_DIR, ORDER_ITEM_TABLES_DIR
        );
    }

    @Test
    void getOrders() {
        var date = LocalDate.parse(TEST_DATE);
        var orders = List.of(
                defaultCheckouterOrder(4)
                        .setPaymentId(4001L)
                        .setSubsidyPaymentId(4002L)
                        .build(),
                defaultCheckouterOrder(7)
                        .setFreeDeliveryForShop(true)
                        .build()
        );

        hahn.addTable(ORDER_TABLES_DIR + TEST_DATE,
                StreamEx.of(orders).map(order -> toYTreeMapNode(order, date)).toList());

        var importedOrders = ytDao.getOrders(date);

        assertThat(orders).usingRecursiveFieldByFieldElementComparator()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(importedOrders);
    }

    @Test
    void getOrders_whenHahnUnavailable() {
        var order = defaultCheckouterOrder(4).build();
        var date = LocalDate.parse(TEST_DATE);

        // Хан недоступен, например
        hahn.simulateReadTableError(
                ORDER_TABLES_DIR + TEST_DATE,
                new YtServiceUnavailableException("service unavailable", YtErrorMapping.exceptionFor(503))
        );
        // Но есть Арнольд!
        arnold.addTable(ORDER_TABLES_DIR + TEST_DATE, List.of(toYTreeMapNode(order, date)));

        var orders = ytDao.getOrders(date);

        assertThat(orders).usingRecursiveFieldByFieldElementComparator()
                .hasSize(1)
                .contains(order);
    }

    @Test
    void getOrders_whenThereAreNoOrders() {
        var date = LocalDate.parse(TEST_DATE);

        hahn.addTable(ORDER_TABLES_DIR + TEST_DATE, List.of());

        var orders = ytDao.getOrders(date);

        assertThat(orders).isEmpty();
    }

    @Test
    void getOrderItems() {
        var date = LocalDate.parse(TEST_DATE);
        var order = defaultCheckouterOrder(4).build();
        var items = List.of(
                defaultCheckouterOrderItem(6, order.getId()).build(),
                defaultCheckouterOrderItem(7, order.getId()).build()
        );

        hahn.addTable(
                ORDER_ITEM_TABLES_DIR + TEST_DATE,
                StreamEx.of(items).map(item -> toYTreeMapNode(item, date)).toList()
        );

        var fetchedItems = ytDao.getOrderItems(date);

        assertThat(fetchedItems).usingRecursiveFieldByFieldElementComparator()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(items);
    }

}
