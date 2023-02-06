package ru.yandex.chemodan.app.psbilling.core.billing.distributionplatform;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractGroupServicesTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingBalanceFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.DistributionPlatformCalculationService;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.GroupServiceTransactionsCalculationService;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.DistributionPlatformCalculationDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.DistributionPlatformTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.DistributionPlatformTransactionEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupsManager;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.model.response.ClientActInfo;
import ru.yandex.misc.test.Assert;

public abstract class BaseDistributionPlatformTest extends AbstractGroupServicesTest {
    protected LocalDate now = new LocalDate(2021, 7, 15);

    @Autowired
    protected GroupServiceTransactionsCalculationService groupServiceTransactionsCalculationService;
    @Autowired
    protected GroupServiceDao groupServiceDao;
    @Autowired
    protected GroupServiceTransactionsDao groupServiceTransactionsDao;
    @Autowired
    protected DistributionPlatformCalculationService distributionPlatformCalculationService;
    @Autowired
    protected DistributionPlatformTransactionsDao distributionPlatformTransactionsDao;
    @Autowired
    protected DistributionPlatformCalculationDao distributionPlatformCalculationDao;
    @Autowired
    protected GroupsManager groupsManager;
    @Autowired
    protected BalanceClientStub balanceClientStub;
    @Autowired
    protected BalanceService balanceService;
    @Autowired
    protected PsBillingBalanceFactory psBillingBalanceFactory;
    @Autowired
    protected PsBillingTransactionsFactory psBillingTransactionsFactory;

    @Before
    public void setup() {
        DateUtils.freezeTime(now);
    }

    protected void checkDpTransaction(Group group, ListF<DistributionPlatformTransactionEntity> dpTransactions,
                                      ClientActInfo actInfo,
                                      LocalDate calcMonth, ListF<GroupServiceTransactionsDao.ExportRow> transactions) {
        UUID groupId = group.getId();
        ListF<GroupServiceTransactionsDao.ExportRow> groupTransactions = findGroupTransactions(transactions, group);
        BigDecimal sum =
                groupTransactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);
        BigDecimal userSeconds =
                groupTransactions.map(x -> x.getGroupServiceTransaction().getUserSecondsCount()).reduceLeft(BigDecimal::add);

        DistributionPlatformTransactionEntity groupTransaction =
                dpTransactions.find(x -> x.getGroupId().equals(groupId)).get();

        Assert.equals(sum, groupTransaction.getAmount());
        Assert.equals("RUB", groupTransaction.getCurrency().getCurrencyCode());
        Assert.equals(userSeconds, groupTransaction.getPaidUserCount());
        Assert.equals(calcMonth.withDayOfMonth(1), groupTransaction.getCalcMonth());
        Assert.equals(group.getClid().get(), groupTransaction.getClid());
        Assert.equals(actInfo.getId(), groupTransaction.getActId());
        Assert.notNull(groupTransaction.getCreatedAt());
    }

    protected ClientActInfo createActInfo(LocalDate actDate, BigDecimal amount, BigDecimal paidAmount,
                                          long contractId) {
        return psBillingBalanceFactory.createActInfo(contractId, actDate, amount, paidAmount);
    }

    protected Group createGroup(String clid, Long clientId, ListF<PsBillingTransactionsFactory.Service> services) {
        return createGroup(clid, clientId, services, 1);
    }

    protected Group createGroup(String clid, Long clientId, ListF<PsBillingTransactionsFactory.Service> services,
                                int userCount) {
        return psBillingTransactionsFactory.createGroup(x -> x.clid(Option.ofNullable(clid)), clientId, services,
                userCount);
    }

    protected ListF<GroupServiceTransactionsDao.ExportRow> findGroupTransactions(ListF<GroupServiceTransactionsDao.ExportRow> transactions, Group group) {
        return transactions.filter(x ->
                groupServiceDao.findById(x.getGroupServiceTransaction().getGroupServiceId()).getGroupId()
                        .equals(group.getId()));
    }

    protected void calculateClient(Long clientId, LocalDate calcMonth) {
        UUID taskId = UUID.randomUUID();
        distributionPlatformCalculationDao.initCalculation(calcMonth, clientId, taskId, null);
        distributionPlatformCalculationService.calculateClient(taskId, clientId, calcMonth);
    }

    protected LocalDate getActDt(LocalDate calcMonth) {
        return psBillingBalanceFactory.getActDt(calcMonth);
    }

    protected DistributionPlatformTransactionsDao.InsertData createData(
            LocalDate calcDate, Group group,
            Function<DistributionPlatformTransactionsDao.InsertData.InsertDataBuilder,
                    DistributionPlatformTransactionsDao.InsertData.InsertDataBuilder> customizer) {
        return customizer.apply(DistributionPlatformTransactionsDao.InsertData.builder()
                .actId("act_id")
                .amount(BigDecimal.TEN)
                .calcMonth(calcDate)
                .clid("clid")
                .currency(Currency.getInstance("RUB"))
                .groupId(group.getId())
                .paidUserCount(BigDecimal.TEN)).build();
    }
}
