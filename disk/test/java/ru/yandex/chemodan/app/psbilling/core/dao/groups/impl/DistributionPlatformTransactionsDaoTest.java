package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import java.math.BigDecimal;
import java.util.Currency;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.DistributionPlatformTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.DistributionPlatformTransactionEntity;
import ru.yandex.misc.test.Assert;

public class DistributionPlatformTransactionsDaoTest extends AbstractPsBillingCoreTest {
    private LocalDate calcDate = LocalDate.parse("2021-07-21");

    @Autowired
    private DistributionPlatformTransactionsDao dao;

    @Test
    public void insert() {
        Group group = psBillingGroupsFactory.createGroup();
        DistributionPlatformTransactionsDao.InsertData data =
                createData(calcDate, group, x -> x.paidUserCount(BigDecimal.ONE));
        dao.batchInsert(Cf.list(data));

        ListF<DistributionPlatformTransactionEntity> transactions = dao.findTransactions(calcDate, Option.empty(), 1);
        Assert.equals(1, transactions.length());

        DistributionPlatformTransactionEntity transaction = transactions.get(0);
        Assert.equals(data.getActId(), transaction.getActId());
        Assert.equals(data.getAmount(), transaction.getAmount());
        Assert.equals(data.getCalcMonth().withDayOfMonth(1), transaction.getCalcMonth());
        Assert.equals(data.getClid(), transaction.getClid());
        Assert.equals(data.getCurrency(), transaction.getCurrency());
        Assert.equals(data.getGroupId(), transaction.getGroupId());
        Assert.equals(data.getPaidUserCount(), transaction.getPaidUserCount());
        Assert.notNull(transaction.getCreatedAt());
        Assert.notNull(transaction.getId());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void constraints_uk() {
        Group group = psBillingGroupsFactory.createGroup();
        LocalDate calcDate = LocalDate.parse("2021-07-21");
        DistributionPlatformTransactionsDao.InsertData data = createData(calcDate, group);
        dao.batchInsert(Cf.list(data, data));
    }

    @Test
    public void batchInsert() {
        Group group = psBillingGroupsFactory.createGroup();
        DistributionPlatformTransactionsDao.InsertData data = createData(calcDate, group, x -> x.actId("1"));

        dao.batchInsert(Cf.list(
                createData(calcDate, group, x -> x.actId("1")),
                createData(calcDate, group, x -> x.actId("2")),
                createData(calcDate, group, x -> x.actId("3"))));

        ListF<DistributionPlatformTransactionEntity> transactions = dao.findTransactions(calcDate, Option.empty(), 3);
        Assert.equals(3, transactions.length());
    }


    @Test
    public void findTransactions() {
        Group group = psBillingGroupsFactory.createGroup();

        ListF<DistributionPlatformTransactionEntity> transactions;
        transactions = dao.findTransactions(calcDate, Option.empty(), 0);
        Assert.equals(0, transactions.length());

        transactions = dao.findTransactions(calcDate, Option.empty(), 100);
        Assert.equals(0, transactions.length());

        dao.batchInsert(Cf.list(
                createData(calcDate, group, x -> x.actId("1")),
                createData(calcDate, group, x -> x.actId("2")),
                createData(calcDate, group, x -> x.actId("3")),
                createData(calcDate, group, x -> x.actId("4")),
                createData(calcDate, group, x -> x.actId("5"))));

        SetF<DistributionPlatformTransactionEntity> allTransactions = Cf.hashSet();

        transactions = dao.findTransactions(calcDate, Option.empty(), 2);
        Assert.equals(2, transactions.length());
        allTransactions.addAll(transactions);

        transactions = dao.findTransactions(calcDate, Option.of(transactions.last().getId()), 2);
        Assert.equals(2, transactions.length());
        allTransactions.addAll(transactions);

        transactions = dao.findTransactions(calcDate, Option.of(transactions.last().getId()), 2);
        Assert.equals(1, transactions.length());
        allTransactions.addAll(transactions);

        Assert.equals(5, allTransactions.size());
    }

    @Test
    public void findExportRows() {
        Group group = psBillingGroupsFactory.createGroup();

        dao.batchInsert(Cf.list(
                createData(calcDate, group,
                        x -> x.actId("1").clid("1").amount(BigDecimal.TEN).currency(Currency.getInstance("RUB"))),
                createData(calcDate, group,
                        x -> x.actId("2").clid("1").amount(BigDecimal.TEN).currency(Currency.getInstance("USD"))),
                createData(calcDate, group,
                        x -> x.actId("3").clid("2").amount(BigDecimal.TEN).currency(Currency.getInstance("RUB"))),
                createData(calcDate, group,
                        x -> x.actId("4").clid("2").amount(BigDecimal.TEN).currency(Currency.getInstance("RUB")))));

        ListF<DistributionPlatformTransactionsDao.ExportRow> exportRows = dao.findExportRows(calcDate);
        Assert.equals(3, exportRows.length());

        Assert.equals("1", exportRows.get(0).getClid());
        Assert.equals(BigDecimal.TEN, exportRows.get(0).getMoney());
        Assert.equals("RUB", exportRows.get(0).getCurrency());

        Assert.equals("1", exportRows.get(1).getClid());
        Assert.equals(BigDecimal.TEN, exportRows.get(1).getMoney());
        Assert.equals("USD", exportRows.get(1).getCurrency());

        Assert.equals("2", exportRows.get(2).getClid());
        Assert.equals(BigDecimal.valueOf(20), exportRows.get(2).getMoney());
        Assert.equals("RUB", exportRows.get(2).getCurrency());
    }


    @Test
    public void removeTransactions() {
        Group group = psBillingGroupsFactory.createGroup();
        ListF<DistributionPlatformTransactionEntity> transactions;

        dao.removeCalculations(calcDate, 100);

        dao.batchInsert(Cf.list(
                createData(calcDate.minusMonths(1), group, x -> x.actId("01")),
                createData(calcDate.minusMonths(1), group, x -> x.actId("02")),
                createData(calcDate.minusMonths(1), group, x -> x.actId("03"))));

        transactions = dao.findTransactions(calcDate, Option.empty(), 100);
        Assert.equals(0, transactions.length());

        dao.removeCalculations(calcDate, 100);
        dao.findTransactions(calcDate, Option.empty(), 100);

        dao.batchInsert(Cf.list(
                createData(calcDate, group, x -> x.actId("1")),
                createData(calcDate, group, x -> x.actId("2")),
                createData(calcDate, group, x -> x.actId("3"))));

        transactions = dao.findTransactions(calcDate, Option.empty(), 100);
        Assert.equals(3, transactions.length());

        dao.removeCalculations(calcDate, 2);
        transactions = dao.findTransactions(calcDate, Option.empty(), 100);
        Assert.equals(1, transactions.length());

        dao.removeCalculations(calcDate, 2);
        transactions = dao.findTransactions(calcDate, Option.empty(), 100);
        Assert.equals(0, transactions.length());

        transactions = dao.findTransactions(calcDate.minusMonths(1), Option.empty(), 100);
        Assert.equals(3, transactions.length());
    }

    private DistributionPlatformTransactionsDao.InsertData createData(LocalDate calcDate, Group group) {
        return createData(calcDate, group, x -> x);
    }

    private DistributionPlatformTransactionsDao.InsertData createData(
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
