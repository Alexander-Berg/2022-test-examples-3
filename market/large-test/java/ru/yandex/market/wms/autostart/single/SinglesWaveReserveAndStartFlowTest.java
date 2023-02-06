package ru.yandex.market.wms.autostart.single;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetups({
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/singles/configs.xml",
                connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/singles/locations.xml",
                connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/singles/sku.xml",
                connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/singles/waves.xml",
                connection = "wmwhseConnection")}
)
public class SinglesWaveReserveAndStartFlowTest extends TestcontainersConfiguration {

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/singles/single-overflow-enabled/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/singles/single-overflow-enabled/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldMarkDespiteConsolidationIsFullIfOverflowEnabled() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/singles/single-overflow-disabled/before.xml",
            type = DatabaseOperation.REFRESH)
    public void shouldFailIfConsolidationIsFullAndNonForceAndOverflowDisabled() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));

        result.andExpect(status().is4xxClientError());
    }


    @Test
    @DatabaseSetup(value =
            "/testcontainers/controller/waves/db/singles/start-with-force/before.xml",
            type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/singles/start-with-force/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldMarkAllDetailsIfStartWithForceWithoutConsLine() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/start-waves-without-sorter-station.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value =
            "/testcontainers/controller/waves/db/singles/start-with-force-consline/before.xml",
            type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/singles/start-with-force-consline/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldMarkAllDetailsIfStartWithForceWithConsLine() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-consline.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/singles/fail-if-force-with-sort-station/before.xml",
            type = DatabaseOperation.REFRESH
    )
    public void shouldFailIfStartWithForceAndNonexistentConsLine() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/start-waves-nonexistent-consline.json")));

        result.andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/singles/single-overflow-disabled/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/singles/single-overflow-disabled/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldMarkIfOverflowDisabledAndStartWithForce() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions startResult = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/start-waves-without-sorter-station.json")));

        startResult.andExpect(status().isOk());
    }
}
