package ru.yandex.market.mbi.orderservice.tms.persistence.repository

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.orderservice.common.model.pg.LogisticServicePartnerLinkEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.LogisticServicePartnerLinkRepository
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest

@DbUnitDataSet(before = ["logisticService/logisticServicePartnerLinkRepositoryTest.before.csv"])
class LogisticServicePartnerLinkRepositoryTest() : FunctionalTest() {
    @Autowired
    lateinit var logisticServicePartnerLinkRepository: LogisticServicePartnerLinkRepository

    @DbUnitDataSet(after = ["logisticService/logisticServicePartnerLinkRepositoryTest.after.csv"])
    @Test
    fun testSetPartnersLinks() {
        logisticServicePartnerLinkRepository.setLinksForPartners(
            mapOf(
                101L to listOf(
                    LogisticServicePartnerLinkEntity(101L, 1L, 1001L),
                    LogisticServicePartnerLinkEntity(101L, 2L, 1002L),
                    LogisticServicePartnerLinkEntity(101L, 3L, 1003L)
                ),
                102L to listOf(LogisticServicePartnerLinkEntity(102L, 8L, 1008L)),
            )
        )
    }

    @Test
    fun testGetPartnersLinks() {
        val actual = logisticServicePartnerLinkRepository.getLinksForPartners(listOf(101L, 102L))
        val expected = mapOf(
            101L to listOf(
                LogisticServicePartnerLinkEntity(101L, 1L, 1001L),
                LogisticServicePartnerLinkEntity(101L, 4L, 1004L)
            ),
            102L to emptyList()
        )
        assertThat(actual).isEqualTo(expected)
    }
}
