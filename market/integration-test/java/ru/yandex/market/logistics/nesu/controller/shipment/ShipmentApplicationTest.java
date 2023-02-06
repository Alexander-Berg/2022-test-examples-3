package ru.yandex.market.logistics.nesu.controller.shipment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ShipmentApplicationDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.dto.TimeIntervalDto;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.Service;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.LomShipmentApplicationFactory;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.WithdrawPriceListItemDto;
import ru.yandex.market.logistics.tarifficator.model.dto.WithdrawPriceListItemSearchDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание заявки на отгрузку")
@DatabaseSetup("/repository/shipments/database_prepare.xml")
public class ShipmentApplicationTest extends AbstractContextualTest {

    private static final LogisticsPointResponse WAREHOUSE_FIRST = createLogisticPoint(1L, null);
    private static final LogisticsPointResponse WAREHOUSE_SECOND = createLogisticPoint(2L, null);
    private static final Instant INSTANT = Instant.parse("2019-06-10T18:15:00.00Z");

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private TarifficatorClient tarifficatorClient;

    @BeforeEach
    void prepare() {
        when(lmsClient.getScheduleDay(1L))
            .thenReturn(Optional.of(new ScheduleDayResponse(
                1L,
                1,
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
            )));
        clock.setFixed(INSTANT, CommonsConstants.MSK_TIME_ZONE);
    }

    @Test
    @DisplayName("Отключенный магазин")
    @DatabaseSetup(value = "/repository/shop/before/disabled_shop.xml", type = DatabaseOperation.UPDATE)
    void disabledShop() throws Exception {
        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Забор, успех")
    void createWithdrawShipment() throws Exception {
        mockGetPartner(2L, 5L, PartnerType.DELIVERY);
        mockGetLogisticsPoint(1L);

        mockCreateShipmentApplication(
            createWithdrawShipmentApplication(null, 2L, null),
            createWithdrawShipmentApplication(1L, 2L, null)
        );

        mockSearchShipments();

        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/create_withdraw_response.json"));
    }

    @Test
    @DisplayName("Забор, собственная СД, успех")
    void createOwnDeliveryWithdrawShipment() throws Exception {
        mockGetPartner(2L, 100L, PartnerType.OWN_DELIVERY);
        mockGetLogisticsPoint(1L);

        ShipmentApplicationDto.ShipmentApplicationDtoBuilder
            shipmentApplication = createWithdrawShipmentApplicationBuilder(null, 2L, null)
            .interval(TimeIntervalDto.builder().from(LocalTime.of(9, 0)).to(LocalTime.of(22, 0)).build())
            .shipment(
                ShipmentDto.builder()
                    .marketIdFrom(100L)
                    .marketIdTo(100L)
                    .partnerIdTo(2L)
                    .shipmentType(ShipmentType.WITHDRAW)
                    .shipmentDate(LocalDate.of(2019, 6, 12))
                    .warehouseFrom(1L)
                    .fake(true)
                    .build()
            );

        mockCreateShipmentApplication(shipmentApplication.build(), shipmentApplication.id(1L).build());

        mockSearchShipments();

        createShipment("controller/shipment/request/create_own_delivery_withdraw_request.json", 1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/create_own_delivery_withdraw_response.json"));
    }

    @Test
    @DisplayName("Забор, собственная СД, успех с датой сегодня")
    void createOwnDeliveryWithdrawShipmentToday() throws Exception {
        mockGetPartner(2L, 100L, PartnerType.OWN_DELIVERY);
        mockGetLogisticsPoint(1L);

        ShipmentApplicationDto.ShipmentApplicationDtoBuilder
            shipmentApplication = createWithdrawShipmentApplicationBuilder(null, 2L, 5L)
            .interval(TimeIntervalDto.builder().from(LocalTime.of(9, 0)).to(LocalTime.of(22, 0)).build())
            .shipment(
                ShipmentDto.builder()
                    .marketIdFrom(100L)
                    .marketIdTo(100L)
                    .partnerIdTo(2L)
                    .shipmentType(ShipmentType.WITHDRAW)
                    .shipmentDate(LocalDate.now(clock))
                    .warehouseFrom(1L)
                    .fake(true)
                    .build()
            );

        mockCreateShipmentApplication(shipmentApplication.build(), shipmentApplication.id(1L).build());

        mockSearchShipments();

        createShipment("controller/shipment/request/create_own_delivery_withdraw_request_today.json", 1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shipment/response/create_own_delivery_withdraw_response_today.json"
            ));
    }

    @Test
    @DisplayName("Создать забор — стоимость взята из тарификатора")
    void createWithdrawWithCostFromTarifficator() throws Exception {
        BigDecimal withdrawCost = BigDecimal.valueOf(100);
        mockGetPartner(2L, 5L, PartnerType.DELIVERY);
        mockGetLogisticsPoint(1L, 1L);
        mockGetWithdrawCost(1L, BigDecimal.valueOf(0.006000), withdrawCost);
        ShipmentApplicationDto.ShipmentApplicationDtoBuilder defaultBuilder =
            createWithdrawShipmentApplicationBuilder(null, 2L, null)
                .cost(withdrawCost)
                .locationZoneId(1L);

        mockCreateShipmentApplication(
            defaultBuilder.build(),
            defaultBuilder.id(1L).build()
        );

        mockSearchShipments();

        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shipment/response/create_withdraw_with_cost_from_tarifficator_response.json"
            ));
    }

    @Test
    @DisplayName("Создать забор — склад не привязан к зоне")
    void createWithdrawWarehouseWithoutZone() throws Exception {
        mockGetPartner(2L, 5L, PartnerType.DELIVERY);
        mockGetLogisticsPoint(1L);

        mockCreateShipmentApplication(
            createWithdrawShipmentApplication(null, 2L, null),
            createWithdrawShipmentApplication(1L, 2L, null)
        );

        mockSearchShipments();

        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/create_withdraw_response.json"));

        verifyNoMoreInteractions(tarifficatorClient);
    }

    @Test
    @DisplayName("Создать забор — стоимость не найдена в тарификаторе")
    void createWithdrawTatiffDoesNotExist() throws Exception {
        mockGetPartner(2L, 5L, PartnerType.DELIVERY);
        mockGetLogisticsPoint(1L, 1L);

        mockCreateShipmentApplication(
            createWithdrawShipmentApplicationBuilder(null, 2L, null)
                .locationZoneId(1L)
                .build(),
            createWithdrawShipmentApplicationBuilder(1L, 2L, null)
                .locationZoneId(1L)
                .build()
        );

        mockSearchShipments();

        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/create_withdraw_response.json"));
    }

    @Test
    @DisplayName("Забор, не найдено расписание")
    void createWithdrawShipmentScheduleError() throws Exception {
        mockGetPartner(2L, 5L, PartnerType.DELIVERY);
        mockGetLogisticsPoint(1L);

        when(lmsClient.getScheduleDay(1L))
            .thenReturn(Optional.empty());

        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [SCHEDULE_INTERVAL] with ids [1]\","
                + "\"resourceType\":\"SCHEDULE_INTERVAL\",\"identifiers\":[1]}"));
    }

    @Test
    @DisplayName("Забор, не найден партнер")
    void createWithdrawShipmentPartnerNotFound() throws Exception {
        mockGetLogisticsPoint(1L);

        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [PARTNER] with ids [2]\","
                + "\"resourceType\":\"PARTNER\",\"identifiers\":[2]}"));
    }

    @Test
    @DisplayName("Забор, не найден магазин")
    void createWithdrawShipmentShopNotFound() throws Exception {
        mockGetPartner(2L, 5L, PartnerType.DELIVERY);

        createShipment("controller/shipment/request/create_withdraw_request.json", 3L)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [SHOP] with ids [3]\","
                + "\"resourceType\":\"SHOP\",\"identifiers\":[3]}"));
    }

    @Test
    @DisplayName("Забор, не найден склад")
    void createWithdrawShipmentWarehousesNotFound() throws Exception {
        mockGetLogisticsPoints(Set.of(1L));
        mockGetPartner(2L, 100L, PartnerType.OWN_DELIVERY);
        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [WAREHOUSE] with ids [1]\","
                + "\"resourceType\":\"WAREHOUSE\",\"identifiers\":[1]}"));
    }

    @Test
    @DisplayName("Собственная доставка не принадлежит магазину, создающему заявку на отгрузку")
    void createWithdrawShipmentWrongOwnDeliveryPartner() throws Exception {
        mockGetLogisticsPoints(Set.of(1L));
        mockGetPartner(2L, 1L, PartnerType.OWN_DELIVERY);
        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Собственная доставка id=2 не принадлежит магазину id=1"));
    }

    @Test
    @DisplayName("Забор, склад магазина с другим businessId")
    void createWithdrawWrongBusinessId() throws Exception {
        mockGetPartner(2L, 7L, PartnerType.DELIVERY);
        when(lmsClient.getLogisticsPoint(1L))
            .thenReturn(Optional.of(createWarehouse(1L, 42L, 5L, null)));

        createShipment("controller/shipment/request/create_withdraw_request.json", 1L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));
    }

    @ParameterizedTest
    @MethodSource("validationProvider")
    @DisplayName("Валидация запроса")
    void validation(String requestPath, String field, String message, String code) throws Exception {
        mockGetPartner(5L, 7L, PartnerType.DELIVERY);
        mockGetLogisticsPoints(Set.of(2L, 1L), WAREHOUSE_SECOND, createLogisticPoint(1L, 100L, 5L));
        createShipment(requestPath, 1L)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(applicationFieldError(field, message, code)));
    }

    @Test
    @DisplayName("Валидация запроса, несколько полей с ошибками")
    void multipleFieldsValidation() throws Exception {
        List<ValidationErrorData> expected = List.of(
            applicationFieldError(
                "courier.carBrand",
                "Import application with CAR courier type must have car brand",
                "ValidShipmentCourier"
            ),
            applicationFieldError(
                "courier.carNumber",
                "Import application with CAR courier type must have car number",
                "ValidShipmentCourier"
            ),
            applicationFieldError("courier.firstName", "must not be blank", "NotBlank"),
            applicationFieldError("courier.lastName", "must not be blank", "NotBlank"),
            applicationFieldError(
                "shipment.warehouseTo",
                "Import application must have destination warehouse ID",
                "ValidShipment"
            )
        );
        createShipment("controller/shipment/request/invalid/import_multiple_errors.json", 1L)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(expected));
    }

    @Nonnull
    protected ValidationErrorData applicationFieldError(String field, String message, String code) {
        return ValidationErrorData.fieldError(field, message, "shipmentApplicationRequest", code);
    }

    private static Stream<Arguments> validationProvider() {
        return Stream.of(
            Arguments.of(
                "controller/shipment/request/invalid/import_no_courier.json",
                "courier",
                "Import application must have a courier",
                "ValidShipmentCourier"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/import_empty_courier_type.json",
                "courier.type",
                "must not be null",
                "NotNull"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/import_empty_courier_first_name.json",
                "courier.firstName",
                "must not be blank",
                "NotBlank"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/import_empty_courier_last_name.json",
                "courier.lastName",
                "must not be blank",
                "NotBlank"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/import_car_empty_car_brand.json",
                "courier.carBrand",
                "Import application with CAR courier type must have car brand",
                "ValidShipmentCourier"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/import_car_empty_car_number.json",
                "courier.carNumber",
                "Import application with CAR courier type must have car number",
                "ValidShipmentCourier"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/no_shipment.json",
                "shipment",
                "must not be null",
                "NotNull"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/no_shipment_type.json",
                "shipment.type",
                "must not be null",
                "NotNull"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/no_shipment_date.json",
                "shipment.date",
                "must not be null",
                "NotNull"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/no_shipment_warehouse_from.json",
                "shipment.warehouseFrom",
                "must not be null",
                "NotNull"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/import_no_shipment_warehouse_to.json",
                "shipment.warehouseTo",
                "Import application must have destination warehouse ID",
                "ValidShipment"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/withdraw_no_shipment_partner_to.json",
                "shipment.partnerTo",
                "Withdraw application must have destination partner ID",
                "ValidShipment"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/no_interval.json",
                "intervalId",
                "must not be null",
                "NotNull"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/no_dimensions.json",
                "dimensions",
                "must not be null",
                "NotNull"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/import_shipment_date_earlier_than_today.json",
                "shipment.date",
                "Shipment date must not be earlier than today",
                "ValidShipmentDate"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/withdraw_shipment_date_today.json",
                "shipment.date",
                "Withdraw application shipment date must be later than today",
                "ValidWithdrawShipmentDate"
            ),
            Arguments.of(
                "controller/shipment/request/invalid/withdraw_shipment_date_tomorrow_later_than_21.json",
                "shipment.date",
                "Withdraw application with shipment date tomorrow must be created before 21:00",
                "ValidWithdrawShipmentDate"
            )
        );
    }

    @Test
    @DisplayName("Склад отправителя не принадлежит магазину")
    void invalidMarketIdFrom() throws Exception {
        mockGetLogisticsPoints(Set.of(2L, 1L), createLogisticPoint(2, 42L, 5L), createLogisticPoint(1L, 100L, 5L));
        mockGetPartner(5L, 7L, PartnerType.SORTING_CENTER);
        createShipment("controller/shipment/request/create_import_request.json", 1L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [2]"));
    }

    @Test
    @DisplayName("Склад отправителя - ПВЗ")
    void warehouseFromPickupPoint() throws Exception {
        mockGetLogisticsPoints(Set.of(2L, 1L), WAREHOUSE_FIRST);

        createShipment("controller/shipment/request/create_import_request.json", 1L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [2]"));
    }

    @Test
    @DisplayName("У точки отсутствует идентификатор партнера")
    void nullPartnerToId() throws Exception {
        mockGetLogisticsPoints(Set.of(2L, 1L), createLogisticPoint(1L, null), createLogisticPoint(2L, null));

        createShipment("controller/shipment/request/create_import_request.json", 1L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Склад получателя id=1 должен принадлежать службе доставки или сортировочному центру. "
                    + "Не указан идентификатор партнера в логистической точке."
            ));
    }

    @Test
    @DisplayName("Заявка с такими параметрами уже существует")
    @DatabaseSetup(
        value = "/repository/shipments/database_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void existingApplication() throws Exception {
        mockGetPartner(5L, 7L, PartnerType.DELIVERY);
        mockGetLogisticsPoints(Set.of(2L, 1L), WAREHOUSE_SECOND, createLogisticPoint(1L, 5L));

        ShipmentApplicationDto dto = createImportShipmentApplicationDto(1L);
        ShipmentApplicationDto dtoNull = createImportShipmentApplicationDto(null);

        mockCreateShipmentApplication(dtoNull, dto);

        ShipmentSearchFilter shipmentSearchFilter = ShipmentSearchFilter.builder()
            .warehousesFrom(Set.of(dto.getShipment().getWarehouseFrom()))
            .warehouseTo(dto.getShipment().getWarehouseTo())
            .fromDate(dto.getShipment().getShipmentDate())
            .toDate(dto.getShipment().getShipmentDate())
            .shipmentType(dto.getShipment().getShipmentType())
            .partnerIdsTo(Set.of(dto.getShipment().getPartnerIdTo()))
            .marketIdFrom(dto.getShipment().getMarketIdFrom())
            .withApplication(true)
            .build();

        mockSearchShipments(
            shipmentSearchFilter,
            ShipmentSearchDto.builder().applicationId(1L).build()
        );

        createShipment("controller/shipment/request/create_import_request.json", 4L)
            .andExpect(status().is4xxClientError())
            .andExpect(jsonContent("controller/shipment/response/existing_application.json"));
    }

    @Test
    @DisplayName("Забор СЦ, сендеры магазина имеют региональные настройки")
    @DatabaseSetup(
        value = "/repository/shipments/database_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createScWithdrawShipmentShopSendersWithRegionSettings() throws Exception {
        mockGetPartner(5L, 5L, PartnerType.SORTING_CENTER);
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createLogisticPoint(1L, null)));
        mockGetLogisticsPoints(createLogisticsPointsByPartnerIdsFilter(5L), createLogisticPoint(2L, 5L));

        mockCreateShipmentApplication(
            createWithdrawShipmentApplicationBuilder(null, 5L, 2L).build(),
            createWithdrawShipmentApplicationBuilder(1L, 5L, 2L).build()
        );

        mockSearchShipments();

        createShipment("controller/shipment/request/create_withdraw_sc_request.json", 3L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/create_shop_3_withdraw_sc_response.json"));
    }

    @Test
    @DisplayName("Забор СЦ, нет региональных настроек у сендеров c указанным СЦ")
    @DatabaseSetup(
        value = "/repository/shipments/database_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createScWithdrawShipmentShopSortingCenterWarehouseValidationError() throws Exception {
        mockGetPartner(5L, 5L, PartnerType.SORTING_CENTER);
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createLogisticPoint(1L, null)));
        mockGetLogisticsPoints(createLogisticsPointsByPartnerIdsFilter(5L), createLogisticPoint(20L, 5L));

        createShipment("controller/shipment/request/create_withdraw_sc_request.json", 3L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Ни для одного сендера магазина id=3 не настроен склад СЦ id=20 в регионе 1"));
    }

    @Test
    @DisplayName("Самопривоз СД, указанный склад СД не принадлежит партнёру СД")
    @DatabaseSetup(
        value = "/repository/shipments/database_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createDsImportShipmentIncorrectWarehouseTo() throws Exception {
        mockGetPartner(5L, 2L, PartnerType.DELIVERY);
        mockGetLogisticsPoints(Set.of(2L, 1L), createLogisticPoint(2L, null), createLogisticPoint(1L, null));

        createShipment("controller/shipment/request/create_import_all_shipment_fields_request.json", 4L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Указанный склад получателя id=1 не принадлежит партнеру id=5"));
    }

    @Test
    @DisplayName("Самопривоз СД, нет сендеров магазина с указанной СД в регионе")
    @DatabaseSetup(
        value = "/repository/shipments/database_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createDsImportShipmentNoDeliverySettingsForShopSenders() throws Exception {
        mockGetPartner(6L, 2L, PartnerType.DELIVERY);
        mockGetLogisticsPoints(Set.of(2L, 1L), createLogisticPoint(2L, 6L), createLogisticPoint(1L, null));

        createShipment(
            "controller/shipment/request/create_import_all_shipment_fields_no_delivery_setting_request.json",
            4L
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Для магазина id=4 нет сендеров с настройкой для СД id=6 в регионе 1."));
    }

    @Test
    @DisplayName("Самопривоз СД, успех, сендеры магазина с региональными настройками")
    @DatabaseSetup(
        value = "/repository/shipments/database_prepare_region_settings.xml",
        type = DatabaseOperation.INSERT
    )
    void createDsImportShipmentRegionSettings() throws Exception {
        mockGetPartner(5L, 7L, PartnerType.DELIVERY);
        mockGetLogisticsPoints(Set.of(2L, 1L), createLogisticPoint(2L, null), createLogisticPoint(1L, 5L));

        mockCreateShipmentApplication(
            createImportShipmentApplicationDto(null),
            createImportShipmentApplicationDto(1L)
        );

        mockSearchShipments();

        createShipment("controller/shipment/request/create_import_all_shipment_fields_request.json", 4L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/response/create_import_shop_4_ds_response.json"));
    }

    @Nonnull
    protected ResultActions createShipment(String requestPath, long shopId) throws Exception {
        return mockMvc.perform(
            post("/back-office/shipments")
                .param("shopId", shopId + "")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }

    protected void mockGetPartner(long id, long marketId, PartnerType partnerType) {
        when(lmsClient.getPartner(id))
            .thenReturn(Optional.of(createPartner(id, marketId, partnerType)));
    }

    private void mockGetLogisticsPoint(long id) {
        when(lmsClient.getLogisticsPoint(id))
            .thenReturn(Optional.of(WAREHOUSE_FIRST));
    }

    private void mockGetLogisticsPoint(long id, long locationZoneId) {
        when(lmsClient.getLogisticsPoint(id))
            .thenReturn(Optional.of(createLogisticPointWithLocationZoneId(id, locationZoneId)));
    }

    private void mockGetLogisticsPoints(Set<Long> ids, LogisticsPointResponse... warehouses) {
        mockGetLogisticsPoints(createLogisticsPointsFilter(ids), warehouses);
    }

    private void mockGetLogisticsPoints(
        LogisticsPointFilter logisticsPointFilter,
        LogisticsPointResponse... warehouses
    ) {
        when(lmsClient.getLogisticsPoints(refEq(logisticsPointFilter)))
            .thenReturn(List.of(warehouses));
    }

    protected void mockCreateShipmentApplication(
        ShipmentApplicationDto shipmentApplicationRequest,
        ShipmentApplicationDto shipmentApplicationResponse
    ) {
        when(lomClient.createShipmentApplication(safeRefEq(shipmentApplicationRequest)))
            .thenReturn(shipmentApplicationResponse);
    }

    private void mockSearchShipments(
        ShipmentSearchFilter shipmentSearchFilter,
        ShipmentSearchDto shipmentSearchDto
    ) {
        when(lomClient.searchShipments(
            eq(shipmentSearchFilter),
            any(ru.yandex.market.logistics.lom.model.search.Pageable.class)
        )).thenReturn(PageResult.of(List.of(shipmentSearchDto), 1, 1, 1));
    }

    protected void mockSearchShipments() {
        when(lomClient.searchShipments(
            any(ShipmentSearchFilter.class),
            any(ru.yandex.market.logistics.lom.model.search.Pageable.class)
        )).thenReturn(PageResult.of(List.of(), 1, 1, 1));
    }

    @Nonnull
    private ShipmentApplicationDto createImportShipmentApplicationDto(Long id) {
        return LomShipmentApplicationFactory.createImportShipmentApplication(id).build();
    }

    @Nonnull
    private ShipmentApplicationDto createWithdrawShipmentApplication(
        Long id,
        long partnerIdTo,
        Long warehouseTo
    ) {
        return createWithdrawShipmentApplicationBuilder(id, partnerIdTo, warehouseTo).build();
    }

    private ShipmentApplicationDto.ShipmentApplicationDtoBuilder createWithdrawShipmentApplicationBuilder(
        Long id,
        long partnerIdTo,
        Long warehouseTo
    ) {
        return LomShipmentApplicationFactory.createWithdrawShipmentApplication(
            id,
            partnerIdTo,
            warehouseTo
        );
    }

    @Nonnull
    private LogisticsPointFilter createLogisticsPointsFilter(Set<Long> ids) {
        return LogisticsPointFilter.newBuilder()
            .ids(ids)
            .active(true)
            .type(PointType.WAREHOUSE)
            .build();
    }

    @Nonnull
    private LogisticsPointFilter createLogisticsPointsByPartnerIdsFilter(Long... partnerIds) {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(partnerIds))
            .active(true)
            .type(PointType.WAREHOUSE)
            .build();
    }

    @Nonnull
    protected static LogisticsPointResponse createLogisticPoint(long id, @Nullable Long partnerId) {
        return createWarehouse(id, 41L, partnerId, null);
    }

    @Nonnull
    protected static LogisticsPointResponse createLogisticPoint(long id, long businessId, @Nullable Long partnerId) {
        return createWarehouse(id, businessId, partnerId, null);
    }

    @Nonnull
    private static LogisticsPointResponse createLogisticPointWithLocationZoneId(long id, long locationZoneId) {
        return createWarehouse(id, 41L, null, locationZoneId);
    }

    @Nonnull
    private static LogisticsPointResponse createWarehouse(
        long id,
        @Nullable Long businessId,
        @Nullable Long partnerId,
        @Nullable Long locationZoneId
    ) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .partnerId(partnerId)
            .businessId(businessId)
            .externalId("warehouseCode")
            .type(PointType.WAREHOUSE)
            .name("warehouseName")
            .address(
                Address.newBuilder()
                    .locationId(1)
                    .settlement("locality")
                    .postCode("postCode")
                    .latitude(new BigDecimal(1))
                    .longitude(new BigDecimal(2))
                    .street("street")
                    .house("house")
                    .housing("housing")
                    .building("building")
                    .apartment("apartment")
                    .comment("comment")
                    .region("region")
                    .subRegion("subRegion")
                    .addressString("addressString")
                    .shortAddressString("shortAddressString")
                    .build()
            )
            .phones(Set.of(new Phone("+79998887777", "777", "test_phone", PhoneType.PRIMARY)))
            .active(true)
            .schedule(Set.of(new ScheduleDayResponse(1L, 1, LocalTime.of(12, 20), LocalTime.of(13, 20))))
            .contact(new Contact("test_name", "test_surname", "test_patronymic"))
            .cashAllowed(true)
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("test_instructions")
            .returnAllowed(true)
            .services(Set.of(new Service(ServiceCodeName.CASH_SERVICE, true, "test_service_name", "description")))
            .storagePeriod(3)
            .maxWeight(20.0)
            .maxLength(20)
            .maxHeight(15)
            .maxSidesSum(100)
            .locationZoneId(locationZoneId)
            .build();
    }

    @Nonnull
    private PartnerResponse createPartner(long partnerId, long marketId, PartnerType type) {
        return PartnerResponse.newBuilder()
            .id(partnerId)
            .marketId(marketId)
            .partnerType(type)
            .name("Sample Partner")
            .readableName("Sample Readable Partner")
            .status(PartnerStatus.ACTIVE)
            .billingClientId(1L)
            .trackingType("Sample Tracking Type")
            .locationId(1)
            .rating(5)
            .logoUrl("http://test_logo_url/" + partnerId)
            .build();
    }

    private void mockGetWithdrawCost(Long locationZoneId, BigDecimal volume, BigDecimal cost) {
        when(tarifficatorClient.searchWithdrawPriceListItemDto(safeRefEq(
            new WithdrawPriceListItemSearchDto()
                .setVolume(volume)
                .setLocationZoneId(locationZoneId)
        )))
            .thenReturn(Optional.of(new WithdrawPriceListItemDto().setCost(cost)));
    }
}
