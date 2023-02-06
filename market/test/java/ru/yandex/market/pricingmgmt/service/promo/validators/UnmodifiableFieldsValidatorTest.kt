package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.yandex.market.pricingmgmt.exception.ExceptionCode
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoBudgetOwner
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoPurpose
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CheapestAsGift
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CompleteSetKind
import java.time.OffsetDateTime

class UnmodifiableFieldsValidatorTest {

    private val validator = UnmodifiableFieldsValidator()

    @Test
    fun testValidationIsOk() {
        Assertions.assertDoesNotThrow { validator.validate(buildNewPromo(), buildOldPromo()) }
    }

    @Test
    fun testModifyAssortmentLoadMethodThrowsException() {
        val oldPromo = buildOldPromo()
        oldPromo.assortmentLoadMethod = AssortmentLoadMethod.PI

        val newPromo = buildNewPromo()
        newPromo.assortmentLoadMethod = AssortmentLoadMethod.LOYALTY

        val e = assertThrows<ValidationException> { validator.validate(newPromo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_UNMODIFIED_FIELD_CHANGED, e.code)
        assertEquals("Поле \"Способ загрузки ассортимента\" не может быть изменено.", e.message)
        assertEquals(listOf("assortmentLoadMethod"), e.errorFields)
    }

    @Test
    fun testModifyFromNullAssortmentLoadMethodThrowsException() {
        val oldPromo = buildOldPromo()
        oldPromo.assortmentLoadMethod = null

        val newPromo = buildNewPromo()
        newPromo.assortmentLoadMethod = AssortmentLoadMethod.LOYALTY

        val e = assertThrows<ValidationException> { validator.validate(newPromo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_UNMODIFIED_FIELD_CHANGED, e.code)
        assertEquals("Поле \"Способ загрузки ассортимента\" не может быть изменено.", e.message)
        assertEquals(listOf("assortmentLoadMethod"), e.errorFields)
    }

    @Test
    fun testModifyToNullAssortmentLoadMethodThrowsException() {
        val oldPromo = buildOldPromo()
        oldPromo.assortmentLoadMethod = AssortmentLoadMethod.PI

        val newPromo = buildNewPromo()
        newPromo.assortmentLoadMethod = null

        val e = assertThrows<ValidationException> { validator.validate(newPromo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_UNMODIFIED_FIELD_CHANGED, e.code)
        assertEquals("Поле \"Способ загрузки ассортимента\" не может быть изменено.", e.message)
        assertEquals(listOf("assortmentLoadMethod"), e.errorFields)
    }

    @Test
    fun testModifyStartDateOfRunningPromoThrowsException() {
        val newPromo = buildNewPromo()
        newPromo.status = PromoStatus.READY
        newPromo.startDate = OffsetDateTime.now().minusDays(10).toEpochSecond()

        val oldPromo = buildOldPromo()
        oldPromo.startDate = OffsetDateTime.now().minusDays(20).toEpochSecond()
        oldPromo.status = PromoStatus.READY

        val e = assertThrows<ValidationException> { validator.validate(newPromo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RUNNING_START_DATE_CHANGED, e.code)
        assertEquals("Дата старта не может быть изменена во время действия акции.", e.message)
        assertEquals(listOf("startDate"), e.errorFields)
    }

    @Test
    fun testModifyFinishedPromoThrowsException() {
        val newPromo = buildNewPromo()
        newPromo.status = PromoStatus.READY
        newPromo.startDate = OffsetDateTime.now().minusDays(20).toEpochSecond()
        newPromo.endDate = OffsetDateTime.now().minusDays(10).toEpochSecond()
        newPromo.name = "New name"

        val oldPromo = buildOldPromo()
        oldPromo.status = PromoStatus.READY
        oldPromo.startDate = OffsetDateTime.now().minusDays(20).toEpochSecond()
        oldPromo.endDate = OffsetDateTime.now().minusDays(10).toEpochSecond()

        val e = assertThrows<ValidationException> { validator.validate(newPromo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_FINISHED_CHANGED, e.code)
        assertEquals("Акция не может быть изменена после ее окончания.", e.message)
        assertEquals(emptyList<String>(), e.errorFields)
    }

    @Test
    fun testModifySubFieldsThrowsException() {
        val newPromo = buildNewPromo()
        newPromo.status = PromoStatus.READY
        newPromo.startDate = OffsetDateTime.now().minusDays(20).toEpochSecond()
        newPromo.endDate = OffsetDateTime.now().minusDays(10).toEpochSecond()
        newPromo.cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_5)

        val oldPromo = buildOldPromo()
        oldPromo.status = PromoStatus.READY
        oldPromo.startDate = OffsetDateTime.now().minusDays(20).toEpochSecond()
        oldPromo.endDate = OffsetDateTime.now().minusDays(10).toEpochSecond()

        val e = assertThrows<ValidationException> { validator.validate(newPromo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_FINISHED_CHANGED, e.code)
        assertEquals("Акция не может быть изменена после ее окончания.", e.message)
        assertEquals(emptyList<String>(), e.errorFields)
    }

    private fun buildNewPromo(): Promo {
        return Promo(
            promoId = "123",
            startDate = OffsetDateTime.now().toEpochSecond(),
            endDate = OffsetDateTime.now().plusDays(1).toEpochSecond(),
            cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_3),
            departments = listOf("department1", "department2"),
            promoKind = PromoKind.CROSS_CATEGORY,
            purpose = PromoPurpose.GMV_GENERATION,
            budgetOwner = PromoBudgetOwner.PRODUCT,
            supplierType = SupplierType.THIRD_PARTY,
            compensationSource = Compensation.PARTNER,
            status = PromoStatus.NEW,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            tradeManager = "tradeManager",
            markom = "catManager",
            author = "authorLogin",
            assortmentLoadMethod = AssortmentLoadMethod.TRACKER
        )
    }

    private fun buildOldPromo(): Promo {
        return Promo(
            promoId = "123",
            startDate = OffsetDateTime.now().plusDays(10).toEpochSecond(),
            endDate = OffsetDateTime.now().plusDays(15).toEpochSecond(),
            cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_2),
            departments = listOf("department1", "department2"),
            promoKind = PromoKind.CATEGORY,
            purpose = PromoPurpose.CLIENT_ACQUISITION,
            budgetOwner = PromoBudgetOwner.CALL_CENTER,
            supplierType = SupplierType.FIRST_PARTY,
            compensationSource = Compensation.MARKET,
            status = PromoStatus.NEW,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            tradeManager = "oldTradeManager",
            markom = "oldCatManager",
            author = "authorLogin",
            assortmentLoadMethod = AssortmentLoadMethod.TRACKER
        )
    }
}
