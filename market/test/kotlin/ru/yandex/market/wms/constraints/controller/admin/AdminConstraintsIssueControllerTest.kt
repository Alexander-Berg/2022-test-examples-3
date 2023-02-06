package ru.yandex.market.wms.constraints.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.core.base.dto.SkuCharacteristic
import ru.yandex.market.wms.core.base.dto.SkuCharacteristicType
import ru.yandex.market.wms.core.base.dto.SkuSpecification
import ru.yandex.market.wms.core.base.dto.SkuToLocListDto
import ru.yandex.market.wms.core.base.response.GetLocListBySkuListResponse
import ru.yandex.market.wms.core.base.response.SkuCharacteristicsResponse

class AdminConstraintsIssueControllerTest : ConstraintsIntegrationTest() {
    @AfterEach
    fun clear() {
        clearInvocations(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/approve/data.xml")
    @ExpectedDatabase(
        "/controller/admin/constraints-issue/approve/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `approveIssue - ok`() {
        mockMvc.perform(put("/admin/issue/1/approve"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/approve/data.xml")
    @ExpectedDatabase(
        "/controller/admin/constraints-issue/approve/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `approveIssue twice - ok`() {
        repeat(2) {
            mockMvc.perform(put("/admin/issue/1/approve"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk)
        }
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/approve/data.xml")
    @ExpectedDatabase(
        "/controller/admin/constraints-issue/approve/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `approveIssue - issue not found`() {
        mockMvc.perform(put("/admin/issue/9999/approve"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNotFound)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/approve/not-found/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/approve/data.xml")
    @ExpectedDatabase(
        "/controller/admin/constraints-issue/approve/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `approveIssue returns bad request because issue declined`() {
        mockMvc.perform(put("/admin/issue/3/approve"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/approve/already-declined/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/decline/data.xml")
    @ExpectedDatabase(
        "/controller/admin/constraints-issue/decline/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `declineIssue - ok`() {
        mockMvc.perform(put("/admin/issue/1/decline"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/decline/data.xml")
    @ExpectedDatabase(
        "/controller/admin/constraints-issue/decline/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `declineIssue twice - ok`() {
        repeat(2) {
            mockMvc.perform(put("/admin/issue/1/decline"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk)
        }
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/decline/data.xml")
    @ExpectedDatabase(
        "/controller/admin/constraints-issue/decline/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `declineIssue - issue not found`() {
        mockMvc.perform(put("/admin/issue/9999/decline"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNotFound)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/decline/not-found/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/decline/data.xml")
    @ExpectedDatabase(
        "/controller/admin/constraints-issue/decline/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `declineIssue returns bad request because issue approved`() {
        mockMvc.perform(put("/admin/issue/3/decline"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/decline/already-approved/response.json"),
                    true
                )
            )

    }

    @Test
    fun `getIssues returns bad request when filter field is invalid`() {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
                .param("filter", "wrongField==1")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/filter-field-invalid/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when filter field is valid`() {
        setCoreClientMock()
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
                .param("filter", "sku==ROV001")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/filter-valid/response.json"),
                    true
                )
            )

    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "0", "101"])
    fun `getIssues returns bad request when limit value is invalid`(limit: String) {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
                .param("limit", limit)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when limit value is valid`() {
        setCoreClientMock()
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
                .param("limit", "1")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/limit-valid/response.json"),
                    true
                )
            )
    }

    @Test
    fun `getIssues returns bad request when offset value is invalid`() {
        setCoreClientMock()
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
                .param("offset", "-1")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when offset value is valid`() {
        setCoreClientMock()
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
                .param("offset", "1")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/offset-valid/response.json"),
                    true
                )
            )
    }

    @Test
    fun `getIssues return bad request when order value is invalid`() {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
                .param("order", "notValidOrder")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getIssues returns bad request when sort field is invalid`() {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
                .param("sort", "wrongField")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/sort-field-invalid/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when sort field is addWho and order is ASC`() {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "CARGO_TYPE")
                .param("sort", "addWho")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/sort-addWho-order-asc/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when sort field is addWho and order is DESC`() {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "CARGO_TYPE")
                .param("sort", "addWho")
                .param("order", "DESC")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/sort-addWho-order-desc/response.json"),
                    true
                )
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "issueLoc", "sku", "status", "addWho", "addDate"])
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when sort is valid and order is ASC`(field: String) {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "CARGO_TYPE")
                .param("sort", field)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/sort-$field-order-asc/response.json"),
                    true
                )
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "issueLoc", "sku", "status", "addWho", "addDate"])
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when sort is valid and order is DESC`(field: String) {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "CARGO_TYPE")
                .param("sort", field)
                .param("order", "DESC")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/sort-$field-order-desc/response.json"),
                    true
                )
            )
    }

    @Test
    fun `getIssues returns bad request when type is invalid`() {
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "WRONG_TYPE")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when type is DIMENSION`() {
        setCoreClientMock()
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "DIMENSION")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/type-dimension/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/constraints-issue/get-issues/before.xml")
    fun `getIssues returns successful response when type is CARGO_TYPE`() {
        setCoreClientMock()
        mockMvc.perform(
            get("/admin/issue")
                .param("type", "CARGO_TYPE")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/constraints-issue/get-issues/type-cargo-type/response.json"),
                    true
                )
            )
    }

    private fun setCoreClientMock() {
        whenever(coreClient.getSkuCharacteristics(any()))
            .thenReturn(
                SkuCharacteristicsResponse(
                    listOf(
                        SkuSpecification(
                            sku = "ROV001",
                            storerKey = "465852",
                            specification = listOf(
                                SkuCharacteristic(SkuCharacteristicType.LENGTH, "10"),
                                SkuCharacteristic(SkuCharacteristicType.WIDTH, "20"),
                                SkuCharacteristic(SkuCharacteristicType.HEIGHT, "30"),
                                SkuCharacteristic(SkuCharacteristicType.VOLUME, "40"),
                                SkuCharacteristic(SkuCharacteristicType.WEIGHT, "50")
                            )
                        ),
                        SkuSpecification(
                            sku = "ROV002",
                            storerKey = "465852",
                            specification = listOf(
                                SkuCharacteristic(SkuCharacteristicType.LENGTH, "10"),
                                SkuCharacteristic(SkuCharacteristicType.WIDTH, "20"),
                                SkuCharacteristic(SkuCharacteristicType.HEIGHT, "30"),
                                SkuCharacteristic(SkuCharacteristicType.VOLUME, "40"),
                                SkuCharacteristic(SkuCharacteristicType.WEIGHT, "50")
                            )
                        ),
                        SkuSpecification(
                            sku = "ROV006",
                            storerKey = "465852",
                            description = "sku description",
                            specification = listOf(
                                SkuCharacteristic(SkuCharacteristicType.LENGTH, "10"),
                                SkuCharacteristic(SkuCharacteristicType.WIDTH, "20"),
                                SkuCharacteristic(SkuCharacteristicType.HEIGHT, "30"),
                                SkuCharacteristic(SkuCharacteristicType.VOLUME, "40"),
                                SkuCharacteristic(SkuCharacteristicType.WEIGHT, "50")
                            )
                        )
                    )
                )
            )

        whenever(coreClient.getLocListBySkuList(any()))
            .thenReturn(
                GetLocListBySkuListResponse(
                    skuToLocList = listOf(
                        SkuToLocListDto(
                            sku = "ROV001",
                            storerKey = "465852",
                            locList = listOf(
                                "A1-01-01A1",
                                "A1-01-01A2",
                            )
                        ),
                        SkuToLocListDto(
                            sku = "ROV002",
                            storerKey = "465852",
                            locList = listOf(
                                "A1-01-01A3",
                                "A1-01-01A4",
                            )
                        )
                    )
                )
            )
    }
}
