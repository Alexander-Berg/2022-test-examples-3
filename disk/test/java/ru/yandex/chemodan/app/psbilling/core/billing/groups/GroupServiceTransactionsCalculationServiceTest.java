package ru.yandex.chemodan.app.psbilling.core.billing.groups;

import java.math.BigDecimal;

import lombok.SneakyThrows;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractGroupServicesTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupServiceTransaction;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;

import static ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory.DEFAULT_CLIENT_ID;
import static ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory.DEFAULT_BALANCE_PRODUCT_NAME;
import static ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory.DEFAULT_CURRENCY;

public class GroupServiceTransactionsCalculationServiceTest extends AbstractGroupServicesTest {
    private Group group;
    private FeatureEntity feature;
    private GroupService groupService;
    private GroupProduct groupProduct;

    @Autowired
    private GroupServiceTransactionsCalculationService groupServiceTransactionsCalculationService;
    @Autowired
    private GroupServiceTransactionsDao groupServiceTransactionsDao;
    @Autowired
    private PsBillingTransactionsFactory psBillingTransactionsFactory;

    @Test
    public void calculateGroupServicesTransactions() {
        LocalDate now = LocalDate.now();
        setUserCount(group, 2);

        DateUtils.shiftTime(Duration.standardDays(1));
        ListF<GroupServiceTransactionsDao.ExportRow> transactions = calculate(now);
        Assert.assertEquals(1, transactions.size());

        GroupServiceTransactionsDao.ExportRow transaction = transactions.first();
        Assert.assertEquals(String.valueOf(DEFAULT_CLIENT_ID), transaction.getClientId());
        Assert.assertEquals(DEFAULT_BALANCE_PRODUCT_NAME, transaction.getBalanceProductName());
        Assert.assertEquals(groupService.getId(), transaction.getGroupServiceTransaction().getGroupServiceId());
        assertAmountEquals(groupService, transactions, BigDecimal.valueOf(200));
        Assert.assertEquals(now, transaction.getGroupServiceTransaction().getBillingDate());
        Assert.assertEquals(DEFAULT_CURRENCY, transaction.getGroupServiceTransaction().getCurrency());
    }

    @Test
    public void calculateGroupServicesTransactions__postPaidDisabled() {
        LocalDate now = LocalDate.now();
        setUserCount(group, 2);
        DateUtils.shiftTime(Duration.standardHours(12));
        groupServiceDao.setTargetToDisabled(groupService.getId(), Target.ENABLED);

        DateUtils.shiftTime(Duration.standardHours(12));
        ListF<GroupServiceTransactionsDao.ExportRow> transactions = calculate(now);
        Assert.assertEquals(1, transactions.size());

        assertAmountEquals(groupService, transactions, BigDecimal.valueOf(100));
    }

    @Test
    public void calculateGroupServicesTransactionsWithSkipExportTest() {
        // делаем так, чтобы один человекодень стоил 1 рубль
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(b -> b
                .pricePerUserInMonth(new BigDecimal(LocalDate.now().dayOfMonth().getMaximumValue()))
                .skipTransactionsExport(true)
        );
        psBillingProductsFactory.createProductFeature(groupProduct.getUserProduct().getId(), feature);
        group = psBillingGroupsFactory.createGroup();
        psBillingGroupsFactory.createGroupService(group, groupProduct);
        setUserCount(group, 2);

        //пробум посчить за завтра, чтобы был полный день
        ListF<GroupServiceTransactionsDao.ExportRow> transactions = calculate(LocalDate.now().plusDays(1));

        Assert.assertEquals(0, transactions.size());
    }

    @Test
    public void testCalculationWithPriceOverrides() {
        LocalDate now = LocalDate.now();
        // делаем так, чтобы один человекодень стоил 1 рубль
        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(c ->
                c.price(BigDecimal.ZERO).durationMeasurement(Option.of(CustomPeriodUnit.ONE_HOUR))
                        .duration(Option.of(12)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(
                b -> b.pricePerUserInMonth(new BigDecimal(now.dayOfMonth().getMaximumValue()))
                        .trialDefinitionId(Option.of(trialDefinition.getId())));
        psBillingProductsFactory.createProductFeature(groupProduct.getUserProductId(), feature);

        Group group = psBillingGroupsFactory.createGroup();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
        setUserCount(group, 2);

        DateUtils.shiftTime(Duration.standardDays(1));
        ListF<GroupServiceTransactionsDao.ExportRow> transactions = calculate(now);

        Assert.assertEquals(1, transactions.size());

        // да поьльзователя ценой по 1 рубль в день, но триал на полдня. итого за первый день - 1 рубль
        GroupServiceTransactionsDao.ExportRow transaction = transactions.first();
        Assert.assertEquals(String.valueOf(DEFAULT_CLIENT_ID), transaction.getClientId());
        Assert.assertEquals(DEFAULT_BALANCE_PRODUCT_NAME, transaction.getBalanceProductName());
        Assert.assertEquals(groupService.getId(), transaction.getGroupServiceTransaction().getGroupServiceId());
        Assert.assertEquals(0, new BigDecimal(1).compareTo(transaction.getGroupServiceTransaction().getAmount()));
        Assert.assertEquals(now, transaction.getGroupServiceTransaction().getBillingDate());
        Assert.assertEquals(DEFAULT_CURRENCY, transaction.getGroupServiceTransaction().getCurrency());
    }

    @Test
    public void userSecondCount() {
        // 2 группа для проверки непересечения расчетов между группасм
        Group group2 = psBillingGroupsFactory.createGroup();
        psBillingGroupsFactory.createGroupService(group2, groupProduct);
        setUserCount(group2, 10);

        // 2 пользователя на 12 часов = 100 рублей. 86400 секунд
        setUserCount(group, 2);
        DateUtils.shiftTime(Duration.standardHours(12));

        // 10 пользователей на 6 часов = 250 рублей. 216000 секунд
        setUserCount(group, 10);
        DateUtils.shiftTime(Duration.standardHours(6));

        // 3 пользователя на 6 часов = 75 рублей. 64800 секунд
        setUserCount(group, 3);
        DateUtils.shiftTime(Duration.standardHours(6));

        ListF<GroupServiceTransactionsDao.ExportRow> transactions = calculate(LocalDate.parse("2021-07-01"));
        assertAmountEquals(groupService, transactions, new BigDecimal(100 + 250 + 75));
        assertUserCountEquals(groupService, transactions, new BigDecimal(86400 + 216000 + 64800));
    }

    @Test
    public void findGroupTransactions() {
        setUserCount(group, 1);
        DateUtils.freezeTime(new LocalDate(2022, 1, 1));
        calculate(new LocalDate(2021, 6, 30));
        calculate(new LocalDate(2021, 7, 15));
        calculate(new LocalDate(2021, 8, 1));

        ListF<GroupServiceTransaction> groupTransactions =
                groupServiceTransactionsDao.findGroupTransactions(new LocalDate(2021, 7, 15), group.getId());

        Assert.assertEquals(1, groupTransactions.size());
    }

    @Test
    public void prepaid_calculateGroupServicesTransactions() {
        DateUtils.freezeTime(LocalDate.parse("2021-07-01"));
        LocalDate now = LocalDate.now();
        long clientId = 1L;
        // делаем так, чтобы один человекодень стоил 100 рублей
        groupProduct = psBillingProductsFactory.createGroupProduct(
                b -> b.pricePerUserInMonth(new BigDecimal(LocalDate.now().dayOfMonth().getMaximumValue() * 100))
                        .paymentType(GroupPaymentType.PREPAID));

        group = psBillingTransactionsFactory.createGroup(clientId,
                Cf.list(new PsBillingTransactionsFactory.Service(groupProduct.getCode())), 2);
        // считаем что денег хватает на пол-дня - ожидаем что накрутим 100р - по 50 на каждого человека,
        // несмотря на то, что по факту услуга была весь день
        groupServiceDao.updatePrepaidNextBillingDateForClient(clientId, groupProduct.getPriceCurrency(),
                Option.of(Instant.now().plus(Duration.standardHours(12))));

        DateUtils.shiftTime(Duration.standardDays(1));
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                psBillingTransactionsFactory.calculateTransactions(now, LocalDate.now());
        // ожидаем открутку только за сегодня. За последующие дни откруток быть не должно
        Assert.assertEquals(1, transactions.size());

        GroupServiceTransactionsDao.ExportRow transaction = transactions.first();
        Assert.assertEquals(String.valueOf(clientId), transaction.getClientId());
        Assert.assertEquals(100, transaction.getGroupServiceTransaction().getAmount().floatValue(), 0.01);
        Assert.assertEquals(now, transaction.getGroupServiceTransaction().getBillingDate());
        Assert.assertEquals(groupProduct.getPriceCurrency(), transaction.getGroupServiceTransaction().getCurrency());
    }

    @Test
    public void calculateClientTransactions_forClient() {
        DateUtils.freezeTime(LocalDate.parse("2021-07-01"));
        long clientId = 1L;
        // делаем так, чтобы один человекодень стоил 100 рублей
        groupProduct = psBillingProductsFactory.createGroupProduct(
                b -> b.pricePerUserInMonth(new BigDecimal(LocalDate.now().dayOfMonth().getMaximumValue() * 100))
                        .paymentType(GroupPaymentType.PREPAID));

        group = psBillingTransactionsFactory.createGroup(clientId,
                Cf.list(new PsBillingTransactionsFactory.Service(groupProduct.getCode())), 1);
        Group group2 = psBillingTransactionsFactory.createGroup(2L,
                Cf.list(new PsBillingTransactionsFactory.Service(groupProduct.getCode())), 2);

        setUserCount(group, 1);
        groupServiceDao.updatePrepaidNextBillingDateForClient(clientId, groupProduct.getPriceCurrency(),
                Option.of(Instant.now().plus(Duration.standardDays(7))));

        DateUtils.shiftTime(Duration.standardHours(12));
        groupServiceTransactionsCalculationService.calculateClientTransactions(1L, LocalDate.now());
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                groupServiceTransactionsDao.findTransactions(LocalDate.now(), Option.empty(), 100000);
        Assert.assertEquals(1, transactions.size());

        GroupServiceTransactionsDao.ExportRow transaction = transactions.first();
        Assert.assertEquals(String.valueOf(clientId), transaction.getClientId());
        Assert.assertEquals(50, transaction.getGroupServiceTransaction().getAmount().floatValue(), 0.01);
        Assert.assertEquals(LocalDate.now(), transaction.getGroupServiceTransaction().getBillingDate());
        Assert.assertEquals(groupProduct.getPriceCurrency(), transaction.getGroupServiceTransaction().getCurrency());
    }

    @Test
    public void calculationOnNotCleanData() {
        setUserCount(group, 1);
        ListF<GroupServiceTransactionsDao.ExportRow> rows1 =
                psBillingTransactionsFactory.calculateTransactions(LocalDate.now());

        ListF<GroupServiceTransactionsDao.ExportRow> rows2 =
                psBillingTransactionsFactory.calculateTransactions(LocalDate.now());
        Assert.assertEquals(rows1, rows2);

        // make half price for service in transaction
        DateUtils.shiftTime(Duration.standardHours(12));
        groupServiceDao.setTargetToDisabled(groupService.getId(), Target.ENABLED);
        actualizeFeature(group, feature);

        ListF<GroupServiceTransactionsDao.ExportRow> rows3 =
                psBillingTransactionsFactory.calculateTransactions(LocalDate.now());
        Assert.assertNotEquals(rows1, rows3);
        Assert.assertEquals(1, rows3.length());
        Assert.assertEquals(50, rows3.get(0).getGroupServiceTransaction().getAmount().intValue());
    }

    @Test
    public void calculateGroupServicesTransactions__today() {
        DateUtils.freezeTime(LocalDate.parse("2021-07-01"));
        long clientId = 1L;
        // делаем так, чтобы один человекодень стоил 100 рублей
        groupProduct = psBillingProductsFactory.createGroupProduct(
                b -> b.pricePerUserInMonth(new BigDecimal(LocalDate.now().dayOfMonth().getMaximumValue() * 100))
                        .paymentType(GroupPaymentType.PREPAID));

        group = psBillingTransactionsFactory.createGroup(clientId,
                Cf.list(new PsBillingTransactionsFactory.Service(groupProduct.getCode())), 1);

        Instant bdate = Instant.now().plus(Duration.standardHours(12));
        groupServiceDao.updatePrepaidNextBillingDateForClient(clientId, groupProduct.getPriceCurrency(),
                Option.of(bdate));

        DateUtils.freezeTime(bdate.plus(Duration.standardHours(1)));
        groupServiceTransactionsCalculationService.calculateClientTransactions(1L, LocalDate.now());
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                groupServiceTransactionsDao.findTransactions(LocalDate.now(), Option.empty(), 100000);
        Assert.assertEquals(1, transactions.size());

        GroupServiceTransactionsDao.ExportRow transaction = transactions.first();
        Assert.assertEquals(String.valueOf(clientId), transaction.getClientId());
        Assert.assertEquals(50, transaction.getGroupServiceTransaction().getAmount().floatValue(), 0.01);
        Assert.assertEquals(LocalDate.now(), transaction.getGroupServiceTransaction().getBillingDate());
        Assert.assertEquals(groupProduct.getPriceCurrency(), transaction.getGroupServiceTransaction().getCurrency());
        Assert.assertEquals(bdate, transaction.getGroupServiceTransaction().getCalculatedAt());
    }

    @Before
    public void setup() {
        DateUtils.freezeTime(LocalDate.parse("2021-07-01"));

        group = psBillingGroupsFactory.createGroup();

        // делаем так, чтобы один человекодень стоил 100 рублей
        groupProduct = psBillingProductsFactory.createGroupProduct(
                b -> b.pricePerUserInMonth(new BigDecimal(LocalDate.now().dayOfMonth().getMaximumValue() * 100)));
        UserProduct userProduct = groupProduct.getUserProduct();

        feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
        psBillingProductsFactory.createProductFeature(userProduct.getId(), feature);
        groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
    }

    @SneakyThrows
    private void setUserCount(Group group, int userCount) {
        actualizeFeature(group, feature, userCount);
    }

    private ListF<GroupServiceTransactionsDao.ExportRow> calculate(LocalDate date) {
        groupServiceTransactionsCalculationService.performSyncCalculation(date, true);
        return groupServiceTransactionsDao.findTransactions(date, Option.empty(), 100000);
    }

    private void assertAmountEquals(GroupService groupService,
                                    ListF<GroupServiceTransactionsDao.ExportRow> transactions, BigDecimal expected) {
        GroupServiceTransactionsDao.ExportRow transaction =
                transactions.find(x -> x.getGroupServiceTransaction().getGroupServiceId().equals(groupService.getId())).get();
        BigDecimal actual = transaction.getGroupServiceTransaction().getAmount();
        if (expected.compareTo(actual) != 0) {
            Assert.fail(String.format("amount expected %s but found %s", expected, actual));
        }
    }

    private void assertUserCountEquals(GroupService groupService,
                                       ListF<GroupServiceTransactionsDao.ExportRow> transactions, BigDecimal expected) {
        GroupServiceTransactionsDao.ExportRow transaction =
                transactions.find(x -> x.getGroupServiceTransaction().getGroupServiceId().equals(groupService.getId())).get();
        BigDecimal actual = transaction.getGroupServiceTransaction().getUserSecondsCount();
        if (expected.compareTo(actual) != 0) {
            Assert.fail(String.format("user count expected %s but found %s", expected, actual));
        }
    }
}
