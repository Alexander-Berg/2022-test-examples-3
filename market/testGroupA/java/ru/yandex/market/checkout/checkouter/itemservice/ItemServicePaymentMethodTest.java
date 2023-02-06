package ru.yandex.market.checkout.checkouter.itemservice;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.OfferServiceBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.common.util.collections.CollectionUtils.emptyIfNull;
import static ru.yandex.common.util.collections.CollectionUtils.firstOrNull;
import static ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType.ITEM_SERVICE_AVAILABLE_PREPAY_METHODS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType.ITEM_SERVICE_AVAILABLE_PREPAY_SUBMETHODS;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;

public class ItemServicePaymentMethodTest extends AbstractWebTestBase {

    @Autowired
    private CheckouterFeatureWriter featureWriter;

    @Test
    @DisplayName("Выбран тип оплаты, доступный для услуг (предоплата включена)")
    public void shouldSetPrepayWhenTurnedOnAndAvailableMethodChosen() {
        checkouterProperties.setEnableServicesPrepay(true);
        featureWriter.writeValue(ITEM_SERVICE_AVAILABLE_PREPAY_METHODS, Set.of(PaymentMethod.YANDEX));
        featureWriter.writeValue(ITEM_SERVICE_AVAILABLE_PREPAY_SUBMETHODS, Set.of(PaymentSubmethod.DEFAULT));

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        itemService.setDate(null);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var createdMultiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            var cart = multiCart.getCarts().iterator().next();
            var is = cart.getItems().iterator().next().getServices().iterator().next();
            is.setDate(null);
            is.setFromTime((LocalTime) null);
            is.setToTime((LocalTime) null);
            is.setCount(2);
        });
        Long orderId = firstOrNull(emptyIfNull(createdMultiOrder.getOrders())).getId();
        Order actualOrder = orderService.getOrder(orderId, ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        assertEquals(actualOrder.getPaymentType(), actualItemService.getPaymentType());
        assertEquals(actualOrder.getPaymentMethod(), actualItemService.getPaymentMethod());
    }

    @Test
    @DisplayName("Выбран тип оплаты, недоступный для услуг (предоплата включена)")
    public void shouldSetPostpayWhenTurnedOnAndUnavailableMethodChosen() {
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableInstallments(true);
        featureWriter.writeValue(ITEM_SERVICE_AVAILABLE_PREPAY_METHODS, Set.of(PaymentMethod.YANDEX));
        featureWriter.writeValue(ITEM_SERVICE_AVAILABLE_PREPAY_SUBMETHODS, Set.of(PaymentSubmethod.DEFAULT));

        var parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(5000));
        parameters.setShowInstallments(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS);
        var itemService = ItemServiceProvider.defaultItemService();
        itemService.setDate(null);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var createdMultiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            var cart = multiCart.getCarts().iterator().next();
            var is = cart.getItems().iterator().next().getServices().iterator().next();
            is.setDate(null);
            is.setFromTime((LocalTime) null);
            is.setToTime((LocalTime) null);
            is.setCount(2);
            cart.setPaymentSubmethod(null);
        });
        Long orderId = firstOrNull(emptyIfNull(createdMultiOrder.getOrders())).getId();
        Order actualOrder = orderService.getOrder(orderId, ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        assertEquals(PaymentType.POSTPAID, actualItemService.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actualItemService.getPaymentMethod());
    }

    @Test
    @DisplayName("Выбран подтип оплаты, недоступный для услуг (предоплата включена)")
    public void shouldSetPostpayWhenTurnedOnAndUnavailableSubmethodChosen() {
        checkouterProperties.setEnableItemServiceTimeslots(false);
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
        featureWriter.writeValue(ITEM_SERVICE_AVAILABLE_PREPAY_METHODS, Set.of(PaymentMethod.YANDEX));
        featureWriter.writeValue(ITEM_SERVICE_AVAILABLE_PREPAY_SUBMETHODS, Set.of(PaymentSubmethod.DEFAULT));

        Parameters parameters = defaultBnplParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        itemService.setDate(null);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var createdMultiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            var cart = multiCart.getCarts().iterator().next();
            var is = cart.getItems().iterator().next().getServices().iterator().next();
            is.setDate(null);
            is.setFromTime((LocalTime) null);
            is.setToTime((LocalTime) null);
            is.setCount(2);
            cart.setPaymentSubmethod(null);
        });
        Long orderId = firstOrNull(emptyIfNull(createdMultiOrder.getOrders())).getId();
        Order actualOrder = orderService.getOrder(orderId, ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        assertEquals(PaymentType.POSTPAID, actualItemService.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actualItemService.getPaymentMethod());
    }

    @Test
    @DisplayName("Выбран тип оплаты, доступный для услуг (предоплата выключена)")
    public void shouldSetPostpayWhenTurnedOffAndAvailableMethodChosen() {
        checkouterProperties.setEnableServicesPrepay(false);
        featureWriter.writeValue(ITEM_SERVICE_AVAILABLE_PREPAY_METHODS, Set.of(PaymentMethod.YANDEX));
        featureWriter.writeValue(ITEM_SERVICE_AVAILABLE_PREPAY_SUBMETHODS, Set.of(PaymentSubmethod.DEFAULT));

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        itemService.setDate(null);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var createdMultiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            var cart = multiCart.getCarts().iterator().next();
            var is = cart.getItems().iterator().next().getServices().iterator().next();
            is.setDate(null);
            is.setFromTime((LocalTime) null);
            is.setToTime((LocalTime) null);
            is.setCount(2);
        });
        Long orderId = firstOrNull(emptyIfNull(createdMultiOrder.getOrders())).getId();
        Order actualOrder = orderService.getOrder(orderId, ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        assertEquals(PaymentType.POSTPAID, actualItemService.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actualItemService.getPaymentMethod());
    }
}
