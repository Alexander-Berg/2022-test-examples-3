package ru.yandex.market.mbi.orderservice.tms.service.order

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.orderservice.common.enum.OrderStockFreezeStatus
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.external.StockStorageApiService
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Clock
import java.time.Instant

/**
 * Тест на [UnfreezeOrderStockService]
 */
@CleanupTables(
    [
        OrderStockFreezeStatusEntity::class
    ]
)
class UnfreezeOrderStockServiceTest : FunctionalTest() {

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var stockStorageApiService: StockStorageApiService

    @Autowired
    lateinit var unfreezeOrderStockService: UnfreezeOrderStockService

    @BeforeEach
    fun init() {
        whenever(clock.instant()).thenReturn(Instant.ofEpochMilli(1600000000000))
    }

    @Test
    fun `simple unfreeze test`() {
        orderRepository.storeStockFreezeStatuses(
            listOf(
                /**
                 * Не попадет в выборку
                 */
                OrderStockFreezeStatusEntity(
                    key = OrderStockFreezeStatusKey(1, 1),
                    orderCreatedAt = clock.instant(),
                    OrderStockFreezeStatus.STARTED
                ),
                /**
                 * Попадет в выборку
                 */
                OrderStockFreezeStatusEntity(
                    key = OrderStockFreezeStatusKey(1, 2),
                    orderCreatedAt = clock.instant().minusMillis(1_000_000),
                    OrderStockFreezeStatus.STARTED
                ),
                /**
                 * Не попадет в выборку
                 */
                OrderStockFreezeStatusEntity(
                    key = OrderStockFreezeStatusKey(1, 3),
                    orderCreatedAt = clock.instant().minusMillis(100_000),
                    OrderStockFreezeStatus.FROZEN
                ),
                /**
                 * Не попадет в выборку
                 */
                OrderStockFreezeStatusEntity(
                    key = OrderStockFreezeStatusKey(1, 4),
                    orderCreatedAt = clock.instant().minusMillis(100_000),
                    OrderStockFreezeStatus.UNFROZEN
                ),
                /**
                 * Попадет в выборку
                 */
                OrderStockFreezeStatusEntity(
                    key = OrderStockFreezeStatusKey(1, 5),
                    orderCreatedAt = clock.instant(),
                    OrderStockFreezeStatus.UNFREEZE_PENDING
                ),
            )
        )

        unfreezeOrderStockService.runUnfreeze()

        verify(stockStorageApiService, times(2)).unfreezeStocks(any())

        Assertions.assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(
                    1,
                    1
                )
            )
        ).satisfies {
            Assertions.assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.STARTED)
        }

        Assertions.assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(
                    1,
                    2
                )
            )
        ).satisfies {
            Assertions.assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.UNFROZEN)
        }

        Assertions.assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(
                    1,
                    3
                )
            )
        ).satisfies {
            Assertions.assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.FROZEN)
        }

        Assertions.assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(
                    1,
                    4
                )
            )
        ).satisfies {
            Assertions.assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.UNFROZEN)
        }

        Assertions.assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(
                    1,
                    5
                )
            )
        ).satisfies {
            Assertions.assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.UNFROZEN)
        }
    }
}
