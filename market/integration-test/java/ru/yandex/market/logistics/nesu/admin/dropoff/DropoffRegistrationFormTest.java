package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.LmsFactory;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Форма регистрации дропоффа")
@ParametersAreNonnullByDefault
class DropoffRegistrationFormTest extends AbstractContextualTest {
    private static final long DROPOFF_LOGISTIC_POINT_ID = 1L;

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение формы регистрации дропоффа без указания логистической точки")
    void getRegistrationForm() throws Exception {
        mockMvc.perform(get("/admin/dropoff-registration/new"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff/registration_form.json"));
    }

    @Test
    @DisplayName("Получение формы регистрации дропоффа с указанием логистической точки")
    void getRegistrationFormForLogisticPoint() throws Exception {
        mockGetLogisticsPoint(getDefaultLogisticsPointResponseBuilder());

        mockMvc.perform(get("/admin/dropoff-registration/new?parentId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff/registration_form_logistic_point.json"));
        verifyGetLogisticsPoint();
    }

    @Test
    @DisplayName(
        "Получение формы регистрации дропоффа с указанием логистической точки, "
            + "время работы которой меньше, чем время по умолчанию для сдачи заказов"
    )
    void getRegistrationFormAvailabilityTimeTo() throws Exception {
        mockGetLogisticsPoint(
            getDefaultLogisticsPointResponseBuilder()
                .schedule(Set.of(new ScheduleDayResponse(1L, 1, LocalTime.of(10, 0), LocalTime.of(13, 0))))
        );

        mockMvc.perform(get("/admin/dropoff-registration/new?parentId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/dropoff/registration_form_logistic_point_time_to_less_than_default.json"
            ));
        verifyGetLogisticsPoint();
    }

    @Test
    @DisplayName("Не найдена активная логистическая точка")
    void logisticPointIsInactive() throws Exception {
        mockGetLogisticsPoint(getDefaultLogisticsPointResponseBuilder().active(false));

        mockMvc.perform(get("/admin/dropoff-registration/new?parentId=1"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTICS_POINT] with ids [1]"));
        verifyGetLogisticsPoint();
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

    private void mockGetLogisticsPoint(
        LogisticsPointResponse.LogisticsPointResponseBuilder logisticsPointResponseBuilder
    ) {
        when(lmsClient.getLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID))
            .thenReturn(Optional.of(logisticsPointResponseBuilder.build()));
    }

    private void verifyGetLogisticsPoint() {
        verify(lmsClient).getLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID);
    }
}
