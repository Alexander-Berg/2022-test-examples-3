package ru.yandex.market.loyalty.core.service.promocode;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_CODE;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

public class PromocodeServiceTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final String PROMOCODE_FIRST_ORDER = "some promocode first order";
    private static final String PROMOCODE = "some promocode";
    private static final String PROMOCODE_OTHER = "some promocode second order";
    private static final long USER_ID = 123L;
    private static final long MUID = 1152921505053355864L;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private StorePromocodeService storePromocodeService;

    private Promo promocodePromo;

    @Before
    public void configure() {
        promocodePromo = promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode().setEmissionBudget(BigDecimal.ONE).setCode(PROMOCODE));
        promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFirstOrderPromocode().setEmissionBudget(BigDecimal.ONE).setCode(
                        PROMOCODE_FIRST_ORDER));
        promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode().setEmissionBudget(BigDecimal.ONE).setCode(
                        PROMOCODE_OTHER));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());
    }

    @Test
    public void shouldActivateCoinPromocode() {
        PromocodesActivationResult activateResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .externalPromocodes(Set.of(PROMOCODE))
                        .userId(USER_ID)
                        .build());

        assertThat(activateResult.getActivationResults(), hasItem(allOf(
                hasProperty("code", is(PROMOCODE)),
                hasProperty("activationResultCode", is(PromocodeActivationResultCode.SUCCESS)),
                hasProperty("coinKey", notNullValue())
        )));

        CoinKey coinKey = activateResult.getActivationResults().get(0).getCoinKey();

        Coin coin = coinService.search.getCoin(coinKey).get();

        assertThat(coin.getStatus(), comparesEqualTo(CoreCoinStatus.ACTIVE));
        assertThat(coin.getUid(), comparesEqualTo(USER_ID));
    }

    @Test
    public void shouldActivateCoinPromocodeForManyUsers() {

        List<PromocodesActivationResult> results = new ArrayList<>();
        LongStream.range(0L, 500L).forEach(uid -> results.add(promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(uid)
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build()))
        );

        assertThat(results, hasSize(500));
    }

    @Test
    public void shouldNotSpendAllEmissionOnCoinCreation() {
        budgetService.spendEmission(promocodePromo, BudgetMode.SYNC);

        assertThat(
                budgetService.getBalance(promocodePromo.getBudgetEmissionAccountId()),
                greaterThanOrEqualTo(BigDecimal.ZERO)
        );

        PromocodesActivationResult activateResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .externalPromocodes(Set.of(PROMOCODE))
                        .userId(USER_ID)
                        .build());

        assertThat(activateResult.getActivationResults(), hasItem(allOf(
                hasProperty("code", is(PROMOCODE)),
                hasProperty("activationResultCode", is(PromocodeActivationResultCode.SUCCESS)),
                hasProperty("coinKey", notNullValue())
        )));
    }

    @Test
    public void shouldNotActivateCoinPromocodeTwice() {
        promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .externalPromocodes(Set.of(PROMOCODE))
                        .userId(USER_ID)
                        .build());

        PromocodesActivationResult activateRequest = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .externalPromocodes(Set.of(PROMOCODE))
                        .userId(USER_ID)
                        .build());

        assertThat(activateRequest.getActivationResults(), hasItem(allOf(
                hasProperty("code", is(PROMOCODE)),
                hasProperty("activationResultCode", is(PromocodeActivationResultCode.ALREADY_ACTIVE)),
                hasProperty("coinKey", notNullValue())
        )));
    }

    @Test
    public void shouldNotActivateWithoutUserId() {

        PromocodesActivationResult activateResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build());

        assertThat(activateResult.getActivationResults(), hasItem(allOf(
                hasProperty("code", is(PROMOCODE)),
                hasProperty("activationResultCode", is(PromocodeActivationResultCode.ANONYMOUS_USER)),
                hasProperty("coinKey", nullValue())
        )));
    }

    @Test
    public void shouldNotActivatePromocodeForFirstOrderWithMuid() {

        PromocodesActivationResult activateResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(MUID)
                        .externalPromocodes(Set.of(PROMOCODE_FIRST_ORDER))
                        .build());

        assertThat(activateResult.getActivationResults(), hasItem(allOf(
                hasProperty("code", is(PROMOCODE_FIRST_ORDER)),
                hasProperty("activationResultCode", is(PromocodeActivationResultCode.ANONYMOUS_USER)),
                hasProperty("coinKey", nullValue())
        )));
    }

    @Test
    public void shouldNotActivatePromocodeWithMuid() {

        PromocodesActivationResult activateResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(MUID)
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build());

        assertThat(activateResult.getActivationResults(), hasItem(allOf(
                hasProperty("code", is(PROMOCODE)),
                hasProperty("activationResultCode", is(PromocodeActivationResultCode.ANONYMOUS_USER)),
                hasProperty("coinKey", nullValue())
        )));
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldExceptOverlappingPromoCode() {
        promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setCode(PROMOCODE)
        );
    }

    @Test
    public void shouldNotExceptOnOverlappingPromoCode() {
        final Instant endDate = this.promocodePromo.getEndDate().toInstant();
        final Promo promocodePromo = promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setStartDate(Date.from(endDate.plus(1, ChronoUnit.DAYS)))
                        .setEndDate(Date.from(endDate.plus(30, ChronoUnit.DAYS)))
                        .setCode(PROMOCODE)
        );

        assertThat(promocodePromo, notNullValue());
        assertThat(promocodePromo.getActionCode(), lessThanOrEqualTo(PROMOCODE));
    }

    @Test
    public void shouldActivateTwoPromocodes() {
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE, PROMOCODE_FIRST_ORDER)
        );

        final OperationContextDto operationContextDto = new OperationContextDto();
        operationContextDto.setUid(USER_ID);
        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContextDto)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(2));
        assertThat(promocodesActivationResult.getActivationResults(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("code", equalTo(PROMOCODE.toUpperCase()))
                        ),
                        allOf(
                                hasProperty("code", equalTo(PROMOCODE_FIRST_ORDER.toUpperCase()))
                        )
                )
        );
    }

    @Test
    public void shouldActivateForDifferentCartIdentity() {
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE)
        );

        String uuid = "1234567890123456789";

        storePromocodeService.savePromocodes(
                Identity.Type.UUID.buildIdentity(uuid),
                Set.of(PROMOCODE_OTHER)
        );

        var yandexUid = "214345";

        storePromocodeService.savePromocodes(
                Identity.Type.YANDEX_UID.buildIdentity(yandexUid),
                Set.of(PROMOCODE_FIRST_ORDER)
        );

        final OperationContextDto operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);
        operationContext.setUuid(uuid);
        operationContext.setYandexUid(yandexUid);

        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .useSaved(true)
                        .operationContext(operationContext)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(3));
        assertThat(promocodesActivationResult.getActivationResults(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("code", equalTo(PROMOCODE.toUpperCase()))
                        ),
                        allOf(
                                hasProperty("code", equalTo(PROMOCODE_OTHER.toUpperCase()))
                        ),
                        allOf(
                                hasProperty("code", equalTo(PROMOCODE_FIRST_ORDER.toUpperCase()))
                        )
                )
        );
    }

    @Test
    public void shouldSaveCouponAndActivatePromocode() {
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(PROMOCODE, DEFAULT_COUPON_CODE)
        );

        final OperationContextDto operationContextDto = new OperationContextDto();
        operationContextDto.setUid(USER_ID);
        var promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContextDto)
                        .useSaved(true)
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(1));
        assertThat(promocodesActivationResult.getActivationResults(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("code", equalTo(PROMOCODE.toUpperCase()))
                        )
                )
        );
    }

    @Test
    public void shouldNotSaveThePromocodeIfExisistACouponWithTheSameName() {
        Promo coupon = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("COUPON")
        );

        var exception = assertThrows(MarketLoyaltyException.class, () ->
                promoManager.createPromocodePromo(
                        PromoUtils.SmartShopping.defaultFixedPromocode()
                                .setCode("COUPON")
                )
        );
        assertEquals(MarketLoyaltyErrorCode.OTHER_ERROR, exception.getMarketLoyaltyErrorCode());
        assertEquals("Промокод COUPON совпадает с купоном: " +
                        "https://localhost/promo/" + coupon.getPromoId().getId() +
                        ", просьба обратится к команде сервиса Loyalty для решения проблемы",
                exception.getMessage()
        );
    }
}
