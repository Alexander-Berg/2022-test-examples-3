package ru.yandex.market.checkout.checkouter.b2b;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class B2bOrderInSanctionsPayTest extends AbstractWebTestBase {
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private OrderPayHelper orderPayHelper;

    @BeforeEach
    void init() {
        b2bCustomersMockConfigurer.mockGeneratePaymentInvoice();
    }

    @AfterEach
    void resetMocks() {
        b2bCustomersMockConfigurer.resetAll();
    }

    @Test
    void restrictInSanctionsPay() {
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, false);
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();

        assertThrows(AssertionError.class, () -> orderCreateHelper.createOrder(parameters));
    }

    @Test
    void allowNotInSanctionsPay() {
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        Order b2bOrder = orderCreateHelper.createOrder(parameters);

        assertEquals(OrderStatus.UNPAID, b2bOrder.getStatus());
        orderPayHelper.payForBusinessOrder(b2bOrder);
        assertEquals(OrderStatus.PENDING, orderService.getOrder(b2bOrder.getId()).getStatus());
    }
}
