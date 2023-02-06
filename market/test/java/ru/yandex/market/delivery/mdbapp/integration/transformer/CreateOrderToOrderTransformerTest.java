package ru.yandex.market.delivery.mdbapp.integration.transformer;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.integration.enricher.EnrichmentFailException;
import ru.yandex.market.delivery.mdbapp.request.CreateOrder;

public class CreateOrderToOrderTransformerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Long> orderIdCator;

    @Mock
    private CheckouterServiceClient checkouterServiceClient;

    private CreateOrderToOrderTransformer transformer;

    @Before
    public void setUp() {
        transformer = new CreateOrderToOrderTransformer(checkouterServiceClient);
    }

    @Test
    public void testThatCheckouterWillBeCalledWithCorrectValue() {
        Mockito.when(checkouterServiceClient.getOrder(Mockito.anyLong())).thenReturn(new Order());
        CreateOrder createOrder = makeCreateOrder();

        transformer.transform(createOrder);

        Mockito.verify(checkouterServiceClient).getOrder(orderIdCator.capture());

        Assertions.assertThat(orderIdCator.getValue()).as("Actual order Id").isEqualTo(123L);
    }

    @Test
    public void testThatTransformerWillThrowException() {
        thrown.expect(EnrichmentFailException.class);
        thrown.expectMessage("Can not fetch order with id 123 from checkouter");

        CreateOrder createOrder = makeCreateOrder();

        transformer.transform(createOrder);
    }

    private CreateOrder makeCreateOrder() {
        CreateOrder createOrder = new CreateOrder();
        createOrder.setOrderId(123L);
        return createOrder;
    }

}
