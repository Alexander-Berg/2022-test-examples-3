package ru.yandex.market.loyalty.core.service.spread;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.spread.SpreadDiscountPromoDescription;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.loyalty.core.dao.promo.Queries.PROMO_TYPE_FIELD;
import static ru.yandex.market.loyalty.core.model.promo.CorePromoType.GENERIC_BUNDLE;
import static ru.yandex.market.loyalty.core.model.promo.CorePromoType.SPREAD_COUNT;
import static ru.yandex.market.loyalty.core.model.promo.CorePromoType.SPREAD_RECEIPT;

public class SpreadPromoServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final String DESCRIPTION = "description";
    private static final String ANAPLAN_ID = "anaplan";
    private static final String FIRST_SHOP_PROMO_ID = "first_shop_promo_id";
    private static final String SOURCE = "source";
    private static final String PROMO_KEY = "some promo key";

    private SpreadDiscountPromoDescription.SpreadDiscountPromoDescriptionBuilder spreadBuilder;

    @Autowired
    private SpreadPromoService spreadPromoService;
    @Autowired
    private PromoDao promoDao;

    @Before
    public void prepare() {
        PromoBundleUtils.enableAllBundleFeatures(configurationService);
        spreadBuilder = SpreadDiscountPromoDescription.builder()
                .promoSource(LOYALTY_VALUE)
                .source(SOURCE)
                .anaplanId(ANAPLAN_ID)
                .startTime(clock.dateTime())
                .endTime(clock.dateTime().plusYears(10))
                .description(DESCRIPTION)
                .name(PROMO_KEY)
                .allowBerubonus(true)
                .allowPromocode(false);
    }

    @After
    public void after() {
        PromoBundleUtils.disableAllBundleFeatures(configurationService);
    }

    @Test
    public void shouldCreateSpreadCountPromoOnce() {
        int initialSize = promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)).size();

        SpreadDiscountPromoDescription descriptionFirst = createFirstSpreadCount();
        SpreadDiscountPromoDescription descriptionLast = createFirstSpreadCount();

        assertThat(descriptionFirst.getPromoId(), equalTo(descriptionLast.getPromoId()));
        assertThat(initialSize + 1, comparesEqualTo(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(SPREAD_COUNT)).size()));
    }

    @Test
    public void shouldCreateSpreadReceiptPromoOnce() {
        int initialSize = promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(GENERIC_BUNDLE)).size();

        SpreadDiscountPromoDescription descriptionFirst = createFirstSpreadReceipt();
        SpreadDiscountPromoDescription descriptionLast = createFirstSpreadReceipt();

        assertThat(descriptionFirst.getPromoId(), equalTo(descriptionLast.getPromoId()));
        assertThat(initialSize + 1, comparesEqualTo(promoDao.getPromos(PROMO_TYPE_FIELD.eqTo(SPREAD_RECEIPT)).size()));
    }

    private SpreadDiscountPromoDescription createFirstSpreadCount() {
        return spreadPromoService.createOrUpdateSpreadPromo(spreadBuilder
                .promoSource(LOYALTY_VALUE)
                .feedId(FEED_ID)
                .promoKey(PROMO_KEY)
                .shopPromoId(FIRST_SHOP_PROMO_ID)
                .promoType(ReportPromoType.SPREAD_COUNT)
                .build());
    }

    private SpreadDiscountPromoDescription createFirstSpreadReceipt() {
        return spreadPromoService.createOrUpdateSpreadPromo(spreadBuilder
                .promoSource(LOYALTY_VALUE)
                .feedId(FEED_ID)
                .promoKey(PROMO_KEY)
                .shopPromoId(FIRST_SHOP_PROMO_ID)
                .promoType(ReportPromoType.SPREAD_RECEIPT)
                .build());
    }
}
