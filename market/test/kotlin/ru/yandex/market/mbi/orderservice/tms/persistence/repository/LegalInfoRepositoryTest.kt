package ru.yandex.market.mbi.orderservice.tms.persistence.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.orderservice.common.model.pg.PartnerLegalInfoEntity
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.LegalInfoRepository

class LegalInfoRepositoryTest : FunctionalTest() {

    @Autowired
    lateinit var legalInfoRepository: LegalInfoRepository

    @Test
    fun `test crud operations`() {
        legalInfoRepository.batchInsert(
            listOf(
                PartnerLegalInfoEntity(
                    partnerId = 100,
                    inn = "124235213412",
                    kpp = "123412151234",
                    legalName = "supplier",
                    legalAddress = "Russia"
                ),
                PartnerLegalInfoEntity(
                    partnerId = 100,
                    inn = "12345",
                    kpp = "1337",
                    legalName = "supplier",
                    legalAddress = "Wonderland"
                ),
            )
        )

        val legalInfo = legalInfoRepository.findByPartnerId(100)
        assertThat(legalInfo!!).isNotNull.satisfies {
            assertThat(it.partnerId).isEqualTo(100)
            assertThat(it.inn).isEqualTo("12345")
            assertThat(it.kpp).isEqualTo("1337")
            assertThat(it.legalName).isEqualTo("supplier")
            assertThat(it.legalAddress).isEqualTo("Wonderland")
        }
    }
}
