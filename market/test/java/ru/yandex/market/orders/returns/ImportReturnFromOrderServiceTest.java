package ru.yandex.market.orders.returns;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

import static org.mockito.Mockito.when;

class ImportReturnFromOrderServiceTest extends FunctionalTest {
    @Autowired
    private YtHttpFactory ytHttpFactory;

    @Autowired
    private ImportReturnsFromOrderServiceExecutor importReturnsFromOrderServiceExecutor;

    private static final String SENECA = "seneca-vla.yt.yandex.net";
    private static final Object NULL = new Object();

    private YtCluster ytCluster;

    private static final Function<Object, YTreeNode> LONG_NODE = v -> v == NULL ? YtUtilTest.nullNode() :
            YtUtilTest.longNode((Long) (v));
    private static final Function<Object, YTreeNode> STRING_NODE = v -> v == NULL ? YtUtilTest.nullNode() :
            YtUtilTest.stringNode((String) v);

    private static final Map<String, Function<Object, YTreeNode>> deliveryReturnsYtFields = Map.of(
            "checkouterReturnId", LONG_NODE,
            "returnShipmentRecipientType", STRING_NODE,
            "logisticReturnLineStatus", STRING_NODE,
            "logisticReturnLineStatusCommittedAt", LONG_NODE,
            "pickupLogisticPointId", LONG_NODE
    );

    @Test
    @DbUnitDataSet(
            before = "ImportReturnsFromOrderServiceTest.before.csv")
    void updateDeliveryReturns() {
        var dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        setYtData(toYtNodes(List.of(
                Map.of(
                        "checkouterReturnId", 88L,
                        "returnShipmentRecipientType", "SHOP",
                        "logisticReturnLineStatus", "READY_FOR_PICKUP",
                        "logisticReturnLineStatusCommittedAt",
                        Instant.from(dateFormatter.parse("2021-05-26T07:00:00.000+03:00")).toEpochMilli(),
                        "pickupLogisticPointId", 10001L
                ),
                Map.of(
                        "checkouterReturnId", 89L,
                        "returnShipmentRecipientType", "SHOP",
                        "logisticReturnLineStatus", "IN_TRANSIT",
                        "logisticReturnLineStatusCommittedAt",
                        Instant.from(dateFormatter.parse("2021-05-26T20:01:22.901+03:00")).toEpochMilli(),
                        "pickupLogisticPointId", 10001L
                ),
                Map.of(
                        "checkouterReturnId", 90L,
                        "returnShipmentRecipientType", "SHOP",
                        "logisticReturnLineStatus", "PICKED",
                        "logisticReturnLineStatusCommittedAt",
                        Instant.from(dateFormatter.parse("2021-05-26T20:01:22.901+03:00")).toEpochMilli(),
                        "pickupLogisticPointId", 10001L
                ),
                Map.of(
                        "checkouterReturnId", 91L,
                        "returnShipmentRecipientType", "SHOP",
                        "logisticReturnLineStatus", "IN_TRANSIT",
                        "logisticReturnLineStatusCommittedAt",
                        Instant.from(dateFormatter.parse("2021-05-26T20:01:22.901+03:00")).toEpochMilli(),
                        "pickupLogisticPointId", 10001L
                ),
                Map.of(
                        "checkouterReturnId", 92L,
                        "returnShipmentRecipientType", "SHOP",
                        "logisticReturnLineStatus", NULL,
                        "logisticReturnLineStatusCommittedAt", NULL,
                        "pickupLogisticPointId", NULL
                ),
                Map.of(
                        "checkouterReturnId", 93L,
                        "returnShipmentRecipientType", "SHOP",
                        "logisticReturnLineStatus", "IN_TRANSIT",
                        "logisticReturnLineStatusCommittedAt",
                        Instant.from(dateFormatter.parse("2021-05-26T20:01:22.901+03:00")).toEpochMilli(),
                        "pickupLogisticPointId", 10001L
                )
        )));

        importReturnsFromOrderServiceExecutor.runImport(ytCluster);
    }

    void setYtData(List<YTreeMapNode> ytData) {
        final Yt seneca = YtUtilTest.mockYt(ytData);
        when(ytHttpFactory.getYt(SENECA)).thenReturn(seneca);

        ytCluster = new YtCluster(SENECA, seneca);
    }

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> orderReturns) {
        return orderReturns.stream()
                .map(orderReturn -> YtUtilTest.treeMapNode(
                                deliveryReturnsYtFields.entrySet().stream()
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
