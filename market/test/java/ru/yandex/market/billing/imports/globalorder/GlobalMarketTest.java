package ru.yandex.market.billing.imports.globalorder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.factoring.ContractPayoutFrequencyDao;
import ru.yandex.market.billing.imports.globalcontract.GlobalPartnerContractDao;
import ru.yandex.market.billing.imports.globalcontract.GlobalPartnerContractImportExecutor;
import ru.yandex.market.billing.imports.globalcontract.GlobalPartnerContractImportService;
import ru.yandex.market.billing.imports.globalcontract.GlobalPartnerContractYtDao;
import ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterYtDao;
import ru.yandex.market.billing.imports.globalorder.dao.GlobalOrderDao;
import ru.yandex.market.billing.imports.globalorder.dao.GlobalOrderTrantimeDao;
import ru.yandex.market.billing.payment.dao.PaymentDao;
import ru.yandex.market.billing.payment.services.CommonPayoutFromAccrualExecutor;
import ru.yandex.market.billing.payment.services.GlobalAccrualTrantimesProcessingExecutor;
import ru.yandex.market.billing.payment.services.PaymentOrderDraftExecutor;
import ru.yandex.market.billing.payment.services.PaymentOrderFromDraftExecutor;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDatesProcessingService;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.billing.tlog.collection.PayoutsTransactionLogCollectionExecutor;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.model.PartnerContractType;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.yt.YtClusterStub;

import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultCheckouterOrder;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultCheckouterOrderItem;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.defaultShopContract;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalCheckouterTestUtil.toYTreeMapNode;

@ParametersAreNonnullByDefault
public class GlobalMarketTest extends FunctionalTest {
    private static final String ORDER_TABLES_DIR = "//yt/global/dictionaries/billing_order/";
    private static final String ORDER_ITEM_TABLES_DIR = "//yt/global/dictionaries/billing_order_item/";
    private static final String SHOP_CONTRACT_TABLES_DIR = "//yt/global/dictionaries/billing_shop/";

    @Autowired
    private TestableClock clock;

    @Autowired
    private EnvironmentAwareDatesProcessingService environmentAwareDatesProcessingService;

    @Autowired
    private GlobalPartnerContractDao globalPartnerContractDao;

    @Autowired
    private ContractPayoutFrequencyDao contractPayoutFrequencyDao;

    @Autowired
    private GlobalOrderDao globalOrderDao;

    @Autowired
    private GlobalOrderTrantimeDao globalOrderTrantimeDao;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private GlobalAccrualTrantimesProcessingExecutor globalAccrualTrantimesProcessingExecutor;

    @Autowired
    private CommonPayoutFromAccrualExecutor globalPayoutFromAccrualExecutor;

    @Autowired
    private PaymentOrderDraftExecutor paymentOrderDraftExecutor;

    @Autowired
    private PaymentOrderFromDraftExecutor paymentOrderFromDraftExecutor;

    @Autowired
    private PayoutsTransactionLogCollectionExecutor payoutsPaymentsTransactionLogCollectionExecutor;

    @Autowired
    private PayoutsTransactionLogCollectionExecutor payoutsExpensesTransactionLogCollectionExecutor;

    @Autowired
    private EnvironmentService environmentService;

    private YtClusterStub hahn = new YtClusterStub("hahn");
    private YtClusterStub arnold = new YtClusterStub("arnold");

    private GlobalPartnerContractImportExecutor contractImportExecutor;
    private GlobalOrderImportExecutor orderImportExecutor;

    @BeforeEach
    void setUp() {
        GlobalCheckouterYtDao ytDao = new GlobalCheckouterYtDao(
                new YtTemplate(new YtCluster[]{hahn, arnold}),
                ORDER_TABLES_DIR, ORDER_ITEM_TABLES_DIR
        );
        GlobalPartnerContractYtDao globalPartnerContractYtDao = new GlobalPartnerContractYtDao(
                new YtTemplate(new YtCluster[]{hahn, arnold}),
                SHOP_CONTRACT_TABLES_DIR
        );

        contractImportExecutor = new GlobalPartnerContractImportExecutor(
                new GlobalPartnerContractImportService(
                        globalPartnerContractYtDao, globalPartnerContractDao, contractPayoutFrequencyDao,
                        transactionTemplate
                ),
                environmentAwareDatesProcessingService
        );

        orderImportExecutor = new GlobalOrderImportExecutor(
                new GlobalOrderImportService(
                        ytDao, globalOrderDao, globalOrderTrantimeDao, paymentDao,
                        transactionTemplate, environmentService
                ),
                environmentAwareDatesProcessingService
        );
    }

    @AfterEach
    void tearDown() {
        clock.clearFixed();
    }

    @Test
    @DbUnitDataSet(before = "GlobalMarketTest.before.csv", after = "GlobalMarketTest.after.csv")
    void test() {
        // сегодня будут выплаты
        var today = LocalDate.of(2021, 12, 16);
        var yesterday = today.minusDays(1);
        var now = LocalDateTime.of(today, LocalTime.of(13, 58))
                .atZone(DateTimes.MOSCOW_TIME_ZONE).toInstant();
        clock.setFixed(now, ZoneId.systemDefault());

        var shopContract = defaultShopContract(
                3450491, PartnerContractType.INCOME, OffsetDateTime.parse("2021-12-08T20:12:39+03").toInstant()
        ).build();

        var order = defaultCheckouterOrder(100005)
                .setShopId(shopContract.getShopId())
                .setPaymentId(11L)
                .setSubsidyPaymentId(12L)
                .setItemsTotal(1100L)
                .setSubsidyTotal(0L)
                .setCreatedAt(OffsetDateTime.parse("2021-12-08T21:41:23+03").toInstant())
                .setProcessInBillingAt(OffsetDateTime.parse("2021-12-08T21:51:52+03").toInstant())
                .build();

        var item = defaultCheckouterOrderItem(100007, order.getId())
                .setPrice(100)
                .setSubsidy(0)
                .setMarketCategoryId(90401)
                .setShopCategoryId(1L)
                .setOfferId("45496452599")
                .setOfferName("Nintendo Switch")
                .build();

        hahn.addTable(SHOP_CONTRACT_TABLES_DIR + yesterday, List.of(toYTreeMapNode(shopContract, yesterday)));
        hahn.addTable(ORDER_TABLES_DIR + yesterday, List.of(toYTreeMapNode(order, yesterday)));
        hahn.addTable(ORDER_ITEM_TABLES_DIR + yesterday, List.of(toYTreeMapNode(item, yesterday)));

        contractImportExecutor.doJob();
        orderImportExecutor.doJob();
        globalAccrualTrantimesProcessingExecutor.doJob();
        globalPayoutFromAccrualExecutor.doJob();
        paymentOrderDraftExecutor.doJob();
        paymentOrderFromDraftExecutor.doJob();
        payoutsPaymentsTransactionLogCollectionExecutor.doJob();
        payoutsExpensesTransactionLogCollectionExecutor.doJob();
    }
}
