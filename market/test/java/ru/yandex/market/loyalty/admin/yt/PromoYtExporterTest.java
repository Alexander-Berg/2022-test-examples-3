package ru.yandex.market.loyalty.admin.yt;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.model.PromoForYt;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtExporter;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.PROMO_SOURCE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_CASHBACK;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_CASHBACK_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PARENT_CATEGORY_ID;
import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY;
import static NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE;
import static NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE_VALUE;

public class PromoYtExporterTest extends MarketLoyaltyAdminMockedDbTest {

    @YtHahn
    @Autowired
    private PromoYtExporter promoYtExporter;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private Clock clock;
    @Autowired
    @YtHahn
    private Yt yt;
    @Autowired
    private PromoService promoService;

    @Test
    public void shouldQueryPromosFromFuture() {
        final int beforeTestSize = promoYtExporter.loadAndMapPromos().size();
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .setDescription("cashback promo description1")
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(1234))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_CASHBACK)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(PARENT_CATEGORY_ID))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .setDescription("cashback promo description2")
                        .setStartDate(Date.from(clock.instant().plus(6, ChronoUnit.HOURS)))
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(1234))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_CASHBACK)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(PARENT_CATEGORY_ID))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .setDescription("cashback promo description2")
                        .setStartDate(Date.from(clock.instant().plus(7, ChronoUnit.HOURS)))
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(1234))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_CASHBACK)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(PARENT_CATEGORY_ID))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .setDescription("cashback promo description2")
                        .setStartDate(Date.from(clock.instant().plus(13, ChronoUnit.HOURS)))
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(1234))
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, PerkType.YANDEX_CASHBACK)
                        .addCashbackRule(CATEGORY_FILTER_RULE, CATEGORY_ID, ImmutableSet.of(PARENT_CATEGORY_ID))
        );
        promoManager.createCashbackPromo(
                PromoUtils.PersonalCashback.defaultPerson()
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                        .addCashbackRule(RuleType.FAKE_USER_CUTTING_RULE, RuleParameterName.APPLIED_TO_FAKE_USER, true)
        );
        final Collection<PromoForYt> promoForYts = promoYtExporter.loadAndMapPromos().values();
        assertThat(promoForYts, hasSize(beforeTestSize + 3));
    }

    @Test
    public void shouldFilterFixedPromo() {
        final int beforeTestSize = promoYtExporter.loadAndMapPromos().size();

        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(200))
                        .setDescription("cashback that should be filtered")
        );

        final Collection<PromoForYt> promoForYts = promoYtExporter.loadAndMapPromos().values();
        assertThat(promoForYts, hasSize(beforeTestSize));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSavePromoKeyIndex() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(200)));

        when(yt.transactions().start(any(Optional.class), anyBoolean(), any(Duration.class), any(Map.class)))
                .thenReturn(GUID.valueOf("1-8e9c4f69-a3bc6964-3e99ab10"));

        promoYtExporter.exportToYt();
        promoYtExporter.exportToYt();

        cashbackCacheService.reloadCashbackPromos();
        assertEquals(1, cashbackCacheService.getReportToLoyaltyPromoKeyIndex().size());
    }

    @Test
    public void shouldFilterCashbackByFlag() {
        var beforeTestSize = promoYtExporter.loadAndMapPromos().size();

        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .setDontUploadToIdx(true)
                        .setDescription("cashback that should be filtered")
        );

        var promoForYts = promoYtExporter.loadAndMapPromos().values();
        assertThat("There is an unfiltered promo", promoForYts, hasSize(beforeTestSize));
    }

    @Test
    public void shouldMapCashbackPromoSourceOrDefault() {
        var promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));
        var promoForYts = promoYtExporter.loadAndMapPromos().values().toArray();
        assertThat(((PromoForYt)promoForYts[0]).getPromo().getSourceType(), is(LOYALTY));

        promoService.setPromoParam(promo.getPromoId().getId(), PROMO_SOURCE, PARTNER_SOURCE_VALUE);
        promoForYts = promoYtExporter.loadAndMapPromos().values().toArray();
        assertThat(((PromoForYt)promoForYts[0]).getPromo().getSourceType(), is(PARTNER_SOURCE));
    }
}
