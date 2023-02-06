package ru.yandex.market.logistics.mqm.service.management

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.management.PartnerRelationParams
import java.time.Duration

class PartnerRelationParamsServiceImplTest: AbstractContextualTest() {
    @Autowired
    private lateinit var service: PartnerRelationParamsService

    @DisplayName("Проверка получения параметров")
    @Test
    @DatabaseSetup("/service/management/partner_relation_params_service/before/parameters.xml")
    fun getParameters() {
        val partnerRelationParams = service.findPartnerRelationParamsOrNull(
            FROM_PARTNER_1_ID,
            TO_PARTNER_1_ID
        )

        val expectedPartnerRelationParams = PartnerRelationParams(
            fromPartnerId = FROM_PARTNER_1_ID,
            fromPartnerName = FROM_PARTNER_1_NAME,
            toPartnerId = TO_PARTNER_1_ID,
            toPartnerName = TO_PARTNER_1_NAME,
            intakeDelta = Duration.ofHours(25).plusMinutes(30)
        )

        partnerRelationParams!!.fromPartnerId shouldBe expectedPartnerRelationParams.fromPartnerId
        partnerRelationParams.fromPartnerName shouldBe expectedPartnerRelationParams.fromPartnerName
        partnerRelationParams.toPartnerId shouldBe expectedPartnerRelationParams.toPartnerId
        partnerRelationParams.toPartnerName shouldBe expectedPartnerRelationParams.toPartnerName
        partnerRelationParams.intakeDelta shouldBe expectedPartnerRelationParams.intakeDelta
    }

    @DisplayName("Проверка возвращения пустого результата, если нет даных о партнере")
    @Test
    @DatabaseSetup("/service/management/partner_relation_params_service/before/parameters.xml")
    fun getEmptyResultsIfNotPartner() {
        service.findPartnerRelationParamsOrNull(
            FROM_PARTNER_2_ID,
            TO_PARTNER_2_ID
        ) shouldBe null
    }

    companion object {
        const val FROM_PARTNER_1_ID = 101L
        const val FROM_PARTNER_1_NAME = "name_101"
        const val FROM_PARTNER_2_ID = 102L
        const val TO_PARTNER_1_ID = 201L
        const val TO_PARTNER_1_NAME = "name_201"
        const val TO_PARTNER_2_ID = 202L
    }
}
