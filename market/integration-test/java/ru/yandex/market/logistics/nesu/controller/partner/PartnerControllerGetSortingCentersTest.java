package ru.yandex.market.logistics.nesu.controller.partner;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.utils.ShopAvailableDeliveriesUtils;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение списка всех доступных сортировочных центров")
@DatabaseSetup("/repository/shop-deliveries-availability/setup.xml")
class PartnerControllerGetSortingCentersTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, mbiApiClient);
    }

    @Test
    @DisplayName("Получение информации о всех сортировочных центрах")
    void getSortingCenters() throws Exception {
        ShopAvailableDeliveriesUtils.mockShopAvailableDeliveries(lmsClient);
        mockMvc.perform(
            get("/back-office/partner/sorting-centers")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/all_sorting_centers_response.json"));
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.AVAILABLE_DIRECTLY_PARTNERS_FILTER);
    }
}
