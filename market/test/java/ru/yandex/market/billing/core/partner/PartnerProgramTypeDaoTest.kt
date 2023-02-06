package ru.yandex.market.billing.core.partner

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.billing.core.FunctionalTest
import ru.yandex.market.common.test.db.DbUnitDataSet
import java.time.LocalDateTime
import java.time.ZoneId

internal class PartnerProgramTypeDaoTest : FunctionalTest() {

    @Autowired
    private lateinit var partnerProgramTypeDao: PartnerProgramTypeDao


    @Test
    @DbUnitDataSet(before = ["PartnerProgramTypeDaoTest.common.before.csv"])
    fun getDropshipPartners() {
        val dropshipPartners = partnerProgramTypeDao.filterEverDropshipPartners(
            arrayListOf(
                4868830,
                5040418,
                5040889,
                5046121,
                5050000,
                5060000
            )
        )

        assertThat(dropshipPartners).hasSize(2)
        assertThat(dropshipPartners).containsExactlyInAnyOrder(5050000L, 5060000L)
    }

    @Test
    @DbUnitDataSet(before = ["PartnerProgramTypeDaoTest.common.before.csv"])
    fun getPartnerProgramTypeByIds() {
        val partnerProgramTypes = partnerProgramTypeDao.getPartnerProgramTypeByIds(
            arrayListOf(
                4868830,
                5040418,
                5040889,
                5046121
            )
        )
        // Прочитаем только 2, у остальных статус не SUCCESS
        assertThat(partnerProgramTypes).hasSize(2)
        assertThat(partnerProgramTypes).containsExactlyInAnyOrder(
            PartnerProgramType(
                partnerId = 4868830,
                program = PartnerPlacementProgramType.CPC,
                updateAt = LocalDateTime.of(2022, 3, 11, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant(),
                createdAt = LocalDateTime.of(2022, 3, 2, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant(),
                status = PartnerPlacementProgramStatus.SUCCESS,
                everActivated = true
            ),
            PartnerProgramType(
                partnerId = 5046121,
                program = PartnerPlacementProgramType.FULFILLMENT,
                updateAt = LocalDateTime.of(2022, 3, 10, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant(),
                createdAt = LocalDateTime.of(2022, 3, 10, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant(),
                status = PartnerPlacementProgramStatus.SUCCESS,
                everActivated = true
            )
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerProgramTypeDaoTest.common.before.csv"],
        after = ["PartnerProgramTypeDaoTest.store.after.csv"]
    )
    fun store() {
        val partnerProgramType = PartnerProgramType(
            partnerId = 100500,
            program = PartnerPlacementProgramType.CLICK_AND_COLLECT,
            updateAt = LocalDateTime.of(2022, 3, 1, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant(),
            createdAt = LocalDateTime.of(2022, 3, 1, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant(),
            status = PartnerPlacementProgramStatus.CONFIGURE,
            everActivated = false
        )

        partnerProgramTypeDao.store(arrayListOf(partnerProgramType))
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerProgramTypeDaoTest.common.before.csv"],
        after = ["PartnerProgramTypeDaoTest.clearTable.after.csv"]
    )
    fun clearTable() {
        partnerProgramTypeDao.clearTable()
    }
}
