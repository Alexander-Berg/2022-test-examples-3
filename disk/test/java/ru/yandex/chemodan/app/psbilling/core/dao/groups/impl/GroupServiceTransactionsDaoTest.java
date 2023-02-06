package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import java.math.BigDecimal;
import java.util.Currency;

import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupServiceTransaction;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;

public class GroupServiceTransactionsDaoTest extends AbstractPsBillingCoreTest {

    @Autowired
    private GroupServiceTransactionsDao dao;

    @Test
    public void findGroupTransactions() {
        String uid = "123";
        Long clientId = 1L;
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
        dao.batchInsert(Cf.list(new GroupServiceTransaction(LocalDate.now(), groupService.getId(),
                BigDecimal.valueOf(100), Currency.getInstance("RUB"), BigDecimal.ZERO, Instant.now())));

        ListF<GroupServiceTransaction> transactions = dao.findGroupTransactions(LocalDate.now(), group.getId());

        Assert.assertEquals(1, transactions.length());
    }

    @Test
    public void findClientTransactionsFrom() {
        String uid = "123";
        Long clientId = 1L;
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
        dao.batchInsert(Cf.list(new GroupServiceTransaction(LocalDate.now(), groupService.getId(),
                BigDecimal.valueOf(100), Currency.getInstance("RUB"), BigDecimal.ZERO, Instant.now())));

        ListF<GroupServiceTransaction> transactions = dao.findClientTransactionsFrom(LocalDate.now(),
                clientId);

        Assert.assertEquals(1, transactions.length());
    }

    @Test
    public void findTransactionsClients_empty() {
        ListF<Long> clients = dao.findTransactionsClients(LocalDate.now(), Option.empty(), 1000);
        Assert.assertEquals(0, clients.length());
    }

    @Test
    public void findTransactionsClients_severalClient_oneCurrency() {
        createTransaction(LocalDate.now().minusDays(1), 0L, "RUB");
        createTransaction(LocalDate.now().minusDays(1), 1L, "RUB");
        createTransaction(LocalDate.now(), 2L, "RUB");
        createTransaction(LocalDate.now(), 3L, "RUB");
        ListF<Long> clients =
                dao.findTransactionsClients(LocalDate.now(), Option.empty(), 1);
        Assert.assertEquals(1, clients.length());

        clients = clients.plus(
                dao.findTransactionsClients(LocalDate.now(), Option.of(clients.last()), 1));
        Assert.assertEquals(2, clients.length());
        Assert.assertTrue(clients.containsTs(2L));
        Assert.assertTrue(clients.containsTs(3L));
    }

    @Test
    public void findTransactionsClients_severalClient_severalCurrency() {
        createTransaction(LocalDate.now(), 1L, "RUB");
        createTransaction(LocalDate.now(), 1L, "USD");
        createTransaction(LocalDate.now(), 1L, "GBP");
        createTransaction(LocalDate.now(), 2L, "RUB");

        ListF<Long> clients =
                dao.findTransactionsClients(LocalDate.now(), Option.empty(), 2);
        Assert.assertEquals(2, clients.length());

        Assert.assertTrue(clients.containsTs(1L));
        Assert.assertTrue(clients.containsTs(2L));
    }

    @Test
    public void findTransactionsClients_duplicates() {
        createTransaction(LocalDate.now(), 1L, "RUB");
        createTransaction(LocalDate.now(), 1L, "RUB");

        ListF<Long> clients = dao.findTransactionsClients(LocalDate.now(), Option.empty(), 2);
        Assert.assertEquals(1, clients.length());

        Assert.assertTrue(clients.containsTs(1L));
    }

    @Test
    public void batchInsert_doUpdate() {
        String uid = "123";
        Long clientId = 1L;
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
        dao.batchInsert(Cf.list(new GroupServiceTransaction(LocalDate.now(), groupService.getId(),
                BigDecimal.valueOf(100), Currency.getInstance("RUB"), BigDecimal.valueOf(123), Instant.now())));

        ListF<GroupServiceTransaction> transactions = dao.findGroupTransactions(LocalDate.now(), group.getId());
        Assert.assertEquals(1, transactions.length());
        Assert.assertEquals(BigDecimal.valueOf(100), transactions.get(0).getAmount());
        Assert.assertEquals(Currency.getInstance("RUB"), transactions.get(0).getCurrency());
        Assert.assertEquals(BigDecimal.valueOf(123), transactions.get(0).getUserSecondsCount());

        dao.batchInsert(Cf.list(new GroupServiceTransaction(LocalDate.now(), groupService.getId(),
                BigDecimal.valueOf(200), Currency.getInstance("USD"), BigDecimal.valueOf(321), Instant.now())));

        transactions = dao.findGroupTransactions(LocalDate.now(), group.getId());
        Assert.assertEquals(1, transactions.length());
        Assert.assertEquals(BigDecimal.valueOf(200), transactions.get(0).getAmount());
        Assert.assertEquals(Currency.getInstance("USD"), transactions.get(0).getCurrency());
        Assert.assertEquals(BigDecimal.valueOf(321), transactions.get(0).getUserSecondsCount());
    }

    private void createTransaction(LocalDate date, Long clientId, String currency) {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, "123")));
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
        GroupServiceTransaction transaction = new GroupServiceTransaction(date, groupService.getId(),
                BigDecimal.valueOf(100), Currency.getInstance(currency), BigDecimal.ZERO, Instant.now());

        dao.batchInsert(Cf.list(transaction));
    }

}
