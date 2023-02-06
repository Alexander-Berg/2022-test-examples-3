package ru.yandex.market.wms.core.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.dao.LocDao

internal class PassesCoordinatesServiceTest(
    @Autowired val passesCoordinatesService: PassesCoordinatesService,
    @Autowired @MockBean val locDao: LocDao,
) : IntegrationTest() {

    @BeforeEach
    fun setUp() {
        reset(locDao)
        whenever(locDao.getAllXCoordinatesInZone(anyString(), anyInt())).thenReturn(setOf(1, 2, 9))
    }

    @Test
    fun testCache() {
        val zone1 = "ZONE1"
        val zone2 = "ZONE2"
        val zone3 = "ZONE3"
        passesCoordinatesService.getPassesXCoordinates(zone1)
        passesCoordinatesService.getPassesXCoordinates(zone2)
        passesCoordinatesService.getPassesXCoordinates(zone1)
        passesCoordinatesService.getPassesXCoordinates(zone2)
        passesCoordinatesService.getPassesXCoordinates(zone3)
        passesCoordinatesService.getPassesXCoordinates(zone1)
        verify(locDao, times(1)).getAllXCoordinatesInZone(eq(zone1), anyInt())
        verify(locDao, times(1)).getAllXCoordinatesInZone(eq(zone2), anyInt())
        verify(locDao, times(1)).getAllXCoordinatesInZone(eq(zone3), anyInt())
    }
}
