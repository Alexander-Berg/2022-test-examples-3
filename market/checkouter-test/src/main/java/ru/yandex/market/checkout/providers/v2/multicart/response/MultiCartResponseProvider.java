package ru.yandex.market.checkout.providers.v2.multicart.response;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.BnplPlanDetails;
import ru.yandex.market.checkout.checkouter.cart.BnplRegularPayment;
import ru.yandex.market.checkout.checkouter.cart.BnplVisualProperties;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.CostLimitInformation;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfile;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.ItemCashback;
import ru.yandex.market.checkout.checkouter.cashback.model.OrderCashback;
import ru.yandex.market.checkout.checkouter.credit.CreditError;
import ru.yandex.market.checkout.checkouter.credit.CreditInformation;
import ru.yandex.market.checkout.checkouter.credit.CreditOption;
import ru.yandex.market.checkout.checkouter.credit.InvalidCreditOrderItem;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsInfo;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsOption;
import ru.yandex.market.checkout.checkouter.installments.MonthlyPayment;
import ru.yandex.market.checkout.checkouter.order.AdditionalCartInfo;
import ru.yandex.market.checkout.checkouter.order.CoinError;
import ru.yandex.market.checkout.checkouter.order.CoinInfo;
import ru.yandex.market.checkout.checkouter.order.HelpingHand;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.MultiCartPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.BnplInfoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.BnplPlanDetailsResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.BnplRegularPaymentResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.BnplVisualPropertiesResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartCashbackResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartFailureResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartItemResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CashbackOptionsPromoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CashbackOptionsResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CashbackProfileResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CashbackResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.ChangeResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CoinErrorResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CoinInfoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CostLimitInformationResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CreditErrorResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CreditInformationResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CreditOptionResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.DimensionsResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.HelpingHandResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.InstallmentsOptionResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.InstallmentsResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.InvalidCreditCartItemResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.ItemCashbackResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.ItemPromoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.ItemServiceResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartPromoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartTotalsResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.OrderPromoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.PriceLeftForFreeDeliveryResponse;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public final class MultiCartResponseProvider {

    @Nonnull
    public MultiCart toMultiCart(@Nonnull MultiCartResponse multiCartResponse, Parameters parameters) {
        MultiCart multiCart = new MultiCart();
        multiCart.setCarts(
                multiCartResponse.getCarts().stream()
                        .map(cart -> toOrder(cart, parameters))
                        .collect(Collectors.toUnmodifiableList()));
        multiCart.setCartFailures(
                multiCartResponse.getCartFailures().stream()
                        .map(cartResponse -> toOrderFailure(cartResponse, parameters))
                        .collect(Collectors.toUnmodifiableList()));
        multiCart.setCoinInfo(toCoinInfo(multiCartResponse.getCoinInfo()));
        multiCart.setCreditInformation(toCreditInformation(multiCartResponse.getCreditInformation()));
        multiCart.setHelpingHand(toHelpingHand(multiCartResponse.getHelpingHand()));
        multiCart.setCostLimitInformation(toCostLimitInformation(multiCartResponse.getCostLimitInformation()));
        multiCart.setDeliveryDiscountMap(toDeliveryDiscountMap(multiCartResponse.getDeliveryDiscountMap()));
        multiCart.setValidationErrors(nullIfEmpty(multiCartResponse.getValidationErrors()));
        multiCart.setValidationWarnings(nullIfEmpty(multiCartResponse.getValidationWarnings()));
        multiCart.setBnplInfo(toBnplInfo(multiCartResponse.getBnplInfo()));
        if (multiCartResponse.getInstallmentsInfo() != null) {
            multiCart.setInstallmentsInfo(toInstallmentsInfo(multiCartResponse.getInstallmentsInfo()));
        }
        if (multiCartResponse.getTotals() != null) {
            multiCart.setTotals(toTotals(multiCartResponse.getTotals()));
            multiCart.setCashback(toCashback(multiCartResponse.getTotals().getCashback()));
        }
        multiCart.setCashbackOptionsProfiles(toCashbackProfiles(multiCartResponse.getCashbackProfiles()));
        multiCart.setCashbackBalance(multiCartResponse.getCashbackBalance());
        multiCart.setSelectedCashbackOption(multiCartResponse.getSelectedCashbackOption());
        return multiCart;
    }

    private <T> List<T> nullIfEmpty(List<T> origin) {
        return CollectionUtils.isEmpty(origin) ? null : origin;
    }

    @Nonnull
    private InstallmentsInfo toInstallmentsInfo(@Nonnull InstallmentsResponse installmentsInfo) {
        return new InstallmentsInfo(installmentsInfo.getOptions().stream()
                .map(this::toInstallmentsOption)
                .collect(Collectors.toUnmodifiableList()), null);
    }

    @Nonnull
    private InstallmentsOption toInstallmentsOption(@Nonnull InstallmentsOptionResponse installmentsOptionResponse) {
        return new InstallmentsOption(installmentsOptionResponse.getTerm(), new MonthlyPayment(
                installmentsOptionResponse.getMonthlyPayment().getCurrency(),
                installmentsOptionResponse.getMonthlyPayment().getValue()
        ));
    }

    private List<CashbackProfile> toCashbackProfiles(List<CashbackProfileResponse> cashbackProfilesResponse) {
        if (CollectionUtils.isEmpty(cashbackProfilesResponse)) {
            return null;
        }
        return cashbackProfilesResponse
                .stream()
                .map(cashbackProfileResponse -> new CashbackProfile(
                        cashbackProfileResponse.getCashbackTypes(),
                        cashbackProfileResponse.getCashbackOptionsPreconditions(),
                        null,
                        null,
                        toCashback(cashbackProfileResponse.getCashback()),
                        toCashbackOrders(cashbackProfileResponse.getOrders())))
                .collect(toList());
    }

    private List<OrderCashback> toCashbackOrders(List<CartCashbackResponse> cartCashbackResponses) {
        if (CollectionUtils.isEmpty(cartCashbackResponses)) {
            return null;
        }
        return cartCashbackResponses
                .stream()
                .map(cartCashbackResponse ->
                        new OrderCashback(cartCashbackResponse.getCartId(),
                                null,
                                toCashbackItems(cartCashbackResponse.getItems()),
                                toCashback(cartCashbackResponse.getCashback())))
                .collect(Collectors.toList());
    }

    private List<ItemCashback> toCashbackItems(List<ItemCashbackResponse> itemCashbackResponses) {
        if (CollectionUtils.isEmpty(itemCashbackResponses)) {
            return null;
        }
        return itemCashbackResponses
                .stream()
                .map(itemCashbackResponse ->
                        new ItemCashback(itemCashbackResponse.getOfferId(),
                                itemCashbackResponse.getFeedId(),
                                itemCashbackResponse.getCartId(),
                                itemCashbackResponse.getBundleId(),
                                toCashback(itemCashbackResponse.getCashback())))
                .collect(Collectors.toList());
    }

    private Cashback toCashback(CashbackResponse cashbackResponse) {
        if (cashbackResponse == null) {
            return null;
        }
        return new Cashback(toCashbackOptions(cashbackResponse.getEmit()),
                toCashbackOptions(cashbackResponse.getSpend()));
    }

    private CashbackOptions toCashbackOptions(CashbackOptionsResponse cashbackOptionsResponse) {
        if (cashbackOptionsResponse == null) {
            return null;
        }
        return new CashbackOptions(
                cashbackOptionsResponse.getPromoKey(),
                null,
                cashbackOptionsResponse.getAmount(),
                null,
                toCashbackPromos(cashbackOptionsResponse.getPromos()),
                cashbackOptionsResponse.getType(),
                cashbackOptionsResponse.getRestrictionReason(),
                cashbackOptionsResponse.getUiPromoFlags(),
                null,
                null
        );
    }

    private List<CashbackPromoResponse> toCashbackPromos(List<CashbackOptionsPromoResponse> promos) {
        if (CollectionUtils.isEmpty(promos)) {
            return null;
        }
        return promos.stream()
                .map(promo -> new CashbackPromoResponse(
                        promo.getAmount(),
                        promo.getPromoKey(),
                        promo.getPartnerId(),
                        promo.getNominal(),
                        promo.getMarketTariff(),
                        promo.getPartnerTariff(),
                        promo.getError(),
                        promo.getRevertToken(),
                        promo.getUiPromoFlags(),
                        promo.getCmsSemanticId(),
                        promo.getDetailsGroupName(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
                .collect(Collectors.toList());
    }

    private BnplInfo toBnplInfo(BnplInfoResponse bnplInfoResponse) {
        if (bnplInfoResponse == null) {
            return null;
        }
        BnplInfo bnplInfo = new BnplInfo();
        bnplInfo.setAvailable(bnplInfoResponse.isAvailable());
        bnplInfo.setSelected(bnplInfoResponse.isSelected());
        bnplInfo.setBnplPlanDetails(toBnplPlanDetails(bnplInfoResponse.getBnplPlanDetails()));
        bnplInfo.setPlans(toBnplPlans(bnplInfoResponse.getPlans()));
        bnplInfo.setSelectedPlan(bnplInfoResponse.getSelectedPlan());
        return bnplInfo;
    }

    private Collection<BnplPlanDetails> toBnplPlans(Collection<BnplPlanDetailsResponse> plans) {
        if (CollectionUtils.isEmpty(plans)) {
            return null;
        }
        return plans.stream()
                .map(this::toBnplPlanDetails)
                .collect(Collectors.toList());
    }

    private BnplPlanDetails toBnplPlanDetails(BnplPlanDetailsResponse bnplPlanDetailsResponse) {
        if (bnplPlanDetailsResponse == null) {
            return null;
        }
        BnplPlanDetails bnplPlanDetails = new BnplPlanDetails();
        bnplPlanDetails.setDeposit(bnplPlanDetailsResponse.getDeposit());
        bnplPlanDetails.setPayments(toBnplPayments(bnplPlanDetailsResponse.getPayments()));
        bnplPlanDetails.setVisualProperties(toVisualProperties(bnplPlanDetailsResponse.getVisualProperties()));
        bnplPlanDetails.setFee(bnplPlanDetailsResponse.getFee());
        bnplPlanDetails.setType(bnplPlanDetailsResponse.getType());
        bnplPlanDetails.setConstructor(bnplPlanDetailsResponse.getConstructor());
        bnplPlanDetails.setDetailsUrl(getBnplPlanDetailsUrl(bnplPlanDetails.getType()));
        return bnplPlanDetails;
    }

    private String getBnplPlanDetailsUrl(String bnplPlanType) {
        if (bnplPlanType == null) {
            return null;
        }
        if (bnplPlanType.equals("long_split")) {
            return "https://split.yandex.ru/#longsplit";
        }
        return "https://split.yandex.ru/market";
    }

    private BnplVisualProperties toVisualProperties(BnplVisualPropertiesResponse visualProperties) {
        if (visualProperties == null) {
            return null;
        }
        BnplVisualProperties bnplVisualProperties = new BnplVisualProperties();
        bnplVisualProperties.setNextDatesDescription(visualProperties.getNextDatesDescription());
        bnplVisualProperties.setNextPaymentsDescription(visualProperties.getNextPaymentsDescription());
        bnplVisualProperties.setColors(visualProperties.getColors());
        bnplVisualProperties.setShortTitle(visualProperties.getShortTitle());
        return bnplVisualProperties;
    }

    private List<BnplRegularPayment> toBnplPayments(List<BnplRegularPaymentResponse> payments) {
        if (CollectionUtils.isEmpty(payments)) {
            return null;
        }
        return payments
                .stream()
                .map(paymentResponse -> {
                    BnplRegularPayment payment = new BnplRegularPayment();
                    payment.setPaymentStatus(paymentResponse.getPaymentStatus());
                    payment.setAmount(paymentResponse.getAmount());
                    payment.setDatetime(paymentResponse.getDatetime());
                    return payment;
                })
                .collect(Collectors.toList());
    }

    private Map<FreeDeliveryReason, PriceLeftForFreeDeliveryResponseV3> toDeliveryDiscountMap(
            Map<FreeDeliveryReason, PriceLeftForFreeDeliveryResponse> deliveryDiscountMap) {
        if (CollectionUtils.isEmpty(deliveryDiscountMap)) {
            return null;
        }
        return deliveryDiscountMap.entrySet().stream().map(
                priceByReason -> Pair.of(priceByReason.getKey(),
                        new PriceLeftForFreeDeliveryResponseV3(
                                priceByReason.getValue().getPriceLeftForFreeDelivery(),
                                priceByReason.getValue().getThreshold(),
                                priceByReason.getValue().getStatus())))
                .collect(toMap(Pair::getKey, Pair::getValue));

    }

    private CostLimitInformation toCostLimitInformation(CostLimitInformationResponse costLimitInformationResponse) {
        if (costLimitInformationResponse == null) {
            return null;
        }
        CostLimitInformation costLimitInformation = new CostLimitInformation();
        costLimitInformation.setRemainingBeforeCheckout(costLimitInformationResponse.getRemainingBeforeCheckout());
        costLimitInformation.setMinCost(costLimitInformationResponse.getMinCost());
        costLimitInformation.setErrors(costLimitInformationResponse.getErrors());
        return costLimitInformation;
    }

    private HelpingHand toHelpingHand(HelpingHandResponse helpingHandResponse) {
        if (helpingHandResponse == null) {
            return null;
        }
        return HelpingHand.of(helpingHandResponse.getStatus(), helpingHandResponse.getDonationAmount());
    }

    private CreditInformation toCreditInformation(CreditInformationResponse creditInformationResponse) {
        if (creditInformationResponse == null) {
            return null;
        }
        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setPriceForCreditAllowed(creditInformationResponse.getPriceForCreditAllowed());
        creditInformation.setCreditMonthlyPayment(creditInformationResponse.getCreditMonthlyPayment());
        creditInformation.setCreditErrors(toCreditErrors(creditInformationResponse.getCreditErrors()));
        creditInformation.setOptions(toCreditOptions(creditInformationResponse.getOptions()));
        return creditInformation;
    }

    private List<CreditOption> toCreditOptions(List<CreditOptionResponse> options) {
        if (CollectionUtils.isEmpty(options)) {
            return null;
        }
        return options.stream()
                .map(creditOptionResponse -> {
                    MonthlyPayment creditMonthlyPayment =
                            new MonthlyPayment(creditOptionResponse.getMonthlyPayment().getCurrency(),
                                    creditOptionResponse.getMonthlyPayment().getValue());
                    return new CreditOption(creditOptionResponse.getTerm(), creditMonthlyPayment);
                })
                .collect(Collectors.toList());
    }

    private List<CreditError> toCreditErrors(List<CreditErrorResponse> creditErrors) {
        if (CollectionUtils.isEmpty(creditErrors)) {
            return null;
        }
        return creditErrors
                .stream()
                .map(creditErrorResponse -> {
                    CreditError creditError = new CreditError();
                    creditError.setErrorCode(creditErrorResponse.getErrorCode());
                    creditError.setInvalidItems(toInvalidItems(creditErrorResponse.getInvalidItems()));
                    return creditError;
                })
                .collect(Collectors.toList());
    }

    private List<InvalidCreditOrderItem> toInvalidItems(List<InvalidCreditCartItemResponse> invalidItemsResponse) {
        if (CollectionUtils.isEmpty(invalidItemsResponse)) {
            return null;
        }
        return invalidItemsResponse
                .stream()
                .map(invalidItemRs ->
                        new InvalidCreditOrderItem(invalidItemRs.getWareMd5()))
                .collect(Collectors.toList());
    }

    private CoinInfo toCoinInfo(CoinInfoResponse coinInfoResponse) {
        if (coinInfoResponse == null) {
            return null;
        }
        CoinInfo coinInfo = new CoinInfo();
        coinInfo.setAllCoins(coinInfoResponse.getAllCoins());
        coinInfo.setUnusedCoinIds(coinInfoResponse.getUnusedCoinIds());
        coinInfo.setCoinErrors(toCoinInfoErrors(coinInfoResponse.getCoinErrors()));
        return coinInfo;
    }

    private List<CoinError> toCoinInfoErrors(List<CoinErrorResponse> coinErrors) {
        if (CollectionUtils.isEmpty(coinErrors)) {
            return null;
        }
        return coinErrors
                .stream()
                .map(coinErrorResponse -> {
                    CoinError coinError = new CoinError();
                    coinError.setCoinId(coinErrorResponse.getCoinId());
                    coinError.setMessage(coinErrorResponse.getMessage());
                    coinError.setCode(coinErrorResponse.getCode());
                    return coinError;
                })
                .collect(Collectors.toList());
    }

    private MultiCartTotals toTotals(MultiCartTotalsResponse totalsResponse) {
        if (totalsResponse == null) {
            return null;
        }
        MultiCartTotals multiCartTotals = new MultiCartTotals();
        multiCartTotals.setBuyerTotal(totalsResponse.getBuyerTotal());
        multiCartTotals.setBuyerItemsTotal(totalsResponse.getBuyerItemsTotal());
        multiCartTotals.setBuyerDeliveryTotal(totalsResponse.getBuyerDeliveryTotal());
        multiCartTotals.setPromos(toTotalPromos(totalsResponse.getPromos()));
        return multiCartTotals;
    }

    private List<MultiCartPromo> toTotalPromos(List<MultiCartPromoResponse> promosResponse) {
        if (CollectionUtils.isEmpty(promosResponse)) {
            return null;
        }
        return promosResponse.stream().map(
                promoResponse -> {
                    MultiCartPromo multiCartPromo = new MultiCartPromo();
                    multiCartPromo.setDeliveryDiscount(promoResponse.getDeliveryDiscount());
                    multiCartPromo.setBuyerItemsDiscount(promoResponse.getBuyerItemsDiscount());
                    multiCartPromo.setPromoDefinition(PromoDefinition.builder()
                            .type(promoResponse.getType())
                            .coinId(promoResponse.getCoinId())
                            .promoCode(promoResponse.getPromocode())
                            .build());
                    return multiCartPromo;
                }
        ).collect(toList());
    }

    @Nonnull
    private Order toOrder(@Nonnull CartResponse cartResponse,
                          @Nonnull Parameters parameters) {
        Order order = new Order();
        order.setLabel(cartResponse.getLabel());
        order.setShopId(cartResponse.getShopId());
        order.setFulfilment(cartResponse.getFulfilment());
        order.setItems(toItem(cartResponse.getItems()));
        order.setValidationErrors(nullIfEmpty(cartResponse.getValidationErrors()));
        order.setValidationWarnings(nullIfEmpty(cartResponse.getValidationWarnings()));
        order.setChanges(toChanges(cartResponse.getChanges()));
        order.setAdditionalCartInfo(toAdditionalCartInfo(cartResponse.getDimensions()));
        order.setPreorder(cartResponse.getPreorder() == Boolean.TRUE);
        order.setRgb(parameters.configuration().cart().request().getColor());
        order.setPromos(cartResponse.getPromos().stream()
                .map(this::toOrderPromo)
                .collect(Collectors.toUnmodifiableList()));
        return order;
    }

    @Nonnull
    private OrderPromo toOrderPromo(@Nonnull OrderPromoResponse promoResponse) {
        var definition = PromoDefinition.builder()
                .type(promoResponse.getType())
                .marketPromoId(promoResponse.getMarketPromoId())
                .bundleId(promoResponse.getBundleId())
                .coinId(promoResponse.getCoinId())
                .build();
        var promo = new OrderPromo(definition);
        promo.setBuyerItemsDiscount(promoResponse.getBuyerItemsDiscount());
        promo.setDeliveryDiscount(promoResponse.getDeliveryDiscount());
        return promo;
    }

    @Nonnull
    private OrderFailure toOrderFailure(@Nonnull CartFailureResponse failureResponse,
                                        @Nonnull Parameters parameters) {
        return new OrderFailure(
                toOrder(failureResponse.getCart(), parameters),
                failureResponse.getErrorCode(),
                failureResponse.getErrorReason(),
                failureResponse.getErrorDetails(),
                failureResponse.getErrorDevDetails()
        );
    }

    private List<AdditionalCartInfo> toAdditionalCartInfo(List<DimensionsResponse> dimensions) {
        if (dimensions == null) {
            return null;
        }
        return dimensions
                .stream()
                .map(dimension -> {
                    AdditionalCartInfo additionalCartInfo = new AdditionalCartInfo();
                    additionalCartInfo.setWidth(dimension.getWidth());
                    additionalCartInfo.setHeight(dimension.getHeight());
                    additionalCartInfo.setWeight(dimension.getWeight());
                    additionalCartInfo.setDepth(dimension.getDepth());
                    return additionalCartInfo;
                })
                .collect(toList());
    }

    private Set<CartChange> toChanges(List<ChangeResponse> changes) {
        if (CollectionUtils.isEmpty(changes)) {
            return null;
        }
        return changes.stream().map(ChangeResponse::getCartChange).collect(Collectors.toSet());
    }


    private Collection<OrderItem> toItem(List<CartItemResponse> items) {
        if (CollectionUtils.isEmpty(items)) {
            return null;
        }
        return items.stream()
                .map(item -> {
                    OrderItem orderItem = new OrderItem(
                            null,
                            null,
                            null,
                            item.getBuyerPriceNominal(),
                            item.getBuyerPriceBeforeDiscount(),
                            null,
                            null,
                            null,
                            null);
                    orderItem.setLabel(item.getLabel());
                    orderItem.setMsku(item.getMsku());
                    orderItem.setFeedId(item.getFeedId());
                    Integer count = item.getCount();
                    orderItem.setCount(count);
                    orderItem.setQuantity(null == count ? null : BigDecimal.valueOf(count));
                    orderItem.setRelatedItemLabel(item.getRelatedItemLabel());
                    orderItem.setOfferId(item.getOfferId());
                    orderItem.setBuyerPrice(item.getBuyerPrice());
                    orderItem.setQuantPrice(item.getBuyerPrice());
                    orderItem.setChanges(item.getChanges());
                    orderItem.setPrimaryInBundle(item.getPrimaryInBundle());
                    orderItem.setServices(toServices(item.getServices()));
                    orderItem.setPromos(toPromos(item.getPromos()));
                    return orderItem;
                })
                .collect(toList());
    }

    private Set<? extends ItemPromo> toPromos(List<ItemPromoResponse> promos) {
        if (CollectionUtils.isEmpty(promos)) {
            return null;
        }
        return promos.stream()
                .map(
                        promo -> new ItemPromo(
                                PromoDefinition.builder()
                                        .type(promo.getType())
                                        .bundleId(promo.getBundleId())
                                        .coinId(promo.getCoinId())
                                        .build(),
                                promo.getBuyerDiscount(),
                                null,
                                null))
                .collect(Collectors.toSet());
    }

    private Set<ItemService> toServices(List<ItemServiceResponse> services) {
        if (CollectionUtils.isEmpty(services)) {
            return null;
        }
        return services
                .stream()
                .map(serviceResponse -> {
                    ItemService itemService = new ItemService();
                    itemService.setId(serviceResponse.getId());
                    itemService.setServiceId(serviceResponse.getServiceId());
                    itemService.setDate(serviceResponse.getDate());
                    itemService.setFromTime(serviceResponse.getFromTime());
                    itemService.setToTime(serviceResponse.getToTime());
                    itemService.setPrice(serviceResponse.getPrice());
                    return itemService;
                })
                .collect(toSet());
    }
}
