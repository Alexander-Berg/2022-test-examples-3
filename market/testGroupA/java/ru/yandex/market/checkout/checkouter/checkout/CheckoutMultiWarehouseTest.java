package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.validation.DifferentWarehousesError;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.FF_SHOP_ID;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SHOP_SKU;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SKU;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.DELIVERY_PRICE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryResultProvider.SHIPMENT_DAY;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CheckoutMultiWarehouseTest extends AbstractWebTestBase {

    @Autowired
    private FulfillmentConfigurer fulfillmentConfigurer;

    @Autowired
    private WireMockServer reportMock;

    @Test
    public void shouldCreateOrderWithSameWarehouses() {
        OrderItem orderItem1 = OrderItemProvider.defaultOrderItem();
        OrderItem orderItem2 = OrderItemProvider.getAnotherOrderItem();

        Parameters parameters = orderParameters(orderItem1, orderItem2);
        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(222));

        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(Color.BLUE, order.getRgb());

        assertThat(
                reportMock.getServeEvents().getServeEvents()
                        .stream()
                        .filter(
                                se -> se.getRequest()
                                        .queryParameter("place")
                                        .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId())
                        )
                        .collect(Collectors.toList()),
                hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    public void doNotCreateOrderWithDifferentWarehouses() {
        OrderItem orderItem1 = OrderItemProvider.defaultOrderItem();
        OrderItem orderItem2 = OrderItemProvider.getAnotherOrderItem();

        Parameters parameters = orderParameters(orderItem1, orderItem2);
        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(oi.getWareMd5().equals(orderItem1.getWareMd5()) ? 222 : 333));

        parameters.setCheckCartErrors(false);

        MultiCart mc = orderCreateHelper.cart(parameters);
        List<ValidationResult> validationErrors = mc.getCarts().iterator().next().getValidationErrors();
        assertThat(validationErrors, hasSize(1));
        assertEquals(DifferentWarehousesError.CODE, validationErrors.iterator().next().getCode());

        assertThat(
                reportMock.getServeEvents().getServeEvents()
                        .stream()
                        .filter(
                                se -> se.getRequest()
                                        .queryParameter("place")
                                        .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId())
                        )
                        .collect(Collectors.toList()),
                hasSize(equalTo(0)));
    }

    private Parameters orderParameters(OrderItem... orderItems) {
        return createParameters(params -> {
            params.getOrder().setItems(asList(orderItems));
            //предустановленая опция будет мешать
            params.getOrder().setDelivery(DeliveryProvider.yandexDelivery()
                    .serviceId(MOCK_DELIVERY_SERVICE_ID)
                    .price(DELIVERY_PRICE)
                    .nextDays(2)
                    .build());
            params.setupFulfillment(new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU));

            params.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
            params.setDeliveryType(DeliveryType.DELIVERY);
            params.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
            params.getReportParameters().setActualDelivery(
                    ActualDeliveryProvider.builder().addDelivery(MOCK_DELIVERY_SERVICE_ID, SHIPMENT_DAY).build()
            );
        });
    }

    private Parameters createParameters(Consumer<Parameters> configurer) {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setColor(BLUE);
        parameters.getOrder().setRgb(BLUE);
        parameters.setShopId(SHOP_ID_WITH_SORTING_CENTER);

        if (configurer != null) {
            configurer.accept(parameters);
        }

        fulfillmentConfigurer.configure(parameters, true);
        return parameters;
    }
}
