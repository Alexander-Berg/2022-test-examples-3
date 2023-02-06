package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.refund.SubsidyRefundStrategy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author : poluektov
 * date: 20.07.18.
 */
public class SubsidyAmountCalculatorUnitTest {

    private Order order;

    @BeforeEach
    public void prepareOrder() {
        order = new Order();
        order.setBuyerItemsTotal(new BigDecimal("1000.0"));
        order.getPromoPrices().setSubsidyTotal(new BigDecimal("500.0"));
        order.setExchangeRate(BigDecimal.ONE);
    }

    @Test
    public void case1() {
        BigDecimal result = SubsidyRefundStrategy.calcSubsidyRefundAmount(new BigDecimal("999.0"), order);
        assertThat(result, equalTo(new BigDecimal("500")));
    }

    @Test
    public void case2() {
        BigDecimal result = SubsidyRefundStrategy.calcSubsidyRefundAmount(new BigDecimal("1000.0"), order);
        assertThat("Полный возврат субсидии, т.к. возвращаем сумму за весь заказ",
                result, equalTo(new BigDecimal("500")));
    }

    @Test
    public void case3() {
        BigDecimal result = SubsidyRefundStrategy.calcSubsidyRefundAmount(new BigDecimal("500.59"), order);
        assertThat(result, equalTo(new BigDecimal("251")));
    }

    @Test
    public void case4() {
        BigDecimal result = SubsidyRefundStrategy.calcSubsidyRefundAmount(new BigDecimal("500.0"), order);
        assertThat(result, equalTo(new BigDecimal("250")));
    }

    @Test
    public void case5() {
        order.setSubsidyRefundPlanned(new BigDecimal("480"));
        order.setSubsidyRefundActual(new BigDecimal("10"));
        BigDecimal result = SubsidyRefundStrategy.calcSubsidyRefundAmount(new BigDecimal("500.0"), order);
        assertThat("Часть субсидий уже вернули ранее", result, equalTo(new BigDecimal("10.0")));
    }

    @Test
    public void case6() {
        order.setSubsidyRefundPlanned(new BigDecimal("10"));
        order.setSubsidyRefundActual(new BigDecimal("10"));
        BigDecimal result = SubsidyRefundStrategy.calcSubsidyRefundAmount(new BigDecimal("500.0"), order);
        assertThat(result, equalTo(new BigDecimal("250")));
    }

    @Test
    public void case7() {
        BigDecimal result = SubsidyRefundStrategy.calcSubsidyRefundAmount(new BigDecimal("2000.0"), order);
        assertThat("Нельзя вернуть больше чем было субсидий", result.doubleValue(), equalTo(500d));
    }

}
