package ru.yandex.market.logistics.tarifficator.admin.tariffDestinationPartner;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.admin.controller.AdminTariffDestinationPartnerController.PARAM_TARIFF_ID;
import static ru.yandex.market.logistics.tarifficator.admin.controller.AdminTariffDestinationPartnerController.PATH_ADMIN_TARIFFS_DESTINATION_PARTNERS;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/tariffs/db/before/tariff_destination_partners_exist_active.xml")
@DisplayName("Получение связанных ПВЗ-партнёров тарифа")
class GetTariffDestinationPartnersTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получить ПВЗ-партнёров тарифа")
    void shouldReturnDestinationPartners() throws Exception {
        doReturn(List.of(
            PartnerResponse.newBuilder().id(4L).readableName("partner_" + 4L).build()
        ))
            .when(lmsClient).searchPartners(any());

        getTariffDestinationPartners(3)
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(
                "controller/admin/tariffDestinationPartners/response/getTariffDestinationPartners_response.json"
            ));

        var expectedSearchPartnerFilter = SearchPartnerFilter.builder().setIds(Set.of(4L)).build();
        verify(lmsClient).searchPartners(expectedSearchPartnerFilter);
    }

    @Test
    @DisplayName("Получить пустой результат для тарифа без ПВЗ-партнёров")
    void shouldReturnEmpty_whenTariffHasNoDestinationPartners() throws Exception {
        getTariffDestinationPartners(1)
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/common/empty_response.json"));
    }

    @Test
    @DisplayName("Получить пустой результат для несуществующего тарифа")
    void shouldFailed_whenRequestedForNonExistingTariff() throws Exception {
        getTariffDestinationPartners(5)
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/common/empty_response.json"));
    }

    @Test
    @DisplayName("Ошибка при про попытке получить ПВЗ-партнёров без указания тарифа")
    void shouldFailed_whenRequestedWithoutTariffId() throws Exception {
        getTariffDestinationPartners()
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("tariffId", "long"));
    }

    @Test
    @DisplayName("Поиск для тарифа без партнеров в tariff_destination_partner")
    void noDestinationTariffPartners() throws Exception {
        getTariffDestinationPartners(2)
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/common/empty_response.json"));
    }

    @Nonnull
    ResultActions getTariffDestinationPartners(long tariffId) throws Exception {
        return mockMvc.perform(
            get(PATH_ADMIN_TARIFFS_DESTINATION_PARTNERS)
                .param(PARAM_TARIFF_ID, Long.toString(tariffId))
        );
    }

    @Nonnull
    ResultActions getTariffDestinationPartners() throws Exception {
        return mockMvc.perform(
            get(PATH_ADMIN_TARIFFS_DESTINATION_PARTNERS)
        );
    }
}
