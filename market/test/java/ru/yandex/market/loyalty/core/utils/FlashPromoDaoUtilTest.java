package ru.yandex.market.loyalty.core.utils;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoStatus;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.FlashPromoDaoUtil.merge;

public class FlashPromoDaoUtilTest {
    private static final long FEED_ID = 123;
    private static final String PROMO = "some promo";
    private static final String ANOTHER_PROMO = "another promo";

    @Test
    public void shouldUpdatePromoDescription() {
        FlashPromoDescription to = FlashPromoDescription.builder()
                .id(1L)
                .promoId(2L)
                .feedId(FEED_ID)
                .status(FlashPromoStatus.ACTIVE)
                .shopPromoId(PROMO)
                .promoKey(PROMO)
                .creationTime(LocalDateTime.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .description(randomString())
                .url(randomString())
                .build();

        FlashPromoDescription from = FlashPromoDescription.builder()
                .status(FlashPromoStatus.INACTIVE)
                .feedId(FEED_ID)
                .shopPromoId(PROMO)
                .promoKey(ANOTHER_PROMO)
                .startTime(LocalDateTime.now())
                .description("another")
                .url("another")
                .build();

        assertThat(merge(from, to), allOf(
                hasProperty("id", comparesEqualTo(1L)),
                hasProperty("promoId", comparesEqualTo(2L)),
                hasProperty("shopPromoId", is(PROMO)),
                hasProperty("promoKey", is(ANOTHER_PROMO)),
                hasProperty("description", is("another")),
                hasProperty("url", is("another"))
        ));
    }
}
