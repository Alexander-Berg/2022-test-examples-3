package ru.yandex.market.wms.core.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.base.dto.ConveyorZoneType
import ru.yandex.market.wms.core.exception.NoEnabledZonesException
import ru.yandex.market.wms.core.exception.NoZonesWithConveyorTypeException
import ru.yandex.market.wms.core.service.zone.ZoneSelectionService

class ZoneSelectionServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var zoneSelectionService: ZoneSelectionService

    @MockBean
    @Autowired
    private lateinit var dbConfigService: DbConfigService

    @BeforeEach
    fun setupConfig() {
        Mockito.`when`(dbConfigService.getConfigAsStringList("YM_DEST_ZONE_TYPES"))
            .thenReturn(
                listOf(
                    ConveyorZoneType.EXPENSIVE,
                    ConveyorZoneType.FIRST_FLOOR
                ).map { it.name })
        Mockito.`when`(dbConfigService.getConfigAsInteger(eq("YM_ZONE_CONGESTION_HOUR_OFFSET"), anyInt()))
            .thenReturn(-5)
    }

    @Test
    @DatabaseSetup("/service/zone-selection/happy-path-first-floor/db.xml")
    @ExpectedDatabase(
        value = "/service/zone-selection/happy-path-first-floor/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun happyPathFirstFloor() {
        val result: String =
            zoneSelectionService.selectDestinationZone(ConveyorZoneType.FIRST_FLOOR, "1", true)
        assertEquals(result, "01")
    }

    @Test
    @DatabaseSetup("/service/zone-selection/happy-path-few-sku/db.xml")
    @ExpectedDatabase(
        value = "/service/zone-selection/happy-path-few-sku/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun happyPathFewSku() {
        val result: String =
            zoneSelectionService.selectDestinationZone(null, "CONTAINER02", true)
        assertEquals(result, "07")
    }

    @Test
    @DatabaseSetup("/service/zone-selection/happy-path-one-sku/db.xml")
    @ExpectedDatabase(
        value = "/service/zone-selection/happy-path-one-sku/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun happyPathOneSku() {
        val result: String =
            zoneSelectionService.selectDestinationZone(null, "CONTAINER01", true)
        assertEquals(result, "06")
    }

    @Test
    @DatabaseSetup("/service/zone-selection/one-loaded-zone/db.xml")
    @ExpectedDatabase(
        value = "/service/zone-selection/one-loaded-zone/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun happyPathOneLoadedZone() {
        val result: String =
            zoneSelectionService.selectDestinationZone(null, "CONTAINER01", true)
        assertEquals(result, "07")
    }

    @Test
    @DatabaseSetup("/service/zone-selection/happy-path-random-zone/db.xml")
    @ExpectedDatabase(
        value = "/service/zone-selection/happy-path-random-zone/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun happyPathRandomFloorZone() {
        val result: String =
            zoneSelectionService.selectDestinationZone(ConveyorZoneType.FOURTH_FLOOR, "CONTAINER", true)
        assertTrue(result == "04" || result == "04-2")
    }


    @Test
    @DatabaseSetup("/service/zone-selection/no-enabled-zones/db.xml")
    @ExpectedDatabase(
        value = "/service/zone-selection/no-enabled-zones/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun noEnabledZones() {
        assertThrows(NoEnabledZonesException::class.java) {
            zoneSelectionService.selectDestinationZone(
                ConveyorZoneType.EXPENSIVE,
                "CONTAINER01",
                true
            )
        }
    }

    @Test
    @DatabaseSetup("/service/zone-selection/no-placement-buffer-cell/db.xml")
    @ExpectedDatabase(
        value = "/service/zone-selection/no-placement-buffer-cell/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun noPlacementBufferCell() {
        assertThrows(Exception::class.java) {
            zoneSelectionService.selectDestinationZone(
                ConveyorZoneType.EXPENSIVE,
                "CONTAINER01",
                true
            )
        }
    }

    @Test
    @DatabaseSetup("/service/zone-selection/no-stock-zones/db.xml")
    @ExpectedDatabase(
        value = "/service/zone-selection/no-stock-zones/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun noStockZones() {
        val result: String =
            zoneSelectionService.selectDestinationZone(null, "CONTAINER01", true)
        assertEquals(result, "01")
    }

    @Test
    fun noZonesWhenEnabledZonesNotConsidered() {
        assertThrows(NoZonesWithConveyorTypeException::class.java) {
            zoneSelectionService.selectDestinationZone(
                ConveyorZoneType.SECOND_FLOOR,
                "Container",
                false
            )
        }
    }
}
