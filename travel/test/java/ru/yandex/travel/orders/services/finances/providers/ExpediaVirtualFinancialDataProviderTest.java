package ru.yandex.travel.orders.services.finances.providers;

import java.math.BigDecimal;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.hotels.common.orders.ExpediaHotelItinerary;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.services.finances.OverallServiceBalance;
import ru.yandex.travel.orders.workflows.orderitem.expedia.ExpediaProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpediaVirtualFinancialDataProviderTest {
    private final ExpediaProperties properties = ExpediaProperties.builder()
            .approximateFeeRate(new BigDecimal("0.08"))
            .build();
    private final ExpediaVirtualFinancialDataProvider provider = new ExpediaVirtualFinancialDataProvider(properties);

    @Test
    public void testOverallBalance() {
        OrderItem service = testService(10_000);
        OverallServiceBalance balance = provider.getApproximateOverallServiceBalance(service);

        assertThat(balance.getPartner()).isEqualTo(Money.of(9_200, "RUB"));
        assertThat(balance.getFee()).isEqualTo(Money.of(800, "RUB"));
    }

    private OrderItem testService(int price) {
        ExpediaOrderItem service = new ExpediaOrderItem();
        ExpediaHotelItinerary itinerary = new ExpediaHotelItinerary();
        service.setItinerary(ExpediaHotelItinerary.builder()
                .fiscalPrice(Money.of(price, "RUB"))
                .build());
        return service;
    }
}
