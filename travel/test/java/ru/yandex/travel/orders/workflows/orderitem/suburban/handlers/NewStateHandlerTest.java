package ru.yandex.travel.orders.workflows.orderitem.suburban.handlers;

import java.util.UUID;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.SuburbanOrderItem;
import ru.yandex.travel.orders.factories.SuburbanOrderItemEnvProviderFactory;
import ru.yandex.travel.orders.services.partners.BillingPartnerService;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.TReservationStart;
import ru.yandex.travel.orders.workflow.orderitem.suburban.proto.TBeforeReservation;
import ru.yandex.travel.orders.workflow.orderitem.suburban.proto.TReservationCommit;
import ru.yandex.travel.orders.workflows.orderitem.suburban.SuburbanProperties;
import ru.yandex.travel.suburban.model.SuburbanReservation;
import ru.yandex.travel.suburban.partners.SuburbanCarrier;
import ru.yandex.travel.suburban.partners.SuburbanProvider;
import ru.yandex.travel.workflow.BasicMessagingContext;
import ru.yandex.travel.workflow.BasicStateMessagingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NewStateHandlerTest {
    private NewStateHandler handler;
    private final SuburbanProperties props = SuburbanProperties.builder()
            .providers(SuburbanProperties.Providers.builder()
                    .movista(SuburbanProperties.MovistaProps.builder()
                            .common(SuburbanProperties.ProviderProps.builder()
                                    .build()).build()).build()).build();

    @Before
    public void setUp() {
        handler = new NewStateHandler(mock(BillingPartnerService.class), SuburbanOrderItemEnvProviderFactory.createEnvProvider(props));
    }

    @Test
    public void testOrderReserved() {
        var orderItem = createItem(EOrderItemState.IS_NEW);
        BasicMessagingContext<SuburbanOrderItem> msgCtx = new BasicMessagingContext<>(UUID.randomUUID(), orderItem, 0, 0);
        var ctx = BasicStateMessagingContext.fromMessagingContext(msgCtx);
        handler.handleEvent(TReservationStart.newBuilder().build(), ctx);

        assertThat(orderItem.getItemState()).isEqualTo(EOrderItemState.IS_RESERVING);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TBeforeReservation.class);
        assertThat(ctx.getScheduledEvents().get(1).getMessage()).isInstanceOf(TReservationCommit.class);
    }

    private SuburbanOrderItem createItem(EOrderItemState state) {
        var item = new SuburbanOrderItem();
        item.setState(state);
        item.setReservation(SuburbanReservation.builder()
                .provider(SuburbanProvider.MOVISTA)
                .carrier(SuburbanCarrier.CPPK)
                .stationFrom(SuburbanReservation.Station.builder().build())
                .stationTo(SuburbanReservation.Station.builder().build())
                .price(Money.of(100.2, ProtoCurrencyUnit.RUB))
                .build());
        return item;
    }
}
