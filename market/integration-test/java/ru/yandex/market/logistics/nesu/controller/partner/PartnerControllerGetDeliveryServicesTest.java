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

@DisplayName("Получение списка всех доступных служб доставки")
@DatabaseSetup("/repository/shop-deliveries-availability/setup.xml")
class PartnerControllerGetDeliveryServicesTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, mbiApiClient);
    }

    @Test
    @DisplayName("Получение информации о всех службах доставки")
    void getDeliveryServices() throws Exception {
        ShopAvailableDeliveriesUtils.mockShopAvailableDeliveries(lmsClient);
        mockMvc.perform(
            get("/back-office/partner/delivery")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/all_deliveries_response.json"));

        verify(lmsClient).searchPartnerRelation(ShopAvailableDeliveriesUtils.SORTING_CENTER_RELATION_FILTER);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.ALL_AVAILABLE_PARTNERS_FILTER);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER);
    }
}
