package ru.yandex.market.checkout.checkouter.b2b;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.b2b.B2bCustomersServiceImpl.StatelessProvider;
import ru.yandex.market.checkout.checkouter.b2b.B2bCustomersSlowApi.PaymentInvoiceResponse;
import ru.yandex.market.checkout.checkouter.b2b.model.GenerateInvoiceRequestBody;
import ru.yandex.market.checkout.checkouter.b2b.model.GenerateMultiInvoiceRequestBody;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_B2B_MULTI_INVOICE_API;

@ExtendWith(MockitoExtension.class)
public class B2bInvoiceProviderFactoryTest {

    @Mock
    private B2bCustomersSlowApi b2bApi;
    @Mock
    private OrderService orderService;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;

    @InjectMocks
    public B2bInvoiceProviderFactory factory;

    @Test
    public void getOrdersById_ifUseUnifiedFalseThenProviderCallsOrderService() {
        // Given
        doReturn(false).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId = 1L;
        StatelessProvider provider = factory.pickProvider(orderId);
        Order foundOrder = order(orderId);
        doReturn(foundOrder).when(orderService).getOrder(orderId);

        // When
        Map<Long, Order> orders = provider.getOrdersById();

        // Then
        org.assertj.core.api.Assertions.assertThat(orders.keySet())
                .contains(orderId)
                .hasSize(1);
        assertEquals(foundOrder, orders.get(orderId));
    }

    private Order order(Long orderId) {
        Order order = B2bCustomersTestProvider.defaultB2bParameters().getOrder();
        order.setId(orderId);
        return order;
    }

    @Test
    public void getOrdersById_ifUseUnifiedFalseThenProviderFailsForMoreThenOneOrder() {
        // Given
        doReturn(false).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId1 = 1L;
        Long orderId2 = 2L;
        Order order1 = order(orderId1);
        Order order2 = order(orderId2);
        StatelessProvider provider = factory.pickProvider(orderId1);

        // When + Then
        Assertions.assertThrows(IllegalArgumentException.class, () -> provider.getInvoice(List.of(order1, order2)));
    }

    @Test
    public void getInvoice_ifUseUnifiedFalseThenProviderReturnsCorrectResponse() {
        // Given
        doReturn(false).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId = 1L;
        Order order = order(orderId);
        String expectedUrl = "link_to_invoice";
        StatelessProvider provider = factory.pickProvider(orderId);
        doReturn(response(expectedUrl)).when(b2bApi)
                .generatePaymentInvoice(eq(orderId), any(GenerateInvoiceRequestBody.class));

        // When
        PaymentInvoiceResponse generatedInvoice = provider.getInvoice(List.of(order));

        // Then
        assertEquals(expectedUrl, generatedInvoice.getUrl());
    }

    private PaymentInvoiceResponse response(String linkToInvoice) {
        return new PaymentInvoiceResponse(linkToInvoice);
    }

    @Test
    public void getInvoice_ifUseUnifiedFalseThenProviderCallsB2bApiWithExpectedArguments() {
        // Given
        doReturn(false).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId = 1L;
        Order order = order(orderId);
        StatelessProvider provider = factory.pickProvider(orderId);
        doReturn(response("link_to_invoice")).when(b2bApi)
                .generatePaymentInvoice(any(Long.class), any(GenerateInvoiceRequestBody.class));

        // When
        provider.getInvoice(List.of(order));

        // Then
        ArgumentCaptor<GenerateInvoiceRequestBody> bodyCaptor =
                ArgumentCaptor.forClass(GenerateInvoiceRequestBody.class);
        verify(b2bApi).generatePaymentInvoice(eq(orderId), bodyCaptor.capture());
        GenerateInvoiceRequestBody actualBody = bodyCaptor.getValue();
        GenerateInvoiceRequestBody expectedBody =
                B2bCustomersRequestBuilder.buildGenerateInvoiceRequest(order, 0);
        assertEquals(expectedBody, actualBody);
    }

    @Test
    public void getOrdersById_ifUseUnifiedTrueThenProviderSearchesForMultiOrderId() {
        // Given
        doReturn(true).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId1 = 1L;
        Long orderId2 = 2L;
        StatelessProvider provider = factory.pickProvider(orderId1);
        doReturn(List.of(orderId1, orderId2)).when(orderService)
                .findAllMultiOrderIdsByAnyParticular(orderId1);

        // When
        provider.getOrdersById();

        // Then
        verify(orderService).findAllMultiOrderIdsByAnyParticular(orderId1);
    }

    @Test
    public void getOrdersById_ifUseUnifiedTrueThenProviderReturnsOneOrderWhenMultiIdsAreNotFound() {
        // Given
        doReturn(true).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId = 1L;
        StatelessProvider provider = factory.pickProvider(orderId);
        Mockito.doReturn(Collections.emptyList()).when(orderService)
                .findAllMultiOrderIdsByAnyParticular(orderId);
        Order foundOrder = order(orderId);
        doReturn(foundOrder).when(orderService).getOrder(orderId);

        // When
        Map<Long, Order> orders = provider.getOrdersById();

        // Then
        org.assertj.core.api.Assertions.assertThat(orders.keySet())
                .contains(orderId)
                .hasSize(1);
        assertEquals(foundOrder, orders.get(orderId));
    }

    @Test
    public void getOrdersById_ifUseUnifiedTrueThenReturnsManyOrdersWhenMultiIdsAreFound() {
        // Given
        doReturn(true).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId1 = 1L;
        Long orderId2 = 2L;
        StatelessProvider provider = factory.pickProvider(orderId1);
        Mockito.doReturn(List.of(orderId1, orderId2)).when(orderService)
                .findAllMultiOrderIdsByAnyParticular(orderId1);
        Order foundOrder1 = order(orderId1);
        Order foundOrder2 = order(orderId2);
        doReturn(Map.of(
                orderId1, foundOrder1,
                orderId2, foundOrder2)
        ).when(orderService).getOrders(List.of(orderId1, orderId2));

        // When
        Map<Long, Order> orders = provider.getOrdersById();

        // Then
        org.assertj.core.api.Assertions.assertThat(orders.keySet())
                .contains(orderId1, orderId2)
                .hasSize(2);
        assertEquals(foundOrder1, orders.get(orderId1));
        assertEquals(foundOrder2, orders.get(orderId2));
    }

    @Test
    public void getInvoice_ifUseUnifiedTrueThenProviderReturnsCorrectResponse() {
        // Given
        doReturn(true).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId1 = 1L;
        Long orderId2 = 2L;
        Order order1 = order(orderId1);
        Order order2 = order(orderId2);
        String expectedUrl = "link_to_invoice";
        StatelessProvider provider = factory.pickProvider(orderId1);
        doReturn(response(expectedUrl)).when(b2bApi)
                .generateMultiPaymentInvoice(any(GenerateMultiInvoiceRequestBody.class));

        // When
        PaymentInvoiceResponse generatedInvoice = provider.getInvoice(List.of(order1, order2));

        // Then
        assertEquals(expectedUrl, generatedInvoice.getUrl());
    }

    @Test
    public void getInvoice_ifUseUnifiedTrueThenProviderCallsB2bApiWithExpectedArguments() {
        // Given
        doReturn(true).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        Long orderId1 = 1L;
        Long orderId2 = 2L;
        Order order1 = order(orderId1);
        order1.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiId");
        Order order2 = order(orderId2);
        order2.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiId");
        String expectedUrl = "link_to_invoice";
        StatelessProvider provider = factory.pickProvider(orderId1);
        doReturn(response(expectedUrl)).when(b2bApi)
                .generateMultiPaymentInvoice(any(GenerateMultiInvoiceRequestBody.class));

        // When
        provider.getInvoice(List.of(order1, order2));

        // Then
        ArgumentCaptor<GenerateMultiInvoiceRequestBody> bodyCaptor =
                ArgumentCaptor.forClass(GenerateMultiInvoiceRequestBody.class);
        verify(b2bApi).generateMultiPaymentInvoice(bodyCaptor.capture());
        GenerateMultiInvoiceRequestBody actualBody = bodyCaptor.getValue();
        GenerateMultiInvoiceRequestBody expectedBody = makeExpected(List.of(order1, order2));
        assertEquals(expectedBody, actualBody);
    }

    private GenerateMultiInvoiceRequestBody makeExpected(List<Order> orders) {
        GenerateMultiInvoiceRequestBody multiOrderRequest = new GenerateMultiInvoiceRequestBody();
        multiOrderRequest.setOrders(orders.stream()
                .map(o -> B2bCustomersRequestBuilder.buildGenerateInvoiceRequest(o, orders.size()))
                .collect(Collectors.toList()));
        multiOrderRequest.setMultiOrderId(orders.get(0).getProperty(OrderPropertyType.MULTI_ORDER_ID));
        return multiOrderRequest;
    }

    @Test
    public void getInvoice_ifUseUnifiedFalseThenProviderCallsB2bApiWithExpectedArgumentsForMultiOrder() {
        // Given
        Long orderId = 1L;
        doReturn(false).when(checkouterFeatureReader).getBoolean(USE_B2B_MULTI_INVOICE_API);
        doReturn(List.of(orderId)).when(orderService).findAllMultiOrderIdsByAnyParticular(orderId);
        Order order = order(orderId);
        order.setProperty(OrderPropertyType.MULTI_ORDER_ID, "multiOrderId");
        StatelessProvider provider = factory.pickProvider(orderId);
        doReturn(response("link_to_invoice")).when(b2bApi)
                .generatePaymentInvoice(any(Long.class), any(GenerateInvoiceRequestBody.class));

        // When
        provider.getInvoice(List.of(order));

        // Then
        ArgumentCaptor<GenerateInvoiceRequestBody> bodyCaptor =
                ArgumentCaptor.forClass(GenerateInvoiceRequestBody.class);
        verify(b2bApi).generatePaymentInvoice(eq(orderId), bodyCaptor.capture());
        GenerateInvoiceRequestBody actualBody = bodyCaptor.getValue();
        GenerateInvoiceRequestBody expectedBody =
                B2bCustomersRequestBuilder.buildGenerateInvoiceRequest(order, 1);
        assertEquals(expectedBody, actualBody);
    }

}
