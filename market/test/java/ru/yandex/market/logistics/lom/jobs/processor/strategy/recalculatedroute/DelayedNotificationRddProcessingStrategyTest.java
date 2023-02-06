package ru.yandex.market.logistics.lom.jobs.processor.strategy.recalculatedroute;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.entity.ChangeOrderRequest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.PartnerSettings;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.DeliveryInterval;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.model.dto.RecalculatedOrderDatesPayloadDto;
import ru.yandex.market.logistics.lom.utils.WaybillSegmentFactory;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Тесты DelayedNotificationRrdProcessingStrategy")
@ParametersAreNonnullByDefault
class DelayedNotificationRddProcessingStrategyTest extends BaseRddProcessingStrategyTest {

    private DelayedNotificationRddProcessingStrategy strategy;

    @BeforeEach
    private void setUp() {
        strategy = new DelayedNotificationRddProcessingStrategy(
            changeOrderRequestService,
            deliveryIntervalConverter,
            featureProperties
        );
    }

    @Test
    @DisplayName("Тест применимости стратегии")
    void isApplicable() {
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);
        softly.assertThat(strategy.isApplicable(changeOrderRequest)).isTrue();
    }

    @Test
    @DisplayName("Стратегия неприменима, если по cor нужно уведомлять пользователя")
    void isNotApplicableIfShouldNotifyUser() {
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        mockRecalculateOrderDeliveryDatePayload(true, ServiceCodeName.SHIPMENT, changeOrderRequest, null);
        softly.assertThat(strategy.isApplicable(changeOrderRequest)).isFalse();
    }

    @Test
    @DisplayName("Стратегия неприменима, если причина PRE_DELIVERY_ROUTE_RECALCULATION")
    void isNotApplicableIfReasonPreDeliveryRouteRecalculation() {
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        changeOrderRequest.setReason(ChangeOrderRequestReason.PRE_DELIVERY_ROUTE_RECALCULATION);
        verify(changeOrderRequestService, never()).getPayload(any(), any(), any());
        softly.assertThat(strategy.isApplicable(changeOrderRequest)).isFalse();
    }

    @Test
    @DisplayName("Cor ffShipment должен быть обработан")
    void ffShipmentCorShouldBeProcessed() {
        mockFeatureProperties(true, false, false);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor c приемкой после FF должен быть обработан")
    void intakeAfterFfCorShouldBeProcessed() {
        mockFeatureProperties(true, false, false);
        WaybillSegment segmentAfterFf = createOrder().getWaybill().get(1);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(segmentAfterFf);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.INBOUND);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor dropshipShipment должен быть обработан")
    void dropshipShipmentCorShouldBeProcessed() {
        mockFeatureProperties(false, true, false);
        WaybillSegment dropoffSegment = createOrder(true).getWaybill().get(1);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(dropoffSegment);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.INBOUND);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor scShipment должен быть обработан")
    void scShipmentCorShouldBeProcessed() {
        mockFeatureProperties(false, false, true);
        WaybillSegment scSegment = createOrder(true).getWaybill().get(1);
        scSegment.setPartnerSettings(PartnerSettings.builder().build()); //не дропофф
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(scSegment);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor doShipment должен быть обработан")
    void doShipmentCorShouldBeProcessed() {
        mockFeatureProperties(false, false, true);
        WaybillSegment dropoffSegment = createOrder(true).getWaybill().get(1);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(dropoffSegment);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor c приемкой после dropoff должен быть обработан")
    void intakeAfterScCorShouldBeProcessed() {
        mockFeatureProperties(false, false, true);
        WaybillSegment segmentAfterSc = createOrder(true).getWaybill().get(2);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(segmentAfterSc);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.INBOUND);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor с причиной PROCESSING_DELAYED_BY_PARTNER должен быть обработан")
    void shouldNotBeProcessedIfUnsupportedStatus() {
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0))
            .setReason(ChangeOrderRequestReason.PROCESSING_DELAYED_BY_PARTNER);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor не должен быть обработан, если у заказа нет нужного тега")
    void shouldNotBeProcessedIfOrderHasNoTag() {
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        changeOrderRequest.getOrder().setOrderTags(Set.of());
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Disabled. Order doesn't have DELAYED_RDD_NOTIFICATION tag");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor ffShipment с сервисом INBOUND не должен быть обработан")
    void ffShipmentCorWithInStatusShouldNotBeProcessed() {
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.INBOUND);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Disabled. Unknown RDD recalculation type.");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor dropshipShipment с сервисом SHIPMENT не должен быть обработан")
    void dropshipShipmentCorWithOutStatusShouldNotBeProcessed() {
        mockFeatureProperties(false, true, false);
        WaybillSegment dropoffSegment = createOrder(true).getWaybill().get(1);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(dropoffSegment);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Property applyScDoShipmentRddRecalculation is disabled");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor scShipment с сервисом INBOUND не должен быть обработан")
    void scShipmentCorWithInStatusShouldNotBeProcessed() {
        mockFeatureProperties(false, false, true);
        WaybillSegment scSegment = createOrder(true).getWaybill().get(1);
        scSegment.setPartnerSettings(PartnerSettings.builder().build()); //не дропофф
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(scSegment);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.INBOUND);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Property applyDropshipShipmentRddRecalculation is disabled");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor doShipment не должен быть обработан если тип сегмента не СЦ")
    void doShipmentCorShouldNotBeProcessedIfSegmentTypeNotSc() {
        mockFeatureProperties();
        WaybillSegment dropoffSegment = createOrder(true).getWaybill().get(1);
        dropoffSegment.setSegmentType(SegmentType.PICKUP);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(dropoffSegment);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Disabled. Unknown RDD recalculation type.");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor не должен быть обработан, если пересчитанная дата меньше чем актуальная для пользователя. "
        + "У заказа нет запросов на изменение заказа.")
    void shouldNotBeProcessedIfNewDateLessThanActualWithoutCor() {
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);
        DeliveryInterval deliveryInterval = new DeliveryInterval()
            .setDateMin(LocalDate.of(2022, 1, 25))
            .setDateMax(LocalDate.of(2022, 1, 26));
        changeOrderRequest.getOrder().setDeliveryInterval(deliveryInterval);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("New deliveryDateMin less than actual for user");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor не должен быть обработан, если пересчитанная дата меньше чем актуальная для пользователя. "
        + "У заказа есть видимые пользователю запросы на изменение заказа.")
    void shouldNotBeProcessedIfNewDateLessThanActualWithVisibleCor() {
        mockOrderDeliveryDateCor();
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("New deliveryDateMin less than actual for user");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor не должен быть обработан, если пересчитанная дата меньше чем актуальная для пользователя. "
        + "У заказа есть только не видимые пользователю запросы на изменение заказа.")
    void shouldNotBeProcessedIfNewDateLessThanActualWithInvisibleCor() {
        mockOrderRecalculateRddCorWithoutUserNotification();
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("New deliveryDateMin less than actual for user");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor не должен быть обработан, если пересчитанная дата совпадает с прошлой. "
        + "И настройка updateRouteIfDeliveryIntervalDidNotChange выключена")
    void shouldNotBeProcessedIfDateDidNotChangeAndPropertyDisabled() {
        mockFeatureProperties();
        when(featureProperties.isUpdateRouteIfDeliveryIntervalDidNotChange()).thenReturn(false);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder()
            .setOldDeliveryDateMin(LocalDate.of(2022, 1, 21))
            .setOldDeliveryDateMax(LocalDate.of(2022, 1, 22))
            .build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Delivery dates and delivery interval did not change");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor должен быть обработан, если пересчитанная дата совпадает с прошлой. "
        + "И настройка updateRouteIfDeliveryIntervalDidNotChange включена")
    void shouldNotBeProcessedIfDateDidNotChangeAndPropertyEnabled() {
        mockFeatureProperties();
        when(featureProperties.isUpdateRouteIfDeliveryIntervalDidNotChange()).thenReturn(true);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder()
            .setOldDeliveryDateMin(LocalDate.of(2022, 1, 21))
            .setOldDeliveryDateMax(LocalDate.of(2022, 1, 22))
            .build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(false);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor ffShipment не должен быть обработан если отключена настройка")
    void ffShipmentCorShouldNotBeProcessedIfFeatureDisabled() {
        mockFeatureProperties(false, false, false);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(createOrder().getWaybill().get(0));
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Property applyFfShipmentRddRecalculation is disabled");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor dropshipShipment не должен быть обработан если отключена настройка")
    void dropshipShipmentCorShouldNotBeProcessedIfFeatureDisabled() {
        mockFeatureProperties(true, false, true);
        WaybillSegment dropoffSegment = createOrder(true).getWaybill().get(1);
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(dropoffSegment);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.INBOUND);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Property applyDropshipShipmentRddRecalculation is disabled");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor scShipment не должен быть обработан если отключена настройка")
    void scShipmentCorShouldNotBeProcessedIfFeatureDisabled() {
        mockFeatureProperties(true, true, false);
        WaybillSegment scSegment = createOrder(true).getWaybill().get(1);
        scSegment.setPartnerSettings(PartnerSettings.builder().build()); //не дропофф
        ChangeOrderRequest changeOrderRequest = createChangeOrderRequest(scSegment);
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest, ServiceCodeName.SHIPMENT);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Property applyScDoShipmentRddRecalculation is disabled");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Nonnull
    Order createOrder() {
        return createOrder(false);
    }

    @Nonnull
    Order createOrder(boolean dropshipOrder) {
        WaybillSegment ffSegment = new WaybillSegment()
            .setPartnerType(PartnerType.FULFILLMENT)
            .setSegmentType(SegmentType.FULFILLMENT);
        WaybillSegment scSegment = new WaybillSegment()
            .setPartnerType(PartnerType.SORTING_CENTER)
            .setSegmentType(SegmentType.SORTING_CENTER);
        WaybillSegment deliverySegment = new WaybillSegment().setPartnerType(PartnerType.DELIVERY);
        if (dropshipOrder) {
            ffSegment.setPartnerType(PartnerType.DROPSHIP);
            scSegment.setPartnerSettings(PartnerSettings.builder().dropoff(true).build());
        }
        Order order = WaybillSegmentFactory.joinInOrder(List.of(ffSegment, scSegment, deliverySegment));
        order.addOrderTag(OrderTag.DELAYED_RDD_NOTIFICATION);
        return order;
    }

    @Nonnull
    private ChangeOrderRequest createChangeOrderRequest(WaybillSegment waybillSegment) {
        return new ChangeOrderRequest()
            .setReason(ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_ROUTE_RECALCULATION)
            .setStatus(ChangeOrderRequestStatus.CREATED)
            .setOrder(waybillSegment.getOrder())
            .setWaybillSegment(waybillSegment);
    }

    private void mockFeatureProperties() {
        mockFeatureProperties(true, true, true);
    }

    private void mockFeatureProperties(boolean ffShipment, boolean dropshipShipment, boolean scDoShipment) {
        lenient().when(featureProperties.isApplyFfShipmentRddRecalculation()).thenReturn(ffShipment);
        lenient().when(featureProperties.isApplyDropshipShipmentRddRecalculation()).thenReturn(dropshipShipment);
        lenient().when(featureProperties.isApplyScDoShipmentRddRecalculation()).thenReturn(scDoShipment);
    }

    private void mockRecalculateOrderDeliveryDatePayload(
        ChangeOrderRequest changeOrderRequest,
        ServiceCodeName serviceCodeName
    ) {
        mockRecalculateOrderDeliveryDatePayload(false, serviceCodeName, changeOrderRequest, null);
    }
}
