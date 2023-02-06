package ru.yandex.market.logistics.management.controller.admin.logisticsServices;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@ParametersAreNonnullByDefault
@DisplayName("Календарь сервисов логистических сегментов")
@DatabaseSetup("/data/controller/admin/logisticSegments/services/calendar/prepare_data.xml")
public class AdminLogisticServicesCalendarTest extends AbstractContextualTest {
    @DisplayName("Расписание в недельном календаре")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS})
    void logisticSegmentServiceCalendar(
        @SuppressWarnings("unused") String displayName,
        Map<String, String> queryParams,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-services/1/schedule").params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> logisticSegmentServiceCalendar() {
        return Stream.of(
            Arguments.of(
                "Расписание на 7 дней",
                Map.of(
                    "fromDate", "2020-12-21",
                    "toDate", "2020-12-27"
                ),
                "data/controller/admin/logisticSegments/services/calendar/response/schedule_7_days.json"
            ),
            Arguments.of(
                "Расписание на 3 дня",
                Map.of(
                    "fromDate", "2020-12-22",
                    "toDate", "2020-12-24"
                ),
                "data/controller/admin/logisticSegments/services/calendar/response/schedule_3_days.json"
            ),
            Arguments.of(
                "Расписание на 1 день",
                Map.of(
                    "fromDate", "2020-12-21",
                    "toDate", "2020-12-21"
                ),
                "data/controller/admin/logisticSegments/services/calendar/response/schedule_1_day.json"
            )
        );
    }

    @DisplayName("Расписание в недельном календаре - некорректные запросы")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS})
    void logisticSegmentServiceCalendarBadRequests(
        @SuppressWarnings("unused") String displayName,
        Map<String, String> queryParams
    ) throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-services/1/schedule").params(toParams(queryParams)))
            .andExpect(status().isBadRequest());
    }

    @Nonnull
    private static Stream<Arguments> logisticSegmentServiceCalendarBadRequests() {
        return Stream.of(
            Arguments.of("Не передан fromDate", Map.of("toDate", "2020-12-27")),
            Arguments.of("Не передан toDate", Map.of("fromDate", "2020-12-21")),
            Arguments.of("Расписание на 8 дней", Map.of("fromDate", "2020-12-21", "toDate", "2020-12-28")),
            Arguments.of(
                "Дата начала позже даты окончания",
                Map.of(
                    "fromDate", "2020-12-22",
                    "toDate", "2020-12-21"
                )
            )
        );
    }
}
