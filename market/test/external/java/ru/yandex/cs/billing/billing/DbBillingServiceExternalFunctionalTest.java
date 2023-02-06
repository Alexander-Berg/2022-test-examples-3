package ru.yandex.cs.billing.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.AbstractCsBillingCoreExternalFunctionalTest;
import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.Assert.assertEquals;

@DbUnitDataSet(
        before = "/ru/yandex/cs/billing/billing/DbBillingServiceExternalFunctionalTest/before.csv",
        dataSource = "csBillingDataSource"
)
public class DbBillingServiceExternalFunctionalTest extends AbstractCsBillingCoreExternalFunctionalTest {

    private final Clock clock;
    private final DbBillingService dbBillingService;
    private final BalanceService balanceService;

    @Autowired
    public DbBillingServiceExternalFunctionalTest(final Clock clock,
                                                  final DbBillingService dbBillingService,
                                                  final BalanceService balanceService) {
        this.clock = clock;
        this.dbBillingService = dbBillingService;
        this.balanceService = balanceService;
    }

    @DisplayName("Проверка суммы перевода со счета на счет")
    @DbUnitDataSet(
            before =
                    "/ru/yandex/cs/billing/billing/DbBillingServiceExternalFunctionalTest/testTransferMoney/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    public void testTransferMoney() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 10, 23)));
        BigDecimal transferMoney = new BigDecimal(123);
        long uid = 1;

        dbBillingService.makeTransfer(132, 1, 132, 2, transferMoney, uid);

        ArgumentCaptor<Integer> fromServiceIdArg = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> fromCampaignIdArg = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> toServiceIdArg = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> toCampaignIdArg = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<BigDecimal> currentBalanceInCUArg = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> newBalanceInCUArg = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Long> uidArg = ArgumentCaptor.forClass(Long.class);

        Mockito.verify(balanceService).makeTransfer(
                fromServiceIdArg.capture(), fromCampaignIdArg.capture(), toServiceIdArg.capture(),
                toCampaignIdArg.capture(),
                currentBalanceInCUArg.capture(), newBalanceInCUArg.capture(), uidArg.capture());
        BigDecimal currentBalanceInCU = BigDecimal.valueOf(32224.119217);
        BigDecimal newBalanceInCU = BigDecimal.valueOf(32222.889217);

        assertEquals(currentBalanceInCU, currentBalanceInCUArg.getValue());
        assertEquals(newBalanceInCU, newBalanceInCUArg.getValue());
    }
}
