package ru.yandex.market.logistics.nesu.controller.business;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.nesu.model.LmsFactory;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение склада бизнеса")
@DatabaseSetup("/controller/business/before/setup.xml")
class GetBusinessWarehouseTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private FeatureProperties featureProperties;

    @AfterEach
    void tearDown() {
        featureProperties.setNullableBusinessWarehouseAddress(true);
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Склад не существует")
    @DatabaseSetup(value = "/controller/business/before/several_settings.xml", type = DatabaseOperation.INSERT)
    void nonExistentWarehouse() throws Exception {
        doThrow(new ResourceNotFoundException(4, ResourceType.BUSINESS_WAREHOUSE))
            .when(lmsClient).getBusinessWarehouseForPartner(4L);
        getWarehouse(1, 4)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_WAREHOUSE] with ids [4]"));
        verify(lmsClient).getBusinessWarehouseForPartner(4L);
    }

    @Test
    @DisplayName("Некорректная роль магазина")
    void wrongShopType() throws Exception {
        getWarehouse(2L, 1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Shop must have role [DROPSHIP, SUPPLIER, DROPSHIP_BY_SELLER, RETAIL] for this operation"
            ));
    }

    @Test
    @DisplayName("Отключенный магазин")
    void disabledShop() throws Exception {
        getWarehouse(5, 1)
            .andExpect(status().isForbidden())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Несуществующий магазин")
    void nonExistentShop() throws Exception {
        getWarehouse(100500, 1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [100500]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успех")
    @DatabaseSetup(value = "/controller/business/before/setting.xml", type = DatabaseOperation.INSERT)
    void getBusinessWarehouse(
        @SuppressWarnings("unused") String displayName,
        boolean nullableBusinessWarehouseAddress,
        String resultPath
    ) throws Exception {
        featureProperties.setNullableBusinessWarehouseAddress(nullableBusinessWarehouseAddress);

        doReturn(Optional.of(businessWarehouse().build())).when(lmsClient).getBusinessWarehouseForPartner(2L);
        getWarehouse(1, 2)
            .andExpect(status().isOk())
            .andExpect(jsonContent(resultPath));
        verify(lmsClient).getBusinessWarehouseForPartner(2L);
    }

    @Nonnull
    private static Stream<Arguments> getBusinessWarehouse() {
        return Stream.of(
            Arguments.of(
                "В ответе расписание null",
                true,
                "controller/business/response/get_valid_business_warehouse.json"
            ),
            Arguments.of(
                "В ответе пустое расписание",
                false,
                "controller/business/response/get_valid_business_warehouse_empty_schedule.json"
            )
        );
    }

    @Test
    @DisplayName("Успех, пустой адрес")
    @DatabaseSetup(value = "/controller/business/before/setting.xml", type = DatabaseOperation.INSERT)
    void getBusinessWarehouseEmptyAddress() throws Exception {
        doReturn(Optional.of(businessWarehouse().address(null).build())).when(lmsClient)
            .getBusinessWarehouseForPartner(2L);
        getWarehouse(1, 2)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/business/response/get_valid_business_warehouse_empty_address.json"));
        verify(lmsClient).getBusinessWarehouseForPartner(2L);
    }

    @Test
    @DisplayName("LMS вернул null")
    @DatabaseSetup(value = "/controller/business/before/setting.xml", type = DatabaseOperation.INSERT)
    void getBusinessWarehouseFail() throws Exception {
        doReturn(Optional.empty()).when(lmsClient).getBusinessWarehouseForPartner(2L);
        getWarehouse(1, 2)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_WAREHOUSE] with ids [2]"));
        verify(lmsClient).getBusinessWarehouseForPartner(2L);
    }

    @Test
    @DisplayName("Нет настройки для магазина и партнера")
    void getBusinessWarehouseNoSetting() throws Exception {
        getWarehouse(1, 1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Cannot find shop partner setting for shop 1 and partner 1"));
    }

    @Nonnull
    private BusinessWarehouseResponse.Builder businessWarehouse() {
        return BusinessWarehouseResponse.newBuilder()
            .businessId(42L)
            .marketId(100L)
            .logisticsPointId(2L)
            .logisticsPointName("business warehouse logistics point name")
            .partnerId(1L)
            .externalId("100")
            .name("business warehouse")
            .readableName("бизнес склад")
            .contact(LmsFactory.createContactDto())
            .address(LmsFactory.createAddressDto());
    }

    @Nonnull
    private ResultActions getWarehouse(long shopId, long warehouseId) throws Exception {
        return mockMvc.perform(
            get("/back-office/business/warehouses/{id}", warehouseId)
                .param("shopId", String.valueOf(shopId))
                .param("userId", "1")
        );
    }
}
