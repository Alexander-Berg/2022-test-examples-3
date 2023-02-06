package ru.yandex.chemodan.app.psbilling.core.billing.groups;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingBalanceFactory;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.misc.test.Assert;

public class GroupBalanceServiceTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupBalanceService groupBalanceService;
    @Autowired
    private BalanceClientStub balanceClientStub;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private PsBillingBalanceFactory psBillingBalanceFactory;

    @Test
    public void contractRequiredPayments_noDebt() {
        long clientId = 1L;
        long contractId = 2L;
        Currency currency = Currency.getInstance("RUB");
        psBillingBalanceFactory.createContractBalance(
                psBillingBalanceFactory.createContract(clientId, contractId, currency),
                x -> x
                        .withClientPaymentsSum(BigDecimal.valueOf(100))
                        .withActSum(BigDecimal.valueOf(30)));

        GroupBillingStatus billingStatus = groupBalanceService.calcGroupBillingStatus(1L);

        Assert.equals(0, billingStatus.getContractRequiredPayments().size());
    }

    @Test
    public void contractRequiredPayments_hasDebt() {
        long clientId = 1L;
        long contractId = 2L;
        Currency currency = Currency.getInstance("RUB");
        psBillingBalanceFactory.createContractBalance(
                psBillingBalanceFactory.createContract(clientId, contractId, currency),
                x -> x
                        .withClientPaymentsSum(BigDecimal.valueOf(30))
                        .withActSum(BigDecimal.valueOf(100)));

        GroupBillingStatus billingStatus = groupBalanceService.calcGroupBillingStatus(1L);

        Assert.equals(1, billingStatus.getContractRequiredPayments().size());
        Assert.equals(BigDecimal.valueOf(70),
                billingStatus.getContractRequiredPayments().getTs(contractId).getAmount());
    }
}
