package ru.yandex.market.logistics.nesu.controller.business;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.nesu.controller.partner.AbstractGetAvailableShipmentOptionsTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение доступных вариантов складов для экспресс-невыкупов")
@DatabaseSetup(value = {
    "/controller/business/before/setup.xml",
    "/controller/business/before/shop_partner_settings.xml"
})
class GetAvailableReturnSortingCentersTest extends AbstractGetAvailableShipmentOptionsTest {
    @Test
    @DisplayName("Получение вариантов")
    @DatabaseSetup("/repository/logistic-point-availability/before/available_return_sorting_centers.xml")
    void search() throws Exception {
        Set<Long> warehouseIds = Set.of(210L, 200L, 110L);
        try (
            var ignored1 = mockGetBusinessWarehouse(SHOP_PARTNER_ID, 121908);
            var ignored2 = mockGetWarehouses(warehouseIds, 121905);
            var ignored3 = mockPartnerTo(1000L)
        ) {
            long dropoffPartner = 3000L;
            when(lmsClient.getLogisticsPoints(warehousesFilter(warehouseIds)))
                .thenReturn(List.of(
                    warehouse(200L, 121905, TO_PARTNER_ID).build(),
                    warehouse(210L, 121905, dropoffPartner).build(),
                    warehouse(110L, 121905, dropoffPartner).build()
                ));
            SearchPartnerFilter filter = SearchPartnerFilter.builder()
                .setIds(Set.of(TO_PARTNER_ID, dropoffPartner))
                .build();
            when(lmsClient.searchPartners(filter))
                .thenReturn(List.of(
                    shipmentPartner(TO_PARTNER_ID, false),
                    shipmentPartner(dropoffPartner, true)
                ));
            getAvailableShipmentOptions(1, SHOP_PARTNER_ID)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-return-warehouses/result.json"));
            verify(lmsClient).searchPartners(filter);
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
        verifyGetWarehouses(warehouseIds);
    }

    @Nonnull
    @Override
    protected ResultActions getAvailableShipmentOptions(long shopId, long warehouseId) throws Exception {
        return mockMvc.perform(
            get(
                "/back-office/business/warehouses/{id}/available-return-sorting-centers",
                warehouseId
            )
                .param("shopId", String.valueOf(shopId))
                .param("userId", "1")
        );
    }

    @Nonnull
    @Override
    protected MockHttpServletRequestBuilder createRequestBuilder() {
        return get("/back-office/business/warehouses/1/available-return-sorting-centers");
    }
}
