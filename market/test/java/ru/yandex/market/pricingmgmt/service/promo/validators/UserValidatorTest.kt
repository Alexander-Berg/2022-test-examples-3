package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
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

class UserValidatorTest : AbstractFunctionalTest() {

    @Autowired
    private val userFieldsValidator: UserFieldsValidator? = null

    @DbUnitDataSet(before = ["UserValidatorTest.csv"])
    @Test
    fun allFieldsAreValid() {
        val promo = buildPromo()

        assertDoesNotThrow{userFieldsValidator?.validate(promo, null)}
    }

    @Test
    fun tradeManagerIsNull() {
        val promo = buildPromo()
        promo.tradeManager = null

        val e = assertThrows<ValidationException> { userFieldsValidator?.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_REQUIRED_FIELD_NULL, e.code)
        assertEquals("Не указан ответственный ТМ", e.message)
        assertEquals(listOf("tradeManager"), e.errorFields)
    }

    @Test
    fun tradeManagerIsEmpty() {
        val promo = buildPromo()
        promo.tradeManager = ""

        val e = assertThrows<ValidationException> { userFieldsValidator?.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_REQUIRED_FIELD_NULL, e.code)
        assertEquals("Не указан ответственный ТМ", e.message)
        assertEquals(listOf("tradeManager"), e.errorFields)
    }

    @Test
    fun markomIsNull() {
        val promo = buildPromo()
        promo.markom = null

        val e = assertThrows<ValidationException> {  userFieldsValidator?.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_REQUIRED_FIELD_NULL, e.code)
        assertEquals("Не указан ответственный МаркКом", e.message)
        assertEquals(listOf("markom"), e.errorFields)
    }

    @Test
    fun markomIsEmpty() {
        val promo = buildPromo()
        promo.markom = ""

        val e = assertThrows<ValidationException> { userFieldsValidator?.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_REQUIRED_FIELD_NULL, e.code)
        assertEquals("Не указан ответственный МаркКом", e.message)
        assertEquals(listOf("markom"), e.errorFields)
    }

    @Test
    fun authorIsNull() {
        val promo = buildPromo()
        promo.author = null

        val e = assertThrows<ValidationException> {  userFieldsValidator?.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_REQUIRED_FIELD_NULL, e.code)
        assertEquals("Не указан автор", e.message)
        assertEquals(listOf("author"), e.errorFields)
    }

    @Test
    fun authorIsEmpty() {
        val promo = buildPromo()
        promo.author = ""

        val e = assertThrows<ValidationException> { userFieldsValidator?.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_REQUIRED_FIELD_NULL, e.code)
        assertEquals("Не указан автор", e.message)
        assertEquals(listOf("author"), e.errorFields)
    }

    @DbUnitDataSet(before = ["UserValidatorTest.csv"])
    @Test
    fun userIsNotTrade() {
        val promo = buildPromo()
        promo.tradeManager = "trade1"

        val e = assertThrows<ValidationException> { userFieldsValidator?.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_TRADE_MANAGER_INVALID, e.code)
        assertEquals("Пользователь 'trade1' не является ТМ", e.message)
        assertEquals(listOf("tradeManager"), e.errorFields)
    }

    @DbUnitDataSet(before = ["UserValidatorTest.csv"])
    @Test
    fun userIsNotMarkom() {
        val promo = buildPromo()
        promo.markom = "catman1"

        val e = assertThrows<ValidationException> { userFieldsValidator?.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_MARKOM_INVALID, e.code)
        assertEquals("Пользователь 'catman1' не является МаркКом-ом", e.message)
        assertEquals(listOf("markom"), e.errorFields)
    }

    private fun buildPromo(): Promo {
        return Promo(
            status = PromoStatus.NEW,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            departments = listOf("department1", "department2"),
            purpose = PromoPurpose.GMV_GENERATION,
            compensationSource = Compensation.MARKET,
            tradeManager = "tradeManager",
            markom = "catManager",
            author = "author",
            promoKind = PromoKind.CATEGORY,
            supplierType = SupplierType.UNKNOWN,
            budgetOwner = PromoBudgetOwner.PRODUCT
        )
    }
}
