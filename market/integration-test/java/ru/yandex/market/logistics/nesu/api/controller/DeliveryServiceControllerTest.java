package ru.yandex.market.logistics.nesu.api.controller;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.api.AbstractApiTest;
import ru.yandex.market.logistics.nesu.utils.ShopAvailableDeliveriesUtils;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createWarehousesFilter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/shop-deliveries-availability/setup.xml")
class DeliveryServiceControllerTest extends AbstractApiTest {

    private static final long SHOP_ID = 1L;
    private static final LogisticsPointFilter WAREHOUSES_FILTER = createWarehousesFilter(Set.of(3L, 4L, 5L, 45L));

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void setup() {
        authHolder.mockAccess(mbiApiClient, SHOP_ID);
    }

    @Test
    @DisplayName("Получение информации о всех службах доставки")
    void getDeliveryServices() throws Exception {
        ShopAvailableDeliveriesUtils.mockShopAvailableDeliveries(lmsClient);
        when(lmsClient.getLogisticsPoints(WAREHOUSES_FILTER))
            .thenReturn(List.of(
                createLogisticsPointResponse(4L, 5L, "House 4"),
                createLogisticsPointResponse(2L, 4L, "House 2"),
                createLogisticsPointResponse(1L, 5L, "House 1"),
                createLogisticsPointResponse(3L, 3L, "House 3")
            ));

        mockMvc.perform(requestDeliveryServices("1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/deliveryservice/get_all_delivery_services_response.json"));

        verify(lmsClient).searchPartnerRelation(ShopAvailableDeliveriesUtils.SORTING_CENTER_RELATION_FILTER);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.ALL_AVAILABLE_PARTNERS_FILTER);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER);
        verify(lmsClient).getLogisticsPoints(WAREHOUSES_FILTER);
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение информации о недоступных службах доставки")
    void getForbiddenDeliveryServices() throws Exception {
        mockMvc.perform(requestDeliveryServices("2"))
            .andExpect(status().isForbidden());

        verifyZeroInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение информации о несуществующих службах доставки")
    void getNoDeliveryServices() throws Exception {
        when(lmsClient.searchPartnerRelation(ShopAvailableDeliveriesUtils.SORTING_CENTER_RELATION_FILTER))
            .thenReturn(List.of());
        when(lmsClient.searchPartners(ShopAvailableDeliveriesUtils.AVAILABLE_DIRECTLY_PARTNERS_FILTER))
            .thenReturn(List.of());
        when(lmsClient.searchPartners(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER)).thenReturn(List.of());

        mockMvc.perform(requestDeliveryServices("1"))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));

        verify(lmsClient).searchPartnerRelation(ShopAvailableDeliveriesUtils.SORTING_CENTER_RELATION_FILTER);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.AVAILABLE_DIRECTLY_PARTNERS_FILTER);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER);
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение информации о службах доставки без складов")
    void getDeliveryServicesWithoutWarehouses() throws Exception {
        ShopAvailableDeliveriesUtils.mockShopAvailableDeliveries(lmsClient);
        when(lmsClient.getLogisticsPoints(WAREHOUSES_FILTER))
            .thenReturn(List.of(
                createLogisticsPointResponse(4L, 5L, "House 4"),
                createLogisticsPointResponse(2L, 4L, "House 2"),
                createLogisticsPointResponse(1L, 5L, "House 1"),
                createLogisticsPointResponse(3L, 3L, "House 3")
            ));

        doReturn(List.of()).when(lmsClient).searchPartners(eq(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER));
        LogisticsPointFilter warehousesFilter = createWarehousesFilter(Set.of(3L, 4L, 5L));
        when(lmsClient.getLogisticsPoints(warehousesFilter)).thenReturn(List.of());

        mockMvc.perform(requestDeliveryServices("1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/deliveryservice/get_deliveries_without_warehouses.json"));

        verify(lmsClient).searchPartnerRelation(ShopAvailableDeliveriesUtils.SORTING_CENTER_RELATION_FILTER);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.ALL_AVAILABLE_PARTNERS_FILTER);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER);
        verify(lmsClient).getLogisticsPoints(warehousesFilter);
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение информации без ид кабинета")
    void getDeliveryServicesWithoutCabinetId() throws Exception {
        mockMvc.perform(requestDeliveryServices(null))
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("cabinetId", "Long"));

        verifyZeroInteractions(lmsClient);
    }

    @NotNull
    private MockHttpServletRequestBuilder requestDeliveryServices(String cabinetId) {
        return get("/api/delivery-services")
            .headers(authHeaders())
            .param("cabinetId", cabinetId);
    }

    @NotNull
    private LogisticsPointResponse createLogisticsPointResponse(Long pointId, Long partnerId, String addressString) {
        return LogisticsPointResponse.newBuilder()
            .id(pointId)
            .partnerId(partnerId)
            .externalId("externalId")
            .type(PointType.WAREHOUSE)
            .name("Имя склада")
            .address(createAddressDto(addressString))
            .active(true)
            .businessId(41L)
            .build();
    }

    @Nonnull
    private Address createAddressDto(String addressString) {
        return Address.newBuilder()
            .settlement("Новосибирск")
            .postCode("649220")
            .street("Николаева")
            .house("11")
            .housing("")
            .building("")
            .apartment("")
            .comment("как проехать")
            .addressString(addressString)
            .build();
    }
}
