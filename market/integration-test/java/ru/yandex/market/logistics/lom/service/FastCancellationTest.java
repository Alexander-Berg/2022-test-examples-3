package ru.yandex.market.logistics.lom.service;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.lom.configuration.properties.OrderCancellationProperties;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.WaybillSegmentStatusHistory;
import ru.yandex.market.logistics.lom.entity.enums.CancellationSegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentStatusWithCancellationService;
import ru.yandex.market.logistics.lom.logging.enums.OrderEventCode;
import ru.yandex.market.logistics.lom.service.order.OrderCancellationService;
import ru.yandex.market.logistics.lom.service.order.OrderService;

@ParametersAreNonnullByDefault
@DisplayName("Поддержка быстрой отмены")
class FastCancellationTest extends AbstractExternalServiceTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderCancellationService orderCancellationService;

    private final TestService testService = new TestService(orderService, orderCancellationService);

    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus.class,
        names = {"TRANSIT_COURIER_RECEIVED", "TRANSIT_DELIVERY_AT_START_SORT", "TRANSIT_PICKUP", "OUT"}
    )
    @DisplayName("Не поддерживается для ондеманд заказов после получения чп")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    void notAllowedOnDemand(SegmentStatus status) {
        WaybillSegment waybillSegment = waybillSegment(status, WaybillSegmentTag.ON_DEMAND);
        softly.assertThat(testService.isFastCancellationInDsAllowed(waybillSegment)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus.class,
        names = {"TRANSIT_COURIER_RECEIVED", "TRANSIT_DELIVERY_AT_START_SORT", "TRANSIT_PICKUP", "OUT"}
    )
    @DisplayName("Не поддерживается для экспресс заказов после получения чп")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    void notAllowedExpress(SegmentStatus status) {
        WaybillSegment waybillSegment = waybillSegment(status, WaybillSegmentTag.CALL_COURIER);
        softly.assertThat(testService.isFastCancellationInDsAllowed(waybillSegment)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus.class,
        names = {"TRANSIT_COURIER_RECEIVED", "TRANSIT_DELIVERY_AT_START_SORT", "TRANSIT_PICKUP", "OUT"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Поддерживается для ондеманд заказов")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    void allowedOnDemand(SegmentStatus status) {
        WaybillSegment waybillSegment = waybillSegment(status, WaybillSegmentTag.ON_DEMAND);
        softly.assertThat(testService.isFastCancellationInDsAllowed(waybillSegment)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus.class,
        names = {"TRANSIT_COURIER_RECEIVED", "TRANSIT_DELIVERY_AT_START_SORT", "TRANSIT_PICKUP", "OUT"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Поддерживается для экспресс заказов")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    void allowedExpress(SegmentStatus status) {
        WaybillSegment waybillSegment = waybillSegment(status, WaybillSegmentTag.CALL_COURIER);
        softly.assertThat(testService.isFastCancellationInDsAllowed(waybillSegment)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = SegmentStatus.class)
    @DisplayName("Не поддерживается для обычных заказов, не все сегменты - delivery")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    void notAllowedNotAllDelivery(SegmentStatus status) {
        WaybillSegment waybillSegment = waybillSegment(status, null);
        waybillSegment.getOrder().addWaybillSegment(new WaybillSegment().setPartnerType(PartnerType.SORTING_CENTER));
        softly.assertThat(testService.isFastCancellationInDsAllowed(waybillSegment)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus.class,
        names = {"IN", "TRANSIT_DELIVERY_AT_START_SORT", "TRANSIT_PICKUP", "OUT"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Поддерживается для обычных заказов, все сегменты - delivery")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    void allowedAllDelivered(SegmentStatus status) {
        WaybillSegment waybillSegment = waybillSegment(status, null);
        softly.assertThat(testService.isFastCancellationInDsAllowed(waybillSegment)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus.class,
        names = {"IN", "TRANSIT_DELIVERY_AT_START_SORT", "TRANSIT_PICKUP", "OUT"}
    )
    @DisplayName("Не поддерживается для обычных заказов, у сегмента есть чп, после которого нельзя отменять")
    @DatabaseSetup("/service/process_delivery_date_updated_by_ds/before/setup.xml")
    void allowedAllDeliveredInvalidStatus(SegmentStatus status) {
        WaybillSegment waybillSegment = waybillSegment(status, null);
        softly.assertThat(testService.isFastCancellationInDsAllowed(waybillSegment)).isFalse();
    }

    @Nonnull
    private WaybillSegment waybillSegment(SegmentStatus status, @Nullable WaybillSegmentTag tag) {
        WaybillSegment waybillSegment = new WaybillSegment()
            .setPartnerType(PartnerType.DELIVERY)
            .addWaybillSegmentStatusHistory(new WaybillSegmentStatusHistory().setStatus(status), status);

        if (tag != null) {
            waybillSegment.addTag(tag);
        }
        Order order = new Order().setWaybill(List.of(waybillSegment));
        return waybillSegment;
    }

    @ParametersAreNonnullByDefault
    static class TestService extends ProcessSegmentStatusWithCancellationService {
        TestService(OrderService orderService, OrderCancellationService orderCancellationService) {
            super(
                orderService,
                orderCancellationService,
                OrderEventCode.PROCESS_SEGMENT_CANCELLATION,
                null,
                CancellationSegmentStatus.FAIL,
                new OrderCancellationProperties(),
                new TestableClock()
            );
        }

        @Override
        protected boolean isFastCancellationInDsAllowed(WaybillSegment processingSegment) {
            return super.isFastCancellationInDsAllowed(processingSegment);
        }
    }
}
