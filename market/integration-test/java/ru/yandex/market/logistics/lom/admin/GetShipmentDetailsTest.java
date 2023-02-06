package ru.yandex.market.logistics.lom.admin;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение детальной карточки отгрузки")
class GetShipmentDetailsTest extends AbstractContextualTest {
    private static final LogisticsPointFilter WAREHOUSE_FILTER = LogisticsPointFilter.newBuilder()
        .ids(Set.of(1L))
        .build();

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setupMocks() {
        when(lmsClient.getLogisticsPoints(WAREHOUSE_FILTER))
            .thenReturn(List.of(LmsFactory.createWarehouseResponseBuilder(1).build()));
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение информации об отгрузке")
    @DatabaseSetup("/controller/admin/shipment/before/shipments.xml")
    void getShipmentInfo() throws Exception {
        SearchPartnerFilter partnerFilter = SearchPartnerFilter.builder()
            .setIds(Set.of(1L))
            .build();

        when(lmsClient.searchPartners(partnerFilter))
            .thenReturn(List.of(
                PartnerResponse.newBuilder()
                    .id(1L)
                    .readableName("Имя партнёра")
                    .build()
            ));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shipment/response/detail.json"));

        verify(lmsClient).searchPartners(partnerFilter);
        verify(lmsClient).getLogisticsPoints(WAREHOUSE_FILTER);
    }

    @Test
    @DisplayName("Получение информации об отгрузке без заявки")
    @DatabaseSetup("/controller/admin/shipment/before/shipment_without_application.xml")
    void getShipmentInfoWithoutShipmentApplication() throws Exception {
        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shipment/response/shipment_without_application.json"));

        verify(lmsClient).getLogisticsPoints(WAREHOUSE_FILTER);
    }

    @Test
    @DisplayName("Неизвестный идентификатор")
    void shipmentNotFound() throws Exception {
        getShipment()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/admin/shipment/response/not_found.json"));
    }

    @Nonnull
    private ResultActions getShipment() throws Exception {
        return mockMvc.perform(get("/admin/shipments/1"));
    }
}
