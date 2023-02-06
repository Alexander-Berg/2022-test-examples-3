package ru.yandex.market.logistics.calendaring.monitoring

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import java.time.Clock
import java.time.LocalDateTime

class CustomMonitoringControllerTest(@Autowired private val jdbcTemplate: JdbcTemplate,
                                     @Autowired private val clock: Clock): AbstractContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/empty.xml"])
    @Throws(Exception::class)
    fun logbrokerReadingWhenNotOk() {
        clearPartitionOffset()
        insertPartitionOffset(11, "1")
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/logbroker-reading"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(
                "2;Last successful reading from Logbroker was at 2021-05-11T11:49, more than 10 minutes ago"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/empty.xml"])
    @Throws(Exception::class)
    fun logbrokerReadingWhenOk() {
        clearPartitionOffset()
        insertPartitionOffset(11, "1")
        insertPartitionOffset(9, "2")
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/logbroker-reading"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("0;ok"))
    }

    @Test
    @DatabaseSetup(value = [
        "classpath:fixtures/controller/monitoring/unprocessed-meta-changes/before-not-ok.xml"
    ])
    @Throws(Exception::class)
    fun unprocessedMetaChangesWhenNotOk() {
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/unprocessed-meta-changes"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(
                "2;There are 1 unprocessed meta changes which are too old"))
    }

    @Test
    @DatabaseSetup(value = [
        "classpath:fixtures/controller/monitoring/unprocessed-meta-changes/before-ok.xml"
    ])
    @Throws(Exception::class)
    fun unprocessedMetaChangesWhenOk() {
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/unprocessed-meta-changes"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("0;ok"))
    }

    private fun clearPartitionOffset() {
        jdbcTemplate.update("delete from logbroker.partition_offset")
    }

    private fun insertPartitionOffset(minutesFromNow: Int, entitySuffix: String) {
        val localDateTime = LocalDateTime.now(clock).minusMinutes(minutesFromNow.toLong())
        jdbcTemplate.update("insert into logbroker.partition_offset " +
            "(entity, updated, partition, lb_offset) values " +
            "(?, ?, 'partition', 123)", "entity$entitySuffix", localDateTime)
    }
}
