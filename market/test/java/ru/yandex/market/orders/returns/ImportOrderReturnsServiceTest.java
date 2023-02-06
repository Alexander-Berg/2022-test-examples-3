package ru.yandex.market.orders.returns;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.returns.OrderReturnDao;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.YtUtilTest;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.yt.YtUtilTest.longNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.stringNode;

/**
 * Тесты для {@link ImportOrderReturnsService}
 * <p>
 * Тестовые данные для теста можно брать из yt, сделать replace(`":` -> `", `):
 * SELECT
 * TableRow()
 * FROM hahn.`//home/market/production/checkouter/testing/cdc/checkouter_main/return`
 * LIMIT 100;
 */
class ImportOrderReturnsServiceTest extends FunctionalTest {

    @Autowired
    private YtHttpFactory ytHttpFactory;

    @Autowired
    private OrderReturnDao orderReturnDao;

    @Autowired
    private Clock clock;

    private static final String RETURN_CLUSTER = "hahn.yt.yandex.net";
    private static final String TRACK_CLUSTER = "nehahn.yt.yandex.net";

    private YtCluster ytCluster;
    private YtTemplate ytTemplate;
    private ImportOrderReturnsTrackCodeYtDao importOrderReturnsTrackCodeYtDao;
    private ImportOrderReturnsService importOrderReturnsService;

    private static final Function<Object, YTreeNode> LONG_NODE = v -> YtUtilTest.longNode(((Integer) v).longValue());
    private static final Function<Object, YTreeNode> INT_NODE = v -> YtUtilTest.intNode((Integer) v);
    private static final Function<Object, YTreeNode> BOOL_NODE = v -> YtUtilTest.booleanNode((Boolean) v);
    private static final Function<Object, YTreeNode> STRING_NODE = v -> YtUtilTest.stringNode((String) v);

    private static final Map<String, Function<Object, YTreeNode>> returnYtFields = Map.of(
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

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importApprovedReturns.after.csv")
    void importApprovedReturns() {
        when(clock.instant()).thenReturn(Instant.parse("2021-06-10T12:00:00.000Z"));

        setYtData(toYtNodes(List.of(
                        Map.of(
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-695700.pdf",
                                "created_at", "2021-02-25T19:08:53.71163Z",
                                "id", 695700,
                                "order_id", 37345944,
                                "status", 2,
                                "updated_at", "2021-04-08T17:34:23.037181Z",
                                "status_updated_at", "2021-04-08T17:34:23.037181Z",
                                "fast_return", true
                        ),
                        Map.of(
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-695702.pdf",
                                "created_at", "2021-02-25T19:09:17.657684Z",
                                "id", 695702,
                                "order_id", 37698825,
                                "status", 0,
                                "status_updated_at", "2021-02-25T19:09:17.657684Z",
                                "updated_at", "2021-02-25T19:09:17.657684Z"
                        ),
                        Map.of(
                                "id", 13926,
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-13926" +
                                        ".pdf",
                                "created_at", "2019-03-27T12:01:03.814Z",
                                "order_id", 4954899,
                                "processing_details", "Return 13926expired",
                                "status", 3,
                                "status_updated_at", "2019-04-12T09:29:48.303Z",
                                "updated_at", "2019-04-12T09:29:48.303Z"
                        ),
                        Map.of(
                                "id", 18802,
                                "order_id", 6512371,
                                "status", 1,
                                "created_at", "2019-05-16T09:32:56.888Z",
                                "updated_at", "2019-05-28T14:14:23.939Z",
                                "status_updated_at", "2019-05-28T14:14:23.939Z",
                                "processing_details", "Return 18802expired",
                                "fast_return", false
                        )
                )),
                List.of(YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(18802))
                                .put("track_code", stringNode("4532kkljl35"))
                                .build()
                        ), YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(13926))
                                .put("track_code", stringNode("7823456827yhkh"))
                                .build()
                        )
                ));

        importOrderReturnsService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importLastNotExpired.after.csv")
    void importLastNotExpired() {
        when(clock.instant()).thenReturn(Instant.parse("2021-06-10T12:00:00.000Z"));

        setYtData(toYtNodes(List.of(
                        Map.of(
                                "id", 13926,
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-13926"
                                        +
                                        ".pdf",
                                "created_at", "2021-06-02T12:01:03.814Z",
                                "order_id", 4954899,
                                "processing_details", "Return 13926expired",
                                "status", 2,
                                "status_updated_at", "2021-06-02T09:29:48.303Z",
                                "updated_at", "2021-06-02T09:29:48.303Z"
                        ),
                        Map.of(
                                "id", 18802,
                                "order_id", 4954899,
                                "status", 0,
                                "created_at", "2021-06-03T09:32:56.888Z",
                                "updated_at", "2021-06-03T14:14:23.939Z",
                                "status_updated_at", "2021-06-03T14:14:23.939Z",
                                "processing_details", "Return 18802expired",
                                "fast_return", false
                        ),
                        Map.of(
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-695700.pdf",
                                "created_at", "2021-06-04T12:08:53.71163Z",
                                "id", 695700,
                                "order_id", 4954899,
                                "status", 0,
                                "updated_at", "2021-06-04T17:34:23.037181Z",
                                "status_updated_at", "2021-06-04T17:34:23.037181Z",
                                "fast_return", true
                        )
                )),
                List.of(YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(18802))
                                .put("track_code", stringNode(""))
                                .build()
                        ), YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(13926))
                                .put("track_code", stringNode("7823456827yhkh"))
                                .build()
                        )
                ));

        importOrderReturnsService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importLastExpired.after.csv")
    void importLastExpired() {
        when(clock.instant()).thenReturn(Instant.parse("2021-07-10T12:00:00.000Z"));

        setYtData(toYtNodes(List.of(
                        Map.of(
                                "id", 13926,
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-13926"
                                        +
                                        ".pdf",
                                "created_at", "2021-06-02T12:01:03.814Z",
                                "order_id", 4954899,
                                "processing_details", "Return 13926expired",
                                "status", 2,
                                "status_updated_at", "2021-06-02T09:29:48.303Z",
                                "updated_at", "2021-06-02T09:29:48.303Z"
                        ),
                        Map.of(
                                "id", 18802,
                                "order_id", 4954899,
                                "status", 0,
                                "created_at", "2021-06-03T09:32:56.888Z",
                                "updated_at", "2021-06-03T14:14:23.939Z",
                                "status_updated_at", "2021-06-03T14:14:23.939Z",
                                "processing_details", "Return 18802expired",
                                "fast_return", false
                        ),
                        Map.of(
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-695700.pdf",
                                "created_at", "2021-06-04T12:08:53.71163Z",
                                "id", 695700,
                                "order_id", 4954899,
                                "status", 0,
                                "updated_at", "2021-06-04T17:34:23.037181Z",
                                "status_updated_at", "2021-06-04T17:34:23.037181Z",
                                "fast_return", true
                        )
                )),
                List.of(YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(18802))
                                .put("track_code", stringNode(""))
                                .build()
                        ), YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(13926))
                                .put("track_code", stringNode("7823456827yhkh"))
                                .build()
                        )
                ));

        importOrderReturnsService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importWithTracking.after.csv")
    void importWithTracking() {
        when(clock.instant()).thenReturn(Instant.parse("2021-07-10T12:00:00.000Z"));

        setYtData(toYtNodes(List.of(
                        Map.of(
                                "id", 18802,
                                "order_id", 4954899,
                                "status", 0,
                                "created_at", "2021-06-03T09:32:56.888Z",
                                "updated_at", "2021-06-03T14:14:23.939Z",
                                "status_updated_at", "2021-06-03T14:14:23.939Z",
                                "processing_details", "Return 18802expired",
                                "fast_return", false
                        )
                )),
                List.of(YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(18802))
                                .put("track_code", stringNode("4532kkljl35"))
                                .build()
                        )
                ));

        importOrderReturnsService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(after = "ImportOrderReturnsServiceTest.importTwoOrdersLastNotExpired.after.csv")
    void importTwoOrdersLastNotExpired() {
        when(clock.instant()).thenReturn(Instant.parse("2021-06-10T12:00:00.000Z"));

        setYtData(toYtNodes(List.of(
                        Map.of(
                                "id", 13926,
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-13926"
                                        +
                                        ".pdf",
                                "created_at", "2021-06-02T12:01:03.814Z",
                                "order_id", 4954800,
                                "processing_details", "Return 13926expired",
                                "status", 0,
                                "status_updated_at", "2021-06-02T09:29:48.303Z",
                                "updated_at", "2021-06-02T09:29:48.303Z"
                        ),
                        Map.of(
                                "id", 18802,
                                "order_id", 4954899,
                                "status", 0,
                                "created_at", "2021-06-03T09:32:56.888Z",
                                "updated_at", "2021-06-03T14:14:23.939Z",
                                "status_updated_at", "2021-06-03T14:14:23.939Z",
                                "processing_details", "Return 18802expired",
                                "fast_return", false
                        ),
                        Map.of(
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-695700.pdf",
                                "created_at", "2021-06-04T12:08:53.71163Z",
                                "id", 695700,
                                "order_id", 4954899,
                                "status", 0,
                                "updated_at", "2021-06-04T17:34:23.037181Z",
                                "status_updated_at", "2021-06-04T17:34:23.037181Z",
                                "fast_return", true
                        )
                )),
                List.of(YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(18802))
                                .put("track_code", stringNode(""))
                                .build()
                        ), YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(13926))
                                .put("track_code", stringNode("7823456827yhkh"))
                                .build()
                        )
                ));

        importOrderReturnsService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(
            before = "ImportOrderReturnsServiceTest.updateStoredReturns.before.csv",
            after = "ImportOrderReturnsServiceTest.updateStoredReturns.after.csv")
    void updateStoredReturns() {
        when(clock.instant()).thenReturn(Instant.parse("2021-06-10T12:00:00.000Z"));

        setYtData(toYtNodes(List.of(
                        Map.of(
                                "id", 13926,
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-13926"
                                        +
                                        ".pdf",
                                "created_at", "2021-06-02T12:01:03.814Z",
                                "order_id", 4954800,
                                "processing_details", "Return 13926expired",
                                "status", 2,
                                "status_updated_at", "2021-06-02T09:29:48.303Z",
                                "updated_at", "2021-06-02T09:29:48.303Z"
                        ),
                        Map.of(
                                "application_url", "https://market-checkouter-prod.s3.mds.yandex" +
                                        ".net/return-application-695700.pdf",
                                "created_at", "2021-06-04T12:08:53.71163Z",
                                "id", 695700,
                                "order_id", 4954899,
                                "status", 0,
                                "updated_at", "2021-06-04T17:34:23.037181Z",
                                "status_updated_at", "2021-06-04T17:34:23.037181Z",
                                "fast_return", true
                        )
                )),
                List.of(YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(13926))
                                .put("track_code", stringNode("4532kkljl35"))
                                .build()
                        ), YtUtilTest.treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("return_id", longNode(695700))
                                .put("track_code", stringNode("7823456827yhkh"))
                                .build()
                        )
                ));

        importOrderReturnsService.runAtCluster(ytCluster);
    }

    void setYtData(List<YTreeMapNode> returnYtData, List<YTreeMapNode> trackCodeYtData) {
        final Yt returnYt = YtUtilTest.mockYt(returnYtData);
        when(ytHttpFactory.getYt(RETURN_CLUSTER)).thenReturn(returnYt);

        final Yt trackYt = YtUtilTest.mockYt(trackCodeYtData);
        when(ytHttpFactory.getYt(TRACK_CLUSTER)).thenReturn(trackYt);

        ytCluster = new YtCluster(RETURN_CLUSTER, returnYt);
        ytTemplate = new YtTemplate(new YtCluster[]{new YtCluster(TRACK_CLUSTER, trackYt)});

        importOrderReturnsTrackCodeYtDao = new ImportOrderReturnsTrackCodeYtDao(ytTemplate,
                "//home/table");
        importOrderReturnsService = new ImportOrderReturnsService(orderReturnDao, importOrderReturnsTrackCodeYtDao,
                "//home/table2", 20, clock);
    }

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> orderReturns) {
        return orderReturns.stream()
                .map(orderReturn -> YtUtilTest.treeMapNode(
                                returnYtFields.entrySet().stream()
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
