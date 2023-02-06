package ru.yandex.travel.orders.services.promo

import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.travel.orders.commons.proto.EPromoCodeNominalType
import ru.yandex.travel.orders.entities.promo.PromoAction
import ru.yandex.travel.orders.entities.promo.PromoCode
import ru.yandex.travel.orders.entities.promo.PromoCodeHelpers
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class PromoCodeHelpersTest {
    @Test
    fun `Valid dates are taken in correct order`() {
        val now = Instant.now()
        val actionValidFrom = now.minus(10, ChronoUnit.DAYS)
        val actionValidTill = now.plus(10, ChronoUnit.DAYS)
        val promoValidFrom = now.minus(5, ChronoUnit.DAYS)
        val promoValidTill = now.minus(1, ChronoUnit.DAYS)

        val promoAction = PromoAction().also { action ->
            action.id = UUID.randomUUID()
            action.name = "SUCCESS_ACTION"
            action.validFrom = actionValidFrom
            action.validTill = actionValidTill
        }
        val promoCodeWithoutDates = PromoCode().also { code ->
            code.id = UUID.randomUUID()
            code.code = "SUCCESS"
            code.nominal = BigDecimal.TEN
            code.nominalType = EPromoCodeNominalType.NT_PERCENT
            code.promoAction = promoAction
        }
        val promoCodeWithDates = PromoCode().also { code ->
            code.id = UUID.randomUUID()
            code.code = "FAIL"
            code.nominal = BigDecimal.TEN
            code.nominalType = EPromoCodeNominalType.NT_PERCENT
            code.promoAction = promoAction
            code.validFrom = promoValidFrom
            code.validTill = promoValidTill
        }

        Assertions.assertThat(PromoCodeHelpers.getPromoCodeValidFrom(promoCodeWithoutDates)).isEqualTo(actionValidFrom)
        Assertions.assertThat(PromoCodeHelpers.getPromoCodeValidTill(promoCodeWithoutDates)).isEqualTo(actionValidTill)
        Assertions.assertThat(PromoCodeHelpers.getPromoCodeValidFrom(promoCodeWithDates)).isEqualTo(promoValidFrom)
        Assertions.assertThat(PromoCodeHelpers.getPromoCodeValidTill(promoCodeWithDates)).isEqualTo(promoValidTill)
    }
}
