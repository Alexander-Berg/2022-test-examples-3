package ru.yandex.market.transferact.entity.item

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import java.math.BigDecimal

const val externalId = "externalId"
const val placeId = "placeId"
const val orderType = "ORDER"

class ItemRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Test
    fun `When findByExternalId then return Item`() {
        val item = ItemEntity(
            itemIdentifier = ItemIdentifier(
                externalId = externalId,
                placeId = null
            ),
            type = orderType,
            declaredCost = BigDecimal.TEN,
            placeCount = 1
        )
        itemRepository.save(item)

        val foundItems = itemRepository.findAllByItemIdentifierExternalIdInAndItemIdentifierPlaceIdIsNull(listOf(externalId))
        assertThat(foundItems.size).isEqualTo(1)
        val foundItem = foundItems[0]
        assertThat(foundItem).isNotNull
        assertThat(foundItem.itemIdentifier.externalId).isEqualTo(externalId)
        assertThat(foundItem.declaredCost).isEqualTo(BigDecimal.TEN)
        assertThat(foundItem.type).isEqualTo(orderType)
        assertThat(foundItem.placeCount).isEqualTo(1)
    }

    @Test
    fun `When findByExternalId then return null`() {
        val item = ItemEntity(
            itemIdentifier = ItemIdentifier(
                externalId = externalId,
                placeId = null
            ),
            type = orderType,
            declaredCost = BigDecimal.TEN,
            placeCount = 1
        )
        itemRepository.save(item)

        val foundItems = itemRepository.findAllByItemIdentifierExternalIdInAndItemIdentifierPlaceIdIsNull(listOf("bad external id"))

        assertThat(foundItems).isEmpty()
    }

    @Test
    fun `When findAllByPlaceIdIn then return Item`() {
        val item = ItemEntity(
            itemIdentifier = ItemIdentifier(
                externalId = externalId,
                placeId = placeId
            ),
            type = orderType,
            declaredCost = BigDecimal.TEN,
            placeCount = 1,
        )
        itemRepository.save(item)

        val foundItems = itemRepository.findAllByItemIdentifierPlaceIdIn(listOf(placeId))
        assertThat(foundItems.size).isEqualTo(1)
        val foundItem = foundItems[0]
        assertThat(foundItem).isNotNull
        assertThat(foundItem.itemIdentifier.externalId).isEqualTo(externalId)
        assertThat(foundItem.itemIdentifier.placeId).isEqualTo(placeId)
        assertThat(foundItem.declaredCost).isEqualTo(BigDecimal.TEN)
        assertThat(foundItem.type).isEqualTo(orderType)
        assertThat(foundItem.placeCount).isEqualTo(1)
    }

}
