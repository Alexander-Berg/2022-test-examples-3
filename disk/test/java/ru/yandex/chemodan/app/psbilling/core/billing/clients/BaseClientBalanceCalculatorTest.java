package ru.yandex.chemodan.app.psbilling.core.billing.clients;

import java.math.BigDecimal;
import java.util.Currency;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceCalculator;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceInfo;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.GroupServiceTransactionsCalculationService;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;
import ru.yandex.inside.passport.PassportUid;

public abstract class BaseClientBalanceCalculatorTest extends AbstractPsBillingCoreTest {
    protected Long clientId = 1L;
    protected Currency rub = Currency.getInstance("RUB");
    protected PassportUid uid = PassportUid.MAX_VALUE;

    @Autowired
    protected FeatureFlags featureFlags;
    @Autowired
    protected ClientBalanceCalculator calculator;
    @Autowired
    protected BalanceClientStub balanceClientStub;
    @Autowired
    protected ClientBalanceDao clientBalanceDao;
    @Autowired
    protected GroupServiceDao groupServiceDao;
    @Autowired
    protected DirectoryClient directoryClient;
    @Autowired
    protected GroupServiceTransactionsCalculationService groupServiceTransactionsCalculationService;

    @Before
    public void setup() {
        Mockito.when(directoryClient.getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, false));
    }

    protected GetClientContractsResponseItem createContract(Long clientId, Long contractId, GroupProduct groupProduct) {
        return psBillingBalanceFactory.createContract(clientId, contractId, groupProduct.getPriceCurrency());
    }

    protected void assertEquals(float expected, BigDecimal actual) {
        Assert.assertEquals(String.format("expected %s to be %s", actual, expected), 0,
                BigDecimal.valueOf(expected).compareTo(actual));
    }

    protected ClientBalanceInfo createBalance(Long clientId, int income, Currency currency) {
        ClientBalanceInfo balanceInfo = new ClientBalanceInfo(clientId, currency);
        balanceInfo.setIncomeSum(BigDecimal.valueOf(income));
        return balanceInfo;
    }

    protected void assertVoidDate(long clientId, Currency currency, Instant expectedVoidDate) {
        ClientBalanceEntity balance = calculator.updateClientBalance(clientId).getTs(currency);
        Assert.assertEquals(expectedVoidDate, balance.getBalanceVoidAt().get());
    }

    protected GroupService createGroupService(Group group, GroupProduct groupProduct) {
        return createGroupService(group, groupProduct, 1);
    }

    protected GroupService createGroupService(Group group, GroupProduct groupProduct, int userCount) {
        return psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()).createdAt(Instant.now()), userCount);
    }
}
