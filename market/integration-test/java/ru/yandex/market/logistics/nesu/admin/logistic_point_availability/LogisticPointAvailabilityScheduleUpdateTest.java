package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.response.LogisticPointAvailabilityScheduleDayDetailDto;
import ru.yandex.market.logistics.nesu.admin.model.response.LogisticPointAvailabilityScheduleDayDetailDto.LogisticPointAvailabilityScheduleDayDetailDtoBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
@DatabaseSetup("/repository/logistic-point-availability/before/schedule_prepare_data.xml")
class LogisticPointAvailabilityScheduleUpdateTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получение слота отгрузки конфигурации доступности склада для магазинов")
    void getDetail() throws Exception {
        mockMvc.perform(get("/admin/logistic-point-availability/schedule/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/logistic-point-availability/schedule/get_detail.json"));
    }

    @Test
    @DisplayName(
        "Получение слота отгрузки конфигурации доступности склада для магазинов по несуществующему идентификатору"
    )
    void getDetailUnknownId() throws Exception {
        mockMvc.perform(get("/admin/logistic-point-availability/schedule/2"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY_SCHEDULE_DAY] with ids [2]"));
    }

    @Test
    @DisplayName("Редактирование слота отгрузки конфигурации доступности склада для магазинов")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/schedule_update_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void update() throws Exception {
        mockMvc.perform(request(
            HttpMethod.PUT,
            "/admin/logistic-point-availability/schedule/1",
            getDetailDtoBaseBuilder().build()
        ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/logistic-point-availability/schedule/update_result.json"));
    }

    @Test
    @DisplayName("Редактирование слота отгрузки конфигурации доступности склада для магазинов "
        + "по несуществующему идентификатору")
    void updateUnknownId() throws Exception {
        mockMvc.perform(request(
            HttpMethod.PUT,
            "/admin/logistic-point-availability/schedule/2",
            getDetailDtoBaseBuilder().build()
        ))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY_SCHEDULE_DAY] with ids [2]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("updatingArgumentsIncorrectFieldsValues")
    @DisplayName("Попытка редактирования слота отгрузки конфигурации доступности склада с ошибками валидации")
    void updateBadRequest(
        @SuppressWarnings("unused") String caseName,
        LogisticPointAvailabilityScheduleDayDetailDto detailDto,
        String errorFieldName,
        String message,
        String code,
        Map<String, Object> arguments
    ) throws Exception {
        mockMvc.perform(request(
            HttpMethod.PUT,
            "/admin/logistic-point-availability/schedule/1",
            detailDto
        ))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError(errorFieldName, message, "logisticPointAvailabilityScheduleDayDetailDto", code, arguments)
            )));
    }

    @Nonnull
    public static Stream<Arguments> updatingArgumentsIncorrectFieldsValues() {
        return Stream.of(
            Arguments.of(
                "Не указан день недели",
                getDetailDtoBaseBuilder().day(null).build(),
                "day",
                "Обязательно для заполнения",
                "NotNull",
                Map.of()
            ),
            Arguments.of(
                "Не указано время начала",
                getDetailDtoBaseBuilder().from(null).build(),
                "from",
                "Обязательно для заполнения",
                "NotNull",
                Map.of()
            ),
            Arguments.of(
                "Не указано время окончания",
                getDetailDtoBaseBuilder().to(null).build(),
                "to",
                "Обязательно для заполнения",
                "NotNull",
                Map.of()
            ),
            Arguments.of(
                "Номер дня недели меньше 1",
                getDetailDtoBaseBuilder().day((short) 0).build(),
                "day",
                "Значение должно быть не меньше 1",
                "Min",
                Map.of("value", 1)
            ),
            Arguments.of(
                "Номер дня недели больше 7",
                getDetailDtoBaseBuilder().day((short) 8).build(),
                "day",
                "Значение должно быть не больше 7",
                "Max",
                Map.of("value", 7)
            )
        );
    }

    @Nonnull
    private static LogisticPointAvailabilityScheduleDayDetailDtoBuilder getDetailDtoBaseBuilder() {
        return LogisticPointAvailabilityScheduleDayDetailDto.builder()
            .logisticPointAvailabilityId(new ReferenceObject("1", "1", "nesu/logistic-point-availability"))
            .day((short) 2)
            .from(LocalTime.of(9, 0))
            .to(LocalTime.of(18, 0))
            .enabled(false);
    }
}
