package ru.yandex.market.wms.shipping.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import java.time.Instant
import java.time.LocalDateTime

class OutboundDaoTest(@Autowired private val dao: VehicleOutboundDao) : IntegrationTest() {

    @Test
    @DatabaseSetup("/dao/outbound/db.xml")
    fun getOutboundDayStartTest() {
        val atTheVeryStart: LocalDateTime = dao.getStartOfDay(Instant.parse("2020-12-12T00:00:00.00Z"))
        val rightBeforeCutoff: LocalDateTime = dao.getStartOfDay(Instant.parse("2020-12-12T08:00:00.00Z"))
        // граница дня (12 часов) по utc
        val atCutoff: LocalDateTime = dao.getStartOfDay(Instant.parse("2020-12-12T09:00:00.00Z"))
        val afterCutoff: LocalDateTime = dao.getStartOfDay(Instant.parse("2020-12-12T23:00:00.00Z"))
        Assertions.assertEquals(LocalDateTime.of(2020, 12, 11, 9, 0, 0), atTheVeryStart)
        Assertions.assertEquals(LocalDateTime.of(2020, 12, 11, 9, 0, 0), rightBeforeCutoff)
        Assertions.assertEquals(LocalDateTime.of(2020, 12, 12, 9, 0, 0), atCutoff)
        Assertions.assertEquals(LocalDateTime.of(2020, 12, 12, 9, 0, 0), afterCutoff)
    }
}
