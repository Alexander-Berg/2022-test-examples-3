package ru.yandex.market.loyalty.admin.tms.yt.promo.export;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.PromoYtUpdateProcessor;
import ru.yandex.market.loyalty.admin.yt.PromoYtTestHelper;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.blueSet;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.source;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;

@TestFor(PromoYtUpdateProcessor.class)
public class PromoYtUpdateProcessorFailureTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private PromoYtUpdateProcessor bundlesUpdateProcessor;
    @Autowired
    private PromoBundleService promoBundleService;
    @Autowired
    private PromoYtTestHelper promoBundlesTestHelper;

    @Test
    public void shouldNotDisablePromoOnClientFailure() {
        PromoBundleDescription expectedDescription = promoBundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                shopPromoId(randomString()),
                source(YtSource.FIRST_PARTY_PIPELINE.getCode()),
                promoKey("some expired promo"),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                123,
                                proportion(randomString(), 30),
                                proportion(randomString(), 30)
                        )),
                        primary()
                )
        ));

        promoBundlesTestHelper.usingMock(
                db -> {
                    throw new RuntimeException("some yt problem");
                },
                bundlesUpdateProcessor::updatePromoFromYt
        );

        assertThat(promoBundleService.getActivePromoKeys(), hasItem(
                expectedDescription.getPromoKey()
        ));
    }
}
