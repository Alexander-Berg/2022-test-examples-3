package ru.yandex.market.logistics.nesu.controller.settings;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.dto.SenderRegionDeliverySettings;
import ru.yandex.market.logistics.nesu.dto.SenderRegionDeliverySettings.SenderRegionDeliverySettingsBuilder;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryType;
import ru.yandex.market.logistics.nesu.jobs.producer.ModifierUploadTaskProducer;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.model.dto.SenderDeliverySettingsDto;
import ru.yandex.market.logistics.nesu.model.dto.SenderDeliverySettingsDto.SenderDeliverySettingsDtoBuilder;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.resourceNotFoundMatcher;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Тесты обновления настроек СД АПИ SettingsController")
@DatabaseSetup("/repository/settings/sender/before/logistics_point_availabilities_dataset.xml")
class SettingsControllerUpdateDeliverySettingsTest extends AbstractSettingsControllerTest {

    @Autowired
    private ModifierUploadTaskProducer producer;

    @BeforeEach
    void setup() {
        doNothing().when(producer).produceTask(anyLong());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(producer);
    }

    @Test
    @DisplayName("Добавление нового региона связей сендера со службами доставки")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_deliveries_dataset.xml")
    @ExpectedDatabase(
        value = "/repository/settings/sender/after/after_region_delivery_added.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addNewRegionDelivery() throws Exception {
        mockAvailableWarehousesAndPartners();
        updateDeliverySettings("controller/settings/sender/add_new_region_delivery.json")
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Добавление новых связей сендера со службами доставки в регионе")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_region_deliveries_dataset.xml")
    @ExpectedDatabase(
        value = "/repository/settings/sender/after/after_delivery_added_to_region.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addNewDeliveryToExistingRegion() throws Exception {
        mockAvailableWarehousesAndPartners();
        mockSortingCenterRelation();

        updateDeliverySettings("controller/settings/sender/add_new_delivery_to_region.json")
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Изменение СЦ в регионе")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_region_deliveries_dataset.xml")
    @ExpectedDatabase(
        value = "/repository/settings/sender/after/after_sc_warehouse_changed_in_region.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateScWarehouseInRegion() throws Exception {
        mockAvailableWarehousesAndPartners();

        updateDeliverySettings("controller/settings/sender/change_sc_warehouse_in_region.json")
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Удаление связи сендера со службой доставки в регионе")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_region_deliveries_dataset.xml")
    @ExpectedDatabase(
        value = "/repository/settings/sender/after/after_delivery_removed_from_region.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void deleteRegionDeliveryService() throws Exception {
        mockAvailableWarehousesAndPartners();

        updateDeliverySettings("controller/settings/sender/remove_delivery_from_region.json")
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Нет настройки доступности для склада СД для доставки напрямую для указанного типа отгрузки")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_deliveries_dataset.xml")
    void deliveryServiceShipmentTypeIsNotAvailable() throws Exception {
        mockAvailableWarehousesAndPartners();

        updateDeliverySettings("controller/settings/sender/add_not_available_delivery_to_region.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/settings/sender/sender_8_settings_error_response.json"));
    }

    @Test
    @DisplayName("СД недоступна для отгрузки через СЦ из-за отсутствия связки в LMS")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_deliveries_dataset.xml")
    void deliveryServiceIsNotAvailableViaSc() throws Exception {
        mockAvailableWarehousesAndPartners();

        updateDeliverySettings("controller/settings/sender/add_delivery_via_sc_to_region_no_partner_relation.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/settings/sender/sender_1_settings_error_response.json"));
    }

    @Test
    @DisplayName("СД недоступна для отгрузки через СЦ из-за отсутствия необходимого типа отгрузки")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_deliveries_dataset.xml")
    void deliveryServiceIsNotAvailableViaScNoShipmentType() throws Exception {
        mockAvailableWarehousesAndPartners();
        mockSortingCenterRelation();

        updateDeliverySettings("controller/settings/sender/add_delivery_via_sc_to_region_no_shipment_type.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/settings/sender/sender_1_settings_error_response.json"));
    }

    @Test
    @DisplayName("Для СД указан неверный склад для отгрузки через СЦ")
    @DatabaseSetup("/repository/settings/sender/before/sender_with_region_deliveries_dataset.xml")
    void notValidWarehouseIdForDeliveryViaSc() throws Exception {
        mockAvailableWarehousesAndPartners();
        mockSortingCenterRelation();

        updateDeliverySettings("controller/settings/sender/add_new_delivery_to_region_not_valid_warehouse_via_sc.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/settings/sender/not_valid_warehouse_for_delivery_via_sc_response.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("updateRequestValidationSource")
    @DisplayName("Валидация запроса на обновление")
    void updateRequestValidation(
        ValidationErrorDataBuilder error,
        SenderRegionDeliverySettingsBuilder settings
    ) throws Exception {
        updateDeliverySettings(settings.build())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject("senderRegionDeliverySettings")));
    }

    @Nonnull
    private static Stream<Arguments> updateRequestValidationSource() {
        return Stream.of(
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder("deliverySettings", ErrorType.NOT_EMPTY),
                defaultRequest()
                    .deliverySettings(List.of())
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder("deliverySettings", ErrorType.NOT_NULL_ELEMENTS),
                defaultRequest()
                    .deliverySettings(Collections.singletonList(null))
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder("deliverySettings[0].shipmentType", ErrorType.NOT_NULL),
                defaultRequest()
                    .deliverySettings(List.of(defaultDeliverySetting().shipmentType(null).build()))
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder("deliverySettings[0].deliveryServiceId", ErrorType.NOT_NULL),
                defaultRequest()
                    .deliverySettings(List.of(defaultDeliverySetting().deliveryServiceId(null).build()))
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder("deliverySettings[0].deliveryTypes", ErrorType.NOT_NULL_ELEMENTS),
                defaultRequest()
                    .deliverySettings(List.of(
                        defaultDeliverySetting().deliveryTypes(Collections.singletonList(null)).build()
                    ))
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder("locationId", ErrorType.NOT_NULL),
                defaultRequest()
                    .locationId(null)
            ),
            Arguments.of(
                ValidationErrorData.fieldErrorBuilder("sortingCenterWarehouseId", ErrorType.NOT_NULL),
                defaultRequest()
                    .sortingCenterWarehouseId(null)
            )
        );
    }

    @Nonnull
    private static SenderRegionDeliverySettingsBuilder defaultRequest() {
        return SenderRegionDeliverySettings.builder()
            .locationId(1)
            .sortingCenterWarehouseId(100L)
            .deliverySettings(List.of(defaultDeliverySetting().build()));
    }

    @Nonnull
    private static SenderDeliverySettingsDtoBuilder defaultDeliverySetting() {
        return SenderDeliverySettingsDto.builder()
            .deliveryServiceId(1L)
            .shipmentType(ShipmentType.IMPORT)
            .deliveryTypes(List.of(DeliveryType.COURIER));
    }

    @Test
    @DisplayName("Попытка обновления информации о службах доставки несуществующего сендера")
    @DatabaseSetup("/repository/settings/sender/before/common_sender.xml")
    void createOrUpdateDeliveryServicesNoSender() throws Exception {
        updateDeliverySettings("2", extractFileContent("controller/settings/sender/add_new_delivery_to_region.json"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Склад, указанный в настройках СД, не принадлежит СД")
    @DatabaseSetup({
        "/repository/settings/sender/before/common_sender.xml",
        "/repository/settings/sender/before/logistics_point_availabilities_dataset.xml",
    })
    @ExpectedDatabase(
        value = "/repository/settings/sender/before/common_sender.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrUpdateDeliveryServicesAddNewWithWrongWarehouseId() throws Exception {
        mockAvailableWarehousesAndPartners();
        mockSortingCenterRelation();
        doReturn(List.of(
            LmsFactory.createLogisticsPointResponse(
                8L,
                9L,
                "warehouse",
                PointType.WAREHOUSE
            ),
            LmsFactory.createLogisticsPointResponse(
                2L,
                2L,
                "warehouse",
                PointType.WAREHOUSE
            )
        )).when(lmsClient).getLogisticsPoints(refEq(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(2L, 8L))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        ));
        updateDeliverySettings("controller/settings/sender/add_new_delivery_with_warehouse_id.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/settings/sender/warehouse_not_belong_to_partner_response.json"));
    }

    @Test
    @DisplayName("Указанный склад не существует")
    @DatabaseSetup("/repository/settings/sender/before/common_sender.xml")
    @ExpectedDatabase(
        value = "/repository/settings/sender/before/common_sender.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrUpdateDeliveryServicesAddNewWithNonExistWarehouseId() throws Exception {
        mockAvailableWarehousesAndPartners();
        doReturn(List.of(
            LmsFactory.createLogisticsPointResponse(
                8L,
                8L,
                "warehouse",
                PointType.WAREHOUSE
            )
        )).when(lmsClient).getLogisticsPoints(refEq(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(2L, 8L))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        ));
        updateDeliverySettings("controller/settings/sender/add_new_delivery_with_warehouse_id.json")
            .andExpect(status().isNotFound())
            .andExpect(resourceNotFoundMatcher(ResourceType.WAREHOUSE, Set.of(2L)));
    }

    @Nonnull
    private ResultActions updateDeliverySettings(String contentPath) throws Exception {
        return updateDeliverySettings("1", extractFileContent(contentPath));
    }

    @Nonnull
    private ResultActions updateDeliverySettings(SenderRegionDeliverySettings settings) throws Exception {
        return updateDeliverySettings("1", objectMapper.writeValueAsString(settings));
    }

    @Nonnull
    private ResultActions updateDeliverySettings(String senderId, String content) throws Exception {
        return mockMvc.perform(
            post("/back-office/settings/sender/delivery")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", senderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        );
    }
}
