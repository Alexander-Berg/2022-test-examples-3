package ru.yandex.travel.orders.workflows.orderitem.suburban.handlers;

import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.SuburbanOrderItem;
import ru.yandex.travel.orders.entities.VatType;
import ru.yandex.travel.orders.factories.SuburbanOrderItemEnvProviderFactory;
import ru.yandex.travel.orders.services.suburban.environment.SuburbanOrderItemEnvProvider;
import ru.yandex.travel.orders.services.suburban.providers.SuburbanProviderBase;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflows.orderitem.suburban.SuburbanProperties;
import ru.yandex.travel.suburban.model.SuburbanReservation;
import ru.yandex.travel.suburban.partners.SuburbanCarrier;
import ru.yandex.travel.suburban.partners.SuburbanProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservingStateHandlerTest {
    private ReservingStateHandler handler;
    private final SuburbanProperties props = SuburbanProperties.builder()
            .providers(SuburbanProperties.Providers.builder()
                    .movista(SuburbanProperties.MovistaProps.builder()
                            .common(SuburbanProperties.ProviderProps.builder()
                                    .fiscalInfo(SuburbanProperties.FiscalInfo.builder()
                                            .inn("12345")
                                            .title("Partner Inc").build()).build()).build()).build()).build();

    @Before
    public void setUp() {
        handler = new ReservingStateHandler(SuburbanOrderItemEnvProviderFactory.createEnvProvider(props));
    }

    @Test
    public void testGenerateFiscalItems() {
        var orderItem = createItem(EOrderItemState.IS_NEW);
        orderItem.getPayload().setPrice(Money.of(72, ProtoCurrencyUnit.RUB));
        SuburbanOrderItemEnvProvider env = SuburbanOrderItemEnvProviderFactory.createEnvProvider(props);
        SuburbanProviderBase provider = env.createEnv(orderItem).createSuburbanProvider();
        handler.generateFiscalItems(orderItem, provider);

        List<FiscalItem> fiscalItems = orderItem.getFiscalItems();
        assertThat(fiscalItems.size()).isEqualTo(1);
        FiscalItem fiscalItem = fiscalItems.get(0);
        assertThat(fiscalItem).isEqualTo(FiscalItem.builder()
                .inn("12345")
                .title("Partner Inc")
                .vatType(VatType.VAT_NONE)
                .type(FiscalItemType.SUBURBAN_MOVISTA_TICKET)
                .moneyAmount(Money.of(72, ProtoCurrencyUnit.RUB))
                .build());
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
