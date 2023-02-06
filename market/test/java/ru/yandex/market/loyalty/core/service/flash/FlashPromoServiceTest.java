package ru.yandex.market.loyalty.core.service.flash;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.flash.FlashPromoDao;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoStatus;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
public class FlashPromoServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final String PROMO = "some promo";
    private static final String ANOTHER_PROMO = "another promo";
    private static final String ANOTHER_ONE_PROMO = "another one promo";

    @Autowired
    private FlashPromoService flashPromoService;
    @Autowired
    private FlashPromoDao flashPromoDao;

    @Test
    public void shouldCreateFlashPromo() {
        FlashPromoDescription promo = flashPromoService.createPromo(flashPromo());

        haveRequiredProperties(promo);
        assertThat(promo.getStatus(), is(FlashPromoStatus.ACTIVE));
    }

    @Test
    public void shouldUpdateFlashPromoByFeed() {
        FlashPromoDescription firstVersion = flashPromoService.createPromo(flashPromo());

        assertThat(firstVersion.getId(), greaterThan(0L));

        assertThat(flashPromoService.createPromo(flashPromo(
                promoKey(ANOTHER_PROMO)
        )), allOf(
                hasProperty("id", is(firstVersion.getId())),
                hasProperty("promoKey", is(ANOTHER_PROMO))
        ));
    }

    @Test
    public void shouldCreateSnapshotOnEachUpdate() {
        FlashPromoDescription firstVersion = flashPromoService.createPromo(flashPromo());

        assertThat(firstVersion.getId(), greaterThan(0L));

        assertThat(flashPromoService.createPromo(flashPromo(
                promoKey(ANOTHER_PROMO)
        )), hasProperty("id", is(firstVersion.getId())));

        assertThat(flashPromoDao.selectFirst(
                FlashPromoDescription.PROMO_KEY.eqTo(firstVersion.getPromoKey())
                        .and(FlashPromoDescription.SNAPSHOT_VERSION.eqTo(true))), notNullValue());

        assertThat(flashPromoService.createPromo(flashPromo(
                promoKey(ANOTHER_ONE_PROMO)
        )), hasProperty("id", is(firstVersion.getId())));

        assertThat(flashPromoDao.select(
                FlashPromoDescription.SHOP_PROMO_ID.eqTo(firstVersion.getShopPromoId())
                        .and(FlashPromoDescription.SNAPSHOT_VERSION.eqTo(true))), hasSize(2));

        assertThat(
                flashPromoDao.selectPromoKeys(FlashPromoDao.activeOn(clock.dateTime())),
                hasItems(PROMO, ANOTHER_PROMO, ANOTHER_ONE_PROMO)
        );
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
        assertThat(promo.getFeedId(), is(FEED_ID));
        assertThat(promo.getShopPromoId(), notNullValue());
        assertThat(promo.getPromoKey(), notNullValue());
        assertThat(promo.getCreationTime(), notNullValue());
        assertThat(promo.getUpdateTime(), notNullValue());
        assertThat(promo.getDeactivationTime(), nullValue());
        assertThat(promo.getStartTime(), notNullValue());
        assertThat(promo.getRestrictions(), notNullValue());
    }
}
