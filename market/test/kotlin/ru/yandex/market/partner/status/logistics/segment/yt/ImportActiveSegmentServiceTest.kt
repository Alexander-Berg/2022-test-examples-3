package ru.yandex.market.partner.status.logistics.segment.yt

import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.yt.YtTableReader
import ru.yandex.market.partner.status.yt.factory.YtTableReaderFactory
import java.time.Instant

/**
 * Тесты для [ImportActiveSegmentService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ImportActiveSegmentServiceTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var importActiveSegmentService: ImportActiveSegmentService

    @Autowired
    private lateinit var ytTableReaderFactory: YtTableReaderFactory

    @Test
    @DbUnitDataSet(after = ["ImportSegmentStatusServiceTest/empty.after.csv"])
    fun `empty yt table`() {
        mockYt(emptyList())
        importActiveSegmentService.doImport()
    }

    @Test
    @DbUnitDataSet(after = ["ImportSegmentStatusServiceTest/new.after.csv"])
    fun `all segments are new`() {
        mockYt(
            listOf(
                CombinatorActiveSegmentYtEntry(
                    serviceId = 100L,
                    serviceType = CombinatorDeliveryServiceYtEntryType.DELIVERY,
                    Instant.now()
                ),
                CombinatorActiveSegmentYtEntry(
                    serviceId = 200L,
                    serviceType = CombinatorDeliveryServiceYtEntryType.SUPPLIER,
                    Instant.now()
                ),
            )
        )
        importActiveSegmentService.doImport()
    }

    @Test
    @DbUnitDataSet(
        before = ["ImportSegmentStatusServiceTest/outdated.before.csv"],
        after = ["ImportSegmentStatusServiceTest/outdated.after.csv"]
    )
    fun `delete outdated segments`() {
        mockYt(
            listOf(
                CombinatorActiveSegmentYtEntry(
                    serviceId = 100L,
                    serviceType = CombinatorDeliveryServiceYtEntryType.DELIVERY,
                    Instant.now()
                ),
                CombinatorActiveSegmentYtEntry(
                    serviceId = 200L,
                    serviceType = CombinatorDeliveryServiceYtEntryType.SUPPLIER,
                    Instant.now()
                ),
            )
        )
        importActiveSegmentService.doImport()
    }

    @Test
    @DbUnitDataSet(
        before = ["ImportSegmentStatusServiceTest/partnerWithLink.before.csv"],
        after = ["ImportSegmentStatusServiceTest/partnerWithLink.after.csv"]
    )
    fun `update resolver status`() {
        mockYt(
            listOf(
                CombinatorActiveSegmentYtEntry(
                    serviceId = 100L,
                    serviceType = CombinatorDeliveryServiceYtEntryType.DELIVERY,
                    Instant.now()
                )
            )
        )
        importActiveSegmentService.doImport()
    }


    private fun mockYt(data: List<CombinatorActiveSegmentYtEntry>) {
        whenever(
            ytTableReaderFactory.createYQLPreparedReader<CombinatorActiveSegmentYtEntry>(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(YtTableReader { flow { data.forEach { emit(it) } } })
    }
}
