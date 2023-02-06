package ru.yandex.chemodan.app.psbilling.core.billing.distributionplatform;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.DistributionPlatformCalculationDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.DistributionPlatformTransactionEntity;
import ru.yandex.chemodan.app.psbilling.core.util.MathUtils;
import ru.yandex.chemodan.balanceclient.model.response.ClientActInfo;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.misc.test.Assert;

public class DistributionPlatformCalculationTest extends BaseDistributionPlatformTest {
    @Autowired
    protected DistributionPlatformCalculationDao distributionPlatformCalculationDao;

    @Test
    public void calculate() {
        Group group1 = createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);
        Group group2 = createGroup("clid1", 2L, Cf.list(
                new PsBillingTransactionsFactory.Service("code2").createdAt(now.minusMonths(2))), 2);
        Group group3 = createGroup("clid2", 2L, Cf.list(
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusMonths(2))), 3);

        LocalDate calcMonth = now.minusMonths(1);
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                psBillingTransactionsFactory.calculateMonthTransactions(calcMonth);
        ListF<GroupServiceTransactionsDao.ExportRow> group1Transactions = findGroupTransactions(transactions, group1);
        ListF<GroupServiceTransactionsDao.ExportRow> group2Transactions = findGroupTransactions(transactions, group2);
        ListF<GroupServiceTransactionsDao.ExportRow> group3Transactions = findGroupTransactions(transactions, group3);
        BigDecimal group1Sum =
                group1Transactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);
        BigDecimal group2Sum =
                group2Transactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);
        BigDecimal group3Sum =
                group3Transactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);

        GetClientContractsResponseItem contract1 = psBillingBalanceFactory.createContract(1L, 123L,
                Currency.getInstance("RUB"));
        GetClientContractsResponseItem contract2 = psBillingBalanceFactory.createContract(2L, 456L,
                Currency.getInstance("RUB"));

        LocalDate actDate = getActDt(calcMonth);
        ClientActInfo actInfo1 = createActInfo(actDate, group1Sum, group1Sum, contract1.getId());
        ClientActInfo actInfo2 = createActInfo(actDate, group2Sum.add(group3Sum), group2Sum.add(group3Sum),
                contract2.getId());
        balanceClientStub.createOrReplaceClientActs(1L, Cf.list(actInfo1));
        balanceClientStub.createOrReplaceClientActs(2L, Cf.list(actInfo2));

        calculateClient(1L, calcMonth);
        calculateClient(2L, calcMonth);
        ListF<DistributionPlatformTransactionEntity> dpTransactions =
                distributionPlatformTransactionsDao.findTransactions(calcMonth, Option.empty(), 10000);
        Assert.equals(3, dpTransactions.length());

        checkDpTransaction(group1, dpTransactions, actInfo1, calcMonth, transactions);
        checkDpTransaction(group2, dpTransactions, actInfo2, calcMonth, transactions);
        checkDpTransaction(group3, dpTransactions, actInfo2, calcMonth, transactions);
    }

    @Test
    public void calculate_groupWithNoClid() {
        Group group1 = createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);
        Group group2 = createGroup(null, 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code2").createdAt(now.minusMonths(2))), 2);

        LocalDate calcMonth = now.minusMonths(1);
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                psBillingTransactionsFactory.calculateMonthTransactions(calcMonth);

        ListF<GroupServiceTransactionsDao.ExportRow> group1Transactions = findGroupTransactions(transactions, group1);
        ListF<GroupServiceTransactionsDao.ExportRow> group2Transactions = findGroupTransactions(transactions, group2);

        BigDecimal group1Sum = MathUtils.sum(group1Transactions.map(x -> x.getGroupServiceTransaction().getAmount()));
        BigDecimal group2Sum = MathUtils.sum(group2Transactions.map(x -> x.getGroupServiceTransaction().getAmount()));

        GetClientContractsResponseItem contract = psBillingBalanceFactory.createContract(1L, 123L,
                Currency.getInstance("RUB"));

        LocalDate actDate = getActDt(calcMonth);
        BigDecimal totalSum = group1Sum.add(group2Sum);
        ClientActInfo actInfo1 = createActInfo(actDate, totalSum, totalSum, contract.getId());
        balanceClientStub.createOrReplaceClientActs(1L, Cf.list(actInfo1));

        calculateClient(1L, calcMonth);
        ListF<DistributionPlatformTransactionEntity> dpTransactions =
                distributionPlatformTransactionsDao.findTransactions(calcMonth, Option.empty(), 10000);
        Assert.equals(1, dpTransactions.length());

        checkDpTransaction(group1, dpTransactions, actInfo1, calcMonth, transactions);
    }

    @Test
    public void calculateWithNoTransactions() {
        createGroup("clid1", 1L,
                Cf.list(new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);

        LocalDate calcMonth = now.minusMonths(1);
        GetClientContractsResponseItem contract1 = psBillingBalanceFactory.createContract(1L, 123L,
                Currency.getInstance("RUB"));

        LocalDate actDate = getActDt(calcMonth);
        ClientActInfo actInfo1 = createActInfo(actDate, BigDecimal.TEN, BigDecimal.TEN, contract1.getId());
        balanceClientStub.createOrReplaceClientActs(1L, Cf.list(actInfo1));

        Assert.assertThrows(() ->
                calculateClient(1L, calcMonth), IllegalStateException.class);
    }

    @Test
    public void recalculate() {
        Group group1 = createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);

        LocalDate calcMonth = now.minusMonths(1);
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                psBillingTransactionsFactory.calculateMonthTransactions(calcMonth);
        ListF<GroupServiceTransactionsDao.ExportRow> group1Transactions = findGroupTransactions(transactions, group1);
        BigDecimal group1Sum =
                group1Transactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);

        GetClientContractsResponseItem contract1 = psBillingBalanceFactory.createContract(1L, 123L,
                Currency.getInstance("RUB"));

        LocalDate actDate = getActDt(calcMonth);
        ClientActInfo actInfo1 = createActInfo(actDate, group1Sum, group1Sum, contract1.getId());
        balanceClientStub.createOrReplaceClientActs(1L, Cf.list(actInfo1));

        calculateClient(1L, calcMonth);
        ListF<DistributionPlatformTransactionEntity> dpTransactions =
                distributionPlatformTransactionsDao.findTransactions(calcMonth, Option.empty(), 10000);
        Assert.equals(1, dpTransactions.length());

        checkDpTransaction(group1, dpTransactions, actInfo1, calcMonth, transactions);

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        calculateClient(1L, calcMonth);
        dpTransactions = distributionPlatformTransactionsDao.findTransactions(calcMonth, Option.empty(), 10000);
        Assert.equals(1, dpTransactions.length());
        checkDpTransaction(group1, dpTransactions, actInfo1, calcMonth, transactions);
    }

    @Test
    public void calculateWithNoActualActs() {
        Group group1 = createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);

        LocalDate calcMonth = now.minusMonths(1);
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                psBillingTransactionsFactory.calculateMonthTransactions(calcMonth);
        ListF<GroupServiceTransactionsDao.ExportRow> group1Transactions = findGroupTransactions(transactions, group1);
        BigDecimal group1Sum =
                group1Transactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);

        GetClientContractsResponseItem contract1 = psBillingBalanceFactory.createContract(1L, 123L,
                Currency.getInstance("RUB"));

        // old act date
        LocalDate actDate = getActDt(calcMonth).minusMonths(1);
        ClientActInfo actInfo1 = createActInfo(actDate, group1Sum, group1Sum, contract1.getId());
        balanceClientStub.createOrReplaceClientActs(1L, Cf.list(actInfo1));

        calculateClient(1L, calcMonth);
        ListF<DistributionPlatformTransactionEntity> dpTransactions =
                distributionPlatformTransactionsDao.findTransactions(calcMonth, Option.empty(), 10000);
        Assert.equals(0, dpTransactions.length());
    }

    @Test
    public void calculateWithNotPaidAct() {
        Group group1 = createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);

        LocalDate calcMonth = now.minusMonths(1);
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                psBillingTransactionsFactory.calculateMonthTransactions(calcMonth);
        ListF<GroupServiceTransactionsDao.ExportRow> group1Transactions = findGroupTransactions(transactions, group1);
        BigDecimal group1Sum =
                group1Transactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);

        GetClientContractsResponseItem contract1 = psBillingBalanceFactory.createContract(1L, 123L,
                Currency.getInstance("RUB"));

        LocalDate actDate = getActDt(calcMonth);
        ClientActInfo actInfo1 = createActInfo(actDate, group1Sum, group1Sum.divide(BigDecimal.valueOf(2),
                        RoundingMode.FLOOR),
                contract1.getId());
        balanceClientStub.createOrReplaceClientActs(1L, Cf.list(actInfo1));

        calculateClient(1L, calcMonth);
        ListF<DistributionPlatformTransactionEntity> dpTransactions =
                distributionPlatformTransactionsDao.findTransactions(calcMonth, Option.empty(), 10000);
        Assert.equals(0, dpTransactions.length());
    }

    @Test
    public void calculateWithOverPaidAct() {
        Group group1 = createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);

        LocalDate calcMonth = now.minusMonths(1);
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                psBillingTransactionsFactory.calculateMonthTransactions(calcMonth);
        ListF<GroupServiceTransactionsDao.ExportRow> group1Transactions = findGroupTransactions(transactions, group1);
        BigDecimal group1Sum =
                group1Transactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);

        GetClientContractsResponseItem contract1 = psBillingBalanceFactory.createContract(1L, 123L,
                Currency.getInstance("RUB"));

        LocalDate actDate = getActDt(calcMonth);
        ClientActInfo actInfo1 = createActInfo(actDate, group1Sum, group1Sum.multiply(BigDecimal.valueOf(2)),
                contract1.getId());
        balanceClientStub.createOrReplaceClientActs(1L, Cf.list(actInfo1));

        calculateClient(1L, calcMonth);
        ListF<DistributionPlatformTransactionEntity> dpTransactions =
                distributionPlatformTransactionsDao.findTransactions(calcMonth, Option.empty(), 10000);
        Assert.equals(1, dpTransactions.length());
        checkDpTransaction(group1, dpTransactions, actInfo1, calcMonth, transactions);
    }
}
