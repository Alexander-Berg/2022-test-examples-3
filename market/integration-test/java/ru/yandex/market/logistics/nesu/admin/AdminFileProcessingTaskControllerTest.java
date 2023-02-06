package ru.yandex.market.logistics.nesu.admin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminFileProcessingTaskStatus;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminFileProcessingTaskType;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminFileProcessingTaskSearchFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@ParametersAreNonnullByDefault
@DisplayName("Операции с задачами на обработку файлов")
@DatabaseSetup("/repository/file-processing-task/before.xml")
class AdminFileProcessingTaskControllerTest extends AbstractContextualTest {

    @SneakyThrows
    @MethodSource
    @DisplayName("Успешный поиск задачи на обработку файла")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void successSearch(String name, AdminFileProcessingTaskSearchFilter filter, String expectedPath) {
        mockMvc.perform(get("/admin/file-processing-task").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectedPath));
    }

    @Nonnull
    private static Stream<Arguments> successSearch() {
        return Stream.of(
            Arguments.of(
                "Фильтр по всем параметрам",
                createFilter(
                    AdminFileProcessingTaskStatus.PROCESSING,
                    AdminFileProcessingTaskType.UPDATE_LOGISTIC_POINT_AVAILABILITIES,
                    LocalDateTime.ofInstant(
                        Instant.parse("2022-07-19T16:30:00Z"),
                        ZoneId.ofOffset("UTC", ZoneOffset.UTC)
                    )
                ),
                "controller/admin/file-processing-task/search.json"
            ),
            Arguments.of(
                "Фильтр по типу",
                createFilter(
                    null,
                    AdminFileProcessingTaskType.UPDATE_LOGISTIC_POINT_AVAILABILITIES,
                    null
                ),
                "controller/admin/file-processing-task/search_type.json"
            ),
            Arguments.of(
                "Фильтр по статусу",
                createFilter(
                    AdminFileProcessingTaskStatus.CREATED,
                    null,
                    null
                ),
                "controller/admin/file-processing-task/search_status.json"
            ),
            Arguments.of(
                "Фильтр по дате создания",
                createFilter(
                    null,
                    null,
                    LocalDateTime.ofInstant(
                        Instant.parse("2022-07-19T16:30:00Z"),
                        ZoneId.ofOffset("UTC", ZoneOffset.UTC)
                    )
                ),
                "controller/admin/file-processing-task/search_date.json"
            )
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение детальной карточки")
    void getFileProcessingTask() {
        mockMvc.perform(get("/admin/file-processing-task/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/file-processing-task/get_detail.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Задача не найдена")
    void notFoundFileProcessingTask() {
        mockMvc.perform(get("/admin/file-processing-task/11"))
            .andExpect(status().isNotFound());
    }

    @Nonnull
    private static AdminFileProcessingTaskSearchFilter createFilter(
        @Nullable AdminFileProcessingTaskStatus status,
        @Nullable AdminFileProcessingTaskType type,
        @Nullable LocalDateTime localDateTime
    ) {
        var filter = new AdminFileProcessingTaskSearchFilter();
        filter.setStatus(status);
        filter.setType(type);
        filter.setCreated(localDateTime);
        return filter;
    }

}
