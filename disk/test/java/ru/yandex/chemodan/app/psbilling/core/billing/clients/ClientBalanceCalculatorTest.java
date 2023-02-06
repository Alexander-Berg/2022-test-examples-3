package ru.yandex.chemodan.app.psbilling.core.billing.clients;

import java.math.BigDecimal;
import java.util.Currency;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceCalculator;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceInfo;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.tasks.UpdateClientBalanceTask;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.tasks.UpdateClientTransactionsBalanceTask;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizationStatus;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.model.response.ClientActInfo;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetPartnerBalanceContractResponseItem;
import ru.yandex.chemodan.util.date.DateTimeUtils;

public class ClientBalanceCalculatorTest extends BaseClientBalanceCalculatorTest {

    @Test
    public void calculateClientBalance_no_data() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        Assert.assertFalse(calculator.calculateClientBalanceWithTransactions(clientId)
                .containsKeyTs(groupProduct.getPriceCurrency()));
    }

    @Test
    public void calculateClientBalance() {
        long contractId = 2L;
        DateUtils.freezeTime(new LocalDate(2021, 11, 15)); // берем месяц с 30 днями

        // создаем организацию с 1 пользователем, которая 2 месяца пользуется платной услугой за 300/мес или 10р/день
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()).createdAt(LocalDate.now().withDayOfMonth(1).minusMonths(2)), 1);

        // за 2 месяца выставили акты. Один оплачен целиком, второй пока не полностью
        GetClientContractsResponseItem contract = createContract(clientId, contractId, groupProduct);
        psBillingBalanceFactory.createContractBalance(contract, x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(1000))
                        .withActSum(BigDecimal.valueOf(300 + 300))
                        .withLastActDT(DateTimeUtils.toInstant(
                                psBillingBalanceFactory.getActDt(LocalDate.now().minusMonths(1)))));
        ClientActInfo actInfo1 = psBillingBalanceFactory.createActInfo(contractId, LocalDate.now().minusMonths(2),
                BigDecimal.valueOf((float) 300), BigDecimal.valueOf((float) 300));
        ClientActInfo actInfo2 = psBillingBalanceFactory.createActInfo(contractId, LocalDate.now().minusMonths(1),
                BigDecimal.valueOf((float) 300), BigDecimal.valueOf((float) 290));
        balanceClientStub.createOrReplaceClientActs(clientId, Cf.list(actInfo1, actInfo2));

        DateUtils.shiftTime(Duration.standardDays(1));
        psBillingTransactionsFactory.calculateTransactions(
                LocalDate.now().withDayOfMonth(1).minusMonths(2), LocalDate.now());

        ClientBalanceInfo balanceInfo =
                calculator.calculateClientBalanceWithTransactions(clientId).getTs(groupProduct.getPriceCurrency());

        Assert.assertEquals(clientId, balanceInfo.getClientId());
        Assert.assertEquals(groupProduct.getPriceCurrency(), balanceInfo.getCurrency());
        assertEquals(1000, balanceInfo.getIncomeSum());
        assertEquals(600, balanceInfo.getActSum());
        // за текущий месяц половинка цены
        assertEquals(150, balanceInfo.getTransactionsSum());
        assertEquals(1000 - 300 - 300 - 150, balanceInfo.getBalance());
        Assert.assertEquals(Option.of(actInfo2.getDt()), balanceInfo.getLastActDate());
    }

    @Test
    public void calculateVoidDate_noServices() {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));

        ClientBalanceInfo balanceInfo = new ClientBalanceInfo(clientId, groupProduct.getPriceCurrency());
        Option<Instant> voidDate = calculator.calculateVoidDate(balanceInfo, Option.empty());
        AssertHelper.assertEquals(voidDate.get(), Instant.now());
    }

    @Test
    public void calculateVoidDate_noServicesWithPositiveBalance() {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));

        ClientBalanceInfo balanceInfo = new ClientBalanceInfo(clientId, groupProduct.getPriceCurrency());
        balanceInfo.setIncomeSum(BigDecimal.TEN);
        Option<Instant> voidDate = calculator.calculateVoidDate(balanceInfo, Option.empty());
        AssertHelper.assertTrue(voidDate.isEmpty());
    }

    @Test
    public void calculateVoidDate_onlyFreeService() {
        Option<Instant> voidDate = calculateVoidDate(rub, 0, 1, createBalance(1L, 1, rub));
        Assert.assertEquals(Option.empty(), voidDate);
    }

    @Test
    public void calculateVoidDate_noServicesWithCurrency() {
        Option<Instant> voidDate = calculateVoidDate(rub, 300, 1, createBalance(1L, 0,
                Currency.getInstance("GBP")));
        AssertHelper.assertEquals(voidDate.get(), Instant.now());
    }

    @Test
    public void calculateVoidDate_voidAtCurMonth() {
        DateUtils.freezeTime(new LocalDate(2021, 11, 1)); // берем месяц с 30 днями

        Option<Instant> voidDate = calculateVoidDate(rub, 300, 1, createBalance(1L, 150,
                rub));
        Assert.assertEquals(Option.of(Instant.now().plus(Duration.standardDays(15))), voidDate);
    }

    @Test
    public void calculateVoidDate_voidAtNextMonth() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями

        Option<Instant> voidDate = calculateVoidDate(rub, 300, 1, createBalance(1L, 300,
                rub));
        // 150 rub to end of month. 150rubs for next month is half period
        Instant expectedDate = new DateTime(2021, 12, 16, 12, 0, 0).toInstant();
        Assert.assertEquals(Option.of(expectedDate), voidDate);
    }

    @Test
    public void calculateVoidDate_voidOverNextMonth() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями
        Option<Instant> voidDate = calculateVoidDate(rub, 300, 1, createBalance(1L, 600,
                rub));
        // 150 rub nov, 300 dec, 150 jan
        Instant expectedDate = new DateTime(2022, 1, 16, 12, 0, 0).toInstant();
        Assert.assertEquals(Option.of(expectedDate), voidDate);
    }

    @Test
    public void calculateVoidDate_severalUsers() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));

        createGroupService(group, groupProduct, 2);

        ClientBalanceInfo balanceInfo = new ClientBalanceInfo(clientId, groupProduct.getPriceCurrency());
        balanceInfo.setIncomeSum(BigDecimal.valueOf(1200));
        Option<Instant> voidDate = calculator.calculateVoidDate(balanceInfo, Option.empty());
        // 300 rub nov, 600 dec, 300 jan
        Instant expectedDate = new DateTime(2022, 1, 16, 12, 0, 0).toInstant();
        Assert.assertEquals(Option.of(expectedDate), voidDate);
    }

    @Test
    public void calculateVoidDate_severalServices() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями

        Group group1 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        Group group2 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));
        // 2 active
        psBillingTransactionsFactory.createService(group1,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()).createdAt(DateUtils.farPastDateLd())
                , 1);
        psBillingTransactionsFactory.createService(group2,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()).createdAt(DateUtils.pastDateLd()), 1);
        // 1 disabled
        psBillingTransactionsFactory.createService(group1,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode())
                        .createdAt(DateUtils.farPastDateLd()).disabledAt(DateUtils.pastDateLd()), 1);

        ClientBalanceInfo balanceInfo = new ClientBalanceInfo(clientId, groupProduct.getPriceCurrency());
        balanceInfo.setIncomeSum(BigDecimal.valueOf(1200));
        Option<Instant> voidDate = calculator.calculateVoidDate(balanceInfo, Option.empty());
        // 300 rub nov, 600 dec, 300 jan
        Instant expectedDate = new DateTime(2022, 1, 16, 12, 0, 0).toInstant();
        Assert.assertEquals(Option.of(expectedDate), voidDate);
    }

    @Test
    public void calculateVoidDate_serviceInInit() {
        DateUtils.freezeTime(new DateTime(2021, 11, 1, 0, 0, 0)); // берем месяц с 30 днями

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));

        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
        Assert.assertEquals(SynchronizationStatus.INIT, groupService.getStatus());

        // mock users in directory
        int userCount = 1;
        ListF<Long> uids = Cf.range(0, userCount).map(x -> (long) x + 1);
        Mockito.when(directoryClient.usersInOrganization(Mockito.eq(group.getExternalId()),
                        Mockito.anyBoolean(), Mockito.anyInt(),
                        Mockito.any()))
                .thenReturn(uids.toLinkedHashSet());

        ClientBalanceInfo balanceInfo = new ClientBalanceInfo(clientId, groupProduct.getPriceCurrency());
        balanceInfo.setIncomeSum(BigDecimal.valueOf(300));
        Option<Instant> voidDate = calculator.calculateVoidDate(balanceInfo, Option.empty());
        Instant expectedDate = new DateTime(2021, 12, 1, 0, 0, 0).toInstant();
        Assert.assertEquals(Option.of(expectedDate), voidDate);
    }

    @Test
    public void updateClientBalance() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.pricePerUserInMonth(BigDecimal.valueOf(300))
                        .paymentType(GroupPaymentType.PREPAID));

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        // 1 enabled
        GroupService enabledService = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()).createdAt(DateUtils.farPastDateLd())
                , 1);
        // 1 disabled
        GroupService disabledService = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()).createdAt(
                        DateUtils.farPastDateLd()).disabledAt(DateUtils.pastDateLd()), 1);
        Instant disabledServiceBdate = groupServiceDao.findById(disabledService.getId()).getNextBillingDate().get();
        GetClientContractsResponseItem contract = createContract(clientId, 2L, groupProduct);
        psBillingBalanceFactory.createContractBalance(contract, x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(600)));

        calculator.updateClientBalance(group.getPaymentInfo().get().getClientId(), groupProduct.getPriceCurrency());
        ClientBalanceEntity clientBalance = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(),
                groupProduct.getPriceCurrency()).get();

        // 150 rub nov, 300 dec, 150 jan
        Instant expectedDate = new DateTime(2022, 1, 16, 12, 0, 0).toInstant();

        Assert.assertEquals(clientId, clientBalance.getClientId());
        assertEquals(600, clientBalance.getBalanceAmount());
        Assert.assertEquals(groupProduct.getPriceCurrency(), clientBalance.getBalanceCurrency());
        Assert.assertEquals(expectedDate, clientBalance.getBalanceVoidAt().get());
        Assert.assertEquals(Instant.now(), clientBalance.getBalanceUpdatedAt());

        Assert.assertEquals(expectedDate, groupServiceDao.findById(enabledService.getId()).getNextBillingDate().get());
        Assert.assertEquals(disabledServiceBdate,
                groupServiceDao.findById(disabledService.getId()).getNextBillingDate().get());
    }

    @Test
    public void updateClientBalanceForDisabledService() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.pricePerUserInMonth(BigDecimal.valueOf(300))
                        .paymentType(GroupPaymentType.PREPAID));
        Instant oldVoidDate = Instant.now();
        int balance = 0;
        psBillingBalanceFactory.createBalance(clientId, rub, balance, oldVoidDate);
        GetClientContractsResponseItem contract = createContract(clientId, 2L, groupProduct);
        psBillingBalanceFactory.createContractBalance(contract, x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(balance)));

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        GroupService disabledService = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()).createdAt(
                        DateUtils.farPastDateLd()).disabledAt(DateUtils.pastDate()), 1);
        groupServiceDao.updateNextBillingDate(disabledService.getId(), Option.of(oldVoidDate));
        disabledService = groupServiceDao.findById(disabledService.getId());
        psBillingGroupsFactory.createDefaultProductService(group);
        Assert.assertEquals(oldVoidDate, disabledService.getNextBillingDate().get());

        DateUtils.shiftTime(Duration.standardHours(1));
        calculator.updateClientBalance(group.getPaymentInfo().get().getClientId(), groupProduct.getPriceCurrency());

        ClientBalanceEntity clientBalance = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(),
                groupProduct.getPriceCurrency()).get();

        Assert.assertEquals(clientId, clientBalance.getClientId());
        AssertHelper.assertEquals(clientBalance.getBalanceAmount(), balance);
        Assert.assertEquals(groupProduct.getPriceCurrency(), clientBalance.getBalanceCurrency());
        Assert.assertTrue(clientBalance.getBalanceVoidAt().isPresent());
        Assert.assertEquals(oldVoidDate, clientBalance.getBalanceVoidAt().get());
        Assert.assertEquals(Instant.now(), clientBalance.getBalanceUpdatedAt());

        Assert.assertEquals(oldVoidDate, groupServiceDao.findById(disabledService.getId()).getNextBillingDate().get());
    }

    @Test
    public void clientBalanceRespectTransactions() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями

        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingTransactionsFactory.createGroup(clientId,
                Cf.list(new PsBillingTransactionsFactory.Service(groupProduct.getCode())), 2);
        psBillingBalanceFactory.createContractBalance(createContract(clientId, 2L, groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(0)));
        // 10*2 rub
        LocalDate now = LocalDate.now();
        DateUtils.shiftTime(Duration.standardDays(1));
        groupServiceTransactionsCalculationService.performSyncCalculation(now, true);

        Assert.assertEquals(1, bazingaTaskManagerStub.findTasks(UpdateClientTransactionsBalanceTask.class).length());
        bazingaTaskManagerStub.executeTasks(UpdateClientTransactionsBalanceTask.class);
        Assert.assertEquals(1, bazingaTaskManagerStub.findTasks(UpdateClientBalanceTask.class).length());
        bazingaTaskManagerStub.executeTasks(UpdateClientBalanceTask.class);

        ClientBalanceEntity clientBalance = clientBalanceDao.find(clientId, groupProduct.getPriceCurrency()).get();
        Assert.assertEquals(clientId, clientBalance.getClientId());
        assertEquals(-20, clientBalance.getBalanceAmount());
        Assert.assertEquals(groupProduct.getPriceCurrency(), clientBalance.getBalanceCurrency());
        Assert.assertEquals(Instant.now(), clientBalance.getBalanceVoidAt().get());
        Assert.assertEquals(Instant.now(), clientBalance.getCreatedAt());
        Assert.assertEquals(Instant.now(), clientBalance.getBalanceUpdatedAt());
    }

    @Test
    public void scheduleUpdateObsoleteClientBalances() {
        Instant now = Instant.now();
        psBillingBalanceFactory.createBalance(1L, "RUB", now.minus(Duration.standardDays(2)));
        psBillingBalanceFactory.createBalance(1L, "USD", now.minus(Duration.standardDays(2)));

        psBillingBalanceFactory.createBalance(2L, "RUB", now.minus(Duration.standardDays(2)));
        psBillingBalanceFactory.createBalance(3L, "USD", now);
        psBillingBalanceFactory.createBalance(4L, "RUB", now.minus(Duration.standardHours(12)));

        ClientBalanceEntity balance = psBillingBalanceFactory.createBalance(5L, "USD",
                now.plus(Duration.standardHours(12)));
        clientBalanceDao.updateInvoiceDate(balance.getId(), Option.of(now.minus(Duration.standardDays(2))));

        balance =
                psBillingBalanceFactory.createBalance(6L, "USD", now.minus(Duration.standardHours(12)));
        clientBalanceDao.updateInvoiceDate(balance.getId(), Option.of(now.minus(Duration.standardDays(8))));

        calculator.scheduleUpdateObsoleteClientBalances();
        ListF<UpdateClientBalanceTask> tasks =
                bazingaTaskManagerStub.findTasks(UpdateClientBalanceTask.class).map(x -> x._1);
        Assert.assertEquals(3, tasks.length());
        Assert.assertEquals(1, tasks.filter(x -> x.getParametersTyped().getClientId().equals(1L)).length());
        Assert.assertEquals(1, tasks.filter(x -> x.getParametersTyped().getClientId().equals(2L)).length());
        Assert.assertEquals(1, tasks.filter(x -> x.getParametersTyped().getClientId().equals(5L)).length());
    }

    @Test
    public void scheduleUpdateObsoleteClientBalances_updatesToNotDisableUpdateTasks() {
        Instant now = Instant.now();
        ClientBalanceEntity balance =
                psBillingBalanceFactory.createBalance(1L, "RUB", now.minus(Duration.standardDays(2)));

        calculator.scheduleUpdateObsoleteClientBalances();
        assertUpdateClientBalanceTaskLength(1);
        bazingaTaskManagerStub.reset();

        // updateInvoiceDate doesn't affect balance
        clientBalanceDao.updateInvoiceDate(balance.getId(), Option.of(now.minus(Duration.standardDays(100))));
        calculator.scheduleUpdateObsoleteClientBalances();
        assertUpdateClientBalanceTaskLength(1);
        bazingaTaskManagerStub.reset();

        // updateVoidAt doesn't affect balance
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(now.minus(Duration.standardDays(100))));
        calculator.scheduleUpdateObsoleteClientBalances();
        assertUpdateClientBalanceTaskLength(1);
    }

    @Test
    public void scheduleUpdateObsoleteClientBalances_endlessUpdate() {
        Instant now = Instant.now();
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                false)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()).createdAt(DateUtils.farPastDateLd())
                , 1);
        ClientBalanceEntity balance = psBillingBalanceFactory.createBalance(clientId, "RUB",
                now.minus(Duration.standardDays(100)));

        GetClientContractsResponseItem contract = createContract(clientId, 2L, groupProduct);
        psBillingBalanceFactory.createContractBalance(contract, x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(600)));

        calculator.scheduleUpdateObsoleteClientBalances();
        assertUpdateClientBalanceTaskLength(1);

        bazingaTaskManagerStub.executeTasks(UpdateClientBalanceTask.class);
        ClientBalanceEntity updatedBalance = clientBalanceDao.find(balance.getClientId(),
                balance.getBalanceCurrency()).get();
        Assert.assertNotEquals(updatedBalance, balance);
        calculator.scheduleUpdateObsoleteClientBalances();
        assertUpdateClientBalanceTaskLength(0);
    }

    @Test
    // https://st.yandex-team.ru/CHEMODAN-81132
    public void clientBalanceRespectTransactions_withTimeShift() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями

        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingTransactionsFactory.createGroup(clientId,
                Cf.list(new PsBillingTransactionsFactory.Service(groupProduct.getCode())), 1);
        psBillingBalanceFactory.createContractBalance(createContract(clientId, 2L, groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(20)));

        Instant startDate = Instant.now();
        // now 18.11.2021 00:00
        Instant expectedVoidDate = startDate.plus(Duration.standardDays(2));
        // now 16.11.2021 00:00
        assertVoidDate(clientId, groupProduct.getPriceCurrency(), expectedVoidDate);

        // calc transactions for yesterday
        // now 17.11.2021 00:00
        DateUtils.shiftTime(Duration.standardHours(24));
        // emulate transactions calculation flag
        // now 17.11.2021 01:00
        DateUtils.shiftTime(Duration.standardHours(1));
        groupServiceTransactionsCalculationService.performSyncCalculation(startDate.toDateTime().toLocalDate(), true);

        // emulate balance calculation lag
        // now 17.11.2021 02:00
        DateUtils.shiftTime(Duration.standardHours(1));
        assertVoidDate(clientId, groupProduct.getPriceCurrency(), expectedVoidDate);

        // calc transactions for today
        // now 17.11.2021 12:00
        DateUtils.shiftTime(Duration.standardHours(10));
        groupServiceTransactionsCalculationService.performSyncCalculation(LocalDate.now(), true);
        assertVoidDate(clientId, groupProduct.getPriceCurrency(), expectedVoidDate);

        // even if transactions are not fresh - all is ok
        // now 17.11.2021 13:00
        DateUtils.shiftTime(Duration.standardHours(1));
        assertVoidDate(clientId, groupProduct.getPriceCurrency(), expectedVoidDate);
    }

    @Test
    // https://st.yandex-team.ru/CHEMODAN-81132
    public void updateClientBalance_NegativeBalance() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями

        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingTransactionsFactory.createGroup(clientId,
                Cf.list(new PsBillingTransactionsFactory.Service(groupProduct.getCode())), 1);
        GetPartnerBalanceContractResponseItem contractBalance =
                psBillingBalanceFactory.createContractBalance(createContract(clientId, 2L, groupProduct), x ->
                        x.withClientPaymentsSum(BigDecimal.valueOf(5)));

        Instant now = Instant.now();
        Instant expectedVoidDate = now.plus(Duration.standardHours(12));
        assertVoidDate(clientId, groupProduct.getPriceCurrency(), expectedVoidDate);

        // emulate overbilling
        psBillingBalanceFactory.updateContractBalance(contractBalance.withClientPaymentsSum(BigDecimal.valueOf(2)));

        DateUtils.freezeTime(expectedVoidDate);
        DateUtils.shiftTime(Duration.standardHours(1)); // emulate not sync update transactions
        groupServiceTransactionsCalculationService.performSyncCalculation(now.toDateTime().toLocalDate(), true);

        DateUtils.shiftTime(Duration.standardHours(1)); // emulate not sync update void date
        // ensure not changed
        assertVoidDate(clientId, groupProduct.getPriceCurrency(), expectedVoidDate);
    }

    @Test
    // https://st.yandex-team.ru/CHEMODAN-81358
    public void calculateClientBalance_hasBadDebt() {
        DateUtils.freezeTime(new DateTime(2021, 11, 16, 0, 0, 0)); // берем месяц с 30 днями

        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingTransactionsFactory.createGroup(clientId,
                Cf.list(new PsBillingTransactionsFactory.Service(groupProduct.getCode())), 1);
        long contractId = 2L;
        psBillingBalanceFactory.createContractBalance(createContract(clientId, contractId, groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(1000))
                        .withActSum(BigDecimal.valueOf(300 + 200))
                        .withLastActDT(DateTimeUtils.toInstant(
                                psBillingBalanceFactory.getActDt(LocalDate.now().minusMonths(1)))));

        ClientActInfo actInfo1 = psBillingBalanceFactory.createActInfo(contractId, LocalDate.now().minusMonths(2),
                BigDecimal.valueOf((float) 300), BigDecimal.valueOf((float) 100));
        actInfo1.setBadDebts(Cf.list(new ClientActInfo.BadDebt(false, "not yandex fault")));

        ClientActInfo actInfo2 = psBillingBalanceFactory.createActInfo(contractId, LocalDate.now().minusMonths(1),
                BigDecimal.valueOf((float) 500), BigDecimal.valueOf((float) 200));
        actInfo2.setBadDebts(Cf.list(new ClientActInfo.BadDebt(true, "yandex fault")));
        balanceClientStub.createOrReplaceClientActs(clientId, Cf.list(actInfo1, actInfo2));

        ClientBalanceInfo balanceInfo =
                calculator.calculateClientBalanceWithTransactions(clientId).getTs(groupProduct.getPriceCurrency());

        Assert.assertEquals(clientId, balanceInfo.getClientId());
        Assert.assertEquals(groupProduct.getPriceCurrency(), balanceInfo.getCurrency());
        assertEquals(1000, balanceInfo.getIncomeSum());
        assertEquals(300 + 200, balanceInfo.getActSum());
        // за текущий месяц половинка цены
        assertEquals(1000 - (300 + 200), balanceInfo.getBalance());
        Assert.assertEquals(Option.of(actInfo2.getDt()), balanceInfo.getLastActDate());
    }

    @Test
    // https://st.yandex-team.ru/CHEMODAN_82861
    public void calculatePricePeriods_noPeriodForPast() {
        Instant milestone1 = DateUtils.freezeTime("2021-01-01");
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product =
                psBillingProductsFactory.createGroupProduct(x -> x.pricePerUserInMonth(BigDecimal.valueOf(100)));
        GroupService service1 = createGroupService(group, product, 1);
        Instant milestone2 = DateUtils.shiftTime(Duration.standardHours(1));

        GroupService service2 = createGroupService(group, product, 1);
        ListF<ClientBalanceCalculator.PricePeriod> pricePeriods =
                calculator.calculatePricePeriods(DateUtils.pastDate(), Cf.list(service1, service2), rub);

        AssertHelper.assertSize(pricePeriods, 2);

        AssertHelper.assertEquals(pricePeriods.get(0).getFrom(), milestone1);
        AssertHelper.assertEquals(pricePeriods.get(0).getTo(), milestone2);
        AssertHelper.assertEquals(pricePeriods.get(0).getPrice(), BigDecimal.valueOf(100));

        AssertHelper.assertEquals(pricePeriods.get(1).getFrom(), milestone2);
        AssertHelper.assertEquals(pricePeriods.get(1).getTo(), DateTimeUtils.MAX_INSTANT);
        AssertHelper.assertEquals(pricePeriods.get(1).getPrice(), BigDecimal.valueOf(200));
    }

    private void assertUpdateClientBalanceTaskLength(int count) {
        ListF<UpdateClientBalanceTask> tasks = findUpdateClientBalanceTasks();
        Assert.assertEquals(count, tasks.length());
    }

    private ListF<UpdateClientBalanceTask> findUpdateClientBalanceTasks() {
        return bazingaTaskManagerStub.findTasks(UpdateClientBalanceTask.class).map(x -> x._1);
    }

    private Option<Instant> calculateVoidDate(Currency productCurrency, int pricePerMonth,
                                              int userCount, ClientBalanceInfo balanceInfo) {
        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(balanceInfo.getClientId(), uid, false)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(pricePerMonth)).priceCurrency(productCurrency));
        createGroupService(group, groupProduct, userCount);
        return calculator.calculateVoidDate(balanceInfo, Option.empty());
    }
}
