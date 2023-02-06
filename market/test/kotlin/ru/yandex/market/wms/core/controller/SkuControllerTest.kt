package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte
import ru.yandex.market.logistic.api.model.fulfillment.UnitId
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.common.spring.servicebus.model.dto.PushReferenceItemsResultDto
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushReferenceItemsRequest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.core.async.pushDimension.PushReferenceItemsProducer
import ru.yandex.market.wms.core.async.pushDimension.UpdateDimensionsProducer
import ru.yandex.market.wms.core.base.request.DimensionItem
import ru.yandex.market.wms.core.base.request.SkuDimensionsItem
import ru.yandex.market.wms.core.base.request.UpdateDimensionsRequest
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class SkuControllerTest : IntegrationTest() {

    @Autowired
    @MockBean
    private lateinit var updateDimensionsProducer: UpdateDimensionsProducer

    @Autowired
    @MockBean
    private lateinit var pushReferenceItemsProducer: PushReferenceItemsProducer

    @Test
    @DatabaseSetup("/controller/sku/db/immutable.xml")
    @ExpectedDatabase("/controller/sku/db/immutable.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getDimensionsHappyPath() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/info-for-msrmnt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/request/getDimensionsHappyPath.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/sku/response/getDimensionsHappyPath.json"
                    ),
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/sku/db/immutable.xml")
    @ExpectedDatabase("/controller/sku/db/immutable.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getDimensionsForNonExistingSku() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/info-for-msrmnt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/request/getDimensionsNonExistingSku.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/sku/response/getDimensionsNonExistingSku.json"
                    ),
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/sku/db/immutable.xml")
    @ExpectedDatabase("/controller/sku/db/immutable.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getDimensionsEmptyRequestData() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/info-for-msrmnt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/request/getDimensionsEmptyData.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/sku/response/getDimensionsEmptyData.json"
                    ),
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/sku/db/immutable.xml")
    @ExpectedDatabase("/controller/sku/db/immutable.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getDimensionsNotValidRequestDataReturnsError() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/info-for-msrmnt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/request/getDimensionsNotValidRequest.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/sku/db/immutable-with-description.xml")
    @ExpectedDatabase("/controller/sku/db/immutable-with-description.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getDescriptionHappyPath() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/info-for-msrmnt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/request/getDimensionsHappyPath.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/sku/response/getDimensionsWithDescriptionsHappyPath.json"
                    ),
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/sku/db/need_measurement_before.xml")
    @ExpectedDatabase(
        "/controller/sku/db/need_measurement_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `should set need measure to true`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/need-measure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "storerKey": "603674",
                    "sku": "ROV0000000012"
                }
            """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/sku/db/already_measurement_before.xml")
    @ExpectedDatabase(
        "/controller/sku/db/already_measurement_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `no error for already needed measurement sku`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/need-measure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "storerKey": "603674",
                    "sku": "ROV0000000034"
                }
            """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/sku/db/need_measurement_before.xml")
    @ExpectedDatabase(
        "/controller/sku/db/need_measurement_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `should not set need measure to unknown sku`() {
        val response = mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/need-measure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "storerKey": "603674",
                    "sku": "unknown_sku"
                }
            """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
            .response
            .getContentAsString(StandardCharsets.UTF_8)

        Assertions.assertThat(response).contains("Ошибка отправки на обмер sku: 'UNKNOWN_SKU', storerKey: '603674'")
    }

    @Test
    @DatabaseSetup("/controller/sku/db/need_measurement_before.xml")
    @ExpectedDatabase(
        "/controller/sku/db/need_measurement_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `needMeasurement should set need measure to true`() {
        val andExpect = mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/needMeasurement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "skuId": {
                            "storerKey": 603674,
                            "sku": "ROV0000000012"
                        },
                        "measurementReason": "RECEIVING"
                    }
                """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/sku/db/already_measurement_before.xml")
    @ExpectedDatabase(
        "/controller/sku/db/already_measurement_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `needMeasurement returns no error for already needed measurement sku`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/needMeasurement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "skuId": {
                            "storerKey": "603674",
                            "sku": "ROV0000000034"
                        },
                        "measurementReason": "RECEIVING"
                    }
                """.trimIndent()
                    )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/sku/db/need_measurement_before.xml")
    @ExpectedDatabase(
        "/controller/sku/db/need_measurement_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `needMeasurement should not set need measure to unknown sku`() {
        val response = mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/needMeasurement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "skuId": {
                            "storerKey": "603674",
                            "sku": "unknown_sku"
                        },
                        "measurementReason": "CONSTRAINTS"
                    }
            """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
            .response
            .getContentAsString(StandardCharsets.UTF_8)

        Assertions.assertThat(response).contains("Ошибка отправки на обмер sku: 'UNKNOWN_SKU', storerKey: '603674'")
    }

    @Test
    fun `needMeasurement returns bad request when sku id is not passed`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/needMeasurement")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .getContentAsString(StandardCharsets.UTF_8)
    }

    @Test
    fun `needMeasurement returns bad request when sku id is null`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/needMeasurement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "skuId": null
                    }
                """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .getContentAsString(StandardCharsets.UTF_8)
    }

    @Test
    fun `needMeasurement returns bad request when storerKey is not passed`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/needMeasurement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "skuId": {
                            "sku": "ROV0000000034"
                        }
                    }
                """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .getContentAsString(StandardCharsets.UTF_8)
    }

    @Test
    @DatabaseSetup("/controller/sku/characteristics/immutable.xml")
    @ExpectedDatabase(
        "/controller/sku/characteristics/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get sku's characteristics`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/sku/characteristics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/characteristics/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/sku/characteristics/response.json"
                    ),
                    false
                )
            )
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DatabaseSetup("/controller/sku/dimensions/immutable.xml")
    @ExpectedDatabase(
        "/controller/sku/dimensions/happy-pass/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateSkuDimensionsTrustedSourceHappyPass() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/sku/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/dimensions/happy-pass/trusted/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        verify(updateDimensionsProducer).produceNotification(
            UpdateDimensionsRequest(
                listOf(
                    SkuDimensionsItem(
                        SkuId("20000", "ROV0000000018"),
                        "200081",
                        DimensionItem(
                            BigDecimal.valueOf(4.8),
                            BigDecimal.valueOf(9.71),
                            BigDecimal.valueOf(4.45),
                            BigDecimal.valueOf(6.74)
                        )
                    )
                ),
                listOf(
                    SkuDimensionsItem(
                        SkuId("10000", "ROV0000000012"),
                        "BSH2002MW01",
                        DimensionItem(
                            BigDecimal.valueOf(10.12).setScale(5),
                            BigDecimal.valueOf(6.54),
                            BigDecimal.valueOf(6.35),
                            BigDecimal.valueOf(5.0)
                        )
                    )
                ),
                listOf(
                    SkuDimensionsItem(
                        SkuId("10000", "ROV0000000012BOM1"),
                        "9020015500",
                        DimensionItem(
                            BigDecimal.valueOf(2.5),
                            BigDecimal.valueOf(6.35),
                            BigDecimal.valueOf(5),
                            BigDecimal.valueOf(6.54)
                        )
                    )
                )
            )
        )
    }

    @Test
    @DatabaseSetup("/controller/sku/dimensions/immutable.xml")
    @ExpectedDatabase(
        "/controller/sku/dimensions/happy-pass/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateSkuDimensionsNonTrustedSourceHappyPass() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/sku/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/dimensions/happy-pass/non-trusted/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        verify(pushReferenceItemsProducer).produceNotification(
            PushReferenceItemsRequest.builder().items(
                listOf(
                    PushReferenceItemsResultDto.builder()
                        .unitId(
                            UnitId("200081", 20000L, "200081")
                        )
                        .korobyte(
                            Korobyte.KorobyteBuiler(7, 5, 10, BigDecimal.valueOf(4.8)).build()
                        )
                        .build(),
                    PushReferenceItemsResultDto.builder()
                        .unitId(
                            UnitId("BSH2002MW01", 10000L, "BSH2002MW01")
                        )
                        .korobyte(
                            Korobyte.KorobyteBuiler(
                                5, 7, 7,
                                BigDecimal.valueOf(10.12).setScale(5)
                            ).build()
                        )
                        .build()
                )
            ).build()
        )
    }

    @Test
    @DatabaseSetup("/controller/sku/dimensions/parent-sku/before.xml")
    @ExpectedDatabase(
        "/controller/sku/dimensions/parent-sku/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateSkuDimensionsParentSku() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/sku/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/dimensions/parent-sku/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers
                    .content()
                    .string(
                        FileContentUtils.getFileContent("controller/sku/dimensions/parent-sku/response.txt")
                    )
            )
    }

    @Test
    @DatabaseSetup("/controller/sku/dimensions/immutable.xml")
    @ExpectedDatabase(
        "/controller/sku/dimensions/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateSkuDimensionsNonExistingSku() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/sku/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/dimensions/non-existing-sku/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/sku/dimensions/immutable.xml")
    @ExpectedDatabase(
        "/controller/sku/dimensions/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateSkuDimensionsBadRequest() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/sku/dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/sku/dimensions/bad-request/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/sku/db/immutable-with-measurement-info.xml")
    @ExpectedDatabase(
        "/controller/sku/db/immutable-with-measurement-info.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getSkuMeasurementInfoHappyPath() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/sku/info")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(
                MockMvcResultMatchers
                    .content()
                    .json(
                        FileContentUtils.getFileContent("controller/sku/response/getSkuMeasurementInfoHappyPath.json"),
                        false
                    )
            )
    }

    @Test
    @DatabaseSetup("/controller/sku/db/immutable-with-measurement-info.xml")
    @ExpectedDatabase(
        "/controller/sku/db/immutable-with-measurement-info.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getSkuMeasurementInfoSecondPage() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/sku/info")
                .param("limit", "20")
                .param("offset", "3")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(
                MockMvcResultMatchers
                    .content()
                    .json(
                        FileContentUtils.getFileContent("controller/sku/response/getSkuMeasurementInfoSecondPage.json"),
                        false
                    )
            )
    }

    @Test
    @DatabaseSetup("/controller/sku/db/immutable-with-measurement-info.xml")
    @ExpectedDatabase(
        "/controller/sku/db/immutable-with-measurement-info.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getSkuMeasurementInfoWithFilter() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/sku/info")
                .param("filter", "lastMeasurementDate=='2022-03-30 21:22:15'")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(
                MockMvcResultMatchers
                    .content()
                    .json(
                        FileContentUtils.getFileContent("controller/sku/response/getSkuMeasurementInfoWithFilter.json"),
                        false
                    )
            )
    }

    @Test
    @DatabaseSetup("/controller/sku/db/immutable-with-measurement-info.xml")
    @ExpectedDatabase(
        "/controller/sku/db/immutable-with-measurement-info.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getSkuMeasurementInfoWithSort() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/sku/info")
                .param("sort", "description")
                .param("order", "DESC")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(
                MockMvcResultMatchers
                    .content()
                    .json(
                        FileContentUtils.getFileContent("controller/sku/response/getSkuMeasurementInfoWithSort.json"),
                        true
                    )
            )
    }
}
