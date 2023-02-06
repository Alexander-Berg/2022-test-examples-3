package ru.yandex.direct.core.entity.deal.model;

import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.Percent;

import static java.math.BigDecimal.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class DealTest {

    @Test
    public void getAgencyMinPrice_success() {
        Deal deal = new Deal();
        deal.withCpm(valueOf(100.0))
                .withCurrencyCode(CurrencyCode.RUB)
                .withMarginRatio(Percent.fromPercent(valueOf(5)));

        // CPM = 100.0, премия Агенства 15%, комиссия Adfox 5%
        // Ожидаем (100+15%+5%) = (100*1.15*1.05) =
        Money expected = Money.valueOf(120.8, CurrencyCode.RUB);
        assertThat(deal.getAgencyMinPrice()).isEqualByComparingTo(expected);
    }

}
