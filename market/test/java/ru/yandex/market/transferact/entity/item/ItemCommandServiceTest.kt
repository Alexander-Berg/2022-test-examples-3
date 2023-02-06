package ru.yandex.market.transferact.entity.item

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import java.math.BigDecimal

const val type = "order"

class ItemCommandServiceTest : AbstractTest() {

    @Autowired
    lateinit var itemCommandService: ItemCommandService

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Test
    fun `When create item request then return new entity`() {
        val createItemProjection = itemCommandService.create(
            listOf(
                ItemCommand.Create(
                    ItemIdentifier(externalId, null),
                    type,
                    BigDecimal.TEN,
                    1
                )
            )
        )
        Assertions.assertThat(createItemProjection.size).isEqualTo(1)
        val createdItem = itemRepository.getByIdOrThrow(createItemProjection[0].id)
        Assertions.assertThat(createdItem.itemIdentifier.externalId).isEqualTo(externalId)
        Assertions.assertThat(createdItem.type).isEqualTo(type)
        Assertions.assertThat(createdItem.declaredCost).isEqualTo(BigDecimal.TEN)
        Assertions.assertThat(createdItem.placeCount).isEqualTo(1)
    }

    @Test
    fun `Update item cost if item with same external id exist`() {
        val createdEntities = itemCommandService.create(
            listOf(
                ItemCommand.Create(
                    ItemIdentifier(externalId, null),
                    type,
                    BigDecimal.TEN,
                    1
                )
            )
        )

        val projectionWithSameExternalId = itemCommandService.create(
            listOf(
                ItemCommand.Create(
                    ItemIdentifier(externalId, null),
                    type,
                    BigDecimal.ONE,
                    1
                )
            )
        )

        Assertions.assertThat(createdEntities.size).isEqualTo(1)
        Assertions.assertThat(createdEntities[0].created).isTrue
        Assertions.assertThat(createdEntities[0].declaredCost).isEqualTo(BigDecimal.TEN)
        Assertions.assertThat(projectionWithSameExternalId.size).isEqualTo(1)
        Assertions.assertThat(projectionWithSameExternalId[0].created).isFalse
        Assertions.assertThat(projectionWithSameExternalId[0].declaredCost).isEqualTo(BigDecimal.ONE)
    }

    @Test
    fun `Do not update item cost if new cost is 0`() {
        val createdEntities = itemCommandService.create(
            listOf(
                ItemCommand.Create(
                    ItemIdentifier(externalId, null),
                    type,
                    BigDecimal.TEN,
                    1
                )
            )
        )

        val projectionWithSameExternalId = itemCommandService.create(
            listOf(
                ItemCommand.Create(
                    ItemIdentifier(externalId, null),
                    type,
                    BigDecimal.ZERO,
                    1
                )
            )
        )

        Assertions.assertThat(createdEntities.size).isEqualTo(1)
        Assertions.assertThat(createdEntities[0].created).isTrue
        Assertions.assertThat(createdEntities[0].declaredCost).isEqualTo(BigDecimal.TEN)
        Assertions.assertThat(projectionWithSameExternalId.size).isEqualTo(1)
        Assertions.assertThat(projectionWithSameExternalId[0].created).isFalse
        Assertions.assertThat(projectionWithSameExternalId[0].declaredCost).isEqualTo(BigDecimal.TEN)
    }

    @Test
    fun `Create multiplace order as two places and then as single order`() {
        val firstPlace = createItem(
            ItemCommand.Create(
                ItemIdentifier(externalId, "1"),
                type = "PLACE",
                BigDecimal.TEN,
                1
            )
        )
        Assertions.assertThat(firstPlace.created).isTrue

        val secondPlace = createItem(
            ItemCommand.Create(
                ItemIdentifier(externalId, "2"),
                type = "PLACE",
                BigDecimal.TEN,
                1
            )
        )
        Assertions.assertThat(secondPlace.created).isTrue

        val order = createItem(
            ItemCommand.Create(
                ItemIdentifier(externalId, null),
                type = "ORDER",
                BigDecimal.TEN,
                1
            )
        )
        Assertions.assertThat(order.created).isTrue
    }

    @Test
    fun `Create item with different externalId but same placeId`() {
        var createItemProjections = itemCommandService.create(
            listOf(
                ItemCommand.Create(
                    ItemIdentifier(externalId, placeId),
                    "PLACE",
                    BigDecimal.TEN,
                    1
                )
            )
        )
        Assertions.assertThat(createItemProjections.size).isEqualTo(1)
        var createdItem = createItemProjections[0]
        Assertions.assertThat(createdItem.created).isTrue

        createItemProjections = itemCommandService.create(
            listOf(
                ItemCommand.Create(
                    ItemIdentifier("another_external_id", placeId),
                    "PLACE",
                    BigDecimal.TEN,
                    1
                )
            )
        )
        Assertions.assertThat(createItemProjections.size).isEqualTo(1)
        createdItem = createItemProjections[0]
        Assertions.assertThat(createdItem.created).isTrue
    }

    private fun createItem(command: ItemCommand.Create): CreateItem {
        val items = itemCommandService.create(listOf(command))
        Assertions.assertThat(items.size).isEqualTo(1)

        return items[0]
    }
}
