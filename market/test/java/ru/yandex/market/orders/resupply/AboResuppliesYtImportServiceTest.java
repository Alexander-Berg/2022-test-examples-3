package ru.yandex.market.orders.resupply;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link AboResuppliesYtImportService}
 */
@DbUnitDataSet(before = "AboResuppliesYtImportServiceTest.before.csv")
class AboResuppliesYtImportServiceTest extends FunctionalTest {

    private static final String HAHN = "hahn.yt.yandex.net";

    @Autowired
    private YtHttpFactory ytHttpFactory;

    private YtCluster ytCluster;

    @Autowired
    private AboResuppliesYtImportService aboResuppliesYtImportService;

    private static YTreeMapNode buildAboResupplyMapNode(
            long id,
            long orderId,
            long orderItemId,
            long supplierId,
            long count,
            String createdAt,
            String stock,
            boolean delivered,
            String warehouse,
            List<String> attributes,
            long requestId,
            Long warehouseId
    ) {
        Map<String, YTreeNode> params = new HashMap<>();
        params.put("id", YtUtilTest.longNode(id));
        params.put("order_id", YtUtilTest.longNode(orderId));
        params.put("order_item_id", YtUtilTest.longNode(orderItemId));
        params.put("supplier_id", YtUtilTest.longNode(supplierId));
        params.put("count", YtUtilTest.longNode(count));
        params.put("created_at", YtUtilTest.stringNode(createdAt));
        params.put("stock", YtUtilTest.stringNode(stock));
        params.put("delivered", YtUtilTest.booleanNode(delivered));
        params.put("warehouse", YtUtilTest.stringNode(warehouse));
        params.put("attributes", YtUtilTest.treeListNode(attributes.stream()
                .map(YtUtilTest::stringNode)
                .collect(Collectors.toList())));
        params.put("ff_request_id", YtUtilTest.longNode(requestId));
        params.put("warehouse_id", warehouseId == null ? YtUtilTest.nullNode() : YtUtilTest.longNode(warehouseId));
        return YtUtilTest.treeMapNode(params);
    }

    void setUp(List<YTreeMapNode> ytData) {
        final Yt hahn = YtUtilTest.mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        ytCluster = new YtCluster(HAHN, hahn);
    }

    @Test
    @DbUnitDataSet(after = "AboResuppliesYtImportServiceTest.after.csv")
    void testImport() {
        setUp(List.of(
                buildAboResupplyMapNode(
                        1,
                        1,
                        1,
                        1,
                        1,
                        "2019-12-27 14:17:20.30320",
                        "GOOD",
                        true,
                        "TOMILINO",
                        List.of("SCRATCHES", "DEFORMED"),
                        777,
                        171L
                ),
                buildAboResupplyMapNode(
                        2,
                        2,
                        2,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "BAD_3P",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                )
        ));

        aboResuppliesYtImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(after = "AboResuppliesYtImportServiceTest.after.csv")
    void testReimport() {
        setUp(List.of(
                buildAboResupplyMapNode(
                        2,
                        1,
                        1,
                        1,
                        1,
                        "2019-12-27 14:17:20.30320",
                        "GOOD",
                        true,
                        "TOMILINO",
                        List.of("SCRATCHES", "DEFORMED"),
                        777,
                        171L
                ),
                buildAboResupplyMapNode(
                        1,
                        2,
                        2,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "BAD_3P",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                )
        ));

        aboResuppliesYtImportService.runAtCluster(ytCluster);

        setUp(List.of(
                buildAboResupplyMapNode(
                        1,
                        1,
                        1,
                        1,
                        1,
                        "2019-12-27 14:17:20.30320",
                        "GOOD",
                        true,
                        "TOMILINO",
                        List.of("SCRATCHES", "DEFORMED"),
                        777,
                        171L
                ),
                buildAboResupplyMapNode(
                        2,
                        2,
                        2,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "BAD_3P",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                )
        ));
        aboResuppliesYtImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(
            before = "AboResuppliesYtImportServiceTest.jobFailEnabled.before.csv",
            after = "AboResuppliesYtImportServiceTest.after.csv")
    void testImportIncorrectJobFailEnabled() {
        setUp(List.of(
                buildAboResupplyMapNode(
                        1,
                        1,
                        1,
                        1,
                        1,
                        "2019-12-27 14:17:20.30320",
                        "GOOD",
                        true,
                        "TOMILINO",
                        List.of("SCRATCHES", "DEFORMED"),
                        777,
                        171L
                ),
                buildAboResupplyMapNode(
                        2,
                        2,
                        2,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "BAD_3P",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                ),
                buildAboResupplyMapNode(
                        3,
                        100500,
                        2,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "INCORRECT orderId",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                ),
                buildAboResupplyMapNode(
                        4,
                        2,
                        100500,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "INCORRECT orderItemId",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                ),
                buildAboResupplyMapNode(
                        5,
                        2,
                        2,
                        100500,
                        1,
                        "2019-12-27 14:17:20.303",
                        "INCORRECT supplierId",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                )
        ));

        assertThrows(IllegalStateException.class, () -> aboResuppliesYtImportService.runAtCluster(ytCluster));
    }

    @Test
    @DbUnitDataSet(after = "AboResuppliesYtImportServiceTest.after.csv")
    void testImportIncorrectJobFailDisabled() {
        setUp(List.of(
                buildAboResupplyMapNode(
                        1,
                        1,
                        1,
                        1,
                        1,
                        "2019-12-27 14:17:20.30320",
                        "GOOD",
                        true,
                        "TOMILINO",
                        List.of("SCRATCHES", "DEFORMED"),
                        777,
                        171L
                ),
                buildAboResupplyMapNode(
                        2,
                        2,
                        2,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "BAD_3P",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                ),
                buildAboResupplyMapNode(
                        3,
                        100500,
                        2,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "INCORRECT orderId",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                ),
                buildAboResupplyMapNode(
                        4,
                        2,
                        100500,
                        1,
                        1,
                        "2019-12-27 14:17:20.303",
                        "INCORRECT orderItemId",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                ),
                buildAboResupplyMapNode(
                        5,
                        2,
                        2,
                        100500,
                        1,
                        "2019-12-27 14:17:20.303",
                        "INCORRECT supplierId",
                        false,
                        "SOME UNKNOWN WAREHOUSE",
                        List.of(),
                        777,
                        null
                )
        ));

        aboResuppliesYtImportService.runAtCluster(ytCluster);
    }
}
