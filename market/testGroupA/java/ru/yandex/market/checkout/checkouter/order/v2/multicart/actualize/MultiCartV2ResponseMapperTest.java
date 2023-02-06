package ru.yandex.market.checkout.checkouter.order.v2.multicart.actualize;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ChangeReason;
import ru.yandex.market.checkout.checkouter.cart.CostLimitInformation;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfile;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfileDelivery;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.ItemCashback;
import ru.yandex.market.checkout.checkouter.cashback.model.OrderCashback;
import ru.yandex.market.checkout.checkouter.credit.CreditError;
import ru.yandex.market.checkout.checkouter.credit.CreditInformation;
import ru.yandex.market.checkout.checkouter.credit.CreditOption;
import ru.yandex.market.checkout.checkouter.credit.InvalidCreditOrderItem;
import ru.yandex.market.checkout.checkouter.installments.MonthlyPayment;
import ru.yandex.market.checkout.checkouter.order.CoinInfo;
import ru.yandex.market.checkout.checkouter.order.HelpingHand;
import ru.yandex.market.checkout.checkouter.order.HelpingHandStatus;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.MultiCartPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.MultiCartV2ResponseMapper;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartCashbackResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartItemResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CashbackOptionsPromoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CashbackOptionsResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CashbackProfileResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CashbackResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.ChangeResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CostLimitInformationResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CreditErrorResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CreditInformationResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CreditOptionResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.ItemCashbackResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.ItemPromoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.ItemServiceResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartPromoResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartTotalsResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.PriceLeftForFreeDeliveryResponse;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.loyalty.api.model.CashbackOptionsPrecondition;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.api.model.coin.CoinRestriction;
import ru.yandex.market.loyalty.api.model.coin.CoinRestrictionType;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.Link;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiCartV2ResponseMapperTest {

    @Test
    public void mapperValidationResult() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        String codeExpected = "code";
        String messageExpected = "message";
        multiCart.setValidationErrors(
                List.of(new ValidationResult(codeExpected, ValidationResult.Severity.ERROR, messageExpected)));
        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);
        var validationResultResponse = multiCartResponse.getValidationErrors().get(0);
        assertEquals(codeExpected, validationResultResponse.getCode());
        assertEquals(ValidationResult.Severity.ERROR, validationResultResponse.getSeverity());
        assertEquals(messageExpected, validationResultResponse.getMessage());
    }

    @Test
    public void mapperDeliveryDiscountMap() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        FreeDeliveryReason expectedReason = FreeDeliveryReason.YA_PLUS_FREE_DELIVERY;
        PriceLeftForFreeDeliveryResponseV3 freeDeliveryExpected = new PriceLeftForFreeDeliveryResponseV3(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(101),
                FreeDeliveryStatus.ALREADY_FREE);
        multiCart.setDeliveryDiscountMap(Map.of(expectedReason, freeDeliveryExpected));
        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);

        Map.Entry<FreeDeliveryReason, PriceLeftForFreeDeliveryResponse> priceLeftByReason =
                multiCartResponse.getDeliveryDiscountMap().entrySet().iterator().next();
        assertEquals(expectedReason, priceLeftByReason.getKey());
        assertEquals(freeDeliveryExpected.getPriceLeftForFreeDelivery(),
                priceLeftByReason.getValue().getPriceLeftForFreeDelivery());
        assertEquals(freeDeliveryExpected.getStatus(), priceLeftByReason.getValue().getStatus());
        assertEquals(freeDeliveryExpected.getThreshold(), priceLeftByReason.getValue().getThreshold());
    }

    @Test
    public void mapperCostLimitInformation() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        CostLimitInformation costLimitInformation = new CostLimitInformation();
        costLimitInformation.setMinCost(BigDecimal.valueOf(135));
        costLimitInformation.setRemainingBeforeCheckout(BigDecimal.valueOf(20));
        costLimitInformation.setErrors(List.of(CostLimitInformation.Code.TOO_CHEAP_MULTI_CART));
        multiCart.setCostLimitInformation(costLimitInformation);
        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);

        CostLimitInformationResponse costLimitInformationResponse = multiCartResponse.getCostLimitInformation();
        assertEquals(costLimitInformation.getRemainingBeforeCheckout(),
                costLimitInformationResponse.getRemainingBeforeCheckout());
        assertEquals(costLimitInformation.getMinCost(), costLimitInformationResponse.getMinCost());
        assertEquals(costLimitInformation.getErrors().get(0),
                costLimitInformationResponse.getErrors().get(0));
    }

    @Test
    public void mapperHelpingHand() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        multiCart.setHelpingHand(HelpingHand.of(HelpingHandStatus.ENABLED, 123));
        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);
        assertEquals(multiCart.getHelpingHand().getStatus(), multiCartResponse.getHelpingHand().getStatus());
        assertEquals(multiCart.getHelpingHand().getDonationAmount(),
                multiCartResponse.getHelpingHand().getDonationAmount());

    }

    @Test
    public void mapperCreditInformation() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setPriceForCreditAllowed(BigDecimal.valueOf(432));
        creditInformation.setCreditMonthlyPayment(BigDecimal.valueOf(231));
        CreditError creditError = new CreditError();
        creditError.setErrorCode("code");
        InvalidCreditOrderItem invalidCreditOrderItem = new InvalidCreditOrderItem();
        invalidCreditOrderItem.setWareMd5("wareMd5");
        creditError.setInvalidItems(List.of(invalidCreditOrderItem));
        creditInformation.setCreditErrors(List.of(creditError));
        CreditOption creditOption = new CreditOption("3",
                new MonthlyPayment(Currency.findByName("RUR"), "1075"));
        creditInformation.setOptions(List.of(creditOption));
        multiCart.setCreditInformation(creditInformation);

        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);

        CreditInformationResponse creditInformationResponse = multiCartResponse.getCreditInformation();
        assertEquals(creditInformation.getPriceForCreditAllowed(),
                creditInformationResponse.getPriceForCreditAllowed());
        assertEquals(creditInformation.getCreditMonthlyPayment(), creditInformationResponse.getCreditMonthlyPayment());

        CreditErrorResponse creditErrorResponse = creditInformationResponse.getCreditErrors().get(0);
        assertEquals(creditError.getErrorCode(), creditErrorResponse.getErrorCode());
        InvalidCreditOrderItem creditOrderItemResponse = creditError.getInvalidItems().get(0);
        assertEquals(invalidCreditOrderItem.getWareMd5(), creditOrderItemResponse.getWareMd5());

        CreditOptionResponse creditOptionResponse =
                Objects.requireNonNull(creditInformationResponse.getOptions()).get(0);
        assertEquals(creditOption.getTerm(), creditOptionResponse.getTerm());
        assertEquals(creditOption.getMonthlyPayment().getCurrency(),
                creditOptionResponse.getMonthlyPayment().getCurrency());
        assertEquals(creditOption.getMonthlyPayment().getValue(),
                creditOptionResponse.getMonthlyPayment().getValue());
    }

    @Test
    public void mapperCoinInfo() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        List<UserCoinResponse> allCoinsExpected = buildAllCoins();
        CoinInfo coinInfo = new CoinInfo();
        coinInfo.setAllCoins(allCoinsExpected);
        multiCart.setCoinInfo(coinInfo);

        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);

        assertEquals(allCoinsExpected, multiCartResponse.getCoinInfo().getAllCoins());
    }

    @Test
    public void mapperTotals() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        MultiCartTotals totals = new MultiCartTotals();
        totals.setBuyerDeliveryTotal(BigDecimal.valueOf(111));
        totals.setBuyerTotal(BigDecimal.valueOf(321));
        totals.setBuyerItemsTotal(BigDecimal.valueOf(211));
        MultiCartPromo multiCartPromo = new MultiCartPromo();
        multiCartPromo.setBuyerItemsDiscount(BigDecimal.valueOf(20));
        multiCartPromo.setDeliveryDiscount(BigDecimal.valueOf(13));
        PromoDefinition promoDefinition = PromoDefinition.builder()
                .type(PromoType.YANDEX_PLUS)
                .coinId(133L)
                .promoCode("promocode")
                .build();
        multiCartPromo.setPromoDefinition(promoDefinition);
        totals.setPromos(List.of(multiCartPromo));
        multiCart.setTotals(totals);
        Currency buyerCurrency = Currency.RUR;
        multiCart.setBuyerCurrency(buyerCurrency);

        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);

        MultiCartTotalsResponse totalsActual = multiCartResponse.getTotals();
        assertEquals(totals.getBuyerDeliveryTotal(), totalsActual.getBuyerDeliveryTotal());
        assertEquals(totals.getBuyerTotal(), totalsActual.getBuyerTotal());
        assertEquals(totals.getBuyerItemsTotal(), totalsActual.getBuyerItemsTotal());
        assertEquals(buyerCurrency, totalsActual.getBuyerCurrency());

        MultiCartPromoResponse multiCartPromoActual = totalsActual.getPromos().get(0);
        assertEquals(multiCartPromo.getDeliveryDiscount(), multiCartPromoActual.getDeliveryDiscount());
        assertEquals(multiCartPromo.getBuyerItemsDiscount(), multiCartPromoActual.getBuyerItemsDiscount());
        assertEquals(multiCartPromo.getPromoDefinition().getPromoCode(), multiCartPromoActual.getPromocode());
        assertEquals(multiCartPromo.getPromoDefinition().getCoinId(), multiCartPromoActual.getCoinId());
        assertEquals(multiCartPromo.getPromoDefinition().getType(), multiCartPromoActual.getType());
    }

    @Test
    public void mapperCart() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        Order order = new Order();
        order.setLabel("label");
        order.setShopId(1221L);
        order.setFulfilment(true);
        order.setPreorder(false);
        multiCart.setCarts(List.of(order));
        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);
        assertEquals(1, multiCartResponse.getCarts().size());
        CartResponse cartActual = multiCartResponse.getCarts().get(0);
        assertEquals(order.getLabel(), cartActual.getLabel());
        assertEquals(order.getShopId(), cartActual.getShopId());
        assertEquals(order.isFulfilment(), cartActual.getFulfilment());
        assertEquals(order.isPreorder(), cartActual.getPreorder());
    }

    @Test
    public void mapperChanged() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        Order order = new Order();
        multiCart.setCarts(List.of(order));
        CartChange cartChangeExpected = CartChange.DELIVERY;
        ChangeReason changeReasonExpected = ChangeReason.DELIVERY_OPTION_MISMATCH;
        order.setChangesReasons(Map.of(cartChangeExpected, List.of(changeReasonExpected)));
        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);
        List<ChangeResponse> changes = multiCartResponse.getCarts().get(0).getChanges();
        assertEquals(1, changes.size());
        assertEquals(cartChangeExpected, changes.get(0).getCartChange());
        assertEquals(changeReasonExpected, changes.get(0).getChangeReason());
    }

    @Test
    public void mapperItems() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        Order order = new Order();
        OrderItem orderItem = OrderItemProvider.defaultOrderItem();

        orderItem.addChange(ItemChange.COUNT);

        ItemPromo itemPromo = new ItemPromo(
                PromoDefinition.builder()
                        .coinId(1111L)
                        .type(PromoType.GENERIC_BUNDLE)
                        .bundleId("2222").build(),
                BigDecimal.valueOf(231),
                BigDecimal.valueOf(11),
                BigDecimal.valueOf(15));
        orderItem.setPromos(Set.of(itemPromo));

        ItemService itemService = ItemServiceProvider.defaultItemService();
        orderItem.setServices(Set.of(itemService));

        order.setItems(List.of(orderItem));
        multiCart.setCarts(List.of(order));
        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);
        CartItemResponse cartItemActual = multiCartResponse.getCarts().get(0).getItems().get(0);

        assertEquals(orderItem.getLabel(), cartItemActual.getLabel());
        assertEquals(orderItem.getMsku(), cartItemActual.getMsku());
        assertEquals(orderItem.getFeedId(), cartItemActual.getFeedId());
        assertEquals(orderItem.getCount(), cartItemActual.getCount());
        assertEquals(orderItem.getRelatedItemLabel(), cartItemActual.getRelatedItemLabel());
        assertEquals(orderItem.getOfferId(), cartItemActual.getOfferId());
        assertEquals(orderItem.getPrices().getBuyerPriceNominal(), cartItemActual.getBuyerPriceNominal());
        assertEquals(orderItem.getPrices().getBuyerPriceBeforeDiscount(), cartItemActual.getBuyerPriceBeforeDiscount());
        assertEquals(orderItem.getBuyerPrice(), cartItemActual.getBuyerPrice());
        assertEquals(orderItem.getChanges(), cartItemActual.getChanges());
        assertEquals(orderItem.getPrimaryInBundle(), cartItemActual.getPrimaryInBundle());

        ItemPromoResponse itemPromoActual = cartItemActual.getPromos().get(0);
        assertEquals(itemPromo.getType(), itemPromoActual.getType());
        assertEquals(itemPromo.getBuyerDiscount(), itemPromoActual.getBuyerDiscount());
        assertEquals(itemPromo.getPromoDefinition().getCoinId(), itemPromoActual.getCoinId());
        assertEquals(itemPromo.getPromoDefinition().getBundleId(), itemPromoActual.getBundleId());

        ItemServiceResponse itemServiceActual = cartItemActual.getServices().get(0);
        assertEquals(itemService.getId(), itemServiceActual.getId());
        assertEquals(itemService.getServiceId(), itemServiceActual.getServiceId());
        assertEquals(itemService.getDate(), itemServiceActual.getDate());
        assertEquals(itemService.getFromTime(), itemServiceActual.getFromTime());
        assertEquals(itemService.getToTime(), itemServiceActual.getToTime());
        assertEquals(itemService.getPrice(), itemServiceActual.getPrice());
    }

    @Test
    public void mapperBuyerItemsTotalBeforeDiscount() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();
        Order order = new Order();
        BigDecimal buyerItemsTotalBeforeDiscount = BigDecimal.valueOf(100);
        order.getPromoPrices().setBuyerItemsTotalBeforeDiscount(buyerItemsTotalBeforeDiscount);
        multiCart.setCarts(List.of(order));
        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);
        assertEquals(buyerItemsTotalBeforeDiscount, multiCartResponse.getTotals().getBuyerItemsTotalBeforeDiscount());
    }

    @Test
    public void mapperCashback() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();

        CashbackOptions emit = new CashbackOptions(null, null, BigDecimal.valueOf(20),
                null, null, CashbackPermision.ALLOWED,
                CashbackRestrictionReason.CASHBACK_DISABLED, List.of("flag"), null, null);


        CashbackOptions spend = new CashbackOptions(null, null, BigDecimal.valueOf(20),
                null, null, CashbackPermision.ALLOWED,
                CashbackRestrictionReason.CASHBACK_DISABLED, List.of("flag"), null, null);

        Cashback cashback = new Cashback(emit, spend);
        multiCart.setCashback(cashback);

        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);

        CashbackResponse cashbackActual = multiCartResponse.getTotals().getCashback();
        checkCashback(emit, cashbackActual.getEmit());
        checkCashback(spend, cashbackActual.getSpend());
    }

    @Test
    public void mapperCashbackProfiles() {
        MultiCart multiCart = MultiCartProvider.createBuilder().build();

        List<CashbackPromoResponse> promos = List.of(new CashbackPromoResponse(
                BigDecimal.valueOf(21), "promoKey1", 22L, BigDecimal.valueOf(23),
                BigDecimal.valueOf(24), BigDecimal.valueOf(25), MarketLoyaltyErrorCode.COIN_ALREADY_USED,
                "token", List.of("ui"), "cms", "detaild", null, null, null, null,
                null, null, null, null));

        CashbackOptions emit = new CashbackOptions("promoKey1", null, BigDecimal.valueOf(20),
                null, promos, CashbackPermision.ALLOWED,
                CashbackRestrictionReason.CASHBACK_DISABLED, List.of("flag"), null, null);

        CashbackOptions emitOrder = new CashbackOptions("promoKey2", null, BigDecimal.valueOf(21),
                null, null, CashbackPermision.ALLOWED,
                CashbackRestrictionReason.NOT_SUITABLE_PAYMENT_TYPE, List.of("flag2"), null, null);

        CashbackOptions emitItem = new CashbackOptions("promoKey3", null, BigDecimal.valueOf(22),
                null, null, CashbackPermision.RESTRICTED,
                CashbackRestrictionReason.INCOMPLETE_REQUEST, List.of("flag3"), null, null);

        ItemCashback itemCashback = new ItemCashback("offerId", 123L, "cartId",
                "bundleId", new Cashback(emitItem, null));
        OrderCashback orderCashback = new OrderCashback("label1", null, List.of(itemCashback),
                new Cashback(emitOrder, null));
        CashbackProfile cashbackProfile = new CashbackProfile(Set.of(CashbackType.EMIT),
                List.of(CashbackOptionsPrecondition.DELIVERY),
                null,
                new CashbackProfileDelivery(List.of(DeliveryType.COURIER)),
                new Cashback(emit, null),
                List.of(orderCashback));

        multiCart.setCashbackOptionsProfiles(List.of(cashbackProfile));

        MultiCartResponse multiCartResponse = MultiCartV2ResponseMapper.toMultiCartResponse(multiCart);

        List<CashbackProfileResponse> cashbackProfilesActual = multiCartResponse.getCashbackProfiles();

        assertEquals(1, cashbackProfilesActual.size());
        CashbackProfileResponse cashbackProfileResponseActual = cashbackProfilesActual.get(0);

        checkCashback(emit, cashbackProfileResponseActual.getCashback().getEmit());

        assertEquals(cashbackProfile.getCashbackTypes(), cashbackProfileResponseActual.getCashbackTypes());
        assertEquals(cashbackProfile.getCashbackOptionsPreconditions(),
                cashbackProfileResponseActual.getCashbackOptionsPreconditions());
        assertEquals(cashbackProfile.getDelivery().getTypes(), cashbackProfileResponseActual.getDelivery().getTypes());

        assertEquals(1, cashbackProfileResponseActual.getOrders().size());
        CartCashbackResponse cartCashbackResponse = cashbackProfileResponseActual.getOrders().get(0);

        assertEquals(orderCashback.getCartId(), cartCashbackResponse.getCartId());
        checkCashback(emitOrder, cartCashbackResponse.getCashback().getEmit());

        assertEquals(1, cartCashbackResponse.getItems().size());
        ItemCashbackResponse itemCashbackActual = cartCashbackResponse.getItems().get(0);

        checkCashback(emitItem, itemCashbackActual.getCashback().getEmit());
        assertEquals(itemCashbackActual.getCartId(), itemCashbackActual.getCartId());
        assertEquals(itemCashbackActual.getBundleId(), itemCashbackActual.getBundleId());
        assertEquals(itemCashbackActual.getOfferId(), itemCashbackActual.getOfferId());
        assertEquals(itemCashbackActual.getFeedId(), itemCashbackActual.getFeedId());
    }

    private void checkCashback(CashbackOptions cashbackOptions, CashbackOptionsResponse cashbackOptionsActual) {
        assertEquals(cashbackOptions.getAmount(), cashbackOptionsActual.getAmount());
        assertEquals(cashbackOptions.getUiPromoFlags(), cashbackOptionsActual.getUiPromoFlags());
        assertEquals(cashbackOptions.getType(), cashbackOptionsActual.getType());
        assertEquals(cashbackOptions.getRestrictionReason(), cashbackOptionsActual.getRestrictionReason());
        assertEquals(cashbackOptions.getPromoKey(), cashbackOptionsActual.getPromoKey());
        checkCashbackPromosWithEmptyOrOneElement(cashbackOptions.getPromos(), cashbackOptionsActual.getPromos());
    }

    private void checkCashbackPromosWithEmptyOrOneElement(List<CashbackPromoResponse> promos,
                                                          List<CashbackOptionsPromoResponse> promosActual) {
        if (CollectionUtils.isEmpty(promos)) {
            assertTrue(CollectionUtils.isEmpty(promosActual));
            return;
        }
        assertEquals(1, promosActual.size());
        assertEquals(promos.size(), promosActual.size());
        CashbackPromoResponse promoResponse = promos.get(0);
        CashbackOptionsPromoResponse promoResponseActual = promosActual.get(0);

        assertEquals(promoResponse.getAmount(), promoResponseActual.getAmount());
        assertEquals(promoResponse.getPromoKey(), promoResponseActual.getPromoKey());
        assertEquals(promoResponse.getPartnerId(), promoResponseActual.getPartnerId());
        assertEquals(promoResponse.getNominal(), promoResponseActual.getNominal());
        assertEquals(promoResponse.getMarketTariff(), promoResponseActual.getMarketTariff());
        assertEquals(promoResponse.getPartnerTariff(), promoResponseActual.getPartnerTariff());
        assertEquals(promoResponse.getError(), promoResponseActual.getError());
        assertEquals(promoResponse.getRevertToken(), promoResponseActual.getRevertToken());
        assertEquals(promoResponse.getUiPromoFlags(), promoResponseActual.getUiPromoFlags());
        assertEquals(promoResponse.getCmsSemanticId(), promoResponseActual.getCmsSemanticId());
        assertEquals(promoResponse.getDetailsGroupName(), promoResponseActual.getDetailsGroupName());
    }

    @NotNull
    private List<UserCoinResponse> buildAllCoins() {
        return List.of(new UserCoinResponse(
                555L,
                "title",
                "subtitle",
                CoinType.FIXED,
                BigDecimal.valueOf(100),
                "description",
                "inactiveDescription",
                new Date(),
                DateUtils.addDays(new Date(), 1),
                "image",
                Map.of("keyImages", "valueImages"),
                "green",
                CoinStatus.ACTIVE,
                true,
                "activationToken",
                buildCoinRestriction(),
                CoinCreationReason.OTHER,
                "reasonParam",
                false,
                buildBonusLink(),
                "outgoingLink",
                true,
                "promoKey",
                null));
    }

    private List<Link> buildBonusLink() {
        return List.of(new Link("ref", "titleLink"));
    }

    private List<CoinRestriction> buildCoinRestriction() {
        return List.of(new CoinRestriction(CoinRestrictionType.CATEGORY, null, 943));
    }
}
