package ru.yandex.direct.intapi.entity.balanceclient.model;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderTestHelper;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Тесты на метод {@link NotifyOrderParameters#hasAnyMoney()}
 */
public class NotifyOrderParametersTest {
    NotifyOrderParameters updateRequest;

    @Test
    public void checkResultFalse_forAllFieldsAreNull() {
        updateRequest = new NotifyOrderParameters()
                .withChipsCost(null)
                .withChipsSpent(null)
                .withSumRealMoney(null)
                .withSumUnits(null)
                .withTotalSum(null);
        assertFalse(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultFalse_forAllFieldsAreZero() {
        updateRequest = new NotifyOrderParameters()
                .withChipsCost(BigDecimal.ZERO)
                .withChipsSpent(BigDecimal.ZERO)
                .withSumRealMoney(BigDecimal.ZERO)
                .withSumUnits(BigDecimal.ZERO)
                .withTotalSum(BigDecimal.ZERO);
        assertFalse(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultFalse_forAllFieldsAreZeroWithScale() {
        updateRequest = new NotifyOrderParameters()
                .withChipsCost(BigDecimal.valueOf(0, 1))
                .withChipsSpent(BigDecimal.valueOf(0, 2))
                .withSumRealMoney(BigDecimal.valueOf(0, 3))
                .withSumUnits(BigDecimal.valueOf(0, 4))
                .withTotalSum(BigDecimal.valueOf(0, 5));
        assertFalse(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultFalse_forCombinationOfNullAndZeroFields() {
        updateRequest = new NotifyOrderParameters()
                .withChipsCost(BigDecimal.ZERO)
                .withChipsSpent(null)
                .withSumRealMoney(BigDecimal.ZERO)
                .withSumUnits(null)
                .withTotalSum(BigDecimal.ZERO);
        assertFalse(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultTrue_forAllFieldsHasRandomValue() {
        updateRequest = NotifyOrderTestHelper.generateNotifyOrderParameters();
        assertTrue(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultTrue_forAllFieldsHasPositiveValue() {
        updateRequest = new NotifyOrderParameters().withChipsCost(BigDecimal.ONE);
        assertTrue(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultTrue_forPositiveValueOfChipsSpent() {
        updateRequest = new NotifyOrderParameters().withChipsSpent(BigDecimal.TEN);
        assertTrue(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultTrue_forPositiveValueOfSumRealMoney() {
        updateRequest = new NotifyOrderParameters().withSumRealMoney(BigDecimal.TEN);
        assertTrue(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultTrue_forPositiveValueOfSumUnits() {
        updateRequest = new NotifyOrderParameters().withSumUnits(BigDecimal.TEN);
        assertTrue(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultTrue_forPositiveValueOfTotalSum() {
        updateRequest = new NotifyOrderParameters().withTotalSum(BigDecimal.TEN);
        assertTrue(updateRequest.hasAnyMoney());
    }

    @Test
    public void checkResultTrue_forPositiveValueOfChipsCost() {
        updateRequest = new NotifyOrderParameters()
                .withChipsCost(BigDecimal.TEN)
                .withChipsSpent(BigDecimal.TEN)
                .withSumRealMoney(BigDecimal.TEN)
                .withSumUnits(BigDecimal.TEN)
                .withTotalSum(BigDecimal.TEN);
        assertTrue(updateRequest.hasAnyMoney());
    }
}
