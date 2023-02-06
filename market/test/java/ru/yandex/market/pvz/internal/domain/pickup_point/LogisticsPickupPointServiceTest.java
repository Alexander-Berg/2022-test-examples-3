package ru.yandex.market.pvz.internal.domain.pickup_point;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.logistics.dto.CourierDsDayOffDto;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.config.MarketHubFfApiConfiguration;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.ApiSettingsCreator;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.DropOffCreateService;
import ru.yandex.market.pvz.core.domain.delivery_service.CourierDsDayOffCommandService;
import ru.yandex.market.pvz.core.domain.notification.MbiNotificationService;
import ru.yandex.market.pvz.core.domain.order.OrderCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderExpirationDateService;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointLmsMonitoringHistoryRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointSyncService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLogRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.deactivation.mapper.DeactivationDtoMapper;
import ru.yandex.market.sc.internal.client.ScLogisticsClient;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParams.DEFAULT_COURIER_DELIVERY_SERVICE_ID;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LogisticsPickupPointServiceTest {

    private static final String REASON = "Причина";
    private static final String DETAILS = "Описание причины";
    private static final String LOGISTICS_REASON = "UNPROFITABLE";
    public static final long COURIER_DELIVERY_SERVICE_ID = 1L;
    public static final LocalDate DAY_OFF = LocalDate.of(2021, 2, 8);

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final PickupPointLmsMonitoringHistoryRepository lmsMonitoringHistoryRepository;
    private final PickupPointCommandService pickupPointCommandService;
    private final PickupPointDeactivationLogRepository pickupPointDeactivationLogRepository;
    private final PickupPointDeactivationCommandService deactivationCommandService;
    private final PickupPointRepository pickupPointRepository;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final PickupPointSyncService pickupPointSyncService;
    private final TestableClock clock;

    private LogisticsPickupPointService logisticsPickupPointService;

    private final CourierDsDayOffCommandService courierDsDayOffCommandService;
    private final PickupPointQueryService pickupPointQueryService;
    private final PickupPointDeactivationCommandService pickupPointDeactivationCommandService;
    private final OrderQueryService orderQueryService;
    private final OrderCommandService orderCommandService;
    private final OrderExpirationDateService expirationDateService;
    private final MbiNotificationService mbiNotificationService;
    private final DeactivationReasonQueryService deactivationReasonQueryService;
    private final DeactivationDtoMapper deactivationDtoMapper;

    @Mock
    private MarketHubFfApiConfiguration marketHubFfApiConfiguration;

    @Mock
    private ApiSettingsCreator apiSettingsCreator;

    @Mock
    private ScLogisticsClient scLogisticsClient;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-12-05T12:00:00Z"), ZoneOffset.ofHours(3));

        when(marketHubFfApiConfiguration.getCreateOrder()).thenReturn("createOrder");
        when(marketHubFfApiConfiguration.getCancelOrder()).thenReturn("cancelOrder");
        when(marketHubFfApiConfiguration.getGetOrderHistory()).thenReturn("getOrderHistory");
        when(marketHubFfApiConfiguration.getUpdateOrderItems()).thenReturn("updateOrderItems");
        when(marketHubFfApiConfiguration.getUpdateOrder()).thenReturn("updateOrder");
        when(marketHubFfApiConfiguration.getGetOrdersStatus()).thenReturn("getOrdersStatus");
        when(marketHubFfApiConfiguration.getCreateReturnRegister()).thenReturn("getCreateReturnRegister");

        DropOffCreateService mockedDropOffCreateService = new DropOffCreateService(
                marketHubFfApiConfiguration, apiSettingsCreator, pickupPointCommandService, scLogisticsClient);

        logisticsPickupPointService = new LogisticsPickupPointService(
                courierDsDayOffCommandService, pickupPointQueryService, mockedDropOffCreateService,
                pickupPointCommandService, pickupPointDeactivationCommandService, orderQueryService,
                orderCommandService, expirationDateService, mbiNotificationService,
                deactivationReasonQueryService, deactivationDtoMapper, pickupPointSyncService
        );
    }


    @Test
    void createDayOff() {
        CourierDsDayOffDto dto = new CourierDsDayOffDto(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);
        CourierDsDayOffDto actual = logisticsPickupPointService.createDayOff(dto);

        CourierDsDayOffDto expected = new CourierDsDayOffDto(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void deleteDayOff() {
        logisticsPickupPointService.createDayOff(new CourierDsDayOffDto(COURIER_DELIVERY_SERVICE_ID, DAY_OFF));

        boolean deleted = logisticsPickupPointService.deleteDayOff(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);

        assertThat(deleted).isTrue();
    }

    @Test
    void deleteNotExistentDayOff() {
        boolean deleted = logisticsPickupPointService.deleteDayOff(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);

        assertThat(deleted).isFalse();
    }


    @Test
    void processRegionMonitoring() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order =
                orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
        order = orderFactory.receiveOrder(order.getId());
        assertThat(order.getRecipientName()).isNotNull();
        var externalIds = List.of(order.getExternalId());
        logisticsPickupPointService.processRegionMonitoring(pickupPoint.getLmsId(), externalIds);

        var pickupPointLmsMonitoringHistory = lmsMonitoringHistoryRepository.findByLmsIdAndBuyerName(
                pickupPoint.getLmsId(), order.getRecipientName()).get();

        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("lmsId", pickupPoint.getLmsId());
        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("externalIds", externalIds);
        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("buyerName", order.getRecipientName());
        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("operatorNotificationSent", true);
    }

    @Test
    void processRegionMonitoringWithoutMarketShopId() {
        var pickupPointParams = TestPickupPointFactory.PickupPointTestParams.builder().marketShopId(null).build();
        var builder = TestPickupPointFactory.CreatePickupPointBuilder.builder().params(pickupPointParams).build();
        var pickupPoint = pickupPointFactory.createPickupPoint(builder);

        var orderBuilder = TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build();
        var order =
                orderFactory.createOrder(orderBuilder);

        order = orderFactory.receiveOrder(order.getId());
        assertThat(order.getRecipientName()).isNotNull();
        var externalIds = List.of(order.getExternalId());
        logisticsPickupPointService.processRegionMonitoring(pickupPoint.getLmsId(), externalIds);

        var pickupPointLmsMonitoringHistory = lmsMonitoringHistoryRepository.findByLmsIdAndBuyerName(
                pickupPoint.getLmsId(), order.getRecipientName()).get();

        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("lmsId", pickupPoint.getLmsId());
        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("externalIds", externalIds);
        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("operatorNotificationSent", false);
    }

    @Test
    void processRegionMonitoringWithoutOrderParams() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order =
                orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
        order = orderFactory.receiveOrder(order.getId());
        assertThat(order.getRecipientName()).isNotNull();
        var externalIds = List.of("12344", "1235123");
        logisticsPickupPointService.processRegionMonitoring(pickupPoint.getLmsId(), externalIds);

        var pickupPointLmsMonitoringHistory = lmsMonitoringHistoryRepository.findByLmsId(
                pickupPoint.getLmsId());

        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("lmsId", pickupPoint.getLmsId());
        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("externalIds", externalIds);
        assertThat(pickupPointLmsMonitoringHistory).hasFieldOrPropertyWithValue("operatorNotificationSent", false);

    }

    @Test
    void createDropOff() {
        var pickupPoint = createPickupPointFullActive();
        checkFullActivePickupPoint(pickupPoint);

        logisticsPickupPointService.createDropOff(pickupPoint.getId(), DEFAULT_COURIER_DELIVERY_SERVICE_ID);
        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void createDropOffUnknownLmsId() {
        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .lmsId(null)
                        .prepayAllowed(true)
                        .active(true)
                        .build());
        assertThatThrownBy(() -> logisticsPickupPointService.createDropOff(pickupPoint.getId(),
                DEFAULT_COURIER_DELIVERY_SERVICE_ID))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void createDropOffWhenPickupPointNotFullDisabled() {
        var pickupPoint = createPickupPointFullActive();
        checkFullActivePickupPoint(pickupPoint);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                REASON, DETAILS, false, true, null
        );
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason.getId(), LocalDate.now(clock));
        checkMainFlagsDeactivated(pickupPoint);
        var actual = pickupPointRepository.findByPvzMarketIdOrThrow(pvzMarketId);
        assertThat(actual.getDropOffFeature()).isFalse();
        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        assertThat(deactivation.getDetails().getDropOffFeature()).isFalse();

        logisticsPickupPointService.createDropOff(pickupPoint.getId(), DEFAULT_COURIER_DELIVERY_SERVICE_ID);
        checkNotFullDeactivatedPickupPointWithDropOff(pickupPoint);

        deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        assertThat(deactivation.getDetails().getDropOffFeature()).isTrue();
    }

    @Test
    void createDropOffWhenPickupPointLogisticDisabled() {
        var pickupPoint = createPickupPointFullActive();
        checkFullActivePickupPoint(pickupPoint);
        logisticsPickupPointService.createDropOff(pickupPoint.getId(), DEFAULT_COURIER_DELIVERY_SERVICE_ID);
        checkFullActivePickupPointWithDropOff(pickupPoint);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                REASON, DETAILS, true, true, LOGISTICS_REASON
        );
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason.getId(), LocalDate.now(clock));
        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);

        logisticsPickupPointService.createDropOff(pickupPoint.getId(), DEFAULT_COURIER_DELIVERY_SERVICE_ID);
        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void notCreateDropOffWhenAppliedFullDeactivation() {
        var pickupPoint = createPickupPointFullActive();
        checkFullActivePickupPoint(pickupPoint);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                REASON, DETAILS, true, true, null
        );
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason.getId(), LocalDate.now(clock));
        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);

        logisticsPickupPointService.createDropOff(pickupPoint.getId(), DEFAULT_COURIER_DELIVERY_SERVICE_ID);
        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);
    }

    @Test
    void createDropOffWhenNotAppliedFullDeactivation() {
        var pickupPoint = createPickupPointFullActive();
        checkFullActivePickupPoint(pickupPoint);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                REASON, DETAILS, true, true, null
        );
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(
                pvzMarketId, deactivationReason.getId(), LocalDate.now(clock).plusDays(3)
        );
        checkFullActivePickupPoint(pickupPoint);

        logisticsPickupPointService.createDropOff(pickupPoint.getId(), DEFAULT_COURIER_DELIVERY_SERVICE_ID);
        checkFullActivePickupPointWithDropOff(pickupPoint);

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        assertThat(deactivation.getDetails().getDropOffFeature()).isTrue();
    }

    @Test
    void cancelNotAppliedLogisticDeactivation() {
        var pickupPoint = createPickupPointFullActive();
        logisticsPickupPointService.createDropOff(pickupPoint.getId(), DEFAULT_COURIER_DELIVERY_SERVICE_ID);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                REASON, DETAILS, true, true, LOGISTICS_REASON
        );
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(
                pvzMarketId, deactivationReason.getId(), LocalDate.now(clock).plusDays(3)
        );
        checkFullActivePickupPointWithDropOff(pickupPoint);
        var deactivations = pickupPointDeactivationLogRepository
                .findAllByPickupPointIdAndCancelledIsFalseAndActivationAppliedAtIsNull(pickupPoint.getId());
        assertThat(deactivations).hasSize(1);

        logisticsPickupPointService.createDropOff(pickupPoint.getId(), DEFAULT_COURIER_DELIVERY_SERVICE_ID);
        deactivations = pickupPointDeactivationLogRepository
                .findAllByPickupPointIdAndCancelledIsFalseAndActivationAppliedAtIsNull(pickupPoint.getId());
        assertThat(deactivations).hasSize(0);
    }

    private PickupPoint createPickupPointFullActive() {
        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .cardAllowed(true)
                        .cashAllowed(true)
                        .returnAllowed(true)
                        .prepayAllowed(true)
                        .active(true)
                        .build());
        cancelFirstDeactivation(pickupPoint.getPvzMarketId());
        return pickupPoint;
    }

    private void cancelFirstDeactivation(Long pvzMarketId) {
        var deactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
    }

    private PickupPoint checkFullActivePickupPoint(PickupPoint pickupPoint) {
        var actual = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(actual.getActive()).isTrue();
        assertThat(actual.getReturnAllowed()).isTrue();
        assertThat(actual.getCardAllowed()).isTrue();
        assertThat(actual.getCashAllowed()).isTrue();
        assertThat(actual.getPrepayAllowed()).isTrue();
        return actual;
    }

    private void checkFullActivePickupPointWithDropOff(PickupPoint pickupPoint) {
        var actual = checkFullActivePickupPoint(pickupPoint);
        assertThat(actual.getDropOffFeature()).isTrue();
    }

    private void checkFullDeactivatedPickupPointWithDropOff(PickupPoint pickupPoint) {
        var actual = checkFullDeactivatedPickupPoint(pickupPoint);
        assertThat(actual.getDropOffFeature()).isFalse();
    }

    private PickupPoint checkFullDeactivatedPickupPoint(PickupPoint pickupPoint) {
        var actual = checkMainFlagsDeactivated(pickupPoint);
        assertThat(actual.getActive()).isFalse();
        return actual;
    }

    private void checkNotFullDeactivatedPickupPointWithDropOff(PickupPoint pickupPoint) {
        var actual = checkMainFlagsDeactivated(pickupPoint);
        assertThat(actual.getActive()).isTrue();
        assertThat(actual.getDropOffFeature()).isTrue();
    }

    private PickupPoint checkMainFlagsDeactivated(PickupPoint pickupPoint) {
        var actual = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(actual.getReturnAllowed()).isFalse();
        assertThat(actual.getCardAllowed()).isFalse();
        assertThat(actual.getCashAllowed()).isFalse();
        assertThat(actual.getPrepayAllowed()).isFalse();
        return actual;
    }

}
