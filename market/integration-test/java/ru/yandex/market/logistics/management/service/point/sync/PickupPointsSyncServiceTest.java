package ru.yandex.market.logistics.management.service.point.sync;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.LocationFilter;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateBool;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.PickupPoint;
import ru.yandex.market.logistic.gateway.common.model.delivery.PickupPointType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Service;
import ru.yandex.market.logistic.gateway.common.model.delivery.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.SettingsMethodSync;
import ru.yandex.market.logistics.management.domain.entity.YtOutlet;
import ru.yandex.market.logistics.management.domain.entity.type.MethodType;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.facade.PartnerCountryFacade;
import ru.yandex.market.logistics.management.queue.producer.PickupPointSyncProducer;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.repository.SettingsMethodSyncRepository;
import ru.yandex.market.logistics.management.repository.YtOutletRepository;
import ru.yandex.market.logistics.management.service.client.PartnerExternalParamService;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.util.TestRegions.MOSCOW_REGION_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;
import static ru.yandex.market.logistics.management.util.TestUtil.point;

@ParametersAreNonnullByDefault
@DatabaseSetup(
    value = "/data/executor/before/sync_partner_pickup_points.xml",
    connection = "dbUnitQualifiedDatabaseConnection",
    type = DatabaseOperation.CLEAN_INSERT
)
@DisplayName("Тесты сервиса PickupPointsSyncService")
@SuppressWarnings({"checkstyle:MagicNumber"})
class PickupPointsSyncServiceTest extends AbstractContextualTest {
    private static final int AVAILABLE_FOR_SYNC_PARTNERS_NUM = 3;
    private static final long PARTNER_ID_1 = 1;
    private static final long PARTNER_ID_2 = 2;
    private static final long PARTNER_ID_3 = 3;
    private static final long PARTNER_ID_4 = 4;
    private static final long PARTNER_ID_5 = 5;
    private static final long PARTNER_ID_8 = 8;
    private static final long PARTNER_ID_10 = 10;
    private static final long PARTNER_ID_100 = 100;
    private static final long PARTNER_ID_103 = 103;
    private static final long PARTNER_ID_12 = 12;
    private static final long PARTNER_ID_13 = 13;
    private static final String ERROR_MESSAGE = "internal error";
    private static final double LATITUDE = 50.23;
    private static final double LONGITUDE = 50.17;
    private static final double LAT_KAPOTNYA = 55.631152;
    private static final double LON_KAPOTNYA = 37.799472;
    private static final int REGION_ID_KAPOTNYA = 120545;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private GeoClient geoClient;

    @Autowired
    private HttpGeobase httpGeobase;

    @Autowired
    private ImportPartnerPickupPointsService importPartnerPickupPointsService;

    @Autowired
    private LogisticsPointPhotoService logisticsPointPhotoService;

    @Autowired
    private TestableClock clock;

    @Autowired
    private PartnerExternalParamService partnerExternalParamService;

    @Autowired
    private PickupPointsSyncService pickupPointsSyncService;

    @Autowired
    private PickupPointSyncProducer pickupPointSyncProducer;

    @Autowired
    private SettingsMethodSyncRepository settingsMethodSyncRepository;

    @Autowired
    private YtOutletRepository ytOutletRepository;

    @Autowired
    private PartnerCountryFacade partnerCountryFacade;

    @Autowired
    private RegionService regionService;

    @Captor
    private ArgumentCaptor<SettingsMethodSync> settingsMethodSyncArgumentCaptor;

    @BeforeEach
    void init() {
        clock.setFixed(LocalDate.of(2021, 2, 1).atStartOfDay(
            ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()
        );

        pickupPointsSyncService = new PickupPointsSyncService(
            deliveryClient,
            importPartnerPickupPointsService,
            logisticsPointPhotoService,
            partnerExternalParamService,
            settingsMethodSyncRepository,
            pickupPointSyncProducer,
            clock,
            partnerCountryFacade
        );

        when(geoClient.find(eq("Россия, Московская область, Москва"), any(GeoSearchParams.class))).thenReturn(List.of(
            geoObject("213", "55.753215 37.622504", "Россия, Москва", Kind.PROVINCE)
        ));

        when(geoClient.find(
            eq("Россия, Московская область, Москва, Ленина, дом 12, строение 34, корпус 10"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(
                geoObject("274", point(LATITUDE, LONGITUDE), "Москва, Ленина, дом 12, строение 34, корпус 10", null)
            ));

        when(geoClient.find(
            eq("Россия, Московская область, Москва, Льва Толстого, дом 18"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(geoObject("777", "", "Москва, Льва Толстого, дом 18", null)));

        when(geoClient.find(
            eq("Казахстан, Воронежская область, Воронеж"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(geoObject("1234", null, null, Kind.PROVINCE)));

        when(geoClient.find(
            eq("Казахстан, Воронежская область, Воронеж, Сталина, дом 12, строение 21, корпус 15"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(geoObject(null, "210 110", null, null)));
    }

    @Test
    @DisplayName("Очистить external_hash для всех точек партнера")
    @ExpectedDatabase(
        value = "/data/executor/after/clear_external_hash.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void clearPickupPointsExternalHash() {
        pickupPointsSyncService.clearPickupPointsExternalHash(1L);
    }

    @Test
    @DisplayName("Фэйл синка ПВЗ от СД")
    void syncWithException() throws GatewayApiException {
        // По данному партнеру будет исключение
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_5))))
            .thenThrow(new RuntimeException(ERROR_MESSAGE));

        // Проверить исключение
        assertThrows(RuntimeException.class, () -> pickupPointsSyncService.syncPickupPoints(5L));
    }

    @Test
    @DisplayName("2е попытки зафейлились, 3я успешная при обновлении точек в БД")
    void syncRetrySuccess() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_1))))
            .thenReturn(List.of(pickupPoint1()));

        doThrow(new RuntimeException("Error 1"))
            .doThrow(new RuntimeException("Error 2"))
            .doCallRealMethod()
            .when(transactionTemplate).execute(any());

        when(httpGeobase.getRegionId(anyDouble(), anyDouble()))
            .thenReturn(MOSCOW_REGION_ID);

        pickupPointsSyncService.syncPickupPoints(PARTNER_ID_1);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
        verify(httpGeobase).getRegionId(LATITUDE, LONGITUDE);
    }

    @Test
    @DisplayName("3и из 3х попыток зафейлились при обновлении точек в БД")
    void syncRetryError() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_1))))
            .thenReturn(List.of(pickupPoint1()));

        doThrow(new RuntimeException("Error 1"))
            .doThrow(new RuntimeException("Error 2"))
            .doThrow(new RuntimeException("Final error"))
            .when(transactionTemplate).execute(any());

        RuntimeException error = assertThrows(
            RuntimeException.class,
            () -> pickupPointsSyncService.syncPickupPoints(PARTNER_ID_1)
        );

        softly.assertThat(error.getMessage())
            .isEqualTo("Errors occurred during updating partner 1 points: Final error");
    }

    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_points_to_change_coords.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_with_changed_coords.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @DisplayName("Координаты от партнера устанавливаются независимо от ответа геокодера")
    void coordsWereOverrodeByPartner() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_100))))
            .thenReturn(List.of(pickupPoint100()));
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(PARTNER_ID_100);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
        verify(httpGeobase).getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_points.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешный синк ПВЗ от СД")
    void syncSuccessful() throws GatewayApiException {
        // По данному партнеру одна точка будет обновлена, одна будет удалена (не пришла от deliveryClient),
        // еще одна будет пропущена, так как у двух точек совпадают идентификаторы в системе партнера,
        // флаг is_available_for_c2c игнорируется
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_1))))
            .thenReturn(List.of(pickupPoint1(), pickupPoint1()));

        // По данному партнеру одна точка будет обновлена, одна в заморозке и не изменится,
        // еще одна не изменится (так как значение external_hash в базе совпадет с хэшом пришедшей точки),
        // одна новая добавится
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_2))))
            .thenReturn(List.of(pickupPoint3(), pickupPoint4(), pickupPoint9(), pickupPoint11()));

        // Данные партнеры в неподходящем статусе, по ним ничего не поменяется
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_3))))
            .thenReturn(List.of(pickupPoint5()));
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_4))))
            .thenReturn(List.of(pickupPoint6()));

        // Нет данных по данному партнеру
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_5))))
            .thenReturn(List.of());

        // По данному партнеру будет обновлено расписание точки
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_8))))
            .thenReturn(List.of(pickupPoint10()));

        // По этому партнёру выключена синхронизация календарей точек
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_10))))
            .thenReturn(List.of(pickupPoint12()));

        // Создание точки для партнера с сабтипом go_platform
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_103))))
            .thenReturn(List.of(pickupPoint103()));

        // Дерево регионов, чтобы проверить корректность определения locationId через exactLocationId
        // в address
        when(regionService.get()).thenReturn(buildRegionTree());

        when(httpGeobase.getRegionId(anyDouble(), anyDouble()))
            .thenReturn(MOSCOW_REGION_ID);

        partnerRepository.findPartnersSupportingMethod(
                Arrays.asList(PartnerStatus.ACTIVE, PartnerStatus.TESTING),
                MethodType.GET_REFERENCE_PICKUP_POINTS
            )
            .forEach(
                partnerId -> pickupPointsSyncService.syncPickupPoints(partnerId)
            );
        assertWorkingTimes();
        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(7))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
        verify(httpGeobase, times(2)).getRegionId(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Проверка интервала календарей [today - 10, today + 80]")
    void testCalendarInterval() {
        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        softly.assertThat(calendarInterval.getFrom().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .contains("2021-01-22T12:00:00");
        softly.assertThat(calendarInterval.getTo().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .contains("2021-04-22T12:00:00");
    }

    @Test
    @DisplayName("Если для партнёра запрещены возвраты это оверрайдит getReferencePickupPoints")
    @DatabaseSetup(
        value = "/data/service/pickpoint/before/partner_external_params_return_allowed_override.xml",
        type = DatabaseOperation.INSERT
    )
    void testForciblyDisableReturns() throws GatewayApiException {
        softly.assertThat(pickupPoint10().getReturnAllowed()).isEqualTo(true);

        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_12))))
            .thenReturn(List.of(pickupPoint13()
            ));

        pickupPointsSyncService.syncPickupPoints(PARTNER_ID_12);

        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), any(), any());

        ArgumentCaptor<List<PickupPoint>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(importPartnerPickupPointsService, times(1))
            .importPoints(eq(PARTNER_ID_12), argumentCaptor.capture());

        softly.assertThat(argumentCaptor.getValue().get(0).getReturnAllowed()).isEqualTo(false);
    }

    @Test
    @DisplayName("У партнера включено включение отложенного вызова курьера по дефолту")
    @DatabaseSetup(
        value = "/data/service/pickpoint/before/partner_subtype_go_platform_features.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/executor/after/go_platform_partner_import.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testGoPlatformPickup() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_103))))
            .thenReturn(List.of(pickupPoint103()));
        when(deliveryClient.getReferencePickupPoints(isNull(), isNull(), any(), eq(lgwPartner(PARTNER_ID_2))))
            .thenReturn(List.of(pickupPoint11()));
        when(httpGeobase.getRegionId(anyDouble(), anyDouble()))
            .thenReturn(MOSCOW_REGION_ID);

        pickupPointsSyncService.syncPickupPoints(PARTNER_ID_2);
        pickupPointsSyncService.syncPickupPoints(PARTNER_ID_103);

        verify(deliveryClient, times(2))
            .getReferencePickupPoints(isNull(), isNull(), any(), any());
    }

    @Test
    @DisplayName("Передаем фильтры по странам, с которыми связан партнер")
    void testSyncPickupPointsForPartnerWithCountries() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), isNull(), any(), eq(lgwPartner(PARTNER_ID_13))))
            .thenReturn(List.of());

        pickupPointsSyncService.syncPickupPoints(PARTNER_ID_13);

        verify(deliveryClient).getReferencePickupPoints(
            eq(getExpectedLocationFiltersForPartner13()),
            isNull(),
            any(),
            eq(lgwPartner(PARTNER_ID_13))
        );
    }

    @Test
    @DisplayName("Запускает dbqueue таску для каждого метода который пора синхронизировать по крону")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_points_all_sync.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_points_all_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_callsProduceTaskForEachValidMethodSyncEntity() {
        clock.setFixed(Instant.ofEpochSecond(2272190401L), ZoneId.of("UTC")); // 2042-01-01 12:00:01

        pickupPointsSyncService.syncAllPickupPoints();

        verify(pickupPointSyncProducer, times(AVAILABLE_FOR_SYNC_PARTNERS_NUM)).produceTask(
            settingsMethodSyncArgumentCaptor.capture(),
            eq(false)
        );
        verifyNoMoreInteractions(pickupPointSyncProducer);

        List<SettingsMethodSync> actual = settingsMethodSyncArgumentCaptor.getAllValues();
        List<SettingsMethodSync> expected = settingsMethodSyncRepository.findAllById(List.of(1L, 2L, 5L));
        assertThat(actual).containsExactlyInAnyOrder(expected.toArray(SettingsMethodSync[]::new));
    }

    @Nonnull
    private static Partner lgwPartner(long id) {
        return new Partner(id);
    }

    @Nonnull
    private static PickupPoint pickupPoint1() {
        return PickupPoint.builder()
            .setCode("CODE1")
            .setName("POINT 1")
            .setAddress(location1())
            .setPhones(List.of(phone1(), phone2()))
            .setActive(true)
            .setCardAllowed(false)
            .setCashAllowed(false)
            .setPrepayAllowed(false)
            .setReturnAllowed(false)
            .setAvailableForC2C(false)
            .setMaxWeight(new BigDecimal("500"))
            .setMaxLength(BigDecimal.valueOf(330))
            .setMaxWidth(BigDecimal.valueOf(40))
            .setMaxHeight(BigDecimal.valueOf(60))
            .setMaxSidesSum(BigDecimal.valueOf(666))
            .setStoragePeriod(12)
            .setInstruction("instruction 1")
            .setSchedule(List.of(workTime1(), workTime3()))
            .setServices(List.of(service(ServiceType.CHECK), service(ServiceType.COMPLECT)))
            .setCalendar(
                List.of(
                    dateBool(9, true),
                    dateBool(10, true),
                    dateBool(11, true),
                    dateBool(12, true),
                    dateBool(13, false),
                    dateBool(14, false),
                    dateBool(15, false)
                )
            )
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint3() {
        return PickupPoint.builder()
            .setCode("CODE3")
            .setName("POINT 3")
            .setAddress(location1())
            .setPhones(List.of(phone1(), phone2()))
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(false)
            .setSchedule(List.of(workTime1(), workTime2()))
            .setCalendar(List.of(dateBool(15, false)))
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint4() {
        return PickupPoint.builder()
            .setCode("CODE4")
            .setName("POINT 4")
            .setAddress(location2())
            .setPhones(List.of(phone2()))
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(false)
            .setType(PickupPointType.TERMINAL)
            .setServices(List.of(
                service(ServiceType.CASH_SERVICE),
                service(ServiceType.CHECK),
                service(ServiceType.TRYING)
            ))
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint5() {
        return PickupPoint.builder()
            .setCode("CODE5")
            .setName("POINT 5")
            .setAddress(location1())
            .setPhones(List.of(phone1(), phone2()))
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(false)
            .setType(PickupPointType.TERMINAL)
            .setSchedule(List.of(workTime1(), workTime2()))
            .setCalendar(List.of(dateBool(15, false)))
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint6() {
        return PickupPoint.builder()
            .setCode("CODE6")
            .setName("POINT 6")
            .setAddress(location1())
            .setPhones(List.of(phone1(), phone2()))
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(false)
            .setType(PickupPointType.TERMINAL)
            .setSchedule(List.of(workTime1(), workTime2()))
            .setCalendar(List.of(dateBool(15, false)))
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint9() {
        return PickupPoint.builder()
            .setCode("CODE9")
            .setName("UNCHANGED_POINT")
            .setAddress(location3())
            .setPhones(List.of(phone1(), phone2()))
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(true)
            .setMaxWeight(new BigDecimal("15"))
            .setMaxLength(BigDecimal.valueOf(15))
            .setMaxWidth(BigDecimal.valueOf(15))
            .setMaxHeight(BigDecimal.valueOf(15))
            .setMaxSidesSum(BigDecimal.valueOf(125))
            .setStoragePeriod(10)
            .setInstruction("instruction9")
            .setType(PickupPointType.PICKUP_POINT)
            .setSchedule(List.of(workTime1(), workTime2()))
            .setCalendar(List.of(dateBool(15, false)))
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint10() {
        return PickupPoint.builder()
            .setCode("CODE10")
            .setName("UPDATED_PICKUP")
            .setAddress(location5())
            .setPhones(List.of())
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(true)
            .setReturnAllowed(true)
            .setPrepayAllowed(true)
            .setMaxWeight(new BigDecimal("15"))
            .setMaxLength(BigDecimal.valueOf(15))
            .setMaxWidth(BigDecimal.valueOf(15))
            .setMaxHeight(BigDecimal.valueOf(15))
            .setMaxSidesSum(BigDecimal.valueOf(125))
            .setStoragePeriod(10)
            .setInstruction("instruction10")
            .setType(PickupPointType.PICKUP_POINT)
            .setSchedule(List.of(workTime1(), workTime2()))
            .setCalendar(List.of())
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint11() {
        return PickupPoint.builder()
            .setCode("CREATED_CODE")
            .setName("CREATED_POINT")
            .setAddress(location4())
            .setPhones(List.of(phone1(), phone2()))
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(false)
            .setMaxWeight(new BigDecimal("12.4"))
            .setMaxLength(BigDecimal.valueOf(220))
            .setMaxWidth(BigDecimal.valueOf(30))
            .setMaxHeight(BigDecimal.valueOf(40))
            .setMaxSidesSum(BigDecimal.valueOf(77))
            .setStoragePeriod(11)
            .setInstruction("created_instruction")
            .setType(PickupPointType.TERMINAL)
            .setSchedule(List.of(workTime1(), workTime2()))
            .setCalendar(List.of(dateBool(15, false)))
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint12() {
        return PickupPoint.builder()
            .setCode("CODE10")
            .setName("NO_CALENDAR_SYNC_POINT")
            .setAddress(location6())
            .setPhones(List.of(phone1(), phone2()))
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(false)
            .setMaxWeight(new BigDecimal("12.4"))
            .setMaxLength(BigDecimal.valueOf(220))
            .setMaxWidth(BigDecimal.valueOf(30))
            .setMaxHeight(BigDecimal.valueOf(40))
            .setMaxSidesSum(BigDecimal.valueOf(77))
            .setStoragePeriod(11)
            .setInstruction("created_instruction")
            .setType(PickupPointType.PICKUP_POINT)
            .setSchedule(List.of())
            .setCalendar(List.of(dateBool(16, false)))
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint13() {
        return PickupPoint.builder()
            .setCode("CODE13")
            .setName("UPDATED_PICKUP")
            .setAddress(location6())
            .setPhones(List.of())
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(true)
            .setReturnAllowed(true)
            .setPrepayAllowed(true)
            .setMaxWeight(new BigDecimal("15"))
            .setMaxLength(BigDecimal.valueOf(15))
            .setMaxWidth(BigDecimal.valueOf(15))
            .setMaxHeight(BigDecimal.valueOf(15))
            .setMaxSidesSum(BigDecimal.valueOf(125))
            .setStoragePeriod(10)
            .setInstruction("instruction13")
            .setType(PickupPointType.PICKUP_POINT)
            .setSchedule(List.of(workTime1(), workTime2()))
            .setCalendar(List.of())
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint100() {
        return PickupPoint.builder()
            .setCode("CODE100")
            .setName("changedLatLonPoint")
            .setAddress(location100())
            .setPhones(List.of())
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(true)
            .setReturnAllowed(true)
            .setPrepayAllowed(true)
            .setMaxWeight(new BigDecimal("15"))
            .setMaxLength(BigDecimal.valueOf(15))
            .setMaxWidth(BigDecimal.valueOf(15))
            .setMaxHeight(BigDecimal.valueOf(15))
            .setMaxSidesSum(BigDecimal.valueOf(125))
            .setStoragePeriod(10)
            .setInstruction("instruction100")
            .setType(PickupPointType.PICKUP_POINT)
            .setSchedule(List.of(workTime1(), workTime2()))
            .setCalendar(List.of())
            .build();
    }

    @Nonnull
    private static PickupPoint pickupPoint103() {
        return PickupPoint.builder()
            .setCode("CREATED_CODE103")
            .setName("CREATED_POINT103")
            .setAddress(location4())
            .setPhones(List.of())
            .setActive(true)
            .setCardAllowed(true)
            .setCashAllowed(false)
            .setType(PickupPointType.TERMINAL)
            .setSchedule(List.of())
            .setCalendar(List.of())
            .build();
    }

    @Nonnull
    private static Location location1() {
        return new Location.LocationBuilder("Россия", "Москва", "Московская область")
            .setStreet("Ленина")
            .setHouse("12")
            .setHousing("10")
            .setBuilding("34")
            .setRoom("12a")
            .setZipCode("649220")
            .setLocationId(54321)
            .build();
    }

    @Nonnull
    private static Location location2() {
        return new Location.LocationBuilder("Казахстан", "Воронеж", "Воронежская область")
            .setStreet("Сталина")
            .setHouse("12")
            .setHousing("15")
            .setBuilding("21")
            .setRoom("12a")
            .setZipCode("555555")
            .setLat(new BigDecimal("110"))
            .setLng(new BigDecimal("210"))
            .setLocationId(1234)
            .build();
    }

    @Nonnull
    private static Location location3() {
        return new Location.LocationBuilder("Россия", "Москва", "Московская область")
            .setStreet("Льва Толстого")
            .setHouse("16")
            .setRoom("105")
            .setZipCode("119021")
            .setLat(new BigDecimal("37.588144"))
            .setLng(new BigDecimal("55.733842"))
            .setLocationId(434)
            .build();
    }

    @Nonnull
    private static Location location4() {
        return new Location.LocationBuilder("Россия", "Москва", "Московская область")
            .setStreet("Льва Толстого")
            .setHouse("18")
            .setRoom("105")
            .setLat(new BigDecimal("37.588144"))
            .setZipCode("119021")
            .setLocationId(123)
            .build();
    }

    @Nonnull
    private static Location location5() {
        return new Location.LocationBuilder("Россия", "Новосибирск", "Новосибирская область")
            .setStreet("Николаева")
            .setHouse("11")
            .setLat(new BigDecimal("54.857955"))
            .setLng(new BigDecimal("83.111606"))
            .setZipCode("630090")
            .setLocationId(null)
            .build();
    }

    @Nonnull
    private static Location location6() {
        return new Location.LocationBuilder("Россия", "Москва", "Московская область")
            .setStreet("Льва Толстого")
            .setHouse("18")
            .setRoom("106")
            .setLat(new BigDecimal("37.588144"))
            .setZipCode("119021")
            .setLocationId(123)
            .build();
    }

    @Nonnull
    private static Location location100() {
        return new Location.LocationBuilder("Россия", "Москва", "Московская область")
            .setStreet("Капотня 5-й квартал")
            .setHouse("20")
            .setLat(new BigDecimal("55.631152"))
            .setLng(new BigDecimal("37.799472"))
            .setZipCode("109649")
            .build();
    }

    @Nonnull
    private static Phone phone1() {
        return new Phone.PhoneBuilder("555777")
            .setAdditional("333555")
            .build();
    }

    @Nonnull
    private static Phone phone2() {
        return new Phone.PhoneBuilder("666777")
            .build();
    }

    @Nonnull
    private static WorkTime workTime1() {
        return new WorkTime(
            1,
            List.of(
                TimeInterval.of(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                TimeInterval.of(LocalTime.of(13, 0), LocalTime.of(19, 0))
            )
        );
    }

    @Nonnull
    private static WorkTime workTime2() {
        return new WorkTime(
            2,
            List.of(
                TimeInterval.of(LocalTime.of(10, 0), LocalTime.of(13, 0)),
                TimeInterval.of(LocalTime.of(14, 0), LocalTime.of(20, 0))
            )
        );
    }

    @Nonnull
    private static WorkTime workTime3() {
        return new WorkTime(
            3,
            List.of(
                TimeInterval.of(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                TimeInterval.of(LocalTime.of(10, 0), LocalTime.of(10, 30)),
                TimeInterval.of(LocalTime.of(11, 0), LocalTime.of(11, 30)),
                TimeInterval.of(LocalTime.of(12, 0), LocalTime.of(12, 30)),
                TimeInterval.of(LocalTime.of(13, 0), LocalTime.of(13, 30)),
                TimeInterval.of(LocalTime.of(14, 0), LocalTime.of(14, 30)),
                TimeInterval.of(LocalTime.of(15, 0), LocalTime.of(15, 30)),
                TimeInterval.of(LocalTime.of(22, 0), LocalTime.of(22, 30)),
                TimeInterval.of(LocalTime.of(23, 0), LocalTime.of(23, 30))
            )
        );
    }

    @Nonnull
    private static Service service(ServiceType type) {
        return new Service(type, false, null, null, null, null);
    }

    @Nonnull
    private static DateBool dateBool(int dayOfMonth, boolean flag) {
        return new DateBool(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 12, dayOfMonth, 0, 0)), flag);
    }

    @SneakyThrows
    private void assertWorkingTimes() {
        transactionTemplate.execute(callback -> {
            assertJsonsAreEqual("data/executor/after/yt_outlet_working_time_1.json", getYtOutletWorkingDays(1));
            assertJsonsAreEqual("data/executor/after/yt_outlet_working_time_2.json", getYtOutletWorkingDays(2));
            assertJsonsAreEqual("data/executor/after/yt_outlet_working_time_3.json", getYtOutletWorkingDays(3));
            assertJsonsAreEqual(null, getYtOutletWorkingDays(4));
            assertJsonsAreEqual(null, getYtOutletWorkingDays(5));
            assertJsonsAreEqual(null, getYtOutletWorkingDays(6));
            assertJsonsAreEqual(null, getYtOutletWorkingDays(7));
            assertJsonsAreEqual(null, getYtOutletWorkingDays(8));
            assertJsonsAreEqual(null, getYtOutletWorkingDays(9));
            assertJsonsAreEqual("data/executor/after/yt_outlet_working_time_10.json", getYtOutletWorkingDays(10));
            assertJsonsAreEqual(null, getYtOutletWorkingDays(11));
            assertJsonsAreEqual(null, getYtOutletWorkingDays(12));
            assertJsonsAreEqual("data/executor/after/yt_outlet_working_time_13.json", getYtOutletWorkingDays(13));
            return null;
        });
    }

    @Nullable
    private String getYtOutletWorkingDays(long id) {
        return Optional.of(ytOutletRepository.getOne(id))
            .map(YtOutlet::getWorkingTime)
            .map(JsonNode::toString)
            .orElse(null);
    }

    @SneakyThrows
    private static void assertJsonsAreEqual(@Nullable String path, @Nullable String actualContent) {
        JSONAssert.assertEquals(
            Optional.ofNullable(path).map(IntegrationTestUtils::extractFileContent).orElse(null),
            actualContent,
            JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Nonnull
    private SimpleGeoObject geoObject(
        @Nullable String geoId,
        @Nullable String point,
        @Nullable String addressLine,
        @Nullable Kind kind
    ) {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder().withGeoid(geoId).withPoint(point).withKind(kind).build())
            .withAddressInfo(
                AddressInfo.newBuilder().withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .withAddressLine(addressLine).build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }

    @Nonnull
    private List<LocationFilter> getExpectedLocationFiltersForPartner13() {
        return List.of(
            LocationFilter.builder()
                .setLocationId(100L)
                .setCountry("Россия")
                .build(),
            LocationFilter.builder()
                .setLocationId(101L)
                .setCountry("Казахстан")
                .build()
        );
    }
}
