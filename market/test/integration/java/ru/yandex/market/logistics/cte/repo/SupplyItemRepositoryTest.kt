package ru.yandex.market.logistics.cte.repo

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest


internal class SupplyItemRepositoryTest(
    @Autowired val supplyItemRepository: SupplyItemRepository
) : IntegrationTest()  {

    @Test
    @DatabaseSetup("classpath:repository/supply_item_updated_at/before.xml")
    fun validateUpdatedAtOnUpdate() {
        val supplyItem = supplyItemRepository.findById(1).get()
        val previousUpdatedTimestamp = supplyItem.updatedAt
        supplyItem.boxId = "some update"
        supplyItemRepository.save(supplyItem)

        val updatedSupplyItem = supplyItemRepository.findById(1).get()
        val currentUpdatedTimestamp = updatedSupplyItem.updatedAt

        assertions.assertThat(previousUpdatedTimestamp < currentUpdatedTimestamp).isTrue
        assertions.assertThat(supplyItem.updatedAt).isNotNull
        assertions.assertThat(updatedSupplyItem.updatedAt).isNotNull
    }

    @Test
    @DatabaseSetup("classpath:repository/supply_item_updated_at/before.xml")
    fun validateUpdatedAtOnUpdateWithNoAttribute() {
        val supplyItem = supplyItemRepository.findAllBySupplyIdAndUuidIn(1, setOf("uuid1"))

        assertions.assertThat(supplyItem.size).isOne
    }
}
