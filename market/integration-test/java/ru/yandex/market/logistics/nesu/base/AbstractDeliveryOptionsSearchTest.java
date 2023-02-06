package ru.yandex.market.logistics.nesu.base;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchResponse;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerDaysOffFilter;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerDaysOffFilterPartner;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.CalendarsFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.CourierScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.capacity.PartnerCapacityDayOffSearchResult;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.CalendarHolidaysResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.api.model.Dimensions;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsDestinationLocation;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterCost;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterSettings;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterShipment;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsLocation;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryType;
import ru.yandex.market.logistics.nesu.model.DeliveryOptionFactory;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffSearchFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockCourierSchedule;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createAddressDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.enabledPartnerExternalParam;
import static ru.yandex.market.logistics.nesu.utils.SenderAvailableDeliveriesUtils.mockGetSenderAvailableDeliveries;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
public abstract class AbstractDeliveryOptionsSearchTest extends AbstractDeliveryOptionsSearchVirtualPartnersCasesTest {

    @BeforeEach
    void setup() {
        doReturn(List.of(geoObject(LOCATION_TO_GEO_ID, LOCATION_ADDRESS))).when(geoSearchClient).find(LOCATION_ADDRESS);

        DeliverySearchResponse response = defaultResponse();
        when(deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().build())).thenReturn(response);
        mockPickupPointSearch(response);
        mockSearchPartners(List.of(
            new PartnerExternalParam(PartnerExternalParamType.IS_MULTIPLACES_SUPPORTED.name(), "", "1")
        ));

        when(lmsClient.getPartner(SORTING_CENTER_ID))
            .thenReturn(Optional.of(partner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER)));

        when(lmsClient.getLogisticsPoint(SORTING_CENTER_WAREHOUSE_ID)).thenReturn(Optional.of(sortingCenterWarehouse));
        mockGetWarehousesByPartnerIds(warehouse(600L, 400L), warehouse(601L, 420L), sortingCenterWarehouse);

        when(lomClient.searchShipments(any(ShipmentSearchFilter.class), safeRefEq(SHIPMENT_PAGEABLE)))
            .thenReturn(shipmentPageResult(List.of()));

        mockGetSenderAvailableDeliveries(
            lmsClient,
            partnerBuilder(SORTING_CENTER_ID, PartnerType.SORTING_CENTER)
                .params(List.of(enabledPartnerExternalParam(PartnerExternalParamType.IS_MULTIPLACES_SUPPORTED)))
                .build(),
            Set.of(400L, 410L, 420L).stream()
                .map(id ->
                    partnerBuilder(id, PartnerType.DELIVERY)
                        .params(List.of(enabledPartnerExternalParam(PartnerExternalParamType.IS_MULTIPLACES_SUPPORTED)))
                        .build()
                )
                .collect(Collectors.toList()),
            List.of(
                warehouse(600L, 400L),
                warehouse(601L, 420L),
                sortingCenterWarehouse
            )
        );

        doAnswer(i -> {
            TariffSearchFilter filter = i.getArgument(0);
            long tariffId = filter.getTariffIds().stream().mapToLong(Long::longValue).min().orElseThrow();
            return List.of(
                TariffDto.builder().id(tariffId).name("Tariff " + tariffId).build()
            );
        }).when(tarifficatorClient).searchTariffs(any(TariffSearchFilter.class));

        clock.setFixed(NOW, ZoneId.systemDefault());
    }

    @Test
    @DisplayName("У партнера нет расписания заборов")
    void noIntakeSchedule() throws Exception {
        when(lmsClient.getPartner(SORTING_CENTER_ID))
            .thenReturn(Optional.of(
                partnerBuilder(SORTING_CENTER_ID, PartnerType.DELIVERY)
                    .intakeSchedule(List.of()).build()
            ));
        mockSearchSenderWarehouse();

        DeliveryOptionsFilterShipment deliveryOptionsFilterShipment = new DeliveryOptionsFilterShipment();
        deliveryOptionsFilterShipment.setDate(LocalDate.of(2019, 8, 1));
        deliveryOptionsFilterShipment.setWarehouseId(SENDER_WAREHOUSE_ID);
        search(defaultFilter().andThen(f -> f.setShipment(deliveryOptionsFilterShipment)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_no_intake.json"));

        verifyLomSearchTodayShipmentApplications();
        verifyLomSearchShipments(LocalDate.of(2019, 8, 1));
    }

    @Test
    @DisplayName("Не передана локация отправления для магазина с более чем одним настроенным регионом")
    @DatabaseSetup(
        value = "/repository/delivery-options/sender_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void emptyLocationMoreThanOneRegionSettings() throws Exception {
        search(defaultFilter(), 6L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(
                "Не удалось определить региональные настройки. У сендера 6 есть настройки для нескольких регионов."
            ));

        verifyZeroInteractions(lmsClient);
    }

    @Test
    @DisplayName("Не передана локация отправления для магазина с одним настроенным регионом")
    @DatabaseSetup(
        value = "/repository/delivery-options/sender_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void emptyLocationOneRegionSetting() throws Exception {
        when(deliveryCalculatorClient.deliverySearch(
            defaultCalculatorRequest()
                .deliveryServiceIds(Set.of(400L, 420L))
                .senderId(7L)
                .locationFrom(213)
                .build())
        )
            .thenReturn(defaultResponse());

        search(defaultFilter(), 7L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_region_setting.json"));
    }

    @Test
    @DisplayName("Передана локация отправления для магазина с несколькими настроенными регионами")
    @DatabaseSetup(
        value = "/repository/delivery-options/sender_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void locationMoreThanOneRegionSettings() throws Exception {
        when(deliveryCalculatorClient.deliverySearch(
            defaultCalculatorRequest()
                .deliveryServiceIds(Set.of(400L, 420L))
                .senderId(6L)
                .locationFrom(213)
                .build())
        )
            .thenReturn(defaultResponse());

        search(defaultFilter().andThen(filter -> filter.setFrom(new DeliveryOptionsLocation().setGeoId(1))), 6L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_region_setting.json"));
    }

    @Test
    @DisplayName("Передана локация отправления, для которой нет настройки у магазина")
    @DatabaseSetup(
        value = "/repository/delivery-options/sender_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void locationNoSettingForRegion() throws Exception {
        search(defaultFilter().andThen(filter -> filter.setFrom(new DeliveryOptionsLocation().setGeoId(11117))), 7L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Нет региональных настроек для локации 11117 для сендера 7."));
    }

    @Test
    @DisplayName("Пустое расписание работы курьеров")
    void emptyCourierSchedule() throws Exception {
        mockCourierSchedule(lmsClient, LOCATION_TO_GEO_ID, Set.of(400L, 420L));

        when(deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().build()))
            .thenReturn(courierResponse());

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_empty_courier_schedule.json"));
    }

    @Test
    @DisplayName("Расписание работы курьеров сдвигает дату доставки")
    void calculatedDeliveryDateByCourierSchedule() throws Exception {
        when(deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().build()))
            .thenReturn(courierResponse());

        when(lmsClient.getCourierScheduleDays(safeRefEq(
            CourierScheduleFilter.newBuilder()
                .locationIds(Set.of(LOCATION_TO_GEO_ID))
                .partnerIds(Set.of(400L, 420L))
                .build()
        ))).thenReturn(List.of(
            courierSchedule(3, 400L).build(),
            courierSchedule(5, 420L).build()
        ));
        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_delivery_date_by_courier_schedule.json"));

        verify(lmsClient).getHolidays(safeRefEq(
            CalendarsFilter.builder()
                .calendarIds(List.of(2400L, 2420L, 2500L))
                .dateFrom(LocalDate.of(2019, 8, 1))
                .dateTo(LocalDate.of(2019, 9, 1))
                .build()
        ));
    }

    @Test
    @DisplayName("Расписание работы ПВЗ изменяет набор вариантов")
    void calculatedDeliveryDateByPickupPointSchedule() throws Exception {
        mockPickupPointSearch(defaultResponse(), id -> Set.of(
            scheduleDay(10 * id),
            scheduleDay(10 * id + 1)
        ));

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_delivery_date_by_pickup_schedule.json"));
    }

    @Test
    @DisplayName("Результат поиска без ПВЗ")
    void noPickupPointsResult() throws Exception {
        mockCourierSchedule(lmsClient, LOCATION_TO_GEO_ID, Set.of(420L));
        doReturn(DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOptionBuilder(11L, 420L, TariffType.COURIER, 2).build()
            ))
            .build())
            .when(deliveryCalculatorClient).deliverySearch(defaultCalculatorRequest().build());

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_no_pickup_points.json"));
    }

    @Test
    @DisplayName("Список опций доставки пустой, если поиск активных ПВЗ ничего не вернул")
    void emptyDeliveryOptionsInactivePickupPoints() throws Exception {
        Set<Long> pickupPointIds = defaultResponse().getDeliveryOptions()
            .stream()
            .map(DeliveryOption::getPickupPoints)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .ids(pickupPointIds)
            .active(true)
            .type(PointType.PICKUP_POINT)
            .build();
        when(lmsClient.getLogisticsPoints(refEq(filter))).thenReturn(List.of());
        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY, true));
    }

    @Test
    @DisplayName("Стоимость страховки по оценочной стоимости")
    void assessedValueInsurance() throws Exception {
        search(defaultFilter().andThen(filter ->
            filter.setCost(new DeliveryOptionsFilterCost().setAssessedValue(BigDecimal.valueOf(15000)))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_insurance.json"));
    }

    @Test
    @DisplayName(
        "Не показывать партнеров, без флага IS_MULTIPLACES_SUPPORTED, если в настройках showDisabledOptions=FALSE"
    )
    void showDisabledOptionsFalse() throws Exception {
        mockMultiplaceSupportedTestData();
        search(defaultFilter().andThen(f -> f.setPlaces(
            List.of(
                createDeliveryOptionsFilterPlace(d -> {
                }),
                createDeliveryOptionsFilterPlace(d -> {
                })
            )
        )))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_multiplace_supported.json"));
    }

    @Test
    @DisplayName("Показывать партнера без флага IS_MULTIPLACES_SUPPORTED, если в запросе есть только одно грузоместо")
    void showDisabledOptionsFalseOnePlace() throws Exception {
        mockMultiplaceSupportedTestData();
        search(defaultFilter().andThen(f -> f.setPlaces(
            List.of(createDeliveryOptionsFilterPlace(d -> {
            }))
        )))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_multiplace_one_place.json"));
    }

    @Test
    @DisplayName("Показывать партнера без флага IS_MULTIPLACES_SUPPORTED, если нет грузомест в запросе")
    void showDisabledOptionsFalseNoPlaces() throws Exception {
        mockMultiplaceSupportedTestData();
        search(defaultFilter().andThen(f -> f.setPlaces(null)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_multiplace_one_place.json"));
    }

    @Test
    @DisplayName(
        "Указывать причину, почему опции недоступны для партнеров, у которых нет флага IS_MULTIPLACES_SUPPORTED"
    )
    void showDisabledOptionsTrue() throws Exception {
        mockMultiplaceSupportedTestData();
        search(defaultFilter().andThen(
            f -> f
                .setSettings(new DeliveryOptionsFilterSettings().setShowDisabledOptions(true))
                .setPlaces(List.of(
                    createDeliveryOptionsFilterPlace(d -> {
                    }),
                    createDeliveryOptionsFilterPlace(d -> {
                    })
                ))
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_multiplace_enabled.json"));
    }

    @Test
    @DisplayName(
        "Показывать партнера без флага IS_MULTIPLACES_SUPPORTED,"
            + " если в запросе есть только одно грузоместо showDisabledOptions = true"
    )
    void showDisabledOptionsTrueOnePlace() throws Exception {
        mockMultiplaceSupportedTestData();
        search(defaultFilter().andThen(
            f -> f
                .setSettings(new DeliveryOptionsFilterSettings().setShowDisabledOptions(true))
                .setPlaces(List.of(createDeliveryOptionsFilterPlace(d -> {
                })))
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_multiplace_one_place.json"));
    }

    @Test
    @DisplayName(
        "Показывать партнера без флага IS_MULTIPLACES_SUPPORTED,"
            + " если нет грузомест в запросе showDisabledOptions = true"
    )
    void showDisabledOptionsTrueNoPlaces() throws Exception {
        mockMultiplaceSupportedTestData();
        search(defaultFilter().andThen(
            f -> f
                .setSettings(new DeliveryOptionsFilterSettings().setShowDisabledOptions(true))
                .setPlaces(null)
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_multiplace_one_place.json"));
    }

    @Test
    @DisplayName("Поиск вариантов по общим габаритам заказа")
    void searchByOrderDimensions() throws Exception {
        mockMultiplaceSupportedTestData();
        when(deliveryCalculatorClient.deliverySearch(
            defaultCalculatorRequest()
                .height(100)
                .width(150)
                .length(200)
                .weight(BigDecimal.valueOf(15L))
                .deliveryServiceIds(Set.of(400L, 420L)).build()
        ))
            .thenReturn(courierResponse());

        search(defaultFilter().andThen(f -> f.setDimensions(
            new Dimensions()
                .setHeight(100)
                .setWidth(150)
                .setLength(200)
                .setWeight(BigDecimal.valueOf(15L))
        )))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_multiplace_one_place.json"));
    }

    @Test
    @DisplayName("Максимальная стоимость страховки")
    void assessedValueMax() throws Exception {
        search(defaultFilter().andThen(filter ->
            filter.setCost(new DeliveryOptionsFilterCost().setAssessedValue(BigDecimal.valueOf(1500000)))))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY, true));
    }

    @Test
    @DisplayName("Стоимость товаров")
    void itemsSum() throws Exception {
        when(deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().offerPrice(1000000L).build()))
            .thenReturn(defaultResponse());

        search(defaultFilter().andThen(filter ->
            filter.setCost(new DeliveryOptionsFilterCost().setItemsSum(BigDecimal.valueOf(10000)))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_items_sum.json"));
    }

    @Test
    @DisplayName("Модифицированная стоимость доставки")
    void modifiedDeliveryCost() throws Exception {
        doReturn(
            DeliverySearchResponse.builder()
                .deliveryOptions(List.of(
                    deliveryOptionBuilder(10L, 400L, TariffType.POST, 2).cost(200_00).build()
                ))
                .build()
        )
            .when(deliveryCalculatorClient).deliverySearch(defaultCalculatorRequest().build());

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_modified_delivery_cost.json"));
    }

    @Test
    @DisplayName("Стоимость доставки для клиента указана магазином")
    void manualDeliveryForCustomer() throws Exception {
        search(defaultFilter().andThen(filter ->
            filter.setCost(new DeliveryOptionsFilterCost().setManualDeliveryForCustomer(BigDecimal.valueOf(1000)))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_manual_delivery.json"));
    }

    @Test
    @DisplayName("Учет времени комплектации в СЦ")
    void handlingTime() throws Exception {
        when(lmsClient
            .searchPartnerRelation(refEq(PartnerRelationFilter.newBuilder().fromPartnerId(SORTING_CENTER_ID).build())))
            .thenReturn(List.of(
                PartnerRelationEntityDto.newBuilder()
                    .handlingTime(2)
                    .intakeSchedule(Set.of())
                    .toPartnerId(420L)
                    .enabled(true)
                    .build()
            ));

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_handling_time.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Учет времени комплектации в СЦ, проверка начала интервала отгрузки из СЦ в СД")
    @MethodSource
    void sortingCenterShipmentStarted(
        @SuppressWarnings("unused") String displayName,
        @Nullable Integer sortingCenterLocation,
        Integer minutesDeltaFromNow,
        String resultFilePath
    ) throws Exception {
        when(lmsClient.getPartner(SORTING_CENTER_ID))
            .thenReturn(Optional.of(
                partnerBuilder(SORTING_CENTER_ID, PartnerType.SORTING_CENTER)
                    .intakeSchedule(
                        LongStream.rangeClosed(1, 7)
                            .mapToObj(this::scheduleDay)
                            .collect(Collectors.toList())
                    )
                    .build()
            ));

        doReturn(List.of(
            warehouse(600L, 400L),
            warehouse(601L, 420L),
            warehouseBuilder(SORTING_CENTER_WAREHOUSE_ID, SORTING_CENTER_ID)
                .address(Address.newBuilder().locationId(sortingCenterLocation).build())
                .schedule(
                    LongStream.rangeClosed(1, 7)
                        .mapToObj(this::scheduleDay)
                        .collect(Collectors.toSet())
                )
                .businessId(42L)
                .build()
        ))
            .when(lmsClient).getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(400L, 420L, SORTING_CENTER_ID))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        );

        LocalTime intakeStartTime = LocalTime.now(clock).plusMinutes(minutesDeltaFromNow);
        when(lmsClient
            .searchPartnerRelation(refEq(PartnerRelationFilter.newBuilder().fromPartnerId(SORTING_CENTER_ID).build())))
            .thenReturn(List.of(
                PartnerRelationEntityDto.newBuilder()
                    .handlingTime(0)
                    .intakeSchedule(Set.of(
                        new ScheduleDayResponse(1L, 6, intakeStartTime, intakeStartTime.plusHours(1))
                    ))
                    .toPartnerId(420L)
                    .enabled(true)
                    .build()
            ));

        search(
            defaultFilter()
                .andThen(filter -> filter.getShipment().setDate(TODAY))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(resultFilePath));
    }

    @Nonnull
    private static Stream<Arguments> sortingCenterShipmentStarted() {
        return Stream.of(
            Arguments.of(
                "Не указана локация, наступил интервал отгрузки",
                null,
                -30,
                "controller/delivery-options/search_result_sc_ds_shipment_started.json"
            ),
            Arguments.of(
                "Неизвестная локация, наступил интервал отгрузки",
                214,
                -30,
                "controller/delivery-options/search_result_sc_ds_shipment_started.json"
            ),
            Arguments.of(
                "Локация, для которой не указано значение timezone offset, наступил интервал отгрузки",
                121908,
                -30,
                "controller/delivery-options/search_result_sc_ds_shipment_started.json"
            ),
            Arguments.of(
                "Не указана локация, интервал отгрузки еще не наступил (граничное значение)",
                null,
                0,
                "controller/delivery-options/search_result_sc_ds_before_shipment_started.json"
            ),
            Arguments.of(
                "Неизвестная локация, интервал отгрузки еще не наступил",
                214,
                30,
                "controller/delivery-options/search_result_sc_ds_before_shipment_started.json"
            ),
            Arguments.of(
                "Локация, для которой не указано значение timezone offset, интервал отгрузки еще не наступил",
                121908,
                30,
                "controller/delivery-options/search_result_sc_ds_before_shipment_started.json"
            )
        );
    }

    @Test
    @DisplayName("Отгрузка партнеру не в СЦ (только стандартные отгрузки)")
    void noSortingCenterShipment() throws Exception {
        search(
            defaultFilter().andThen(f ->
                Objects.requireNonNull(f.getShipment())
                    .setPartnerId(100L)
                    .setIncludeNonDefault(false)
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_no_sorting.json"));

        verifySearchSortingCenterPartnerRelations();
    }

    @Test
    @DisplayName("Отгрузка партнеру не в СЦ (без флага 'нестандартные отгрузки')")
    void noSortingCenterShipmentNoIncludeNonDefaultFlag() throws Exception {
        search(defaultFilter().andThen(f -> Objects.requireNonNull(f.getShipment()).setPartnerId(100L)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_no_sorting.json"));

        verifySearchSortingCenterPartnerRelations();
    }

    @Test
    @DisplayName("Фильтр по пунктам выдачи заказов")
    void pickupPointsFilter() throws Exception {
        List<Long> pickupPointIds = List.of(
            40000L,
            40002L
        );

        DeliverySearchResponse response = newPostDeliveryOptions(pickupPointIds);
        mockPickupPointSearch(response);

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(defaultCalculatorRequest()
                .pickupPoints(Set.copyOf(pickupPointIds))
                .build());

        search(defaultFilter().andThen(filter -> filter.getTo().setPickupPointIds(Set.copyOf(pickupPointIds))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_pickup_points.json"));
    }

    @Test
    @DisplayName("Фильтр по индексу почтового отделения")
    void postalCodeFilter() throws Exception {
        String postalCode = "000100";
        long pickUpPointId = 100L;

        DeliverySearchResponse response = newPostDeliveryOptions(List.of(pickUpPointId));

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(defaultCalculatorRequest()
                .postalCode(postalCode)
                .build());

        mockPostOffice(pickUpPointId);

        search(defaultFilter().andThen(filter -> filter.getTo().setPostalCode(postalCode)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_postal_code.json"));
    }

    @Nonnull
    private static Stream<Arguments> assessedValueSource() {
        return Stream.of(
            Arguments.of(
                "Вычисляем минимально допустимую объявленную ценность, если ее не указали",
                null,
                0,
                "controller/delivery-options/search_result_with_new_assessed_value.json"
            ),
            Arguments.of(
                "Корректировка объявленной ценности до минимально допустимой",
                BigDecimal.valueOf(351),
                0,
                "controller/delivery-options/search_result_with_new_assessed_value.json"
            ),
            Arguments.of(
                "Не меняем объявленную ценность, если она выше минимально допустимой",
                BigDecimal.valueOf(1800),
                0,
                "controller/delivery-options/search_result_with_no_changed_assessed_value.json"
            ),
            Arguments.of(
                "Страховка меньше минимальной, корректировка объявленной ценности до минимально допустимой",
                BigDecimal.valueOf(351),
                50_00,
                "controller/delivery-options/search_result_with_less_then_min_insurance.json"
            ),
            Arguments.of(
                "Страховка меньше минимальной, не меняем объявленную ценность, если она выше минимально допустимой",
                BigDecimal.valueOf(1800),
                50_00,
                "controller/delivery-options/search_result_with_less_then_min_insurance_no_change_assessed_value.json"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("assessedValueSource")
    @DisplayName("Найти варианты доставки почтой с расчетом новой оценочной стоимости")
    @DatabaseSetup(
        value = "/repository/delivery-options/update/set_customer_pay_for_post_insurance.xml",
        type = DatabaseOperation.UPDATE
    )
    void calculateNewAssessedValue(
        @SuppressWarnings("unused") String caseName,
        @Nullable BigDecimal originalAssessedValue,
        long minInsurance,
        String path
    ) throws Exception {
        String postalCode = "000100";
        long pickUpPointId = 100L;

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOptionBuilder(10L, 400L, TariffType.POST, 2)
                    .services(
                        List.of(
                            DeliveryOptionFactory.deliveryServiceBuilder().build(),
                            DeliveryOptionFactory.insuranceServiceBuilder().minPrice(minInsurance).build(),
                            DeliveryOptionFactory.cashServiceBuilder().build(),
                            DeliveryOptionFactory.returnServiceBuilder().build()
                        )
                    )
                    .pickupPoints(List.of(pickUpPointId))
                    .build()
            ))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .offerPrice(62550L)
                    .postalCode(postalCode)
                    .build()
            );

        mockPostOffice(pickUpPointId);
        mockEnabledParams(
            PartnerExternalParamType.ASSESSED_VALUE_TOTAL_CHECK,
            PartnerExternalParamType.IS_MULTIPLACES_SUPPORTED
        );

        search(defaultFilter()
            .andThen(filter -> filter.getTo().setPostalCode(postalCode))
            .andThen(filter -> filter.setCost(
                new DeliveryOptionsFilterCost()
                    .setAssessedValue(originalAssessedValue)
                    .setItemsSum(BigDecimal.valueOf(625.50))
                )
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(path));
    }

    @Test
    @DisplayName("Фильтр по индексу почтового отделения, способ доставки не Почтой России")
    void postalCodeFilterForPickupDeliveryType() throws Exception {
        doReturn(courierResponse())
            .when(deliveryCalculatorClient)
            .deliverySearch(defaultCalculatorRequest().tariffType(TariffType.COURIER).build());

        mockCourierSchedule(lmsClient, LOCATION_TO_GEO_ID, Set.of(400L, 420L));

        search(
            defaultFilter()
                .andThen(filter -> filter.setDeliveryType(DeliveryType.COURIER))
                .andThen(filter -> filter.getTo().setPostalCode("000100"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_empty_courier_schedule.json"));
    }

    @Test
    @DisplayName("Фильтр по службам доставки")
    void deliveryServiceFilter() throws Exception {
        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();
        when(deliveryCalculatorClient.deliverySearch(
            defaultCalculatorRequest()
                .deliveryServiceIds(Set.of(420L))
                .build()
        )).thenReturn(response);
        mockPickupPointSearch(response);
        search(defaultFilter().andThen(filter -> {
            DeliveryOptionsFilterShipment shipment = new DeliveryOptionsFilterShipment();
            shipment
                .setIncludeNonDefault(true)
                .setPartnerId(420L)
                .setDate(LocalDate.of(2019, 8, 1));
            filter.setShipment(shipment);
        }))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_delivery_service.json"));
    }

    @Test
    @DisplayName("Учет отключенных служб доставки")
    void disabledDeliveryServices() throws Exception {
        doReturn(List.of(partner(420, PartnerType.DELIVERY), partner(SORTING_CENTER_ID, PartnerType.SORTING_CENTER)))
            .when(lmsClient)
            .searchPartners(createFilter());

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(11L, 420L, TariffType.PICKUP, 2)))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(defaultCalculatorRequest().deliveryServiceIds(Set.of(420L)).build());

        mockPickupPointSearch(response);
        mockGetWarehousesByPartnerIds(warehouse(601L, 420L), sortingCenterWarehouse);

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_delivery_service_default.json"));
    }

    @Test
    @DisplayName("Фильтр по способу доставки")
    void deliveryTypeFilter() throws Exception {
        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(10L, 400L, TariffType.POST, 2)))
            .build();
        when(deliveryCalculatorClient.deliverySearch(
            defaultCalculatorRequest()
                .deliveryServiceIds(Set.of(400L))
                .tariffType(TariffType.POST)
                .build()
        )).thenReturn(response);

        mockPickupPointSearch(response);

        search(defaultFilter().andThen(filter -> filter.setDeliveryType(DeliveryType.POST)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_delivery_type.json"));
    }

    @Test
    @DisplayName("Фильтр по отключенному способу доставки")
    void disabledDeliveryTypeFilter() throws Exception {
        search(defaultFilter().andThen(filter -> filter.setDeliveryType(DeliveryType.PICKUP)))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY, true));
    }

    @Test
    @DisplayName("Фильтр по способу отгрузки")
    void shipmentTypeFilter() throws Exception {
        search(defaultFilter().andThen(filter -> {
            DeliveryOptionsFilterShipment shipment = new DeliveryOptionsFilterShipment();
            shipment
                .setIncludeNonDefault(true)
                .setType(ShipmentType.WITHDRAW)
                .setDate(LocalDate.of(2019, 8, 1));
            filter.setShipment(shipment);
        }))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_shipment_type.json"));
    }

    @Test
    @DisplayName("Фильтр без даты отгрузки")
    void shipmentWithoutDate() throws Exception {
        search(defaultFilter().andThen(filter -> {
            DeliveryOptionsFilterShipment shipment = new DeliveryOptionsFilterShipment();
            shipment.setType(ShipmentType.WITHDRAW);
            filter.setShipment(shipment);
        }))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_shipment_no_date.json"));
    }

    @Test
    @DisplayName("Фильтр с указанием доступного склада отправки, находящегося в области")
    void shipmentWithWarehouseIdSuccess() throws Exception {
        long marketId = 2000L;
        long deliveryServiceId = 420L;
        LocalDate fromDate = LocalDate.of(2019, 2, 3);
        LocalDate toDate = LocalDate.of(2019, 2, 6);

        doReturn(
            shipmentPageResult(List.of(
                ShipmentSearchDto.builder()
                    .shipmentDate(fromDate)
                    .shipmentType(ru.yandex.market.logistics.lom.model.enums.ShipmentType.WITHDRAW)
                    .marketIdFrom(marketId)
                    .warehouseFrom(SENDER_WAREHOUSE_ID)
                    .partnerIdTo(deliveryServiceId)
                    .build(),
                ShipmentSearchDto.builder()
                    .shipmentDate(toDate)
                    .shipmentType(ru.yandex.market.logistics.lom.model.enums.ShipmentType.WITHDRAW)
                    .marketIdFrom(marketId)
                    .warehouseFrom(SENDER_WAREHOUSE_ID)
                    .partnerIdTo(SORTING_CENTER_ID)
                    .build()
            ))
        ).when(lomClient).searchShipments(
            safeRefEq(ShipmentSearchFilter.builder()
                .marketIdFrom(marketId)
                .warehousesFrom(Set.of(SENDER_WAREHOUSE_ID))
                .fromDate(fromDate)
                .toDate(toDate)
                .build()),
            safeRefEq(SHIPMENT_PAGEABLE)
        );

        mockSearchSenderWarehouse(point -> point.address(createAddressDto(10716)));

        search(defaultFilter().andThen(filter -> {
            DeliveryOptionsFilterShipment shipment = new DeliveryOptionsFilterShipment();
            shipment
                .setIncludeNonDefault(true)
                .setWarehouseId(1L)
                .setType(ShipmentType.WITHDRAW);
            filter.setShipment(shipment);
        }))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_shipment_with_warehouse_id.json"));

        verifyLomSearchShipments(fromDate, toDate);
        verifyLomSearchTodayShipmentApplications();
    }

    @Test
    @DisplayName("Фильтр с указанием полной предоплаты")
    void fullyPrepaidShipments() throws Exception {
        search(defaultFilter().andThen(filter -> filter.setCost(new DeliveryOptionsFilterCost().setFullyPrepaid(true))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_fully_prepaid.json"));
    }

    @Test
    @DisplayName("Фильтрация ПВЗ для непредоплаченного заказа")
    @DatabaseSetup(
        value = "/repository/delivery-options/pickup_delivery_service.xml",
        type = DatabaseOperation.INSERT
    )
    void filterPickupPointsForNonprepaidOrder() throws Exception {
        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOptionBuilder(11L, 420L, TariffType.PICKUP, 2)
                    .pickupPoints(List.of(121L, 122L))
                    .build(),
                deliveryOptionBuilder(12L, 421L, TariffType.PICKUP, 2)
                    .pickupPoints(List.of(123L, 124L, 125L))
                    .build()
            ))
            .build();

        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(
                defaultCalculatorRequest()
                    .senderId(2000L)
                    .deliveryServiceIds(Set.of(420L, 421L))
                    .locationsTo(Set.of(213))
                    .tariffType(TariffType.PICKUP)
                    .pickupPoints(Set.of(121L, 122L, 123L, 124L, 125L))
                    .build()
            );

        doReturn(List.of(
            pickupPointBuilder(121L, 420L, true, false, true).build(),
            pickupPointBuilder(122L, 420L, false, true, true).build(),
            pickupPointBuilder(123L, 421L, true, true, true).build(),
            pickupPointBuilder(124L, 421L, false, false, true).build(),
            pickupPointBuilder(125L, 421L, false, false, false).build()
        )).
            when(lmsClient).getLogisticsPoints(
            refEq(LogisticsPointFilter.newBuilder()
                .type(PointType.PICKUP_POINT)
                .ids(Set.of(121L, 122L, 123L, 124L, 125L))
                .active(true)
                .build()
            )
        );

        mockGetWarehousesByIds(
            sortingCenterWarehouse,
            warehouse(600L, 400L),
            warehouse(601L, 420L),
            warehouse(1L, 420L),
            warehouse(2L, 421L)
        );

        DeliveryOptionsDestinationLocation to = new DeliveryOptionsDestinationLocation().setPickupPointIds(
            Set.of(121L, 122L, 123L, 124L, 125L)
        );
        to.setGeoId(213);

        search(
            defaultFilter().andThen(
                filter -> filter.setCost(new DeliveryOptionsFilterCost().setFullyPrepaid(false))
                    .setDeliveryType(DeliveryType.PICKUP)
                    .setTo(to)
            ),
            2000L
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_nonprepaid.json"));
    }

    @Test
    @DisplayName("Один тариф для одной СД с несколькими складами, без склада по умолчанию")
    @DatabaseSetup("/repository/delivery-options/no_default_warehouse.xml")
    void multipleTariffsForSingleDS() throws Exception {
        mockGetWarehousesByIds(sortingCenterWarehouse, warehouse(600L, 440L), warehouse(601L, 440L));
        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(Set.of(440L), null, Set.of(PartnerStatus.ACTIVE))))
            .thenReturn(List.of(partner(440L, PartnerType.DELIVERY)));

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(12L, 440L, TariffType.PICKUP, 2)))
            .build();
        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(defaultCalculatorRequest().deliveryServiceIds(Set.of(440L)).build());
        mockPickupPointSearch(response);
        mockGetWarehousesByPartnerIds(warehouse(600L, 440L), warehouse(601L, 440L), sortingCenterWarehouse);

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/single_tariff_for_ds_without_default_warehouse.json"));
    }

    @Test
    @DisplayName("Использование склада по умолчанию")
    @DatabaseSetup("/repository/delivery-options/default_warehouse.xml")
    void singleTariffForSingleDsImport() throws Exception {
        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(Set.of(440L), null, Set.of(PartnerStatus.ACTIVE))))
            .thenReturn(List.of(partner(440L, PartnerType.DELIVERY)));

        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(12L, 440L, TariffType.PICKUP, 2)))
            .build();
        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(defaultCalculatorRequest().deliveryServiceIds(Set.of(440L)).build());
        mockPickupPointSearch(response);

        mockGetWarehousesByIds(warehouse(48L, 440L));
        mockGetWarehousesByIds(warehouse(48L, 440L), sortingCenterWarehouse);

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/single_tariff_for_ds_with_default_warehouse.json"));
    }

    @Test
    @DisplayName("Использование любого склада при отсутствии склада по умолчанию")
    @DatabaseSetup("/repository/delivery-options/default_warehouse.xml")
    void defaultWarehouseIsNotFound() throws Exception {
        when(lmsClient.searchPartners(LmsFactory.createPartnerFilter(Set.of(440L), null, Set.of(PartnerStatus.ACTIVE))))
            .thenReturn(List.of(partner(440L, PartnerType.DELIVERY)));
        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(deliveryOption(12L, 440L, TariffType.PICKUP, 2)))
            .build();
        doReturn(response)
            .when(deliveryCalculatorClient)
            .deliverySearch(defaultCalculatorRequest().deliveryServiceIds(Set.of(440L)).build());
        mockPickupPointSearch(response);
        mockGetWarehousesByIds(warehouse(48L, 440L), sortingCenterWarehouse);
        mockGetWarehousesByPartnerIds(warehouse(600L, 440L), warehouse(601L, 440L), sortingCenterWarehouse);

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/single_tariff_for_ds_without_default_warehouse.json"));
    }

    @Test
    @DisplayName("Отсутствует расписание работы курьера")
    void noCourierSchedule() throws Exception {
        when(deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().build()))
            .thenReturn(
                DeliverySearchResponse.builder()
                    .deliveryOptions(List.of(
                        deliveryOption(10L, 400L, TariffType.COURIER, 1),
                        deliveryOption(11L, 420L, TariffType.COURIER, 2)
                    ))
                    .build()
            );
        mockSearchSenderWarehouse();

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));

        verify(lmsClient).getHolidays(safeRefEq(
            CalendarsFilter.builder()
                .calendarIds(List.of(2400L, 2420L, 2500L))
                .dateFrom(LocalDate.of(2019, 2, 2))
                .dateTo(LocalDate.of(2019, 3, 2))
                .build()
        ));
        verifyLomSearchTodayShipmentApplications();
    }

    @Test
    @DisplayName("Выходные дни курьеров")
    void courierHolidays() throws Exception {
        when(deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().build()))
            .thenReturn(courierResponse());

        when(lmsClient.getCourierScheduleDays(safeRefEq(
            CourierScheduleFilter.newBuilder()
                .locationIds(Set.of(LOCATION_TO_GEO_ID))
                .partnerIds(Set.of(400L, 420L))
                .build()
        ))).thenReturn(List.of(
            courierSchedule(3, 400L).calendarId(1L).build(),
            courierSchedule(5, 420L).calendarId(2L).build()
        ));

        when(lmsClient.getHolidays(safeRefEq(
            CalendarsFilter.builder()
                .calendarIds(List.of(1L, 2L))
                .dateFrom(LocalDate.of(2019, 8, 1))
                .dateTo(LocalDate.of(2019, 9, 1))
                .build()
        ))).thenReturn(List.of(
            CalendarHolidaysResponse.builder().id(2L).days(List.of(LocalDate.of(2019, 8, 9))).build()
        ));

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_delivery_date_by_courier_holidays.json"));
    }

    @Test
    @DisplayName("Выходные дни курьеров применяются для доставки почтой и в ПВЗ")
    void courierHolidaysForPickupPoints() throws Exception {
        mockPickupPointSearch(
            defaultResponse(),
            id -> LongStream.rangeClosed(1, 7).mapToObj(this::scheduleDay).collect(Collectors.toSet())
        );

        when(lmsClient.getCourierScheduleDays(safeRefEq(
            CourierScheduleFilter.newBuilder()
                .locationIds(Set.of(LOCATION_TO_GEO_ID))
                .partnerIds(Set.of(400L, 420L))
                .build()
        ))).thenReturn(List.of(
            courierSchedule(3, 400L).calendarId(1L).build(),
            courierSchedule(5, 420L).calendarId(2L).build()
        ));
        when(lmsClient.getHolidays(safeRefEq(
            CalendarsFilter.builder()
                .calendarIds(List.of(1L, 2L))
                .dateFrom(LocalDate.of(2019, 8, 1))
                .dateTo(LocalDate.of(2019, 9, 1))
                .build()
        ))).thenReturn(List.of(
            CalendarHolidaysResponse.builder().id(1L).days(List.of(LocalDate.of(2019, 8, 5))).build(),
            CalendarHolidaysResponse.builder().id(2L).days(List.of(LocalDate.of(2019, 8, 6))).build()
        ));

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_delivery_date_by_pickup_holidays.json"));
    }

    @Test
    @DisplayName("Дэй-оффы партнеров")
    void partnerDaysOff() throws Exception {
        List<ScheduleDayResponse> fullSchedule = fullSchedule();

        doReturn(List.of(
            partnerBuilder(400, PartnerType.DELIVERY).intakeSchedule(fullSchedule).build()
        )).when(lmsClient).searchPartners(
            LmsFactory.createPartnerFilter(Set.of(400L), null, Set.of(PartnerStatus.ACTIVE))
        );

        doReturn(List.of(
            warehouseBuilder(SORTING_CENTER_WAREHOUSE_ID, SORTING_CENTER_ID).schedule(Set.copyOf(fullSchedule)).build()
        ))
            .when(lmsClient)
            .getLogisticsPoints(refEq(
                LogisticsPointFilter.newBuilder()
                    .type(PointType.WAREHOUSE)
                    .partnerIds(Set.of(400L, SORTING_CENTER_ID))
                    .active(true)
                    .build()
            ));

        PartnerResponse sortingCenter = partnerBuilder(SORTING_CENTER_ID, PartnerType.SORTING_CENTER)
            .intakeSchedule(fullSchedule)
            .build();
        doReturn(Optional.of(sortingCenter)).when(lmsClient).getPartner(SORTING_CENTER_ID);

        when(lmsClient.searchDaysOff(any(PartnerDaysOffFilter.class))).thenReturn(List.of(
            PartnerCapacityDayOffSearchResult.builder()
                .partnerId(SORTING_CENTER_ID)
                .days(List.of(LocalDate.of(2019, 8, 1)))
                .build(),
            PartnerCapacityDayOffSearchResult.builder()
                .partnerId(400L)
                .deliveryType(ru.yandex.market.logistics.management.entity.type.DeliveryType.POST)
                .days(List.of(LocalDate.of(2019, 8, 2)))
                .build()
        ));

        DeliverySearchResponse response = DeliverySearchResponse.builder().deliveryOptions(List.of(
            deliveryOption(1L, 400L, TariffType.POST, 4)
        )).build();
        DeliverySearchRequest request = defaultCalculatorRequest()
            .deliveryServiceIds(Set.of(400L))
            .tariffType(TariffType.POST)
            .build();
        when(deliveryCalculatorClient.deliverySearch(request)).thenReturn(response);
        mockPickupPointSearch(response);

        search(defaultFilter().andThen(f -> {
            DeliveryOptionsFilterShipment shipment = new DeliveryOptionsFilterShipment();
            shipment.setIncludeNonDefault(true)
                .setPartnerId(SORTING_CENTER_ID)
                .setType(ShipmentType.IMPORT)
                .setDate(LocalDate.of(2019, 8, 1));
            f.setShipment(shipment);
            f.setDeliveryType(DeliveryType.POST);
        }))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_partner_days_off.json"));

        ArgumentCaptor<PartnerDaysOffFilter> filterCaptor = ArgumentCaptor.forClass(PartnerDaysOffFilter.class);
        verify(lmsClient).searchDaysOff(filterCaptor.capture());
        softly.assertThat(filterCaptor.getValue()).usingRecursiveComparison().isEqualTo(
            PartnerDaysOffFilter.builder()
                .platformClientId(3L)
                .locationFrom(1)
                .locationsTo(Set.of(LOCATION_TO_GEO_ID))
                .dateFrom(LocalDate.of(2019, 8, 1))
                .dateTo(LocalDate.of(2019, 9, 1))
                .partners(List.of(
                    PartnerDaysOffFilterPartner.builder()
                        .partnerId(400L)
                        .deliveryType(ru.yandex.market.logistics.management.entity.type.DeliveryType.POST)
                        .build(),
                    PartnerDaysOffFilterPartner.builder()
                        .partnerId(SORTING_CENTER_ID)
                        .deliveryType(ru.yandex.market.logistics.management.entity.type.DeliveryType.POST)
                        .build()
                ))
                .build()
        );
    }

    @Test
    @DisplayName("Выходные партнёров")
    void partnerHoliday() throws Exception {
        when(lmsClient.getHolidays(refEq(
            CalendarsFilter.builder()
                .calendarIds(List.of(2400L, 2420L, 2500L))
                .dateFrom(LocalDate.of(2019, 8, 1))
                .dateTo(LocalDate.of(2019, 9, 1))
                .build()
        ))).thenReturn(List.of(
            CalendarHolidaysResponse.builder()
                .id(2420L)
                .days(List.of(LocalDate.of(2019, 8, 2)))
                .build(),
            CalendarHolidaysResponse.builder()
                .id(2500L)
                .days(List.of(LocalDate.of(2019, 8, 1)))
                .build()
        ));

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_holidays.json"));
    }

    @Test
    @DisplayName("Выходные партнёров, несколько вариантов на партнёра")
    void partnerHolidayMultiplePartners() throws Exception {
        DeliverySearchResponse response = DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOption(10L, 420L, TariffType.POST, 1),
                deliveryOption(11L, 420L, TariffType.PICKUP, 2)
            ))
            .build();
        when(deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().build()))
            .thenReturn(response);

        mockPickupPointSearch(response);

        when(lmsClient.getHolidays(refEq(
            CalendarsFilter.builder()
                .calendarIds(List.of(2420L, 2500L))
                .dateFrom(LocalDate.of(2019, 8, 1))
                .dateTo(LocalDate.of(2019, 9, 1))
                .build()
        ))).thenReturn(List.of(
            CalendarHolidaysResponse.builder()
                .id(2420L)
                .days(List.of(LocalDate.of(2019, 8, 2)))
                .build(),
            CalendarHolidaysResponse.builder()
                .id(2500L)
                .days(List.of(LocalDate.of(2019, 8, 1)))
                .build()
        ));

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_multiple_per_partner.json"));
    }

    @Test
    @DisplayName("Поиск опций доставки собственной СД")
    void ownDeliveryOptions() throws Exception {
        when(lmsClient.searchPartners(getFilterForOwnDeliveries()))
            .thenReturn(List.of(partner(430, PartnerType.OWN_DELIVERY)));

        when(deliveryCalculatorClient
            .deliverySearch(defaultCalculatorRequest().deliveryServiceIds(Set.of(400L, 420L, 430L)).build()))
            .thenReturn(
                DeliverySearchResponse.builder()
                    .deliveryOptions(
                        List.of(deliveryOptionBuilder(13L, 430L, TariffType.COURIER, 1).build())
                    ).build()
            );

        search(defaultFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_own_delivery.json"));
    }

    @Test
    @DisplayName("Желаемая дата доставки")
    void desiredDeliveryDate() throws Exception {
        List<ScheduleDayResponse> fullSchedule = fullSchedule();
        doReturn(List.of(
            partnerBuilder(400, PartnerType.DELIVERY).intakeSchedule(fullSchedule).build(),
            partnerBuilder(420, PartnerType.DELIVERY).intakeSchedule(fullSchedule).build(),
            partnerBuilder(SORTING_CENTER_ID, PartnerType.SORTING_CENTER).intakeSchedule(fullSchedule).build()
        ))
            .when(lmsClient)
            .searchPartners(createFilter());

        mockPickupPointSearch(
            defaultResponse(),
            id -> LongStream.rangeClosed(1, 7).mapToObj(this::scheduleDay).collect(Collectors.toSet())
        );

        Set<ScheduleDayResponse> fullScheduleSet = Set.copyOf(fullSchedule);
        mockGetWarehousesByPartnerIds(
            warehouseBuilder(600L, 400L).schedule(fullScheduleSet).build(),
            warehouseBuilder(601L, 420L).schedule(fullScheduleSet).build(),
            warehouseBuilder(601L, 420L).schedule(fullScheduleSet).build(),
            warehouseBuilder(SORTING_CENTER_WAREHOUSE_ID, SORTING_CENTER_ID).schedule(fullScheduleSet).build()
        );

        search(defaultFilter().andThen(f -> f.setShipment(null).setDesiredDeliveryDate(LocalDate.of(2019, 2, 10))))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_desired_delivery_date.json"));
    }

    @Test
    @DisplayName(
        "При выставленном desiredDeliveryDate возвращаются подходящие опции доставки, отличающиеся только набором ПВЗ"
    )
    void differentPickupPoints() throws Exception {
        List<ScheduleDayResponse> fullSchedule = fullSchedule();
        doReturn(List.of(
            partnerBuilder(400, PartnerType.DELIVERY).intakeSchedule(fullSchedule).build(),
            partnerBuilder(420, PartnerType.DELIVERY).intakeSchedule(fullSchedule).build(),
            partnerBuilder(SORTING_CENTER_ID, PartnerType.SORTING_CENTER).intakeSchedule(fullSchedule).build()
        ))
            .when(lmsClient)
            .searchPartners(createFilter());

        when(deliveryCalculatorClient.deliverySearch(defaultCalculatorRequest().build()))
            .thenReturn(differentPickupPointsResponse());

        mockPickupPointSearch(
            differentPickupPointsResponse(),
            id -> LongStream.rangeClosed(1, 7).mapToObj(this::scheduleDay).collect(Collectors.toSet())
        );

        Set<ScheduleDayResponse> fullScheduleSet = Set.copyOf(fullSchedule);
        mockGetWarehousesByPartnerIds(
            warehouseBuilder(600L, 400L).schedule(fullScheduleSet).build(),
            warehouseBuilder(601L, 420L).schedule(fullScheduleSet).build(),
            warehouseBuilder(601L, 420L).schedule(fullScheduleSet).build(),
            warehouseBuilder(SORTING_CENTER_WAREHOUSE_ID, SORTING_CENTER_ID).schedule(fullScheduleSet).build()
        );

        search(defaultFilter().andThen(f -> f.setShipment(null).setDesiredDeliveryDate(LocalDate.of(2019, 2, 10))))
            .andExpect(status().isOk())
            .andExpect(
                jsonContent(
                    "controller/delivery-options/search_result_desired_delivery_date_different_pickup_points.json"
                )
            );
    }

    @Test
    @DisplayName("Расчёт даты доставки при заказе сегодня, создана заявка на отгрузку")
    void shipmentForTodayWithApplication() throws Exception {
        mockSearchSenderWarehouse();
        mockSortingCenter(6);

        doReturn(
            shipmentPageResult(List.of(
                ShipmentSearchDto.builder()
                    .shipmentDate(TODAY)
                    .partnerIdTo(SORTING_CENTER_ID)
                    .status(ShipmentApplicationStatus.CREATED)
                    .shipmentType(ru.yandex.market.logistics.lom.model.enums.ShipmentType.WITHDRAW)
                    .build()
            ))
        ).when(lomClient).searchShipments(
            safeRefEq(getUpcomingShipmentApplicationsFilter()),
            safeRefEq(SHIPMENT_PAGEABLE)
        );

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/delivery-options/search_result_shipment_today_with_application.json"));

        verifyLomSearchTodayShipmentApplications();
        verifyLomSearchShipments(TODAY, LocalDate.of(2019, 2, 2));
    }

    @Test
    @DisplayName("Расчёт даты доставки при заказе сегодня, создана заявка на отгрузку, но не указан склад отправления")
    void shipmentForTodayWithApplicationWithoutWarehouse() throws Exception {
        mockSearchSenderWarehouse();
        mockSortingCenter(6);
        ShipmentSearchFilter applicationFilter = getUpcomingShipmentApplicationsFilterBuilder()
            .warehouseTo(null)
            .build();

        doReturn(
            shipmentPageResult(List.of(
                ShipmentSearchDto.builder()
                    .shipmentDate(TODAY)
                    .partnerIdTo(SORTING_CENTER_ID)
                    .status(ShipmentApplicationStatus.CREATED)
                    .shipmentType(ru.yandex.market.logistics.lom.model.enums.ShipmentType.WITHDRAW)
                    .build()
            ))
        ).when(lomClient).searchShipments(
            safeRefEq(applicationFilter),
            safeRefEq(SHIPMENT_PAGEABLE)
        );

        search(defaultFilter().andThen(filter -> filter.setShipment(new DeliveryOptionsFilterShipment())))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/delivery-options/search_result_shipment_today_with_application_without_warehouse.json"
            ));
    }

    @Test
    @DisplayName("Расчёт даты доставки при заказе сегодня, отсутствует заявка на отгрузку")
    void shipmentForTodayWithoutApplication() throws Exception {
        mockSearchSenderWarehouse();
        mockSortingCenter(7);

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/delivery-options/search_result_shipment_today_without_application.json"
            ));

        verifyLomSearchTodayShipmentApplications();
        verifyLomSearchShipments(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 3));
    }

    @Test
    @DisplayName("Расчёт даты доставки при заказе сегодня, по заявке отправлены реестры")
    void shipmentForTodayWithApplicationWithRegistrySent() throws Exception {
        mockSearchSenderWarehouse();
        mockSortingCenter(6);

        doReturn(
            shipmentPageResult(List.of(
                ShipmentSearchDto.builder()
                    .shipmentDate(TODAY)
                    .partnerIdTo(SORTING_CENTER_ID)
                    .status(ShipmentApplicationStatus.REGISTRY_SENT)
                    .shipmentType(ru.yandex.market.logistics.lom.model.enums.ShipmentType.IMPORT)
                    .build()
            ))
        ).when(lomClient).searchShipments(
            safeRefEq(getUpcomingShipmentApplicationsFilter()),
            safeRefEq(SHIPMENT_PAGEABLE)
        );

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/delivery-options/search_result_shipment_today_with_application_with_registry_sent.json"
            ));

        verifyLomSearchTodayShipmentApplications();
        verifyLomSearchShipments(LocalDate.of(2019, 2, 3), LocalDate.of(2019, 2, 9));
    }

    @ParameterizedTest
    @MethodSource("warehouseAndSearchSettingsSource")
    @DisplayName("Учёт настроек склада в зависимости от настроек поиска")
    void warehouseAndSearchSettings(
        Boolean useHandlingTime,
        Boolean useWarehouseSchedule,
        String responsePath
    ) throws Exception {
        mockSearchSenderWarehouse(
            point -> point.handlingTime(Duration.ofDays(2L))
                .schedule(Set.of(
                    LmsFactory.createScheduleDayDto(null, 1),
                    LmsFactory.createScheduleDayDto(null, 4),
                    LmsFactory.createScheduleDayDto(null, 7)
                ))
        );

        search(
            defaultFilter()
                .andThen(withSenderWarehouse())
                .andThen(filter -> filter.setSettings(
                    new DeliveryOptionsFilterSettings()
                        .setUseHandlingTime(useHandlingTime)
                        .setUseWarehouseSchedule(useWarehouseSchedule)
                ))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> warehouseAndSearchSettingsSource() {
        return Stream.of(
            Arguments.of(
                null,
                null,
                "controller/delivery-options/search_result_today_sender_warehouse_handling_time_schedule.json"
            ),
            Arguments.of(
                null,
                false,
                "controller/delivery-options/search_result_today_sender_warehouse_handling_time.json"
            ),
            Arguments.of(
                null,
                true,
                "controller/delivery-options/search_result_today_sender_warehouse_handling_time_schedule.json"
            ),
            Arguments.of(
                false,
                null,
                "controller/delivery-options/search_result_today_sender_warehouse_schedule.json"
            ),
            Arguments.of(
                false,
                false,
                "controller/delivery-options/search_result_today_sender_warehouse.json"
            ),
            Arguments.of(
                false,
                true,
                "controller/delivery-options/search_result_today_sender_warehouse_schedule.json"
            ),
            Arguments.of(
                true,
                null,
                "controller/delivery-options/search_result_today_sender_warehouse_handling_time_schedule.json"
            ),
            Arguments.of(
                true,
                false,
                "controller/delivery-options/search_result_today_sender_warehouse_handling_time.json"
            ),
            Arguments.of(
                true,
                true,
                "controller/delivery-options/search_result_today_sender_warehouse_handling_time_schedule.json"
            )
        );
    }

    @Test
    @DisplayName("Склад продавца без расписания")
    void noScheduleSenderWarehouse() throws Exception {
        mockSearchSenderWarehouse(point -> point.schedule(null));

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Schedule not set for warehouse id = 1"));
    }

    @Test
    @DisplayName("Склад продавца с пустым расписанием")
    void emptyScheduleSenderWarehouse() throws Exception {
        mockSearchSenderWarehouse(point -> point.schedule(Set.of()));

        search(defaultFilter().andThen(withSenderWarehouse()))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Schedule not set for warehouse id = 1"));
    }

    @Test
    @DisplayName("В локации назначения передан geoId = 0")
    void locationToGeoIdIsZero() throws Exception {
        search(defaultFilter().andThen(
            filter -> {
                DeliveryOptionsDestinationLocation locationTo = new DeliveryOptionsDestinationLocation();
                locationTo.setGeoId(0);
                filter.setTo(locationTo);
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY, true));
    }

    private void mockPostOffice(long pickupPointId) {
        LogisticsPointResponse logisticsPointResponse = LogisticsPointResponse.newBuilder()
            .id(pickupPointId)
            .pickupPointType(PickupPointType.POST_OFFICE)
            .cashAllowed(true)
            .build();
        when(lmsClient.getLogisticsPoints(argThat(a ->
            a.getType() == PointType.PICKUP_POINT && a.getIds().contains(pickupPointId)
        )))
            .thenReturn(List.of(logisticsPointResponse));
    }

    private void mockSearchPartners(@Nullable List<PartnerExternalParam> params) {
        doAnswer(
            invocation -> {
                SearchPartnerFilter filter = invocation.getArgument(0);
                if (filter == null || filter.getIds() == null) {
                    return List.of();
                }
                return filter.getIds().stream()
                    .map(
                        id ->
                            partnerBuilder(
                                id,
                                id == SORTING_CENTER_ID ? PartnerType.SORTING_CENTER : PartnerType.DELIVERY
                            )
                                .params(params)
                                .build()
                    )
                    .collect(Collectors.toList());
            }
        ).when(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    @Nonnull
    private DeliverySearchResponse newPostDeliveryOptions(List<Long> pickupPointIds) {
        return DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOptionBuilder(10L, 400L, TariffType.POST, 2)
                    .pickupPoints(pickupPointIds)
                    .build()
            ))
            .build();
    }

    @Nonnull
    private DeliverySearchResponse differentPickupPointsResponse() {
        return DeliverySearchResponse.builder()
            .deliveryOptions(List.of(
                deliveryOptionBuilder(10L, 400L, TariffType.POST, 1)
                    .pickupPoints(List.of(40000L, 40001L, 40002L))
                    .build(),
                deliveryOptionBuilder(11L, 420L, TariffType.PICKUP, 2)
                    .pickupPoints(List.of(42000L, 42001L, 42002L))
                    .build(),
                deliveryOptionBuilder(12L, 420L, TariffType.PICKUP, 2)
                    .pickupPoints(List.of(42000L, 42001L, 42002L))
                    .build(),
                deliveryOptionBuilder(13L, 420L, TariffType.PICKUP, 2)
                    .pickupPoints(List.of(42003L, 42004L, 42005L))
                    .build()
            ))
            .build();
    }

    private void mockEnabledParams(PartnerExternalParamType... params) {
        mockSearchPartners(
            Arrays.stream(params)
                .map(LmsFactory::enabledPartnerExternalParam)
                .collect(Collectors.toList())
        );
    }

    private void mockGetWarehousesByIds(LogisticsPointResponse... warehouses) {
        when(lmsClient.getLogisticsPoints(
            refEq(LogisticsPointFilter.newBuilder()
                .ids(Arrays.stream(warehouses).map(LogisticsPointResponse::getId).collect(Collectors.toSet()))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build())
        ))
            .thenReturn(List.of(warehouses));
    }

    private void mockGetWarehousesByPartnerIds(LogisticsPointResponse... warehouses) {
        when(lmsClient.getLogisticsPoints(
            refEq(LogisticsPointFilter.newBuilder()
                .partnerIds(
                    Arrays.stream(warehouses)
                        .map(LogisticsPointResponse::getPartnerId)
                        .collect(Collectors.toSet())
                )
                .type(PointType.WAREHOUSE)
                .active(true)
                .build())
        ))
            .thenReturn(List.of(warehouses));
    }

    private void mockMultiplaceSupportedTestData() {
        doReturn(List.of(
            partnerBuilder(400, PartnerType.DELIVERY).params(List.of()).build(),
            partnerBuilder(420, PartnerType.DELIVERY).build(),
            partnerBuilder(SORTING_CENTER_ID, PartnerType.SORTING_CENTER).params(List.of()).build()
        ))
            .when(lmsClient).searchPartners(searchPartnerFilter(Set.of(400L, 420L, 410L, SORTING_CENTER_ID)));
        mockCourierSchedule(lmsClient, LOCATION_TO_GEO_ID, Set.of(400L, 420L));
        when(deliveryCalculatorClient.deliverySearch(
            defaultCalculatorRequest().deliveryServiceIds(Set.of(400L, 420L)).build()
        ))
            .thenReturn(courierResponse());
    }

    private void mockSortingCenter(int day) {
        when(lmsClient.getPartner(SORTING_CENTER_ID))
            .thenReturn(Optional.of(
                partnerBuilder(SORTING_CENTER_ID, PartnerType.SORTING_CENTER)
                    .intakeSchedule(List.of(
                        createScheduleDay(100, day, 23)
                    ))
                    .build()
            ));
    }
}
