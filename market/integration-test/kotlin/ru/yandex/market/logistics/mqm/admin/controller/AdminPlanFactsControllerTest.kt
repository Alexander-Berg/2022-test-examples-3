package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import java.net.URL
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.admin.MqmPlugin
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminPlanFactsControllerTest: AbstractContextualTest() {

    @Autowired
    private lateinit var mdsS3Client: MdsS3Client

    @Autowired
    private lateinit var resourceLocationFactory: ResourceLocationFactory

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_search/before/plan_facts_search.xml")
    fun planFactsSearchByIds() {
        val requestBuilder = get("/admin/plan-facts/search")
            .param("planFactId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_search/response/plan_facts_search_ids.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_search/before/plan_facts_search.xml")
    fun planFactsSearchByTypes() {
        val requestBuilder = get("/admin/plan-facts/search")
            .param("entityTypes", "LOM_ORDER", "LOM_WAYBILL_SEGMENT")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_search/response/plan_facts_search_types.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_search/before/plan_facts_search.xml")
    fun planFactsSearchByPages() {
        val requestBuilder = get("/admin/plan-facts/search")
            .param("page", "0")
            .param("size", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_search/response/plan_facts_search_pages.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_get_plan_fact/before/plan_facts_get_plan_fact.xml")
    fun planFactsGetById() {
        val requestBuilder = get("/admin/plan-facts/1")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_get_plan_fact/response/plan_facts_get_plan_fact.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_get_plan_fact/before/plan_facts_get_plan_fact_with_segment.xml")
    fun planFactsGetByIdWithSegment() {
        val requestBuilder = get("/admin/plan-facts/1")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_get_plan_fact/response/plan_facts_get_plan_fact_with_segment.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_get_plan_fact/before/plan_facts_get_plan_fact_without_segment.xml")
    fun planFactsGetByIdWithoutSegment() {
        val requestBuilder = get("/admin/plan-facts/1")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_get_plan_fact/response/plan_facts_get_plan_fact_without_segment.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_get_plan_fact/before/plan_facts_get_plan_fact.xml")
    fun planFactsGetByIdButNotFound() {
        val requestBuilder = get("/admin/plan-facts/10")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound)
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_search/before/plan_facts_lom_orders_search.xml")
    fun planFactsSearchByLomOrders() {
        val url = "/admin/plan-facts/${MqmPlugin.SLUG_PLAN_FACTS_LOM_ORDERS}/search"
        val requestBuilder = get(url)
            .param("orderId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_search/response/plan_facts_lom_orders_search.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_search/before/plan_facts_lom_orders_search.xml")
    fun planFactsSearchByLomOrdersButNotFound() {
        val requestBuilder = get("/admin/plan-facts/lom-orders/search")
            .param("orderId", "10")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound)
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_search/before/plan_facts_plan_facts_groups_search.xml")
    fun planFactsSearchByPlanFactsGroups() {
        val url = "/admin/plan-facts/${MqmPlugin.SLUG_PLAN_FACTS_PLAN_FACTS_GROUPS}/search"
        val requestBuilder = get(url)
            .param("planFactGroupId", "1001")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_search/response/plan_facts_plan_facts_groups_search.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_search/before/plan_facts_plan_facts_groups_search.xml")
    fun planFactsSearchByPlanFactsGroupsButNotFound() {
        val url = "/admin/plan-facts/${MqmPlugin.SLUG_PLAN_FACTS_PLAN_FACTS_GROUPS}/search"
        val requestBuilder = get(url)
            .param("planFactGroupId", "10")
        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound)
    }


    @Test
    @DisplayName("Успешная загрузка файла закрытия ПФ")
    @ExpectedDatabase(
        value = "/admin/controller/plan_facts_close/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closePlanFactsSuccess() {
        whenever(resourceLocationFactory.createLocation(any())).thenReturn(RESOURCE_LOCATION)
        whenever(mdsS3Client.getUrl(any())).thenReturn(TEST_URL)

        uploadFile(createMultipartFile()).andExpect(status().isOk)
    }

    @Test
    @DisplayName("Ошибка валидации загрузки файла закрытия ПФ")
    @ExpectedDatabase(
        value = "/admin/controller/plan_facts_close/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closePlanFactsValidationFail() {
        uploadFile(createMultipartFile("text/plain"))
            .andExpect(status().isBadRequest)
            .andExpect(
                status().reason(
                   "closePlanFacts.multipartFile: File must not be empty, size must be less then 536870912," +
                       " and have one of types [application/vnd.openxmlformats-officedocument.spreadsheetml.sheet]"
                )
            )
    }

    private fun uploadFile(mockMultipartFile: MockMultipartFile): ResultActions {
        val requestBuilder = MockMvcRequestBuilders.multipart("/admin/plan-facts/close")
            .file(mockMultipartFile)
        return mockMvc.perform(requestBuilder)
    }

    companion object {
        private val TEST_FILE_CONTENT = "Some test".toByteArray(StandardCharsets.UTF_8)
        private val RESOURCE_LOCATION = ResourceLocation.create("test-bucket-name", "1")
        private val TEST_URL = URL("http://localhost:8080/test-bucket-name-1.xml")

        private fun createMultipartFile(
            contentType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ): MockMultipartFile {
            return MockMultipartFile(
                "request",
                "file.xlsx",
                contentType,
                TEST_FILE_CONTENT
            )
        }
    }
}
