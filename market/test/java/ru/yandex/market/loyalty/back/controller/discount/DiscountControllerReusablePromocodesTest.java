package ru.yandex.market.loyalty.back.controller.discount;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.UserActivePromocodeEntryDao;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.UserPromocodeEntry;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinSearchService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodesActivationResult;
import ru.yandex.market.loyalty.core.service.promocode.UserPromocodeInfo;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;

@TestFor({DiscountController.class})
public class DiscountControllerReusablePromocodesTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final long USER_ID = 123L;
    private static final long FEED_ID = 123;
    private static final String FIRST_SSKU = "first offer";

    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private UserActivePromocodeEntryDao userActivePromocodeEntryDao;
    @Autowired
    private CoinSearchService coinSearchService;

    private String fixedPromoCode;
    private String reusablePromoCode;
    private Promo reusablePromo;

    @Before
    public void configure() {
        fixedPromoCode = promocodeService.generateNewPromocode();
        reusablePromoCode = promocodeService.generateNewPromocode();
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setBindOnlyOnce(true)
                .setCode(fixedPromoCode));
        reusablePromo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(reusablePromoCode));
    }

    @Test
    public void shouldCanApplyTwiceReusablePromocode() {
        for (var i = 0; i < 3; i++) {
            activatePromocodeAndSpend(reusablePromoCode);
        }
    }

    @Test
    @Repeat(5)
    public void shouldNotActiveTwoPromocodeInReusablePromo() throws InterruptedException {
        testConcurrency(() -> {
            promocodeService.activatePromocodes(
                    PromocodeActivationRequest.builder()
                            .userId(USER_ID)
                            .externalPromocodes(Set.of(reusablePromoCode))
                            .build());
            return () -> {
                final List<UserPromocodeEntry> userPromocodeEntries = userActivePromocodeEntryDao.selectActive(
                        UserActivePromocodeEntryDao.USER_ID.eqTo(USER_ID),
                        UserActivePromocodeEntryDao.CODE.eqTo(reusablePromoCode)
                );
                assertThat(userPromocodeEntries, hasSize(1));
            };
        });
    }

    @Test
    public void shouldNotApplyTwiceSingleUsePromocode() {
        promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(fixedPromoCode))
                        .build());


        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        Set<UserPromocodeInfo> promoCodeInfos = promocodeService.retrievePromocodeInfo(USER_ID, fixedPromoCode);

        MultiCartWithBundlesDiscountResponse discountResponseOnOrder1 = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order1)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(fixedPromoCode)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponseOnOrder1);

        assertThat(discountResponseOnOrder1.getCoins(), empty());
        assertThat(discountResponseOnOrder1.getUnusedPromocodes(), empty());
        assertThat(discountResponseOnOrder1.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));

        var promocodeInfo = promoCodeInfos.stream()
                .findFirst();
        assertThat(promocodeInfo.isPresent(), is(true));

        Coin coin1 = coinService.search.getCoin(promocodeInfo.get().getCoinKey()).get();

        assertThat(coin1.getStatus(), is(CoreCoinStatus.USED));

        promoCodeInfos = promocodeService.retrievePromocodeInfo(USER_ID, fixedPromoCode);

        promocodeInfo = promoCodeInfos.stream()
                .findFirst();

        assertThat(promocodeInfo.get().getCode(), is(fixedPromoCode));
        assertThat(promocodeInfo.get().getErrorCode(), nullValue());

        clock.spendTime(1, ChronoUnit.MILLIS);

        //Попытка повторной активации
        final PromocodesActivationResult promocodesActivationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(fixedPromoCode))
                        .build());

        assertThat(promocodesActivationResult.getActivationResults(), hasSize(1));
        assertThat(promocodesActivationResult.getActivationResults(),
                hasItems(
                        allOf(
                                hasProperty("activationResultCode", equalTo(PromocodeActivationResultCode.ERROR))
                        ))
        );

    }

    @Test
    public void shouldApplyReusbalePromocodeAfterRevertFirst() {
        final MultiCartWithBundlesDiscountResponse discountResponse = activatePromocodeAndSpend(reusablePromoCode);

        assertThat(coinSearchService.getCoinsByUid(USER_ID, 10), hasSize(1));
        assertThat(coinSearchService.getCoinsByUid(USER_ID, 10), hasItems(
                allOf(
                        hasProperty("status", equalTo(CoreCoinStatus.USED))
                )));

        marketLoyaltyClient.revertDiscount(discountResponse.getOrders()
                .stream()
                .flatMap(o -> o.getItems().stream())
                .flatMap(i -> i.getPromos().stream())
                .map(ItemPromoResponse::getDiscountToken)
                .collect(Collectors.toSet()));

        assertThat(coinSearchService.getCoinsByUid(USER_ID, 10), hasSize(1));
        assertThat(coinSearchService.getCoinsByUid(USER_ID, 10), hasItems(
                allOf(
                        hasProperty("status", equalTo(CoreCoinStatus.REVOKED))
                )));

        for (int i = 0; i < 2; i++) {
            activatePromocodeAndSpend(reusablePromoCode);
        }

        final List<UserPromocodeEntry> userPromocodeEntries = userActivePromocodeEntryDao.selectActive(
                UserActivePromocodeEntryDao.USER_ID.eqTo(USER_ID),
                UserActivePromocodeEntryDao.CODE.eqTo(reusablePromoCode)
        );

        assertThat(userPromocodeEntries, hasSize(0));

    }

    @Test
    public void shouldNotCreateMoreOneInActive() {
        CoinSearchService.CoinMaxIdAndStatus activePromoAndMaxCoinId = coinSearchService.hasActiveOrUsedPromoAndMaxCoinId(
                USER_ID, reusablePromo.getId());

        assertThat(activePromoAndMaxCoinId.isActive(), is(false));
        assertThat(activePromoAndMaxCoinId.getMaxCoinId(), nullValue());

        promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(reusablePromoCode))
                        .build());

        activePromoAndMaxCoinId = coinSearchService.hasActiveOrUsedPromoAndMaxCoinId(
                USER_ID, reusablePromo.getId());

        assertThat(activePromoAndMaxCoinId.isActive(), is(true));
        assertThat(activePromoAndMaxCoinId.getMaxCoinId(), notNullValue());

        promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(reusablePromoCode))
                        .build());

        assertThat(coinSearchService.getCoinsByUid(USER_ID, 10), hasSize(1));

        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                )
                .build();

        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order1)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(reusablePromoCode)
                        .build());

        activePromoAndMaxCoinId = coinSearchService.hasActiveOrUsedPromoAndMaxCoinId(
                USER_ID, reusablePromo.getId());

        assertThat(activePromoAndMaxCoinId.isActive(), is(false));
        assertThat(activePromoAndMaxCoinId.getMaxCoinId(), notNullValue());

        promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(reusablePromoCode))
                        .build());

        assertThat(coinSearchService.getCoinsByUid(USER_ID, 10), hasSize(2));
        assertThat(coinSearchService.getCoinsByUid(USER_ID, 10), containsInAnyOrder(
                allOf(
                        hasProperty("status", equalTo(CoreCoinStatus.USED))
                ),
                allOf(
                        hasProperty("status", equalTo(CoreCoinStatus.ACTIVE))
                )
        ));
    }

    @Test
    @Repeat(10)
    public void shouldNotCreateMoreOneInActiveConcurrently() throws InterruptedException {
        testConcurrency(() -> {
            promocodeService.activatePromocodes(
                    PromocodeActivationRequest.builder()
                            .userId(USER_ID)
                            .externalPromocodes(Set.of(reusablePromoCode))
                            .build());
            return () -> assertThat(coinSearchService.getCoinsByUid(USER_ID, 100), hasSize(1));
        });
    }

    private MultiCartWithBundlesDiscountResponse activatePromocodeAndSpend(String code) {
        clock.spendTime(1, ChronoUnit.MILLIS);

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(Set.of(code))
                .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(10000)
                ).build();

        Set<UserPromocodeInfo> promoCodeInfos = promocodeService.retrievePromocodeInfo(USER_ID, code);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(code)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(discountResponse.getCoins(), empty());
        assertThat(discountResponse.getUnusedPromocodes(), empty());
        assertThat(discountResponse.getPromocodeErrors(), empty());

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.MARKET_PROMOCODE)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )))
        )));

        var promoCodeInfo = promoCodeInfos
                .stream()
                .findFirst();

        assertThat(promoCodeInfo.isPresent(), is(true));

        Coin coin = coinService.search.getCoin(promoCodeInfo.get().getCoinKey()).get();

        assertThat(coin.getStatus(), is(CoreCoinStatus.USED));

        promoCodeInfos = promocodeService.retrievePromocodeInfo(USER_ID, code);

        promoCodeInfo = promoCodeInfos
                .stream()
                .findFirst();

        assertThat(promoCodeInfo.get().getCode(), is(code));
        assertThat(promoCodeInfo.get().getErrorCode(), nullValue());

        return discountResponse;
    }

}
