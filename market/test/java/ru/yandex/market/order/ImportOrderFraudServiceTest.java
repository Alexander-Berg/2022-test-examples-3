package ru.yandex.market.order;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.yt.YtUtilTest;
import ru.yandex.market.order.matchers.OrderFraudInfoMatchers;
import ru.yandex.market.order.model.OrderFraudInfo;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.yt.YtUtilTest.booleanNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.longNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.stringNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.treeListNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.yPathHasPath;

/**
 * Тесты для {@link ImportOrderFraudService}
 */
@ExtendWith(MockitoExtension.class)
class ImportOrderFraudServiceTest extends FunctionalTest {

    private static LocalDate DUMMY_DATE = LocalDate.EPOCH;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OrderFraudDao orderFraudDao;

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Mock
    private YtTables ytTables;

    @Value("${mbi.billing.order.fraud.info.table}")
    private String ytOrderFraudInfoTable;

    @Captor
    private ArgumentCaptor<List<OrderFraudInfo>> upsertOrderFraudInfo;

    private ImportOrderFraudService importOrderFraudService;

    private ImportOrderFraudInfoExecutor importOrderFraudInfoExecutor;

    private static YTreeMapNodeImpl buildOrderFraudInfoNode(OrderFraudInfo orderFraudInfo, List<List<String>> userFraudMarkers) {
        YTreeMapNodeImpl yTreeMapNode = new YTreeMapNodeImpl(new OpenHashMap<>());
        yTreeMapNode.put("order_id", longNode(orderFraudInfo.getOrderId()));
        yTreeMapNode.put("order_fraud", booleanNode(orderFraudInfo.isOrderFraud()));
        yTreeMapNode.put("buyer_uid", stringNode(String.valueOf(orderFraudInfo.getBuyerUid())));
        yTreeMapNode.put("is_first", booleanNode(orderFraudInfo.isFirstOrder()));
        yTreeMapNode.put("traffic_type", stringNode(orderFraudInfo.getTrafficType()));
        yTreeMapNode.put("over_limit", booleanNode(orderFraudInfo.isOverLimit()));
        yTreeMapNode.put(
                "user_fraud_markers",
                treeListNode(
                        userFraudMarkers.stream()
                                .map(list -> list.stream()
                                        .map(YtUtilTest::stringNode)
                                        .collect(Collectors.toList())
                                )
                                .map(YtUtilTest::treeListNode)
                                .collect(Collectors.toList())
                )
        );
        return yTreeMapNode;
    }

    @BeforeEach
    void init() {
        orderFraudDao = Mockito.spy(orderFraudDao);
        Clock fixedClock = Clock.fixed(DateTimes.toInstantAtDefaultTz(2019, 10, 11), ZoneId.systemDefault());
        importOrderFraudService = new ImportOrderFraudService(yt, orderFraudDao, ytOrderFraudInfoTable,
                transactionTemplate, fixedClock);
        importOrderFraudInfoExecutor = new ImportOrderFraudInfoExecutor(importOrderFraudService);
    }

    @DisplayName("Ежедневный импорт данных от антифрода по заказам из YT")
    @Test
    @DbUnitDataSet(before = "db/ImportOrderFraudInfoExecutorTest.before.csv")
    void shouldImportOrderFraudInfoWhenGiven() {
        initYt(ytOrderFraudInfoTable, List.of(
                buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(1)
                                .setBuyerUid(123)
                                .setOrderFraud(false)
                                .setFirstOrder(true)
                                .setTrafficType(null)
                                .setOverLimit(false)
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of()
                ),
                buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(2)
                                .setBuyerUid(234)
                                .setOrderFraud(true)
                                .setFirstOrder(false)
                                .setOverLimit(true)
                                .setTrafficType("coupon")
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of()
                ),
                buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(3)
                                .setBuyerUid(345)
                                .setOrderFraud(true)
                                .setFirstOrder(false)
                                .setOverLimit(false)
                                .setTrafficType("context")
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of()
                ),
                buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(4)
                                .setBuyerUid(456)
                                .setOrderFraud(false)
                                .setFirstOrder(false)
                                .setOverLimit(true)
                                .setTrafficType("organic")
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of(List.of("reseller_t0"), List.of("any_other_marker"))
                ),
                buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(5)
                                .setBuyerUid(567)
                                .setOrderFraud(false)
                                .setFirstOrder(false)
                                .setOverLimit(false)
                                .setTrafficType(null)
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of(List.of("any_other_marker"))
                ),
                buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(6)
                                .setBuyerUid(567)
                                .setOrderFraud(false)
                                .setFirstOrder(false)
                                .setOverLimit(false)
                                .setTrafficType("toolbar")
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of(List.of("any_other_marker"))
                ),
                buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(7)
                                .setBuyerUid(567)
                                .setOrderFraud(false)
                                .setFirstOrder(false)
                                .setOverLimit(false)
                                .setTrafficType(null)
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of(List.of("any_other_marker"))
                ),
                buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(8)
                                .setBuyerUid(567)
                                .setOrderFraud(false)
                                .setFirstOrder(false)
                                .setOverLimit(false)
                                .setTrafficType("coupon")
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of(List.of("any_other_marker"))
                )
                ,buildOrderFraudInfoNode(new OrderFraudInfo.Builder()
                                .setOrderId(9)
                                .setBuyerUid(567)
                                .setOrderFraud(false)
                                .setFirstOrder(false)
                                .setOverLimit(false)
                                .setTrafficType("coupon")
                                .setUpdatedAt(DUMMY_DATE)
                                .build(),
                        List.of(List.of("any_other_marker"))
                )
        ));

        doNothing().when(orderFraudDao).upsertOrderFraudInfo(anyList());
        doNothing().when(orderFraudDao).deleteOrderFraudInfo(anyList());
        when(cypress.exists(any(YPath.class))).thenReturn(true);

        importOrderFraudInfoExecutor.doJob(null);

        verify(ytTables).read(
                ArgumentMatchers.argThat(ytPath ->
                        yPathHasPath(ytPath, ytOrderFraudInfoTable)),
                any(YTableEntryType.class),
                any(Consumer.class)
        );

        verify(orderFraudDao).upsertOrderFraudInfo(upsertOrderFraudInfo.capture());

        assertThat(upsertOrderFraudInfo.getValue(), hasSize(6));
        assertThat(
                upsertOrderFraudInfo.getValue().stream()
                        .sorted(Comparator.comparingLong(OrderFraudInfo::getOrderId))
                        .collect(Collectors.toList()),
                contains(
                        allOf(
                                OrderFraudInfoMatchers.hasOrderId(2L),
                                OrderFraudInfoMatchers.hasBuyerUid(234L),
                                OrderFraudInfoMatchers.isOrderFraud(true),
                                OrderFraudInfoMatchers.isOverLimit(true),
                                OrderFraudInfoMatchers.isFirstOrder(false),
                                OrderFraudInfoMatchers.hasTrafficType("coupon"),
                                OrderFraudInfoMatchers.hasUpdatedAt(LocalDate.of(2019, 10, 11))
                        ),
                        allOf(
                                OrderFraudInfoMatchers.hasOrderId(3L),
                                OrderFraudInfoMatchers.hasBuyerUid(345L),
                                OrderFraudInfoMatchers.isOrderFraud(true),
                                OrderFraudInfoMatchers.isFirstOrder(false),
                                OrderFraudInfoMatchers.isOverLimit(false),
                                OrderFraudInfoMatchers.hasTrafficType("context"),
                                OrderFraudInfoMatchers.hasUpdatedAt(LocalDate.of(2019, 10, 11))
                        ),
                        allOf(
                                OrderFraudInfoMatchers.hasOrderId(4L),
                                OrderFraudInfoMatchers.hasBuyerUid(456L),
                                OrderFraudInfoMatchers.isOrderFraud(true),
                                OrderFraudInfoMatchers.isFirstOrder(false),
                                OrderFraudInfoMatchers.isOverLimit(true),
                                OrderFraudInfoMatchers.hasTrafficType("organic"),
                                OrderFraudInfoMatchers.hasUpdatedAt(LocalDate.of(2019, 10, 11))
                        ),
                        allOf(
                                OrderFraudInfoMatchers.hasOrderId(5L),
                                OrderFraudInfoMatchers.hasBuyerUid(567L),
                                OrderFraudInfoMatchers.isOrderFraud(false),
                                OrderFraudInfoMatchers.isFirstOrder(false),
                                OrderFraudInfoMatchers.isOverLimit(false),
                                OrderFraudInfoMatchers.hasTrafficType(null),
                                OrderFraudInfoMatchers.hasUpdatedAt(LocalDate.of(2019, 10, 11))
                        ),
                        allOf(
                                OrderFraudInfoMatchers.hasOrderId(6L),
                                OrderFraudInfoMatchers.hasBuyerUid(567L),
                                OrderFraudInfoMatchers.isOrderFraud(false),
                                OrderFraudInfoMatchers.isFirstOrder(false),
                                OrderFraudInfoMatchers.isOverLimit(false),
                                OrderFraudInfoMatchers.hasTrafficType("toolbar"),
                                OrderFraudInfoMatchers.hasUpdatedAt(LocalDate.of(2019, 10, 11))
                        ),
                        allOf(
                                OrderFraudInfoMatchers.hasOrderId(9L),
                                OrderFraudInfoMatchers.hasBuyerUid(567L),
                                OrderFraudInfoMatchers.isOrderFraud(false),
                                OrderFraudInfoMatchers.isFirstOrder(false),
                                OrderFraudInfoMatchers.isOverLimit(false),
                                OrderFraudInfoMatchers.hasTrafficType("coupon"),
                                OrderFraudInfoMatchers.hasUpdatedAt(LocalDate.of(2019, 10, 11))
                        )
                )
        );
    }

    private void initYt(String path, List<YTreeMapNodeImpl> ytReturnValue) {
        String tableFullPath = path;
        when(cypress.list(
                argThat(ytPath -> yPathHasPath(ytPath, path)),
                any(SetF.class)
        )).thenReturn(
                Cf.arrayList(new YTreeStringNodeImpl(
                        "",
                        Cf.map("type", stringNode("table"))
                ))
        );

        doAnswer(invocation -> {
            Consumer<YTreeMapNode> consumer = invocation.getArgument(2);

            List<YTreeMapNode> nodes = new ArrayList<>(ytReturnValue);
            nodes.forEach(consumer);

            return null;
        }).when(ytTables).read(
                ArgumentMatchers.argThat(ytPath -> yPathHasPath(ytPath, tableFullPath)),
                any(YTableEntryType.class),
                any(Consumer.class)
        );

        when(yt.cypress()).thenReturn(cypress);
        when(yt.tables()).thenReturn(ytTables);
    }
}
