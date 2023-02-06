package ru.yandex.market.antifraud.orders.detector;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.detector.v2.PostPayFraudDetectorImpl;
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.storage.entity.rules.DetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.v2.ExperimentAwareDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.v2.PostPayFraudDetectorConfiguration;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryType;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderPaymentFullInfoDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderPaymentType;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class PostPayFraudDetectorTest {
    private static final long BUYER_UID = 123;
    private static final long GLUED_UID = 124;
    private static final String NO_EXPERIMENTS = "";
    private static final int GENERAL_CATEGORY = 0;
    private static final int LIQUID_CATEGORY = 1024;
    private static final String FOR_ANY_ITEM = "for_any_item";
    private static final String FOR_LIQUID_CATEGORY = "for_liquid_category";
    private static final OrderDeliveryRequestDto DELIVERY = OrderDeliveryRequestDto.builder().type(OrderDeliveryType.DELIVERY).build();
    private static final OrderDeliveryRequestDto PICKUP_1 = OrderDeliveryRequestDto.builder().type(OrderDeliveryType.PICKUP).outletId(1L).build();
    private static final OrderDeliveryRequestDto PICKUP_2 = OrderDeliveryRequestDto.builder().type(OrderDeliveryType.PICKUP).outletId(2L).build();

    public static final ExperimentAwareDetectorConfiguration<PostPayFraudDetectorConfiguration> MOCK_CONFIG =
        new ExperimentAwareDetectorConfiguration<>(true,
            PostPayFraudDetectorConfiguration.builder()
                .detectionThreshold(OptionalDouble.empty())
                .build(),
            Map.of(
                FOR_ANY_ITEM, PostPayFraudDetectorConfiguration.builder()
                    .detectionThreshold(OptionalDouble.of(10_000))
                    .build(),
                FOR_LIQUID_CATEGORY, PostPayFraudDetectorConfiguration.builder()
                    .detectionThreshold(OptionalDouble.of(10_000))
                    .categoryHid(LIQUID_CATEGORY)
                    .categoryDescription("в категории")
                    .build()));

    private PostPayFraudDetector detector;

    @Before
    public void setUp() {
        detector = new PostPayFraudDetector(new PostPayFraudDetectorImpl());
    }

    @Test
    public void noTriggeringWithoutExperiments() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                PICKUP_1,
                OrderPaymentType.POSTPAID,
                NO_EXPERIMENTS,
                getOrderItemRequest(GENERAL_CATEGORY, 7_000),
                getOrderItemRequest(LIQUID_CATEGORY, 7_000)),
            getOrder(BUYER_UID, "DELIVERY")
        );
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void noTriggeringCourier() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                DELIVERY,
                OrderPaymentType.POSTPAID,
                FOR_ANY_ITEM,
                getOrderItemRequest(GENERAL_CATEGORY, 7_000),
                getOrderItemRequest(LIQUID_CATEGORY, 7_000)),
            getOrder(BUYER_UID, "DELIVERY")
        );
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void noTriggeringPrepaid() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                PICKUP_1,
                OrderPaymentType.PREPAID,
                FOR_ANY_ITEM,
                getOrderItemRequest(GENERAL_CATEGORY, 7_000),
                getOrderItemRequest(LIQUID_CATEGORY, 7_000)),
            getOrder(BUYER_UID, "DELIVERY")
        );
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void noTriggeringForGeneralCategoryWithSmallAmount() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                PICKUP_1,
                OrderPaymentType.POSTPAID,
                FOR_ANY_ITEM,
                getOrderItemRequest(GENERAL_CATEGORY, 7_000)),
            getOrder(BUYER_UID, "DELIVERY")
        );
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void noTriggeringWithDeliveredOrder() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                PICKUP_1,
                OrderPaymentType.POSTPAID,
                FOR_ANY_ITEM,
                getOrderItemRequest(GENERAL_CATEGORY, 7_000),
                getOrderItemRequest(LIQUID_CATEGORY, 7_000)),
            getOrder(BUYER_UID, "DELIVERED")
        );
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void triggeringWithDeliveredOrderInGlue() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                PICKUP_1,
                OrderPaymentType.POSTPAID,
                FOR_ANY_ITEM,
                getOrderItemRequest(GENERAL_CATEGORY, 7_000),
                getOrderItemRequest(LIQUID_CATEGORY, 7_000)),
            getOrder(BUYER_UID, "DELIVERY"),
            getOrder(GLUED_UID, "DELIVERED")
        );
        assertThat(odr.isFraud()).isTrue();
        assertThat(odr.getActions()).contains(AntifraudAction.PREPAID_ONLY);
        assertThat(odr.getReason()).isEqualTo("Нет выполненных заказов, заказ на ПВЗ, постоплата, сумма больше лимита: 14000. Эксперимент: for_any_item");
    }

    @Test
    public void triggeringForGeneralCategory() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                PICKUP_1,
                OrderPaymentType.POSTPAID,
                FOR_ANY_ITEM,
                getOrderItemRequest(GENERAL_CATEGORY, 7_000),
                getOrderItemRequest(LIQUID_CATEGORY, 7_000)),
            getOrder(BUYER_UID, "DELIVERY")
        );
        assertThat(odr.isFraud()).isTrue();
        assertThat(odr.getActions()).contains(AntifraudAction.PREPAID_ONLY);
        assertThat(odr.getReason()).isEqualTo("Нет выполненных заказов, заказ на ПВЗ, постоплата, сумма больше лимита: 14000. Эксперимент: for_any_item");
    }

    @Test
    public void noTriggeringForLiquidCategory() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                PICKUP_1,
                OrderPaymentType.POSTPAID,
                FOR_LIQUID_CATEGORY,
                getOrderItemRequest(GENERAL_CATEGORY, 7_000),
                getOrderItemRequest(LIQUID_CATEGORY, 7_000)),
            getOrder(BUYER_UID, "DELIVERY")
        );
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void triggeringForLiquidCategory() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                PICKUP_1,
                OrderPaymentType.POSTPAID,
                FOR_LIQUID_CATEGORY,
                getOrderItemRequest(LIQUID_CATEGORY, 15_000)),
            getOrder(BUYER_UID, "DELIVERY")
        );
        assertThat(odr.isFraud()).isTrue();
        assertThat(odr.getActions()).contains(AntifraudAction.PREPAID_ONLY);
        assertThat(odr.getReason()).isEqualTo("Нет выполненных заказов, заказ на ПВЗ, постоплата, сумма больше лимита: 15000, в категории. Эксперимент: for_liquid_category");
    }

    @Test
    public void triggeringForLiquidCategoryWithMultiorder() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                OrderPaymentType.POSTPAID,
                FOR_LIQUID_CATEGORY,
                getCart(
                    PICKUP_1,
                    getOrderItemRequest(LIQUID_CATEGORY, 6_000)
                ),
                getCart(
                    PICKUP_2,
                    getOrderItemRequest(LIQUID_CATEGORY, 6_000)
                ),
                getCart(
                    DELIVERY,
                    getOrderItemRequest(LIQUID_CATEGORY, 6_000)
                ),
                getCart(
                    PICKUP_1,
                    getOrderItemRequest(GENERAL_CATEGORY, 6_000)
                ),
                getCart(
                    PICKUP_1,
                    getOrderItemRequest(LIQUID_CATEGORY, 6_000)
                )
            ),
            getOrder(BUYER_UID, "DELIVERY")
        );
        assertThat(odr.isFraud()).isTrue();
        assertThat(odr.getActions()).contains(AntifraudAction.PREPAID_ONLY);
        assertThat(odr.getReason()).isEqualTo("Нет выполненных заказов, заказ на ПВЗ, постоплата, сумма больше лимита: 12000, в категории. Эксперимент: for_liquid_category");
    }

    @Test
    public void configDeserialize() {
        var json = AntifraudJsonUtil.toJson(MOCK_CONFIG);
        System.out.println(json);
        var configuration = AntifraudJsonUtil.fromJson(json, DetectorConfiguration.class);
        assertThat(configuration).isEqualTo(MOCK_CONFIG);
    }

    @Test
    public void configDeserializeOld() {
        var json = "{\"_class\":\"ru.yandex.market.antifraud.orders.storage.entity.rules.PostPayFraudDetectorConfiguration\",\"enabled\":true," +
            "\"experiment\":false,\"experimentsConfig\":[{\"expName\":\"for_any_item\",\"minOrderAmount\":10000,\"categoryHids\":[]}," +
            "{\"expName\":\"for_liquid_category\",\"minOrderAmount\":10000,\"categoryDescription\":\"в категории\",\"categoryHids\":[1024]}]," +
            "\"createdAt\":\"2021-08-12T14:55:55.553192Z\"}";
        var orderRequest = getOrderRequest(
            PICKUP_1,
            OrderPaymentType.POSTPAID,
            NO_EXPERIMENTS,
            getOrderItemRequest(GENERAL_CATEGORY, 31_000));
        var buyerContext = BuyerRole.builder()
            .detectorConfigurations(Map.of(detector.getUniqName(), AntifraudJsonUtil.fromJson(json, DetectorConfiguration.class)))
            .build();
        var odr = detector.detectFraud(
            OrderDataContainer.builder()
                .orderRequest(orderRequest)
                .lastOrdersFuture(new FutureValueHolder<>(List.of(getOrder(BUYER_UID, "DELIVERY"))))
                .build(),
            buyerContext);
        assertThat(odr.get().isFraud()).isTrue();
    }

    @Test
    public void configDeserialize2() {
        var json = "{\"_class\":\"ru.yandex.market.antifraud.orders.storage.entity.rules.v2.ExperimentAwareDetectorConfiguration\",\"enabled\":true,\"experiment\":false,\"defaultConfig\":{\"@c\":\".PostPayFraudDetectorConfiguration\"," +
            "\"categoryHids\":[]},\"experimentsConfig\":{\"for_liquid_category\":{\"@c\":\".PostPayFraudDetectorConfiguration\",\"detectionThreshold\":10000.0,\"categoryDescription\":\"в категории\",\"categoryHids\":[1024]}," +
            "\"for_any_item\":{\"@c\":\".PostPayFraudDetectorConfiguration\",\"detectionThreshold\":10000.0,\"categoryHids\":[]}},\"changedAt\":\"2022-01-06T10:19:33.816446Z\"}\n";
        assertThatCode(() -> AntifraudJsonUtil.fromJson(json, DetectorConfiguration.class))
            .doesNotThrowAnyException();
    }

    private OrderDetectorResult runDetector(MultiCartRequestDto orderRequest, Order... lastOrders) {
        return detector.detectFraud(
            OrderDataContainer.builder()
                .orderRequest(orderRequest)
                .lastOrdersFuture(new FutureValueHolder<>(List.of(lastOrders)))
                .build(),
            MOCK_CONFIG);
    }

    private long lastOrderIndex = 1;

    private MultiCartRequestDto getOrderRequest(OrderDeliveryRequestDto delivery,
                                                OrderPaymentType paymentType,
                                                String experiments,
                                                OrderItemRequestDto... orderItems) {
        return getOrderRequest(paymentType, experiments, getCart(delivery, orderItems));
    }

    private MultiCartRequestDto getOrderRequest(OrderPaymentType paymentType,
                                                String experiments,
                                                CartRequestDto... carts) {
        return MultiCartRequestDto.builder()
            .checkout(true)
            .buyer(OrderBuyerRequestDto.builder()
                .uid(BUYER_UID)
                .uuid(String.valueOf(BUYER_UID))
                .build())
            .paymentFullInfo(OrderPaymentFullInfoDto.builder().orderPaymentType(paymentType).build())
            .experiments(experiments)
            .carts(List.of(carts))
            .build();
    }

    private CartRequestDto getCart(OrderDeliveryRequestDto delivery,
                                   OrderItemRequestDto... orderItems) {
        return CartRequestDto.builder()
            .delivery(delivery)
            .items(List.of(orderItems))
            .build();
    }

    private OrderItemRequestDto getOrderItemRequest(int categoryId, long price) {
        return OrderItemRequestDto.builder()
            .categoryId(categoryId)
            .count(1)
            .price(BigDecimal.valueOf(price))
            .build();
    }

    private Order getOrder(Long uid, String status) {
        return Order.newBuilder()
            .setKeyUid(Uid.newBuilder()
                .setType(UidType.PUID)
                .setStringValue(String.valueOf(uid))
                .build())
            .addItems(OrderItem.newBuilder()
                .setId(1)
                .setCount(1)
                .setPrice(12300000)
                .build())
            .setRgb(RGBType.BLUE)
            .setStatus(status)
            .setId(lastOrderIndex++)
            .build();
    }
}
