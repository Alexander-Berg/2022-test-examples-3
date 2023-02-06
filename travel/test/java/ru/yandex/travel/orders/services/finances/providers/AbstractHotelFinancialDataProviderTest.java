package ru.yandex.travel.orders.services.finances.providers;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.TestOrderObjects.moneyMarkup;
import static ru.yandex.travel.orders.services.finances.providers.AbstractHotelFinancialDataProvider.fitMarkupToActualPrice;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

public class AbstractHotelFinancialDataProviderTest {

    @Test
    public void testFitMarkupToActualPrice() {
        assertThat(fitMarkupToActualPrice(moneyMarkup(1, 9), rub(12))).isEqualTo(moneyMarkup(1, 11));
        assertThat(fitMarkupToActualPrice(moneyMarkup(1, 9), rub(8))).isEqualTo(moneyMarkup(1, 7));
        assertThat(fitMarkupToActualPrice(moneyMarkup(9, 1), rub(12))).isEqualTo(moneyMarkup(9, 3));
        assertThat(fitMarkupToActualPrice(moneyMarkup(9, 1), rub(8))).isEqualTo(moneyMarkup(8, 0));
    }
}
