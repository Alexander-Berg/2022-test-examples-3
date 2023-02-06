package ru.yandex.market.logistics.management.controller.admin.scheduleDay;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.getGrid;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение таблицы интервалов доставки в конечных точках")
@DatabaseSetup("/data/controller/admin/scheduleDay/before/prepare_data.xml")
public class AdminDeliveryIntervalScheduleDayControllerGetGridTest extends AbstractContextualTest {

    @DisplayName("ReadOnly mode - Успешно")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void getGridSuccessReadOnly(
        @SuppressWarnings("unused") String caseName,
        Map<String, String> queryParams,
        String responsePathReadOnlyMode
    ) throws Exception {
        mockMvc.perform(getGrid().params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePathReadOnlyMode));
    }

    @Nonnull
    private static Stream<Arguments> getGridArguments() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                Map.of(),
                "data/controller/admin/scheduleDay/response/search_no_filter.json"
            ),
            Arguments.of(
                "По идентификатору партнера",
                Map.of("partner", "3000"),
                "data/controller/admin/scheduleDay/response/search_by_partner.json"
            ),
            Arguments.of(
                "По региону",
                Map.of("location", "163"),
                "data/controller/admin/scheduleDay/response/search_region.json"
            ),
            Arguments.of(
                "По дню недели",
                Map.of("dayOfWeek", "MONDAY"),
                "data/controller/admin/scheduleDay/response/search_by_day.json"
            ),
            Arguments.of(
                "По времени начала интервала",
                Map.of("fromTime", "10:00"),
                "data/controller/admin/scheduleDay/response/search_by_time_from.json"
            ),
            Arguments.of(
                "По времени конца интервала",
                Map.of("toTime", "18:00"),
                "data/controller/admin/scheduleDay/response/search_by_time_to.json"
            ),
            Arguments.of(
                "По всем параметрам",
                Map.of(
                    "partner", "3000",
                    "location", "162"
                        + "",
                    "dayOfWeek", "TUESDAY",
                    "fromTime", "10:00",
                    "toTime", "14:00"
                ),
                "data/controller/admin/scheduleDay/response/search_by_all.json"
            )
        );
    }
}
