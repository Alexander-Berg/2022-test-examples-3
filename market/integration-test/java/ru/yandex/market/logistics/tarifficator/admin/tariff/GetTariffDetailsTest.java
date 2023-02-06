package ru.yandex.market.logistics.tarifficator.admin.tariff;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.PlatformClient;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение детальной информации о тарифе через админку")
@DatabaseSetup("/controller/tariffs/db/search_prepare.xml")
class GetTariffDetailsTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Получить детальную информацию о тарифе")
    void getTariffDetailInfo() throws Exception {
        when(lmsClient.getPartner(1L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().id(1L).readableName("partner_1").build()));

        mockMvc.perform(get("/admin/tariffs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/tariffs/response/id_1_details.json"));
    }

    @Test
    @DisplayName("Получить детальную информацию о тарифе, партнёр не найден")
    void getTariffDetailInfoPartnerNotFound() throws Exception {
        mockMvc.perform(get("/admin/tariffs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/tariffs/response/id_1_details_partner_not_found.json"));
    }

    @Test
    @DisplayName("Получить детальную информацию о тарифе, тариф не найден")
    void getTariffDetailNotFound() throws Exception {
        mockMvc.perform(get("/admin/tariffs/5"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [[5]]"));
    }

    @Test
    @DisplayName("Получить детальную информацию о тарифе платформы")
    void getTariffDetailInfoWithPlatform() throws Exception {
        when(lmsClient.getPartner(1L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().id(1L).readableName("partner_1").build()));

        mockMvc.perform(
            get("/admin/platform/tariffs/1")
                .param("platformClients", PlatformClient.YANDEX_DELIVERY.name())
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/tariffs/response/id_1_details.json"));
    }

    @Test
    @DisplayName("Получить детальную информацию о тарифе без платформы")
    void getTariffDetailInfoWithoutPlatform() throws Exception {
        when(lmsClient.getPartner(1L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().id(1L).readableName("partner_1").build()));

        mockMvc.perform(
            get("/admin/platform/tariffs/2")
                .param("platformClients", "")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/tariffs/response/id_2_details.json"));
    }

    @Test
    @DisplayName("Получить детальную информацию о тарифе платформы, доступ запрещен")
    void getTariffDetailWithPlatformForbiddenError() throws Exception {
        mockMvc.perform(
            get("/admin/platform/tariffs/1")
                .param("platformClients", "")
        )
            .andExpect(status().isForbidden())
            .andExpect(errorMessage("Unable to access [TARIFF] with ids [1]"));
    }

    @Test
    @DisplayName("Получить детальную информацию о не принадлежащем платформе тарифе, доступ запрещен")
    void getTariffDetailWithoutPlatformForbiddenError() throws Exception {
        mockMvc.perform(
            get("/admin/platform/tariffs/2")
                .param("platformClients", PlatformClient.YANDEX_DELIVERY.name())
        )
            .andExpect(status().isForbidden())
            .andExpect(errorMessage("Unable to access [TARIFF] with ids [2]"));
    }
}
