package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminLomWaybillSegmentsControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/lom_waybill_segments_search/before/lom_waybill_segments_search.xml")
    fun segmentsSearchByIds() {
        val requestBuilder = get("/admin/lom-waybill-segments/search")
            .param("lomWaybillSegmentId", "101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/lom_waybill_segments_search/response/lom_waybill_segments_search_ids.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/lom_waybill_segments_get_segment/before/lom_waybill_segments_get_segment.xml")
    fun segmentGetById() {
        val requestBuilder = get("/admin/lom-waybill-segments/101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/lom_waybill_segments_get_segment/response/lom_waybill_segments_get_segment.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/lom_waybill_segments_get_segment/before/lom_waybill_segments_get_segment.xml")
    fun segmentGetByIdButNotFound() {
        val requestBuilder = get("/admin/lom-waybill-segments/10")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
