package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
@DisplayName("Получение детальной карточки запроса на отключение дропоффа")
class DropoffDisablingDetailTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение детальной карточки")
    void getDetail() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder()
                .id(1L)
                .name("point1")
                .build()
        ));

        mockMvc.perform(get("/admin/dropoff-disabling/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff-disabling/detail.json"));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Получение карточки с недоступной отменой")
    void getDetailCancelDisabled() throws Exception {
        when(lmsClient.getLogisticsPoint(3L)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder()
                .id(3L)
                .name("point3")
                .build()
        ));

        mockMvc.perform(get("/admin/dropoff-disabling/3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff-disabling/detail_cancel_disabled.json"));

        verify(lmsClient).getLogisticsPoint(3L);
    }

    @Test
    @DisplayName("Ошибка. Запись не найдена")
    void getDetailUnknownId() throws Exception {
        mockMvc.perform(get("/admin/dropoff-disabling/5"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DROPOFF_DISABLING_REQUEST] with ids [5]"));
    }
}
