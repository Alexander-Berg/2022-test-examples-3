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

class ImportOrderReturnPickupInfoServiceTest extends FunctionalTest {
    @Autowired
    private ImportOrderReturnPickupInfoService importOrderReturnPickupInfoService;

    private static final String HAHN = "hahn.yt.yandex.net";
    private static final Object NULL = new Object();

    private YtCluster ytCluster;

    private static final Function<Object, YTreeNode> LONG_NODE = v -> v == NULL ? YtUtilTest.nullNode() :
            YtUtilTest.longNode(((Integer) (v)).longValue());
    private static final Function<Object, YTreeNode> STRING_NODE = v -> v == NULL ? YtUtilTest.nullNode() :
            YtUtilTest.stringNode((String) v);

    private static final Map<String, Function<Object, YTreeNode>> ORDER_RETURN_DELIVERY_YT_FIELDS = Map.of(
            "return_id", STRING_NODE,
            "external_order_id", LONG_NODE,
            "delivery_status", STRING_NODE,
            "delivery_status_updated_at", STRING_NODE,
            "destination_sc_logistic_point", LONG_NODE,
            "destination_sc_partner", LONG_NODE
    );

    @Test
    @DbUnitDataSet(before = "ImportOrderReturnPickupInfoServiceTest.before.csv",
            after = "ImportOrderReturnPickupInfoServiceTest.after.csv")
    void updateOrdersReturnDeliveryInfo() {
        setYtData(toYtNodes(List.of(
                Map.of(
                        "return_id", "88",
                        "external_order_id", 1613,
                        "delivery_status", "READY_FOR_PICKUP",
                        "delivery_status_updated_at", "2021-05-26T00:00:00.000+03:00",
                        "destination_sc_logistic_point", 10001,
                        "destination_sc_partner", 73555
                ),
                Map.of(
                        "return_id", "89",
                        "external_order_id", 1613,
                        "delivery_status", NULL,
                        "delivery_status_updated_at", NULL,
                        "destination_sc_logistic_point", NULL,
                        "destination_sc_partner", NULL
                ),
                Map.of(
                        "return_id", "90",
                        "external_order_id", 1614,
                        "delivery_status", "DELIVERED",
                        "delivery_status_updated_at", "2021-05-26T20:01:22.901+03:00",
                        "destination_sc_logistic_point", 10001,
                        "destination_sc_partner", 73555
                ),
                Map.of(
                        "return_id", "91",
                        "external_order_id", 1615,
                        "delivery_status", "DELIVERY",
                        "delivery_status_updated_at", "2021-05-26T20:01:22.901+03:00",
                        "destination_sc_logistic_point", 10001,
                        "destination_sc_partner", 73555
                ),
                Map.of(
                        "return_id", "92",
                        "external_order_id", 1616,
                        "delivery_status", NULL,
                        "delivery_status_updated_at", NULL,
                        "destination_sc_logistic_point", NULL,
                        "destination_sc_partner", NULL
                ),
                Map.of(
                        "return_id", "93",
                        "external_order_id", 1617,
                        "delivery_status", "DELIVERY",
                        "delivery_status_updated_at", "2021-05-26T20:01:22.901+03:00",
                        "destination_sc_logistic_point", 10001,
                        "destination_sc_partner", 73555
                )
        )));

        importOrderReturnPickupInfoService.runAtCluster(ytCluster);
    }

    void setYtData(List<YTreeMapNode> ytData) {
        final Yt hahn = YtUtilTest.mockYt(ytData);
        ytCluster = new YtCluster(HAHN, hahn);
    }

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> orderReturns) {
        return orderReturns.stream()
                .map(orderReturn -> YtUtilTest.treeMapNode(
                                ORDER_RETURN_DELIVERY_YT_FIELDS.entrySet().stream()
                                        .filter(entry -> orderReturn.containsKey(entry.getKey()))
                                        .collect(Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        entry -> entry.getValue().apply(orderReturn.get(entry.getKey()))
                                                )
                                        )
                        )
                ).collect(Collectors.toList());
    }
}
