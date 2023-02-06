package ru.yandex.market.logistics.yard.health

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import java.time.Clock
import java.time.LocalDateTime

class CustomMonitoringControllerTest(@Autowired private val jdbcTemplate: JdbcTemplate,
                                     @Autowired private val clock: Clock): AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/empty.xml"])
    @Throws(Exception::class)
    fun logbrokerReadingWhenNotOk() {
        clearPartitionOffset()
        insertPartitionOffset(31, "1")
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/logbroker-reading"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(
                "2;Last successful reading from Logbroker was at 2020-01-01T14:29, " +
                    "more than 30 minutes ago"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/empty.xml"])
    @Throws(Exception::class)
    fun logbrokerReadingWhenOk() {
        clearPartitionOffset()
        insertPartitionOffset(31, "1")
        insertPartitionOffset(29, "2")
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/logbroker-reading"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("0;ok"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/empty.xml"])
    @Throws(Exception::class)
    fun logbrokerReadingWhenNoDataFromLogBroker() {
        clearPartitionOffset()
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/logbroker-reading"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("0;ok"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/empty.xml"])
    @Throws(Exception::class)
    fun clientsInStatesOkWhenNoData() {
        clearPartitionOffset()
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/client-in-state-too-long"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("0;ok"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/custom-monitoring/before.xml"])
    @Throws(Exception::class)
    fun clientsInStatesNotOk() {
        clearPartitionOffset()
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/client-in-state-too-long"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("2;Clients stayed in states too long: [6]"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/custom-monitoring/before_2.xml"])
    @Throws(Exception::class)
    fun clientsInStatesOk() {
        clearPartitionOffset()
        mockMvc!!.perform(MockMvcRequestBuilders.get("/health/client-in-state-too-long"))
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
