package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.TestUtils
import ru.yandex.market.pricingmgmt.client.promo.api.PromoApiClient
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
import ru.yandex.market.pricingmgmt.model.promo.mechanics.Promocode
import ru.yandex.market.pricingmgmt.model.promo.mechanics.PromocodeType
import java.util.*
import java.util.stream.Stream

class MechanicsTypeValidatorTest : AbstractFunctionalTest() {

    @MockBean
    private lateinit var promoApiClient: PromoApiClient

    @Autowired
    private lateinit var mechanicsTypeValidator: MechanicsTypeValidator

    companion object {
        private val promoMechanicsTypesWithWarehouseRequired = HashSet(
            listOf(
                PromoMechanicsType.CHEAPEST_AS_GIFT,
                PromoMechanicsType.BLUE_SET,
                PromoMechanicsType.SPREAD_DISCOUNT_RECEIPT,
                PromoMechanicsType.SPREAD_DISCOUNT_COUNT
            )
        )

        private val assortmentLoadMethods: EnumMap<AssortmentLoadMethod, Set<PromoMechanicsType>> =
            EnumMap(AssortmentLoadMethod::class.java)

        init {
            assortmentLoadMethods[AssortmentLoadMethod.TRACKER] = setOf(
                PromoMechanicsType.CHEAPEST_AS_GIFT,
                PromoMechanicsType.DIRECT_DISCOUNT,
                PromoMechanicsType.BLUE_FLASH,
                PromoMechanicsType.GENERIC_BUNDLE,
                PromoMechanicsType.BLUE_SET,
                PromoMechanicsType.SPREAD_DISCOUNT_COUNT,
                PromoMechanicsType.SPREAD_DISCOUNT_RECEIPT
            )

            assortmentLoadMethods[AssortmentLoadMethod.PI] = setOf(
                PromoMechanicsType.DIRECT_DISCOUNT,
                PromoMechanicsType.PROMO_CODE,
                PromoMechanicsType.CHEAPEST_AS_GIFT,
                PromoMechanicsType.BLUE_FLASH
            )

            assortmentLoadMethods[AssortmentLoadMethod.LOYALTY] = setOf(
                PromoMechanicsType.PROMO_CODE,
                PromoMechanicsType.COIN,
                PromoMechanicsType.CASHBACK
            )
        }

        private fun getPromoMechanicsTypes(predicate: (PromoMechanicsType) -> Boolean): Stream<Arguments> {
            return PromoMechanicsType.values()
                .filter { promoMechanicsType -> promoMechanicsType != PromoMechanicsType.UNKNOWN }
                .filter(predicate)
                .map { m -> Arguments.of(m) }.stream()
        }

        @JvmStatic
        fun getPromoMechanicsTypesAll(): Stream<Arguments> {
            return getPromoMechanicsTypes { true }
        }

        @JvmStatic
        fun getPromoMechanicsTypesWithWarehouseRequired(): Stream<Arguments> {
            return getPromoMechanicsTypes { promoMechanicsType ->
                promoMechanicsTypesWithWarehouseRequired.contains(
                    promoMechanicsType
                )
            }
        }

        @JvmStatic
        fun getPromoMechanicsTypesWithWarehouseNotRequired(): Stream<Arguments> {
            return getPromoMechanicsTypes { promoMechanicsType ->
                !promoMechanicsTypesWithWarehouseRequired.contains(
                    promoMechanicsType
                )
            }
        }

        @JvmStatic
        fun getTrackerMechanics(): Stream<Arguments> {
            return getPromoMechanicsTypes { promoMechanicsType ->
                assortmentLoadMethods[AssortmentLoadMethod.TRACKER]!!.contains(
                    promoMechanicsType
                )
            }
        }

        @JvmStatic
        fun getNotTrackerMechanics(): Stream<Arguments> {
            return getPromoMechanicsTypes { promoMechanicsType ->
                !assortmentLoadMethods[AssortmentLoadMethod.TRACKER]!!.contains(
                    promoMechanicsType
                )
            }
        }

        class PromoMechanicsTypesAssortmentLoadMethod(
            val promoMechanicsTypes: PromoMechanicsType,
            val assortmentLoadMethod: AssortmentLoadMethod,
            val allowed: Boolean
        )

        private fun getAssortmentLoadMethodAllowed(
            promoMechanicsTypes: PromoMechanicsType,
            assortmentLoadMethod: AssortmentLoadMethod
        ): Boolean {
            val set = assortmentLoadMethods[assortmentLoadMethod]
            if (set != null) {
                return set.contains(promoMechanicsTypes)
            }
            return false
        }

        private fun getPromoMechanicsTypesAssortmentLoadMethod(allowed: Boolean): Stream<Arguments> {
            return PromoMechanicsType.values()
                .filter { promoMechanicsType -> promoMechanicsType != PromoMechanicsType.UNKNOWN }
                .flatMap { promoMechanicsType ->
                    AssortmentLoadMethod.values()
                        .filter { assortmentLoadMethod -> assortmentLoadMethod != AssortmentLoadMethod.UNKNOWN }
                        .map { assortmentLoadMethod ->
                            PromoMechanicsTypesAssortmentLoadMethod(
                                promoMechanicsType,
                                assortmentLoadMethod,
                                getAssortmentLoadMethodAllowed(promoMechanicsType, assortmentLoadMethod)
                            )
                        }
                }
                .filter { promoMechanicsTypesAssortmentLoadMethod -> promoMechanicsTypesAssortmentLoadMethod.allowed == allowed }
                .map { m -> Arguments.of(m.promoMechanicsTypes, m.assortmentLoadMethod) }
                .stream()
        }

        @JvmStatic
        fun getPromoMechanicsTypesWithAssortmentLoadMethodAllowed(): Stream<Arguments> {
            return getPromoMechanicsTypesAssortmentLoadMethod(true)
        }

        @JvmStatic
        fun getPromoMechanicsTypesWithAssortmentLoadMethodNotAllowed(): Stream<Arguments> {
            return getPromoMechanicsTypesAssortmentLoadMethod(false)
        }
    }

    @Test
    fun testValidateMechanicsType_mechanicsTypeIsUnknown_exceptionThrown() {
        val promo = buildPromo()
        promo.mechanicsType = PromoMechanicsType.UNKNOWN
        promo.cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_3)

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_MECHANICS_TYPE_UNKNOWN, e.code)
        assertEquals("Тип механики не распознан", e.message)
        assertEquals(listOf("mechanicsType"), e.errorFields)
    }

    @Test
    fun testValidateMechanicsType_mechanicsTypeIsOk_cheapestAsGiftIsNull_exceptionThrown() {
        val promo = buildPromo(PromoMechanicsType.CHEAPEST_AS_GIFT)
        promo.cheapestAsGift = null
        promo.assortmentLoadMethod = AssortmentLoadMethod.PI

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_MECHANICS_DATA_INVALID, e.code)
        assertEquals("Тип механики не соответствует данным заявки", e.message)
        assertEquals(listOf("cheapestAsGift"), e.errorFields)
    }

    @Test
    fun testValidateMechanicsType_mechanicsTypeIsOk_cheapestAsGiftIsNull_ok() {
        val promo = buildPromo(PromoMechanicsType.CHEAPEST_AS_GIFT)
        promo.cheapestAsGift = null
        promo.assortmentLoadMethod = null

        assertDoesNotThrow { mechanicsTypeValidator.validate(promo, null) }
    }

    @Test
    fun testValidateMechanicsType_mechanicsTypeIsOk_cheapestAsGiftCountIsUnknown_exceptionThrown() {
        val promo = buildPromo()
        promo.cheapestAsGift = CheapestAsGift(CompleteSetKind.UNKNOWN)

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_CHEAPEST_AS_GIFT_COMPLETE_SET_KIND_UNKNOWN, e.code)
        assertEquals("Количество товаров в комплекте не распознано", e.message)
        assertEquals(listOf("cheapestAsGift.count"), e.errorFields)
    }

    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesAll")
    fun testValidateMechanicsType_validationIsOk(mechanicsType: PromoMechanicsType) {
        val promo = buildPromo(mechanicsType)
        promocodeIsAvailableStub()
        assertDoesNotThrow { mechanicsTypeValidator.validate(promo, null) }
    }

    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesAll")
    fun testValidateMechanicsType_warehouseNotNull_validationIsOk(mechanicsType: PromoMechanicsType) {
        val promo = buildPromo(mechanicsType)
        promocodeIsAvailableStub()

        assertDoesNotThrow { mechanicsTypeValidator.validate(promo, null) }
    }

    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithWarehouseNotRequired")
    fun testValidateMechanicsType_warehouseNull_validationIsOk(mechanicsType: PromoMechanicsType) {
        val promo = buildPromo(mechanicsType)
        promo.warehousesRestriction = null
        promocodeIsAvailableStub()

        assertDoesNotThrow { mechanicsTypeValidator.validate(promo, null) }
    }

    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithWarehouseRequired")
    fun testValidateMechanicsType_warehouseNull_exceptionThrown(mechanicsType: PromoMechanicsType) {
        val promo = buildPromo(mechanicsType)
        promo.warehousesRestriction = null

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_WAREHOUSES_EMPTY, e.code)
        assertEquals("Для механики \"${mechanicsType.displayName}\" не указаны склады", e.message)
        assertEquals(listOf("warehousesRestriction"), e.errorFields)
    }

    @ParameterizedTest
    @MethodSource("getTrackerMechanics")
    fun testValidateMechanicsType_tracker_severalWarehouses_exceptionThrown(mechanicsType: PromoMechanicsType) {
        val promo = buildPromo(mechanicsType)
        promo.warehousesRestriction = listOf(1L, 2L, 3L)

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_WAREHOUSES_MORE_THAN_ONE, e.code)
        assertEquals("Указано более одного склада", e.message)
        assertEquals(listOf("warehousesRestriction"), e.errorFields)
    }

    @ParameterizedTest
    @MethodSource("getNotTrackerMechanics")
    fun testValidateMechanicsType_tracker_severalWarehouses_ok(mechanicsType: PromoMechanicsType) {
        val promo = buildPromo(mechanicsType)
        promo.warehousesRestriction = listOf(1L, 2L, 3L)
        promocodeIsAvailableStub()

        assertDoesNotThrow { mechanicsTypeValidator.validate(promo, null) }
    }

    private fun buildPromo(): Promo {
        return buildPromo(PromoMechanicsType.CHEAPEST_AS_GIFT)
    }

    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesAll")
    fun testValidateMechanicsType_assortmentLoadMethodNull_validationIsOk(mechanicsType: PromoMechanicsType) {
        val promo = buildPromo(mechanicsType)
        promo.assortmentLoadMethod = null
        promocodeIsAvailableStub()

        assertDoesNotThrow { mechanicsTypeValidator.validate(promo, null) }
    }

    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithAssortmentLoadMethodAllowed")
    fun testValidateMechanicsType_assortmentLoadMethodNotNull_validationIsOk(
        mechanicsType: PromoMechanicsType,
        assortmentLoadMethod: AssortmentLoadMethod
    ) {
        val promo = buildPromo(mechanicsType)
        promo.assortmentLoadMethod = assortmentLoadMethod
        promocodeIsAvailableStub()

        assertDoesNotThrow { mechanicsTypeValidator.validate(promo, null) }
    }

    @ParameterizedTest
    @MethodSource("getPromoMechanicsTypesWithAssortmentLoadMethodNotAllowed")
    fun testValidateMechanicsType_assortmentLoadMethodNotNull_exceptionThrown(
        mechanicsType: PromoMechanicsType,
        assortmentLoadMethod: AssortmentLoadMethod
    ) {
        val promo = buildPromo(mechanicsType)
        promo.assortmentLoadMethod = assortmentLoadMethod
        promocodeIsAvailableStub()

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_ASSORTMENT_LOAD_METHOD_FORBIDDEN_FOR_MECHANICS_TYPE, e.code)
        assertEquals(
            "Способ загрузки ассортимента \"${assortmentLoadMethod.displayName}\" запрещен для типа механики \"${mechanicsType.displayName}\"",
            e.message
        )
        assertEquals(listOf("assortmentLoadMethod"), e.errorFields)
    }

    private fun buildPromo(mechanicsType: PromoMechanicsType): Promo {
        val promo = Promo(
            promoKind = PromoKind.CROSS_CATEGORY,
            purpose = PromoPurpose.GMV_GENERATION,
            budgetOwner = PromoBudgetOwner.PRODUCT,
            supplierType = SupplierType.THIRD_PARTY,
            compensationSource = Compensation.PARTNER,
            status = PromoStatus.NEW,
            mechanicsType = mechanicsType,
            warehousesRestriction = listOf(1L),
            startDate = 1,
            endDate = 2
        )

        when (mechanicsType) {
            PromoMechanicsType.CHEAPEST_AS_GIFT -> promo.cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_2)
            PromoMechanicsType.PROMO_CODE -> promo.promocode = Promocode(
                codeType = PromocodeType.FIXED_DISCOUNT,
                value = 1,
                code = "code",
                minCartPrice = 2,
                maxCartPrice = 3,
                applyMultipleTimes = true,
                additionalConditions = "additionalConditions"
            )
            else -> {}
        }

        return promo
    }

    @Test
    fun testValidateMechanicsType_promocodeNull_exceptionThrown() {
        val promo = buildPromo(PromoMechanicsType.PROMO_CODE)
        promo.promocode = null
        promo.assortmentLoadMethod = AssortmentLoadMethod.PI

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_MECHANICS_DATA_INVALID, e.code)
        assertEquals("Тип механики не соответствует данным заявки", e.message)
        assertEquals(listOf("promocode"), e.errorFields)
    }

    @Test
    fun testValidateMechanicsType_promocodeNull_ok() {
        val promo = buildPromo(PromoMechanicsType.PROMO_CODE)
        promo.promocode = null
        promo.assortmentLoadMethod = AssortmentLoadMethod.LOYALTY

        assertDoesNotThrow { mechanicsTypeValidator.validate(promo, null) }
    }

    @Test
    fun testValidateMechanicsType_promocodeCodeTypeNull_exceptionThrown() {
        val promo = buildPromo(PromoMechanicsType.PROMO_CODE)
        promo.promocode!!.codeType = null

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_REQUIRED_FIELD_NULL, e.code)
        assertEquals("Не указано обязательное поле \"Тип промокода\"", e.message)
        assertEquals(listOf("promocode.codeType"), e.errorFields)
    }

    @Test
    fun testValidateMechanicsType_promocodeValueNull_exceptionThrown() {
        val promo = buildPromo(PromoMechanicsType.PROMO_CODE)
        promo.promocode!!.value = null

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_PROMOCODE_VALUE_NULL, e.code)
        assertEquals("Не указан номинал промокода", e.message)
        assertEquals(listOf("promocode.value"), e.errorFields)
    }

    @Test
    fun testValidateMechanicsType_promocodeCodeNull_exceptionThrown() {
        val promo = buildPromo(PromoMechanicsType.PROMO_CODE)
        promo.promocode!!.code = null

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_PROMOCODE_CODE_EMPTY, e.code)
        assertEquals("Не указан код промокода", e.message)
        assertEquals(listOf("promocode.code"), e.errorFields)
    }

    @Test
    fun testValidateMechanicsType_severalMechanicData_exceptionThrown() {
        val promo = buildPromo(PromoMechanicsType.PROMO_CODE)
        promo.cheapestAsGift = CheapestAsGift(completeSetKind = CompleteSetKind.SET_OF_10)

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_MECHANICS_DATA_INVALID, e.code)
        assertEquals("Тип механики не соответствует данным заявки", e.message)
        assertEquals(listOf("cheapestAsGift"), e.errorFields)
    }

    @Test
    fun testPromocodeCodeChange_exceptionThrown() {
        val newPromo = buildPromo(PromoMechanicsType.PROMO_CODE)
        val oldPromp = buildPromo(PromoMechanicsType.PROMO_CODE)
        oldPromp.promocode?.code = oldPromp.promocode?.code + "CHANGE"
        promocodeIsAvailableStub()

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(newPromo, oldPromp) }

        assertEquals(ExceptionCode.PROMO_PROMOCODE_CODE_CHANGE, e.code)
        assertEquals("Изменение кода промокода запрещено", e.message)
        assertEquals(listOf("promocode.code"), e.errorFields)
    }

    @Test
    fun testPromocodeIsNotAvailable_exceptionThrown() {
        val promo = buildPromo(PromoMechanicsType.PROMO_CODE)

        `when`(
            promoApiClient.isPromocodeAvailable(
                TestUtils.any(String::class.java),
                TestUtils.any(Long::class.java),
                TestUtils.any(Long::class.java)
            )
        ).thenReturn(false)

        val e = assertThrows<ValidationException> { mechanicsTypeValidator.validate(promo, null) }

        assertEquals(ExceptionCode.PROMO_PROMOCODE_CODE_OCCUPIED, e.code)
        assertEquals("Указанный код промокода уже используется", e.message)
        assertEquals(listOf("promocode.code"), e.errorFields)
    }

    private fun promocodeIsAvailableStub() {
        `when`(
            promoApiClient.isPromocodeAvailable(
                TestUtils.any(String::class.java),
                TestUtils.any(Long::class.java),
                TestUtils.any(Long::class.java)
            )
        ).thenReturn(true)
    }
}
