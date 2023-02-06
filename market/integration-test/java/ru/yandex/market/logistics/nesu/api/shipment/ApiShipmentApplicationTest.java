package ru.yandex.market.logistics.nesu.api.shipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

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

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.api.model.Dimensions;
import ru.yandex.market.logistics.nesu.api.model.shipment.ShopShipmentApplicationRequest;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.controller.shipment.ShipmentApplicationTest;
import ru.yandex.market.logistics.nesu.dto.shipments.Shipment;
import ru.yandex.market.logistics.nesu.model.LomShipmentApplicationFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание заявки на отгрузку в Open API")
class ApiShipmentApplicationTest extends ShipmentApplicationTest {

    @Autowired
    private BlackboxService blackboxService;
    @Autowired
    private MbiApiClient mbiApiClient;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
    }

    @Test
    @DisplayName("Забор, магазин не указан")
    void noShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 2L);

        createShipmentNoAuth("controller/shipment/request/create_withdraw_request.json", null)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                applicationFieldError("cabinetId", "must not be null", "NotNull")
            ));
    }

    @Test
    @DisplayName("Забор, магазин недоступен")
    void inaccessibleShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 2L);

        createShipmentNoAuth("controller/shipment/request/create_withdraw_request.json", 2L)
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Замена склада")
    @DatabaseSetup(
        value = {
            "/repository/delivery-options/api_warehouse_substitution.xml",
            "/repository/shipments/change_warehouse.xml"
        },
        type = DatabaseOperation.INSERT
    )
    void createWithdrawShipmentWithSubstitution(
        @SuppressWarnings("unused") String name,
        Long shopId,
        Long marketId,
        Long businessId,
        Long newWarehouseId
    ) throws Exception {
        authHolder.mockAccess(mbiApiClient, shopId);
        mockGetPartner(2L, 5L, PartnerType.DELIVERY);
        when(lmsClient.getLogisticsPoint(newWarehouseId))
            .thenReturn(Optional.of(createLogisticPoint(newWarehouseId, businessId, null)));

        var applicationDtoBuilder = LomShipmentApplicationFactory
            .createWithdrawShipmentApplication(null, marketId, 2L, newWarehouseId, null)
            .requisiteId(shopId.toString())
            .balanceContractId(shopId)
            .balancePersonId(100 + shopId);

        mockCreateShipmentApplication(applicationDtoBuilder.build(), applicationDtoBuilder.id(1L).build());
        mockSearchShipments();

        createShipmentNoAuth(shopShipmentApplicationRequest(newWarehouseId), shopId).andExpect(status().isOk());
        verify(lmsClient).getLogisticsPoint(newWarehouseId);
    }

    @Nonnull
    private static Stream<Arguments> createWithdrawShipmentWithSubstitution() {
        return Stream.of(
            Arguments.of("Склад был заменен", 1L, 100L, 41L, 500L),
            Arguments.of("Нет замены для склада, есть по businessId", 3L, 3L, 42L, 3L),
            Arguments.of("Нет замены по businessId, есть по складу", 4L, 4L, 44L, 4L),
            Arguments.of("Нет замены по businessId, и складу", 5L, 5L, 45L, 5L)
        );
    }

    @Override
    @Nonnull
    protected ResultActions createShipment(String requestPath, long shopId) throws Exception {
        authHolder.mockAccess(mbiApiClient, shopId);

        return createShipmentNoAuth(requestPath, shopId);
    }

    @Nonnull
    @Override
    protected ValidationErrorData applicationFieldError(String field, String message, String code) {
        return ValidationErrorData.fieldError(field, message, "shopShipmentApplicationRequest", code);
    }

    @Nonnull
    private ResultActions createShipmentNoAuth(String requestPath, Long shopId) throws Exception {
        return createShipmentNoAuth(
            objectMapper.readValue(extractFileContent(requestPath), ShopShipmentApplicationRequest.class),
            shopId
        );
    }

    @Nonnull
    private ResultActions createShipmentNoAuth(ShopShipmentApplicationRequest request, Long shopId) throws Exception {
        request.setCabinetId(shopId);

        return mockMvc.perform(post("/api/shipments/application")
            .headers(authHolder.authHeaders())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    @Nonnull
    private ShopShipmentApplicationRequest shopShipmentApplicationRequest(Long warehouseFrom) {
        ShopShipmentApplicationRequest request = new ShopShipmentApplicationRequest();
        request.setShipment(
            new Shipment()
                .setDate(LocalDate.of(2019, 6, 12))
                .setPartnerTo(2L)
                .setWarehouseFrom(warehouseFrom)
                .setType(ShipmentType.WITHDRAW)
        )
            .setIntervalId(1L)
            .setDimensions(
                new Dimensions().setWidth(15).setWeight(BigDecimal.valueOf(5.5)).setLength(10).setHeight(40)
            );
        return request;
    }
}
