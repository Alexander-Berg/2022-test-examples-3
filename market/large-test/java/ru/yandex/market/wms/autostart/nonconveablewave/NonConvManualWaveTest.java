package ru.yandex.market.wms.autostart.nonconveablewave;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetups({
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonconv/configs.xml",
                connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonconv/locations.xml",
                connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonconv/sku.xml",
                connection = "wmwhseConnection")}
)
public class NonConvManualWaveTest extends TestcontainersConfiguration {

    @Test
    @DatabaseSetup(value =
            "/testcontainers/controller/waves/db/nonconv/happy/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonconv/happy/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldCreateNonConvWave() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-wave-one-order.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value =
            "/testcontainers/controller/waves/db/nonconv/flag-disabled/before.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(
            value = "/testcontainers/controller/waves/db/nonconv/flag-disabled/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotCreateNonConvWaveIfFlagDisabled() throws Exception {
        ResultActions result = mockMvc.perform(post("/waves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/wavesControllerLargeTest/request/create-wave-one-order.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonconv/startwave/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/nonconv/startwave/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldStartNonConvWave() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonconv/oversize/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/nonconv/oversize/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldStartNonConvWaveAndMarkOversize() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves-non-force.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonconv/start-with-force/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/nonconv/start-with-force/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSendToSelectedSortStationWithForce() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/controller/waves/db/nonconv/oversize-with-force/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/waves/db/nonconv/oversize-with-force/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSendToSelectedSortStationAndMarkOversizeWithForce() throws Exception {
        ResultActions reservationResult = mockMvc.perform(post("/waves/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "testcontainers/controller/waves/request/reserve-waves.json")));
        ResultActions result = mockMvc.perform(post("/waves/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/waves/request/start-waves.json")));
        result.andExpect(status().isOk());
    }

}
