package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import ru.yandex.market.pricingmgmt.exception.ExceptionCode
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoBudgetOwner
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoPurpose
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import java.time.OffsetDateTime

class DateTimeValidatorTest {

    private val dateTimeValidator = DateTimeValidator()

    @Test
    fun testValidatePromoDateTime_noStartDate_exceptionThrown() {
        val promo = buildPromo()
        promo.endDate = OffsetDateTime.now().plusHours(1).toEpochSecond()

        val e = assertThrows<ValidationException> { dateTimeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_START_DATE_NULL, e.code)
        assertEquals("Не указана дата и время старта промо", e.message)
        assertEquals(listOf("startDate"), e.errorFields)
    }

    @Test
    fun testValidatePromoDateTime_noEndDate_exceptionThrown() {
        val promo = buildPromo()
        promo.startDate = OffsetDateTime.now().plusHours(1).toEpochSecond()

        val e = assertThrows<ValidationException> { dateTimeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_END_DATE_NULL, e.code)
        assertEquals("Не указана дата и время окончания промо", e.message)
        assertEquals(listOf("endDate"), e.errorFields)
    }

    @Test
    fun testValidatePromoDateTime_startDateIsAfterEndDate_exceptionThrown() {
        val promo = buildPromo()
        promo.startDate = OffsetDateTime.now().plusHours(1).toEpochSecond()
        promo.endDate = OffsetDateTime.now().toEpochSecond()

        val e = assertThrows<ValidationException> { dateTimeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_START_DATE_AFTER_END_DATE, e.code)
        assertEquals("Дата окончания промо раньше даты старта", e.message)
        assertEquals(listOf("startDate", "endDate"), e.errorFields)
    }

    @Test
    fun testValidatePromoDateTime_validationIsOk() {
        val promo = buildPromo()
        promo.startDate = OffsetDateTime.now().toEpochSecond()
        promo.endDate = OffsetDateTime.now().plusHours(1).toEpochSecond()

        assertDoesNotThrow { dateTimeValidator.validate(promo, null) }
    }

    private fun buildPromo(): Promo {
        return Promo(
            promoKind = PromoKind.CROSS_CATEGORY,
            purpose = PromoPurpose.GMV_GENERATION,
            budgetOwner = PromoBudgetOwner.PRODUCT,
            supplierType = SupplierType.THIRD_PARTY,
            compensationSource = Compensation.PARTNER,
            status = PromoStatus.NEW,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT
        )
    }
}
