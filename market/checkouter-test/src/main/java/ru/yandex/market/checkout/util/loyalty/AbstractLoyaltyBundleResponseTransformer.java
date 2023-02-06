package ru.yandex.market.checkout.util.loyalty;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyUtils;
import ru.yandex.market.checkout.util.loyalty.model.CoinDiscountEntry;
import ru.yandex.market.checkout.util.loyalty.model.PromocodeDiscountEntry;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleResponse;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.loyalty.api.model.AbstractOrder;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.CouponError;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundleItem;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.cart.CartFlag;
import ru.yandex.market.loyalty.api.model.coin.CoinError;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryDiscountWithPromoType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryPromoResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryRequest;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeError;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeWarning;

import static com.google.common.collect.Multimaps.flatteningToMultimap;
import static com.google.common.collect.Multimaps.toMultimap;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount.PROMOCODE;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount.PROMOCODE_PROMO_KEY;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount.TEST_ITEM_SUBSIDY_VALUE;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyParameters.DeliveryDiscountsMode.FORCE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_APPLICABLE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.INVALID_REQUEST;

public abstract class AbstractLoyaltyBundleResponseTransformer extends ResponseDefinitionTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLoyaltyBundleResponseTransformer.class);
    private static final Map<String, Long> TOKEN_TO_COIN_ID = new HashMap<>();
    private final ObjectMapper objectMapper;

    protected AbstractLoyaltyBundleResponseTransformer(ObjectMapper marketLoyaltyObjectMapper) {
        this.objectMapper = marketLoyaltyObjectMapper;
    }

    private static List<LoyaltyDiscount> getDiscountsForDeliveryType(
            LoyaltyParameters loyaltyParameters,
            DeliveryType type
    ) {
        // default discounts for all types;
        List<LoyaltyDiscount> discounts = new ArrayList<>();
        discounts.addAll(loyaltyParameters.getDeliveryPromoResponse().getOrDefault(null, Collections.emptyList()));
        discounts.addAll(loyaltyParameters.getDeliveryPromoResponse().getOrDefault(type, Collections.emptyList()));

        return discounts;
    }

    private static boolean hasBundleId(@Nullable OrderBundleResponse bundle) {
        return bundle != null && bundle.getPromoType() == ru.yandex.market.loyalty.api.model.PromoType.GENERIC_BUNDLE;
    }

    @Nullable
    private static Boolean isPrimaryInBundle(@Nullable OrderBundleResponse bundleResponse,
                                             @Nonnull BundledOrderItemRequest itemRequest) {
        if (!hasBundleId(bundleResponse)) {
            return null;
        }

        return bundleResponse.getBundle().getItems().stream()
                .filter(item -> item.getFeedId().equals(itemRequest.getFeedId())
                        && item.getOfferId().equals(itemRequest.getOfferId()))
                .map(OrderBundleItem::isPrimaryInBundle)
                .findFirst().orElse(null);
    }

    private static BundledOrderItemResponse buildItemResponse(
            OrderItemResponseBuilder itemResponseBuilder,
            List<LoyaltyDiscount> loyaltyDiscounts,
            LoyaltyParameters loyaltyParameters,
            Set<Long> usedCoins
    ) {
        loyaltyDiscounts.stream()
                .peek(loyaltyDiscount -> addIgnoreNull(usedCoins, loyaltyDiscount.getCoinId()))
                .map(LoyaltyDiscount::toItemPromoResponse)
                .forEach(itemResponseBuilder::promo);

        loyaltyParameters.getExpectedPromoBundles().stream()
                .map(b -> b.promoFor(itemResponseBuilder.feedOfferId()))
                .filter(Objects::nonNull)
                .forEach(itemResponseBuilder::promo);

        return itemResponseBuilder.build();
    }

    @Override
    public ResponseDefinition transform(
            Request request, ResponseDefinition responseDefinition, FileSource files,
            Parameters parameters
    ) {
        MultiCartWithBundlesDiscountRequest discountRequest;
        try {
            discountRequest = getRequest(request, MultiCartWithBundlesDiscountRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LoyaltyParameters loyaltyParameters = (LoyaltyParameters) parameters.get("loyaltyParameters");
        if (loyaltyParameters == null) {
            return responseDefinition;
        }
        loyaltyParameters.setExpectedPromoCode(discountRequest.getCoupon());
        loyaltyParameters.setLastDiscountRequest(discountRequest);
        try {
            validateRequest(discountRequest, loyaltyParameters);
        } catch (ValidationException e) {
            return buildErrorResponse(responseDefinition, e.marketLoyaltyError);
        }

        MultiCartWithBundlesDiscountResponse response = createResponse(
                discountRequest, loyaltyParameters);

        try {
            return buildResponse(responseDefinition, response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected   <T> T getRequest(Request request, Class<T> objClass) throws IOException {
        return objectMapper.readValue(
                request.getBodyAsString(),
                objClass
        );
    }

    protected ResponseDefinition validateRequest(
            MultiCartWithBundlesDiscountRequest discountRequest,
            LoyaltyParameters loyaltyParameters
    ) {
        Set<String> promos = discountRequest.getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .map(BundledOrderItemRequest::getPromoKeys)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        if (!loyaltyParameters.getExpectedPromoBundles().stream()
                .map(b -> b.getBundle().getPromoKey())
                .allMatch(promos::contains)) {
            throw new ValidationException(
                    new MarketLoyaltyError(INVALID_REQUEST.name(),
                            "Items doesn't has promos " + promos, null
                    )
            );
        }

        if (!createRequestOrderItemsStream(discountRequest).findFirst().isPresent()
                && StringUtils.isNotBlank(discountRequest.getCoupon())) {
            throw new ValidationException(
                    new MarketLoyaltyError(COUPON_NOT_APPLICABLE.name(), "Order has no items", null)
            );
        }

        if (createRequestOrderItemsStream(discountRequest).anyMatch(it -> it.getHyperCategoryId() == null)) {
            throw new ValidationException(
                    new MarketLoyaltyError(INVALID_REQUEST.name(),
                            "Order item has empty HyperCategoryId: "
                                    + discountRequest, null
                    )
            );
        }

        if (discountRequest.getPlatform() == MarketPlatform.BLUE && createRequestOrderItemsStream(discountRequest)
                .anyMatch(it -> it.getSku() == null)) {
            throw new ValidationException(
                    new MarketLoyaltyError(INVALID_REQUEST.name(),
                            "Blue order item has empty sku: " + discountRequest, null
                    )
            );
        }

        discountRequest.getOrders().forEach(
                c -> {
                    if (emptyIfNull(c.getDeliveries()).stream()
                            .collect(Collectors.groupingBy(DeliveryRequest::getId, Collectors.counting()))
                            .entrySet().stream().anyMatch(p -> p.getValue() > 1)) {
                        throw new ValidationException(
                                new MarketLoyaltyError(
                                        INVALID_REQUEST.name(),
                                        "Duplicate delivery discount request ids " + c.getDeliveries(),
                                        null
                                )
                        );
                    }

                    if (emptyIfNull(c.getDeliveries()).stream()
                            .filter(DeliveryRequest::isSelected).count() > 1) {
                        throw new ValidationException(
                                new MarketLoyaltyError(
                                        INVALID_REQUEST.name(),
                                        "Several selected deliveryRequests " + c.getDeliveries(),
                                        null
                                )
                        );
                    }

                }
        );
        return null;
    }

    private Stream<BundledOrderItemRequest> createRequestOrderItemsStream(
            MultiCartWithBundlesDiscountRequest discountRequest
    ) {
        return discountRequest.getOrders().stream()
                .flatMap(o -> o.getItems().stream());
    }

    protected ResponseDefinition buildErrorResponse(
            ResponseDefinition responseDefinition,
            MarketLoyaltyError marketLoyaltyError
    ) {
        try {
            return new ResponseDefinitionBuilder()
                    .withStatus(responseDefinition.getStatus())
                    .withBody(objectMapper.writeValueAsString(marketLoyaltyError))
                    .withHeader("Content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .withStatus(HttpStatus.BAD_REQUEST.value())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private MultiCartWithBundlesDiscountResponse createResponse(
            MultiCartWithBundlesDiscountRequest discountRequest,
            LoyaltyParameters loyaltyParameters
    ) {
        Set<Long> usedCoins = new HashSet<>();

        Multimap<String, OrderBundleResponse> bundleByPromos = loyaltyParameters.getExpectedPromoBundles().stream()
                .collect(toMultimap(b -> b.getBundle().getPromoKey(), Function.identity(), HashMultimap::create));

        Multimap<String, OrderBundleResponse> bundleByOrder = discountRequest.getOrders().stream()
                .collect(flatteningToMultimap(OrderWithBundlesRequest::getCartId, o -> o.getItems().stream()
                        .flatMap(itemRequest -> itemRequest.getPromoKeys().stream()
                                .flatMap(promo -> bundleByPromos.get(promo).stream())), HashMultimap::create));

        List<OrderWithBundlesResponse> orders = discountRequest.getOrders().stream()
                .map(o -> buildOrderWithDeliveriesResponse(o, discountRequest, loyaltyParameters, usedCoins,
                        bundleByOrder.get(o.getCartId())
                ))
                .collect(Collectors.toList());

        CashbackResponse cashbackResponse;
        if (loyaltyParameters.getCalcsExpectedCashbackResponse() == null) {
            cashbackResponse = defaultCashback(loyaltyParameters.getSelectedCashbackOption());
        } else {
            cashbackResponse = loyaltyParameters.getCalcsExpectedCashbackResponse();
        }
        return new MultiCartWithBundlesDiscountResponse(
                orders,
                buildCoins(loyaltyParameters, usedCoins),
                discountRequest.getCoins().stream()
                        .filter(requestedCoin -> !usedCoins.contains(requestedCoin.getId()))
                        .collect(Collectors.toList()),
                buildCoinErrors(loyaltyParameters),
                buildOldCouponError(loyaltyParameters.getPromocodeDiscountEntries()),
                buildPromocodeErrors(loyaltyParameters.getPromocodeDiscountEntries()),
                buildPromocodeWarnings(loyaltyParameters.getPromocodeDiscountEntries()),
                buildUnusedPromocodes(loyaltyParameters.getPromocodeDiscountEntries()),
                loyaltyParameters.getPriceLeftForFreeDelivery(),
                loyaltyParameters.getFreeDeliveryThreshold(),
                avoidNull(loyaltyParameters.getFreeDeliveryReason(), FreeDeliveryReason.UNKNOWN),
                avoidNull(loyaltyParameters.getFreeDeliveryStatus(), FreeDeliveryStatus.UNKNOWN),
                loyaltyParameters.getDeliveryDiscountMap(),
                cashbackResponse,
                avoidNull(loyaltyParameters.getYandexPlusSale(), CartFlag.UNKNOWN)
        );
    }

    @Nonnull
    private List<CoinError> buildCoinErrors(@Nonnull LoyaltyParameters loyaltyParameters) {
        return loyaltyParameters.getCoinDiscountEntries().stream()
                .filter(CoinDiscountEntry::hasError)
                .map(CoinDiscountEntry::toCoinError)
                .collect(Collectors.toUnmodifiableList());
    }

    @Nonnull
    private List<UserCoinResponse> buildCoins(@Nonnull LoyaltyParameters loyaltyParameters,
                                              @Nonnull Set<Long> usedCoins) {
        var discountCoins = loyaltyParameters.getCoinDiscountEntries().stream()
                .map(CoinDiscountEntry::toCoinResponse)
                .collect(Collectors.toUnmodifiableList());
        var existedCoins = discountCoins.stream()
                .map(UserCoinResponse::getId)
                .collect(Collectors.toSet());
        return Stream.concat(discountCoins.stream(), loyaltyParameters.getDeliveryPromoResponse().values().stream()
                .flatMap(List::stream)
                .map(LoyaltyDiscount::getCoinId)
                .filter(Objects::nonNull)
                .filter(id -> !existedCoins.contains(id))
                .map(id -> new UserCoinResponse(id, null, null, CoinType.FREE_DELIVERY, null, null, null, new Date(),
                        new Date(), null, null, null, CoinStatus.ACTIVE, true, null, null, null, null, false, null,
                        null, false, null, null)))
                .collect(Collectors.toList());
    }

    @Nonnull
    private Set<String> buildUnusedPromocodes(List<PromocodeDiscountEntry> promocodeDiscountEntries) {
        return promocodeDiscountEntries.stream()
                .filter(PromocodeDiscountEntry::isUnused)
                .map(PromocodeDiscountEntry::getPromocode)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    private Set<PromocodeError> buildPromocodeErrors(List<PromocodeDiscountEntry> promocodeDiscountEntries) {
        return promocodeDiscountEntries.stream()
                .filter(pe -> pe.getPromocodeError() != null)
                .map(PromocodeDiscountEntry::getPromocodeError)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    private Set<PromocodeWarning> buildPromocodeWarnings(List<PromocodeDiscountEntry> promocodeDiscountEntries) {
        return promocodeDiscountEntries.stream()
                .filter(pe -> pe.getPromocodeWarning() != null)
                .map(PromocodeDiscountEntry::getPromocodeWarning)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    private CouponError buildOldCouponError(List<PromocodeDiscountEntry> promocodeDiscountEntries) {
        return promocodeDiscountEntries.stream()
                .filter(pe -> pe.getPromocodeError() != null)
                .filter(pe -> pe.getPromoType() == ru.yandex.market.loyalty.api.model.PromoType.MARKET_COUPON)
                .map(PromocodeDiscountEntry::getPromocodeError)
                .map(PromocodeError::getError)
                .findFirst().orElse(null);
    }

    private OrderWithBundlesResponse buildOrderWithDeliveriesResponse(
            OrderWithBundlesRequest orderRequest,
            MultiCartWithBundlesDiscountRequest discountRequest,
            LoyaltyParameters loyaltyParameters,
            Set<Long> usedCoins,
            Collection<OrderBundleResponse> bundles
    ) {
        CashbackResponse cashbackResponse;
        if (loyaltyParameters.getCalcsExpectedCashbackResponse() == null) {
            cashbackResponse = defaultCashback(loyaltyParameters.getSelectedCashbackOption());
        } else {
            cashbackResponse = loyaltyParameters.getCalcsExpectedCashbackResponse();
        }
        return new OrderWithBundlesResponse(
                orderRequest.getCartId(),
                orderRequest.getOrderId(),
                buildItemResponses(orderRequest, discountRequest.getCertificateToken(), loyaltyParameters, bundles,
                        usedCoins
                ),
                buildDeliveryResponses(
                        loyaltyParameters,
                        usedCoins,
                        discountRequest.getCertificateToken(),
                        emptyIfNull(orderRequest.getDeliveries())
                ),
                bundles.stream()
                        .map(OrderBundleResponse::getBundle)
                        .collect(Collectors.toList()),
                loyaltyParameters.getExpectedDestroyedPromoBundles(),
                loyaltyParameters.getExpectedItemDiscountFault(),
                cashbackResponse
        );
    }

    private List<DeliveryResponse> buildDeliveryResponses(
            LoyaltyParameters loyaltyParameters,
            Set<Long> usedCoins,
            String certificateToken,
            @Nonnull Collection<DeliveryRequest> deliveryRequests
    ) {
        List<DeliveryResponse> deliveryResponses;
        if (certificateToken != null) {
            LOG.debug("Certificate is not null {}", certificateToken);
            Long coinId = TOKEN_TO_COIN_ID.computeIfAbsent(
                    certificateToken,
                    key -> RandomUtils.nextLong()
            );
            deliveryResponses = deliveryRequests.stream()
                    .map(deliveryRequest -> {
                        Map<DeliveryType, Map<PaymentType, DeliveryDiscountWithPromoType>> deliveryDiscountGrid =
                                loyaltyParameters.getDeliveryDiscountGrid();
                        DeliveryPromoResponse deliveryPromoResponse = new DeliveryPromoResponse(
                                deliveryRequest.getPrice(),
                                LoyaltyUtils.PromoTypeConverter.toLoyaltyPromoType(PromoType.MARKET_COIN),
                                "loyaltyTokenForPromo_external_certificate_token",
                                "external_certificate_promo_key",
                                new IdObject(coinId),
                                deliveryDiscountGrid == null ? null : deliveryDiscountGrid.get(
                                        deliveryRequest.getType())
                        );
                        return new DeliveryResponse(deliveryRequest.getId(), singletonList(deliveryPromoResponse));
                    })
                    .collect(Collectors.toList());
        } else {
            LOG.debug("Certificate is null");
            boolean hasSelected = deliveryRequests.stream().anyMatch(DeliveryRequest::isSelected);
            LOG.debug("Has selected: {}", hasSelected);
            LOG.debug("Delivery Requests: {}", deliveryRequests);
            deliveryResponses = deliveryRequests.stream()
                    .map(deliveryRequest -> {
                        if (loyaltyParameters.getPromoOnlySelectedOption() && !deliveryRequest.isSelected()) {
                            return null;
                        }
                        List<LoyaltyDiscount> discounts = getDiscountsForDeliveryType(
                                loyaltyParameters,
                                deliveryRequest.getType()
                        );
                        LOG.debug("Discounts for delivery type {}: {}", deliveryRequest.getType(), discounts);

                        Set<Long> coinsUsedByDelivery = new HashSet<>();
                        List<DeliveryPromoResponse> promos = buildDeliveryPromos(
                                discounts,
                                coinsUsedByDelivery,
                                deliveryRequest.getPrice(),
                                loyaltyParameters.getDeliveryDiscountsMode()
                        );
                        LOG.debug("Delivery promos: {}", promos);
                        if (promos == null) {
                            return null;
                        }

                        //если была выбрана опция доставки, то монету считаем примененной, если она применилась к
                        // выбранной опции.
                        // если опции не было выбрано, то если применилась хотя бы к одной опции
                        if (!hasSelected || deliveryRequest.isSelected()) {
                            usedCoins.addAll(coinsUsedByDelivery);
                        }

                        return new DeliveryResponse(deliveryRequest.getId(), promos);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        LOG.debug("Delivery responses: " + deliveryResponses);
        return deliveryResponses;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private List<DeliveryPromoResponse> buildDeliveryPromos(
            List<LoyaltyDiscount> discounts,
            Set<Long> usedCoins, BigDecimal deliveryPrice,
            LoyaltyParameters.DeliveryDiscountsMode mode
    ) {
        if (CollectionUtils.isEmpty(discounts)) {
            return null;
        }

        if (deliveryPrice.equals(BigDecimal.ZERO)) {
            return null;
        }


        //проверять, что сумма скидок больше цены доставки не нужно. все что есть добавляем.
        if (mode == FORCE) {
            return discounts.stream()
                    .peek(discount -> usedCoins.add(discount.getCoinId()))
                    .map(LoyaltyDiscount::toDeliveryPromoResponse).collect(Collectors.toList());
        }

        List<DeliveryPromoResponse> deliveryPromos = new ArrayList<>();

        BigDecimal currentTotalDiscount = BigDecimal.ZERO;
        for (LoyaltyDiscount discount : discounts) {
            // после добавления текущей скидки сумма будет больше либо равна стоимости, берем часть суммы и
            // прекращаем добавлять
            if (currentTotalDiscount.add(discount.getDiscount()).compareTo(deliveryPrice) >= 0) {
                // mode == ADJUST
                deliveryPromos.add(discount.toDeliveryPromoResponse(deliveryPrice.subtract(currentTotalDiscount)));
                usedCoins.add(discount.getCoinId());
                break;
            }


            currentTotalDiscount = currentTotalDiscount.add(discount.getDiscount());

            //добавляем на всю сумму
            deliveryPromos.add(discount.toDeliveryPromoResponse());
            usedCoins.add(discount.getCoinId());
        }

        return deliveryPromos;
    }

    private List<BundledOrderItemResponse> buildItemResponses(
            AbstractOrder<BundledOrderItemRequest> orderRequest,
            String certificateToken,
            LoyaltyParameters loyaltyParameters,
            Collection<OrderBundleResponse> bundles,
            Set<Long> usedCoins
    ) {
        CashbackResponse cashbackResponse;
        if (loyaltyParameters.getCalcsExpectedCashbackResponse() == null) {
            cashbackResponse = defaultCashback(loyaltyParameters.getSelectedCashbackOption());
        } else {
            cashbackResponse = loyaltyParameters.getCalcsExpectedCashbackResponse();
        }

        if (certificateToken != null) {
            // сертификат нельзя применять одновременно с купонами, монетками и комплектами
            BundledOrderItemRequest singleItem = Iterables.getOnlyElement(orderRequest.getItems());
            Long coinId = TOKEN_TO_COIN_ID.computeIfAbsent(
                    certificateToken,
                    key -> RandomUtils.nextLong()
            );
            BundledOrderItemResponse response = new BundledOrderItemResponse(
                    singleItem.getOfferId(),
                    singleItem.getFeedId(),
                    singleItem.getPrice(),
                    singleItem.getQuantity(),
                    singleItem.isDownloadable(),
                    singletonList(
                            new ItemPromoResponse(
                                    singleItem.getPrice(),
                                    LoyaltyUtils.PromoTypeConverter.toLoyaltyPromoType(PromoType.MARKET_COIN),
                                    "loyaltyTokenForPromo_external_certificate_token",
                                    "external_certificate_promo_key",
                                    null,
                                    null,
                                    null,
                                    new IdObject(coinId),
                                    null,
                                    null,
                                    null,
                                    null
                            )
                    ),
                    null,
                    null,
                    cashbackResponse
            );
            return singletonList(response);
        }

        Multimap<OfferItemKey, LoyaltyDiscount> loyaltyDiscountsByItemId = collectPromocodeDiscounts(loyaltyParameters);
        loyaltyDiscountsByItemId.putAll(collectCoinDiscounts(loyaltyParameters));
        for (Map.Entry<OfferItemKey, List<LoyaltyDiscount>> entry :
                loyaltyParameters.getLoyaltyDiscountsByOfferId().entrySet()) {
            loyaltyDiscountsByItemId.putAll(entry.getKey(), entry.getValue());
        }

        boolean needToAddCoupon = needToAddCouponPromo(loyaltyParameters, loyaltyDiscountsByItemId.asMap());

        List<OrderItemResponseBuilder> responseBuilders;

        if (!loyaltyParameters.getExpectedResponseItems().isEmpty()) {
            responseBuilders = loyaltyParameters.getExpectedResponseItems();
        } else {
            final Map<String, OrderBundleResponse> bundleByPromo = bundles.stream()
                    .collect(Collectors.toMap(b -> b.getBundle().getPromoKey(), Function.identity()));
            responseBuilders = orderRequest.getItems().stream()
                    .map(itemRequest -> buildItemResponseBuilder(itemRequest, bundleByPromo))
                    .map(itemBuilder -> itemBuilder.cashback(cashbackResponse))
                    .collect(Collectors.toList());
        }

        return responseBuilders.stream()
                .map(responseBuilder -> buildItemResponse(
                        responseBuilder,
                        adjustCouponPromo(loyaltyDiscountsByItemId.get(responseBuilder.offerItemKey()),
                                needToAddCoupon),
                        loyaltyParameters,
                        usedCoins
                ))
                .collect(Collectors.toList());
    }

    @Nonnull
    private Multimap<OfferItemKey, LoyaltyDiscount> collectCoinDiscounts(@Nonnull LoyaltyParameters loyaltyParameters) {
        return loyaltyParameters.getCoinDiscountEntries().stream()
                .filter(cd -> !cd.isUnused()
                        && !cd.hasError()
                        && cd.getPromoType() == ru.yandex.market.loyalty.api.model.PromoType.SMART_SHOPPING
                ).flatMap(cd -> cd.getItemDiscounts().entrySet().stream()
                        .map(entry -> Map.entry(entry.getKey(), LoyaltyDiscount.builder()
                                .coinId(cd.getCoinId())
                                .promoType(PromoType.MARKET_COIN)
                                .promoKey(cd.getPromoKey())
                                .shopPromoId(cd.getShopPromoId())
                                .anaplanId(cd.getAnaplanId())
                                .discount(entry.getValue())
                                .build()))
                ).collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));
    }

    @Nonnull
    private Multimap<OfferItemKey, LoyaltyDiscount> collectPromocodeDiscounts(
            @Nonnull LoyaltyParameters loyaltyParameters) {
        return loyaltyParameters.getPromocodeDiscountEntries().stream()
                .filter(pe -> !pe.isUnused()
                        && pe.getPromocodeError() == null
                        && pe.getPromoType() == ru.yandex.market.loyalty.api.model.PromoType.MARKET_PROMOCODE
                        && (pe.getActivationResultCode() == PromocodeActivationResultCode.ALREADY_ACTIVE
                        || pe.getActivationResultCode() == PromocodeActivationResultCode.SUCCESS)
                ).flatMap(pe -> pe.getItemDiscounts().entrySet().stream()
                        .map(entry -> Map.entry(entry.getKey(), LoyaltyDiscount.builder()
                                .promoType(PromoType.MARKET_PROMOCODE)
                                .promoKey(pe.getPromoKey())
                                .shopPromoId(pe.getShopPromoId())
                                .anaplanId(pe.getAnaplanId())
                                .clientId(pe.getClientId())
                                .discount(entry.getValue())
                                .promocode(pe.getPromocode())
                                .build()))
                ).collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));
    }

    private boolean needToAddCouponPromo(
            LoyaltyParameters loyaltyParameters,
            Map<OfferItemKey, Collection<LoyaltyDiscount>> loyaltyDiscountsByItemId
    ) {
        return loyaltyParameters.getPromocodeDiscountEntries().isEmpty()
                && StringUtils.isNotEmpty(loyaltyParameters.getExpectedPromoCode())
                && loyaltyDiscountsByItemId.values().stream().flatMap(Collection::stream)
                .noneMatch(d -> PromoType.MARKET_COUPON == d.getPromoType());
    }

    private List<LoyaltyDiscount> adjustCouponPromo(Collection<LoyaltyDiscount> discounts, boolean needToAddCoupon) {
        if (needToAddCoupon) {
            return Stream.concat(discounts.stream(), Stream.of(defaultCouponPromo()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(discounts);
    }

    private LoyaltyDiscount defaultCouponPromo() {
        return LoyaltyDiscount.builder()
                .promoKey(PROMOCODE_PROMO_KEY)
                .discount(TEST_ITEM_SUBSIDY_VALUE)
                .promocode(PROMOCODE)
                .promoType(PromoType.MARKET_COUPON)
                .build();
    }

    protected ResponseDefinition buildResponse(ResponseDefinition responseDefinition,
                                            Object response) throws JsonProcessingException {
        return new ResponseDefinitionBuilder()
                .withStatus(responseDefinition.getStatus())
                .withBody(objectMapper.writeValueAsString(response))
                .withHeader("Content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .build();
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Не меняй чиселки и буковки в этом методе, а то чекаутер не сможет зарелизиться!
     *
     * @return дефолтный кешбэчный ответ от лоялти
     */
    protected CashbackResponse defaultCashback(@Nullable CashbackType selectedCashbackOption) {
        return new CashbackResponse(
                new CashbackOptions("promoKey", null, BigDecimal.valueOf(100L),
                        CashbackPermision.ALLOWED, null, null, null, null, null),
                new CashbackOptions("promoKey", null, BigDecimal.valueOf(30L),
                        CashbackPermision.ALLOWED, null, null, null, null, null),
                selectedCashbackOption);
    }

    protected OrderItemResponseBuilder buildItemResponseBuilder(
            BundledOrderItemRequest item,
            Map<String, OrderBundleResponse> bundlesByPromoKey
    ) {
        final OrderBundleResponse bundle = Optional.ofNullable(item.getPromoKeys())
                .filter(CollectionUtils::isNotEmpty)
                .map(keys -> bundlesByPromoKey.get(keys.iterator().next()))
                .orElseGet(() -> bundlesByPromoKey.values().stream()
                        .filter(b -> b.getBundle().getItems().stream()
                                .anyMatch(i -> item.getFeedId().equals(i.getFeedId())
                                        && item.getOfferId().equals(i.getOfferId())))
                        .findFirst().orElse(null));

        final boolean hasBundleId = hasBundleId(bundle);

        return OrderItemResponseBuilder.create()
                .offer(
                        item.getFeedId(), item.getOfferId(),
                        hasBundleId ? bundle.getBundle().getBundleId() : null
                )
                .primaryInBundle(isPrimaryInBundle(bundle, item))
                .price(item.getPrice())
                .quantity(item.getQuantity());
    }

    protected static class ValidationException extends IllegalArgumentException {

        private final MarketLoyaltyError marketLoyaltyError;

        public ValidationException(MarketLoyaltyError marketLoyaltyError) {
            this.marketLoyaltyError = marketLoyaltyError;
        }

    }
}
