package ru.yandex.travel.orders.entities;

import java.time.Clock;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.factories.SuburbanOrderItemEnvProviderFactory;
import ru.yandex.travel.orders.services.orders.OrderCompatibilityUtils;
import ru.yandex.travel.orders.services.payments.DefaultTrustPaymentPolicy;
import ru.yandex.travel.orders.services.payments.InvoicePaymentFlags;
import ru.yandex.travel.orders.services.payments.TrustPaymentPolicy;
import ru.yandex.travel.orders.services.payments.TrustPaymentPolicyProperties;
import ru.yandex.travel.orders.services.payments.model.TrustTerminalForPartner;
import ru.yandex.travel.orders.services.promo.mir2020.Mir2020PromoService;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.suburban.model.SuburbanReservation;
import ru.yandex.travel.suburban.partners.SuburbanCarrier;
import ru.yandex.travel.suburban.partners.SuburbanProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TestTrustInvoice {
    public GenericOrder createSuburbanOrder(SuburbanProvider provider) {
        var order = new GenericOrder();
        order.setState(EOrderState.OS_RESERVED);
        order.setCurrency(ProtoCurrencyUnit.RUB);

        var orderItem = new SuburbanOrderItem();
        orderItem.setState(EOrderItemState.IS_RESERVED);

        orderItem.setReservation(SuburbanReservation.builder()
                .provider(provider)
                .carrier(SuburbanCarrier.SZPPK)
                .price(Money.of(72, ProtoCurrencyUnit.RUB))
                .stationFrom(SuburbanReservation.Station.builder().titleDefault("Одинцово").build())
                .stationTo(SuburbanReservation.Station.builder().titleDefault("Москва (Белорусский вокзал)").build())
                .build());
        order.addOrderItem(orderItem);
        return order;
    }

    private FiscalItem createFiscalItem(GenericOrder order) {
        SuburbanOrderItem orderItem = OrderCompatibilityUtils.getSuburbanOrderItem(order);
        return FiscalItem.builder()
                .orderItem(orderItem)
                .moneyAmount(orderItem.getPayload().getPrice())
                .build();
    }

    @Test
    public void testCreateSuburbanInvoice() {
        TrustPaymentPolicy trustPaymentPolicy = new DefaultTrustPaymentPolicy(
                mock(Clock.class), mock(TrustPaymentPolicyProperties.class), mock(Mir2020PromoService.class),
                SuburbanOrderItemEnvProviderFactory.createEnvProvider()
        );

        GenericOrder order = createSuburbanOrder(SuburbanProvider.MOVISTA);
        InvoicePaymentFlags paymentFlags = trustPaymentPolicy.getInvoicePaymentFlags(order);
        assertThat(paymentFlags.getTerminalForPartner()).isEqualTo(TrustTerminalForPartner.MOVISTA);

        order = createSuburbanOrder(SuburbanProvider.IM);
        paymentFlags = trustPaymentPolicy.getInvoicePaymentFlags(order);
        assertThat(paymentFlags.getTerminalForPartner()).isEqualTo(TrustTerminalForPartner.IM);
        TrustInvoice invoice = TrustInvoice.createInvoice(
                order,
                mock(AuthorizedUser.class),
                List.of(createFiscalItem(order)),
                paymentFlags);
        assertThat(invoice.getTerminalForPartner()).isEqualTo(TrustTerminalForPartner.IM);
    }
}
