package ru.yandex.market.logistics.nesu.controller.shipment;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ShipmentConfirmationDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.error.EntityError;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Подтверждение отгрузки")
@DatabaseSetup("/repository/shipments/database_prepare.xml")
class ShipmentConfirmTest extends AbstractContextualTest {

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setUp() {
        mockSearchShipments(List.of(ShipmentSearchDto.builder().marketIdFrom(100L).build()));
    }

    @Test
    @DisplayName("Успешное подтверждение")
    void successConfirmation() throws Exception {
        when(lomClient.confirmShipmentApplication(1L))
            .thenReturn(createConfirmationResult(List.of()));
        confirm(1L)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ошибка при подтверждении")
    void failedConfirmation() throws Exception {
        when(lomClient.confirmShipmentApplication(1L))
            .thenReturn(createConfirmationResult(List.of(createConfirmationError())));
        confirm(1L)
            .andExpect(status().isPreconditionFailed())
            .andExpect(jsonContent("controller/shipment/response/shipment_confirmation_error_cutoff_reached.json"));
    }

    @Test
    @DisplayName("Ошибка при подтверждении: заказ в статусе черновик")
    void failedConfirmationOrderDraft() throws Exception {
        when(lomClient.confirmShipmentApplication(1L))
            .thenReturn(createConfirmationResult(List.of(createConfirmationOrderError())));
        confirm(1L)
            .andExpect(status().isPreconditionFailed())
            .andExpect(jsonContent("controller/shipment/response/shipment_confirmation_error_draft.json"));
    }

    @Test
    @DisplayName("Ошибка при подтверждении: неподдерживаемый тип сущности")
    void failedConfirmationUnknownEntityType() throws Exception {
        when(lomClient.confirmShipmentApplication(1L))
            .thenReturn(createConfirmationResult(List.of(createConfirmationUnknownEntityTypeError())));
        confirm(1L)
            .andExpect(status().isPreconditionFailed())
            .andExpect(jsonContent(
                "controller/shipment/response/shipment_confirmation_error_unknown_entity_type.json"
            ));
    }

    @Test
    @DisplayName("Отключенный магазин")
    @DatabaseSetup(value = "/repository/shop/before/disabled_shop.xml", type = DatabaseOperation.UPDATE)
    void disabledShop() throws Exception {
        confirm(1)
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Попытка подтверждения чужой отгрузки")
    void failedConfirmationAlien() throws Exception {
        mockSearchShipments(List.of());
        confirm(1L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHIPMENT_APPLICATION] with ids [1]"));

    }

    @Test
    @DisplayName("Отгрузка не найдена")
    void failedConfirmationNotFoundShipment() throws Exception {
        mockSearchShipments(List.of());
        confirm(1L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHIPMENT_APPLICATION] with ids [1]"));

    }

    @Test
    @DisplayName("Магазин не найден")
    void failedConfirmationNotFoundShop() throws Exception {
        confirm(42L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [42]"));
    }

    @Nonnull
    private ResultActions confirm(long shopId) throws Exception {
        return mockMvc.perform(
            put("/back-office/shipments/1/confirm")
                .param("userId", "1")
                .param("shopId", String.valueOf(shopId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        );
    }

    private void mockSearchShipments(List<ShipmentSearchDto> result) {
        doReturn(createPageResult(result))
            .when(lomClient)
            .searchShipments(
                safeRefEq(
                    ShipmentSearchFilter.builder()
                        .marketIdFrom(100L)
                        .withApplication(true)
                        .shipmentApplicationIds(Set.of(1L))
                        .build()
                ),
                any()
            );
    }

    @Nonnull
    private ShipmentConfirmationDto createConfirmationResult(List<EntityError> errors) {
        return ShipmentConfirmationDto.builder().errors(errors).build();
    }

    @Nonnull
    private PageResult<ShipmentSearchDto> createPageResult(List<ShipmentSearchDto> values) {
        return new PageResult<ShipmentSearchDto>()
            .setData(values)
            .setTotalPages(1)
            .setPageNumber(0)
            .setTotalElements(values.size())
            .setSize(10);
    }

    @Nonnull
    private EntityError createConfirmationError() {
        return EntityError.builder()
            .entityType(EntityType.SHIPMENT_APPLICATION)
            .errorCode(EntityError.ErrorCode.CUTOFF_REACHED)
            .id(1L)
            .build();
    }

    @Nonnull
    private EntityError createConfirmationOrderError() {
        return EntityError.builder()
            .entityType(EntityType.ORDER)
            .errorCode(EntityError.ErrorCode.DRAFT)
            .id(10L)
            .build();
    }

    @Nonnull
    private EntityError createConfirmationUnknownEntityTypeError() {
        return EntityError.builder()
            .entityType(EntityType.PARTNER)
            .errorCode(EntityError.ErrorCode.OTHER)
            .id(100L)
            .build();
    }
}
