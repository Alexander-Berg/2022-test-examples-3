package ru.yandex.market.logistics.lom.jobs.processor.strategy.recalculatedroute;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.entity.ChangeOrderRequest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.DeliveryInterval;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.model.dto.RecalculatedOrderDatesPayloadDto;
import ru.yandex.market.logistics.lom.utils.WaybillSegmentFactory;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

import static org.mockito.Mockito.when;

@DisplayName("Тесты PreDeliveryRddProcessingStrategy")
@ParametersAreNonnullByDefault
class PreDeliveryRddProcessingStrategyTest extends BaseRddProcessingStrategyTest {

    private PreDeliveryRddProcessingStrategy strategy;

    @BeforeEach
    private void setUp() {
        strategy = new PreDeliveryRddProcessingStrategy(
            changeOrderRequestService,
            deliveryIntervalConverter,
            featureProperties
        );
    }

    @Test
    @DisplayName("Тест применимости стратегии")
    void isApplicable() {
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        softly.assertThat(strategy.isApplicable(changeOrderRequest)).isTrue();
    }

    @Test
    @DisplayName("Стратегия неприменима, если причина не PRE_DELIVERY_ROUTE_RECALCULATION")
    void isNotApplicableIfReasonNotPreDeliveryRouteRecalculation() {
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        changeOrderRequest.setReason(ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_ROUTE_RECALCULATION);
        softly.assertThat(strategy.isApplicable(changeOrderRequest)).isFalse();
    }

    @Test
    @DisplayName("Cor в день РДД должен быть обработан")
    void shouldBeProcessedIsRddDay() {
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor за день до РДД должен быть обработан")
    void shouldBeProcessedBeforePdoDay() {
        mockFeatureProperties(null, true);
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(true, ServiceCodeName.SHIPMENT, changeOrderRequest, false);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(true);
        softly.assertThat(response).isEqualTo(expected);
    }


    @Test
    @DisplayName("Cor в день РДД не должен быть обработан, "
        + "если выключена настройка applyPreDeliveryRddRecalculationRddDay")
    void shouldNotBeProcessedIfFeatureDisabled() {
        mockFeatureProperties(false, null);
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Property applyPreDeliveryRddRecalculationRddDay is disabled");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor за день до РДД не должен быть обработан,"
        + " если выключена настройка applyPreDeliveryRddRecalculationBeforeRddDay")
    void shouldNotBeProcessedIfBeforeRddDayFeatureDisabled() {
        mockFeatureProperties(null, false);
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(true, ServiceCodeName.SHIPMENT, changeOrderRequest, false);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Property applyPreDeliveryRddRecalculationBeforeRddDay is disabled");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor не должен быть обработан, если у заказа нет нужного тега")
    void shouldNotBeProcessedIfOrderHasNoTag() {
        mockFeatureProperties(null, true);
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        changeOrderRequest.getOrder().setOrderTags(Set.of());
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(true, ServiceCodeName.SHIPMENT, changeOrderRequest, false);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Disabled. Order doesn't have DELAYED_RDD_NOTIFICATION tag");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor не должен быть обработан, если пересчитанная дата совпадает с актуальной для пользователя.")
    void shouldNotBeProcessedIfNewDateEqualActual() {
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        DeliveryInterval deliveryInterval = deliveryIntervalConverter.fromApi(datesPayload);
        changeOrderRequest.getOrder().setDeliveryInterval(deliveryInterval);
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("Final delivery data the same as actual");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor не должен быть обработан, если пересчитанная дата меньше чем актуальная для пользователя. "
        + "У заказа нет запросов на изменение заказа.")
    void shouldNotBeProcessedIfNewDateLessThanActualWithoutCor() {
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        DeliveryInterval deliveryInterval = new DeliveryInterval()
            .setDateMin(LocalDate.of(2022, 1, 25))
            .setDateMax(LocalDate.of(2022, 1, 26));
        changeOrderRequest.getOrder().setDeliveryInterval(deliveryInterval);
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest);

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
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest);

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
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder().build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected =
            RddProcessingStrategy.Response.reject("New deliveryDateMin less than actual for user");
        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Cor должен быть обработан, если пересчитанная дата совпадает с прошлой.")
    void shouldNotBeProcessedIfDateDidNotChange() {
        mockFeatureProperties();
        ChangeOrderRequest changeOrderRequest = mockChangeOrderRequest();
        RecalculatedOrderDatesPayloadDto datesPayload = new TestDatesPayloadDtoBuilder()
            .setOldDeliveryDateMin(LocalDate.of(2022, 1, 21))
            .setOldDeliveryDateMax(LocalDate.of(2022, 1, 22))
            .build();
        mockRecalculateOrderDeliveryDatePayload(changeOrderRequest);

        RddProcessingStrategy.Response response = strategy.shouldBeProcessed(changeOrderRequest, datesPayload);

        RddProcessingStrategy.Response expected = RddProcessingStrategy.Response.proceed(false);
        softly.assertThat(response).isEqualTo(expected);
    }

    @Nonnull
    private ChangeOrderRequest mockChangeOrderRequest() {
        WaybillSegment waybillSegment = new WaybillSegment();
        Order order = WaybillSegmentFactory.joinInOrder(List.of(waybillSegment));
        order.addOrderTag(OrderTag.DELAYED_RDD_NOTIFICATION);
        ChangeOrderRequest changeOrderRequest = new ChangeOrderRequest()
            .setStatus(ChangeOrderRequestStatus.CREATED)
            .setReason(ChangeOrderRequestReason.PRE_DELIVERY_ROUTE_RECALCULATION)
            .setOrder(order)
            .setWaybillSegment(waybillSegment);
        return changeOrderRequest;
    }

    private void mockFeatureProperties() {
        mockFeatureProperties(true, null);
    }

    private void mockFeatureProperties(@Nullable Boolean enabledPdoDay, @Nullable Boolean enabledBeforePdoDay) {
        if (enabledPdoDay != null) {
            when(featureProperties.isApplyPreDeliveryRddRecalculationRddDay()).thenReturn(enabledPdoDay);
        }
        if (enabledBeforePdoDay != null) {
            when(featureProperties.isApplyPreDeliveryRddRecalculationBeforeRddDay()).thenReturn(enabledBeforePdoDay);
        }
    }

    private void mockRecalculateOrderDeliveryDatePayload(ChangeOrderRequest changeOrderRequest) {
        mockRecalculateOrderDeliveryDatePayload(true, ServiceCodeName.SHIPMENT, changeOrderRequest, true);
    }
}
