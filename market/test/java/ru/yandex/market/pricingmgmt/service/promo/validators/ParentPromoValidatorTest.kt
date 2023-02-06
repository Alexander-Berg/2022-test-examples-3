package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.exception.ExceptionCode
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.repository.postgres.ParentPromoRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal class ParentPromoValidatorTest : AbstractFunctionalTest() {
    @Autowired
    private lateinit var parentPromoValidator: ParentPromoValidator

    @Autowired
    private lateinit var parentPromoRepository: ParentPromoRepository

    private val oldPromo: Promo? = null
    private var promo: Promo = Promo()

    @BeforeEach
    fun setUp() {
        promo = Promo()
        promo.parentPromoId = "SP#002"
        promo.startDate = OffsetDateTime.of(2022, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toEpochSecond()
        promo.endDate = OffsetDateTime.of(2022, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC).toEpochSecond()
    }

    @Test
    @DbUnitDataSet(before = ["ParentPromoValidatorTest.csv"])
    fun validate_ok() {
        assertDoesNotThrow { parentPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    @DbUnitDataSet(before = ["ParentPromoValidatorTest.csv"])
    fun validate_parentPromoWithoutStartAndEndDates_ok() {
        promo.parentPromoId = "SP#001"
        assertDoesNotThrow { parentPromoValidator.validate(promo, oldPromo) }
    }

    @ParameterizedTest
    @EnumSource(value = PromoKind::class, names = ["NATIONAL", "CROSS_CATEGORY"], mode = EnumSource.Mode.EXCLUDE)
    fun validate_parentPromoId_null_ok(promoKind: PromoKind) {
        promo.promoKind = promoKind
        promo.parentPromoId = null
        assertDoesNotThrow { parentPromoValidator.validate(promo, oldPromo) }
    }

    @ParameterizedTest
    @EnumSource(value = PromoKind::class, names = ["NATIONAL", "CROSS_CATEGORY"], mode = EnumSource.Mode.INCLUDE)
    fun validate_parentPromoId_null_throws(promoKind: PromoKind) {
        promo.promoKind = promoKind
        promo.parentPromoId = null
        val e = assertThrows<ValidationException> { parentPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_PARENT_PROMO_NULL, e.code)
        assertEquals("Не указано родительское промо", e.message)
        assertEquals(listOf("parentPromoId"), e.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["ParentPromoValidatorTest.csv"])
    fun validate_parentPromoId_unknown_throws() {
        promo.parentPromoId = "parentPromoId_unknown"
        val e = assertThrows<ValidationException> { parentPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_PARENT_PROMO_NOT_FOUND, e.code)
        assertEquals("Указанное родительское промо не найдено, id: parentPromoId_unknown", e.message)
        assertEquals(listOf("parentPromoId"), e.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["ParentPromoValidatorTest.csv"])
    fun validate_startDate_null_throws() {
        promo.startDate = null
        val e = assertThrows<RuntimeException> { parentPromoValidator.validate(promo, oldPromo) }
        assertEquals("startDate == null", e.message)
    }

    @Test
    @DbUnitDataSet(before = ["ParentPromoValidatorTest.csv"])
    fun validate_startDate_before_throws() {
        val parentPromo = parentPromoRepository.findById(promo.parentPromoId!!)!!
        promo.startDate = parentPromo.startDate!! - 1
        val e = assertThrows<ValidationException> { parentPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_START_DATE_BEFORE_PARENT_PROMO_START_DATE, e.code)
        assertEquals("Дата старта промо раньше, чем дата старта родительского промо", e.message)
        assertEquals(listOf("startDate"), e.errorFields)
    }

    @Test
    @DbUnitDataSet(before = ["ParentPromoValidatorTest.csv"])
    fun validate_endDate_null_throws() {
        promo.endDate = null
        val e = assertThrows<RuntimeException> { parentPromoValidator.validate(promo, oldPromo) }
        assertEquals("endDate == null", e.message)
    }

    @Test
    @DbUnitDataSet(before = ["ParentPromoValidatorTest.csv"])
    fun validate_endDate_after_throws() {
        val parentPromo = parentPromoRepository.findById(promo.parentPromoId!!)!!
        promo.endDate = parentPromo.endDate!! + 1
        val e = assertThrows<ValidationException> { parentPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_END_DATE_AFTER_PARENT_PROMO_END_DATE, e.code)
        assertEquals("Дата окончания промо позже, чем дата окончания родительского промо", e.message)
        assertEquals(listOf("endDate"), e.errorFields)
    }
}
