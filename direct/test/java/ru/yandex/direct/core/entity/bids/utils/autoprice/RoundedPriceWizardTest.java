package ru.yandex.direct.core.entity.bids.utils.autoprice;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;

import static org.assertj.core.api.Assertions.assertThat;

public class RoundedPriceWizardTest {

    @Test
    public void calcPrice_whenBidGreaterThenMaxShowBid() throws Exception {
        BigDecimal actual =
                new RoundedPriceWizard<Void>(context -> BigDecimal.valueOf(169L), null, CurrencyCode.YND_FIXED)
                        .calcPrice(null);
        assertThat(actual).isEqualByComparingTo(BigDecimal.valueOf(168L));
    }

    @Test
    public void calcPrice_success() throws Exception {
        BigDecimal actual =
                new RoundedPriceWizard<Void>(context -> BigDecimal.valueOf(167L), null, CurrencyCode.YND_FIXED)
                        .calcPrice(null);
        assertThat(actual).isEqualByComparingTo(BigDecimal.valueOf(167L));
    }
}
