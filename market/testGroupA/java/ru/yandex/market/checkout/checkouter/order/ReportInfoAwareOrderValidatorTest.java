package ru.yandex.market.checkout.checkouter.order;

import java.util.Collections;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ThrowsException;

import ru.yandex.market.checkout.checkouter.actualization.validation.ReportInfoAwareOrderValidator;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.QuantityLimits;

public class ReportInfoAwareOrderValidatorTest {

    private ReportInfoAwareOrderValidator reportInfoAwareOrderValidator;
    private Supplier<Boolean> switcher;

    @BeforeEach
    public void setUp() throws Exception {
        switcher = Mockito.mock(Supplier.class, new ThrowsException(new UnsupportedOperationException("unsupported")));

        reportInfoAwareOrderValidator = new ReportInfoAwareOrderValidator(switcher);
    }

    @Test
    public void shouldFailIfCountIsLesserThanMinCount() throws Exception {
        Mockito.doReturn(Boolean.TRUE).when(switcher).get();

        QuantityLimits quantityLimits = new QuantityLimits();
        quantityLimits.setMinimum(4);
        quantityLimits.setStep(2);
        //
        OrderItem orderItem = new OrderItem(new FeedOfferId("1", 1L), null, 3);
        orderItem.setQuantityLimits(quantityLimits);
        //
        Order order = new Order();
        order.setItems(Collections.singletonList(orderItem));

        reportInfoAwareOrderValidator.validateOrder(order);
        Assertions.assertTrue(order.hasErrors());
    }

    @Test
    public void shouldFailIfCannotBeRepresentedByFormula() throws Exception {
        Mockito.doReturn(Boolean.TRUE).when(switcher).get();

        QuantityLimits quantityLimits = new QuantityLimits();
        quantityLimits.setMinimum(4);
        quantityLimits.setStep(2);
        //
        OrderItem orderItem = new OrderItem(new FeedOfferId("1", 1L), null, 5);
        orderItem.setQuantityLimits(quantityLimits);
        //
        Order order = new Order();
        order.setItems(Collections.singletonList(orderItem));

        reportInfoAwareOrderValidator.validateOrder(order);
        Assertions.assertTrue(order.hasErrors());
    }

    @Test
    public void shouldNotFailIfExactlyMinimum() throws Exception {
        Mockito.doReturn(Boolean.TRUE).when(switcher).get();

        QuantityLimits quantityLimits = new QuantityLimits();
        quantityLimits.setMinimum(4);
        quantityLimits.setStep(2);
        //
        OrderItem orderItem = new OrderItem(new FeedOfferId("1", 1L), null, 4);
        orderItem.setQuantityLimits(quantityLimits);
        //
        Order order = new Order();
        order.setItems(Collections.singletonList(orderItem));

        reportInfoAwareOrderValidator.validateOrder(order);
        Assertions.assertFalse(order.hasErrors());
    }

    @Test
    public void shouldNotFailIfMinimumAndStepBy2() throws Exception {
        Mockito.doReturn(Boolean.TRUE).when(switcher).get();

        QuantityLimits quantityLimits = new QuantityLimits();
        quantityLimits.setMinimum(4);
        quantityLimits.setStep(2);
        //
        OrderItem orderItem = new OrderItem(new FeedOfferId("1", 1L), null, 8);
        orderItem.setQuantityLimits(quantityLimits);
        //
        Order order = new Order();
        order.setItems(Collections.singletonList(orderItem));

        reportInfoAwareOrderValidator.validateOrder(order);
        Assertions.assertFalse(order.hasErrors());
    }

    @Test
    public void shouldNotFailIfDisabledButIncorrect() throws Exception {
        Mockito.doReturn(false).when(switcher).get();

        QuantityLimits quantityLimits = new QuantityLimits();
        quantityLimits.setMinimum(4);

        OrderItem orderItem = new OrderItem(new FeedOfferId("1", 1L), null, 8);
        orderItem.setQuantityLimits(quantityLimits);

        Order order = new Order();
        order.setItems(Collections.singletonList(orderItem));

        reportInfoAwareOrderValidator.validateOrder(order);
        Assertions.assertFalse(order.hasErrors());
    }

}
