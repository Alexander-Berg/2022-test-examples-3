package ru.yandex.market.checkout.util.loyalty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.http.Fault;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.util.loyalty.model.CoinDiscountEntry;
import ru.yandex.market.checkout.util.loyalty.model.PromocodeDiscountEntry;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleResponse;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.loyalty.api.model.CashbackOptionsResponse;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundleDestroyed;
import ru.yandex.market.loyalty.api.model.cart.CartFlag;
import ru.yandex.market.loyalty.api.model.coin.CoinError;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryDiscountWithPromoType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.loyalty.api.model.discount.ExternalItemDiscountFault;
import ru.yandex.market.loyalty.api.model.discount.ExternalItemDiscountFault.ExternalItemDiscountFaultBuilder;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;

import static ru.yandex.market.checkout.util.loyalty.LoyaltyParameters.DeliveryDiscountsMode.ADJUST;

/**
 * @author sergeykoles
 * Created on: 04.05.18
 */
public class LoyaltyParameters {

    private final List<PromocodeDiscountEntry> promocodeDiscountEntries = new ArrayList<>();
    private final List<CoinDiscountEntry> coinDiscountEntries = new ArrayList<>();

    /**
     * Признак, работает ли бесплатная доставка при наличии Yandex+.
     * Если <code>null</code>, то считается, что у пользователя нет Yandex+.
     */
    private Boolean yandexPlusFreeDelivery;
    /**
     * Признак, работает ли бесплатная доставка при наличии Prime.
     * Если <code>null</code>, то считается, что у пользоватебя нет Prime.
     */
    private Boolean primeFreeDelivery;

    /**
     * Идентификатор заказа, которым был куплен прайм.
     */
    private String primeOrderId;

    private Fault perkFailure;

    /**
     * промокод, который ожидается в запросе от лоялти. будет мокаться запрос к лоялти, в котором указан именно это
     * промокод
     */
    private String expectedPromoCode;

    private MultiCartWithBundlesDiscountRequest lastDiscountRequest;

    private final Map<OfferItemKey, List<LoyaltyDiscount>> loyaltyDiscountsByOfferId = new HashMap<>();

    //ключ null - это скидки на всех
    private final Map<DeliveryType, List<LoyaltyDiscount>> deliveryPromoResponse = new HashMap<>();

    /**
     * признак того, как нужно добавлять скидки в лоялти на доставку
     */
    private DeliveryDiscountsMode deliveryDiscountsMode = ADJUST;

    private final List<UserCoinResponse> allCoins = new ArrayList<>();

    private List<OrderItemResponseBuilder> expectedResponseItems = new ArrayList<>();
    private final Map<String, OrderBundleResponse> expectedPromoBundles = new HashMap<>();
    private final Map<String, OrderBundleDestroyed> expectedDestroyedPromoBundles = new HashMap<>();
    private final Set<ExternalItemDiscountFault> externalItemDiscountFaults = new LinkedHashSet<>();

    private final List<CoinError> coinErrors = new ArrayList<>();

    private BigDecimal priceLeftForFreeDelivery;

    private BigDecimal freeDeliveryThreshold;

    private FreeDeliveryReason freeDeliveryReason;

    private FreeDeliveryStatus freeDeliveryStatus;

    private Map<FreeDeliveryReason, PriceLeftForFreeDeliveryResponseV3> deliveryDiscountMap = new HashMap<>();

    private Map<ru.yandex.market.loyalty.api.model.delivery.DeliveryType,
            Map<ru.yandex.market.loyalty.api.model.PaymentType, DeliveryDiscountWithPromoType>> deliveryDiscountGrid;

    /**
     * Если требуется касомная заглушка для лоялти - её надо вставить сюда
     */
    private Consumer<LoyaltyConfigurer> customLoyaltyMockConfiguration = null;

    private CashbackOptionsResponse expectedCashbackOptionsResponse =
            new CashbackOptionsResponse(Collections.emptyList());

    private CashbackResponse calcsExpectedCashbackResponse;

    private CartFlag yandexPlusSale;

    /**
     * Опция списания/начисления от лоялти
     */
    private CashbackType selectedCashbackOption;

    private Boolean isPromoOnlySelectedOption = false;

    public Fault getPerkFailure() {
        return perkFailure;
    }

    public void setPerkFailure(Fault perkFailure) {
        this.perkFailure = perkFailure;
    }

    public Boolean getYandexPlusFreeDelivery() {
        return yandexPlusFreeDelivery;
    }

    public void setYandexPlusFreeDelivery(Boolean yandexPlusFreeDelivery) {
        this.yandexPlusFreeDelivery = yandexPlusFreeDelivery;
    }

    public void setSelectedCashbackOption(CashbackType cashbackType) {
        selectedCashbackOption = cashbackType;
    }

    public CashbackType getSelectedCashbackOption() {
        return selectedCashbackOption;
    }

    public Boolean getPrimeFreeDelivery() {
        return primeFreeDelivery;
    }

    public String getPrimeOrderId() {
        return primeOrderId;
    }

    public void setPrimeFreeDelivery(Boolean primeFreeDelivery, String primeOrderId) {
        this.primeFreeDelivery = primeFreeDelivery;
        this.primeOrderId = primeOrderId;
    }

    public String getExpectedPromoCode() {
        return expectedPromoCode;
    }

    public void setExpectedPromoCode(String expectedPromoCode) {
        this.expectedPromoCode = expectedPromoCode;
    }

    public Map<OfferItemKey, List<LoyaltyDiscount>> getLoyaltyDiscountsByOfferId() {
        return loyaltyDiscountsByOfferId;
    }

    @Deprecated
    @Nonnull
    public LoyaltyParameters addLoyaltyDiscount(
            @Nonnull OrderItem item,
            @Nonnull PromoType promoType,
            @Nonnull BigDecimal discount
    ) {
        loyaltyDiscountsByOfferId.computeIfAbsent(item.getOfferItemKey(), id -> new ArrayList<>())
                .add(LoyaltyDiscount.builder()
                        .promoType(promoType)
                        .discount(discount)
                        .build());
        return this;
    }

    @Nonnull
    public LoyaltyParameters addLoyaltyDiscount(
            @Nonnull OrderItem item,
            @Nonnull LoyaltyDiscount loyaltyDiscount
    ) {
        loyaltyDiscountsByOfferId.computeIfAbsent(item.getOfferItemKey(), id -> new ArrayList<>())
                .add(loyaltyDiscount);
        return this;
    }

    public void clearDiscounts() {
        loyaltyDiscountsByOfferId.clear();
        deliveryPromoResponse.clear();
    }

    public DeliveryDiscountsMode getDeliveryDiscountsMode() {
        return deliveryDiscountsMode;
    }

    public void setDeliveryDiscountsMode(DeliveryDiscountsMode deliveryDiscountsMode) {
        this.deliveryDiscountsMode = deliveryDiscountsMode;
    }

    public Map<DeliveryType, List<LoyaltyDiscount>> getDeliveryPromoResponse() {
        return deliveryPromoResponse;
    }

    public LoyaltyParameters addDeliveryDiscount(LoyaltyDiscount loyaltyDiscount) {
        return addDeliveryDiscount(null, loyaltyDiscount);
    }

    public LoyaltyParameters addDeliveryDiscount(DeliveryType loyaltyDeliveryType, LoyaltyDiscount loyaltyDiscount) {
        deliveryPromoResponse.computeIfAbsent(loyaltyDeliveryType, id -> new ArrayList<>())
                .add(loyaltyDiscount);
        return this;
    }

    public Consumer<LoyaltyConfigurer> getCustomLoyaltyMockConfiguration() {
        return customLoyaltyMockConfiguration;
    }

    public void setCustomLoyaltyMockConfiguration(Consumer<LoyaltyConfigurer> customLoyaltyMockConfiguration) {
        this.customLoyaltyMockConfiguration = customLoyaltyMockConfiguration;
    }

    public CashbackOptionsResponse getExpectedCashbackOptionsResponse() {
        return expectedCashbackOptionsResponse;
    }

    public void setExpectedCashbackOptionsResponse(CashbackOptionsResponse expectedCashbackOptionsResponse) {
        this.expectedCashbackOptionsResponse = expectedCashbackOptionsResponse;
    }

    public CashbackResponse getCalcsExpectedCashbackResponse() {
        return calcsExpectedCashbackResponse;
    }

    public LoyaltyParameters setCalcsExpectedCashbackResponse(CashbackResponse calcsExpectedCashbackResponse) {
        this.calcsExpectedCashbackResponse = calcsExpectedCashbackResponse;
        return this;
    }

    public BigDecimal getFreeDeliveryThreshold() {
        return freeDeliveryThreshold;
    }

    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) {
        this.freeDeliveryThreshold = freeDeliveryThreshold;
    }

    public BigDecimal getPriceLeftForFreeDelivery() {
        return priceLeftForFreeDelivery;
    }

    public void setPriceLeftForFreeDelivery(BigDecimal priceLeftForFreeDelivery) {
        this.priceLeftForFreeDelivery = priceLeftForFreeDelivery;
    }

    public FreeDeliveryReason getFreeDeliveryReason() {
        return freeDeliveryReason;
    }

    public void setFreeDeliveryReason(FreeDeliveryReason freeDeliveryReason) {
        this.freeDeliveryReason = freeDeliveryReason;
    }

    public FreeDeliveryStatus getFreeDeliveryStatus() {
        return freeDeliveryStatus;
    }

    public void setFreeDeliveryStatus(FreeDeliveryStatus freeDeliveryStatus) {
        this.freeDeliveryStatus = freeDeliveryStatus;
    }

    public Map<FreeDeliveryReason, PriceLeftForFreeDeliveryResponseV3> getDeliveryDiscountMap() {
        return deliveryDiscountMap;
    }

    public void setDeliveryDiscountMap(
            Map<FreeDeliveryReason, PriceLeftForFreeDeliveryResponseV3> deliveryDiscountMap) {
        this.deliveryDiscountMap = deliveryDiscountMap;
    }

    public void addDeliveryDiscount(FreeDeliveryReason reason, PriceLeftForFreeDeliveryResponseV3 priceLeft) {
        this.deliveryDiscountMap.put(reason, priceLeft);
    }

    public LoyaltyParameters expectPromoBundle(OrderBundleBuilder bundleBuilder) {
        OrderBundleResponse br = bundleBuilder.build();
        expectedPromoBundles.put(br.getBundle().getBundleId(), br);
        return this;
    }

    public LoyaltyParameters expectResponseItems(OrderItemResponseBuilder... itemResponseBuilders) {
        expectedResponseItems = List.of(itemResponseBuilders);
        return this;
    }

    public LoyaltyParameters expectResponseItem(OrderItemResponseBuilder itemResponseBuilder) {
        expectedResponseItems.add(itemResponseBuilder);
        return this;
    }

    public List<OrderBundleResponse> getExpectedPromoBundles() {
        return expectedPromoBundles.values().stream().collect(Collectors.toUnmodifiableList());
    }

    public List<OrderItemResponseBuilder> getExpectedResponseItems() {
        return Collections.unmodifiableList(expectedResponseItems);
    }

    public LoyaltyParameters expectDestroyedPromoBundle(OrderBundleBuilder bundleDestroyed) {
        OrderBundleDestroyed bd = bundleDestroyed.buildDestroyed();
        expectedDestroyedPromoBundles.put(bd.getBundleId(), bd);
        return this;
    }

    public LoyaltyParameters expectItemDiscountFault(
            ExternalItemDiscountFaultBuilder itemDiscountFaultBuilder
    ) {
        externalItemDiscountFaults.add(itemDiscountFaultBuilder.build());
        return this;
    }

    public LoyaltyParameters expectPromocode(@Nonnull PromocodeDiscountEntry.Builder promocodeBuilder) {
        promocodeDiscountEntries.add(promocodeBuilder.build());
        return this;
    }

    public LoyaltyParameters expectCoin(@Nonnull CoinDiscountEntry.Builder coinBuilder) {
        coinDiscountEntries.add(coinBuilder.build());
        return this;
    }

    public LoyaltyParameters expectPromocodes(@Nonnull PromocodeDiscountEntry.Builder... promocodeBuilders) {
        for (PromocodeDiscountEntry.Builder promocodeBuilder : promocodeBuilders) {
            promocodeDiscountEntries.add(promocodeBuilder.build());
        }
        return this;
    }

    @Nonnull
    public List<PromocodeDiscountEntry> getPromocodeDiscountEntries() {
        return promocodeDiscountEntries;
    }

    @Nonnull
    public List<CoinDiscountEntry> getCoinDiscountEntries() {
        return coinDiscountEntries;
    }

    public List<OrderBundleDestroyed> getExpectedDestroyedPromoBundles() {
        return expectedDestroyedPromoBundles.values().stream().collect(Collectors.toUnmodifiableList());
    }

    @Nonnull
    public List<ExternalItemDiscountFault> getExpectedItemDiscountFault() {
        return externalItemDiscountFaults.stream().collect(Collectors.toUnmodifiableList());
    }

    public Map<DeliveryType, Map<PaymentType, DeliveryDiscountWithPromoType>> getDeliveryDiscountGrid() {
        return deliveryDiscountGrid;
    }

    public void setDeliveryDiscountGrid(
            Map<DeliveryType, Map<PaymentType, DeliveryDiscountWithPromoType>> deliveryDiscountGrid
    ) {
        this.deliveryDiscountGrid = deliveryDiscountGrid;
    }

    public MultiCartWithBundlesDiscountRequest getLastDiscountRequest() {
        return lastDiscountRequest;
    }

    public void setLastDiscountRequest(MultiCartWithBundlesDiscountRequest lastDiscountRequest) {
        this.lastDiscountRequest = lastDiscountRequest;
    }

    public CartFlag getYandexPlusSale() {
        return yandexPlusSale;
    }

    public void setYandexPlusSale(CartFlag yandexPlusSale) {
        this.yandexPlusSale = yandexPlusSale;
    }

    public Boolean getPromoOnlySelectedOption() {
        return isPromoOnlySelectedOption;
    }

    public void setPromoOnlySelectedOption(Boolean promoOnlySelectedOption) {
        isPromoOnlySelectedOption = promoOnlySelectedOption;
    }

    public enum DeliveryDiscountsMode {
        // добавляет все скидки, какие были в тесте (для проверки на некорректный расчет скидки)
        FORCE,
        // если вдруг сумма скидки была больше доставки, берет наименьшую из сумм доставки и суммы скидки
        ADJUST
    }
}
