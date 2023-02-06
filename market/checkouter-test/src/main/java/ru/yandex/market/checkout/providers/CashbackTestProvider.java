package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackOptionsPrecondition;
import ru.yandex.market.loyalty.api.model.CashbackOptionsResponse;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackProfileDeliveryResponse;
import ru.yandex.market.loyalty.api.model.CashbackProfilePaymentResponse;
import ru.yandex.market.loyalty.api.model.CashbackProfileResponse;
import ru.yandex.market.loyalty.api.model.CashbackPromoResponse;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.CashbackThresholdResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.ItemCashbackResponse;
import ru.yandex.market.loyalty.api.model.OrderCashbackResponse;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;

import static ru.yandex.market.loyalty.api.model.CashbackPermision.ALLOWED;
import static ru.yandex.market.loyalty.api.model.CashbackPermision.RESTRICTED;

/**
 * @author : poluektov
 * date: 2020-09-14.
 */
public final class CashbackTestProvider {

    public static final long FEED = 1;
    public static final String OFFER = "first offer";
    public static final String CART_LABEL = "145_ABCDE";
    public static final BigDecimal CASHBACK_THRESHOLD_AMOUNT = BigDecimal.valueOf(5L);
    private static final String PROMO_KEY = "promo key";
    private static final String UI_PROMO_PARAM = "promo param";
    private static final String PROMO_CODE = "PROMO";

    private CashbackTestProvider() {
    }

    public static Parameters defaultCashbackParameters() {
        Parameters singleItemWithCashbackParams = BlueParametersProvider.defaultBlueOrderParameters();
        singleItemWithCashbackParams.setCheckCartErrors(false);
        singleItemWithCashbackParams.setupPromo(PROMO_CODE);
        singleItemWithCashbackParams.getLoyaltyParameters()
                .setExpectedCashbackOptionsResponse(singleItemCashbackResponse());
        singleItemWithCashbackParams.setMockLoyalty(true);
        return singleItemWithCashbackParams;
    }

    public static Parameters severalItemsCashbackParameters(OrderItem... items) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(items);
        parameters.setCheckCartErrors(false);
        parameters.setupPromo(PROMO_CODE);
        parameters.getLoyaltyParameters()
                .setExpectedCashbackOptionsResponse(singleItemCashbackResponse());
        parameters.setMockLoyalty(true);
        return parameters;
    }

    public static CashbackOptionsResponse singleItemCashbackResponse(BigDecimal spend) {
        CashbackResponse allowedResponse = new CashbackResponse(
                CashbackOptions.allowed(BigDecimal.valueOf(50L)),
                CashbackOptions.allowed(spend),
                null);
        CashbackResponse restrictedResponse = new CashbackResponse(
                null,
                CashbackOptions.restricted(CashbackRestrictionReason.NOT_SUITABLE_PAYMENT_TYPE),
                null);
        return new CashbackOptionsResponse(List.of(
                getCashbackProfileResponse(allowedResponse),
                getSpendCashbackProfileResponse(restrictedResponse)
        ));
    }

    public static CashbackOptionsResponse singleItemCashbackResponse() {
        CashbackResponse allowedResponse = new CashbackResponse(
                CashbackOptions.allowed(BigDecimal.valueOf(50L)),
                CashbackOptions.allowed(BigDecimal.valueOf(300L)),
                null);
        CashbackResponse restrictedResponse = new CashbackResponse(
                null,
                CashbackOptions.restricted(CashbackRestrictionReason.NOT_SUITABLE_PAYMENT_TYPE),
                null);
        return new CashbackOptionsResponse(List.of(
                getCashbackProfileResponse(allowedResponse),
                getSpendCashbackProfileResponse(restrictedResponse)
        ));
    }

    public static CashbackOptionsResponse singleItemCashbackResponseWithUiParams() {
        CashbackResponse allowedResponseWithParams = new CashbackResponse(
                CashbackOptions.allowedWithPromoKeyAndUiPromoFlags(
                        CashbackPromoResponse.builder()
                                .setAmount(BigDecimal.valueOf(50L))
                                .setPromoKey(PROMO_KEY)
                                .setUiPromoFlags(List.of(UI_PROMO_PARAM))
                                .build(),
                        List.of(UI_PROMO_PARAM)),
                CashbackOptions.allowedWithPromoKeyAndUiPromoFlags(
                        CashbackPromoResponse.builder()
                                .setAmount(BigDecimal.valueOf(300L))
                                .setPromoKey(PROMO_KEY)
                                .setUiPromoFlags(List.of(UI_PROMO_PARAM))
                                .build(),
                        List.of(UI_PROMO_PARAM)), null);
        return new CashbackOptionsResponse(List.of(
                getCashbackProfileResponse(allowedResponseWithParams)
        ));
    }

    public static CashbackOptionsResponse onlyOrderCashbackOptionsEmitResponseWithUiPromoFlags(
            BigDecimal amount,
            List<String> uiPromoFlags) {
        return new CashbackOptionsResponse(List.of(
                getOnlyOrderCashbackProfileResponse(сashbackEmitResponseWithUiPromoFlags(amount, uiPromoFlags))
        ));
    }

    @SuppressWarnings("checkstyle:MethodName")
    public static CashbackResponse сashbackEmitResponseWithUiPromoFlags(BigDecimal amount, List<String> uiPromoFlags) {
        return new CashbackResponse(
                CashbackOptions.allowedWithPromoKeyAndUiPromoFlags(CashbackPromoResponse.builder()
                                .setAmount(amount)
                                .setPromoKey(PROMO_KEY)
                                .setUiPromoFlags(uiPromoFlags)
                                .build(),
                        uiPromoFlags),
                null,
                CashbackType.EMIT
        );
    }

    public static CashbackOptionsResponse singleItemCashbackResponseWithThresholds() {
        CashbackResponse allowedResponse = new CashbackResponse(
                getCashbackOptionsWithThresholds(BigDecimal.valueOf(50L), ALLOWED),
                getCashbackOptionsWithThresholds(BigDecimal.valueOf(300L), ALLOWED),
                null);
        CashbackResponse restrictedResponse = new CashbackResponse(
                null,
                getCashbackOptionsWithThresholds(null, RESTRICTED),
                null);
        return new CashbackOptionsResponse(List.of(
                getCashbackProfileResponse(allowedResponse),
                getSpendCashbackProfileResponse(restrictedResponse)
        ));
    }

    @NotNull
    private static CashbackProfileResponse getSpendCashbackProfileResponse(CashbackResponse cashbackResponse) {
        return new CashbackProfileResponse(
                Set.of(CashbackType.SPEND),
                List.of(CashbackOptionsPrecondition.PAYMENT),
                new CashbackProfilePaymentResponse(
                        List.of(PaymentType.SHOP_PREPAID, PaymentType.CASH_ON_DELIVERY,
                                PaymentType.CARD_ON_DELIVERY, PaymentType.EXTERNAL_CERTIFICATE,
                                PaymentType.CREDIT, PaymentType.INSTALLMENT)),
                null,
                cashbackResponse,
                List.of(new OrderCashbackResponse(
                        CART_LABEL,
                        null,
                        List.of(new ItemCashbackResponse(OFFER, FEED, CART_LABEL, null,
                                cashbackResponse)),
                        cashbackResponse)));
    }

    @NotNull
    private static CashbackProfileResponse getCashbackProfileResponse(CashbackResponse cashbackResponse) {
        return new CashbackProfileResponse(
                Set.of(CashbackType.EMIT, CashbackType.SPEND),
                List.of(CashbackOptionsPrecondition.PAYMENT),
                new CashbackProfilePaymentResponse(
                        List.of(PaymentType.APPLE_PAY, PaymentType.BANK_CARD, PaymentType.GOOGLE_PAY,
                                PaymentType.YANDEX, PaymentType.YANDEX_MONEY)),
                new CashbackProfileDeliveryResponse(List.of(DeliveryType.COURIER, DeliveryType.PICKUP)),
                cashbackResponse,
                List.of(new OrderCashbackResponse(
                        CART_LABEL,
                        null,
                        List.of(new ItemCashbackResponse(OFFER, FEED, CART_LABEL, null,
                                cashbackResponse)),
                        cashbackResponse)));
    }

    @NotNull
    private static CashbackProfileResponse getOnlyOrderCashbackProfileResponse(CashbackResponse cashbackResponse) {
        return new CashbackProfileResponse(
                Set.of(CashbackType.EMIT, CashbackType.SPEND),
                List.of(CashbackOptionsPrecondition.PAYMENT),
                new CashbackProfilePaymentResponse(
                        List.of(PaymentType.APPLE_PAY, PaymentType.BANK_CARD, PaymentType.GOOGLE_PAY,
                                PaymentType.YANDEX, PaymentType.YANDEX_MONEY)),
                new CashbackProfileDeliveryResponse(List.of(DeliveryType.COURIER, DeliveryType.PICKUP)),
                cashbackResponse,
                List.of(new OrderCashbackResponse(
                        CART_LABEL,
                        null,
                        Collections.emptyList(),
                        cashbackResponse)));
    }

    private static CashbackOptions getCashbackOptionsWithThresholds(BigDecimal amount, CashbackPermision permission) {
        return new CashbackOptions(
                null,
                null,
                null,
                null,
                amount,
                permission,
                null,
                null,
                List.of(CashbackThresholdResponse.builder()
                        .setPromoKey(PROMO_KEY)
                        .setRemainingMultiCartTotal(permission == ALLOWED ? BigDecimal.ZERO : BigDecimal.TEN)
                        .setMinMultiCartTotal(BigDecimal.TEN)
                        .setAmount(permission == ALLOWED ? BigDecimal.TEN : CASHBACK_THRESHOLD_AMOUNT)
                        .build()
                ),
                null
        );
    }
}
