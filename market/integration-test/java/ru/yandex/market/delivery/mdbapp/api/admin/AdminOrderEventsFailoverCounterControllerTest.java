package ru.yandex.market.delivery.mdbapp.api.admin;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.controller.admin.dto.ActionDto;
import ru.yandex.market.delivery.mdbapp.controller.admin.dto.AdminOrderEventsFailoverCounterFilter;
import ru.yandex.market.delivery.mdbapp.controller.admin.dto.EventDto;
import ru.yandex.market.delivery.mdbapp.controller.admin.enums.AdminEventAction;
import ru.yandex.market.delivery.mdbapp.controller.admin.enums.AdminFailCauseType;
import ru.yandex.market.logistics.test.integration.utils.QueryParamUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Операции с упавшими событиями через админку")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class AdminOrderEventsFailoverCounterControllerTest extends MockContextualTest {
    private static final long EVENT_1_ID = 101L;
    private static final long EVENT_2_ID = 102L;
    private static final long EVENT_3_ID = 103L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("commonJsonMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("Получение идентификаторов заказов фильтру")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("search")
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/failover-counters-operations-insert.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    public void orderIdsByFilter(
        @SuppressWarnings("unused") String name,
        AdminOrderEventsFailoverCounterFilter dto,
        @SuppressWarnings("unused") String responsePath,
        @SuppressWarnings("unused") List<Long> eventIds,
        String orderIdsFilePath
    ) throws Exception {
        softly.assertThat(byAttemptsCount()).isEmpty();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/failover-counters/order-ids")
                    .params(QueryParamUtils.toParams(dto))
            )
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.content().string(extractFileContent(orderIdsFilePath)));
    }

    @DisplayName("Перевыставление упавших событий по фильтру")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("search")
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/failover-counters-operations-insert.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    public void retryByFilter(
        @SuppressWarnings("unused") String name,
        AdminOrderEventsFailoverCounterFilter dto,
        @SuppressWarnings("unused") String responsePath,
        List<Long> eventIds,
        @SuppressWarnings("unused") String orderIdsFilePath
    ) throws Exception {
        softly.assertThat(byAttemptsCount()).isEmpty();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/failover-counters/retry-all-list")
                    .accept(MediaType.APPLICATION_JSON)
                    .params(QueryParamUtils.toParams(dto))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());

        softly.assertThat(byAttemptsCount()).containsExactlyInAnyOrderElementsOf(eventIds);
    }

    @DisplayName("Поиск упавших событий")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/failover-counters-operations-insert.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    public void search(
        @SuppressWarnings("unused") String name,
        AdminOrderEventsFailoverCounterFilter dto,
        String responsePath,
        @SuppressWarnings("unused") List<Long> eventIds,
        @SuppressWarnings("unused") String orderIdsFilePath
    ) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/failover-counters")
                    .accept(MediaType.APPLICATION_JSON)
                    .params(QueryParamUtils.toParams(dto))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockMvcResultMatchers.content().json(extractFileContent(responsePath)))
            .andExpect(status().isOk());
    }

    @Nonnull
    private static Stream<Arguments> search() {
        return Stream.of(
            Arguments.of(
                "Нет событий",
                new AdminOrderEventsFailoverCounterFilter().setEventId(0L),
                "data/admin/failover/empty.json",
                List.of(),
                "data/admin/failover/empty.csv"
            ),
            Arguments.of(
                "Без фильтра",
                new AdminOrderEventsFailoverCounterFilter(),
                "data/admin/failover/all.json",
                List.of(101L, 104L, 105L),
                "data/admin/failover/101_104.csv"
            ),
            Arguments.of(
                "По идентификатору ивента",
                new AdminOrderEventsFailoverCounterFilter().setEventId(104L),
                "data/admin/failover/104.json",
                List.of(104L),
                "data/admin/failover/104.csv"
            ),
            Arguments.of(
                "По идентификатору заказа",
                new AdminOrderEventsFailoverCounterFilter().setOrderId(201L),
                "data/admin/failover/101_105_106.json",
                List.of(101L, 105L),
                "data/admin/failover/101.csv"
            ),
            Arguments.of(
                "По типу события",
                new AdminOrderEventsFailoverCounterFilter()
                    .setFailureOrderEventAction(AdminEventAction.FF_ORDER_CREATE),
                "data/admin/failover/104.json",
                List.of(104L),
                "data/admin/failover/104.csv"
            ),
            Arguments.of(
                "По ошибке",
                new AdminOrderEventsFailoverCounterFilter().setLastFailCause("причина падения"),
                "data/admin/failover/101_104.json",
                List.of(101L),
                "data/admin/failover/101_104.csv"
            ),
            Arguments.of(
                "По пустой строке ошибки",
                new AdminOrderEventsFailoverCounterFilter().setLastFailCause(""),
                "data/admin/failover/all.json",
                List.of(101L, 104L, 105L),
                "data/admin/failover/101_104.csv"
            ),
            Arguments.of(
                "По типу последней ошибки",
                new AdminOrderEventsFailoverCounterFilter().setLastFailCauseType(AdminFailCauseType.FROZEN_SERVICE),
                "data/admin/failover/104.json",
                List.of(104L),
                "data/admin/failover/104.csv"
            ),
            Arguments.of(
                "По всем полям",
                new AdminOrderEventsFailoverCounterFilter()
                    .setEventId(101L)
                    .setOrderId(201L)
                    .setFailureOrderEventAction(AdminEventAction.SC_ORDER_CREATE)
                    .setLastFailCause("причина падения")
                    .setLastFailCauseType(AdminFailCauseType.INTERNAL_SERVER_ERROR),
                "data/admin/failover/101.json",
                List.of(101L),
                "data/admin/failover/101.csv"
            )
        );
    }

    @Test
    @DisplayName("Перевыставление нескольких ивентов")
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(
        value = "/data/failover-counters-operations-insert.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    public void retry() throws Exception {
        softly.assertThat(byAttemptsCount())
            .isEmpty();

        ActionDto actionDto = ActionDto.fromIds(Set.of(EVENT_1_ID, EVENT_2_ID, EVENT_3_ID, 105L));
        mockMvc.perform(
                post("/failover-counters/retry-list")
                    .accept(MediaType.APPLICATION_JSON)
                    .content(toJson(actionDto))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());

        softly.assertThat(byAttemptsCount()).containsOnly(EVENT_1_ID, 105L);
    }

    @Test
    @DisplayName("Перевыставление одного ивента")
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(
        value = "/data/failover-counters-operations-insert.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    public void retryEvent() throws Exception {
        softly.assertThat(byAttemptsCount()).isEmpty();

        mockMvc.perform(
                post("/failover-counters/retry")
                    .accept(MediaType.APPLICATION_JSON)
                    .content(toJson(new EventDto(EVENT_1_ID)))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());
        softly.assertThat(byAttemptsCount()).containsOnly(101L);
    }

    @Test
    @DisplayName("Попытка перевыставить ивент, для которого еще не исчерпались попытки")
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(
        value = "/data/failover-counters-operations-insert.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(value = "/data/failover-counters-operations-clean.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    public void retryEventWithAttempts() throws Exception {
        softly.assertThat(byAttemptsCount()).isEmpty();

        mockMvc.perform(
                post("/failover-counters/retry")
                    .accept(MediaType.APPLICATION_JSON)
                    .content(toJson(new EventDto(EVENT_2_ID)))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());
        softly.assertThat(byAttemptsCount()).isEmpty();
    }

    @Nonnull
    private String toJson(Object o) throws JsonProcessingException {
        return objectMapper.writeValueAsString(o);
    }

    @Nonnull
    private List<Long> byAttemptsCount() {
        return jdbcTemplate.queryForList(
            "select event_id from order_events_failover_counter where attempt_count = 5",
            Long.class
        );
    }
}
