package ru.yandex.market.billing.returns;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.util.EnvironmentAwareDateValidationService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

public class ReturnBillingServiceTest extends FunctionalTest {
    private static final LocalDate BILLING_DATE = LocalDate.of(2021,10, 24);
    private static final String ENV_IGNORED_ORDERS =
            "market.billing.return_item_billing.ignore.return_id";

    @Autowired
    private ReturnItemBillingService billingService;

    @Autowired
    public EnvironmentService environmentService;

    @Autowired
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;

    @Autowired
    @Qualifier("oracleReturnRequestStatusHistoryDao")
    private ReturnRequestStatusHistoryDao returnRequestStatusHistoryDao;

    @Autowired
    private TransactionTemplate billingPgTransactionTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier("oracleReturnItemBilledDao")
    private ReturnItemBilledDao oracleReturnItemBilledDao;

    @Autowired
    @Qualifier("pgReturnItemBilledDao")
    private ReturnItemBilledDao pgReturnItemBilledDao;

    @Autowired
    private ReturnItemBiller returnItemBiller;

    @DbUnitDataSet(
            before = "ReturnItemBilling.before.csv",
            after = "ReturnItemBilling.after.csv"
    )
    @DisplayName("Проверяем, что биллим возвраты корректно")
    @Test
    void testReturnItemsBilling(){
        billingService.process(BILLING_DATE);
    }

    @DbUnitDataSet(
            before = "ReturnItemBilling.before.csv",
            after = "ReturnItemBillingIgnored.after.csv"
    )
    @DisplayName("Игнор определенных заказов")
    @Test
    void testReturnItemsBillingWithIgnoredOrders(){
        environmentService.setValues(ENV_IGNORED_ORDERS, List.of("3"));
        getBillingServiceWithMock().process(BILLING_DATE);
    }

    private ReturnItemBillingService getBillingServiceWithMock() {
        return new ReturnItemBillingService(
                environmentAwareDateValidationService,
                oracleReturnItemBilledDao,
                pgReturnItemBilledDao,
                returnRequestStatusHistoryDao,
                transactionTemplate,
                returnItemBiller,
                billingPgTransactionTemplate,
                environmentService
        );
    }
}
