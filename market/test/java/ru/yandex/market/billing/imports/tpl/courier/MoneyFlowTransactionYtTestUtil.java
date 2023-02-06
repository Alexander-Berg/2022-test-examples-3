package ru.yandex.market.billing.imports.tpl.courier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtUtilTest;

import static org.mockito.Mockito.when;

/**
 * Утильный класс для мока Yt-таблицы для УВ курьерки
 */
@ParametersAreNonnullByDefault
public class MoneyFlowTransactionYtTestUtil {

    private static final Map<String, Function<Object, YTreeNode>> YT_TABLE_SCHEMA = Map.ofEntries(
            Map.entry("id", YtUtilTest::longNode),
            Map.entry("userShiftId", YtUtilTest::nullableLongNode),
            Map.entry("transactionType", YtUtilTest::stringNode),
            Map.entry("eventTime", YtUtilTest::stringNode),
            Map.entry("trantime", YtUtilTest::stringNode),
            Map.entry("partnerId", YtUtilTest::longNode),
            Map.entry("paymentType", YtUtilTest::stringNode),
            Map.entry("productType", YtUtilTest::stringNode),
            Map.entry("amount", YtUtilTest::longNode),
            Map.entry("currency", YtUtilTest::stringNode),
            Map.entry("isCorrection", YtUtilTest::booleanNode),
            Map.entry("orgId", YtUtilTest::longNode),
            Map.entry("userType", YtUtilTest::stringNode),
            Map.entry("clientId", YtUtilTest::stringNode),
            Map.entry("contractId", YtUtilTest::stringNode),
            Map.entry("personId", YtUtilTest::stringNode)
    );

    private MoneyFlowTransactionYtTestUtil() {
    }

    public static void mockYt(YtCluster cluster, Yt yt, List<Map<String, Object>> ytData) {
        when(cluster.getYt()).thenReturn(yt);
        setYtData(ytData, yt);
    }

    private static void setYtData(List<Map<String, Object>> ytData, Yt yt) {
        YtUtilTest.mockYt(YtUtilTest.toYtNodes(ytData, YT_TABLE_SCHEMA), yt);
    }
}
