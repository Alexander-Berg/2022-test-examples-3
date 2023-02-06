package ru.yandex.market.logistics.mqm.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.utils.queue.extractFileContent
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent
import javax.persistence.EntityManager

class PlanFactControllerTest : AbstractContextualTest() {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @DatabaseSetup("/controller/planfact/before/get_plan_fact.xml")
    fun findPlanFact() {
        mockMvc.perform(get("/plan-fact/1"))
            .andExpect(status().isOk)
            .andExpect(jsonContent("controller/planfact/response/find_plan_fact.json"))
    }

    @Test
    fun findPlanFactNotFound() {
        mockMvc.perform(get("/plan-fact/1"))
            .andExpect(status().isNotFound)
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/planfact/after/create_plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createPlanFact() {
        resetSequenceIdGeneratorCache(PlanFact::class.java, entityManager)
        mockMvc.perform(
            post("/plan-fact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/create_plan_fact.json"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonContent("controller/planfact/response/create_plan_fact.json"))
    }

    @Test
    @DatabaseSetup("/controller/planfact/before/create_waybill_segment_plan_fact.xml")
    @ExpectedDatabase(
        value = "/controller/planfact/after/create_waybill_segment_plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createWaybillSegmentPlanFact() {
        resetSequenceIdGeneratorCache(PlanFact::class.java, entityManager)
        mockMvc.perform(
            post("/plan-fact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/create_waybill_segment_plan_fact.json"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonContent("controller/planfact/response/create_waybill_segment_plan_fact.json"))
    }

    @Test
    fun createWaybillSegmentPlanFactWithoutSegment() {
        mockMvc.perform(
            post("/plan-fact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/create_waybill_segment_plan_fact.json"))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun createPlanFactValidationError() {
        mockMvc.perform(
            post("/plan-fact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/create_plan_fact_validation_error.json"))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/planfact/before/save_waybill_segment_fact_time.xml")
    @ExpectedDatabase(
        value = "/controller/planfact/after/save_waybill_segment_fact_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveWaybillSegmentFactTime() {
        mockMvc.perform(
            put("/plan-fact/save-fact-time")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/save_waybill_segment_fact_time.json"))
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planfact/before/search_plan_facts.xml")
    fun searchPlanFacts() {
        mockMvc.perform(
            put("/plan-fact/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/search_plan_facts.json"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonContent("controller/planfact/response/search_plan_facts.json"))
    }

    @Test
    fun searchPlanFactsFindZero() {
        mockMvc.perform(
            put("/plan-fact/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/search_plan_facts.json"))
        )
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))
    }

    @Test
    @DatabaseSetup("/controller/planfact/before/plan_fact_track_received.xml")
    @ExpectedDatabase(
        value = "/controller/planfact/after/plan_fact_track_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun putTrackReceivedError() {
        mockMvc.perform(
            put("/plan-fact/processing-error")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/plan_fact_track_received_error.json"))
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planfact/before/plan_fact_track_received.xml")
    @ExpectedDatabase(
        value = "/controller/planfact/after/plan_fact_track_received_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun putTrackReceivedErrorEmpty() {
        mockMvc.perform(
            put("/plan-fact/processing-error")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/plan_fact_track_received_error_empty.json"))
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planfact/before/plan_fact_track_received.xml")
    @ExpectedDatabase(
        value = "/controller/planfact/before/plan_fact_track_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun putTrackReceivedErrorValidation() {
        mockMvc.perform(
            put("/plan-fact/processing-error")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfact/request/plan_fact_track_received_error_invalid.json"))
        )
            .andExpect(status().isBadRequest)
    }
}
