package ru.yandex.market.marketpromo.core.data.filtration;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.misc.ExtendedClock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class PromoImportFilterTest {

    @Test
    void shouldTestPromoOnOldIdGeneration() {
        assertThat(PromoImportFilter.hasOldPromoId(Promos.promo(
                Promos.promoId("#3434"),
                Promos.directDiscount()
        )), is(true));
    }

    @Test
    void shouldTestPromoOnDateActivity() {
        assertThat(PromoImportFilter.activeByTime(new ExtendedClock(), Promos.promo(
                Promos.promoId("#3434"),
                Promos.starts(LocalDateTime.now()),
                Promos.ends(LocalDateTime.now().plusDays(1)),
                Promos.directDiscount()
        )), is(true));
    }

}
