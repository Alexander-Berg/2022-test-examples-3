package ru.yandex.market.orders.returns;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtUtilTest;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ImportOrderReturnItemsService}
 * <p>
 * Тестовые данные для теста можно брать из yt, сделать replace(`":` -> `", `):
 * SELECT
 * TableRow()
 * FROM hahn.`//home/market/production/checkouter/testing/cdc/checkouter_main/return_item`
 * LIMIT 100;
 */
class ImportOrderReturnItemsServiceTest extends FunctionalTest {
    private static final Object NULL = new Object();

    @Autowired
    private YtHttpFactory ytHttpFactory;

    @Autowired
    private ImportOrderReturnItemsService importOrderReturnItemsService;

    private static final String HAHN = "hahn.yt.yandex.net";

    private YtCluster ytCluster;

    private static final Function<Object, YTreeNode> LONG_NODE =
            v -> v == NULL ? YtUtilTest.nullNode() : YtUtilTest.longNode(((Integer) (v)).longValue());
    private static final Function<Object, YTreeNode> INT_NODE =
            v -> v == NULL ? YtUtilTest.nullNode() : YtUtilTest.intNode((Integer) v);
    private static final Function<Object, YTreeNode> STRING_NODE = v -> YtUtilTest.stringNode((String) v);
    private static final Function<Object, YTreeNode> DOUBLE_NODE = v -> YtUtilTest.floatNode(((Double) v).floatValue());
    private static final Function<Object, YTreeNode> STRING_LIST_NODE =
            v -> YtUtilTest.treeListNode(((List<String>) v).stream()
                    .map(YtUtilTest::stringNode)
                    .collect(Collectors.toList()));

    private static final Map<String, Function<Object, YTreeNode>> returnItemYtFields = Map.of(
            "id", LONG_NODE,
            "return_id", LONG_NODE,
            "order_id", LONG_NODE,
            "item_id", LONG_NODE,
            "count", INT_NODE,
            "supplier_compensation", DOUBLE_NODE,
            "return_reason", STRING_NODE,
            "reason_type", INT_NODE,
            "subreason_type", INT_NODE,
            "pictures_urls", STRING_LIST_NODE
    );

    @Test
    @DbUnitDataSet(before = "ImportOrderReturnItemsServiceTest.before.csv",
            after = "ImportOrderReturnItemsServiceTest.after.csv")
    void testImport() {
        setYtData(getYtReturnItems());

        importOrderReturnItemsService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(before = "ImportOrderReturnItemsServiceTest.before.csv",
            after = "ImportOrderReturnItemsServiceTest.after.csv")
    void testImportTwice() {
        setYtData(getYtReturnItems());

        importOrderReturnItemsService.runAtCluster(ytCluster);
        importOrderReturnItemsService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(before = "ImportOrderReturnItemsServiceTest.before.csv")
    void testImportLongReturnReason() {
        setYtData(toYtNodes(List.of(
                Map.of(
                        "id", 28640,
                        "count", 10,
                        "item_id", 9568212,
                        "order_id", 6512371,
                        "reason_type", 0,
                        "return_id", 18802,
                        "return_reason", Stream.generate(() -> "S").limit(5000).collect(Collectors.joining()),
                        "supplier_compensation", 777.0,
                        "pictures_urls", List.of("single url")
                ))));

        assertDoesNotThrow(() -> importOrderReturnItemsService.runAtCluster(ytCluster));
    }

    private List<YTreeMapNode> getYtReturnItems() {
        return toYtNodes(List.of(
                Map.of(
                        "id", 19356,
                        "count", 2,
                        "item_id", 6296828,
                        "order_id", 4954899,
                        "reason_type", 0,
                        "return_id", 13926,
                        "return_reason", "Разбитый",
                        "supplier_compensation", 1111.0
                ),
                Map.of(
                        "id", 28640,
                        "count", 10,
                        "item_id", 9568212,
                        "order_id", 6512371,
                        "reason_type", 0,
                        "return_id", 18802,
                        "return_reason", "Главный недостаток в клавиатуре, а именно в клавише Пробел. Не всегда нет " +
                                "отклик при нажатии по центру клавиши, по бокам клавиши вообще отклика нет, из-за " +
                                "этой проблемы нет возможности быстро печатать нужно продавливать клавишу.",
                        "supplier_compensation", 777.0,
                        "pictures_urls", List.of("single url")
                ),
                Map.of(
                        "id", 695701,
                        "return_id", 695700,
                        "order_id", 37345944,
                        "item_id", 72450004,
                        "count", 1,
                        "return_reason", "Ошиблась и заказала неправильное средство, которое дублирует мусс Кора из " +
                                "заказа. Поэтому не открывала и хочу оформить возврат.",
                        "reason_type", 1,
                        "subreason_type", 1,
                        "pictures_urls", List.of(
                                "https://avatars.mds.yandex" +
                                        ".net/get-market-ugc/2369747/2a00000177d9f02b8821e14022acbe875dff/orig",
                                "https://avatars.mds.yandex" +
                                        ".net/get-market-ugc/474703/2a00000177d9f0a15a9b21259b8e6a01bf1f/orig",
                                "https://avatars.mds.yandex" +
                                        ".net/get-market-ugc/200370/2a00000177d9f0be1e7cbb5cf0ce92fa7403/orig",
                                "https://avatars.mds.yandex" +
                                        ".net/get-market-ugc/2369747/2a00000177d9f0ed775315fd02d236101f73/orig",
                                "https://avatars.mds.yandex" +
                                        ".net/get-market-ugc/2369747/2a00000177d9f14b14cf6344f8eba423f661/orig"
                        )
                ),
                Map.of(
                        "count", 13,
                        "id", 695703,
                        "item_id", 73103855,
                        "order_id", 37698825,
                        "pictures_urls", List.of(),
                        "reason_type", 1,
                        "subreason_type", 2,
                        "return_id", 695702,
                        "return_reason", "Перепутали, нужен с розовыми ушками"
                ),
                Map.of(
                        "item_id", NULL,
                        "id", 0xdead,
                        "count", NULL,
                        "order_id", 37698825,
                        "pictures_urls", List.of(),
                        "reason_type", NULL,
                        "subreason_type", NULL,
                        "return_id", 1000,
                        "return_reason", "Перепутали, нужен с розовыми ушками"
                )));
    }

    void setYtData(List<YTreeMapNode> ytData) {
        final Yt hahn = YtUtilTest.mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        ytCluster = new YtCluster(HAHN, hahn);
    }

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> orderReturns) {
        return orderReturns.stream()
                .map(orderReturnItem -> YtUtilTest.treeMapNode(
                        returnItemYtFields.entrySet().stream()
                                .filter(entry -> orderReturnItem.containsKey(entry.getKey()))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().apply(orderReturnItem.get(entry.getKey()))
                                        )
                                )
                        )
                ).collect(Collectors.toList());
    }
}
