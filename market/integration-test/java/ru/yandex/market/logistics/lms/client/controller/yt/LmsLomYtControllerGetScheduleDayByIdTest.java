package ru.yandex.market.logistics.lms.client.controller.yt;

import java.time.LocalTime;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.NestedServletException;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Ручка для тестирования получения интервала доставки по идентификатору из yt")
class LmsLomYtControllerGetScheduleDayByIdTest extends AbstractContextualTest {

    private static final String YT_ACTUAL_VERSION = "2022-03-02T08:05:24Z";
    private static final String GET_SCHEDULE_DAY_BY_ID_PATH = "/lms/test-yt/schedule/1";
    private static final String GET_SCHEDULE_DAY_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/schedule_day_by_id_dyn] "
        + "WHERE id = 1";

    @Autowired
    private LmsYtProperties lmsYtProperties;

    @Autowired
    private Yt hahnYt;

    @Autowired
    private YtTables ytTables;

    @BeforeEach
    void setUp() {
        doReturn(ytTables)
            .when(hahnYt).tables();

        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, YT_ACTUAL_VERSION);
    }

    @AfterEach
    void tearDown() {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(ytTables, GET_SCHEDULE_DAY_QUERY);
        verify(hahnYt, times(2)).tables();

        verifyNoMoreInteractions(ytTables, hahnYt);
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение интервала доставки")
    void successGetScheduleDay() {
        YtUtils.mockSelectRowsFromYt(ytTables, scheduleDayResponse(), GET_SCHEDULE_DAY_QUERY);
        mockMvc.perform(get(GET_SCHEDULE_DAY_BY_ID_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/schedule_day_by_id.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Интервал доставки не найден")
    void noScheduleDayFound() {
        YtUtils.mockSelectRowsFromYt(ytTables, Optional.empty(), GET_SCHEDULE_DAY_QUERY);
        mockMvc.perform(get(GET_SCHEDULE_DAY_BY_ID_PATH))
            .andExpect(status().isOk())
            .andExpect(content().string("null"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка при обращении к yt")
    void errorInYt() {
        YtUtils.mockExceptionCallingYt(ytTables, GET_SCHEDULE_DAY_QUERY, new RuntimeException("Some yt exception"));

        softly.assertThatCode(
                () -> mockMvc.perform(get(GET_SCHEDULE_DAY_BY_ID_PATH))
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception"
            );
    }

    @Nonnull
    private Optional<ScheduleDayResponse> scheduleDayResponse() {
        return Optional.of(new ScheduleDayResponse(
            1L,
            2,
            LocalTime.of(10, 30),
            LocalTime.of(20, 30),
            true
        ));
    }
}
