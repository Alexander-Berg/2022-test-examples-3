package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Форма отключения дропоффа")
class DropoffDisablingFormTest extends AbstractContextualTest {

    private static final long DROPOFF_LOGISTIC_POINT_ID = 1L;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-11-22T17:00:00Z"), CommonsConstants.MSK_TIME_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение формы отключения дропоффа без указания логистической точки")
    void getRegistrationForm() throws Exception {
        mockMvc.perform(get("/admin/dropoff-disabling/new"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff/disabling_form.json"));
    }

    @Test
    @DisplayName("Получение формы отключения дропоффа с указанием логистической точки")
    void getRegistrationFormForLogisticPoint() throws Exception {
        when(lmsClient.getLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID))
            .thenReturn(Optional.of(getDefaultLogisticsPointResponseBuilder().build()));

        mockMvc.perform(get("/admin/dropoff-disabling/new?parentId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff/disabling_form_logistic_point.json"));

        verify(lmsClient).getLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID);
    }

    @Nonnull
    private LogisticsPointResponse.LogisticsPointResponseBuilder getDefaultLogisticsPointResponseBuilder() {
        return LmsFactory.createLogisticsPointResponseBuilder(
            DROPOFF_LOGISTIC_POINT_ID,
            10L,
            "Dropoff point",
            PointType.PICKUP_POINT
        );
    }
}
