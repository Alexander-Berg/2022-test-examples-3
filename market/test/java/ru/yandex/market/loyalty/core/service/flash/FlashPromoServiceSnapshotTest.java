package ru.yandex.market.loyalty.core.service.flash;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.flash.FlashPromoDao;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.Set;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.mixin;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.ends;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.flashDescription;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.starts;

@TestFor(RegularFlashPromoService.class)
public class FlashPromoServiceSnapshotTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long FEED_ID = 123L;
    private static final String PROMO = "some promo";

    @Autowired
    private FlashPromoService flashPromoService;
    @Autowired
    private FlashPromoDao flashPromoDao;

    @Test
    public void shouldCreateDifferentPromoSnapshotVersions() {
        FlashPromoDescription promo = flashPromoService.createPromo(flashPromo());

        haveRequiredProperties(promo);

        int count = 10;

        for (int i = 0; i < count; i++) {
            flashPromoService.createPromo(flashPromo(
                    promoKey(PROMO + i)
            ));
        }

        Set<FlashPromoDescription> flashPromoDescriptions = flashPromoDao.select(
                FlashPromoDescription.FEED_ID.eqTo(FEED_ID),
                FlashPromoDescription.SHOP_PROMO_ID.eqTo(PROMO),
                FlashPromoDescription.SNAPSHOT_VERSION.eqTo(true),
                FlashPromoDao.activeOn(clock.dateTime())
        );

        assertThat(flashPromoDescriptions, hasSize(count));
    }

    @Test
    public void shouldCreateSnapshotVersionOnCall() {
        FlashPromoDescription promo = flashPromoService.createPromo(flashPromo());

        haveRequiredProperties(promo);

        assertThat(flashPromoService.createSnapshotPromo(flashPromo(
                promoKey("previous")
        )), notNullValue());

        Set<FlashPromoDescription> flashPromoDescriptions = flashPromoDao.select(
                FlashPromoDescription.FEED_ID.eqTo(FEED_ID),
                FlashPromoDescription.SHOP_PROMO_ID.eqTo(PROMO),
                FlashPromoDescription.SNAPSHOT_VERSION.eqTo(true),
                FlashPromoDao.activeOn(clock.dateTime())
        );

        assertThat(flashPromoDescriptions, hasSize(1));
    }

    @Test
    public void shouldNotCreateSnapshotVersionWithSameParams() {
        FlashPromoDescription promo = flashPromoService.createPromo(flashPromo());

        haveRequiredProperties(promo);

        assertThat(flashPromoService.createSnapshotPromo(flashPromo(
                starts(promo.getStartTime()),
                ends(promo.getEndTime())
        )), nullValue());

        Set<FlashPromoDescription> flashPromoDescriptions = flashPromoDao.select(
                FlashPromoDescription.FEED_ID.eqTo(FEED_ID),
                FlashPromoDescription.SHOP_PROMO_ID.eqTo(PROMO),
                FlashPromoDescription.SNAPSHOT_VERSION.eqTo(true),
                FlashPromoDao.activeOn(clock.dateTime())
        );

        assertThat(flashPromoDescriptions, empty());
    }

    @SafeVarargs
    private FlashPromoDescription flashPromo(
            BuildCustomizer<FlashPromoDescription, FlashPromoDescription.FlashPromoDescriptionBuilder>... customizers
    ) {
        return flashDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(PROMO),
                shopPromoId(PROMO),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                mixin(customizers)
        );
    }

    private void haveRequiredProperties(FlashPromoDescription promo) {
        assertThat(promo, notNullValue());
        assertThat(promo.getId(), notNullValue());
        assertThat(promo.getPromoId(), notNullValue());
        assertThat(promo.getFeedId(), comparesEqualTo(FEED_ID));
        assertThat(promo.getShopPromoId(), notNullValue());
        assertThat(promo.getPromoKey(), notNullValue());
        assertThat(promo.getCreationTime(), notNullValue());
        assertThat(promo.getUpdateTime(), notNullValue());
        assertThat(promo.getDeactivationTime(), nullValue());
        assertThat(promo.getStartTime(), notNullValue());
        assertThat(promo.getRestrictions(), notNullValue());
    }
}
