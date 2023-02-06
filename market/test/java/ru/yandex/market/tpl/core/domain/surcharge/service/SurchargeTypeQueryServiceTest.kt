package ru.yandex.market.tpl.core.domain.surcharge.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeType
import ru.yandex.market.tpl.core.domain.surcharge.repository.SurchargeTypeRepository
import ru.yandex.market.tpl.core.test.TplAbstractTest
import java.util.UUID

internal class SurchargeTypeQueryServiceTest : TplAbstractTest() {

    @Autowired
    private lateinit var surchargeTypeRepository: SurchargeTypeRepository
    @Autowired
    private lateinit var surchargeTypeQueryService: SurchargeTypeQueryService

    @Test
    fun `findAll - success`() {
        val code1 = "1"
        val code2 = "2"
        val code3 = "3"

        surchargeTypeRepository.save(getType(code1))
        surchargeTypeRepository.save(getType(code2))
        surchargeTypeRepository.save(
            getType(code3).apply {
                this.deleted = true
            }
        )

        val result = surchargeTypeQueryService.findAll()
        assertThat(result.size).isEqualTo(2)
        assertThat(result.any { it.code == code1 }).isTrue
        assertThat(result.any { it.code == code2 }).isTrue
        assertThat(result.any { it.code == code3 }).isFalse
    }

    private fun getType(code: String) = SurchargeType(
        id = UUID.randomUUID().toString(),
        code = code,
        name = "name for $code",
        type = SurchargeType.Type.PENALTY,
        description = "desc for $code",
        userShiftIsRequired = true,
        deleted = false,
    )

}
