package ru.yandex.market.billing.imports.orderservicereturn;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtUtilTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class OrderServiceReturnServiceTest extends FunctionalTest {
    private static final ZoneId LOCAL_ZONE_ID = TimeZone.getDefault().toZoneId();
    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final LocalDate DATE_2022_03_07 = LocalDate.of(2022, 3, 7);
    private static final Object NULL = new Object();
    private static final String SENECA_VLA = "seneca-vla.yt.yandex.net";
    private static final Function<Object, YTreeNode> LONG_NODE =
            v -> v == NULL ? YtUtilTest.nullNode() : YtUtilTest.longNode(((Long) (v)));
    private static final Function<Object, YTreeNode> STRING_NODE = v -> YtUtilTest.stringNode((String) v);
    private static final Map<String, Function<Object, YTreeNode>> RETURN_LINE_YT_FIELDS = Map.of(
            "logisticReturnId", LONG_NODE,
            "orderId", LONG_NODE,
            "checkouterReturnId", LONG_NODE,
            "partnerId", LONG_NODE,
            "returnType", STRING_NODE,
            "logisticReturnLineEventId", LONG_NODE,
            "logisticReturnLineStatus", STRING_NODE,
            "logisticReturnLineStatusCommittedAt", LONG_NODE,
            "updatedAt", LONG_NODE
    );

    @Value("${mbi.billing.returns.yt.os.return.table.path}")
    private String orderServiceReturnYtPath;

    @Autowired
    private OrderServiceReturnService orderServiceReturnService;

    private YtCluster ytCluster;

    @BeforeAll
    static void beforeAll() {
        TimeZone.setDefault(DEFAULT_TIME_ZONE);
    }

    @AfterAll
    static void afterAll() {
        TimeZone.setDefault(TimeZone.getTimeZone(LOCAL_ZONE_ID));
    }

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> returns) {
        return returns.stream()
                .map(returnLine -> YtUtilTest.treeMapNode(
                                RETURN_LINE_YT_FIELDS.entrySet().stream()
                                        .filter(entry -> returnLine.containsKey(entry.getKey()))
                                        .collect(Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        entry -> entry.getValue().apply(returnLine.get(entry.getKey()))
                                                )
                                        )
                        )
                ).collect(Collectors.toList());
    }


    @DbUnitDataSet(
            after = "OrderServiceReturnServiceTest.testImport.after.csv"
    )
    @DisplayName("Проверяем, что импортируются и не конфликтуют со старыми данными возвраты/невыкупы из Order Service")
    @Test
    void testImportReturnFromOrderService() {
        setYtData(getYtReturns());
        orderServiceReturnService.runReturnImport(
                ytCluster,
                getYPath(orderServiceReturnYtPath, DATE_2022_03_07)
        );
    }

    private List<YTreeMapNode> getYtReturns() {
        return toYtNodes(List.of(
                        Map.of(
                                "logisticReturnId", 145437L,
                                "orderId", 561785L,
                                "checkouterReturnId", 1760972L,
                                "partnerId", 2L,
                                "returnType", "UNREDEEMED",
                                "logisticReturnLineEventId", 1225L,
                                "logisticReturnLineStatus", "IN_TRANSIT",
                                "logisticReturnLineStatusCommittedAt", 1646462185108L,
                                "updatedAt", 1646462189308L
                        ),
                        Map.of(
                                "logisticReturnId", 145434L,
                                "orderId", 561782L,
                                "checkouterReturnId", 1760969L,
                                "partnerId", 2L,
                                "returnType", "RETURN",
                                "logisticReturnLineEventId", 1210L,
                                "logisticReturnLineStatus", "IN_TRANSIT",
                                "logisticReturnLineStatusCommittedAt", 1646465185108L,
                                "updatedAt", 1646465185308L
                        ),
                        Map.of(
                                "logisticReturnId", 145434L,
                                "orderId", 561782L,
                                "checkouterReturnId", 1760969L,
                                "partnerId", 2L,
                                "returnType", "RETURN",
                                "logisticReturnLineEventId", 1213L,
                                "logisticReturnLineStatus", "READY_FOR_PICKUP",
                                "logisticReturnLineStatusCommittedAt", 1646468287108L,
                                "updatedAt", 1646468287108L
                        )
                )
        );
    }

    private void setYtData(List<YTreeMapNode> ytData) {
        final Yt hahn = YtUtilTest.mockYt(ytData);
        ytCluster = new YtCluster(SENECA_VLA, hahn);
    }

    private YPath getYPath(String path, LocalDate importDate) {
        return YPath.simple(path + "_" + importDate);
    }
}
