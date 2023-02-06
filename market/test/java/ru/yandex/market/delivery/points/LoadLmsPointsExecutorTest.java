package ru.yandex.market.delivery.points;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.delivery.RetryableMbiLmsClient;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletSource;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoService;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.outlet.OutletInfo.NOT_DEFINED_ID;

/**
 * Функциональные тесты для {@link LoadLmsPointsExecutor}
 */
class LoadLmsPointsExecutorTest extends FunctionalTest {

    private static final long DATE_MILLIS = Instant.parse("2020-04-15T10:12:35Z").toEpochMilli();

    @Autowired
    private DeliveryInfoService deliveryInfoService;

    @Autowired
    private RetryableMbiLmsClient retryableMbiLmsClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LogisticsPointConverter logisticsPointConverter;

    @Autowired
    private Clock clock;

    private LoadLmsPointsExecutor executor;
    private final DbMarketDeliveryOutletInfoService marketDeliveryOutletInfoService
            = mock(DbMarketDeliveryOutletInfoService.class);

    @BeforeEach
    void setUp() {
        LmsPointsLoaderService lmsPointsLoader = new LmsPointsLoaderService(deliveryInfoService,
                marketDeliveryOutletInfoService, retryableMbiLmsClient, logisticsPointConverter);
        executor = new LoadLmsPointsExecutor(lmsPointsLoader);
    }

    @Test
    @DbUnitDataSet(before = "testImportWarehouses.before.csv")
    void testImportWarehouses() {
        when(lmsClient.getLogisticsPoints(any())).thenAnswer(invocation -> createPoints1340());
        when(clock.millis()).thenReturn(DATE_MILLIS);

        executor.doJob(null);

        verify(marketDeliveryOutletInfoService)
                .refreshMarketDeliveryOutletInfo(
                        eq(1340L),
                        eq(ru.yandex.market.core.delivery.PointType.INLET),
                        eq(createExpectedOutletInfo1340()),
                        eq(OutletSource.LMS));
    }

    private List<LogisticsPointResponse> createPoints1340() {
        return List.of(
                LogisticsPointResponse.newBuilder()
                        .id(10000978048L)
                        .partnerId(1340L)
                        .externalId("172")
                        .type(PointType.WAREHOUSE)
                        .name("Склад WH05")
                        .address(Address.newBuilder()
                                .locationId(120013)
                                .comment("Россия")
                                .settlement("село Софьино")
                                .postCode("140126")
                                .latitude(new BigDecimal("55.5"))
                                .longitude(new BigDecimal("38.1"))
                                .street("территория Логистический технопарк Софьино")
                                .house("к1")
                                .region("Московская область")
                                .build())
                        .phones(Set.of(new Phone("+79253222231", null, "some comment", PhoneType.PRIMARY)))
                        .schedule(Set.of(createScheduleDayResponse(85791946, 1, "08:00:00", "20:00:00")))
                        .build()
        );
    }

    private List<OutletInfo> createExpectedOutletInfo1340() {
        OutletInfo outletInfo = new OutletInfo(NOT_DEFINED_ID, NOT_DEFINED_ID, OutletType.DEPOT, "Склад WH05", true,
                "10000978048");
        outletInfo.setDeliveryServiceId(1340L);
        outletInfo.setDeliveryServiceOutletId("10000978048");
        outletInfo.setDeliveryServiceOutletCode("172");
        outletInfo.setInlet(true);
        outletInfo.setMain(false);
        outletInfo.setActualizationDate(new Date(DATE_MILLIS));
        outletInfo.setOutletSource(OutletSource.LMS);
        outletInfo.setAddress(ru.yandex.market.core.outlet.Address.builder()
                .setCity("село Софьино")
                .setStreet("территория Логистический технопарк Софьино")
                .setNumber("к1")
                .setOther("Россия")
                .setPostCode("140126")
                .build());
        outletInfo.addPhone(PhoneNumber.builder()
                .setCountry("+7")
                .setCity("925")
                .setNumber("3222231")
                .setPhoneType(ru.yandex.common.util.phone.PhoneType.PHONE)
                .setComments("some comment")
                .build());
        outletInfo.setGeoInfo(new GeoInfo(new Coordinates(38.1, 55.5), 120013L));
        outletInfo.setSchedule(
                new Schedule(NOT_DEFINED_ID, List.of(new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 480, 720))));
        return List.of(outletInfo);
    }

    private ScheduleDayResponse createScheduleDayResponse(long id, int day, String timeFrom, String timeTo) {
        return new ScheduleDayResponse(id, day, LocalTime.parse(timeFrom), LocalTime.parse(timeTo));
    }
}
