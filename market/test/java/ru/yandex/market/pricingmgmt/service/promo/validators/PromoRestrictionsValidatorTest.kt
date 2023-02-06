package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.exception.ExceptionCode
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.restrictions.PromoCategoryRestrictionItem
import java.util.stream.Stream

class PromoRestrictionsValidatorTest : AbstractFunctionalTest() {
    @Autowired
    private lateinit var promoRestrictionsValidator: PromoRestrictionsValidator


    companion object {
        @JvmStatic
        fun getPromoMechanicsTypesWithCategoryDiscount(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(PromoMechanicsType.BLUE_FLASH.name),
                Arguments.of(PromoMechanicsType.DIRECT_DISCOUNT.name)
            )
        }
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_empty_ok() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.PROMO_CODE,
        )
        assertDoesNotThrow { promoRestrictionsValidator.validate(promo, null) }
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_notEmpty_ok() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.PROMO_CODE,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        assertDoesNotThrow { promoRestrictionsValidator.validate(promo, null) }
    }

    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithCategoryDiscount")
    fun validate_categoryWithDiscount_notEmpty_ok(mechanicsType: PromoMechanicsType) {
        val promo = Promo(
            mechanicsType = mechanicsType,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = 5),
                PromoCategoryRestrictionItem(id = 22, percent = 99)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        assertDoesNotThrow { promoRestrictionsValidator.validate(promo, null) }
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_supplier_throw() {
        val promo = Promo(
            partnersRestriction = listOf(1, 2, 3),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("Партнеры не найдены: 3", exception.message)
        assertEquals(listOf(Promo::partnersRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_category_throw() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.PROMO_CODE,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null),
                PromoCategoryRestrictionItem(id = 23, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("Категории не найдены: 23", exception.message)
        assertEquals(listOf(Promo::categoriesRestriction.name), exception.errorFields)
    }

    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithCategoryDiscount")
    fun validate_categoryWithDiscount_notFoundCategory_throw(mechanicsType: PromoMechanicsType) {
        val promo = Promo(
            mechanicsType = mechanicsType,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = 10),
                PromoCategoryRestrictionItem(id = 22, percent = 20),
                PromoCategoryRestrictionItem(id = 23, percent = 30)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("Категории не найдены: 23", exception.message)
        assertEquals(listOf(Promo::categoriesRestriction.name), exception.errorFields)
    }

    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithCategoryDiscount")
    fun validate_categoryWithDiscount_emptyCategory_throw(mechanicsType: PromoMechanicsType) {
        val promo = Promo(
            mechanicsType = mechanicsType,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = null, percent = 10),
                PromoCategoryRestrictionItem(id = 22, percent = 20)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("Список содержит строки без категорий", exception.message)
        assertEquals(listOf(Promo::categoriesRestriction.name), exception.errorFields)
    }

    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithCategoryDiscount")
    fun validate_categoryWithDiscount_duplicateCategory_throw(mechanicsType: PromoMechanicsType) {
        val promo = Promo(
            mechanicsType = mechanicsType,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = 10),
                PromoCategoryRestrictionItem(id = 21, percent = 20)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_DUPLICATE, exception.code)
        assertEquals("Список категорий содержит дубликаты: 21", exception.message)
        assertEquals(listOf(Promo::categoriesRestriction.name), exception.errorFields)
    }

    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithCategoryDiscount")
    fun validate_categoryWithDiscount_emptyPercent_throw(mechanicsType: PromoMechanicsType) {
        val promo = Promo(
            mechanicsType = mechanicsType,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = 10),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("Для категорий не указана величина скидки: 22", exception.message)
        assertEquals(listOf(Promo::categoriesRestriction.name), exception.errorFields)
    }

    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithCategoryDiscount")
    fun validate_categoryWithDiscount_several_throw(mechanicsType: PromoMechanicsType) {
        val promo = Promo(
            mechanicsType = mechanicsType,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = null, percent = 10),
                PromoCategoryRestrictionItem(id = 21, percent = 20),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals(
            "Список содержит строки без категорий\n" +
                "Для категорий не указана величина скидки: 22", exception.message
        )
        assertEquals(listOf(Promo::categoriesRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_vendor_throw() {
        val promo = Promo(
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32, 33),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("Вендоры не найдены: 33", exception.message)
        assertEquals(listOf(Promo::vendorsRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_msku_throw() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.PROMO_CODE,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42, 43),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("MSKU не найдены: 43", exception.message)
        assertEquals(listOf(Promo::mskusRestriction.name), exception.errorFields)
    }

    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithCategoryDiscount")
    fun validate_categoryWithDiscount_invalidPercent_throw(mechanicsType: PromoMechanicsType) {
        val promo = Promo(
            mechanicsType = mechanicsType,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = 4),
                PromoCategoryRestrictionItem(id = 22, percent = 100)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("Для категорий указана скидка вне допустимого диапазона [5,99]: 21,22", exception.message)
        assertEquals(listOf(Promo::categoriesRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_warehouse_throw() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.PROMO_CODE,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 53)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, exception.code)
        assertEquals("Склады не найдены: 53", exception.message)
        assertEquals(listOf(Promo::warehousesRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_duplicateCategory_throw() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.BLUE_SET,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 21, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_DUPLICATE, exception.code)
        assertEquals("Список категорий содержит дубликаты: 21", exception.message)
        assertEquals(listOf(Promo::categoriesRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_duplicatePartner_throw() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.BLUE_SET,
            partnersRestriction = listOf(1, 1),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_DUPLICATE, exception.code)
        assertEquals("Список партнеров содержит дубликаты: 1", exception.message)
        assertEquals(listOf(Promo::partnersRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_duplicateVendor_throw() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.BLUE_SET,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 31),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_DUPLICATE, exception.code)
        assertEquals("Список вендоров содержит дубликаты: 31", exception.message)
        assertEquals(listOf(Promo::vendorsRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_duplicateMsku_throw() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.BLUE_SET,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 41),
            warehousesRestriction = listOf(51, 52)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_DUPLICATE, exception.code)
        assertEquals("Список MSKU содержит дубликаты: 41", exception.message)
        assertEquals(listOf(Promo::mskusRestriction.name), exception.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["PromoRestrictionsValidatorTest.csv"])
    fun validate_duplicateWarehouse_throw() {
        val promo = Promo(
            mechanicsType = PromoMechanicsType.BLUE_SET,
            partnersRestriction = listOf(1, 2),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 21, percent = null),
                PromoCategoryRestrictionItem(id = 22, percent = null)
            ),
            vendorsRestriction = listOf(31, 32),
            mskusRestriction = listOf(41, 42),
            warehousesRestriction = listOf(51, 51)
        )

        val exception = assertThrows<ValidationException> { promoRestrictionsValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_DUPLICATE, exception.code)
        assertEquals("Список складов содержит дубликаты: 51", exception.message)
        assertEquals(listOf(Promo::warehousesRestriction.name), exception.errorFields)
    }
}

