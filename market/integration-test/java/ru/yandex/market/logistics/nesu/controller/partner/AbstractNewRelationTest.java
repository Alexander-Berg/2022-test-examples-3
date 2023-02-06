package ru.yandex.market.logistics.nesu.controller.partner;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.datacamp.client.DataCampClient;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationUpdateDto;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.dto.partner.CpaPartnerInterfaceRelationRequest.CpaPartnerInterfaceRelationRequestBuilder;
import ru.yandex.market.logistics.nesu.enums.ShopShipmentType;
import ru.yandex.market.logistics.nesu.jobs.producer.RemoveDropoffShopBannerProducer;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Сохранение выбранного склада магазина")
public abstract class AbstractNewRelationTest extends AbstractPartnerControllerNewTest {

    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private RemoveDropoffShopBannerProducer removeDropoffShopBannerProducer;

    @BeforeEach
    void setupMocks() {
        mockGetPartner(DROPSHIP_PARTNER_ID, PartnerType.DROPSHIP);
        mockGetPartner(SUPPLIER_PARTNER_ID, PartnerType.SUPPLIER);

        mockGetSingleWarehouse(DROPSHIP_PARTNER_ID, 213);
        mockGetSingleWarehouse(SUPPLIER_PARTNER_ID, 213);

        mockGetDeliveryWarehouse(true, DELIVERY_WAREHOUSE_ID);
        mockGetPartner(DELIVERY_PARTNER_ID, PartnerType.DELIVERY);

        mockGetRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID);
        mockGetRelation(SUPPLIER_PARTNER_ID, DELIVERY_PARTNER_ID);
        mockGetCapacity(DROPSHIP_PARTNER_ID);
        mockGetCapacity(SUPPLIER_PARTNER_ID);

        mockCreateCapacity();
        mockUpdateCapacity();
        mockCreateRelation();
        mockUpdateRelation();
        mockBannerRemoving();
    }

    @AfterEach
    void tearDownAbstractGetAvailableShipmentOptionsTest() {
        verifyNoMoreInteractions(dataCampClient, removeDropoffShopBannerProducer);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({
        "requestValidationSource",
        "importValidationSource",
        "customValidationsSource",
    })
    @DisplayName("Валидация запроса")
    void requestValidation(
        ValidationErrorDataBuilder error,
        CpaPartnerInterfaceRelationRequestBuilder request
    ) throws Exception {
        saveRelation(1, 1, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject("cpaPartnerInterfaceRelationRequest")));
    }

    @Nonnull
    private static Stream<Arguments> requestValidationSource() {
        return Stream.of(
            Arguments.of(
                fieldErrorBuilder("shipmentType", ErrorType.NOT_NULL),
                defaultRequest().shipmentType(null)
            ),
            Arguments.of(
                fieldErrorBuilder("capacityValue", ErrorType.NOT_NULL),
                defaultRequest().capacityValue(null)
            ),
            Arguments.of(
                fieldErrorBuilder("cutoffTime", ErrorType.NOT_NULL),
                defaultRequest().cutoffTime(null)
            ),
            Arguments.of(
                fieldErrorBuilder("handlingTime", ErrorType.NOT_NULL),
                defaultRequest().handlingTime(null)
            ),
            Arguments.of(
                fieldErrorBuilder("capacityValue", ErrorType.min(50)),
                defaultRequest().capacityValue(-1)
            ),
            Arguments.of(
                fieldErrorBuilder("handlingTime", ErrorType.min(0)),
                defaultRequest().handlingTime(-1)
            ),
            Arguments.of(
                fieldErrorBuilder("handlingTime", ErrorType.max(1)),
                defaultRequest().handlingTime(2)
            ),
            Arguments.of(
                fieldErrorBuilder("shipmentScheduleDayIds", ErrorType.NOT_EMPTY),
                defaultRequest().shipmentScheduleDayIds(null)
            ),
            Arguments.of(
                fieldErrorBuilder("shipmentScheduleDayIds", ErrorType.NOT_EMPTY),
                defaultRequest().shipmentScheduleDayIds(Set.of())
            ),
            Arguments.of(
                fieldErrorBuilder("shipmentScheduleDayIds", ErrorType.NOT_EMPTY),
                defaultRequest().shipmentScheduleDayIds(Set.of())
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> importValidationSource() {
        return Stream.of(
            Arguments.of(
                fieldErrorBuilder("toPartnerLogisticsPointId", ErrorType.NOT_NULL),
                defaultRequest(ShopShipmentType.IMPORT).toPartnerLogisticsPointId(null)
            ),
            Arguments.of(
                fieldErrorBuilder("capacityValue", ErrorType.NOT_NULL),
                defaultRequest(ShopShipmentType.IMPORT).capacityValue(null)
            ),
            Arguments.of(
                fieldErrorBuilder("cutoffTime", ErrorType.NOT_NULL),
                defaultRequest(ShopShipmentType.IMPORT).cutoffTime(null)
            ),
            Arguments.of(
                fieldErrorBuilder("handlingTime", ErrorType.NOT_NULL),
                defaultRequest(ShopShipmentType.IMPORT).handlingTime(null)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> customValidationsSource() {
        return Stream.of(
            Arguments.of(
                fieldErrorBuilder(
                    "cutoffTime",
                    new ErrorType(
                        "must be less than or equal to 08:00",
                        "ValidCutoffTime0Day",
                        Map.of("value", "08:00")
                    )
                ),
                defaultRequest().handlingTime(0)
            ),
            Arguments.of(
                fieldErrorBuilder(
                    "cutoffTime",
                    new ErrorType(
                        "must be greater than or equal to 14:00",
                        "ValidCutoffTime",
                        Map.of("value", "14:00")
                    )
                ),
                defaultRequest().cutoffTime(LocalTime.of(13, 0))
            ),
            Arguments.of(
                fieldErrorBuilder(
                    "shipmentScheduleDayIds",
                    new ErrorType(
                        "Interval time must be less or equal to 20:00",
                        "ValidRelationShipmentScheduleInterval",
                        Map.of("value", "20:00")
                    )
                ),
                defaultRequest().shipmentScheduleDayIds(Set.of(101L, 22L, 23L, 24L, 25L))
            ),
            Arguments.of(
                fieldErrorBuilder(
                    "cutoffTime",
                    new ErrorType(
                        "Impossible to set cutoff for same-day shipment interval start time 00:30",
                        "ValidIntervalTime",
                        Map.of("value", "00:30")
                    )
                ),
                defaultRequest().handlingTime(0).shipmentScheduleDayIds(Set.of(1001L, 22L, 23L, 24L, 25L))
            ),
            Arguments.of(
                fieldErrorBuilder(
                    "cutoffTime",
                    new ErrorType(
                        "Impossible to set cutoff for same-day shipment interval start time 01:30",
                        "ValidIntervalTime",
                        Map.of("value", "01:30")
                    )
                ),
                defaultRequest(ShopShipmentType.IMPORT)
                    .handlingTime(0)
                    .shipmentScheduleDayIds(Set.of(1001L, 22L, 23L, 24L, 25L))
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация соответствия расписаний отгрузки и склада")
    void shipmentScheduleValidation(
        @SuppressWarnings("unused") String displayName,
        ValidationErrorDataBuilder error,
        CpaPartnerInterfaceRelationRequestBuilder request
    ) throws Exception {
        saveRelation(1, DROPSHIP_PARTNER_ID, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject("cpaPartnerInterfaceRelation")));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
    }

    @Nonnull
    private static Stream<Arguments> shipmentScheduleValidation() {
        return Stream.of(
            Arguments.of(
                "Слишком маленькое расписание",
                fieldErrorBuilder("shipmentSchedule", ERROR_TYPE_SCHEDULE_DAYS_COUNT),
                getWithdrawRequest(Set.of(22L))
            ),
            Arguments.of(
                "Нет подходящего дня в расписании склада",
                fieldErrorBuilder("shipmentSchedule", ERROR_TYPE_VALID_SHIPMENT_SCHEDULE),
                getWithdrawRequest(Set.of(21L, 22L, 23L, 24L, 30L))
            ),
            Arguments.of(
                "Расписание склада начинается позже расписания отгрузки",
                fieldErrorBuilder("shipmentSchedule", ERROR_TYPE_VALID_SHIPMENT_SCHEDULE),
                getWithdrawRequest(Set.of(21L, 22L, 23L, 24L, 28L))
            ),
            Arguments.of(
                "Расписание склада заканчивается раньше расписания отгрузки",
                fieldErrorBuilder("shipmentSchedule", ERROR_TYPE_VALID_SHIPMENT_SCHEDULE),
                getWithdrawRequest(Set.of(21L, 22L, 23L, 24L, 29L))
            )
        );
    }

    @Test
    @DisplayName("Успешая валидация катофа для отгрузки день в день для забора. 2 часа до начала слота отгрузки")
    void requestValidationCutoffForHandleTime0WithWitdrawType() throws Exception {
        saveRelation(1, 1, defaultRequest().handlingTime(0).cutoffTime(LocalTime.of(8, 0)))
            .andExpect(status().isOk());

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyUpdateRelation(
            defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true)
                .cutoffs(cutoffOf(8, null))
                .build()
        );
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ZERO);
    }

    @Test
    @DisplayName("Не найден магазин")
    void shopNotFound() throws Exception {
        saveRelation(-1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [-1]"));
    }

    @Test
    @DisplayName("Не найден партнёр магазина")
    void partnerNotFound() throws Exception {
        mockNotFoundPartner(DROPSHIP_PARTNER_ID);

        saveRelation(1, 1, defaultRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [1]"));

        verifyGetPartner(DROPSHIP_PARTNER_ID);
    }

    @Test
    @DisplayName("Не найден склад магазина")
    void shopWarehouseNotFound() throws Exception {
        doReturn(List.of())
            .when(lmsClient)
            .getLogisticsPoints(partnerWarehouseFilter(DROPSHIP_PARTNER_ID));

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Партнер id = 1 не имеет активных складов"));

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
    }

    @Test
    @DisplayName("Нет доступных складов для подключения на забор")
    void noAvailableWithdrawWarehouses() throws Exception {
        mockGetSingleWarehouse(DROPSHIP_PARTNER_ID, 40);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("WITHDRAW is not available for warehouse 10"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
    }

    @Test
    @DisplayName("Нет доступных складов для подключения на самопривоз")
    void noAvailableImportWarehouses() throws Exception {
        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultRequest(ShopShipmentType.IMPORT).toPartnerLogisticsPointId(200L)
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("IMPORT to warehouse 200 is not available for warehouse 10"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(
        value = "/repository/partner-relation/before/disable_availabilities.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Обновление связки WITHDRAW партнером с недоступной доступностью")
    void allowModifyingRelationForWithdrawPartnerWithDisabledAvailability() {
        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_withdraw.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyUpdateRelation(defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true).build());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(
        value = "/repository/partner-relation/before/disable_availabilities.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Запрет обновления связки IMPORT партнером с недоступной доступностью")
    void disallowModifyingRelationForImportPartnerWithDisabledAvailability() {
        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest(ShopShipmentType.IMPORT))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("IMPORT to warehouse 100 is not available for warehouse 10"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
    }

    @Test
    @DisplayName("Превышен лимит заказов от партнёра для точки")
    @DatabaseSetup(
        value = "/repository/partner-relation/before/availability_capacity.xml",
        type = DatabaseOperation.UPDATE
    )
    void capacityExceeded() throws Exception {
        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultRequest(ShopShipmentType.IMPORT)
                .toPartnerLogisticsPointId(100L)
                .shipmentScheduleDayIds(Set.of(31L, 32L, 33L, 34L, 35L))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("IMPORT to warehouse 100 is not available for warehouse 10"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
    }

    @Test
    @DisplayName("Превышен лимит заказов от партнёра для уже подключенной точки")
    @DatabaseSetup(
        value = "/repository/partner-relation/before/availability_capacity.xml",
        type = DatabaseOperation.UPDATE
    )
    void existingRelationCapacityExceeded() throws Exception {
        mockGetRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID, ShipmentType.IMPORT);

        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultRequest(ShopShipmentType.IMPORT)
                .toPartnerLogisticsPointId(100L)
                .shipmentScheduleDayIds(Set.of(31L, 32L, 33L, 34L, 35L))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("IMPORT to warehouse 100 is not available for warehouse 10"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
    }

    @Test
    @DisplayName("Не найден склад для отгрузки")
    void deliveryWarehouseNotFound() throws Exception {
        mockGetDeliveryWarehouse(false, DELIVERY_WAREHOUSE_ID);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTICS_POINT] with ids [100]"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
    }

    @Test
    @DisplayName("Не заполнен партнёр логистической точки для отгрузки")
    void shipmentPartnerIsNull() throws Exception {
        mockGetDeliveryWarehouseWithoutPartner();

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Partner is null for logistics point with id 100"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
    }

    @Test
    @DisplayName("Не найден партнёр склада для отгрузки")
    void deliveryPartnerNotFound() throws Exception {
        mockNotFoundPartner(DELIVERY_PARTNER_ID);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [200]"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
    }

    @Test
    @DisplayName("Нет существующей связки. DROPSHIP")
    void noExistingRelationDropship() {
        processNoExistingRelation();
        verifyBannerRemoving(SHOP_ID);
    }

    @SneakyThrows
    private void processNoExistingRelation() {
        mockNoRelation(DROPSHIP_PARTNER_ID);
        mockNoCapacity(DROPSHIP_PARTNER_ID);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_withdraw.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyCreateRelation(defaultCreateRelationDto(DROPSHIP_PARTNER_ID).build());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyCreateCapacity(DROPSHIP_PARTNER_ID, 100L);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Есть существующая связка с другим партнёром, она отключается")
    void disableExistingRelation() throws Exception {
        mockGetRelation(DROPSHIP_PARTNER_ID, ANOTHER_DELIVERY_PARTNER_ID);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_withdraw.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyGetPartner(ANOTHER_DELIVERY_PARTNER_ID);
        verifyUpdateRelation(
            DROPSHIP_PARTNER_ID,
            ANOTHER_DELIVERY_PARTNER_ID,
            anotherUpdateRelationDisableDto().build()
        );
        verifyCreateRelation(defaultCreateRelationDto(DROPSHIP_PARTNER_ID).build());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Обновление существующей WITHDRAW связки")
    void updateExistingWithdrawRelation() throws Exception {
        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_withdraw.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyUpdateRelation(defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true).build());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Есть отключенная связка, она включается, а включенная отключается")
    void enableExistingRelation() throws Exception {
        when(dataCampClient.getCargoTypes(BUSINESS_ID, SHOP_ID)).thenReturn(List.of(300));
        mockGetRelation(
            DROPSHIP_PARTNER_ID,
            List.of(
                defaultRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID).enabled(false).build(),
                defaultRelation(DROPSHIP_PARTNER_ID, ANOTHER_DELIVERY_PARTNER_ID).enabled(true).build()
            )
        );

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_withdraw.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyGetPartner(ANOTHER_DELIVERY_PARTNER_ID);
        verifyUpdateRelation(
            DROPSHIP_PARTNER_ID,
            ANOTHER_DELIVERY_PARTNER_ID,
            anotherUpdateRelationDisableDto().build()
        );
        verifyUpdateRelation(defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true).build());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Есть активная TPL связка с другим партнёром, она отключается")
    void disableExistingTplRelation() throws Exception {
        mockGetRelation(DROPSHIP_PARTNER_ID, ANOTHER_DELIVERY_PARTNER_ID, ShipmentType.TPL);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest(ShopShipmentType.IMPORT))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_import.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyGetPartner(ANOTHER_DELIVERY_PARTNER_ID);
        verifyUpdateRelation(
            DROPSHIP_PARTNER_ID,
            ANOTHER_DELIVERY_PARTNER_ID,
            anotherUpdateRelationDisableDto().shipmentType(ShipmentType.TPL).build()
        );
        verifyCreateRelation(
            defaultCreateRelationDto(DROPSHIP_PARTNER_ID)
                .shipmentType(ShipmentType.IMPORT)
                .intakeSchedule(null)
                .importSchedule(DEFAULT_SCHEDULE)
                .build()
        );
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Обновление существующей TPL связки")
    void updateExistingTPLRelation() throws Exception {
        mockGetRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID, ShipmentType.TPL);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_withdraw.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyUpdateRelation(
            defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true)
                .shipmentType(ShipmentType.TPL)
                .build()
        );
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Изменение точки отгрузки")
    void changeShipmentPoint() throws Exception {
        processChangingShipmentPoint();
        verifyBannerRemoving(SHOP_ID);
    }

    private void processChangingShipmentPoint() throws Exception {
        long secondDeliveryWarehouseId = 101L;

        mockGetRelation(
            DROPSHIP_PARTNER_ID,
            defaultRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID)
                .shipmentType(ShipmentType.IMPORT)
                .build()
        );
        mockGetDeliveryWarehouse(true, secondDeliveryWarehouseId);

        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultRequest(ShopShipmentType.IMPORT)
                .toPartnerLogisticsPointId(secondDeliveryWarehouseId)
                .shipmentScheduleDayIds(Set.of(61L, 62L, 63L, 64L, 65L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_import_new_warehouse.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyGetLogisticPoint(secondDeliveryWarehouseId);
        verifyUpdateRelation(
            defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true)
                .shipmentType(ShipmentType.IMPORT)
                .toPartnerLogisticsPointId(secondDeliveryWarehouseId)
                .intakeSchedule(null)
                .importSchedule(DEFAULT_SCHEDULE)
                .build()
        );
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Связка дропшипа на самопривоз")
    void dropshipImportRelation() {
        processDropshipImport();
    }

    @Test
    @DisplayName("Успешная смена склада, удаление из таблицы баннера об отключении дропоффа")
    @DatabaseSetup("/controller/business/before/dropoff_banner.xml")
    @ExpectedDatabase(
        value = "/controller/business/after/no_dropoff_banner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newBusinessWarehouseWithDeleteBanner() {
        processNoExistingRelation();
        verifyBannerRemoving(SHOP_ID);
    }

    @Test
    @DisplayName("Удаление баннера при активации уже существовавшей у партнера связки")
    @DatabaseSetup("/controller/business/before/dropoff_banner.xml")
    @DatabaseSetup(
        value = "/controller/business/update/set_201_logistic_point_to_2_availability.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/business/after/no_dropoff_banner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteBannerWhenRelationAlreadyExistButNotActive() throws Exception {
        when(dataCampClient.getCargoTypes(BUSINESS_ID, SHOP_ID)).thenReturn(List.of(300));
        mockGetDeliveryWarehouse(true, ANOTHER_DELIVERY_WAREHOUSE_ID);
        mockGetRelation(
            DROPSHIP_PARTNER_ID,
            List.of(
                defaultRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID).enabled(false).build(),
                defaultRelation(DROPSHIP_PARTNER_ID, ANOTHER_DELIVERY_PARTNER_ID)
                    .toPartnerLogisticsPointId(ANOTHER_DELIVERY_WAREHOUSE_ID)
                    .enabled(true)
                    .build()
            )
        );

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest(ShopShipmentType.IMPORT))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_import.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyGetPartner(ANOTHER_DELIVERY_PARTNER_ID);
        verifyUpdateRelation(
            DROPSHIP_PARTNER_ID,
            ANOTHER_DELIVERY_PARTNER_ID,
            anotherUpdateRelationDisableDto()
                .toPartnerLogisticsPointId(ANOTHER_DELIVERY_WAREHOUSE_ID)
                .build()
        );
        verifyUpdateRelation(
            defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true)
                .shipmentType(ShipmentType.IMPORT)
                .intakeSchedule(null)
                .importSchedule(DEFAULT_SCHEDULE)
                .build()
        );
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
        verifyBannerRemoving(SHOP_ID);
    }

    @Test
    @DisplayName("Удаление баннера при смене точки на том же партнере")
    @DatabaseSetup("/controller/business/before/dropoff_banner.xml")
    @ExpectedDatabase(
        value = "/controller/business/after/no_dropoff_banner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteBannerWhenWarehouseOfSamePartnerChanged() throws Exception {
        processChangingShipmentPoint();
        verifyBannerRemoving(SHOP_ID);
    }

    @Test
    @DisplayName("Смена склада на существующий, баннер не удаляется")
    @DatabaseSetup("/controller/business/before/dropoff_banner.xml")
    @ExpectedDatabase(
        value = "/controller/business/after/dropoff_banner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOldBusinessWarehouseWithDeleteBanner() {
        mockGetRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID, ShipmentType.IMPORT);
        processDropshipImport();
    }

    @SneakyThrows
    private void processDropshipImport() {
        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest(ShopShipmentType.IMPORT))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_import.json"));

        verifyDataCampClient(BUSINESS_ID, SHOP_ID);
        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyUpdateRelation(
            defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true)
                .shipmentType(ShipmentType.IMPORT)
                .intakeSchedule(null)
                .importSchedule(DEFAULT_SCHEDULE)
                .build()
        );
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Нет существующей связки. SUPPLIER")
    void noExistingRelationSupplier() throws Exception {
        mockNoRelation(SUPPLIER_PARTNER_ID);
        mockNoCapacity(SUPPLIER_PARTNER_ID);

        saveRelation(
            2,
            SUPPLIER_PARTNER_ID,
            defaultRequest()
                .shipmentScheduleDayIds(Set.of(41L, 42L, 43L, 44L, 45L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/create_supplier_withdraw.json"));

        verifyDataCampClient(BUSINESS_ID, 2L);
        verifyGetPartner(SUPPLIER_PARTNER_ID);
        verifyGetSingleWarehouse(SUPPLIER_PARTNER_ID);
        verifyGetRelation(SUPPLIER_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyCreateRelation(defaultCreateRelationDto(SUPPLIER_PARTNER_ID).build());
        verifyGetCapacity(SUPPLIER_PARTNER_ID);
        verifyCreateCapacity(SUPPLIER_PARTNER_ID, 100);
        verifyUpdateHandlingTime(SUPPLIER_PARTNER_ID, ONE_DAY);
        verifyBannerRemoving(SUPPLIER_PARTNER_ID);
    }

    @Test
    @DisplayName("Связка кроссдока на самопривоз, сборка на следующий день, поздний катофф")
    void supplierImportRelation() throws Exception {
        saveRelation(
            2,
            SUPPLIER_PARTNER_ID,
            defaultRequest(ShopShipmentType.IMPORT)
                .cutoffTime(LocalTime.of(23, 0))
                .capacityValue(124)
                .shipmentScheduleDayIds(Set.of(51L, 52L, 53L, 54L, 55L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/update_supplier_import.json"));

        verifyDataCampClient(BUSINESS_ID, 2L);
        verifyGetPartner(SUPPLIER_PARTNER_ID);
        verifyGetSingleWarehouse(SUPPLIER_PARTNER_ID);
        verifyGetRelation(SUPPLIER_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyUpdateRelation(
            SUPPLIER_PARTNER_ID,
            DELIVERY_PARTNER_ID,
            defaultUpdateRelationDto(SUPPLIER_PARTNER_ID, true)
                .shipmentType(ShipmentType.IMPORT)
                .intakeSchedule(null)
                .importSchedule(DEFAULT_SCHEDULE)
                .cutoffs(cutoffOf(23, 19))
                .build()
        );
        verifyGetCapacity(SUPPLIER_PARTNER_ID);
        verifyUpdateCapacity(124);
        verifyUpdateHandlingTime(SUPPLIER_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Связка кроссдока на забор, сборка день-в-день, ранний катофф")
    void supplierWithdrawRelation() throws Exception {
        saveRelation(
            2,
            SUPPLIER_PARTNER_ID,
            defaultRequest()
                .cutoffTime(LocalTime.of(8, 0))
                .handlingTime(0)
                .shipmentScheduleDayIds(Set.of(41L, 42L, 43L, 44L, 45L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/update_supplier_withdraw.json"));

        verifyDataCampClient(BUSINESS_ID, 2L);
        verifyGetPartner(SUPPLIER_PARTNER_ID);
        verifyGetSingleWarehouse(SUPPLIER_PARTNER_ID);
        verifyGetRelation(SUPPLIER_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyUpdateRelation(
            SUPPLIER_PARTNER_ID,
            DELIVERY_PARTNER_ID,
            defaultUpdateRelationDto(SUPPLIER_PARTNER_ID, true)
                .cutoffs(cutoffOf(8, 10))
                .build()
        );
        verifyGetCapacity(SUPPLIER_PARTNER_ID);
        verifyUpdateHandlingTime(SUPPLIER_PARTNER_ID, Duration.ZERO);
    }

    @Nonnull
    private static PartnerRelationUpdateDto.Builder anotherUpdateRelationDisableDto() {
        return defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, false)
            .toPartnerId(ANOTHER_DELIVERY_PARTNER_ID);
    }

    @Nonnull
    private static CpaPartnerInterfaceRelationRequestBuilder getWithdrawRequest(Set<Long> shipmentScheduleDays) {
        return defaultRequest().shipmentScheduleDayIds(shipmentScheduleDays);
    }

    private void mockGetDeliveryWarehouseWithoutPartner() {
        doReturn(Optional.of(
            LmsFactory.createLogisticsPointResponseBuilder(
                    DELIVERY_WAREHOUSE_ID,
                    null,
                    "Delivery warehouse",
                    PointType.WAREHOUSE
                )
                .active(true)
                .schedule(scheduleDays().collect(Collectors.toSet()))
                .build()
        ))
            .when(lmsClient)
            .getLogisticsPoint(DELIVERY_WAREHOUSE_ID);
    }

    private void mockGetRelation(long fromPartnerId, long toPartnerId) {
        mockGetRelation(fromPartnerId, defaultRelation(fromPartnerId, toPartnerId).build());
    }

    private void mockGetRelation(long fromPartnerId, long toPartnerId, ShipmentType shipmentType) {
        mockGetRelation(fromPartnerId, defaultRelation(fromPartnerId, toPartnerId, shipmentType).build());
    }

    private void mockBannerRemoving() {
        doNothing().when(removeDropoffShopBannerProducer).produceTask(anyLong());
    }

    private void verifyDataCampClient(long businessId, long shopId) {
        verify(dataCampClient).getCargoTypes(businessId, shopId);
    }

    private void verifyBannerRemoving(long shopId) {
        verify(removeDropoffShopBannerProducer).produceTask(eq(shopId));
    }

}
