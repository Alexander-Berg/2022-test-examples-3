package ru.yandex.market.wms.ordermanagement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

@SpringBootTest(classes = [IntegrationTestConfig::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
internal class AnomalyWithdrawalControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/anomaly-withdrawal-controller/withdrawals-by-status/bd.xml")
    @ExpectedDatabase(
        value = "/controller/anomaly-withdrawal-controller/withdrawals-by-status/bd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getAnomalyWithdrawalsByStatus() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/anomaly-withdrawal")
                .param("status", "CREATED_EXTERNALLY")
                .param("storerKey", "STOR")
                .param("sort", "shipDate")
                .param("order", "DESC")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/anomaly-withdrawal-controller/withdrawals-by-status/result.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/anomaly-withdrawal-controller/withdrawals-by-status/bd.xml")
    @ExpectedDatabase(
        value = "/controller/anomaly-withdrawal-controller/withdrawals-by-status/bd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getAnomalyWithdrawalsWithoutStatus() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/anomaly-withdrawal")
                .param("orderKey", "3")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/anomaly-withdrawal-controller/withdrawals/result.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/anomaly-withdrawal-controller/withdrawals-by-status/bd.xml")
    @ExpectedDatabase(
        value = "/controller/anomaly-withdrawal-controller/withdrawals-by-status/bd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getAnomalyWithdrawalsEmptyStatus() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/anomaly-withdrawal")
                .param("status", "PRE_ALLOCATED")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/anomaly-withdrawal-controller/empty-status/result.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/anomaly-withdrawal-controller/anomalies/bd.xml")
    @ExpectedDatabase(
        value = "/controller/anomaly-withdrawal-controller/anomalies/bd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getAnomalyByOrderKey() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/anomaly-withdrawal/anomalies")
                .param("orderKey", "1")
                .param("anomalyBox", "PLT0")
                .param("sort", "anomalyBox")
                .param("order", "DESC")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/anomaly-withdrawal-controller/anomalies/result.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/anomaly-withdrawal-controller/anomalies/bd.xml")
    @ExpectedDatabase(
        value = "/controller/anomaly-withdrawal-controller/anomalies/bd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getAnomalyByNotExistingOrderKey() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/anomaly-withdrawal/anomalies")
                .param("orderKey", "4")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/anomaly-withdrawal-controller/not-existing-order-key/result.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/anomaly-withdrawal-controller/anomalies/bd.xml")
    @ExpectedDatabase(
        value = "/controller/anomaly-withdrawal-controller/short-anomaly-box/expect-bd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shortAnomalyBox() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/anomaly-withdrawal/short-box")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FileContentUtils.getFileContent(
                        "controller/anomaly-withdrawal-controller/short-anomaly-box/request.json"
                    )
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/anomaly-withdrawal-controller/anomalies/bd.xml")
    @ExpectedDatabase(
        value = "/controller/anomaly-withdrawal-controller/short-anomaly-items/expect-bd.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shortAnomalyItems() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/anomaly-withdrawal/short-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FileContentUtils.getFileContent(
                        "controller/anomaly-withdrawal-controller/short-anomaly-items/request.json"
                    )
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}
