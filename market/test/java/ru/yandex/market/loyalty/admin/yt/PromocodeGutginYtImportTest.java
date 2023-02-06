package ru.yandex.market.loyalty.admin.yt;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import Market.Promo.Promo.PromoDetails;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.accounting.Account;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.promo.CorePromoType;
import ru.yandex.market.loyalty.core.model.promo.NominalStrategy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;

import static NMarket.Common.Promo.Promo.ESourceType.GUTGIN;
import static NMarket.Common.Promo.Promo.ESourceType.UNKNOWN_VALUE;
import static NMarket.Common.Promo.Promo.ESourceType.GUTGIN_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy.Type.TO_END_OF_PROMO;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.ADDITIONAL_CONDITIONS_TEXT;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.ANAPLAN_ID;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.BIND_ONLY_ONCE;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.COIN_CREATION_REASON;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.COUPON_EMISSION_DATE_FROM;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.COUPON_EMISSION_DATE_TO;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.IMPORTED;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.LANDING_URL;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.PROMO_OFFER_AND_ACCEPTANCE;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.PROMO_SOURCE;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.UPDATED_AT;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.NOT_IMPORT_AND_CREATE_PROMOCODE_FROM_COLLECTED_PROMO_DETAILS;

public class PromocodeGutginYtImportTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SHOP_PROMO_ID = "#1111";
    private static final String ANAPLAN_PROMO_ID = "#1111";
    private static final String PROMO_KEY = "PP8uzJankfi0gckoQuQFPA==";
    private static final String UPDATE_PROMO_KEY = "VV9uzKewRTi0gckoQuQFPA==";
    private static final long FEED_ID = 0;
    private static final long PERCENT_VALUE = 5;
    private static final long MAX_VALUE = 60_000;
    private static final long BUDGET_VALUE = 10_000_000;
    private static final String CONDITIONS_TEXT = "На 1 заказ и на скидку не более 3000₽";
    private static final String CURRENCY = "RUB";
    private static final String IDX_LANDING_URL = "idx landing url";
    private static final String IDX_URL = "idx url";

    @Autowired
    private PromoYtTestHelper promoYtTestHelper;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private PromoYtImporter promoYtImporter;
    @Autowired
    private PromoService promoService;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private CoinDao coinDao;

    private PromoDetails promoDetailsDescription;
    private String promoCode;
    private ZonedDateTime current;

    @Override
    @Before
    public void initMocks() {
        current = clock.dateTime().atZone(clock.getZone());
        promoCode = promocodeService.generateNewPromocode();

        PromoDetails.Restrictions restrictions = PromoDetails.Restrictions.newBuilder()
                .setBindOnlyOnce(Boolean.TRUE)
                .setOrderMaxPrice(PromoDetails.Money.newBuilder()
                        .setValue(MAX_VALUE)
                        .setCurrency(CURRENCY)
                        .build())
                .setBudgetLimit(PromoDetails.Money.newBuilder()
                        .setValue(BUDGET_VALUE)
                        .setCurrency(CURRENCY)
                        .build())
                .build();

        promoDetailsDescription = PromoDetails.newBuilder()
                .setType(ReportPromoType.PROMOCODE.getCode())
                .setAnaplanPromoId(ANAPLAN_PROMO_ID)
                .setShopPromoId(SHOP_PROMO_ID)
                .setStartDate(current.toEpochSecond())
                .setEndDate(current.plusDays(10).toEpochSecond())
                .setLandingUrl(IDX_LANDING_URL)
                .setUrl(IDX_URL)
                .setPromoCode(promoCode)
                .setSourceType(GUTGIN)
                .setDiscount(PromoDetails.Money.newBuilder().setValue(PERCENT_VALUE).build())
                .setGenerationTs(current.toEpochSecond())
                .setConditions(CONDITIONS_TEXT)
                .setRestrictions(restrictions)
                .build();
        configurationService.enable(NOT_IMPORT_AND_CREATE_PROMOCODE_FROM_COLLECTED_PROMO_DETAILS);
    }

    private void downloadFromIdx() {
        promoYtTestHelper.withMock(
                dataBuilder -> dataBuilder.promo(FEED_ID, PROMO_KEY, promoDetailsDescription.toBuilder()),
                promoYtImporter::importPromos
        );
    }

    private void updatePromoKeyFromIdx() {
        PromoDetails.Builder builder =
                promoDetailsDescription.toBuilder().setGenerationTs(current.plusDays(2).toEpochSecond());
        promoYtTestHelper.withMock(
                dataBuilder -> dataBuilder.promo(FEED_ID, UPDATE_PROMO_KEY, builder),
                promoYtImporter::importPromos
        );
    }

    @Test
    public void shouldDownloadPromocode() {
        downloadFromIdx();

        Promo promo = promoService.getPromoByShopPromoId(SHOP_PROMO_ID);
        Account account = budgetService.getAccount(promo.getBudgetAccountId());
        CoinProps coinProps = coinDao.getCoinPropsPrototypeByPromoId(promo.getPromoId().getId()).orElseThrow();
        RulesContainer rulesContainer = coinProps.getRulesContainer();

        assertEquals(promo.getPromoType(), CorePromoType.SMART_SHOPPING);
        assertEquals(promo.getPromoSubType(), PromoSubType.PROMOCODE);
        assertTrue(promo.getPromoParam(IMPORTED).orElse(false));
        assertEquals(
                promo.getPromoParam(COIN_CREATION_REASON).orElse(null),
                CoreCoinCreationReason.FOR_USER_ACTION
        );
        assertEquals(promo.getPlatform().getApiPlatform(), MarketPlatform.BLUE);
        assertEquals(promo.getNominalStrategy(), NominalStrategy.DefaultStrategy.instance());
        assertEquals(account.getBudgetThreshold().doubleValue(), BigDecimal.ZERO.doubleValue());
        assertFalse(account.getCanBeRestoredFromReserveBudget());
        assertEquals(promo.getStatus(), PromoStatus.INACTIVE);
        assertEquals(promo.getShopPromoId(), SHOP_PROMO_ID);
        assertEquals(promo.getPromoParam(ANAPLAN_ID).orElse(null), ANAPLAN_PROMO_ID);

        assertEquals(promo.getShopPromoId(), promo.getPromoParam(ANAPLAN_ID).orElse(null));
        assertEquals(promo.getActionCode(), promoCode);
        assertEquals(promo.getName(), "Promocode " + promoCode);
        assertEquals(promo.getStartDate().getTime() / 1000, current.toEpochSecond());
        assertEquals(promo.getEndDate().getTime() / 1000, current.plusDays(10).toEpochSecond());
        assertEquals(
                promo.getPromoParam(COUPON_EMISSION_DATE_FROM).orElse(null),
                promo.getStartDate()
        );
        assertEquals(
                promo.getPromoParam(COUPON_EMISSION_DATE_TO).orElse(null),
                promo.getEndDate()
        );
        assertEquals(promo.getPromoParam(PROMO_SOURCE).orElse(UNKNOWN_VALUE), GUTGIN_VALUE);
        assertEquals(promo.getPromoParam(LANDING_URL).orElse(null), IDX_LANDING_URL);
        assertEquals(promo.getPromoParam(PROMO_OFFER_AND_ACCEPTANCE).orElse(null), IDX_URL);
        assertEquals(promo.getPromoParam(BIND_ONLY_ONCE).orElse(false), Boolean.TRUE);
        assertEquals(promo.getPromoParam(ADDITIONAL_CONDITIONS_TEXT).orElse(null), CONDITIONS_TEXT);
        assertEquals(promo.getCurrentBudget().doubleValue(), BigDecimal.valueOf(BUDGET_VALUE).doubleValue());
        assertEquals(promo.getPromoParam(UPDATED_AT).orElse(new Date()).getTime() / 1000, current.toEpochSecond());
        assertEquals(promo.getPromoKey(), PROMO_KEY);

        assertTrue(rulesContainer.hasRule(RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE));
        assertEquals(
                rulesContainer.get(RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE)
                        .getParam(RuleParameterName.MAX_ORDER_TOTAL).orElse(BigDecimal.ZERO),
                BigDecimal.valueOf(MAX_VALUE)
        );
        assertEquals(coinProps.getNominal().doubleValue(), BigDecimal.valueOf(PERCENT_VALUE).doubleValue());
        assertEquals(coinProps.getType(), CoreCoinType.PERCENT);
        assertEquals(coinProps.getExpirationPolicy().getType(), TO_END_OF_PROMO);
    }

    @Test
    public void shouldUpdatePromoKeyPromocode() {
        downloadFromIdx();

        Promo promo = promoService.getPromoByShopPromoId(SHOP_PROMO_ID);
        assertEquals(promo.getPromoParam(UPDATED_AT).orElse(new Date()).getTime() / 1000, current.toEpochSecond());
        assertEquals(promo.getPromoKey(), PROMO_KEY);

        updatePromoKeyFromIdx();

        promo = promoService.getPromoByShopPromoId(SHOP_PROMO_ID);
        assertEquals(
                promo.getPromoParam(UPDATED_AT).orElse(new Date()).getTime() / 1000,
                current.plusDays(2).toEpochSecond()
        );
        assertEquals(promo.getPromoKey(), UPDATE_PROMO_KEY);
    }
}
