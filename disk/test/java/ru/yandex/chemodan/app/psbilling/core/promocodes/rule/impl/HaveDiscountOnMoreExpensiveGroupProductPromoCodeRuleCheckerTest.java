package ru.yandex.chemodan.app.psbilling.core.promocodes.rule.impl;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeType;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.PromoCodeData;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.SafePromoCode;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleChecker;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleCheckerFactory;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleContext;
import ru.yandex.misc.test.Assert;

public class HaveDiscountOnMoreExpensiveGroupProductPromoCodeRuleCheckerTest extends PsBillingPromoCoreTest {

    @Autowired
    private PromoCodeRuleCheckerFactory factory;
    private PromoCodeRuleChecker checker;
    private Group group;

    @Before
    public void setUp() throws Exception {
        this.group = psBillingGroupsFactory.createGroup();
        this.checker = factory.haveDiscountOnMoreExpensiveGroupProduct();
    }

    @Test
    public void testNoneB2bPromoCode() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2C);

        Assert.assertThrows(
                () -> checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext()),
                IllegalArgumentException.class
        );
    }

    @Test
    public void testNoneGroup() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2B);

        Assert.assertThrows(
                () -> checker.check(promoCodeEntity, Option.empty(), Option.empty(), new PromoCodeRuleContext()),
                IllegalArgumentException.class
        );
    }

    @Test
    public void testEmptyPromoTemplateLines() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2B);

        val period = psBillingProductsFactory.createTrialDefinitionWithPeriod();

        val product = psBillingProductsFactory.createGroupProduct(x ->
                x
                        .trialDefinitionId(Option.of(period.getId()))
                        .pricePerUserInMonth(BigDecimal.TEN)
        );

        psBillingGroupsFactory.createGroupService(group, product);

        Assert.assertThrows(
                () -> checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext()),
                IllegalStateException.class
        );
    }


    @Test
    public void testEmptyCurrentProducts() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2B);

        val period = psBillingProductsFactory.createTrialDefinitionWithPeriod();

        val product = psBillingProductsFactory.createGroupProduct(x ->
                x
                        .trialDefinitionId(Option.of(period.getId()))
                        .pricePerUserInMonth(BigDecimal.TEN)
        );

        val productLine = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));
        promoTemplateDao.bindProductLines(promoCodeEntity.getPromoTemplateId().get(), productLine.getId());

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testSameCurrentPrice() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2B);

        val period = psBillingProductsFactory.createTrialDefinitionWithPeriod();

        val product = psBillingProductsFactory.createGroupProduct(x ->
                x
                        .trialDefinitionId(Option.of(period.getId()))
                        .pricePerUserInMonth(BigDecimal.TEN)
        );


        val line = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(product));
        promoTemplateDao.bindProductLines(promoCodeEntity.getPromoTemplateId().get(), line.getId());
        psBillingGroupsFactory.createGroupService(group, product);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testCheapCurrentPrice() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2B);

        val period = psBillingProductsFactory.createTrialDefinitionWithPeriod();

        val productTen = psBillingProductsFactory.createGroupProduct(x ->
                x
                        .trialDefinitionId(Option.of(period.getId()))
                        .pricePerUserInMonth(BigDecimal.TEN)
        );


        val productOne = psBillingProductsFactory.createGroupProduct(x ->
                x
                        .pricePerUserInMonth(BigDecimal.ONE)
        );

        val line = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(productTen));
        promoTemplateDao.bindProductLines(promoCodeEntity.getPromoTemplateId().get(), line.getId());
        psBillingGroupsFactory.createGroupService(group, productOne);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }


    @Test
    public void testExpensiveCurrentPrice() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2B);

        val period = psBillingProductsFactory.createTrialDefinitionWithPeriod();

        val productTen = psBillingProductsFactory.createGroupProduct(x -> x.pricePerUserInMonth(BigDecimal.TEN));


        val productOne = psBillingProductsFactory.createGroupProduct(x ->
                x
                        .trialDefinitionId(Option.of(period.getId()))
                        .pricePerUserInMonth(BigDecimal.ONE)
        );

        val line = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(productOne));
        promoTemplateDao.bindProductLines(promoCodeEntity.getPromoTemplateId().get(), line.getId());
        psBillingGroupsFactory.createGroupService(group, productTen);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isFalse(result.isSuccess());
        Assert.some(result.getError());
    }


    @Test
    public void testWithoutTrialCurrentPrice() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2B);

        val productOne = psBillingProductsFactory.createGroupProduct(x -> x.pricePerUserInMonth(BigDecimal.ONE));

        val line = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(productOne));
        promoTemplateDao.bindProductLines(promoCodeEntity.getPromoTemplateId().get(), line.getId());

        val productTen = psBillingProductsFactory.createGroupProduct(x -> x.pricePerUserInMonth(BigDecimal.TEN));
        psBillingGroupsFactory.createGroupService(group, productTen);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testWithOtherDiscountPriceFirst() {
        final PromoCodeData promoCodeEntity = createFullPromo();

        val product = psBillingProductsFactory.createGroupProduct(x -> x
                .pricePerUserInMonth(BigDecimal.valueOf(1))
        );

        psBillingGroupsFactory.createGroupService(group, product);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testWithOtherDiscountPriceMiddle() {
        final PromoCodeData promoCodeEntity = createFullPromo();

        val product = psBillingProductsFactory.createGroupProduct(x -> x
                .pricePerUserInMonth(BigDecimal.valueOf(2))
        );

        psBillingGroupsFactory.createGroupService(group, product);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testWithOtherDiscountPriceLast() {
        final PromoCodeData promoCodeEntity = createFullPromo();

        val product = psBillingProductsFactory.createGroupProduct(x -> x
                .pricePerUserInMonth(BigDecimal.valueOf(3))
        );

        psBillingGroupsFactory.createGroupService(group, product);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @Test
    public void testWithOtherDiscountPriceExpensive() {
        final PromoCodeData promoCodeEntity = createFullPromo();

        val product = psBillingProductsFactory.createGroupProduct(x -> x
                .pricePerUserInMonth(BigDecimal.valueOf(4))
        );

        psBillingGroupsFactory.createGroupService(group, product);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isFalse(result.isSuccess());
        Assert.some(result.getError());
    }

    @Test
    public void testWithOtherDiscountPriceExpensiveWithHide() {
        final PromoCodeData promoCodeEntity = createFullPromo();

        val product = psBillingProductsFactory.createGroupProduct(x -> x
                .pricePerUserInMonth(BigDecimal.valueOf(10000000))
                .hidden(true)
        );

        psBillingGroupsFactory.createGroupService(group, product);

        val result = checker.check(promoCodeEntity, Option.empty(), Option.of(group), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }

    @NotNull
    private PromoCodeData createFullPromo() {
        val promoCodeEntity = createPromoCode(PromoCodeType.B2B);
        val period = psBillingProductsFactory.createTrialDefinitionWithPeriod();

        val productOne = psBillingProductsFactory.createGroupProduct(x -> x
                .pricePerUserInMonth(BigDecimal.valueOf(1))
                .trialDefinitionId(Option.of(period.getId()))
        );
        val productTwo = psBillingProductsFactory.createGroupProduct(x -> x.
                pricePerUserInMonth(BigDecimal.valueOf(2))
                .trialDefinitionId(Option.of(period.getId()))

        );
        val productThree = psBillingProductsFactory.createGroupProduct(x -> x.
                pricePerUserInMonth(BigDecimal.valueOf(3))
                .trialDefinitionId(Option.of(period.getId()))
        );

        val lineProduct = Cf.list(productOne, productTwo, productThree);

        val line = psBillingProductsFactory.createGroupProductLineWithSet(lineProduct);
        promoTemplateDao.bindProductLines(promoCodeEntity.getPromoTemplateId().get(), line.getId());
        return promoCodeEntity;
    }


    private PromoCodeData createPromoCode(PromoCodeType promoCodeType) {
        val promo = psBillingPromoFactory.createPromo(x -> x);

        PromoCodeEntity promoCode = psBillingPromoFactory.createPromoCode(x ->
                x
                        .codes(Cf.list(SafePromoCode.cons(UUID.randomUUID().toString())))
                        .promoCodeType(promoCodeType)
                        .promoTemplateId(Option.of(promo.getId()))
        );

        return PromoCodeData.byEntityWithLazyTemplate(promoCode, promoCodeTemplateDao::findByIdO);
    }

}
