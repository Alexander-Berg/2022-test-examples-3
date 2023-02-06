package ru.yandex.market.mbi.orderservice.common.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.model.yt.ItemStatuses
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineKey

class OrderTransitionUtilTest {

    @Test
    fun `verify convert to returned`() {
        Assertions.assertThat(
            calculateOrderReturnStatusByLines(
                MerchantOrderStatus.DELIVERED,
                listOf(
                    OrderLineEntity(
                        OrderLineKey(1, 1, 1),
                        itemStatuses = ItemStatuses(
                            orderLineId = 1,
                            mapOf(
                                Pair(MerchantItemStatus.RETURN_CREATED.name, 2),
                                Pair(MerchantItemStatus.CANCELLED.name, 1)
                            )
                        )
                    ),
                    OrderLineEntity(
                        OrderLineKey(1, 1, 2),
                        itemStatuses = ItemStatuses(
                            orderLineId = 2,
                            mapOf(
                                Pair(MerchantItemStatus.UNREDEEMED_PICKED.name, 2),
                                Pair(MerchantItemStatus.DELETED.name, 2)
                            )
                        )
                    ),
                )
            )
        ).isEqualTo(MerchantOrderStatus.RETURNED)
    }

    @Test
    fun `verify convert to partially returned`() {
        Assertions.assertThat(
            calculateOrderReturnStatusByLines(
                MerchantOrderStatus.DELIVERED,
                listOf(
                    OrderLineEntity(
                        OrderLineKey(1, 1, 1),
                        itemStatuses = ItemStatuses(
                            orderLineId = 1,
                            mapOf(
                                Pair(MerchantItemStatus.RETURN_CREATED.name, 2),
                                Pair(MerchantItemStatus.CANCELLED.name, 1)
                            )
                        )
                    ),
                    OrderLineEntity(
                        OrderLineKey(1, 1, 2),
                        itemStatuses = ItemStatuses(
                            orderLineId = 2,
                            mapOf(
                                Pair(MerchantItemStatus.DELIVERED_TO_BUYER.name, 2),
                                Pair(MerchantItemStatus.DELETED.name, 2)
                            )
                        )
                    ),
                )
            )
        ).isEqualTo(MerchantOrderStatus.PARTIALLY_RETURNED)
    }

    @Test
    fun `verify convert to delivered`() {
        Assertions.assertThat(
            calculateOrderReturnStatusByLines(
                MerchantOrderStatus.DELIVERED,
                listOf(
                    OrderLineEntity(
                        OrderLineKey(1, 1, 1),
                        itemStatuses = ItemStatuses(
                            orderLineId = 1,
                            mapOf(
                                Pair(MerchantItemStatus.DELIVERED_TO_BUYER.name, 2)
                            )
                        )
                    ),
                    OrderLineEntity(
                        OrderLineKey(1, 1, 2),
                        itemStatuses = ItemStatuses(
                            orderLineId = 2,
                            mapOf(
                                Pair(MerchantItemStatus.DELETED.name, 2)
                            )
                        )
                    ),
                )
            )
        ).isEqualTo(MerchantOrderStatus.DELIVERED)
    }

    @Test
    fun `verify convert to partially delivered`() {
        Assertions.assertThat(
            calculateOrderReturnStatusByLines(
                MerchantOrderStatus.DELIVERED,
                listOf(
                    OrderLineEntity(
                        OrderLineKey(1, 1, 1),
                        itemStatuses = ItemStatuses(
                            orderLineId = 1,
                            mapOf(
                                Pair(MerchantItemStatus.DELIVERED_TO_BUYER.name, 2),
                                Pair(MerchantItemStatus.CANCELLED.name, 1)
                            )
                        )
                    ),
                    OrderLineEntity(
                        OrderLineKey(1, 1, 2),
                        itemStatuses = ItemStatuses(
                            orderLineId = 2,
                            mapOf(
                                Pair(MerchantItemStatus.UNREDEEMED_PICKED.name, 2),
                                Pair(MerchantItemStatus.DELETED.name, 2)
                            )
                        )
                    ),
                )
            )
        ).isEqualTo(MerchantOrderStatus.PARTIALLY_DELIVERED)
    }

    @Test
    fun `verify convert to unredeemed`() {
        Assertions.assertThat(
            calculateOrderReturnStatusByLines(
                MerchantOrderStatus.DELIVERED,
                listOf(
                    OrderLineEntity(
                        OrderLineKey(1, 1, 1),
                        itemStatuses = ItemStatuses(
                            orderLineId = 1,
                            mapOf(
                                Pair(MerchantItemStatus.CANCELLED.name, 1)
                            )
                        )
                    ),
                    OrderLineEntity(
                        OrderLineKey(1, 1, 2),
                        itemStatuses = ItemStatuses(
                            orderLineId = 2,
                            mapOf(
                                Pair(MerchantItemStatus.UNREDEEMED_PICKED.name, 2),
                                Pair(MerchantItemStatus.DELETED.name, 2)
                            )
                        )
                    ),
                )
            )
        ).isEqualTo(MerchantOrderStatus.CANCELLED_IN_DELIVERY)
    }
}
