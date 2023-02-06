package ru.yandex.market.pvz.core.service.delivery.pickup_point;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistic.api.model.delivery.request.GetReferencePickupPointsRequest;
import ru.yandex.market.logistic.api.model.delivery.response.GetReferencePickupPointsResponse;
import ru.yandex.market.pvz.core.domain.calendar.OfficialHoliday;
import ru.yandex.market.pvz.core.domain.calendar.OfficialHolidaysManager;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.service.delivery.DsApiBaseTest;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.NEW_CAPACITY_CALCULATION_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGIONS_FOR_DEPENDENT_PARAMS;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_HEIGHT;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_LENGTH;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_WIDTH;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.CreatePickupPointBuilder;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointCalendarOverrideTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DsPickupPointManagerTest extends DsApiBaseTest {

    private final TestableClock clock;
    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final DsPickupPointManager dsPickupPointManager;
    private final PickupPointDeactivationCommandService deactivationCommandService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final OfficialHolidaysManager officialHolidaysManager;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @BeforeEach
    void setup() {
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_LENGTH, PickupPoint.REGION_SPECIFIC_MAX_LENGTH);
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_WIDTH, PickupPoint.REGION_SPECIFIC_MAX_WIDTH);
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_HEIGHT, PickupPoint.REGION_SPECIFIC_MAX_HEIGHT);
        configurationGlobalCommandService.setValue(
                REGIONS_FOR_DEPENDENT_PARAMS, PickupPoint.REGIONS_FOR_DEPENDENT_PARAMS);
        configurationGlobalCommandService.setValue(NEW_CAPACITY_CALCULATION_ENABLED, true);
    }

    @Test
    void getReferencePickupPoints() {
        DeliveryService deliveryService = getDeliveryService();
        LegalPartner legalPartner = getLegalPartner(deliveryService);

        PickupPoint pickupPoint = createPickupPoint(legalPartner, true);
        PickupPoint pickupPoint2 = createPickupPoint(legalPartner, false);
        PickupPoint pickupPoint3 = createPickupPoint(legalPartner, false);
        pickupPointFactory.createDropOff(pickupPoint3.getId());

        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime fixedTime = LocalDateTime.of(2020, 9, 1, 5, 0, 0);
        clock.setFixed(fixedTime.toInstant(zone), zone);

        createOrderWithDeliveryDate(LocalDate.now(clock), pickupPoint3);
        createOrderWithDeliveryDate(LocalDate.now(clock), pickupPoint3);

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        var request = readRequest("/ds/pickup_point/get_reference_pickup_points_request.xml",
                GetReferencePickupPointsRequest.class, Map.of());
        var actual = dsPickupPointManager.getReferencePickupPoints(request, deliveryService);

        var expected = readResponse("/ds/pickup_point/get_reference_pickup_points_response.xml",
                GetReferencePickupPointsResponse.class,
                Map.of("code", pickupPoint.getId(), "code2", pickupPoint2.getId(), "code3", pickupPoint3.getId()));

        assertThat(actual.getType()).isEqualTo(expected.getType());
        assertThat(actual.getPickupPoints())
                .containsExactlyInAnyOrderElementsOf(expected.getPickupPoints());
    }

    @Test
    void noPickupPointsForDeliveryService() {
        DeliveryService deliveryService = getDeliveryService();

        var request = readRequest("/ds/pickup_point/get_reference_pickup_points_request.xml",
                GetReferencePickupPointsRequest.class, Map.of());
        var actual = dsPickupPointManager.getReferencePickupPoints(request, deliveryService);

        assertThat(actual.getPickupPoints()).isEmpty();
    }

    @Test
    void beforeDeactivationDayOffsAndOverrides() {
        DeliveryService deliveryService = getDeliveryService();
        LegalPartner legalPartner = getLegalPartner(deliveryService);
        PickupPoint pickupPoint = createPickupPoint(legalPartner, true);

        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime fixedTime = LocalDateTime.of(2020, 9, 1, 5, 0, 0);
        clock.setFixed(fixedTime.toInstant(zone), zone);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason("Логистическая причина", "",
                true, false, "LOGISTICS_REASON");

        deactivationCommandService.deactivate(pickupPoint.getPvzMarketId(), deactivationReason.getId(),
                LocalDate.of(2020, 9, 5));

        var request = readRequest("/ds/pickup_point/get_reference_pickup_points_request.xml",
                GetReferencePickupPointsRequest.class, Map.of());
        var actual = dsPickupPointManager.getReferencePickupPoints(request, deliveryService);
        var expected = readResponse(
                "/ds/pickup_point/get_reference_pickup_points_before_deactivation_response.xml",
                GetReferencePickupPointsResponse.class,
                Map.of("code", pickupPoint.getId()));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void afterDeactivationDayOffsAndOverrides() {
        DeliveryService deliveryService = getDeliveryService();
        LegalPartner legalPartner = getLegalPartner(deliveryService);
        PickupPoint pickupPoint = createPickupPoint(legalPartner, true);

        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime fixedTime = LocalDateTime.of(2020, 8, 27, 5, 0, 0);
        clock.setFixed(fixedTime.toInstant(zone), zone);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason("Логистическая причина", "",
                true, false, "LOGISTICS_REASON");

        deactivationCommandService.deactivate(pickupPoint.getPvzMarketId(), deactivationReason.getId(),
                LocalDate.of(2020, 8, 27));

        fixedTime = LocalDateTime.of(2020, 9, 1, 5, 0, 0);
        clock.setFixed(fixedTime.toInstant(zone), zone);

        var request = readRequest("/ds/pickup_point/get_reference_pickup_points_request.xml",
                GetReferencePickupPointsRequest.class, Map.of());
        var actual = dsPickupPointManager.getReferencePickupPoints(request, deliveryService);
        var expected = readResponse(
                "/ds/pickup_point/get_reference_pickup_points_after_deactivation_response.xml",
                GetReferencePickupPointsResponse.class,
                Map.of("code", pickupPoint.getId()));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void duringDeactivationDayOffsAndOverrides() {
        DeliveryService deliveryService = getDeliveryService();
        LegalPartner legalPartner = getLegalPartner(deliveryService);
        PickupPoint pickupPoint = createPickupPoint(legalPartner, true);

        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime fixedTime = LocalDateTime.of(2020, 9, 1, 5, 0, 0);
        clock.setFixed(fixedTime.toInstant(zone), zone);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason("Логистическая причина", "",
                true, false, "LOGISTICS_REASON");

        deactivationCommandService.deactivate(pickupPoint.getPvzMarketId(), deactivationReason.getId(),
                LocalDate.of(2020, 9, 2));

        fixedTime = LocalDateTime.of(2020, 9, 1, 5, 0, 0);
        clock.setFixed(fixedTime.toInstant(zone), zone);

        var request = readRequest("/ds/pickup_point/get_reference_pickup_points_request.xml",
                GetReferencePickupPointsRequest.class, Map.of());
        var actual = dsPickupPointManager.getReferencePickupPoints(request, deliveryService);
        var expected = readResponse(
                "/ds/pickup_point/get_reference_pickup_points_during_deactivation_response.xml",
                GetReferencePickupPointsResponse.class,
                Map.of("code", pickupPoint.getId()));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void testNotWorkingOnPublicHolidayWithOverride() {
        DeliveryService deliveryService = getDeliveryService();
        LegalPartner legalPartner = getLegalPartner(deliveryService);
        PickupPoint pickupPoint = createPickupPoint(legalPartner, true, List.of(
                PickupPointCalendarOverrideTestParams.builder()
                        .date(LocalDate.of(2020, 1, 3))
                        .isHoliday(false)
                        .build(),
                PickupPointCalendarOverrideTestParams.builder()
                        .date(LocalDate.of(2020, 1, 5))
                        .isHoliday(false)
                        .build(),
                PickupPointCalendarOverrideTestParams.builder()
                        .date(LocalDate.of(2020, 1, 11))
                        .isHoliday(true)
                        .build()
        ), false);

        officialHolidaysManager.update(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 9),
                IntStream.rangeClosed(1, 9)
                        .mapToObj(i -> OfficialHoliday.builder()
                                .date(LocalDate.of(2020, 1, i))
                                .name("ура выходные!!1")
                                .isWeekend(true)
                                .build())
                        .collect(Collectors.toList()));

        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime fixedTime = LocalDateTime.of(2020, 1, 1, 5, 0, 0);
        clock.setFixed(fixedTime.toInstant(zone), zone);

        createOrderWithDeliveryDate(LocalDate.now(clock), pickupPoint);
        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        var request = readRequest("/ds/pickup_point/get_reference_pickup_points_request.xml",
                GetReferencePickupPointsRequest.class, Map.of());
        var actual = dsPickupPointManager.getReferencePickupPoints(request, deliveryService);
        var expected = readResponse(
                "/ds/pickup_point/get_reference_pickup_points_works_in_public_holidays_response.xml",
                GetReferencePickupPointsResponse.class,
                Map.of("code", pickupPoint.getId()));

        assertThat(actual).isEqualTo(expected);
    }

    private DeliveryService getDeliveryService() {
        return deliveryServiceFactory.createDeliveryService(
                TestDeliveryServiceFactory.DeliveryServiceParams.builder()
                        .token("1111")
                        .build());
    }

    private LegalPartner getLegalPartner(DeliveryService deliveryService) {
        return legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService)
                        .build());
    }

    private PickupPoint createPickupPoint(LegalPartner legalPartner, boolean worksOnSunday) {
        return createPickupPoint(legalPartner, worksOnSunday, List.of(), true);
    }

    private PickupPoint createPickupPoint(
            LegalPartner legalPartner, boolean worksOnSunday,
            List<PickupPointCalendarOverrideTestParams> overrideDays, boolean worksOnPublicHolidays) {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .params(PickupPointTestParams.builder()
                                .schedule(PickupPointScheduleTestParams.builder()
                                        .overrideDays(overrideDays)
                                        .build())
                                .build()).build());
        return pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                PickupPointTestParams.builder()
                        .timeOffset(3)
                        .capacity(2)
                        .schedule(PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .timeFrom(LocalTime.of(10, 0))
                                                .timeTo(LocalTime.of(20, 0))
                                                .build(),
                                        createHoliday(worksOnSunday)
                                ))
                                .workOnHoliday(worksOnPublicHolidays)
                                .build())
                        .build()
        );
    }

    private PickupPointScheduleDayTestParams createHoliday(boolean withoutHolidays) {
        return withoutHolidays ?
                PickupPointScheduleDayTestParams.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .timeFrom(LocalTime.of(10, 0))
                        .timeTo(LocalTime.of(20, 0))
                        .build() :
                PickupPointScheduleDayTestParams.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .isWorkingDay(false)
                        .build();
    }

    private void createOrderWithDeliveryDate(LocalDate date, PickupPoint pickupPoint) {
        orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(date)
                        .build())
                .build());
    }
}
