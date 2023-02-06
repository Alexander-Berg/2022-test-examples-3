package ru.yandex.market.logistics.calendaring.solomon.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.calendaring.dbqueue.DbqueueTaskType
import ru.yandex.market.logistics.calendaring.solomon.base.BaseSolomonContextualTest
import ru.yandex.market.logistics.calendaring.util.FileContentUtils

class SolomonMonitoringControllerTest(@Autowired jdbcTemplate: JdbcTemplate) : BaseSolomonContextualTest(jdbcTemplate) {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/empty.xml"])
    fun monitoringReturnedCorrectly() {

        insert(DbqueueTaskType.UPDATE_BOOKING_EVENT, "{\"task\":123}", 0)
        insert(DbqueueTaskType.UPDATE_BOOKING_EVENT, "{\"task\":124}", 11)
        insert(DbqueueTaskType.UPDATE_BOOKING_EVENT, "{\"task\":125}", 16)
        insert(DbqueueTaskType.UPDATE_BOOKING_EVENT, "{\"task\":126}", 20)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/health/solomon/db-queue")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/solomon/dbqueue-solomon-monitoring.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/solomon/meta-mapper-errors/before.xml")
    fun metaMapperErrorsReturnedCorrectly() {
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/health/solomon/meta-mapper-errors")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/solomon/meta-mapper-errors/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }
}
