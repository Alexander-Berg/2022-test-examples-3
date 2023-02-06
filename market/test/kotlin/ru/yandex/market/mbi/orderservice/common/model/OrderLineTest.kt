package ru.yandex.market.mbi.orderservice.common.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.model.yt.ItemStatuses

class OrderLineTest {

    @Test
    fun `verify update statuses for partially unredeemed and return creation`() {
        val oldStatuses = ItemStatuses(
            1,
            mapOf(
                Pair(MerchantItemStatus.CANCELLED.name, 2),
                Pair(MerchantItemStatus.DELIVERED_TO_BUYER.name, 3),
                Pair(MerchantItemStatus.DELETED.name, 1)
            )
        )

        val newStatuses = oldStatuses.updateItemsReturnStatuses(
            5,
            mapOf(
                Pair(MerchantItemStatus.RETURN_CREATED, 1),
                Pair(MerchantItemStatus.UNREDEEMED_CREATED, 1)
            )
        )

        assertThat(newStatuses).satisfies {
            assertThat(it.statuses[MerchantItemStatus.RETURN_CREATED.name]).isEqualTo(1)
            assertThat(it.statuses[MerchantItemStatus.UNREDEEMED_CREATED.name]).isEqualTo(1)
            assertThat(it.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(2)
            assertThat(it.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(1)
            assertThat(it.statuses[MerchantItemStatus.CANCELLED.name]).isEqualTo(1)
            assertThat(it.statuses.size).isEqualTo(5)
        }
    }

    @Test
    fun `verify update statuses for return and unredeemed transits`() {
        val oldStatuses = ItemStatuses(
            1,
            mapOf(
                Pair(MerchantItemStatus.UNREDEEMED_READY_FOR_PICKUP.name, 2),
                Pair(MerchantItemStatus.RETURN_CREATED.name, 3),
                Pair(MerchantItemStatus.DELETED.name, 1)
            )
        )

        val newStatuses = oldStatuses.updateItemsReturnStatuses(
            5,
            mapOf(
                Pair(MerchantItemStatus.RETURN_IN_TRANSIT, 3),
                Pair(MerchantItemStatus.UNREDEEMED_PICKED, 2)
            )
        )

        assertThat(newStatuses).satisfies {
            assertThat(it.statuses[MerchantItemStatus.RETURN_IN_TRANSIT.name]).isEqualTo(3)
            assertThat(it.statuses[MerchantItemStatus.UNREDEEMED_PICKED.name]).isEqualTo(2)
            assertThat(it.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(1)
            assertThat(it.statuses.size).isEqualTo(3)
        }
    }

    @Test
    fun `verify update statuses for remove return`() {
        val oldStatuses = ItemStatuses(
            1,
            mapOf(
                Pair(MerchantItemStatus.UNREDEEMED_READY_FOR_PICKUP.name, 2),
                Pair(MerchantItemStatus.RETURN_CREATED.name, 3),
                Pair(MerchantItemStatus.DELETED.name, 1)
            )
        )

        // Как будто перезаливаем данные и возвратов и невыкупов уже нет, все должно вернуться в delivered и cancelled
        val newStatuses = oldStatuses.updateItemsReturnStatuses(
            5,
            mapOf()
        )

        assertThat(newStatuses).satisfies {
            assertThat(it.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(3)
            assertThat(it.statuses[MerchantItemStatus.CANCELLED.name]).isEqualTo(2)
            assertThat(it.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(1)
            assertThat(it.statuses.size).isEqualTo(3)
        }
    }

    @Test
    fun `verify update statuses for 2 return`() {
        val oldStatuses = ItemStatuses(
            1,
            mapOf(
                Pair(MerchantItemStatus.DELIVERED_TO_BUYER.name, 2),
                Pair(MerchantItemStatus.RETURN_CREATED.name, 3),
                Pair(MerchantItemStatus.DELETED.name, 1)
            )
        )

        val newStatuses = oldStatuses.updateItemsReturnStatuses(
            5,
            mapOf(
                Pair(MerchantItemStatus.RETURN_CREATED, 2),
                Pair(MerchantItemStatus.RETURN_IN_TRANSIT, 3),
                Pair(MerchantItemStatus.DELETED, 1)
            )
        )

        assertThat(newStatuses).satisfies {
            assertThat(it.statuses[MerchantItemStatus.RETURN_CREATED.name]).isEqualTo(2)
            assertThat(it.statuses[MerchantItemStatus.RETURN_IN_TRANSIT.name]).isEqualTo(3)
            assertThat(it.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(1)
            assertThat(it.statuses.size).isEqualTo(3)
        }
    }

    @Test
    fun `verify update statuses for partially unredeemed delivered status`() {
        val oldStatuses = ItemStatuses(
            1,
            mapOf(
                Pair(MerchantItemStatus.DELIVERED_TO_BUYER.name, 2),
                Pair(MerchantItemStatus.DELETED.name, 1)
            )
        )

        val newStatuses = oldStatuses.updateItemsReturnStatuses(
            5,
            mapOf(
                Pair(MerchantItemStatus.UNREDEEMED_CREATED, 1)
            )
        )

        assertThat(newStatuses).satisfies {
            assertThat(it.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(2)
            assertThat(it.statuses[MerchantItemStatus.UNREDEEMED_CREATED.name]).isEqualTo(1)
            assertThat(it.statuses.size).isEqualTo(2)
        }
    }

    @Test
    fun `verify update statuses for full unredeemed cancelled status`() {
        val oldStatuses = ItemStatuses(
            1,
            mapOf(
                Pair(MerchantItemStatus.CANCELLED.name, 2),
                Pair(MerchantItemStatus.DELETED.name, 1)
            )
        )

        val newStatuses = oldStatuses.updateItemsReturnStatuses(
            5,
            mapOf(
                Pair(MerchantItemStatus.UNREDEEMED_CREATED, 1)
            )
        )

        assertThat(newStatuses).satisfies {
            assertThat(it.statuses[MerchantItemStatus.CANCELLED.name]).isEqualTo(1)
            assertThat(it.statuses[MerchantItemStatus.UNREDEEMED_CREATED.name]).isEqualTo(1)
            assertThat(it.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(1)
            assertThat(it.statuses.size).isEqualTo(3)
        }
    }
}
