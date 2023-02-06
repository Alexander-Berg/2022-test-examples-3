package ru.yandex.market.checkout.checkouter.b2b;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.backbone.fintech.AccountPaymentFeatureToggle;
import ru.yandex.market.checkout.checkouter.b2b.B2bCustomersServiceImpl.StatelessProvider;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderProperty;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.pay.AccountPaymentOperations;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.util.MultiLockHelper;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.storage.StorageCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class B2bCustomersServiceImplUnitTest {

    @Mock
    private MultiLockHelper multiLockHelper;
    @Mock
    private OrderUpdateService orderUpdateService;
    @Mock
    private OrderHistoryDao historyDao;
    @Mock
    private AccountPaymentOperations accountPaymentOperations;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;
    @Mock
    private B2bInvoiceProviderFactory b2BInvoiceProviderFactory;

    @InjectMocks
    private B2bCustomersServiceImpl service;

    private List<Long> lockedOrders = new ArrayList<>();

    @BeforeEach
    public void beforeEach() {
        lockedOrders.clear();
        Mockito.doAnswer(invocation -> {
                    lockedOrders.addAll(invocation.getArgument(0));
                    return ((StorageCallback<String>) invocation.getArgument(1)).doQuery();
                })
                .when(multiLockHelper)
                .updateWithOrderLocks(Mockito.any(), Mockito.any());
    }

    @Test
    public void generatePaymentInvoiceUrl_ifPaymentFeatureEnabledThenCreatesAccountPayment() {
        // Given
        Long orderId = 1L;
        Order order = order(orderId);
        order.setProperty(OrderPropertyType.INVOICE_PDF_URL, "existing_link_to_invoice");
        setUpProvider(orderId, Map.of(orderId, order), null);
        doReturn(AccountPaymentFeatureToggle.ON).when(checkouterFeatureReader)
                .getAsTargetType(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.class);

        // When
        service.generatePaymentInvoiceUrl(orderId);

        // Then
        verify(accountPaymentOperations).createAndBindAccountPayment(orderId);
    }

    private Order order(Long orderId) {
        Order order = B2bCustomersTestProvider.defaultB2bParameters().getOrder();
        order.setId(orderId);
        return order;
    }

    private StatelessProvider setUpProvider(Long order, Map<Long, Order> orders, String linkToPdfInvoice) {
        StatelessProvider provider = provider(orders, linkToPdfInvoice);
        doReturn(provider).when(b2BInvoiceProviderFactory).pickProvider(order);
        return provider;
    }

    private StatelessProvider provider(Map<Long, Order> orders, String linkToPdfInvoice) {
        StatelessProvider mock = Mockito.mock(StatelessProvider.class);
        doReturn(orders).when(mock).getOrdersById();
        lenient().doReturn(invoice(linkToPdfInvoice)).when(mock).getInvoice(new ArrayList<>(orders.values()));
        return mock;
    }

    private B2bCustomersSlowApi.PaymentInvoiceResponse invoice(String pdfUrl) {
        return new B2bCustomersSlowApi.PaymentInvoiceResponse(pdfUrl);
    }

    @Test
    public void generatePaymentInvoiceUrl_ifInvoiceExistsReturnExisting() {
        // Given
        Long orderId = 1L;
        Order order = order(orderId);
        order.setProperty(OrderPropertyType.INVOICE_PDF_URL, "existing_link_to_invoice");
        setUpProvider(orderId, Map.of(orderId, order), null);
        doReturn(AccountPaymentFeatureToggle.OFF).when(checkouterFeatureReader)
                .getAsTargetType(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.class);

        // When
        String linkToInvoice = service.generatePaymentInvoiceUrl(orderId);

        // Then
        assertEquals("existing_link_to_invoice", linkToInvoice);
    }

    @Test
    public void generatePaymentInvoiceUrl_ifBuyerIsNotB2bThenException() {
        // Given
        Long orderId = 1L;
        Order order = order(orderId);
        Buyer b2bBuyer = B2bCustomersTestProvider.defaultB2bParameters().getBuyer();
        b2bBuyer.setBusinessBalanceId(null);
        order.setBuyer(b2bBuyer);
        setUpProvider(orderId, Map.of(orderId, order), null);
        doReturn(AccountPaymentFeatureToggle.OFF).when(checkouterFeatureReader)
                .getAsTargetType(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.class);

        // When + Then
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.generatePaymentInvoiceUrl(orderId));
    }

    @ParameterizedTest(name = "{displayName} for {0}")
    @MethodSource("notUnpaidOrderStatus")
    public void generatePaymentInvoiceUrl_ifOrderStatusIsNotUnpaidThenException(OrderStatus orderStatus) {
        // Given
        Long orderId = 1L;
        Order order = order(orderId);
        order.setStatus(orderStatus);
        setUpProvider(orderId, Map.of(orderId, order), null);
        doReturn(AccountPaymentFeatureToggle.OFF).when(checkouterFeatureReader)
                .getAsTargetType(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.class);

        // When + Then
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.generatePaymentInvoiceUrl(orderId));
    }

    public static Stream<Arguments> notUnpaidOrderStatus() {
        return Arrays.stream(OrderStatus.values())
                .filter(Predicate.not(OrderStatus.UNPAID::equals))
                .map(Arguments::of);
    }

    @Test
    public void generatePaymentInvoiceUrl_callGetInvoiceWithCorrectArguments() {
        // Given
        Long orderId = 1L;
        Order order = order(orderId);
        order.setStatus(OrderStatus.UNPAID);
        StatelessProvider provider = setUpProvider(orderId, Map.of(orderId, order), "link_to_invoice");
        doReturn(AccountPaymentFeatureToggle.OFF).when(checkouterFeatureReader)
                .getAsTargetType(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.class);

        // When
        service.generatePaymentInvoiceUrl(orderId);

        // Then
        verify(provider).getInvoice(List.of(order));
    }

    @Test
    public void generatePaymentInvoiceUrl_savesPdfUrlAsOrderProperty() {
        // Given
        Long orderId = 1L;
        Order order = order(orderId);
        order.setStatus(OrderStatus.UNPAID);
        setUpProvider(orderId, Map.of(orderId, order), "link_to_invoice");
        doReturn(AccountPaymentFeatureToggle.OFF).when(checkouterFeatureReader)
                .getAsTargetType(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.class);

        // When
        service.generatePaymentInvoiceUrl(orderId);

        // Then
        ArgumentCaptor<OrderProperty> propertyCaptor = ArgumentCaptor.forClass(OrderProperty.class);
        verify(orderUpdateService).addOrderProperty(propertyCaptor.capture());
        OrderProperty actualValue = propertyCaptor.getValue();

        assertThat(actualValue)
                .isEqualToComparingFieldByField(OrderPropertyType.INVOICE_PDF_URL.create(orderId, "link_to_invoice"));
    }

    @Test
    public void generatePaymentInvoiceUrl_savesHistory() {
        // Given
        Long orderId = 1L;
        Order order = order(orderId);
        order.setStatus(OrderStatus.UNPAID);
        setUpProvider(orderId, Map.of(orderId, order), "link_to_invoice");
        doReturn(AccountPaymentFeatureToggle.OFF).when(checkouterFeatureReader)
                .getAsTargetType(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.class);

        // When
        service.generatePaymentInvoiceUrl(orderId);

        // Then
        verify(historyDao).insertOrderHistory(orderId, HistoryEventType.PAYMENT_INVOICE_GENERATED, ClientInfo.SYSTEM);
    }

    @Test
    public void generatePaymentInvoiceUrl_returnsGeneratedLinkToPdfInvoice() {
        // Given
        Long orderId = 1L;
        Order order = order(orderId);
        order.setStatus(OrderStatus.UNPAID);
        setUpProvider(orderId, Map.of(orderId, order), "link_to_invoice");
        doReturn(AccountPaymentFeatureToggle.OFF).when(checkouterFeatureReader)
                .getAsTargetType(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.class);

        // When
        String actualLink = service.generatePaymentInvoiceUrl(orderId);

        // Then
        assertEquals("link_to_invoice", actualLink);
    }
}
