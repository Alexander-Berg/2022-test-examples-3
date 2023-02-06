package ru.yandex.market.mbi.orderservice.tms.persistence.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.orderservice.common.enum.ContractType
import ru.yandex.market.mbi.orderservice.common.model.pg.PartnerContractEntity
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.PartnerContractRepository
import java.time.LocalDate

class PartnerContractRepositoryTest : FunctionalTest() {

    @Autowired
    lateinit var partnerContractRepository: PartnerContractRepository

    @Test
    fun `test crud operations`() {
        partnerContractRepository.batchInsert(
            listOf(
                PartnerContractEntity(
                    100,
                    100,
                    1001,
                    "contract-1001",
                    ContractType.INCOME,
                    LocalDate.of(2021, 2, 1),
                    true
                ),
                PartnerContractEntity(
                    100,
                    100,
                    1002,
                    "contract-1002",
                    ContractType.OUTCOME,
                    LocalDate.of(2021, 2, 1),
                    true
                ),
            )
        )

        val contracts = partnerContractRepository.findContractsByPartnerId(100)
        assertThat(contracts).hasSize(2).satisfies {
            assertThat(it[0].partnerId).isEqualTo(100)
            assertThat(it[0].contractId).isEqualTo(1001)
            assertThat(it[1].partnerId).isEqualTo(100)
            assertThat(it[1].contractId).isEqualTo(1002)
        }
    }
}
