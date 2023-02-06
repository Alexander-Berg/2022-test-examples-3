package ru.yandex.market.partner.status.mbi.fflink.yt

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
 * Тесты для [ImportPartnerServiceLinkService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ImportPartnerServiceLinkServiceTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var importPartnerServiceLinkService: ImportPartnerServiceLinkService

    @Autowired
    private lateinit var ytTableReaderFactory: YtTableReaderFactory

    @Test
    @DbUnitDataSet(after = ["ImportPartnerServiceLinkServiceTest/empty.after.csv"])
    fun `empty yt table`() {
        mockYt(emptyList())
        importPartnerServiceLinkService.doImport()
    }

    @Test
    @DbUnitDataSet(after = ["ImportPartnerServiceLinkServiceTest/new.after.csv"])
    fun `all entries are new`() {
        mockYt(listOf(
            PartnerServiceLinkYtEntry(partnerId = 100L, 20100L, Instant.now()),
            PartnerServiceLinkYtEntry(partnerId = 200L, 20200L, Instant.now())
        ))

        importPartnerServiceLinkService.doImport()
    }

    @Test
    @DbUnitDataSet(
        before = ["ImportPartnerServiceLinkServiceTest/outdated.before.csv"],
        after = ["ImportPartnerServiceLinkServiceTest/outdated.after.csv"]
    )
    fun `replace outdated`() {
        mockYt(listOf(
            PartnerServiceLinkYtEntry(partnerId = 100L, 20100L, Instant.now()),
            PartnerServiceLinkYtEntry(partnerId = 200L, 20200L, Instant.now())
        ))

        importPartnerServiceLinkService.doImport()
    }

    @Test
    @DbUnitDataSet(
        before = ["ImportPartnerServiceLinkServiceTest/actual.before.csv"],
        after = ["ImportPartnerServiceLinkServiceTest/actual.after.csv"]
    )
    fun `do not update new links`() {
        mockYt(listOf(
            PartnerServiceLinkYtEntry(partnerId = 100L, 20100L, Instant.now(clock)),
            PartnerServiceLinkYtEntry(partnerId = 200L, 20200L, Instant.now(clock))
        ))

        importPartnerServiceLinkService.doImport()
    }

    @Test
    @DbUnitDataSet(
        before = ["ImportPartnerServiceLinkServiceTest/actual.before.csv"],
        after = ["ImportPartnerServiceLinkServiceTest/actual.before.csv"]
    )
    fun `all links from yt are outdated`() {
        mockYt(listOf(
            PartnerServiceLinkYtEntry(partnerId = 100L, 20100L, Instant.now(clock)),
        ))

        importPartnerServiceLinkService.doImport()
    }


    private fun mockYt(data: List<PartnerServiceLinkYtEntry>) {
        whenever(
            ytTableReaderFactory.createYQLPreparedReader<PartnerServiceLinkYtEntry>(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(YtTableReader { flow { data.forEach { emit(it) } } })
    }
}
