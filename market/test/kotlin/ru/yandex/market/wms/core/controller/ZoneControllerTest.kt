package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class ZoneControllerTest : IntegrationTest() {
    @Test
    fun `Get destination zone types returns error when there is empty request given`() =
        postWithAllChecks(
            "types",
            status().isBadRequest,
            "request-empty",
            false
        )

    @Test
    fun `Get destination zone types returns error when loc is null`() =
        postWithAllChecks(
            "types",
            status().isBadRequest,
            "loc-null",
            false
        )

    @Test
    fun `Get destination zone types returns error when there is null blank given`() =
        postWithAllChecks(
            "types",
            status().isBadRequest,
            "loc-blank",
            false
        )

    @Test
    fun `Get destination zone types returns error when location not found`() =
        postWithAllChecks(
            "types",
            status().isNotFound,
            "loc-not-found",
            false
        )

    @Test
    @DatabaseSetup("/controller/zone/types/loc-type-plcmnt_buf/before.xml")
    fun `Get destination zone types returns YM_PLCMNT_BUF_DEST_ZONE_TYPES when loc type is PLCMNT_BUF`() =
        postWithAllChecks(
            "types",
            status().isOk,
            "loc-type-plcmnt_buf",
            true
        )

    @Test
    @DatabaseSetup("/controller/zone/types/loc-type-not-plcmnt_buf/before.xml")
    fun `Get destination zone types returns YM_BUF_DEST_ZONE_TYPES when loc type is not PLCMNT_BUF`() =
        postWithAllChecks(
            "types",
            status().isOk,
            "loc-type-not-plcmnt_buf",
            true
        )

    @Test
    @DatabaseSetup("/controller/zone/types/plcmnt_buf_dest_zone_types-not-set/before.xml")
    fun `Get destination zone types returns empty list when YM_PLCMNT_BUF_DEST_ZONE_TYPES not set`() =
        postWithAllChecks(
            "types",
            status().isOk,
            "plcmnt_buf_dest_zone_types-not-set",
            true
        )

    @Test
    @DatabaseSetup("/controller/zone/types/dest_zone_types-not-set/before.xml")
    fun `Get destination zone types returns empty list when YM_DEST_ZONE_TYPES not set`() =
        postWithAllChecks(
            "types",
            status().isOk,
            "dest_zone_types-not-set",
            true
        )

    @Test
    @DatabaseSetup("/controller/zone/types/plcmnt_kgt_dest_zone_types/before.xml")
    fun `Get destination zone types for KGT `() =
        postWithAllChecks(
            "types",
            status().isOk,
            "plcmnt_kgt_dest_zone_types",
            true
        )

    @Test
    @DatabaseSetup("/controller/zone/types/plcmnt_kgt_dest_zone_types-not-set/before.xml")
    fun `Get destination zone types for KGT returns empty list when YM_DIMENSIONS_BUF_DEST_ZONE_TYPES not set`() =
        postWithAllChecks(
            "types",
            status().isOk,
            "plcmnt_kgt_dest_zone_types-not-set",
            true
        )

    private fun postWithAllChecks(
        urlPart: String,
        expectedStatus: ResultMatcher,
        testResourceDir: String,
        checkResponse: Boolean = true
    ) {
        val requestBuilder = MockMvcRequestBuilders.post("/zone/$urlPart")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/zone/$urlPart/$testResourceDir/request.json"))

        val result = mockMvc.perform(requestBuilder)
            .andExpect(expectedStatus)

        if (checkResponse) {
            result.andExpect(MockMvcResultMatchers.content().json(FileContentUtils
                .getFileContent("controller/zone/$urlPart/$testResourceDir/response.json"), true))
        }
    }
}
