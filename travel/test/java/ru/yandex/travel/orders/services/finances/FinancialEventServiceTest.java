package ru.yandex.travel.orders.services.finances;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventType;
import ru.yandex.travel.orders.repository.FinancialEventRepository;
import ru.yandex.travel.orders.services.finances.providers.DirectHotelBillingFinancialDataProvider;
import ru.yandex.travel.orders.services.finances.providers.FinancialDataProvider;
import ru.yandex.travel.orders.services.finances.providers.FinancialDataProviderRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.commons.proto.EServiceType.PT_DOLPHIN_HOTEL;
import static ru.yandex.travel.orders.commons.proto.EServiceType.PT_TRAVELLINE_HOTEL;

public class FinancialEventServiceTest {
    private FinancialEventServiceProperties properties;
    private FinancialEventRepository repository;
    private FinancialDataProviderRegistry registry;
    private FinancialEventService service;
    private FinancialDataProvider fakeFinDataProvider;

    @Before
    public void setUp() {
        properties = new FinancialEventServiceProperties();
        repository = Mockito.mock(FinancialEventRepository.class);
        registry = Mockito.mock(FinancialDataProviderRegistry.class);
        DirectHotelBillingFinancialDataProvider directHotelBillingFinancialDataProvider =
                Mockito.mock(DirectHotelBillingFinancialDataProvider.class);
        service = new FinancialEventService(properties, repository, registry,
                directHotelBillingFinancialDataProvider, null, null);
        fakeFinDataProvider = Mockito.mock(FinancialDataProvider.class);

        properties.setEnabled(true);
        when(registry.getSupportedServiceTypes()).thenReturn(Set.of(PT_DOLPHIN_HOTEL, PT_TRAVELLINE_HOTEL));
        service.init();

        properties.setEnabled(false);
        when(repository.save(any())).then(inv -> inv.getArgument(0));
        when(registry.getProvider(any())).thenReturn(fakeFinDataProvider);

        FinancialEvent fakeEvent = new FinancialEvent();
        when(fakeFinDataProvider.onConfirmation(any(), any())).thenReturn(List.of(fakeEvent));
        when(fakeFinDataProvider.onRefund(any(), any(), any())).thenReturn(List.of(fakeEvent));
    }

    @Test
    public void isEnabledFor() {
        OrderItem dolphinOrderItem = orderItem(PT_DOLPHIN_HOTEL);
        OrderItem travellineOrder = orderItem(PT_TRAVELLINE_HOTEL);
        assertThat(service.isEnabledFor(dolphinOrderItem)).isFalse();

        properties.setEnabled(true);
        assertThat(service.isEnabledFor(dolphinOrderItem)).isFalse();

        when(registry.supportsProvider(PT_DOLPHIN_HOTEL)).thenReturn(true);
        when(registry.getProvider(PT_DOLPHIN_HOTEL)).thenReturn(fakeFinDataProvider);
        assertThat(service.isEnabledFor(dolphinOrderItem)).isTrue();
        assertThat(service.isEnabledFor(travellineOrder)).isFalse();

        when(registry.supportsProvider(PT_TRAVELLINE_HOTEL)).thenReturn(true);
        when(registry.getProvider(PT_TRAVELLINE_HOTEL)).thenReturn(fakeFinDataProvider);
        assertThat(service.isEnabledFor(dolphinOrderItem)).isTrue();
        assertThat(service.isEnabledFor(travellineOrder)).isTrue();

        properties.setEnabled(false);
        assertThat(service.isEnabledFor(dolphinOrderItem)).isFalse();
        assertThat(service.isEnabledFor(travellineOrder)).isFalse();
    }

    @Test
    public void registerConfirmedService() {
        OrderItem oi = orderItem(PT_DOLPHIN_HOTEL);

        service.registerConfirmedService(oi);
        verify(repository, times(0)).save(any());

        properties.setEnabled(true);
        when(registry.supportsProvider(PT_DOLPHIN_HOTEL)).thenReturn(true);
        service.registerConfirmedService(oi);
        verify(repository, times(1)).save(any());
    }

    @Test
    public void registerRefundedService() {
        OrderItem oi = orderItem(PT_DOLPHIN_HOTEL);

        service.registerRefundedService(oi, null);
        verify(repository, times(0)).saveAll(any());

        properties.setEnabled(true);
        when(registry.supportsProvider(PT_DOLPHIN_HOTEL)).thenReturn(true);
        service.registerRefundedService(oi, null);
        verify(repository, times(1)).saveAll(argThat(events -> Iterables.size(events) == 1));
    }

    @Test
    public void registerRefundedService_refundWithCorrection() {
        OrderItem oi = orderItem(PT_DOLPHIN_HOTEL);
        properties.setEnabled(true);
        when(registry.supportsProvider(PT_DOLPHIN_HOTEL)).thenReturn(true);

        FinancialEvent e1 = FinancialEvent.builder().payoutAt(Instant.now()).type(FinancialEventType.PAYMENT).build();
        FinancialEvent e2 = FinancialEvent.builder().payoutAt(Instant.now()).type(FinancialEventType.PAYMENT).build();
        FinancialEvent e3 = FinancialEvent.builder().payoutAt(Instant.now()).type(FinancialEventType.REFUND).build();
        when(repository.findOriginalPaymentEvent(any())).thenReturn(e1);
        when(fakeFinDataProvider.onRefund(any(), any(), any())).thenReturn(List.of(e2, e3));

        service.registerRefundedService(oi, null);
        verify(repository, times(1)).saveAll(argThat(events -> Iterables.size(events) == 2));

        assertThat(e2.getOriginalEvent()).isNull();
        assertThat(e3.getOriginalEvent()).isEqualTo(e1);
    }

    private OrderItem orderItem(EServiceType type) {
        OrderItem item = Mockito.mock(OrderItem.class);
        when(item.getPublicType()).thenReturn(type);
        Order order = Mockito.mock(Order.class);
        TrustInvoice invoice = Mockito.mock(TrustInvoice.class);
        when(order.getCurrentInvoice()).thenReturn(invoice);
        when(item.getOrder()).thenReturn(order);
        return item;
    }
}
