package ru.yandex.market.mbi.orderservice.tms.persistence.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.orderservice.common.enum.RegionType
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.RegionRepository

@DbUnitDataSet(before = ["region/regionRepositoryTest.before.csv"])
class RegionRepositoryTest : FunctionalTest() {

    @Autowired
    lateinit var regionRepository: RegionRepository

    @Test
    fun `test getting parent region`() {
        val roundToCountry = regionRepository.roundRegionTo(1, RegionType.COUNTRY) ?: fail("Region was expected")
        assertThat(roundToCountry).satisfies {
            assertThat(it.id).isEqualTo(3)
            assertThat(it.name).isEqualTo("Russia")
            assertThat(it.type).isEqualTo(RegionType.COUNTRY)
        }
    }
}
