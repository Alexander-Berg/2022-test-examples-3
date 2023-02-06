package ru.yandex.chemodan.app.psbilling.core.billing.clients;

import java.math.BigDecimal;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;

public class ClientBalanceCalculatorTrialTest extends BaseClientBalanceCalculatorTest {
    @Test
    public void calculateVoidDate_trial() {
        DateUtils.freezeTime("2021-11-01"); // month with 30 days
        Group group = createGroupWithBalance(150);

        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x
                .duration(Option.of(15))
                .durationMeasurement(Option.of(CustomPeriodUnit.ONE_DAY)));

        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(150))
                        .priceCurrency(rub)
                        .trialDefinitionId(Option.of(trialDefinition.getId())));

        createGroupService(group, groupProduct);

        // 2021-11-01 -> 2021/11/16 trial
        // 2021/11/16 -> 2021/12/16 150-150=0
        Option<Instant> voidDate = calculateVoidDate(group);
        Assert.assertEquals(Option.of(Instant.parse("2021-12-16T12:00:00")), voidDate);
    }

    @Test
    public void calculateVoidDate_trialZeroBalance() {
        DateUtils.freezeTime("2021-11-01"); // month with 30 day
        Group group = createGroupWithBalance(0);

        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x
                .duration(Option.of(15))
                .durationMeasurement(Option.of(CustomPeriodUnit.ONE_DAY)));

        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(150))
                        .priceCurrency(rub)
                        .trialDefinitionId(Option.of(trialDefinition.getId())));

        createGroupService(group, groupProduct);

        // 2021-11-01 -> 2021/11/16 trial
        Option<Instant> voidDate = calculateVoidDate(group);
        Assert.assertEquals(Option.of(Instant.parse("2021-11-16")), voidDate);
    }

    @Test
    public void calculateVoidDate_X_X_Y_Y() {
        DateUtils.freezeTime("2021-11-01"); // month with 30 days
        GroupProduct groupProduct = createProductWithPrice(900); // 3rub per day

        Group group = createGroupWithBalance(1500);
        GroupService gs1 = createGroupService(group, groupProduct);
        GroupService gs2 = createGroupService(group, groupProduct);

        createPriceOverride(gs1, 600, "2021-11-01", "2021-11-11"); // X
        createPriceOverride(gs2, 300, "2021-11-16", "2021-11-26"); // Y

        // 2021-11-01 -> 2021-11-11: 1500-200-300=1000
        // 2021-11-11 -> 2021-11-16: 1000-150-150=700
        // 2021-11-16 -> 2021-11-26:  700-300-100=300
        // 2021-11-26 -> 2021-12-01:  300-150-150=0

        Option<Instant> voidDate = calculateVoidDate(group);
        Assert.assertEquals(Option.of(Instant.parse("2021-12-01")), voidDate);
    }

    @Test
    public void calculateVoidDate_X_Y_X_Y() {
        DateUtils.freezeTime("2021-11-01"); // month with 30 days
        GroupProduct groupProduct = createProductWithPrice(900); // 3rub per day

        Group group = createGroupWithBalance(1350);
        GroupService gs1 = createGroupService(group, groupProduct);
        GroupService gs2 = createGroupService(group, groupProduct);

        createPriceOverride(gs1, 600, "2021-11-01", "2021-11-16"); // X
        createPriceOverride(gs2, 300, "2021-11-11", "2021-11-26"); // Y

        // 2021-11-01 -> 2021-11-11: 1350-200-300=850
        // 2021-11-11 -> 2021-11-16:  850-100-50=700
        // 2021-11-16 -> 2021-11-26:  700-300-100=300
        // 2021-11-26 -> 2021-12-01:  300-150-150=0

        Option<Instant> voidDate = calculateVoidDate(group);
        Assert.assertEquals(Option.of(Instant.parse("2021-12-01")), voidDate);
    }

    @Test
    public void calculateVoidDate_X_Y_Y_X() {
        DateUtils.freezeTime("2021-11-01"); // month with 30 days
        GroupProduct groupProduct = createProductWithPrice(900); // 3rub per day

        Group group = createGroupWithBalance(1450);
        GroupService gs1 = createGroupService(group, groupProduct);
        GroupService gs2 = createGroupService(group, groupProduct);

        createPriceOverride(gs1, 600, "2021-11-01", "2021-11-26"); // X
        createPriceOverride(gs2, 300, "2021-11-11", "2021-11-16"); // Y

        // 2021-11-01 -> 2021-11-11: 1450-200-300=950
        // 2021-11-11 -> 2021-11-16:  950-100-50=800
        // 2021-11-16 -> 2021-11-26:  800-200-300=300
        // 2021-11-26 -> 2021-12-01:  300-150-150=0

        Option<Instant> voidDate = calculateVoidDate(group);
        Assert.assertEquals(Option.of(Instant.parse("2021-12-01")), voidDate);
    }

    @Test
    public void calculateVoidDate_0_Y_Y_0() {
        DateUtils.freezeTime("2021-11-01");
        GroupProduct groupProduct = createProductWithPrice(30); // 1rub per day
        GroupProduct freeGroupProduct = createProductWithPrice(0); // 0

        Group group = createGroupWithBalance(1);
         createGroupService(group, freeGroupProduct);
        GroupService gs2 = createGroupService(group, groupProduct);

        createPriceOverride(gs2, 60, "2021-11-10", "2021-11-11"); // Y
        Option<Instant> voidDate = calculateVoidDate(group);
        Assert.assertEquals(Option.of(Instant.parse("2021-11-02")), voidDate);
    }

    @Test
    public void calculateVoidDate_X_XY_Y() {
        DateUtils.freezeTime("2021-11-01"); // month with 30 days
        GroupProduct groupProduct = createProductWithPrice(900); // 3rub per day

        Group group = createGroupWithBalance(1500);
        GroupService gs1 = createGroupService(group, groupProduct);
        GroupService gs2 = createGroupService(group, groupProduct);

        createPriceOverride(gs1, 600, "2021-11-06", "2021-11-16"); // X
        createPriceOverride(gs2, 300, "2021-11-16", "2021-11-26"); // Y

        // 2021-11-01 -> 2021-11-06: 1500-150-150=1200
        // 2021-11-06 -> 2021-11-16: 1200-200-300=700
        // 2021-11-16-> 2021-11-26:  700-300-100=300
        // 2021-11-26 -> 2021-12-01:  300-150-150=0

        Option<Instant> voidDate = calculateVoidDate(group);
        Assert.assertEquals(Option.of(Instant.parse("2021-12-01")), voidDate);
    }

    @Test
    public void calculateVoidDate_XY_XY() {
        DateUtils.freezeTime("2021-11-01"); // month with 30 days
        GroupProduct groupProduct = createProductWithPrice(900); // 3rub per day

        Group group = createGroupWithBalance(1500);
        GroupService gs1 = createGroupService(group, groupProduct);
        GroupService gs2 = createGroupService(group, groupProduct);

        createPriceOverride(gs1, 600, "2021-11-06", "2021-11-16"); // X
        createPriceOverride(gs2, 300, "2021-11-06", "2021-11-16"); // Y

        // 2021-11-01 -> 2021-11-06: 1500-150-150=1200
        // 2021-11-06 -> 2021-11-16: 1200-200-100=900
        // 2021-11-16 -> 2021-12-01:  900-450-450=0

        Option<Instant> voidDate = calculateVoidDate(group);
        Assert.assertEquals(Option.of(Instant.parse("2021-12-01")), voidDate);
    }

    @Test
    public void calculateVoidDate_several_services_with_zero_price() {
        Instant now = DateUtils.freezeTime(Instant.parse("2021-11-01")); // берем месяц с 30 днями

        GroupProduct groupProduct = createProductWithPrice(100);
        GroupProduct freeGroupProduct = createProductWithPrice(0);

        Group group = createGroupWithBalance(0);

        createGroupService(group, freeGroupProduct);
        GroupService gs2 = createGroupService(group, groupProduct);

        createPriceOverride(gs2, 0, now.minus(Duration.standardDays(1)), now.plus(Duration.standardDays(3)));

        Option<Instant> voidDate = calculateVoidDate(group);

        Assert.assertEquals(now.plus(Duration.standardDays(3)), voidDate.get());
    }

    private Group createGroupWithBalance(double balance) {
        psBillingBalanceFactory.createBalance(clientId.intValue(), rub.getCurrencyCode(), balance);
        return psBillingGroupsFactory.createGroup(clientId);
    }

    private GroupProduct createProductWithPrice(double price) {
        return psBillingProductsFactory.createGroupProduct(x ->
                x.pricePerUserInMonth(BigDecimal.valueOf(price)).priceCurrency(rub));
    }

    private Option<Instant> calculateVoidDate(Group group) {
        ClientBalanceEntity clientBalance = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(),
                rub).get();
        return calculator.calculateVoidDate(new ClientBalanceInfo(clientId, rub, clientBalance.getBalanceAmount()),
                Option.empty());
    }

    private void createPriceOverride(GroupService groupService, double price, String from, String to) {
        createPriceOverride(groupService, price, Instant.parse(from), Instant.parse(to));
    }

    private void createPriceOverride(GroupService groupService, double price, Instant from, Instant to) {
        psBillingGroupsFactory.createServicePriceOverrides(groupService, price, from, to);
    }
}
