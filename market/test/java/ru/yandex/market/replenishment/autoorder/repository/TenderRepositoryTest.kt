package ru.yandex.market.replenishment.autoorder.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.TenderAgreement
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.TenderSupplierInfo
import ru.yandex.market.replenishment.autoorder.repository.postgres.TenderRepository
import java.time.LocalDate

class TenderRepositoryTest : FunctionalTest() {
    @Autowired
    private lateinit var repo: TenderRepository

    @DbUnitDataSet(before = ["TenderRepositoryTest_rsIdToAgreementId.before.csv"])
    @Test
    fun rsIdToAgreementId() {
        val list = repo.getRsIdToAgreementId(1L).sortedBy { it.rsId }
        assertEquals(
            listOf(
                TenderAgreement("ВД-1", "Договор-1", "000001"),
                TenderAgreement("ВД-42", "Договор-42", "000002"),
                TenderAgreement("ВД-3", "Договор-3", "000003"),
            ), list
        )
    }

    @DbUnitDataSet(before = ["TenderRepositoryTest_findAllByDemandIdAndSupplierIds.before.csv"])
    @Test
    fun findAllByDemandIdAndSupplierIds() {
        val list = repo.findAllByDemandIdAndSupplierIds(1L, listOf(1, 2)).sortedBy { it.supplierId }
        assertEquals(
            listOf(
                TenderSupplierInfo(1, 1, true),
                TenderSupplierInfo(1, 2, false),
            ), list
        )
    }

    @DbUnitDataSet(before = ["TenderRepositoryTest_getLooserParticipants.before.csv"])
    @Test
    fun getLooserParticipants() {
        val list = repo.getLooserParticipants(1L).sorted()
        assertEquals(listOf(1L, 3L), list)
    }

    @DbUnitDataSet(
        before = ["TenderRepositoryTest_upsertTenderSupplierInfo.before.csv"],
        after = ["TenderRepositoryTest_upsertTenderSupplierInfo.after.csv"]
    )
    @Test
    fun upsertTenderSupplierInfo() {
        repo.upsertTenderSupplierInfo(TenderSupplierInfo(1, 1, false, "{\"sskus\":[\"foo\"]}"))
        repo.upsertTenderSupplierInfo(TenderSupplierInfo(1, 2, true))
    }

    @DbUnitDataSet(
        before = ["TenderRepositoryTest_setTenderRecommendationAdjustmentsOrderDate.before.csv"],
        after = ["TenderRepositoryTest_setTenderRecommendationAdjustmentsOrderDate.after.csv"]
    )
    @Test
    fun setTenderRecommendationAdjustmentsOrderDate() {
        repo.setTenderRecommendationAdjustmentsOrderDate(1, LocalDate.of(2022, 1, 13))
    }

    @DbUnitDataSet(
        before = ["TenderRepositoryTest_deanonymizeSupplier.before.csv"],
        after = ["TenderRepositoryTest_deanonymizeSupplier.after.csv"]
    )
    @Test
    fun deanonymizeSupplier() {
        repo.deanonymizeSupplier(1, 42000000, 1)
    }
}
