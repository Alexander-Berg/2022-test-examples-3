package ru.yandex.market.partner.status.mbi.partner.yt

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
 * Тесты для [ImportPartnerService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ImportPartnerServiceTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var importPartnerService: ImportPartnerService

    @Autowired
    private lateinit var ytTableReaderFactory: YtTableReaderFactory


    @Test
    @DbUnitDataSet(after = ["ImportPartnerServiceTest/empty.after.csv"])
    fun `empty yt table`() {
        mockYt(emptyList())
        importPartnerService.doImport()
    }

    @Test
    @DbUnitDataSet(after = ["ImportPartnerServiceTest/new.after.csv"])
    fun `all entries are new`() {
        mockYt(listOf(
            PartnerYtEntry(id = 100L, type = PartnerYtEntryType.SHOP, Instant.now()),
            PartnerYtEntry(id = 200L, type = PartnerYtEntryType.SUPPLIER, Instant.now())
        ))

        importPartnerService.doImport()
    }

    @Test
    @DbUnitDataSet(
        before = ["ImportPartnerServiceTest/outdated.before.csv"],
        after = ["ImportPartnerServiceTest/outdated.after.csv"]
    )
    fun `replace outdated`() {
        mockYt(listOf(
            PartnerYtEntry(id = 100L, type = PartnerYtEntryType.SHOP, Instant.now()),
            PartnerYtEntry(id = 200L, type = PartnerYtEntryType.SUPPLIER, Instant.now())
        ))

        importPartnerService.doImport()
    }

    @Test
    @DbUnitDataSet(
        before = ["ImportPartnerServiceTest/actual.before.csv"],
        after = ["ImportPartnerServiceTest/actual.before.csv"]
    )
    fun `do not update new partners`() {
        mockYt(listOf(
            PartnerYtEntry(id = 100L, type = PartnerYtEntryType.SHOP, Instant.now()),
            PartnerYtEntry(id = 200L, type = PartnerYtEntryType.SUPPLIER, Instant.now())
        ))

        importPartnerService.doImport()
    }


    private fun mockYt(data: List<PartnerYtEntry>) {
        whenever(
            ytTableReaderFactory.createYQLPreparedReader<PartnerYtEntry>(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(YtTableReader { flow { data.forEach { emit(it) } } })
    }

}
