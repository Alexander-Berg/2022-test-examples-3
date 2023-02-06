package ru.yandex.market.tpl.core.domain.surcharge.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeType
import ru.yandex.market.tpl.core.test.TplAbstractTest
import java.util.UUID

class SurchargeTypeRepositoryTest : TplAbstractTest() {

    @Autowired
    private lateinit var surchargeTypeRepository: SurchargeTypeRepository
    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun `create - success`() {
        val id = UUID.randomUUID().toString()

        val expected = SurchargeType(
            id = id,
            code = "code_test",
            name = "name test",
            type = SurchargeType.Type.PENALTY,
            description = "desc test",
            userShiftIsRequired = true,
            deleted = false,
        )

        surchargeTypeRepository.save(expected)

        transactionTemplate.execute {
            val result = surchargeTypeRepository.getById(id)

            assertThat(result).isEqualTo(expected)
        }
    }
}
