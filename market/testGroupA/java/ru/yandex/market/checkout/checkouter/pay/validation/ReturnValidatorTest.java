package ru.yandex.market.checkout.checkouter.pay.validation;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;

class ReturnValidatorTest extends AbstractServicesTestBase {

    @Autowired
    private ReturnValidator returnValidator;

    @Test
    void testValidateOtherReturnsDoesntContainsSameItems() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(172882069L);
        orderItem.setCount(2);
        orderItem.setBuyerPrice(BigDecimal.valueOf(840));

        Order order = new Order();
        order.setItems(List.of(orderItem));

        Return returnRequest = new Return();
        returnRequest.setId(2093483L);
        returnRequest.setItems(List.of(
                new ReturnItem(172882069L, 1, null, false, BigDecimal.ZERO),
                new ReturnItem(172882069L, 1, null, false, BigDecimal.ZERO)
        ));

        Return oldReturn = new Return();
        oldReturn.setId(2093392L);
        oldReturn.setItems(List.of(
                new ReturnItem(172882069L, 1, null, false, BigDecimal.ZERO)));

        List<Return> otherReturns = List.of(oldReturn);

        Assertions.assertThrows(InvalidRequestException.class, () -> {
            returnValidator.validateOtherReturnsDoesntContainsSameItems(
                    returnRequest, otherReturns, order
            );
        });
    }

    @Test
    void testValidateOtherReturnsDoesntContainsSameItems_ok() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(172882069L);
        orderItem.setCount(3);
        orderItem.setBuyerPrice(BigDecimal.valueOf(840));

        Order order = new Order();
        order.setItems(List.of(orderItem));

        Return returnRequest = new Return();
        returnRequest.setId(2093483L);
        returnRequest.setItems(List.of(
                new ReturnItem(172882069L, 1, null, false, BigDecimal.ZERO),
                new ReturnItem(172882069L, 1, null, false, BigDecimal.ZERO)
        ));

        Return oldReturn = new Return();
        oldReturn.setId(2093392L);
        oldReturn.setItems(List.of(
                new ReturnItem(172882069L, 1, null, false, BigDecimal.ZERO)));

        List<Return> otherReturns = List.of(oldReturn);

        Assertions.assertDoesNotThrow(() -> {
            returnValidator.validateOtherReturnsDoesntContainsSameItems(
                    returnRequest, otherReturns, order
            );
        });
    }
}
