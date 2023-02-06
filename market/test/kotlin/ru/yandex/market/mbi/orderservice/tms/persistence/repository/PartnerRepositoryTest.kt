package ru.yandex.market.mbi.orderservice.tms.persistence.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.orderservice.common.enum.PlacementProgram
import ru.yandex.market.mbi.orderservice.common.model.pg.PartnerEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.PartnerRepository
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.LocalDateTime

class PartnerRepositoryTest : FunctionalTest() {

    @Autowired
    lateinit var partnerRepository: PartnerRepository

    @Test
    fun `test crud operations`() {
        val partner1 = PartnerEntity(
            partnerId = 100,
            businessId = 1000,
            name = "Test",
            organizationName = "Test",
            program = PlacementProgram.FBS
        )

        partnerRepository.upsert(partner1)
        val retrievedPartner1 = partnerRepository.findByPartnerId(100)
        assertThat(retrievedPartner1!!).satisfies {
            assertThat(it.partnerId).isEqualTo(100)
            assertThat(it.businessId).isEqualTo(1000)
            assertThat(it.name).isEqualTo("Test")
            assertThat(it.organizationName).isEqualTo("Test")
            assertThat(it.program).isEqualTo(PlacementProgram.FBS)
        }
        partnerRepository.upsert(partner1.copy(businessId = 1500, name = "Test2", updatedAt = LocalDateTime.now()))
        val retrievedPartner2 = partnerRepository.findByBusinessId(1500)[0]
        assertThat(retrievedPartner2).satisfies {
            assertThat(it.partnerId).isEqualTo(100)
            assertThat(it.businessId).isEqualTo(1500)
            assertThat(it.name).isEqualTo("Test2")
            assertThat(it.organizationName).isEqualTo("Test")
            assertThat(it.program).isEqualTo(PlacementProgram.FBS)
        }

        partnerRepository.upsert(
            partner1.copy(
                businessId = 1300,
                name = "Test3",
                updatedAt = LocalDateTime.now().minusDays(10)
            )
        )
        assertThat(partnerRepository.findByBusinessId(1300)).isEmpty()
        val retrievedPartner3 = partnerRepository.findByBusinessId(1500)[0]
        assertThat(retrievedPartner3).satisfies {
            assertThat(it.partnerId).isEqualTo(100)
            assertThat(it.businessId).isEqualTo(1500)
            assertThat(it.name).isEqualTo("Test2")
            assertThat(it.organizationName).isEqualTo("Test")
            assertThat(it.program).isEqualTo(PlacementProgram.FBS)
        }
    }
}
