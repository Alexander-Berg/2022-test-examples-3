package ru.yandex.market.wms.placement.service.impl

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.placement.service.CoreService

class CoreServiceIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var coreService: CoreService

    @Test
    @DatabaseSetup("/service/placement/core-service/immutable.xml")
    @ExpectedDatabase(
        "/service/placement/core-service/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getPlacementBufLocsReturnActualValues() {
        val expectedLocs = listOf("PLC_BUF_1", "PLC_BUF_2")
        val locs = coreService.getPlacementBufLocs()
        assertTrue(locs.containsAll(expectedLocs), "Expected: $expectedLocs, found: $locs")
        assertEquals(expectedLocs.size, locs.size)
    }
}
