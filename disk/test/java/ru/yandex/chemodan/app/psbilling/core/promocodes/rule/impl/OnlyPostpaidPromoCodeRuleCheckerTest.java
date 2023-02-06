package ru.yandex.chemodan.app.psbilling.core.promocodes.rule.impl;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.PromoCodeData;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleChecker;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleCheckerFactory;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleCheckerResult;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleContext;
import ru.yandex.misc.test.Assert;

public class OnlyPostpaidPromoCodeRuleCheckerTest extends PsBillingPromoCoreTest {

    @Autowired
    private PromoCodeRuleCheckerFactory factory;
    private PromoCodeRuleChecker checker;
    private Group group;

    private PromoCodeData promoCode;

    @Before
    public void setUp() throws Exception {
        this.group = psBillingGroupsFactory.createGroup();
        this.checker = factory.onlyPostpaid();
        this.promoCode = Mockito.mock(PromoCodeData.class);
    }

    @Test
    public void testNoneGroup() {
        Assert.assertThrows(
                () -> checker.check(promoCode, Option.empty(), Option.empty(), new PromoCodeRuleContext()),
                IllegalArgumentException.class
        );
    }

    @Test
    public void testOnlyPostpaid() {
        val product = psBillingProductsFactory.createGroupProduct(x ->
                x.paymentType(GroupPaymentType.POSTPAID)
        );

        psBillingGroupsFactory.createGroupService(group, product);

        PromoCodeRuleCheckerResult result = checker.check(promoCode, Option.empty(), Option.of(group),
                new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testEmptyService() {
        PromoCodeRuleCheckerResult result = checker.check(promoCode, Option.empty(), Option.of(group),
                new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testOnlyPrepaidHidden() {
        val product = psBillingProductsFactory.createGroupProduct(x -> x
                .paymentType(GroupPaymentType.PREPAID)
                .hidden(true)
        );

        psBillingGroupsFactory.createGroupService(group, product);

        PromoCodeRuleCheckerResult result = checker.check(promoCode, Option.empty(), Option.of(group),
                new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testOnlyPrepaid() {
        val product = psBillingProductsFactory.createGroupProduct(x ->
                x.paymentType(GroupPaymentType.PREPAID)
        );

        psBillingGroupsFactory.createGroupService(group, product);

        PromoCodeRuleCheckerResult result = checker.check(promoCode, Option.empty(), Option.of(group),
                new PromoCodeRuleContext());

        Assert.isFalse(result.isSuccess());
        Assert.some(result.getError());
    }


    @Test
    public void testPrepaidAndPostpaid() {
        val productPostpaid = psBillingProductsFactory.createGroupProduct(x ->
                x.paymentType(GroupPaymentType.POSTPAID)
        );

        psBillingGroupsFactory.createGroupService(group, productPostpaid);

        val productPrepaid = psBillingProductsFactory.createGroupProduct(x ->
                x.paymentType(GroupPaymentType.PREPAID)
        );

        psBillingGroupsFactory.createGroupService(group, productPrepaid);

        PromoCodeRuleCheckerResult result = checker.check(promoCode, Option.empty(), Option.of(group),
                new PromoCodeRuleContext());

        Assert.isFalse(result.isSuccess());
        Assert.some(result.getError());
    }

}
