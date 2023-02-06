package ru.yandex.market.replenishment.autoorder.service.tender

import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.model.dto.TenderTruckDTO
import ru.yandex.market.replenishment.autoorder.model.dto.TenderTruckSskuDTO
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin

@WithMockLogin
class TenderTruckServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var service: TenderTruckService

    @Test
    @DbUnitDataSet(before = ["TenderTruckServiceTest.before.csv"])
    fun getTenderTrucks() {
        val trucks = service.getTenderTrucks(10)
        assertThat(trucks.keys, containsInAnyOrder(100, 200))
        var trs = trucks[100]!!
        assertEquals(trs.size, 3)
        val default = trs.first { it.truckId == -1L }
        assertEquals(TenderTruckDTO(-1, default.cargo, null, null, null, null), default)
        assertThat(
            default.cargo, containsInAnyOrder(
                TenderTruckSskuDTO("0100.111", 14),
                TenderTruckSskuDTO("0100.222", 2),
            )
        )

        val truck91 = trs.first { it.truckId == 91L }
        assertEquals(TenderTruckDTO(91, truck91.cargo, warehouseId = 172), truck91)
        assertThat(
            truck91.cargo, containsInAnyOrder(
                TenderTruckSskuDTO("0100.111", 5),
                TenderTruckSskuDTO("0100.222", 7),
                TenderTruckSskuDTO("0100.333", 8),
            )
        )

        val truck92 = trs.first { it.truckId == 92L }
        assertEquals(TenderTruckDTO(92, truck92.cargo, warehouseId = 304), truck92)
        assertThat(
            truck92.cargo, containsInAnyOrder(
                TenderTruckSskuDTO("0100.111", 10),
                TenderTruckSskuDTO("0100.222", 14),
                TenderTruckSskuDTO("0100.333", 16),
            )
        )

        trs = trucks[200]!!
        assertEquals(trs.size, 1)
        assertEquals(trs[0], TenderTruckDTO(-1, listOf(TenderTruckSskuDTO("0200.111", 15))))
    }

    @Test
    @DbUnitDataSet(before = ["TenderTruckServiceTest-without-default.before.csv"])
    fun getTenderTrucks_withoutDefault() {
        val trucks = service.getTenderTrucks(10)
        assertThat(trucks.keys, containsInAnyOrder(100, 200))
        var trs = trucks[100]!!
        assertEquals(trs.size, 2)
        assertThat(
            trs.first { it.truckId == 91L }.cargo, containsInAnyOrder(
                TenderTruckSskuDTO("0100.111", 5),
                TenderTruckSskuDTO("0100.222", 7),
                TenderTruckSskuDTO("0100.333", 8),
            )
        )
        assertThat(
            trs.first { it.truckId == 92L }.cargo, containsInAnyOrder(
                TenderTruckSskuDTO("0100.111", 24),
                TenderTruckSskuDTO("0100.222", 16),
                TenderTruckSskuDTO("0100.333", 16),
            )
        )

        trs = trucks[200]!!
        assertEquals(trs.size, 1)
        assertEquals(trs[0], TenderTruckDTO(-1, listOf(TenderTruckSskuDTO("0200.111", 15))))
    }
}
