package ru.yandex.market.loyalty.core.dao;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.flash.FlashPromoDao;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription.FlashPromoDescriptionBuilder;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoStatus;
import ru.yandex.market.loyalty.core.service.flash.FlashPromoService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;

import java.util.Set;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.mixin;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.ends;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.flashDescription;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.starts;


public class FlashPromoDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final String PROMO_KEY = "promo";
    private static final String SHOP_PROMO_ID = "shop promo id";

    @Autowired
    private FlashPromoDao flashPromoDao;
    @Autowired
    private FlashPromoService flashPromoService;

    private FlashPromoDescription existed;

    @Before
    public void configure() {
        existed = flashPromoService.createPromo(flashPromo(shopPromoId(SHOP_PROMO_ID)));
    }

    @Test
    public void shouldUpdateFlashPromoStatusById() {
        assertThat(existed.getStatus(), is(FlashPromoStatus.ACTIVE));

        assertThat(flashPromoDao.update(existed.toBuilder()
                .status(FlashPromoStatus.INACTIVE)
                .build(), existed), allOf(
                hasProperty("id", is(existed.getId())),
                hasProperty("status", is(FlashPromoStatus.INACTIVE))
        ));
    }

    @Test
    public void shouldUpdateMainFlashPromoPropertiesById() {
        assertThat(flashPromoDao.update(existed.toBuilder()
                .feedId(321)
                .shopPromoId("another")
                .promoKey("another")
                .build(), existed), allOf(
                hasProperty("id", is(existed.getId())),
                hasProperty("feedId", comparesEqualTo(321L)),
                hasProperty("shopPromoId", is("another")),
                hasProperty("promoKey", is("another"))
        ));
    }

    @Test
    public void shouldUpdateFlashPromoPropertiesById() {
        assertThat(flashPromoDao.update(existed.toBuilder()
                .feedId(321)
                .shopPromoId("another")
                .promoKey("another")
                .build(), existed), allOf(
                hasProperty("id", is(existed.getId())),
                hasProperty("feedId", comparesEqualTo(321L)),
                hasProperty("shopPromoId", is("another")),
                hasProperty("promoKey", is("another"))
        ));
    }

    @Test
    public void shouldUpdateStatusById() {
        flashPromoDao.updateStatusById(ImmutableSet.of(existed.getId()), FlashPromoStatus.INACTIVE);

        FlashPromoDescription flashPromoDescription = flashPromoDao.get(existed.getId());
        assertThat(flashPromoDescription, notNullValue());
        assertThat(flashPromoDescription.getStatus(), is(FlashPromoStatus.INACTIVE));
    }

    @Test
    public void shouldUpdateStatusByPromoKey() {
        flashPromoDao.updateStatusByPromoKey(ImmutableSet.of(existed.getPromoKey()), FlashPromoStatus.INACTIVE);

        FlashPromoDescription flashPromoDescription = flashPromoDao.get(existed.getId());
        assertThat(flashPromoDescription, notNullValue());
        assertThat(flashPromoDescription.getStatus(), is(FlashPromoStatus.INACTIVE));
    }

    @Test
    public void shouldUpdateFlashPromoPropertiesByExistedRecord() {
        FlashPromoDescription existed = flashPromoService.createPromo(flashPromo(shopPromoId(SHOP_PROMO_ID)));

        assertThat(flashPromoDao.update(flashPromo(
                promoKey("another"),
                shopPromoId(SHOP_PROMO_ID)
        ), existed), allOf(
                hasProperty("id", is(existed.getId())),
                hasProperty("feedId", comparesEqualTo(existed.getFeedId())),
                hasProperty("shopPromoId", is(existed.getShopPromoId())),
                hasProperty("promoKey", not(existed.getPromoKey()))
        ));
    }

    @Test
    public void shouldGetPromoById() {
        assertThat(flashPromoDao.get(existed.getId()), notNullValue());
    }

    @Test
    public void shouldGetExistedPromo() {
        FlashPromoDescription flashPromoDescription = flashPromoDao.selectExisted(flashPromo(shopPromoId(SHOP_PROMO_ID)));

        assertThat(flashPromoDescription, notNullValue());
        assertThat(flashPromoDescription.getId(), is(existed.getId()));
    }

    @Test
    public void shouldGetFirstPromoBySelector() {
        FlashPromoDescription flashPromoDescription = flashPromoDao.selectFirst(
                FlashPromoDao.activeOn(clock.dateTime())
                        .and(FlashPromoDescription.PROMO_KEY.eqTo(existed.getPromoKey()))
        );

        assertThat(flashPromoDescription, notNullValue());
        assertThat(flashPromoDescription.getId(), is(existed.getId()));
    }

    @Test
    public void shouldGetPromosBySelectors() {
        flashPromoService.createPromo(flashPromo(
                feedId(321),
                promoKey("some"),
                shopPromoId("some promo")
        ));

        Set<FlashPromoDescription> flashPromoDescriptions = flashPromoDao.select(
                FlashPromoDao.activeOn(clock.dateTime())
                        .and(FlashPromoDescription.SHOP_PROMO_ID.in("some promo", SHOP_PROMO_ID))
        );

        assertThat(flashPromoDescriptions, hasSize(2));
    }

    @Test
    public void shouldGetPromoKeysBySelectors() {
        flashPromoService.createPromo(flashPromo(
                feedId(321),
                promoKey("some"),
                shopPromoId("some promo")
        ));

        assertThat(flashPromoDao.selectPromoKeys(
                FlashPromoDao.activeOn(clock.dateTime())
        ), hasItems(PROMO_KEY, "some"));
    }

    @Test
    public void shouldGetIdsBySelectors() {
        flashPromoService.createPromo(flashPromo(
                feedId(321),
                promoKey("some"),
                shopPromoId("some promo")
        ));

        assertThat(flashPromoDao.selectIds(
                FlashPromoDao.activeOn(clock.dateTime())
        ), hasSize(2));
    }

    @SafeVarargs
    private FlashPromoDescription flashPromo(
            BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder>... customizers
    ) {
        return flashDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(PROMO_KEY),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                mixin(customizers)
        );
    }
}
