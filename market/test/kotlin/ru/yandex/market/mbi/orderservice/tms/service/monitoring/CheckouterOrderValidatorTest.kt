package ru.yandex.market.mbi.orderservice.tms.service.monitoring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.OrderSourcePlatform
import ru.yandex.market.mbi.orderservice.common.model.yt.CheckouterEventEntities
import ru.yandex.market.mbi.orderservice.common.model.yt.LongWrapper
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedCheckouterEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.ProcessedCheckouterEventRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterOrderBasicInfo
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.common.util.DEFAULT_TIMEZONE_ZONE_ID
import ru.yandex.market.mbi.orderservice.common.util.UTC_ZONE_ID
import ru.yandex.market.mbi.orderservice.common.util.toInstantAtMoscowTime
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Instant
import java.time.LocalDateTime
import java.util.TimeZone

/**
 * Тесты для [CheckouterOrdersValidator]
 */
@DbUnitDataSet
@CleanupTables(classes = [ProcessedCheckouterEventEntity::class])
class CheckouterOrderValidatorTest : FunctionalTest() {

    private val defaultDateTime = LocalDateTime.of(2021, 10, 10, 12, 20, 0)

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var mockCheckouterApiService: CheckouterApiService

    @Autowired
    lateinit var checkouterOrdersValidator: CheckouterOrdersValidator

    @Autowired
    lateinit var processedCheckouterEventRepository: ProcessedCheckouterEventRepository

    @BeforeAll
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIMEZONE_ZONE_ID))
    }

    @BeforeEach
    fun mockCheckouter() {
        processedCheckouterEventRepository.insertRow(
            ProcessedCheckouterEventEntity(
                eventId = LongWrapper(1),
                eventTimestamp = defaultDateTime.toInstantAtMoscowTime().toEpochMilli(),
                processingTimestamp = Instant.now()
            )
        )
    }

    @Test
    @DbUnitDataSet(after = ["checkouterOrderValidatorTest.after.csv"])
    fun `verify checkouter order count validator pipeline`() {
        whenever(
            mockCheckouterApiService.getCreatedOrdersCountByDate(any(), any())
        ).doReturn(1)

        val rows = getRows()
        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(rows, emptyList(), emptyList())
        )

        checkouterOrdersValidator.runValidation()
    }

    @Test
    fun `verify events processing when order count differs`() {
        whenever(mockCheckouterApiService.getCreatedOrdersCountByDate(any(), any()))
            .doReturn(4)
        whenever(mockCheckouterApiService.getCreatedOrderIds(any(), any(), any()))
            .doReturn(
                listOf(
                    CheckouterOrderBasicInfo(14000L, 0),
                    CheckouterOrderBasicInfo(14001L, 0),
                    CheckouterOrderBasicInfo(14003L, 1_152_921_504_606_846_976),
                    CheckouterOrderBasicInfo(32323, null)
                )
            )

        val rows = getRows()
        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(rows, emptyList(), emptyList())
        )

        checkouterOrdersValidator.runValidation()

        val orderIdCaptor = argumentCaptor<Long>()

        verify(mockCheckouterApiService, times(3)).getEventsByOrderId(orderIdCaptor.capture(), any())
        assertThat(orderIdCaptor.allValues).containsExactlyInAnyOrder(14000L, 14001L, 14003L)
    }

    @Test
    @DbUnitDataSet(
        before = ["checkouterOrderValidatorTest.sameInterval.before.csv"],
        after = ["checkouterOrderValidatorTest.sameInterval.after.csv"]
    )
    fun `verify that same interval is not processed twice`() {
        checkouterOrdersValidator.runValidation()
    }

    private fun getRows(): List<OrderEntity> {
        return listOf(
            // слишком свежий заказ - не попадает в выборку для валидации
            OrderEntity(
                key = OrderKey(123, 15235),
                createdAt = defaultDateTime.minusMinutes(10).toInstantAtMoscowTime(),
                updatedAt = defaultDateTime.minusMinutes(5).toInstantAtMoscowTime(),
                status = MerchantOrderStatus.RESERVED,
            ),
            OrderEntity(
                key = OrderKey(142, 32323),
                createdAt = defaultDateTime.minusMinutes(100).toInstantAtMoscowTime(),
                updatedAt = defaultDateTime.minusMinutes(60).toInstantAtMoscowTime(),
                status = MerchantOrderStatus.DELIVERED,
            ),
            // заказ на внешней площадке - не попадет в выборку для валидации
            OrderEntity(
                key = OrderKey(142, 32324),
                createdAt = defaultDateTime.minusMinutes(100).toInstantAtMoscowTime(),
                updatedAt = defaultDateTime.minusMinutes(60).toInstantAtMoscowTime(),
                status = MerchantOrderStatus.DELIVERED,
                sourcePlatform = OrderSourcePlatform.OTHER
            )
        )
    }

    @AfterAll
    fun tearDown() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC_ZONE_ID))
    }
}
