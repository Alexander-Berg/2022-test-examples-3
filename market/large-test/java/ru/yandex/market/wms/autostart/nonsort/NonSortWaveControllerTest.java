package ru.yandex.market.wms.autostart.nonsort;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetups({
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/configs.xml",
                connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/locations.xml",
                connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/sku.xml",
                connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/waves.xml",
                connection = "wmwhseConnection")}
)
public class NonSortWaveControllerTest extends TestcontainersConfiguration {

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/happypass/before.xml", type =
            DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonsort/happypass/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSetNonSortStationToPickDetail() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/different-assignment/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonsort/different-assignment/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSplitOrderToSeparateAssignment() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
            value = "/testcontainers/controller/waves/db/nonsort/full-consolidation-disabled/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonsort/full-consolidation-disabled/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSendNonSortToSortstationIfConsolidationIsFull() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/full-consolidation-enabled/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonsort/full-consolidation-enabled/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSendNonSortToConsolidationDespiteConsolidationIsFull() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));

        result.andExpect(status().isOk());
    }

    //TODO ближайший рефакторинг MARKETWMS-9581
    @Disabled("Надо переписать с учетом нового перерезерва")
    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/wave-reservation/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonsort/wave-reservation/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldReserveWaveWithNonSort() throws Exception {
        ResultActions result = mockMvc.perform(put("/waves/reserve/WAVE-001"));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/happypass/before.xml", type =
            DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonsort/happypass/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSendToOversizeStationWithForce() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value =
            "/testcontainers/controller/waves/db/nonsort/single-without-free-sortstation/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonsort/single-without-free-sortstation/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotFailWhenNoSortationForSingles() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonsort/mono-packing/before.xml", type =
            DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonsort/mono-packing/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotMarkNonSortToMonoPackingDeliveryServices() throws Exception {
        mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/reserve-waves.json")));

        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));

        result.andExpect(status().isOk());
    }

}
