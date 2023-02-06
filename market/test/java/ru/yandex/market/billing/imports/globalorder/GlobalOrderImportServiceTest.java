package ru.yandex.market.billing.imports.globalorder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterYtDao;
import ru.yandex.market.billing.imports.globalorder.dao.GlobalOrderDao;
import ru.yandex.market.billing.imports.globalorder.dao.GlobalOrderTrantimeDao;
import ru.yandex.market.billing.payment.dao.PaymentDao;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.yt.YtClusterStub;

import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultCheckouterOrder;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultCheckouterOrderItem;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.toYTreeMapNode;

@ParametersAreNonnullByDefault
class GlobalOrderImportServiceTest extends FunctionalTest {

    private static final String TEST_DATE = "2021-11-22";
    private static final String ORDER_TABLES_DIR = "//global/order/dir/";
    private static final String ORDER_ITEM_TABLES_DIR = "//global/order-item/dir/";

    @Autowired
    private GlobalOrderDao globalOrderDao;

    @Autowired
    private GlobalOrderTrantimeDao globalOrderTrantimeDao;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;

    private GlobalOrderImportService service;

    private YtClusterStub hahn = new YtClusterStub("hahn");
    private YtClusterStub arnold = new YtClusterStub("arnold");
    private GlobalCheckouterYtDao ytDao;

    @BeforeEach
    void setUp() {
        ytDao = new GlobalCheckouterYtDao(
                new YtTemplate(new YtCluster[]{hahn, arnold}), ORDER_TABLES_DIR, ORDER_ITEM_TABLES_DIR
        );

        service = new GlobalOrderImportService(
                ytDao, globalOrderDao, globalOrderTrantimeDao, paymentDao, transactionTemplate, environmentService
        );
    }

    @Ignore
    @Test
    @DbUnitDataSet(before = "GlobalOrderImportServiceTest.before.csv", after = "GlobalOrderImportServiceTest.after.csv")
    void process() {
        LocalDate date = LocalDate.parse(TEST_DATE);
        var orders = List.of(
                defaultCheckouterOrder(4)
                        .setPaymentId(41234567890L)
                        .setSubsidyPaymentId(41234567891L)
                        .setProcessInBillingAt(OffsetDateTime.parse("2021-11-22T23:10:00+03").toInstant())
                        .build(),
                defaultCheckouterOrder(7)
                        .setShopId(1111L)
                        .setPaymentId(71234567890L)
                        .setProcessInBillingAt(OffsetDateTime.parse("2021-11-22T20:10:00+03").toInstant())
                        .build()
        );
        var items = orders.stream()
                .map(order -> defaultCheckouterOrderItem(11 * order.getId(), order.getId()).build())
                .collect(Collectors.toList());

        List.of(hahn, arnold).forEach(cluster -> {
            cluster.addTable(ORDER_TABLES_DIR + TEST_DATE,
                    orders.stream().map(order -> toYTreeMapNode(order, date)).collect(Collectors.toList())
            );
            cluster.addTable(ORDER_ITEM_TABLES_DIR + TEST_DATE,
                    items.stream().map(item -> toYTreeMapNode(item, date)).collect(Collectors.toList())
            );
        });

        service.process(date);
    }

    @DisplayName("Импорт заказов с игнором")
    @Test
    @DbUnitDataSet(
            before = "GlobalOrderImportServiceTest.withIgnore.before.csv",
            after = "GlobalOrderImportServiceTest.withIgnore.after.csv"
    )
    void processWithIgnore() {
        LocalDate date = LocalDate.parse(TEST_DATE);
        var orders = List.of(
                defaultCheckouterOrder(4)
                        .setPaymentId(41234567890L)
                        .setSubsidyPaymentId(41234567891L)
                        .setProcessInBillingAt(OffsetDateTime.parse("2021-11-22T12:00:00+03").toInstant())
                        .setFreeDeliveryForShop(true)
                        .build(),
                defaultCheckouterOrder(7)
                        .setShopId(1111L)
                        .setPaymentId(71234567890L)
                        .setProcessInBillingAt(OffsetDateTime.parse("2021-11-22T12:00:00+03").toInstant())
                        .build()
        );
        var items = orders.stream()
                .map(order -> defaultCheckouterOrderItem(11 * order.getId(), order.getId()).build())
                .collect(Collectors.toList());

        List.of(hahn, arnold).forEach(cluster -> {
            cluster.addTable(ORDER_TABLES_DIR + TEST_DATE,
                    orders.stream().map(order -> toYTreeMapNode(order, date)).collect(Collectors.toList())
            );
            cluster.addTable(ORDER_ITEM_TABLES_DIR + TEST_DATE,
                    items.stream().map(item -> toYTreeMapNode(item, date)).collect(Collectors.toList())
            );
        });

        service.process(date);
    }
}
