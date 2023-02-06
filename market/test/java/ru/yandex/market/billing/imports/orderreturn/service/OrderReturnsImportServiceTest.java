package ru.yandex.market.billing.imports.orderreturn.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtUtilTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link OrderReturnsImportService}
 * <p>
 * Тестовые данные для теста можно брать из yt, сделать replace(`":` -> `", `):
 * SELECT
 * TableRow()
 * FROM hahn.`//home/market/production/checkouter/testing/cdc/checkouter_main/return`
 * LIMIT 100;
 */
class OrderReturnsImportServiceTest extends FunctionalTest {

    private static final String HAHN = "hahn.yt.yandex.net";
    private static final Function<Object, YTreeNode> LONG_NODE = v -> YtUtilTest.longNode(((Integer) v).longValue());
    private static final Function<Object, YTreeNode> INT_NODE = v -> YtUtilTest.intNode((Integer) v);
    private static final Function<Object, YTreeNode> BOOL_NODE = v -> YtUtilTest.booleanNode((Boolean) v);
    private static final Function<Object, YTreeNode> STRING_NODE = v -> YtUtilTest.stringNode((String) v);
    private static final Map<String, Function<Object, YTreeNode>> RETURN_YT_FIELDS = Map.of(
            "id", LONG_NODE,
            "order_id", LONG_NODE,
            "status", INT_NODE,
            "created_at", STRING_NODE,
            "updated_at", STRING_NODE,
            "status_updated_at", STRING_NODE,
            "application_url", STRING_NODE,
            "processing_details", STRING_NODE,
            "fast_return", BOOL_NODE
    );

    @Autowired
    private OrderReturnsImportService orderReturnsImportService;

    private YtCluster ytCluster;

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> orderReturns) {
        return orderReturns.stream()
                .map(orderReturn -> YtUtilTest.treeMapNode(
                                RETURN_YT_FIELDS.entrySet().stream()
                                        .filter(entry -> orderReturn.containsKey(entry.getKey()))
                                        .collect(Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        entry -> entry.getValue().apply(orderReturn.get(entry.getKey()))
                                                )
                                        )
                        )
                ).collect(Collectors.toList());
    }

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importApprovedReturns.after.csv")
    void importApprovedReturns() {
        setYtData(toYtNodes(List.of(
                Map.of(
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                ".net/return-application-695700.pdf",
                        "created_at", "2021-02-25T19:08:53.71163+03",
                        "id", 695700,
                        "order_id", 37345944,
                        "status", 2,
                        "updated_at", "2021-04-08T17:34:23.037181+03",
                        "status_updated_at", "2021-04-08T17:34:23.037181+03",
                        "fast_return", true
                ),
                Map.of(
                        "id", 13926,
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex.net/return-application-13926" +
                                ".pdf",
                        "created_at", "2019-03-27T12:01:03.814+03",
                        "order_id", 4954899,
                        "processing_details", "Return 13926expired",
                        "status", 3,
                        "status_updated_at", "2019-04-12T09:29:48.303+03",
                        "updated_at", "2019-04-12T09:29:48.303+03"
                ),
                Map.of(
                        "id", 18802,
                        "order_id", 6512371,
                        "status", 1,
                        "created_at", "2019-05-16T09:32:56.888+03",
                        "updated_at", "2019-05-28T14:14:23.939+03",
                        "status_updated_at", "2019-05-28T14:14:23.939+03",
                        "processing_details", "Return 18802expired",
                        "fast_return", false
                )
        )));

        orderReturnsImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importLastNotExpired.after.csv")
    void importLastNotExpired() {
        setYtData(toYtNodes(List.of(
                Map.of(
                        "id", 13926,
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex.net/return-application-13926" +
                                ".pdf",
                        "created_at", "2021-06-02T12:01:03.814+03",
                        "order_id", 4954899,
                        "processing_details", "Return 13926expired",
                        "status", 2,
                        "status_updated_at", "2021-06-02T09:29:48.303+03",
                        "updated_at", "2021-06-02T09:29:48.303+03"
                ),
                Map.of(
                        "id", 18802,
                        "order_id", 4954899,
                        "status", 0,
                        "created_at", "2021-06-03T09:32:56.888+03",
                        "updated_at", "2021-06-03T14:14:23.939+03",
                        "status_updated_at", "2021-06-03T14:14:23.939+03",
                        "processing_details", "Return 18802expired",
                        "fast_return", false
                ),
                Map.of(
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                ".net/return-application-695700.pdf",
                        "created_at", "2021-06-04T12:08:53.71163+03",
                        "id", 695700,
                        "order_id", 4954899,
                        "status", 0,
                        "updated_at", "2021-06-04T17:34:23.037181+03",
                        "status_updated_at", "2021-06-04T17:34:23.037181+03",
                        "fast_return", true
                )
        )));

        orderReturnsImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importLastExpired.after.csv")
    void importLastExpired() {
        setYtData(toYtNodes(List.of(
                Map.of(
                        "id", 13926,
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex.net/return-application-13926" +
                                ".pdf",
                        "created_at", "2021-06-02T12:01:03.814+03",
                        "order_id", 4954899,
                        "processing_details", "Return 13926expired",
                        "status", 2,
                        "status_updated_at", "2021-06-02T09:29:48.303+03",
                        "updated_at", "2021-06-02T09:29:48.303+03"
                ),
                Map.of(
                        "id", 18802,
                        "order_id", 4954899,
                        "status", 0,
                        "created_at", "2021-06-03T09:32:56.888+03",
                        "updated_at", "2021-06-03T14:14:23.939+03",
                        "status_updated_at", "2021-06-03T14:14:23.939+03",
                        "processing_details", "Return 18802expired",
                        "fast_return", false
                ),
                Map.of(
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                ".net/return-application-695700.pdf",
                        "created_at", "2021-06-04T12:08:53.71163+03",
                        "id", 695700,
                        "order_id", 4954899,
                        "status", 0,
                        "updated_at", "2021-06-04T17:34:23.037181+03",
                        "status_updated_at", "2021-06-04T17:34:23.037181+03",
                        "fast_return", true
                )
        )));

        orderReturnsImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importTwoOrdersLastNotExpired.after.csv")
    void importTwoOrdersLastNotExpired() {
        setYtData(toYtNodes(List.of(
                Map.of(
                        "id", 13926,
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex.net/return-application-13926" +
                                ".pdf",
                        "created_at", "2021-06-02T12:01:03.814+03",
                        "order_id", 4954800,
                        "processing_details", "Return 13926expired",
                        "status", 0,
                        "status_updated_at", "2021-06-02T09:29:48.303+03",
                        "updated_at", "2021-06-02T09:29:48.303+03"
                ),
                Map.of(
                        "id", 18802,
                        "order_id", 4954899,
                        "status", 0,
                        "created_at", "2021-06-03T09:32:56.888+03",
                        "updated_at", "2021-06-03T14:14:23.939+03",
                        "status_updated_at", "2021-06-03T14:14:23.939+03",
                        "processing_details", "Return 18802expired",
                        "fast_return", false
                ),
                Map.of(
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                ".net/return-application-695700.pdf",
                        "created_at", "2021-06-04T12:08:53.71163+03",
                        "id", 695700,
                        "order_id", 4954899,
                        "status", 0,
                        "updated_at", "2021-06-04T17:34:23.037181+03",
                        "status_updated_at", "2021-06-04T17:34:23.037181+03",
                        "fast_return", true
                )
        )));

        orderReturnsImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(
            before = "ImportOrderReturnsServiceTest.updateStoredReturns.before.csv",
            after = "ImportOrderReturnsServiceTest.updateStoredReturns.after.csv")
    void updateStoredReturns() {
        setYtData(toYtNodes(List.of(
                Map.of(
                        "id", 13926,
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex.net/return-application-13926" +
                                ".pdf",
                        "created_at", "2021-06-02T12:01:03.814+03",
                        "order_id", 4954800,
                        "processing_details", "Return 13926expired",
                        "status", 2,
                        "status_updated_at", "2021-06-02T09:29:48.303+03",
                        "updated_at", "2021-06-02T09:29:48.303+03"
                ),
                Map.of(
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                ".net/return-application-695700.pdf",
                        "created_at", "2021-06-04T12:08:53.71163+03",
                        "id", 695700,
                        "order_id", 4954899,
                        "status", 0,
                        "updated_at", "2021-06-04T17:34:23.037181+03",
                        "status_updated_at", "2021-06-04T17:34:23.037181+03",
                        "fast_return", true
                ),
                Map.of(
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                ".net/return-application-695700.pdf",
                        "created_at", "2021-06-04T12:08:53.71163+03",
                        "id", 695702,
                        "order_id", 4954898,
                        "status", 0,
                        "updated_at", "2021-06-04T17:34:23.037181+03",
                        "status_updated_at", "2021-06-04T17:34:23.037181+03",
                        "fast_return", true
                )
        )));

        orderReturnsImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(
            before = "ImportOrderReturnsServiceTest.insertTrantimes.before.csv",
            after = "ImportOrderReturnsServiceTest.insertTrantimes.after.csv")
    void insertTrantimes() {
        setYtData(toYtNodes(List.of(
                Map.of(
                        "id", 13926,
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex.net/return-application-13926" +
                                ".pdf",
                        "created_at", "2021-06-02T12:01:03.814+03",
                        "order_id", 4954800,
                        "processing_details", "Return 13926expired",
                        "status", 2,
                        "status_updated_at", "2021-06-02T09:29:48.303+03",
                        "updated_at", "2021-06-02T09:29:48.303+03"
                ),
                Map.of(
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                ".net/return-application-695700.pdf",
                        "created_at", "2021-06-04T12:08:53.71163+03",
                        "id", 695700,
                        "order_id", 4954899,
                        "status", 2,
                        "updated_at", "2021-06-04T17:34:23.037181+03",
                        "status_updated_at", "2021-06-04T17:34:23.037181+03",
                        "fast_return", true
                ),
                Map.of(
                        "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                ".net/return-application-695700.pdf",
                        "created_at", "2021-06-04T12:08:53.71163+03",
                        "id", 695702,
                        "order_id", 4954898,
                        "status", 0,
                        "updated_at", "2021-06-04T17:34:23.037181+03",
                        "status_updated_at", "2021-06-04T17:34:23.037181+03",
                        "fast_return", true
                )
        )));

        orderReturnsImportService.runAtCluster(ytCluster);
    }

    void setYtData(List<YTreeMapNode> ytData) {
        final Yt hahn = YtUtilTest.mockYt(ytData);
        ytCluster = new YtCluster(HAHN, hahn);
    }
}
