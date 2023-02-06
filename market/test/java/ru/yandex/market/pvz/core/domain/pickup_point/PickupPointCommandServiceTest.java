package ru.yandex.market.pvz.core.domain.pickup_point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType;
import ru.yandex.market.pvz.core.domain.logbroker.crm.produce.CrmLogbrokerEventPublisher;
import ru.yandex.market.pvz.core.domain.pickup_point.branding.BrandRegionParams;
import ru.yandex.market.pvz.core.domain.pickup_point.branding.PickupPointBrandingData;
import ru.yandex.market.pvz.core.domain.pickup_point.branding.PickupPointBrandingDataParams;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarOverrideParams;
import ru.yandex.market.pvz.core.domain.pickup_point.location.PickupPointLocationParams;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.PickupPointScheduleParams;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.PickupPointScheduleParams.PickupPointScheduleDayParams;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.model.PickupPointScheduleDay;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.CreatePickupPointBuilder;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.CAPACITY_FOR_BRAND_PVZ;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.CRM_PVZ_SUPPORT_CARD;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DEFAULT_CAPACITY;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DISABLE_DELIVERY_DATES_PICKUP_POINT_CALENDAR_CHECK;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGIONS_FOR_DEPENDENT_PARAMS;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_HEIGHT;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_LENGTH;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_WIDTH;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.UNEDITABLE_CALENDAR_DAYS_COUNT;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_ACTIVE;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_BRANDED_RETURN_ALLOWED;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_BRANDING_TYPE;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_DROP_OFF_FEATURE;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_PARTIAL_RETURN_ALLOWED;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_RETURN_ALLOWED;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_STORAGE_PERIOD;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_TIME_OFFSET;
import static ru.yandex.market.pvz.core.domain.pickup_point.branding.PickupPointBrandingData.DEFAULT_WORKING_DAYS_COUNT;
import static ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory.DEFAULT_REGIONS;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_BUILDING;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_COUNTRY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_FLOOR;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_HOUSE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_HOUSING;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_INTERCOM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LAT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LNG;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LOCALITY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LOCATION_ID;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_METRO;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_OFFICE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_PORCH;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_REGION;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_STREET;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_ZIP_CODE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_IS_WORKING_DAY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_TO;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleTestParams.DEFAULT_WORKS_ON_HOLIDAY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_BRAND_DATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CARD_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CARD_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CASH_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CASH_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_HEIGHT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_INSTRUCTION;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_LENGTH;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_MAX_SIDES_SUM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_PREPAY_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_TRANSMISSION_REWARD;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_WEIGHT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_WIDTH;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_CLIENT_AREA;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_WAREHOUSE_AREA;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointCommandServiceTest {

    private static final LocalDate DATE = LocalDate.of(2020, 6, 1);

    private final TestPickupPointFactory pickupPointFactory;
    private final TestBrandRegionFactory brandRegionFactory;
    private final TestOrderFactory orderFactory;
    private final TestableClock clock;

    private final PickupPointQueryService pickupPointQueryService;
    private final PickupPointCommandService pickupPointCommandService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    private final DbQueueTestUtil dbQueueTestUtil;

    @MockBean
    private CrmLogbrokerEventPublisher crmLogbrokerEventPublisher;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.EPOCH, ZoneId.systemDefault());
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_LENGTH, PickupPoint.REGION_SPECIFIC_MAX_LENGTH);
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_WIDTH, PickupPoint.REGION_SPECIFIC_MAX_WIDTH);
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_HEIGHT, PickupPoint.REGION_SPECIFIC_MAX_HEIGHT);
        configurationGlobalCommandService.setValue(REGIONS_FOR_DEPENDENT_PARAMS,
                PickupPoint.REGIONS_FOR_DEPENDENT_PARAMS);
        configurationGlobalCommandService.setValue(UNEDITABLE_CALENDAR_DAYS_COUNT, 7);
        configurationGlobalCommandService.setValue(DEFAULT_CAPACITY, PickupPoint.DEFAULT_CAPACITY);
        configurationGlobalCommandService.setValue(CAPACITY_FOR_BRAND_PVZ, PickupPoint.CAPACITY_FOR_BRAND_PVZ);
        configurationGlobalCommandService.setValue(CRM_PVZ_SUPPORT_CARD, true);
    }

    @Test
    void createPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        PickupPointParams actual = pickupPointQueryService.getHeavy(pickupPoint.getId());

        var createdBuilder = getCurrentSystemParamsBuilder(PickupPointParams.builder(), pickupPoint);
        createdBuilder = getDefaultSensitiveParamsBuilder(createdBuilder);
        createdBuilder = getDefaultInsensitiveParamsBuilder(createdBuilder);
        PickupPointParams expected = createdBuilder.active(true).build();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createPickupPointWithRegionSpecificSizes() {
        String region = PickupPoint.REGIONS_FOR_DEPENDENT_PARAMS.stream().findFirst().orElseThrow();

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .location(TestPickupPointFactory.PickupPointLocationTestParams.builder()
                                        .region(region)
                                        .build())
                                .build())
                        .build()
        );

        PickupPointParams created = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(created.getMaxLength()).isEqualTo(PickupPoint.REGION_SPECIFIC_MAX_LENGTH);
        assertThat(created.getMaxWidth()).isEqualTo(PickupPoint.REGION_SPECIFIC_MAX_WIDTH);
        assertThat(created.getMaxHeight()).isEqualTo(PickupPoint.REGION_SPECIFIC_MAX_HEIGHT);
    }

    @Test
    void createPickupPointWithDefaultSizes() {
        String region = "Random region";

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .location(TestPickupPointFactory.PickupPointLocationTestParams.builder()
                                        .region(region)
                                        .build())
                                .build())
                        .build()
        );

        PickupPointParams created = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(created.getMaxLength()).isEqualTo(PickupPoint.DEFAULT_MAX_LENGTH);
        assertThat(created.getMaxWidth()).isEqualTo(PickupPoint.DEFAULT_MAX_WIDTH);
        assertThat(created.getMaxHeight()).isEqualTo(PickupPoint.DEFAULT_MAX_HEIGHT);
    }

    @Transactional
    @Test
    void createBrandedPickupPoint() {
        brandRegionFactory.createDefaults();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .brandingType(PickupPointBrandingType.FULL)
                                .build())
                        .build()
        );

        PickupPointParams created = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(created.getBrandingType()).isEqualTo(PickupPointBrandingType.FULL);
        assertThat(created.getReturnAllowed()).isEqualTo(DEFAULT_BRANDED_RETURN_ALLOWED);
        assertThat(created.getCapacity()).isEqualTo(PickupPoint.CAPACITY_FOR_BRAND_PVZ);
        assertThat(created.getSchedule().getWorksOnHoliday()).isTrue();
        assertThat(StreamEx.of(created.getSchedule().getScheduleDays())
                .noneMatch(d -> d.getIsWorkingDay().equals(false)))
                .isTrue();
    }

    @Test
    void unableToCreatePickupPointWithTheSamePvzMarketId() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Long pvzMarketId = pickupPoint.getPvzMarketId();

        assertThatThrownBy(() ->
                pickupPointFactory.createPickupPoint(
                        CreatePickupPointBuilder.builder()
                                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                        .pvzMarketId(pvzMarketId)
                                        .build())
                                .build()
                ))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void unableToCreatePickupPointWithNotAllDaysOfWeek() {
        assertThatThrownBy(() ->
                pickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
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
                                                        .dayOfWeek(DayOfWeek.FRIDAY)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SATURDAY)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build()
                ))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void unableToCreatePickupPointWithNoWorkingDays() {
        assertThatThrownBy(() ->
                pickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                        .scheduleDays(List.of(
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.MONDAY)
                                                        .isWorkingDay(false)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.TUESDAY)
                                                        .isWorkingDay(false)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                        .isWorkingDay(false)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.THURSDAY)
                                                        .isWorkingDay(false)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.FRIDAY)
                                                        .isWorkingDay(false)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SATURDAY)
                                                        .isWorkingDay(false)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                                        .isWorkingDay(false)
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build()
                ))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void unableToCreatePickupPointWithExtraDaysOfWeek() {
        assertThatThrownBy(() ->
                pickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
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
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.MONDAY)
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build()
                ))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void unableToCreatePickupPointWithRepeatedDaysOfWeek() {
        assertThatThrownBy(() ->
                pickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                        .scheduleDays(List.of(
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.MONDAY)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.MONDAY)
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
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build()
                ))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void unableToCreatePickupPointWithNotSetTimeFromForWorkingDay() {
        assertThatThrownBy(() ->
                pickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                        .scheduleDays(List.of(
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.MONDAY)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.TUESDAY)
                                                        .timeFrom(null)
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
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build()
                ))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void unableToCreatePickupPointWithNotSetTimeToForWorkingDay() {
        assertThatThrownBy(() ->
                pickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                        .scheduleDays(List.of(
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.MONDAY)
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.TUESDAY)
                                                        .timeTo(null)
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
                                                        .build(),
                                                PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build()
                ))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void updatePickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(false)
                        .capacity(300)
                        .location(TestPickupPointFactory.PickupPointLocationTestParams.builder()
                                .metro("Смоленская")
                                .office("5")
                                .build())
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
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
                                                .timeTo(LocalTime.of(18, 0))
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .isWorkingDay(false)
                                                .build()
                                ))
                                .build())
                        .cashboxApiSupportAnswer(true)
                        .cashboxId("dasha-12345")
                        .build());

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(updated.getCardAllowed()).isFalse();
        assertThat(updated.getCapacity()).isEqualTo(300);
        assertThat(updated.getCashAllowed()).isEqualTo(DEFAULT_CASH_ALLOWED);
        assertThat(updated.getLocation().getMetro()).isEqualTo("Смоленская");
        assertThat(updated.getLocation().getOffice()).isEqualTo("5");
        assertThat(updated.getLocation().getCountry()).isEqualTo(DEFAULT_COUNTRY);
        assertThat(updated.getSchedule().getWorksOnHoliday()).isEqualTo(DEFAULT_WORKS_ON_HOLIDAY);
        assertThat(updated.getSchedule().getScheduleDays().get(4).getTimeTo()).isEqualTo(LocalTime.of(18, 0));
        assertThat(updated.getSchedule().getScheduleDays().get(5).getIsWorkingDay()).isFalse();
        assertThat(updated.getSchedule().getScheduleDays().get(6).getIsWorkingDay()).isFalse();
        assertThat(updated.getSchedule().getScheduleDays().get(0).getIsWorkingDay()).isTrue();
        assertThat(updated.getCashboxApiSupportAnswer()).isTrue();
        assertThat(updated.getCashboxId()).isEqualTo("dasha-12345");
    }

    @Test
    @Disabled
    void updatePickupPointBrand() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build());
        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(updated.getCapacity()).isEqualTo(PickupPoint.CAPACITY_FOR_BRAND_PVZ);
    }

    @Test
    void notFoundPickupPointForUpdate() {
        assertThatThrownBy(() ->
                pickupPointFactory.updatePickupPoint(1,
                        TestPickupPointFactory.PickupPointTestParams.builder().build()))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Transactional
    @Test
    void testUpdateSchedule() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        pickupPointCommandService.updateCalendarOverrides(
                pickupPoint.getId(), false,
                List.of(DayOfWeek.FRIDAY),
                List.of()
        );

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        List<PickupPointScheduleDayParams> scheduleDays = updated.getSchedule().getScheduleDays();

        assertThat(pickupPoint.getSchedule().getWorksOnHoliday()).isEqualTo(false);
        assertThat(scheduleDays.get(DayOfWeek.FRIDAY.ordinal()).getIsWorkingDay()).isFalse();
    }

    @Transactional
    @Test
    void testUpdateCalendarOverrides() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        pickupPointCommandService.updateCalendarOverrides(
                pickupPoint.getId(), false,
                List.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                List.of(
                        PickupPointCalendarOverrideParams.builder()
                                .date(day(DayOfWeek.TUESDAY))
                                .isHoliday(true)
                                .build(),

                        PickupPointCalendarOverrideParams.builder()
                                .date(day(DayOfWeek.FRIDAY))
                                .isHoliday(false)
                                .build()
                )
        );

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        List<PickupPointScheduleDayParams> scheduleDays = updated.getSchedule().getScheduleDays();

        assertThat(pickupPoint.getSchedule().getWorksOnHoliday()).isEqualTo(false);
        assertThat(scheduleDays.get(DayOfWeek.TUESDAY.ordinal()).getIsWorkingDay()).isTrue();
        assertThat(scheduleDays.get(DayOfWeek.FRIDAY.ordinal()).getIsWorkingDay()).isFalse();
        assertThat(scheduleDays.get(DayOfWeek.SATURDAY.ordinal()).getIsWorkingDay()).isFalse();
        assertThat(scheduleDays.get(DayOfWeek.SUNDAY.ordinal()).getIsWorkingDay()).isFalse();

        assertThat(updated.getSchedule().getCalendarOverrides()).containsExactlyInAnyOrderElementsOf(List.of(
                PickupPointCalendarOverrideParams.builder()
                        .date(day(DayOfWeek.TUESDAY))
                        .isHoliday(true)
                        .build(),

                PickupPointCalendarOverrideParams.builder()
                        .date(day(DayOfWeek.FRIDAY))
                        .isHoliday(false)
                        .build()
        ));

        pickupPointCommandService.updateCalendarOverrides(
                pickupPoint.getId(), false,
                List.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                List.of(
                        PickupPointCalendarOverrideParams.builder()
                                .date(day(DayOfWeek.TUESDAY))
                                .isHoliday(false)
                                .build()
                )
        );

        updated = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(updated.getSchedule().getCalendarOverrides()).containsExactlyInAnyOrderElementsOf(List.of(
                PickupPointCalendarOverrideParams.builder()
                        .date(day(DayOfWeek.FRIDAY))
                        .isHoliday(false)
                        .build()
        ));
    }

    @Transactional
    @Test
    void updateCalendarOverridesForFullBrandWithNotAllWorkingDays() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        brandRegionFactory.createDefaults();

        var updatedPickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(), TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build());

        assertThatThrownBy(() ->
                pickupPointCommandService.updateCalendarOverrides(
                        updatedPickupPoint.getId(), false,
                        List.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        List.of(
                                PickupPointCalendarOverrideParams.builder()
                                        .date(day(DayOfWeek.TUESDAY))
                                        .isHoliday(true)
                                        .build(),

                                PickupPointCalendarOverrideParams.builder()
                                        .date(day(DayOfWeek.FRIDAY))
                                        .isHoliday(false)
                                        .build()
                        )
                ))
                .isExactlyInstanceOf(TplIllegalArgumentException.class);

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        List<PickupPointScheduleDayParams> actualScheduleDays = updated.getSchedule().getScheduleDays();

        var expectedScheduleDays = List.of(
                PickupPointScheduleDayParams.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .isWorkingDay(true)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDayParams.builder()
                        .dayOfWeek(DayOfWeek.TUESDAY)
                        .isWorkingDay(true)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDayParams.builder()
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .isWorkingDay(true)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDayParams.builder()
                        .dayOfWeek(DayOfWeek.THURSDAY)
                        .isWorkingDay(true)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDayParams.builder()
                        .dayOfWeek(DayOfWeek.FRIDAY)
                        .isWorkingDay(true)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDayParams.builder()
                        .dayOfWeek(DayOfWeek.SATURDAY)
                        .isWorkingDay(true)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDayParams.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .isWorkingDay(true)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build()
        );

        assertThat(actualScheduleDays).isEqualTo(expectedScheduleDays);
    }

    @Test
    void updateCalendarOverridesForFullBrandWithCalendarOverrides() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime dateTime = LocalDateTime.of(2021, 4, 5, 10, 51);
        clock.setFixed(dateTime.toInstant(zone), zone);

        brandRegionFactory.createDefaults();

        pickupPoint = pickupPointFactory.updateCalendarOverrides(
                pickupPoint.getId(),
                false,
                StreamEx.of(pickupPoint.getSchedule().getScheduleDays())
                        .filter(d -> !d.getIsWorkingDay())
                        .map(PickupPointScheduleDay::getDayOfWeek)
                        .toList(),
                List.of(
                        PickupPointCalendarOverrideParams.builder()
                                .isHoliday(true)
                                .date(dateTime.plusDays(10).toLocalDate())
                                .build(),
                        PickupPointCalendarOverrideParams.builder()
                                .isHoliday(true)
                                .date(dateTime.plusDays(11).toLocalDate())
                                .build(),
                        PickupPointCalendarOverrideParams.builder()
                                .isHoliday(true)
                                .date(dateTime.plusDays(12).toLocalDate())
                                .build(),
                        PickupPointCalendarOverrideParams.builder()
                                .isHoliday(true)
                                .date(LocalDate.of(2022, 1, 1))
                                .build()
                )
        );

        dateTime = dateTime.plusDays(11);
        clock.setFixed(dateTime.toInstant(zone), zone);

        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(), TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build());

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        List<PickupPointCalendarOverrideParams> actualCalendarOverrides = updated.getSchedule().getCalendarOverrides();

        var expectedCalendarOverrides = List.of(
                PickupPointCalendarOverrideParams.builder()
                        .isHoliday(true)
                        .date(dateTime.minusDays(1).toLocalDate())
                        .build(),
                PickupPointCalendarOverrideParams.builder()
                        .isHoliday(true)
                        .date(LocalDate.of(2022, 1, 1))
                        .build()
        );

        assertThat(updated.getSchedule().getWorksOnHoliday()).isTrue();
        assertThat(actualCalendarOverrides).isEqualTo(expectedCalendarOverrides);
    }

    @Test
    void unableToCreateCalendarOverridesForPast() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        assertThatThrownBy(() ->
                pickupPointCommandService.updateCalendarOverrides(
                        pickupPoint.getId(),
                        false,
                        List.of(),
                        List.of(
                                PickupPointCalendarOverrideParams.builder()
                                        .date(LocalDate.now(clock).minusDays(1))
                                        .isHoliday(true)
                                        .build()
                        )
                ))
                .isExactlyInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    void unableToCreateCalendarOverridesForTooEarlyDays() {
        var pickupPoint = pickupPointFactory.createPickupPoint();


        assertThatThrownBy(() ->
                pickupPointCommandService.updateCalendarOverrides(
                        pickupPoint.getId(),
                        false,
                        List.of(),
                        List.of(
                                PickupPointCalendarOverrideParams.builder()
                                        .date(LocalDate.now(clock).plusDays(2))
                                        .isHoliday(true)
                                        .build()
                        )
                ))
                .isExactlyInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    void createCalendarOverridesForTooEarlyDaysAndWithExistingOrdersInAdminMode() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        LocalDate date = LocalDate.of(2021, 10, 26);
        createOrderWithDeliveryDate(pickupPoint, date.plusDays(10));
        createOrderWithDeliveryDate(pickupPoint, date.plusDays(11));
        createOrderWithDeliveryDate(pickupPoint, date.plusDays(12));

        pickupPointCommandService.adminUpdateCalendarOverrides(
                pickupPoint.getId(),
                false,
                List.of(),
                List.of(
                        PickupPointCalendarOverrideParams.builder()
                                .date(date.plusDays(2))
                                .isHoliday(true)
                                .build(),
                        PickupPointCalendarOverrideParams.builder()
                                .date(date.plusDays(11))
                                .isHoliday(true)
                                .build()
                )
        );

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        List<PickupPointCalendarOverrideParams> actualCalendarOverrides = updated.getSchedule().getCalendarOverrides();

        var expectedCalendarOverrides = List.of(
                PickupPointCalendarOverrideParams.builder()
                        .isHoliday(true)
                        .date(date.plusDays(2))
                        .build(),
                PickupPointCalendarOverrideParams.builder()
                        .isHoliday(true)
                        .date(date.plusDays(11))
                        .build()
        );

        assertThat(updated.getSchedule().getWorksOnHoliday()).isFalse();
        assertThat(actualCalendarOverrides).isEqualTo(expectedCalendarOverrides);
    }

    @Test
    void unableToCreateCalendarOverridesForDaysWithExistingOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        createOrderWithDeliveryDate(pickupPoint, LocalDate.now(clock).plusDays(10));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.now(clock).plusDays(11));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.now(clock).plusDays(12));


        assertThatThrownBy(() ->
                pickupPointCommandService.updateCalendarOverrides(
                        pickupPoint.getId(),
                        false,
                        List.of(),
                        List.of(
                                PickupPointCalendarOverrideParams.builder()
                                        .date(LocalDate.now(clock).plusDays(11))
                                        .isHoliday(true)
                                        .build()
                        )
                ))
                .isExactlyInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    void createCalendarOverridesForDaysWithExistingOrderButDisabledCheck() {
        configurationGlobalCommandService.setValue(DISABLE_DELIVERY_DATES_PICKUP_POINT_CALENDAR_CHECK, true);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        createOrderWithDeliveryDate(pickupPoint, LocalDate.now(clock).plusDays(10));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.now(clock).plusDays(11));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.now(clock).plusDays(12));


        LocalDate now = LocalDate.now(clock);
        pickupPointCommandService.updateCalendarOverrides(
                pickupPoint.getId(),
                false,
                List.of(),
                List.of(
                        PickupPointCalendarOverrideParams.builder()
                                .date(now.plusDays(11))
                                .isHoliday(true)
                                .build()
                )
        );

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        List<PickupPointCalendarOverrideParams> actualCalendarOverrides = updated.getSchedule().getCalendarOverrides();

        var expectedCalendarOverrides = List.of(
                PickupPointCalendarOverrideParams.builder()
                        .isHoliday(true)
                        .date(now.plusDays(11))
                        .build()
        );

        assertThat(updated.getSchedule().getWorksOnHoliday()).isFalse();
        assertThat(actualCalendarOverrides).isEqualTo(expectedCalendarOverrides);
    }

    private void createOrderWithDeliveryDate(PickupPoint pickupPoint, LocalDate date) {
        orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder
                .builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams
                        .builder()
                        .deliveryDate(date)
                        .build())
                .build());
    }

    private LocalDate day(DayOfWeek dow) {
        return DATE.plusDays(dow.ordinal());
    }

    @Test
    void testInsensitiveUpdate() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        verify(crmLogbrokerEventPublisher, times(1)).publish(isA(PickupPoint.class));

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        var updatedParams = updatedBuilder.build();
        pickupPoint = pickupPointCommandService.updateInsensitive(pickupPoint.getPvzMarketId(), updatedParams);

        PickupPointParams actual = pickupPointQueryService.getHeavy(pickupPoint.getId());

        var expectedBuilder = PickupPointParams.builder();
        expectedBuilder = getCurrentSystemParamsBuilder(expectedBuilder, pickupPoint);
        expectedBuilder = getDefaultSensitiveParamsBuilder(expectedBuilder);
        expectedBuilder = getUpdatedInsensitiveParamsBuilder(expectedBuilder);
        expectedBuilder.active(true);
        PickupPointParams expected = expectedBuilder.build();

        assertThat(actual).isEqualTo(expected);
        verify(crmLogbrokerEventPublisher, times(2)).publish(isA(PickupPoint.class));
    }

    @Test
    void tryToInsensitiveUpdateFullBrandWithWeekHolidays() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        brandRegionFactory.createDefaults();

        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(), TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build());

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        updatedBuilder.brandingType(PickupPointBrandingType.FULL);
        updatedBuilder.schedule(PickupPointScheduleParams.builder()
                .worksOnHoliday(!DEFAULT_WORKS_ON_HOLIDAY)
                .scheduleDays(List.of(
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.TUESDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                .isWorkingDay(false)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.THURSDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.FRIDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.SATURDAY)
                                .isWorkingDay(false)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.SUNDAY)
                                .isWorkingDay(false)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build()
                ))
                .build());
        var updatedParams = updatedBuilder.build();
        pickupPoint = pickupPointCommandService.updateInsensitive(pickupPoint.getPvzMarketId(), updatedParams);

        PickupPointParams actual = pickupPointQueryService.getHeavy(pickupPoint.getId());

        var expectedSchedule = PickupPointScheduleParams.builder()
                .worksOnHoliday(!DEFAULT_WORKS_ON_HOLIDAY)
                .scheduleDays(List.of(
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.TUESDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.THURSDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.FRIDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.SATURDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.SUNDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build()
                ))
                .build();

        assertThat(actual.getSchedule()).isEqualTo(expectedSchedule);
    }

    @Test
    void tryToInsensitiveUpdateNotExistentPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        var updatedParams = updatedBuilder.build();

        assertThatThrownBy(() ->
                pickupPointCommandService.updateInsensitive(pickupPoint.getPvzMarketId() + 1, updatedParams))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Transactional
    @Test
    void tryToInsensitiveUpdateInconsistentPickupPointWithNoPrePickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        var updatedParams = updatedBuilder.build();

        assertThatThrownBy(() ->
                pickupPointCommandService.updateInsensitive(pickupPoint.getPvzMarketId(), updatedParams))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void testSensitiveUpdate() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        verify(crmLogbrokerEventPublisher, times(1)).publish(isA(PickupPoint.class));

        brandRegionFactory.createDefaults();

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        updatedBuilder.brandingType(PickupPointBrandingType.FULL);
        var updatedParams = updatedBuilder.build();
        pickupPoint = pickupPointCommandService.updateSensitive(pickupPoint.getPvzMarketId(), updatedParams);

        PickupPointParams actual = pickupPointQueryService.getHeavy(pickupPoint.getId());

        var expectedBuilder = PickupPointParams.builder();
        expectedBuilder = getCurrentSystemParamsBuilder(expectedBuilder, pickupPoint);
        expectedBuilder = getUpdatedSensitiveParamsBuilder(expectedBuilder);
        expectedBuilder = getUpdatedInsensitiveParamsBuilder(expectedBuilder);
        PickupPointParams expected = expectedBuilder.build();
        expected.setTransmissionReward(actual.getBrandingData().getTransmissionReward());
        expected.setReturnAllowed(DEFAULT_BRANDED_RETURN_ALLOWED);
        expected.setPartialReturnAllowed(DEFAULT_PARTIAL_RETURN_ALLOWED);
        expected.setActive(true);

        assertThat(actual).isEqualTo(expected);

        var queue = dbQueueTestUtil.getQueue(PvzQueueType.UPDATE_BRANDED);
        assertThat(queue).contains(String.valueOf(pickupPoint.getId()));

        verify(crmLogbrokerEventPublisher, times(2)).publish(isA(PickupPoint.class));
    }

    @Test
    void tryToSensitiveUpdateFullBrandWithWeekHolidays() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        brandRegionFactory.createDefaults();

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        updatedBuilder.brandingType(PickupPointBrandingType.FULL);
        updatedBuilder.schedule(PickupPointScheduleParams.builder()
                .worksOnHoliday(!DEFAULT_WORKS_ON_HOLIDAY)
                .scheduleDays(List.of(
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.TUESDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                .isWorkingDay(false)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.THURSDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.FRIDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.SATURDAY)
                                .isWorkingDay(false)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.SUNDAY)
                                .isWorkingDay(false)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build()
                ))
                .build());
        var updatedParams = updatedBuilder.build();
        pickupPoint = pickupPointCommandService.updateSensitive(pickupPoint.getPvzMarketId(), updatedParams);

        PickupPointParams actual = pickupPointQueryService.getHeavy(pickupPoint.getId());

        var expectedSchedule = PickupPointScheduleParams.builder()
                .worksOnHoliday(!DEFAULT_WORKS_ON_HOLIDAY)
                .scheduleDays(List.of(
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.TUESDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.THURSDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.FRIDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.SATURDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build(),
                        PickupPointScheduleDayParams.builder()
                                .dayOfWeek(DayOfWeek.SUNDAY)
                                .isWorkingDay(true)
                                .timeFrom(DEFAULT_TIME_FROM)
                                .timeTo(DEFAULT_TIME_TO)
                                .build()
                ))
                .build();

        assertThat(actual.getSchedule()).isEqualTo(expectedSchedule);
    }

    @Test
    void tryToSensitiveUpdateNotExistentPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        var updatedParams = updatedBuilder.build();

        assertThatThrownBy(() ->
                pickupPointCommandService.updateSensitive(pickupPoint.getPvzMarketId() + 1, updatedParams))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void tryToSensitiveUpdateInconsistentPickupPointWithNoPrePickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        var updatedParams = updatedBuilder.build();

        assertThatThrownBy(() ->
                pickupPointCommandService.updateSensitive(pickupPoint.getPvzMarketId(), updatedParams))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void tryToSensitiveUpdateWithNotExistentBrandRegion() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        updatedBuilder.brandingData(getUpdatedBrandDataBuilder().brandRegion(
                BrandRegionParams.builder()
                        .region("Атлантида")
                        .dailyTransmissionThreshold(DEFAULT_REGIONS.get(0).getDailyTransmissionThreshold())
                        .build())
                .build()
        );
        var updatedParams = updatedBuilder.build();

        assertThatThrownBy(() ->
                pickupPointCommandService.updateSensitive(pickupPoint.getPvzMarketId(), updatedParams))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    private PickupPointParams.PickupPointParamsBuilder getCurrentSystemParamsBuilder(
            PickupPointParams.PickupPointParamsBuilder builder, PickupPoint pickupPoint) {
        return builder
                .id(pickupPoint.getId())
                .pvzMarketId(pickupPoint.getPvzMarketId())
                .legalPartnerId(pickupPoint.getLegalPartner().getId())
                .deliveryServiceId(pickupPoint.getLegalPartner().getDeliveryService().getId())
                .returnAllowed(DEFAULT_RETURN_ALLOWED)
                .timeOffset(DEFAULT_TIME_OFFSET)
                .dropOffFeature(DEFAULT_DROP_OFF_FEATURE)
                .lmsId(pickupPoint.getLmsId());
    }

    private PickupPointParams.PickupPointParamsBuilder getDefaultSensitiveParamsBuilder(
            PickupPointParams.PickupPointParamsBuilder builder) {
        return builder
                .active(DEFAULT_ACTIVE)
                .name(DEFAULT_NAME)
                .prepayAllowed(DEFAULT_PREPAY_ALLOWED)
                .cashAllowed(DEFAULT_CASH_ALLOWED)
                .cardAllowed(DEFAULT_CARD_ALLOWED)
                .instruction(DEFAULT_INSTRUCTION)
                .location(PickupPointLocationParams.builder()
                        .locationId(DEFAULT_LOCATION_ID)
                        .country(DEFAULT_COUNTRY)
                        .locality(DEFAULT_LOCALITY)
                        .region(DEFAULT_REGION)
                        .street(DEFAULT_STREET)
                        .house(DEFAULT_HOUSE)
                        .building(DEFAULT_BUILDING)
                        .housing(DEFAULT_HOUSING)
                        .zipCode(DEFAULT_ZIP_CODE)
                        .porch(DEFAULT_PORCH)
                        .intercom(DEFAULT_INTERCOM)
                        .floor(DEFAULT_FLOOR)
                        .metro(DEFAULT_METRO)
                        .office(DEFAULT_OFFICE)
                        .lat(DEFAULT_LAT)
                        .lng(DEFAULT_LNG)
                        .build())
                .storagePeriod(DEFAULT_STORAGE_PERIOD)
                .capacity(PickupPoint.DEFAULT_CAPACITY)
                .maxWeight(DEFAULT_WEIGHT.setScale(2, RoundingMode.HALF_UP))
                .maxLength(DEFAULT_LENGTH)
                .maxWidth(DEFAULT_WIDTH)
                .maxHeight(DEFAULT_HEIGHT)
                .maxSidesSum(DEFAULT_MAX_SIDES_SUM)
                .cashCompensationRate(DEFAULT_CASH_COMPENSATION_RATE)
                .cardCompensationRate(DEFAULT_CARD_COMPENSATION_RATE)
                .transmissionReward(DEFAULT_TRANSMISSION_REWARD.setScale(2, RoundingMode.HALF_UP))
                .brandingType(DEFAULT_BRANDING_TYPE)
                .clientArea(DEFAULT_CLIENT_AREA.setScale(2, RoundingMode.HALF_UP))
                .warehouseArea(DEFAULT_WAREHOUSE_AREA.setScale(2, RoundingMode.HALF_UP))
                .partialReturnAllowed(DEFAULT_PARTIAL_RETURN_ALLOWED);
    }

    private PickupPointParams.PickupPointParamsBuilder getDefaultInsensitiveParamsBuilder(
            PickupPointParams.PickupPointParamsBuilder builder) {
        return builder
                .schedule(PickupPointScheduleParams.builder()
                        .worksOnHoliday(DEFAULT_WORKS_ON_HOLIDAY)
                        .scheduleDays(List.of(
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.MONDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM)
                                        .timeTo(DEFAULT_TIME_TO)
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.TUESDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM)
                                        .timeTo(DEFAULT_TIME_TO)
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM)
                                        .timeTo(DEFAULT_TIME_TO)
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.THURSDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM)
                                        .timeTo(DEFAULT_TIME_TO)
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.FRIDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM)
                                        .timeTo(DEFAULT_TIME_TO)
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.SATURDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM)
                                        .timeTo(DEFAULT_TIME_TO)
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM)
                                        .timeTo(DEFAULT_TIME_TO)
                                        .build()
                        ))
                        .build())
                .phone(DEFAULT_PHONE);
    }

    private PickupPointParams.PickupPointParamsBuilder getUpdatedSystemParamsBuilder(
            PickupPointParams.PickupPointParamsBuilder builder, PickupPoint pickupPoint) {
        return builder
                .id(pickupPoint.getId())
                .pvzMarketId(pickupPoint.getPvzMarketId() + 1)
                .legalPartnerId(pickupPoint.getLegalPartner().getId() + 1)
                .deliveryServiceId(pickupPoint.getLegalPartner().getDeliveryService().getId() + 1)
                .returnAllowed(!DEFAULT_RETURN_ALLOWED)
                .timeOffset(DEFAULT_TIME_OFFSET + 1);
    }

    private PickupPointParams.PickupPointParamsBuilder getUpdatedSensitiveParamsBuilder(
            PickupPointParams.PickupPointParamsBuilder builder) {
        return builder
                .active(DEFAULT_ACTIVE)
                .name("Новое имя")
                .prepayAllowed(DEFAULT_PREPAY_ALLOWED)
                .cashAllowed(!DEFAULT_CASH_ALLOWED)
                .cardAllowed(!DEFAULT_CARD_ALLOWED)
                .instruction("Новая инструкция")
                .location(PickupPointLocationParams.builder()
                        .country("Новая страна")
                        .locality("Новый город")
                        .region("Новый регион")
                        .street("Новая улица")
                        .house("Новый дом")
                        .building("Новое строение")
                        .housing("Новый корпус")
                        .zipCode("Новый индекс")
                        .porch("Новый подъезд")
                        .intercom("Новый домофон")
                        .floor(DEFAULT_FLOOR + 1)
                        .metro("Новое метро")
                        .office("Новый офис")
                        .lat(DEFAULT_LAT.add(BigDecimal.ONE))
                        .lng(DEFAULT_LNG.add(BigDecimal.ONE))
                        .build())
                .storagePeriod(DEFAULT_STORAGE_PERIOD + 1)
                .capacity(PickupPoint.DEFAULT_CAPACITY + 1)
                .maxWeight(DEFAULT_WEIGHT.add(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP))
                .maxLength(DEFAULT_LENGTH.add(BigDecimal.ONE))
                .maxWidth(DEFAULT_WIDTH.add(BigDecimal.ONE))
                .maxHeight(DEFAULT_HEIGHT.add(BigDecimal.ONE))
                .maxSidesSum(DEFAULT_MAX_SIDES_SUM.add(BigDecimal.ONE))
                .cashCompensationRate(DEFAULT_CASH_COMPENSATION_RATE.add(BigDecimal.ONE))
                .cardCompensationRate(DEFAULT_CARD_COMPENSATION_RATE.add(BigDecimal.ONE))
                .transmissionReward(DEFAULT_TRANSMISSION_REWARD.add(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP))
                .brandingType(PickupPointBrandingType.FULL)
                .brandDate(DEFAULT_BRAND_DATE)
                .brandRegion(DEFAULT_REGIONS.get(0).getRegion())
                .clientArea(DEFAULT_CLIENT_AREA.setScale(2, RoundingMode.HALF_UP))
                .warehouseArea(DEFAULT_WAREHOUSE_AREA.setScale(2, RoundingMode.HALF_UP))
                .brandingData(getUpdatedBrandDataBuilder().build());
    }

    private PickupPointBrandingDataParams.PickupPointBrandingDataParamsBuilder getUpdatedBrandDataBuilder() {
        return PickupPointBrandingDataParams.builder()
                .transmissionReward(
                        PickupPointBrandingData.DEFAULT_TRANSMISSION_REWARD.setScale(2, RoundingMode.HALF_UP))
                .dailyTransmissionThreshold(DEFAULT_REGIONS.get(0).getDailyTransmissionThreshold())
                .workingDaysCount(DEFAULT_WORKING_DAYS_COUNT)
                .brandedSince(DEFAULT_BRAND_DATE)
                .expired(false)
                .brandRegion(BrandRegionParams.builder()
                        .region(DEFAULT_REGIONS.get(0).getRegion())
                        .dailyTransmissionThreshold(DEFAULT_REGIONS.get(0).getDailyTransmissionThreshold())
                        .build());
    }

    private PickupPointParams.PickupPointParamsBuilder getUpdatedInsensitiveParamsBuilder(
            PickupPointParams.PickupPointParamsBuilder builder) {
        return builder
                .schedule(PickupPointScheduleParams.builder()
                        .worksOnHoliday(!DEFAULT_WORKS_ON_HOLIDAY)
                        .scheduleDays(List.of(
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.MONDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM.plusHours(1))
                                        .timeTo(DEFAULT_TIME_TO.plusHours(1))
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.TUESDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM.plusHours(1))
                                        .timeTo(DEFAULT_TIME_TO.plusHours(1))
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM.plusHours(1))
                                        .timeTo(DEFAULT_TIME_TO.plusHours(1))
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.THURSDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM.plusHours(1))
                                        .timeTo(DEFAULT_TIME_TO.plusHours(1))
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.FRIDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM.plusHours(1))
                                        .timeTo(DEFAULT_TIME_TO.plusHours(1))
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.SATURDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM.plusHours(1))
                                        .timeTo(DEFAULT_TIME_TO.plusHours(1))
                                        .build(),
                                PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                        .timeFrom(DEFAULT_TIME_FROM.plusHours(1))
                                        .timeTo(DEFAULT_TIME_TO.plusHours(1))
                                        .build()
                        ))
                        .build())
                .phone("Новый телефон");
    }

    @Transactional
    @Test
    void testBindCabinet() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        SimpleShopRegistrationResponse shopInfo = new SimpleShopRegistrationResponse();
        shopInfo.setCampaignId(123);
        pickupPointCommandService.bindCabinet(pickupPoint.getId(), shopInfo);

        assertThat(pickupPoint.getPvzMarketId()).isEqualTo(shopInfo.getCampaignId());
    }

    @Transactional
    @Test
    void makePickupPointBrandedWithNotDefaultCapacity() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        brandRegionFactory.createDefaults();
        LocalDate brandDate = LocalDate.of(2021, 2, 22);
        TestBrandRegionFactory.BrandRegionTestParams brandRegion = DEFAULT_REGIONS.get(2);
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(brandDate)
                        .brandRegion(brandRegion.getRegion())
                        .capacity(PickupPoint.CAPACITY_FOR_BRAND_PVZ + 100)
                        .build());

        var actual = pickupPointQueryService.getHeavy(pickupPoint.getId());
        var actualBrandingData = actual.getBrandingData();

        var expectedBrandingData = PickupPointBrandingDataParams.builder()
                .transmissionReward(PickupPointBrandingData.DEFAULT_TRANSMISSION_REWARD)
                .workingDaysCount(PickupPointBrandingData.DEFAULT_WORKING_DAYS_COUNT)
                .dailyTransmissionThreshold(brandRegion.getDailyTransmissionThreshold())
                .brandedSince(brandDate)
                .expired(false)
                .brandRegion(BrandRegionParams.builder()
                        .region(brandRegion.getRegion())
                        .dailyTransmissionThreshold(brandRegion.getDailyTransmissionThreshold())
                        .build())
                .build();

        assertThat(actual.getBrandingType()).isEqualTo(PickupPointBrandingType.FULL);
        assertThat(actualBrandingData).isEqualTo(expectedBrandingData);
        assertThat(actual.getReturnAllowed()).isTrue();
        assertThat(actual.getCapacity()).isEqualTo(PickupPoint.CAPACITY_FOR_BRAND_PVZ + 100);
    }

    @Transactional
    @Test
    void updateBrandInfoForBrandedPickupPoint() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        brandRegionFactory.createDefaults();
        LocalDate brandDate = LocalDate.of(2021, 2, 22);
        TestBrandRegionFactory.BrandRegionTestParams brandRegion = DEFAULT_REGIONS.get(2);
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(brandDate)
                        .brandRegion(brandRegion.getRegion())
                        .build());

        LocalDate newBrandDate = LocalDate.of(2021, 2, 23);
        TestBrandRegionFactory.BrandRegionTestParams newBrandRegion = DEFAULT_REGIONS.get(3);
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(newBrandDate)
                        .brandRegion(newBrandRegion.getRegion())
                        .build());
        var actual = pickupPointQueryService.getHeavy(pickupPoint.getId());
        var actualBrandingData = actual.getBrandingData();

        var expectedBrandingData = PickupPointBrandingDataParams.builder()
                .transmissionReward(PickupPointBrandingData.DEFAULT_TRANSMISSION_REWARD)
                .workingDaysCount(DEFAULT_WORKING_DAYS_COUNT)
                .dailyTransmissionThreshold(newBrandRegion.getDailyTransmissionThreshold())
                .brandedSince(newBrandDate)
                .expired(false)
                .brandRegion(BrandRegionParams.builder()
                        .region(newBrandRegion.getRegion())
                        .dailyTransmissionThreshold(newBrandRegion.getDailyTransmissionThreshold())
                        .build())
                .build();

        assertThat(actual.getBrandingType()).isEqualTo(PickupPointBrandingType.FULL);
        assertThat(actualBrandingData).isEqualTo(expectedBrandingData);
        assertThat(actual.getReturnAllowed()).isTrue();
    }

    @Test
    void makeBrandedPickupPointUsual() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        brandRegionFactory.createDefaults();
        LocalDate brandDate = LocalDate.of(2021, 2, 22);
        TestBrandRegionFactory.BrandRegionTestParams brandRegion = DEFAULT_REGIONS.get(2);
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(brandDate)
                        .brandRegion(brandRegion.getRegion())
                        .build());

        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.NONE)
                        .build());

        var actual = pickupPointQueryService.getHeavy(pickupPoint.getId());
        var actualBrandingData = actual.getBrandingData();

        assertThat(actualBrandingData).isNull();
        assertThat(pickupPoint.getBrandingData()).hasSize(1);
        assertThat(pickupPoint.getBrandingData().get(0).isExpired()).isTrue();
        assertThat(actual.getReturnAllowed()).isFalse();
    }

    @Transactional
    @Test
    void makePickupPointBrandedAgain() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        brandRegionFactory.createDefaults();
        LocalDate brandDate = LocalDate.of(2021, 2, 22);
        TestBrandRegionFactory.BrandRegionTestParams brandRegion = DEFAULT_REGIONS.get(2);
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(brandDate)
                        .brandRegion(brandRegion.getRegion())
                        .build());

        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.NONE)
                        .build());

        LocalDate newBrandDate = LocalDate.of(2021, 2, 23);
        TestBrandRegionFactory.BrandRegionTestParams newBrandRegion = DEFAULT_REGIONS.get(3);
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(newBrandDate)
                        .brandRegion(newBrandRegion.getRegion())
                        .build());

        var actual = pickupPointQueryService.getHeavy(pickupPoint.getId());
        var actualBrandingData = actual.getBrandingData();

        var expectedBrandingData = PickupPointBrandingDataParams.builder()
                .transmissionReward(PickupPointBrandingData.DEFAULT_TRANSMISSION_REWARD)
                .workingDaysCount(DEFAULT_WORKING_DAYS_COUNT)
                .dailyTransmissionThreshold(newBrandRegion.getDailyTransmissionThreshold())
                .brandedSince(newBrandDate)
                .expired(false)
                .brandRegion(BrandRegionParams.builder()
                        .region(newBrandRegion.getRegion())
                        .dailyTransmissionThreshold(newBrandRegion.getDailyTransmissionThreshold())
                        .build())
                .build();

        assertThat(actualBrandingData).isEqualTo(expectedBrandingData);
        assertThat(pickupPoint.getBrandingData()).hasSize(2);
        assertThat(actual.getReturnAllowed()).isTrue();
        assertThat(actual.getCapacity()).isEqualTo(PickupPoint.DEFAULT_CAPACITY);
    }

    @Test
    void customizeBrandData() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        verify(crmLogbrokerEventPublisher, times(1)).publish(isA(PickupPoint.class));

        brandRegionFactory.createDefaults();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build());

        verify(crmLogbrokerEventPublisher, times(2)).publish(isA(PickupPoint.class));

        BigDecimal customReward = PickupPointBrandingData.DEFAULT_TRANSMISSION_REWARD.add(BigDecimal.ONE);
        int customWorkingDays = DEFAULT_WORKING_DAYS_COUNT + 10;
        int customThreshold = DEFAULT_REGIONS.get(0).getDailyTransmissionThreshold() + 10;
        var customizeBrandParams = PickupPointBrandingDataParams.builder()
                .transmissionReward(customReward)
                .workingDaysCount(customWorkingDays)
                .dailyTransmissionThreshold(customThreshold)
                .build();
        var actual = pickupPointCommandService.customizeBrandData(pickupPoint.getPvzMarketId(), customizeBrandParams);

        verify(crmLogbrokerEventPublisher, times(3)).publish(isA(PickupPoint.class));

        assertThat(actual.getActualBrandData()).isNotNull();
        assertThat(actual.getActualBrandData().getTransmissionReward()).isEqualTo(customReward);
        assertThat(actual.getActualBrandData().getWorkingDaysCount()).isEqualTo(customWorkingDays);
        assertThat(actual.getActualBrandData().getDailyTransmissionThreshold()).isEqualTo(customThreshold);
    }

    @Test
    void tryToCustomizeBrandDataForNotBrandedPickupPoint() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        brandRegionFactory.createDefaults();

        BigDecimal customReward = PickupPointBrandingData.DEFAULT_TRANSMISSION_REWARD.add(BigDecimal.ONE);
        int customWorkingDays = DEFAULT_WORKING_DAYS_COUNT + 10;
        int customThreshold = DEFAULT_REGIONS.get(0).getDailyTransmissionThreshold() + 10;
        var customizeBrandParams = PickupPointBrandingDataParams.builder()
                .transmissionReward(customReward)
                .workingDaysCount(customWorkingDays)
                .dailyTransmissionThreshold(customThreshold)
                .build();
        assertThatThrownBy(() ->
                pickupPointCommandService.customizeBrandData(pickupPoint.getPvzMarketId(), customizeBrandParams))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void tryToCustomizeBrandDataForNotExoitentPickupPoint() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        brandRegionFactory.createDefaults();

        BigDecimal customReward = PickupPointBrandingData.DEFAULT_TRANSMISSION_REWARD.add(BigDecimal.ONE);
        int customWorkingDays = DEFAULT_WORKING_DAYS_COUNT + 10;
        int customThreshold = DEFAULT_REGIONS.get(0).getDailyTransmissionThreshold() + 10;
        var customizeBrandParams = PickupPointBrandingDataParams.builder()
                .transmissionReward(customReward)
                .workingDaysCount(customWorkingDays)
                .dailyTransmissionThreshold(customThreshold)
                .build();
        assertThatThrownBy(() ->
                pickupPointCommandService.customizeBrandData(pickupPoint.getPvzMarketId() + 1, customizeBrandParams))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void markAsDropOff() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        assertThat(pickupPoint.getDropOffFeature()).isFalse();

        var actual = pickupPointCommandService.makeDropOff(pickupPoint.getId());

        assertThat(actual.getDropOffFeature()).isTrue();
    }

    @Test
    void tryToMarkAsDropOffNotExistentPickupPoint() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        assertThat(pickupPoint.getDropOffFeature()).isFalse();

        assertThatThrownBy(() -> pickupPointCommandService.makeDropOff(pickupPoint.getId() + 100))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void updateLmsId() {
        long lmsId = 1234;
        var pickupPoint = pickupPointFactory.createPickupPoint();

        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint.getId()).getLmsId())
                .isEqualTo(lmsId);
    }

    @Test
    void updateTimeOffset() {
        int timeOffset = 5;

        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointCommandService.updateTimeOffset(pickupPoint.getId(), timeOffset);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint.getId()).getTimeOffset())
                .isEqualTo(timeOffset);
    }

    @Test
    void userUpdateAfterAdminUpdate() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        pickupPointCommandService.adminUpdateCalendarOverrides(
                pickupPoint.getId(),
                true,
                List.of(),
                List.of(PickupPointCalendarOverrideParams.builder()
                        .date(LocalDate.of(1970, 1, 3))
                        .isHoliday(true)
                        .build())
        );

        // no exception if override with same params already exists
        pickupPointCommandService.updateCalendarOverrides(
                pickupPoint.getId(),
                true,
                List.of(),
                List.of(PickupPointCalendarOverrideParams.builder()
                                .date(LocalDate.of(1970, 1, 3))
                                .isHoliday(true)
                                .build(),

                        PickupPointCalendarOverrideParams.builder()
                                .date(LocalDate.of(1970, 1, 12))
                                .isHoliday(true)
                                .build())
        );

        // has exception if existing override has changed
        assertThatThrownBy(() -> pickupPointCommandService.updateCalendarOverrides(
                pickupPoint.getId(),
                true,
                List.of(),
                List.of(PickupPointCalendarOverrideParams.builder()
                                .date(LocalDate.of(1970, 1, 3))
                                .isHoliday(false)
                                .build(),

                        PickupPointCalendarOverrideParams.builder()
                                .date(LocalDate.of(1970, 1, 12))
                                .isHoliday(true)
                                .build())
        ));

        // has exception if existing override was removed and replaced with override for another date
        assertThatThrownBy(() -> pickupPointCommandService.updateCalendarOverrides(
                pickupPoint.getId(),
                true,
                List.of(),
                List.of(PickupPointCalendarOverrideParams.builder()
                                .date(LocalDate.of(1970, 1, 4))
                                .isHoliday(true)
                                .build(),

                        PickupPointCalendarOverrideParams.builder()
                                .date(LocalDate.of(1970, 1, 12))
                                .isHoliday(true)
                                .build())
        ));
    }

    @Test
    void clearCashboxUrlAndToken() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cashboxUrl("https://produman.ru/api")
                        .cashboxToken("sfreiufgieruwfgoir")
                        .build());
        assertThat(pickupPoint.cashboxPaymentAllowed()).isTrue();

        var updatedBuilder = PickupPointParams.builder();
        updatedBuilder = getUpdatedSystemParamsBuilder(updatedBuilder, pickupPoint);
        updatedBuilder = getUpdatedSensitiveParamsBuilder(updatedBuilder);
        updatedBuilder = getUpdatedInsensitiveParamsBuilder(updatedBuilder);
        var updatedParams = updatedBuilder.cashboxToken("  ").cashboxUrl(" ").build();
        var actual = pickupPointCommandService.updateInsensitive(pickupPoint.getPvzMarketId(), updatedParams);

        assertThat(actual.getCashboxUrl()).isNull();
        assertThat(actual.getCashboxToken()).isNull();
        assertThat(actual.cashboxPaymentAllowed()).isFalse();
    }
}
