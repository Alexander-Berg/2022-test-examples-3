package ru.yandex.market.abo.core.contractor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import ru.yandex.market.abo.core.contractor.loader.YtContractorLoader
import ru.yandex.market.abo.core.contractor.model.Contractor
import ru.yandex.market.abo.core.contractor.model.ContractorType
import ru.yandex.market.abo.core.yt.YtService
import ru.yandex.market.util.db.ConfigurationService
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @author i-shunkevich
 * @date 08.02.2022
 */
class ContractorManagerTest {

    private val ytService: YtService = mock()
    private val coreCounterService: ConfigurationService = mock()
    private val contractorService: ContractorService = mock()
    private val ytContractorLoaders: List<YtContractorLoader> = mock()

    private val contractorManager =
        ContractorManager(ytService, coreCounterService, contractorService, ytContractorLoaders)


    @Test
    fun findChangedContractors() {
        val dbContractor1 = createContractor(INN, null, null, ContractorType.PVZ)
        val dbContractor2 = createContractor(INN + 1, null, END_DATE_OLD, ContractorType.PVZ)

        val dbContractor4 = createContractor(INN + 3, OGRN, END_DATE_OLD, ContractorType.PVZ)
        val dbContractor5 = createContractor(INN + 4, OGRN + 1, END_DATE_OLD, ContractorType.PVZ)

        val dbContractor7 = createContractor(null, OGRN + 3, END_DATE_OLD, ContractorType.PVZ)
        val dbContractor8 = createContractor(null, OGRN + 4, null, ContractorType.PVZ)

        val dbContractor10 = createContractor(INN + 6, OGRN + 6, END_DATE_OLD, ContractorType.PVZ)
        val dbContractor11 = createContractor(INN + 7, OGRN + 7, END_DATE_OLD, ContractorType.PVZ)

        val dbContractor13 = createContractor(INN + 8, null, END_DATE_OLD, ContractorType.PVZ)

        val dbContractors = listOf(
            dbContractor1, dbContractor2, dbContractor4, dbContractor5, dbContractor7,
            dbContractor8, dbContractor10, dbContractor11, dbContractor13
        )


        val ytContractor1 = createContractor(INN, null, null, ContractorType.PVZ)
        val ytContractor2 = createContractor(INN + 1, null, END_DATE_NEW, ContractorType.PVZ)
        val ytContractor3 = createContractor(INN + 2, null, END_DATE_OLD, ContractorType.PVZ)

        val ytContractor4 = createContractor(INN + 3, OGRN, END_DATE_OLD, ContractorType.PVZ)
        val ytContractor5 = createContractor(INN + 4, OGRN + 1, null, ContractorType.PVZ)
        val ytContractor6 = createContractor(INN + 5, OGRN + 2, END_DATE_OLD, ContractorType.PVZ)

        val ytContractor7 = createContractor(null, OGRN + 3, END_DATE_OLD, ContractorType.PVZ)
        val ytContractor8 = createContractor(null, OGRN + 4, END_DATE_NEW, ContractorType.PVZ)
        val ytContractor9 = createContractor(null, OGRN + 5, END_DATE_OLD, ContractorType.PVZ)

        val ytContractor10 = createContractor(null, OGRN + 6, END_DATE_OLD, ContractorType.PVZ)
        val ytContractor11 = createContractor(null, OGRN + 7, END_DATE_NEW, ContractorType.PVZ)
        val ytContractor12 = createContractor(null, OGRN + 8, END_DATE_OLD, ContractorType.PVZ)

        val ytContractors = listOf(
            ytContractor1, ytContractor2, ytContractor3, ytContractor4, ytContractor5,
            ytContractor6, ytContractor7, ytContractor8, ytContractor9, ytContractor10, ytContractor11,
            ytContractor12
        )

        val expectedChangedContractors = listOf(
            dbContractor2, ytContractor3, dbContractor5, ytContractor6, dbContractor8, ytContractor9, dbContractor11,
            ytContractor12
        )

        val changedContractors =
            contractorManager.findChangedContractors(dbContractors, ytContractors, ContractorType.PVZ)

        assertEquals(expectedChangedContractors, changedContractors)
        assertEquals(END_DATE_NEW, dbContractor2.endDate)
        assertEquals(null, dbContractor5.endDate)
        assertEquals(END_DATE_NEW, dbContractor8.endDate)
        assertEquals(END_DATE_NEW, dbContractor11.endDate)
    }

    private fun createContractor(inn: Long?, ogrn: Long?, endDate: LocalDate?, contractorType: ContractorType) =
        Contractor().apply {
            this.inn = inn?.toString()
            this.ogrn = ogrn?.toString()
            this.name = "COMPANY"
            this.creationTime = LocalDateTime.now()
            this.modificationTime = LocalDateTime.now()
            this.endDate = endDate
            this.type = contractorType
        }

    companion object {
        private const val INN = 100000000000
        private const val OGRN = 1000000000000
        private val END_DATE_OLD = LocalDate.of(2022, 2, 9)
        private val END_DATE_NEW = LocalDate.of(2022, 2, 10)
    }
}


