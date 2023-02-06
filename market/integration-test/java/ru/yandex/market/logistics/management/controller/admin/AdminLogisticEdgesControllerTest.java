package ru.yandex.market.logistics.management.controller.admin;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Контроллер связей между логистическими сегментами")
@DatabaseSetup("/data/controller/admin/logisticEdges/prepare_data.xml")
@ParametersAreNonnullByDefault
class AdminLogisticEdgesControllerTest extends AbstractContextualTest {
    @DisplayName("Фильтрация логистических связей")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("filterArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_EDGES})
    void getLogisticEdgesGrid(
        @SuppressWarnings("unused") String displayName,
        Map<String, String> queryParams,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-edges").params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> filterArguments() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                Map.of(),
                "data/controller/admin/logisticEdges/response/no_filter.json"
            ),
            Arguments.of(
                "По сегменту откуда",
                Map.of("fromSegment", "10005"),
                "data/controller/admin/logisticEdges/response/filter_by_from_segment_id.json"
            ),
            Arguments.of(
                "По сегментам",
                Map.of("segment", "10001, 10002"),
                "data/controller/admin/logisticEdges/response/filter_by_segment.json"
            ),
            Arguments.of(
                "По сегменту откуда. Пустой список",
                Map.of("fromSegment", ""),
                "data/controller/admin/logisticEdges/response/empty_result.json"
            ),
            Arguments.of(
                "По партнеру откуда. Пустой список",
                Map.of("fromPartner", ""),
                "data/controller/admin/logisticEdges/response/empty_result.json"
            ),
            Arguments.of(
                "По сегменту куда. Пустой список",
                Map.of("toSegment", ""),
                "data/controller/admin/logisticEdges/response/empty_result.json"
            ),
            Arguments.of(
                "По партнеру куда. Пустой список",
                Map.of("toPartner", ""),
                "data/controller/admin/logisticEdges/response/empty_result.json"
            ),
            Arguments.of(
                "По сегментам. Пустой список",
                Map.of("segment", ""),
                "data/controller/admin/logisticEdges/response/empty_result.json"
            ),
            Arguments.of(
                "По несуществующему сегменту откуда",
                Map.of("fromSegment", "10006"),
                "data/controller/admin/logisticEdges/response/empty_result.json"
            ),
            Arguments.of(
                "По паре сегментов откуда - куда",
                Map.of("fromSegment", "10005", "toSegment", "10006"),
                "data/controller/admin/logisticEdges/response/filter_by_both_segment_ids.json"
            ),
            Arguments.of(
                "По партнёру откуда",
                Map.of("fromPartner", "1"),
                "data/controller/admin/logisticEdges/response/filter_by_from_partner_id.json"
            ),
            Arguments.of(
                "По типу сегмента откуда",
                Map.of("fromType", "LINEHAUL"),
                "data/controller/admin/logisticEdges/response/filter_by_from_type.json"
            )
        );
    }

    @Test
    @DisplayName("Ограничение доступа для создания и удаления связи между сегментами")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_EDGES})
    void createLogisticEdgeRestriction() throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-edges/new")).andExpect(status().isForbidden());

        performCreate(Map.of(
            "fromSegmentId", "10001",
            "toSegmentId", "10003"
        ))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/admin/lms/logistic-edges/10")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Попытка создания связи между связанными сегментами")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_EDGES_EDIT})
    void createExistingLogisticEdgeRestriction() throws Exception {
        performCreate(Map.of(
            "fromSegmentId", "10001",
            "toSegmentId", "10002"
        ))
            .andExpect(status().isConflict());

        performCreate(Map.of(
            "fromSegmentId", "10001",
            "toSegmentId", "10001"
        ))
            .andExpect(status().isBadRequest());
    }

    @DisplayName("Создание связи между сегментами с отсутствующими необходимыми полями")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("createLackingArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_EDGES_EDIT})
    void createLogisticEdgeWithWrongBody(
        @SuppressWarnings("unused") String displayName,
        Map<String, String> body
    ) throws Exception {
        performCreate(body).andExpect(status().isBadRequest());
    }

    @Nonnull
    private static Stream<Arguments> createLackingArguments() {
        return Stream.of(
            Arguments.of("С пустым телом запроса", Map.of()),
            Arguments.of("С некоторыми необходимыми полями", Map.of("fromSegmentId", "1"))
        );
    }

    @Test
    @DisplayName("Создание связи между сегментами")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_EDGES_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticEdges/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticEdge() throws Exception {
        performCreate(Map.of(
            "fromSegmentId", "10001",
            "toSegmentId", "10003"
        ))
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/logistic-edges/1"));
    }

    @Nonnull
    private ResultActions performCreate(Map<String, String> body) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/logistic-edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject(body).toString())
        );
    }
}
