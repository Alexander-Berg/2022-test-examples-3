package ru.yandex.market.delivery.transport_manager.controller.routing;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
public class RoutingControllerTest extends AbstractContextualTest {
    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Успешное получение данных для маршрутизации")
    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request.xml"
    )
    void createRouteSuccess() throws Exception {
        mockMvc.perform(
                get("/routing/request/2234562/" + LocalDate.now(clock))
            )
            .andDo(setResponseCharesetEncoding("UTF-8"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routing/request_get/response.json", "created", "updated"));
    }

    @Test
    @DisplayName("Cоздание маршрута на прошедшую дату")
    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request.xml"
    )
    void createRoutePrevDate() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                get("/routing/request/2234562/" + LocalDate.now(clock).minusDays(1))
            )
            .andDo(setResponseCharesetEncoding("UTF-8"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent(
                "controller/routing/request_get/date_in_the_past_response.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Поставки не обогащены")
    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request_not_enriched.xml"
    )
    void createRouteNotEnriched() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                get("/routing/request/2234562/" + LocalDate.now(clock))
            )
            .andDo(setResponseCharesetEncoding("UTF-8"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/routing/request_get/not_enriched_response.json", "created", "updated"));
    }

    @Test
    @DisplayName("Поставки частично не обогащены")
    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request_not_enriched_partial.xml"
    )
    void createRoutePartialNotEnriched() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                get("/routing/request/2234562/" + LocalDate.now(clock))
            )
            .andDo(setResponseCharesetEncoding("UTF-8"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent(
                "controller/routing/request_get/not_enriched_partial_response.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Успешное получение результатов маршрутизации")
    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request.xml"
    )
    @ExpectedDatabase(
        value = "/repository/routing/after/put_routing_result_dbqueue_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void putRoutingResponse() throws Exception {
        mockMvc.perform(put("/routing/result/2234562/" + LocalDate.now(clock))
                .content(extractFileContent("controller/routing/response_put/request.json"))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());
    }
}
