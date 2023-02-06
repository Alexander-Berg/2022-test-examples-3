package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.utils.AdminValidationUtils;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
class LogisticPointAvailabilityScheduleCreateTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получение формы создания слота отгрузки для конфигурации доступности склада для магазинов")
    void getCreationForm() throws Exception {
        mockMvc.perform(get("/admin/logistic-point-availability/schedule/new"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/logistic-point-availability/schedule/creation_form.json"));
    }

    @Test
    @DisplayName("Создание нового слота отгрузки для конфигурации доступности склада для магазинов")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/schedule_creation_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void create() throws Exception {
        create(1, "controller/admin/logistic-point-availability/schedule/create_request.json")
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Попытка создать слот отгрузки для конфигурации доступности склада по несуществующему id")
    void createUnknownLogisticPointAvailabilityId() throws Exception {
        create(4, "controller/admin/logistic-point-availability/schedule/create_request.json")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY] with ids [4]"));
    }

    @Test
    @DisplayName("Попытка создать слот отгрузки для конфигурации доступности склада для магазинов "
        + "с незаполненными обязательными полями")
    void createEmptyRequiredFields() throws Exception {
        create(1, "controller/admin/logistic-point-availability/empty_request.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                createNullFieldError("day"),
                createNullFieldError("from"),
                createNullFieldError("to")
            )));
    }

    @Test
    @DisplayName("Попытка создать слот отгрузки для конфигурации доступности склада для магазинов "
        + "с номером дня недели меньше 1")
    void createDayNumberIsLessThan1() throws Exception {
        create(1, "controller/admin/logistic-point-availability/schedule/create_request_day_less_than_1.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "day",
                "Значение должно быть не меньше 1",
                "logisticPointAvailabilityScheduleDayNewDto",
                "Min",
                Map.of("value", 1)
            )));
    }

    @Test
    @DisplayName("Попытка создать слот отгрузки для конфигурации доступности склада для магазинов "
        + "с номером дня недели больше 7")
    void createDayNumberIsGreaterThan7() throws Exception {
        create(1, "controller/admin/logistic-point-availability/schedule/create_request_day_greater_than_7.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "day",
                "Значение должно быть не больше 7",
                "logisticPointAvailabilityScheduleDayNewDto",
                "Max",
                Map.of("value", 7)
            )));
    }

    @Nonnull
    private ValidationErrorData createNullFieldError(String field) {
        return AdminValidationUtils.createNullFieldError(field, "logisticPointAvailabilityScheduleDayNewDto");
    }

    @Nonnull
    private ResultActions create(long logisticPointAvailabilityId, String requestPath) throws Exception {
        return mockMvc.perform(
            post(String.format("/admin/logistic-point-availability/schedule?parentId=%d", logisticPointAvailabilityId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }
}
