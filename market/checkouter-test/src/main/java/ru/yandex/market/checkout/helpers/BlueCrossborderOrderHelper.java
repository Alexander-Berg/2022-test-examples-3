package ru.yandex.market.checkout.helpers;

import java.io.IOException;
import java.util.Collections;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.pushApi.PushApiConfigurer;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static java.util.Collections.singletonList;

/**
 * @author sergeykoles
 * Created on: 17.09.2019
 */
@WebTestHelper
@Deprecated
public class BlueCrossborderOrderHelper {

    public static final long RED_MARKET_VIRTUAL_SHOP_ID = 1775L;
    public static final long CROSSBORDER_SUPPLIER_ID = 1667L;

    @Autowired
    private CheckouterAPI client;

    @Autowired
    private OrderCreateHelper orderCreateHelper;

    @Autowired
    private PushApiConfigurer pushApiConfigurer;

    public MultiCart doCartBlueWithoutFulfilment(Parameters parameters) throws IOException {
        orderCreateHelper.initializeMock(parameters);
        CartParameters cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        MultiCart multiCartRequest = parameters.getBuiltMultiCart();
        multiCartRequest.setPaymentMethod(PaymentMethod.YANDEX);
        multiCartRequest.setPaymentType(PaymentType.PREPAID);
        return client.cart(multiCartRequest, cartParameters);
    }

    public Parameters createDefaultParameters() {
        DeliveryResponse deliveryResponse = DeliveryProvider.buildShopPostDeliveryResponse(DeliveryResponse::new);
        deliveryResponse.setPaymentOptions(Sets.newHashSet(PaymentMethod.YANDEX));
        return setupParameters(deliveryResponse, BlueParametersProvider.defaultBlueOrderParameters());
    }

    public Parameters setupParametersForMultiOrder(DeliveryResponse deliveryResponse) {
        Parameters crossborderParameters = setupParameters(deliveryResponse,
                BlueParametersProvider.defaultBlueOrderParameters());
        // Заменяем айтем чтобы он не совпадал с айтемом из defaultBlueOrderParameters
        crossborderParameters.getOrder().setItems(null);
        crossborderParameters.addOtherItem();
        crossborderParameters.getOrder().getItems()
                .forEach(oi -> crossborderParameters.getReportParameters().overrideItemInfo(oi.getFeedOfferId())
                        .getFulfilment().fulfilment = false);
        crossborderParameters.getOrder().setDelivery(deliveryResponse);
        crossborderParameters.getReportParameters().setDeliveryPartnerTypes(singletonList("SHOP"));
        crossborderParameters.setPaymentMethod(PaymentMethod.YANDEX);
        return crossborderParameters;
    }

    /**
     * Наделяет набор параметров суперспособностью к трансграничности на синем. Можно сделать несколько айтемов.
     *
     * @param parameters исходных набор параметров. можно тупо {@code new Parameters()}. Они будут изменены.
     * @return всё те же исходные параметры, но с суперсилой.
     */
    public Parameters setupParameters(Parameters parameters) {
        DeliveryResponse deliveryResponse = DeliveryProvider.buildShopPostDeliveryResponse(DeliveryResponse::new);
        deliveryResponse.setPaymentOptions(Sets.newHashSet(PaymentMethod.YANDEX));
        return setupParameters(deliveryResponse, parameters);
    }

    /**
     * Наделяет набор параметров суперспособностью к трансграничности на синем. Можно сделать несколько айтемов.
     *
     * @param deliveryResponse ответ пушапи по доставке
     * @param parameters       исходных набор параметров. можно тупо {@code new Parameters()}. Они будут изменены.
     * @return всё те же исходные параметры, но с суперсилой.
     */
    public Parameters setupParameters(DeliveryResponse deliveryResponse, Parameters parameters) {
        parameters.setColor(Color.BLUE);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setDeliveryType(DeliveryType.POST);

        setupReportParameters(parameters);

        parameters.setPushApiDeliveryResponse(deliveryResponse);
        parameters.setMockLoyalty(true);

        ShopMetaData shopMetaData = ShopSettingsHelper.createCustomCrossborderMeta((int) RED_MARKET_VIRTUAL_SHOP_ID);
        parameters.setShopId(RED_MARKET_VIRTUAL_SHOP_ID);
        parameters.addShopMetaData(RED_MARKET_VIRTUAL_SHOP_ID, shopMetaData);
        parameters.addShopMetaData(CROSSBORDER_SUPPLIER_ID,
                ShopSettingsHelper.createCustomCrossborderMeta((int) CROSSBORDER_SUPPLIER_ID));
        return parameters;
    }

    private void setupReportParameters(Parameters parameters) {
        parameters.getOrders().forEach(
                order -> {
                    order.setShopId(RED_MARKET_VIRTUAL_SHOP_ID);
                    order.getItems().forEach(
                            oi -> {
                                final ItemInfo itemInfo = parameters.getReportParameters()
                                        .overrideItemInfo(oi.getFeedOfferId());
                                itemInfo.setFulfilment(
                                        new ItemInfo.Fulfilment(
                                                CROSSBORDER_SUPPLIER_ID,
                                                FulfilmentProvider.TEST_SKU,
                                                FulfilmentProvider.TEST_SHOP_SKU,
                                                null,
                                                false
                                        )
                                );
                                itemInfo.setSupplierType(SupplierType.THIRD_PARTY);
                                oi.setSupplierId(CROSSBORDER_SUPPLIER_ID);
                            }
                    );
                }
        );
        parameters.getReportParameters()
                .setDeliveryPartnerTypes(singletonList(DeliveryPartnerType.SHOP.name()));
        parameters.getReportParameters().setCrossborder(true);
        parameters.getReportParameters().setDeliveryPartnerTypes(Collections.singletonList("SHOP"));
    }

    public MultiOrder checkoutCrossborder(Parameters parameters) throws IOException {
        parameters.setMockPushApi(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), parameters.getPushApiDeliveryResponses(), true);
        pushApiConfigurer.mockAccept(parameters.getOrder(), true);

        MultiCart cart = doCartBlueWithoutFulfilment(parameters);
        final CheckoutParameters checkoutParameters = CheckoutParameters.builder()
                .withUid(cart.getBuyer().getUid())
                .withSandbox(false)
                .withReserveOnly(false)
                .withRgb(Color.BLUE)
                .withContext(Context.MARKET)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withHitRateGroup(HitRateGroup.LIMIT)
                .build();
        return client.checkout(
                orderCreateHelper.mapCartToOrder(cart, parameters),
                checkoutParameters
        );
    }

}
