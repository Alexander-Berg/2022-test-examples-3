package ru.yandex.market.checkout.checkouter.promo;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.DEFAULT_WARE_MD5;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class LoyaltyBuyerTest extends AbstractWebTestBase {

    private static final String REAL_PROMO_CODE = "REAL-PROMO-CODE";
    private static final String JSON_PATH_BUYER_PHONE = "$.operationContext.phone";
    private static final String OFFER_TAG = UUID.randomUUID().toString();

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CipherService reportCipherService;

    @Test
    public void testBuyerPassedToLoyaltyOnCartWhenBuyerInCartsIsEmpty() {
        Parameters parameters = buildParameters(multiCart -> multiCart.getCarts().forEach(cart -> cart.setBuyer(null)));
        cartAndEnsureBuyerPhonePassed(parameters);
    }

    @Test
    public void testBuyerPassedToLoyaltyOnCartWhenBuyerInCartsIsNotEmpty() {
        Parameters parameters = buildParameters(multiCart -> {
        });
        cartAndEnsureBuyerPhonePassed(parameters);
    }

    private void cartAndEnsureBuyerPhonePassed(Parameters parameters) {
        parameters.getReportParameters().setShopSupportsSubsidies(true);
        parameters.setMockLoyalty(true);

        orderCreateHelper.cart(parameters);
        loyaltyConfigurer.findAll(
                WireMock.postRequestedFor(urlPathEqualTo(LoyaltyConfigurer.URI_CALC_V3))
        ).forEach(this::checkBuyerPhone);
    }

    private Parameters buildParameters(Consumer<MultiCart> buyerConfigurer) {
        Order defaultOrder = OrderProvider.getBlueOrder();
        @Nonnull OrderItem anotherOrderItem = OrderItemProvider.buildOrderItem("2_" + OFFER_TAG, 1L, 1);
        anotherOrderItem.setWareMd5(DEFAULT_WARE_MD5 + "_item2");
        OrderItemProvider.patchShowInfo(anotherOrderItem, reportCipherService);
        defaultOrder.addItem(anotherOrderItem);

        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withOrder(defaultOrder)
                .withDeliveryServiceId(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(100501L, 2, Collections.singletonList(12312303L))
                                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, 1)
                                .build()
                )
                .buildParameters();

        parameters.configureMultiCart(multiCart -> {
            buyerConfigurer.accept(multiCart);
            multiCart.setPaymentMethod(PaymentMethod.YANDEX);
            multiCart.setPaymentType(PaymentType.PREPAID);
            multiCart.setPromoCode(REAL_PROMO_CODE);
        });

        return parameters;
    }

    private void checkBuyerPhone(LoggedRequest a) {
        JsonTest.checkJson(
                a.getBodyAsString(),
                JSON_PATH_BUYER_PHONE,
                "+71234567891"
        );
    }
}
